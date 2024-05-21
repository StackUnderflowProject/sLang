import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

interface ICity {
    val name: Name
    override fun toString(): String
    fun eval(env: Map<Variable, IExpression>): String
}

interface IBlock {
    val name: Name
    val type: String
    override fun toString(): String
    fun eval(env: Map<Variable, IExpression>): String
}

interface ICommand {
    val nextCommand: ICommand
    override fun toString(): String
    fun eval(env: Map<Variable, IExpression>): String
}

interface IExpression {
    override fun toString(): String
    fun eval(env: Map<Variable, IExpression>): String
}

object Nil : IBlock, ICommand, IExpression, ICity {
    override val nextCommand: ICommand = Nil
    override val name: Name = Name("")
    override val type: String = ""
    override fun toString(): String {
        return ""
    }

    override fun eval(env: Map<Variable, IExpression>): String {
        return ""
    }
}

class City(
    override val name: Name,
    val block: IBlock = Nil,
    val nextCity: ICity = Nil
) : ICity {
    override fun toString(): String {
        return """
city $name {
    $block
}
$nextCity
        """
    }

    override fun eval(env: Map<Variable, IExpression>): String {
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
    val command: ICommand = Nil,
    val nextBlock: IBlock = Nil
) : IBlock {
    override fun toString(): String {
        return """
$type $name {
    $command
};
$nextBlock
        """.trimIndent()
    }

    override fun eval(env: Map<Variable, IExpression>): String {

        val blockGeoJson = """
        {
            "type": "Feature",
            "properties": {
                "name": "$name"
            },
            "geometry": ${getMultipleGeometryStart(command.nextCommand)}      
                ${command.eval(env)}
            ${getMultipleGeometryEnd(command.nextCommand)}
        }
        """
        return if (nextBlock is Nil) blockGeoJson.trimIndent() else blockGeoJson.trimIndent() + ",\n" + nextBlock.eval(
            env
        )
    }
}

class Road(name: Name, command: ICommand, nextBlock: IBlock = Nil) : Block("road", name, command, nextBlock)

class Building(name: Name, command: ICommand, nextBlock: IBlock = Nil) : Block("building", name, command, nextBlock)

class Stadium(name: Name, command: ICommand, nextBlock: IBlock = Nil) : Block("stadium", name, command, nextBlock)

class Arena(name: Name, command: ICommand, nextBlock: IBlock = Nil) : Block("arena", name, command, nextBlock)

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

    override fun eval(env: Map<Variable, IExpression>): String {
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

    override fun eval(env: Map<Variable, IExpression>): String {
        val bendPoints = getBendPoints(start, end, angle)
        // Note: GeoJSON does not support 'Bend', but we can represent it as a LineString with additional properties
        val bendGeoJson = """
            {
                "type": "LineString",
                "coordinates": [
                    ${bendPoints.joinToString(",\n") { it.eval(env)}}   
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

    override fun eval(env: Map<Variable, IExpression>): String {
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

    override fun eval(env: Map<Variable, IExpression>): String {
        // TODO: Implement circle drawing
        val circlePoints = getCirclePoints(center, radius)
        val circleGeoJson = """
        {
            "type": "LineString",
            "coordinates": [
                ${circlePoints.joinToString(",\n") { it.eval(env) }}
            ]
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

    override fun eval(env: Map<Variable, IExpression>): String {
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

class Point(
    val lat: Real,
    val lon: Real
) : IExpression {
    override fun toString(): String {
        return "($lon, $lat)"
    }

    override fun eval(env: Map<Variable, IExpression>): String {
        return "[${lon.eval(env)}, ${lat.eval(env)}]"
    }
}

class Real(val value: Double) : IExpression {
    override fun toString(): String {
        return value.toString()
    }

    override fun eval(env: Map<Variable, IExpression>): String {
        return value.toString()
    }
}

class Variable(val name: String) : IExpression {
    override fun toString(): String {
        return name
    }

    override fun eval(env: Map<Variable, IExpression>): String {
        return env[this]?.eval(env) ?: "null"
    }
}

class Name(val value: String) : IExpression {
    override fun toString(): String {
        return value.trim('\"')
    }

    override fun eval(env: Map<Variable, IExpression>): String {
        return value.trim('\"')
    }
}

fun getMultipleGeometryStart(nextCommand: ICommand): String {
    return if (nextCommand is Nil) "" else """
        {
            "type": "GeometryCollection",
            "geometries": [
        """.trimIndent()
}

fun getMultipleGeometryEnd(nextCommand: ICommand): String {
    return if (nextCommand is Nil) "" else """
            ]
        }
        """.trimIndent()
}

fun getCirclePoints(center: Point, radius: Real): List<Point> {
    val points = mutableListOf<Point>()
    val lat = Math.toRadians(center.lat.value)
    val lon = Math.toRadians(center.lon.value)
    val c = radius.value / 1000 / 6371.0  // Convert radius from meters to kilometers and calculate c
    for (i in 0..360 step 10) {
        val beta = Math.toRadians(i.toDouble())
        val newLat = asin(sin(lat) * cos(c) + cos(lat) * sin(c) * cos(beta))
        val newLon = lon + atan2(sin(beta) * sin(c) * cos(lat), cos(c) - sin(lat) * sin(newLat))
        points.add(Point(Real(Math.toDegrees(newLat)), Real(Math.toDegrees(newLon))))
    }
    return points
}

fun getBendPoints(start: Point, end: Point, angle: Real): List<Point> {
    val points = mutableListOf<Point>()
    val angleInRad = Math.toRadians(angle.value)

    // Compute the midpoint
    val midX = (start.lat.value + end.lat.value) / 2
    val midY = (start.lon.value + end.lon.value) / 2

    // Distance between start and end
    val distanceX = end.lat.value - start.lat.value
    val distanceY = end.lon.value - start.lon.value

    // Compute the control point using the angle
    val controlPoint = Point(
        Real(midX + (distanceY / 2) * cos(angleInRad) - (distanceX / 2) * sin(angleInRad)),
        Real(midY + (distanceY / 2) * sin(angleInRad) + (distanceX / 2) * cos(angleInRad))
    )

    // Compute points on the quadratic Bezier curve
    for (i in 0..100) {
        val t = i / 100.0
        val a = (1 - t) * (1 - t)
        val b = 2 * (1 - t) * t
        val c = t * t

        val x = a * start.lat.value + b * controlPoint.lat.value + c * end.lat.value
        val y = a * start.lon.value + b * controlPoint.lon.value + c * end.lon.value

        points.add(Point(Real(x), Real(y)))
    }

    return points
}