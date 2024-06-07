import kotlin.math.*

interface ICity {
    val name: Name
    val block: IBlock
    val nextCity: ICity
    override fun toString(): String
    fun eval(env: Map<String, Double>): String
}

interface IBlock {
    val name: Name
    val type: String
    val command: ICommand
    val nextBlock: IBlock
    override fun toString(): String
    fun eval(env: Map<String, Double>): String
}

interface ICommand {
    val nextCommand: ICommand
    override fun toString(): String
    fun eval(env: Map<String, Double>): String
}

interface IExpression {
    override fun toString(): String
    fun eval(env: Map<String, Double> = emptyMap()): String
}

var IS_COLLECTION = false

val envGlobal: MutableMap<String, Double> = mutableMapOf()

object Nil : IBlock, ICommand, IExpression, ICity {
    override val nextCommand: ICommand = Nil
    override val name: Name = Name("")
    override val block: IBlock = Nil
    override val nextCity: ICity = Nil
    override val type: String = ""
    override val command: ICommand = Nil
    override val nextBlock: IBlock = Nil

    override fun toString(): String {
        return ""
    }

    override fun eval(env: Map<String, Double>): String {
        return ""
    }
}

class City(
    override val name: Name,
    override val block: IBlock = Nil,
    override val nextCity: ICity = Nil
) : ICity {
    override fun toString(): String {
        return """
city $name {
    $block
}
$nextCity
        """
    }

    override fun eval(env: Map<String, Double>): String {
        val cityGeoJson = """
        {
            "type": "FeatureCollection",            
            "features": [
                ${block.eval(env)}
            ]
        }
        """
        return if (nextCity is Nil) cityGeoJson.trimIndent() else cityGeoJson.trimIndent() + ",\n" + nextCity.eval(env)
    }
}

abstract class Block(
    override val type: String,
    override val name: Name,
    override val command: ICommand = Nil,
    override val nextBlock: IBlock = Nil,
    val club: Name = Name(""),
    val capacity: UInt = 0u,
) : IBlock {
    override fun toString(): String {
        return """
$type $name {
    $command
};
$nextBlock
        """.trimIndent()
    }

    override fun eval(env: Map<String, Double>): String {
        val blockGeoJson = """
        {
            "type": "Feature",
            "properties": {
                "name": "$name"
                ${if (club.toString().isNotEmpty()) ", \n\"club\": \"$club\"" else ""}
                ${if (capacity != 0u) ", \n\"capacity\": $capacity" else ""}
            },
            "geometry": ${getMultipleGeometryStart(command)}      
                ${command.eval(env)}
            ${getMultipleGeometryEnd(command)}
        }
        """
        return if (nextBlock is Nil) blockGeoJson.trimIndent() else blockGeoJson.trimIndent() + ",\n" + nextBlock.eval(
            env
        )
    }
}

class Road(name: Name, command: ICommand, nextBlock: IBlock = Nil) : Block("road", name, command, nextBlock)

class Building(name: Name, command: ICommand, nextBlock: IBlock = Nil) : Block("building", name, command, nextBlock)

class Stadium(name: Name, command: ICommand, nextBlock: IBlock = Nil, club: Name = Name(""), capacity: UInt = 0u) :
    Block("stadium", name, command, nextBlock, club, capacity)

class Arena(name: Name, command: ICommand, nextBlock: IBlock = Nil, club: Name = Name(""), capacity: UInt = 0u) :
    Block("arena", name, command, nextBlock, club, capacity)

class Line(
    val start: Point,
    val end: Point,
    override val nextCommand: ICommand = Nil
) : ICommand {
    override fun toString(): String {
        return """
line ($start, $end)
$nextCommand
        """.trimIndent()
    }

    override fun eval(env: Map<String, Double>): String {
        val lineGeoJson = """
        {
            "type": "LineString",
            "coordinates": [
                [${start.lon.eval(env)}, ${start.lat.eval(env)}],
                [${end.lon.eval(env)}, ${end.lat.eval(env)}]
            ]
        }
        """
        return if (nextCommand is Nil) lineGeoJson.trimIndent() else lineGeoJson.trimIndent() + ",\n" + nextCommand.eval(
            env
        )
    }
}

class Bend(
    val start: Point,
    val end: Point,
    val angle: Real,
    override val nextCommand: ICommand = Nil
) : ICommand {
    override fun toString(): String {
        return """
bend ($start, $end, $angle)
$nextCommand
        """.trimIndent()
    }

    override fun eval(env: Map<String, Double>): String {
        val bendPoints = getBendPoints(start, end, angle, env)
        // Note: GeoJSON does not support 'Bend', but we can represent it as a LineString with additional properties
        val bendGeoJson = """
            {
                "type": "LineString",
                "coordinates": [
                    ${bendPoints.joinToString(",\n") { it.eval(env) }}   
                ]
            }
        """
        return if (nextCommand is Nil) bendGeoJson.trimIndent() else bendGeoJson.trimIndent() + ",\n" + nextCommand.eval(
            env
        )
    }
}

