interface ICity {
    val name: String
    override fun toString(): String
}

interface IBlock {
    val name: String
    val type: String
    override fun toString(): String
}

interface ICommand {
    override fun toString(): String
}

interface IExpression {
    override fun toString(): String
}

class Nil : IBlock, ICommand, IExpression, ICity {
    override val name: String = ""
    override val type: String = ""
    override fun toString(): String {
        return ""
    }
}

class City(
    override val name: String,
    val block: IBlock = Nil(),
    val nextCity: ICity = Nil()
) : ICity {
    override fun toString(): String {
        return """
city $name {
    $block
}
$nextCity
        """
    }
}

abstract class Block(
    override val type: String,
    override val name: String,
    val command: ICommand = Nil(),
    val nextBlock: IBlock = Nil()
) : IBlock {
    override fun toString(): String {
        return """
$type $name {
    $command
};
$nextBlock
        """.trimIndent()
    }
}

class Road(name: String, command: ICommand, nextBlock: IBlock = Nil()) : Block("road", name, command, nextBlock)

class Building(name: String, command: ICommand, nextBlock: IBlock = Nil()) : Block("building", name, command, nextBlock)

class Stadium(name: String, command: ICommand, nextBlock: IBlock = Nil()) : Block("stadium", name, command, nextBlock)

class Arena(name: String, command: ICommand, nextBlock: IBlock = Nil()) : Block("arena", name, command, nextBlock)

class Line(
    val start: Point,
    val end: Point,
    val nextCommand: ICommand = Nil()
) : ICommand {
    override fun toString(): String {
        return """
line ($start, $end)
$nextCommand
        """.trimIndent()
    }
}

class Bend(
    val start: Point,
    val end: Point,
    val angle: Real,
    val nextCommand: ICommand = Nil()
) : ICommand {
    override fun toString(): String {
        return """
bend ($start, $end, $angle)
$nextCommand
        """.trimIndent()
    }
}

class Box(
    val start: Point,
    val end: Point,
    val nextCommand: ICommand = Nil()
) : ICommand {
    override fun toString(): String {
        return """
box ($start, $end)
$nextCommand
        """.trimIndent()
    }
}

class Circle(
    val center: Point,
    val radius: Real,
    val nextCommand: ICommand = Nil()
) : ICommand {
    override fun toString(): String {
        return """
            circle ($center, $radius)
            $nextCommand
        """.trimIndent()
    }
}

class Point(
    val x: IExpression,
    val y: IExpression
) : ICommand {
    override fun toString(): String {
        return "($x, $y)"
    }
}

class Real(val value: Double) : IExpression {
    override fun toString(): String {
        return value.toString()
    }
}

class Variable(val name: String) : IExpression {
    override fun toString(): String {
        return name
    }
}