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
                [${start.lng.eval(env)}, ${start.lat.eval(env)}],
                [${end.lng.eval(env)}, ${end.lat.eval(env)}]
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
        // Note: GeoJSON does not support 'Bend', but we can represent it as a LineString with additional properties
        val bendGeoJson = """
            {
                "type": "LineString",
                "coordinates": [
                    [${start.lng.eval(env)}, ${start.lat.eval(env)}],
                    [${end.lng.eval(env)}, ${end.lat.eval(env)}]
                ],
                "properties": {
                    "bend_angle": ${angle.eval(env)}
                }
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
                    [${start.lng.eval(env)}, ${start.lat.eval(env)}],
                    [${end.lng.eval(env)}, ${start.lat.eval(env)}],
                    [${end.lng.eval(env)}, ${end.lat.eval(env)}],
                    [${start.lng.eval(env)}, ${end.lat.eval(env)}],
                    [${start.lng.eval(env)}, ${start.lat.eval(env)}]
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
        val circleGeoJson = """
        {
            "type": "Point",
            "coordinates": [
                ${center.lng.eval(env)}, 
                ${center.lat.eval(env)}
            ],       
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
                [${bottomLeft.lng.eval(env)}, ${bottomLeft.lat.eval(env)}],
                [${bottomRight.lng.eval(env)}, ${bottomRight.lat.eval(env)}],
                [${topRight.lng.eval(env)}, ${topRight.lat.eval(env)}],
                [${topLeft.lng.eval(env)}, ${topLeft.lat.eval(env)}],
                [${bottomLeft.lng.eval(env)}, ${bottomLeft.lat.eval(env)}]
            ]]
        }
        """
        return if (nextCommand is Nil) boxGeoJson.trimIndent() else boxGeoJson.trimIndent() +
                ",\n" + nextCommand.eval(env)
    }
}

class Point(
    val lat: IExpression,
    val lng: IExpression
) : IExpression {
    override fun toString(): String {
        return "($lng, $lat)"
    }

    override fun eval(env: Map<Variable, IExpression>): String {
        return "[${lng.eval(env)}, ${lat.eval(env)}]"
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

fun main() {
    println(
        City(
            Name("\"Maribor\""),
            Stadium(
                Name("\"Ljudski vrt\""),
                Rect(
                    Point(Real(0.0), Real(0.0)),
                    Point(Real(1.0), Real(0.0)),
                    Point(Real(1.0), Real(1.0)),
                    Point(Real(0.0), Real(1.0))
                )
            ),
        ).eval(emptyMap())
    )
}