class Box(
    val start: Point,
    val end: Point,
    override val nextCommand: ICommand = Nil
) : ICommand {
    override fun toString(): String {
        return """
box ($start, $end)
$nextCommand
        """.trimIndent()
    }

    override fun eval(env: Map<String, Double>): String {
        val boxGeoJson = """
            {
                "type": "Polygon",
                "coordinates": [[
                    [${start.lon.eval(env)}, ${start.lat.eval(env)}],
                    [${end.lon.eval(env)}, ${start.lat.eval(env)}],
                    [${end.lon.eval(env)}, ${end.lat.eval(env)}],
                    [${start.lon.eval(env)}, ${end.lat.eval(env)}],
                    [${start.lon.eval(env)}, ${start.lat.eval(env)}]
                ]]
            }
        """
        return if (nextCommand is Nil) boxGeoJson.trimIndent() else boxGeoJson.trimIndent() + ",\n" + nextCommand.eval(
            env
        )
    }
}

class Circle(
    val center: Point,
    val radius: Real,
    override val nextCommand: ICommand = Nil
) : ICommand {
    override fun toString(): String {
        return """
circle ($center, $radius)
$nextCommand
        """.trimIndent()
    }

    override fun eval(env: Map<String, Double>): String {
        val circlePoints = getCirclePoints(center, radius, env)
        val circleGeoJson = """
        {
            "type": "Polygon",
            "coordinates": [[
                ${circlePoints.joinToString(",\n") { it.eval(env) }}
            ]]
        }
        """
        return if (nextCommand is Nil) circleGeoJson.trimIndent() else circleGeoJson.trimIndent() + ",\n" + nextCommand.eval(
            env
        )
    }
}

class Rect(
    val bottomLeft: Point,
    val bottomRight: Point,
    val topRight: Point,
    val topLeft: Point,
    override val nextCommand: ICommand = Nil
) : ICommand {
    override fun toString(): String {
        return """
rect ($bottomLeft, $bottomRight, $topRight, $topLeft)
$nextCommand
        """.trimIndent()
    }

    override fun eval(env: Map<String, Double>): String {
        val boxGeoJson = """
        {
            "type": "Polygon",
            "coordinates": [[
                [${bottomLeft.lon.eval(env)}, ${bottomLeft.lat.eval(env)}],
                [${bottomRight.lon.eval(env)}, ${bottomRight.lat.eval(env)}],
                [${topRight.lon.eval(env)}, ${topRight.lat.eval(env)}],
                [${topLeft.lon.eval(env)}, ${topLeft.lat.eval(env)}],
                [${bottomLeft.lon.eval(env)}, ${bottomLeft.lat.eval(env)}]
            ]]
        }
        """
        return if (nextCommand is Nil) boxGeoJson.trimIndent() else boxGeoJson.trimIndent() +
                ",\n" + nextCommand.eval(env)
    }
}

class Define(
    val name: String,
    val value: Expr,
    override val nextCommand: ICommand = Nil
) : ICommand {
    override fun toString(): String {
        return "$name = $value"
    }

    override fun eval(env: Map<String, Double>): String {
        val newEnv = env.toMutableMap()
        newEnv[name] = value.eval(env)
        envGlobal[name] = value.eval(newEnv)
        return nextCommand.eval(newEnv)
    }

}

class ForLoop(
    val define: Define,
    val end: Expr,
    val body: ICommand,
    override val nextCommand: ICommand = Nil
) : ICommand {
    init {
        IS_COLLECTION = true
    }

    override fun toString(): String {
        return """
for (${define.name} = ${define.value}, $end) {
    $body
}
$nextCommand
        """.trimIndent()
    }

    override fun eval(env: Map<String, Double>): String {
        var commands = ""
        var i = define.value.eval(env).toInt()
        val k = end.eval(env).toInt()
        if (k - i < 0) {
            return nextCommand.eval(env)
        }
        val newEnv = env.toMutableMap()
        while (i <= k) {
            newEnv[define.name] = i.toDouble()
            envGlobal[define.name] = i.toDouble()
            val body = body.eval(newEnv)
            commands += body + if (body.length > 1) {
                ",\n"
            } else {
                ""
            }
            i++
        }
        commands = commands.substring(0, commands.length - 2)
        return if (nextCommand is Nil) commands.trimIndent() else commands.trimIndent() + ",\n" + nextCommand.eval(
            newEnv
        )
    }
}

class If(
    val condition: LogicExpr,
    val body: ICommand,
    val elseBody: ICommand = Nil,
    override val nextCommand: ICommand = Nil
) : ICommand {
    override fun toString(): String {
        return """if ($condition) { $body } else { $elseBody }"""
    }

    override fun eval(env: Map<String, Double>): String {
        return if (condition.eval(env)) {
            body.eval(env)
        } else {
            elseBody.eval(env)
        }
    }
}

class Point(
    val lat: Expr,
    val lon: Expr
) : IExpression {

    override fun toString(): String {
        return "(${lon}, $lat)"
    }

    override fun eval(env: Map<String, Double>): String {
        return "[${lon.eval(env)}, ${lat.eval(env)}]"
    }
}

class Name(private val value: String) : IExpression {
    override fun toString(): String {
        return value.trim('\"')
    }

    override fun eval(env: Map<String, Double>): String {
        return value.trim('\"')
    }
}

interface Expr {
    fun eval(env: Map<String, Double> = emptyMap()): Double
}

abstract class BinaryOperation(private val first: Expr, private val second: Expr, private val operator: String) : Expr {
    override fun toString(): String {
        return "($first $operator $second)"
    }

    override fun eval(env: Map<String, Double>): Double {
        val left = first.eval(env)
        val right = second.eval(env)
        return when (operator) {
            "+" -> left + right
            "-" -> left - right
            "*" -> left * right
            "/" -> left / right
            "//" -> (left.toInt() / right.toInt()).toDouble()
            "^" -> left.pow(right)
            else -> throw IllegalArgumentException("Unknown operator")
        }
    }
}

abstract class UnaryOperation(private val expr: Expr, private val operator: String = "") : Expr {
    override fun toString(): String {
        return "($operator$expr)"
    }

    override fun eval(env: Map<String, Double>): Double {
        return when (operator) {
            "+" -> expr.eval(env)
            "-" -> -expr.eval(env)
            else -> throw IllegalArgumentException("Unknown operator")
        }
    }
}

data class Plus(val first: Expr, val second: Expr) : BinaryOperation(first, second, "+")
data class Minus(val first: Expr, val second: Expr) : BinaryOperation(first, second, "-")
data class Times(val first: Expr, val second: Expr) : BinaryOperation(first, second, "*")
data class Divides(val first: Expr, val second: Expr) : BinaryOperation(first, second, "/")
data class IntegerDivides(val first: Expr, val second: Expr) : BinaryOperation(first, second, "//")
data class Pow(val first: Expr, val second: Expr) : BinaryOperation(first, second, "^")

data class UnaryPlus(val expr: Expr) : UnaryOperation(expr, "+")

data class UnaryMinus(val expr: Expr) : UnaryOperation(expr, "-")

data class Real(val value: Double) : Expr {
    override fun toString(): String {
        return value.toString()
    }

    override fun eval(env: Map<String, Double>): Double {
        return value
    }
}

data class Variable(private val name: String) : Expr {
    override fun toString(): String {
        return name
    }

    override fun eval(env: Map<String, Double>): Double {
        return env[name] ?: envGlobal[name] ?: throw IllegalArgumentException("Unknown variable")
    }
}

interface LogicExpr {
    val nextExpr: LogicExpr?
    fun eval(env: Map<String, Double> = emptyMap()): Boolean
}

class Equals(
    private val first: Expr,
    private val second: Expr,
    override val nextExpr: LogicExpr? = null
) : LogicExpr {
    override fun toString(): String {
        return "($first == $second)"
    }

    override fun eval(env: Map<String, Double>): Boolean {
        var isValid = first.eval(env) == second.eval(env)
        while(nextExpr != null) {
            isValid = isValid && nextExpr.eval(env)
        }
        return isValid
    }
}

class NotEquals(
    private val first: Expr,
    private val second: Expr,
    override val nextExpr: LogicExpr? = null
) : LogicExpr {
    override fun toString(): String {
        return "($first != $second)"
    }

    override fun eval(env: Map<String, Double>): Boolean {
        var isValid = first.eval(env) != second.eval(env)
        while(nextExpr != null) {
            isValid = isValid && nextExpr.eval(env)
        }
        return isValid
    }
}

class LessThan(
    private val first: Expr,
    private val second: Expr,
    override val nextExpr: LogicExpr? = null
) : LogicExpr {
    override fun toString(): String {
        return "($first < $second)"
    }

    override fun eval(env: Map<String, Double>): Boolean {
        var isValid = first.eval(env) < second.eval(env)
        while(nextExpr != null) {
            isValid = isValid && nextExpr.eval(env)
        }
        return isValid
    }
}

class GreaterThan(
    private val first: Expr,
    private val second: Expr,
    override val nextExpr: LogicExpr? = null
) : LogicExpr {
    override fun toString(): String {
        return "($first > $second)"
    }

    override fun eval(env: Map<String, Double>): Boolean {
        var isValid = first.eval(env) > second.eval(env)
        while(nextExpr != null) {
            isValid = isValid && nextExpr.eval(env)
        }
        return isValid
    }
}

class LessThanOrEqual(
    private val first: Expr,
    private val second: Expr,
    override val nextExpr: LogicExpr? = null
) : LogicExpr {
    override fun toString(): String {
        return "($first <= $second)"
    }

    override fun eval(env: Map<String, Double>): Boolean {
        var isValid = first.eval(env) <= second.eval(env)
        while(nextExpr != null) {
            isValid = isValid && nextExpr.eval(env)
        }
        return isValid
    }
}

class GreaterThanOrEqual(
    private val first: Expr,
    private val second: Expr,
    override val nextExpr: LogicExpr? = null
) : LogicExpr {
    override fun toString(): String {
        return "($first >= $second)"
    }

    override fun eval(env: Map<String, Double>): Boolean {
        var isValid = first.eval(env) >= second.eval(env)
        while(nextExpr != null) {
            isValid = isValid && nextExpr.eval(env)
        }
        return isValid
    }
}

fun isGeometryCollection(command: ICommand): Boolean {
    var geoCommand = 0
    var i = command
    while (i !is Nil) {
        if (i !is Define && i !is If && i !is ForLoop) {
            geoCommand++
        }
        i = i.nextCommand
    }
    return geoCommand > 1
}

fun getMultipleGeometryStart(command: ICommand): String {
    return if (IS_COLLECTION || isGeometryCollection(command)) """
        {
            "type": "GeometryCollection",
            "geometries": [
        """.trimIndent() else ""
}

fun getMultipleGeometryEnd(command: ICommand): String {
    return if (IS_COLLECTION || isGeometryCollection(command)) """
            ]
        }
        """.trimIndent() else ""
}

fun getCirclePoints(center: Point, radius: Real, env: Map<String, Double>): List<Point> {
    val points = mutableListOf<Point>()
    val lat = Math.toRadians(center.lat.eval(env))
    val lon = Math.toRadians(center.lon.eval(env))
    val c = radius.value / 1000 / 6371.0  // Convert radius from meters to kilometers and calculate c
    for (i in 0..360 step 10) {
        val beta = Math.toRadians(i.toDouble())
        val newLat = asin(sin(lat) * cos(c) + cos(lat) * sin(c) * cos(beta))
        val newLon = lon + atan2(sin(beta) * sin(c) * cos(lat), cos(c) - sin(lat) * sin(newLat))
        points.add(Point(Real(Math.toDegrees(newLat)), Real(Math.toDegrees(newLon))))
    }
    points.add(
        Point(
            Real(Math.toDegrees(asin(sin(lat) * cos(c) + cos(lat) * sin(c) * cos(Math.toRadians(0.0))))),
            Real(
                Math.toDegrees(
                    lon + atan2(
                        sin(Math.toRadians(0.0)) * sin(c) * cos(lat),
                        cos(c) - sin(lat) * sin(asin(sin(lat) * cos(c) + cos(lat) * sin(c) * cos(Math.toRadians(0.0))))
                    )
                )
            )
        )
    )

    return points
}

fun getBendPoints(start: Point, end: Point, angle: Real, env: Map<String, Double>): List<Point> {
    val points = mutableListOf<Point>()
    val angleInRad = Math.toRadians(angle.value)

    // Compute the midpoint
    val midX = (start.lat.eval(env) + end.lat.eval(env)) / 2
    val midY = (start.lon.eval(env) + end.lon.eval(env)) / 2

    // Distance between start and end
    val distanceX = end.lat.eval(env) - start.lat.eval(env)
    val distanceY = end.lon.eval(env) - start.lon.eval(env)

    // Compute the control point using the angle
    val controlPoint = Point(
        Real(midX + (distanceY / 2) * cos(angleInRad) - (distanceX / 2) * sin(angleInRad)),
        Real(midY + (distanceY / 2) * sin(angleInRad) + (distanceX / 2) * cos(angleInRad))
    )

    // Compute points on the quadratic Bézier curve
    for (i in 0..100) {
        val t = i / 100.0
        val a = (1 - t) * (1 - t)
        val b = 2 * (1 - t) * t
        val c = t * t

        val x = a * start.lat.eval(env) + b * controlPoint.lat.eval(env) + c * end.lat.eval(env)
        val y = a * start.lon.eval(env) + b * controlPoint.lon.eval(env) + c * end.lon.eval(env)

        points.add(Point(Real(x), Real(y)))
    }

    return points
}