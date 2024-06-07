import java.io.File

class Parser(private val scanner: Scanner) {
    private var currentToken: Token? = null

    fun parse(): ICity {
        currentToken = scanner.getToken()
        val result = parseCity()
        expect(Symbol.EOF)
        return result
    }

    private fun expect(symbol: Symbol) {
        if (currentToken?.symbol != symbol) {
            throw IllegalArgumentException("Expected $symbol, but got ${currentToken?.symbol}")
        }
        currentToken = scanner.getToken()
    }

    private fun parseCity(): ICity {
        expect(Symbol.CITY)
        val name = parseName()
        expect(Symbol.BEGIN)
        val block = parseBlocks()
        expect(Symbol.END)
        return City(name, block)
    }

    private fun parseName(): Name {
        val name = Name(currentToken?.lexeme ?: "")
        expect(Symbol.NAME)
        return name
    }

    private fun parseBlocks(): IBlock {
        if (currentToken?.symbol == Symbol.END) {
            return Nil
        }
        val block = parseBlock()
        return if (currentToken?.symbol == Symbol.END) {
            block
        } else {
            val nextBlock = parseBlocks()
            combineBlocks(block, nextBlock)
        }
    }

    private fun parseBlock(): IBlock {
        return when (currentToken?.symbol) {
            Symbol.ROAD -> parseRoad()
            Symbol.BUILDING -> parseBuilding()
            Symbol.STADIUM -> parseStadium()
            Symbol.ARENA -> parseArena()
            else -> throw IllegalArgumentException("Expected block, but got ${currentToken?.symbol}")
        }
    }

    private fun combineBlocks(first: IBlock, second: IBlock): IBlock {
        return when (first) {
            is Road -> Road(first.name, first.command, second)
            is Building -> Building(first.name, first.command, second)
            is Stadium -> Stadium(
                first.name,
                first.command,
                club = first.club,
                capacity = first.capacity,
                nextBlock = second
            )

            is Arena -> Arena(
                first.name,
                first.command,
                club = first.club,
                capacity = first.capacity,
                nextBlock = second
            )

            else -> Nil
        }
    }

    private fun parseRoad(): Road {
        expect(Symbol.ROAD)
        val name = parseName()
        expect(Symbol.BEGIN)
        val command = parseCommands()
        expect(Symbol.END)
        expect(Symbol.TERM)
        return Road(name, command)
    }

    private fun parseBuilding(): Building {
        expect(Symbol.BUILDING)
        val name = parseName()
        expect(Symbol.BEGIN)
        val command = parseCommands()
        expect(Symbol.END)
        expect(Symbol.TERM)
        return Building(name, command)
    }

    private fun parseStadium(): Stadium {
        expect(Symbol.STADIUM)
        val metadata = parseMetadata()
        expect(Symbol.BEGIN)
        val command = parseCommands()
        expect(Symbol.END)
        expect(Symbol.TERM)
        return Stadium(metadata.name, command, club = metadata.club, capacity = metadata.capacity)
    }

    data class Metadata(val name: Name, var club: Name, var capacity: UInt)

    private fun parseMetadata(): Metadata {
        val metadata = Metadata(parseName(), Name(""), 0u)
        if (currentToken?.symbol == Symbol.NAME) {
            metadata.club = parseName()
        }
        if (currentToken?.symbol == Symbol.REAL) {
            metadata.capacity = parseCapacity()
        }
        return metadata
    }

    private fun parseCapacity(): UInt {
        val capacity = currentToken?.lexeme?.toUIntOrNull()
            ?: throw IllegalArgumentException("Invalid capacity: ${currentToken?.lexeme}")
        expect(Symbol.REAL)
        return capacity
    }

    private fun parseArena(): Arena {
        expect(Symbol.ARENA)
        val metadata = parseMetadata()
        expect(Symbol.BEGIN)
        val command = parseCommands()
        expect(Symbol.END)
        expect(Symbol.TERM)
        return Arena(metadata.name, command, club = metadata.club, capacity = metadata.capacity)
    }

    private fun parseCommands(): ICommand {
        val command = parseCommand()
        return if (currentToken?.symbol == Symbol.END) {
            command
        } else {
            val nextCommand = parseCommands()
            combineCommands(if (command.nextCommand is Nil) command else command.nextCommand, nextCommand)
        }
    }

    private fun parseCommand(): ICommand {
        return when (currentToken?.symbol) {
            Symbol.FOR -> parseForLoop()
            Symbol.DEFINE -> parseVariableAssigment()
            Symbol.LINE -> parseLine()
            Symbol.CIRCLE -> parseCircle()
            Symbol.BOX -> parseBox()
            Symbol.BEND -> parseBend()
            Symbol.RECT -> parseRect()
            else -> throw IllegalArgumentException("Expected command, but got ${currentToken?.symbol}")
        }
    }

    private fun combineCommands(first: ICommand, second: ICommand): ICommand {
        return when (first) {
            is Line -> Line(first.start, first.end, second)
            is Box -> Box(first.start, first.end, second)
            is Circle -> Circle(first.center, first.radius, second)
            is Bend -> Bend(first.start, first.end, first.angle, second)
            is Rect -> Rect(first.bottomLeft, first.bottomRight, first.topRight, first.topLeft, second)
            is Define -> Define(first.name, first.value, second)
            is ForLoop -> ForLoop(Define(first.define.name, first.define.value), first.end, first.body, second)
            else -> Nil
        }
    }

    private fun parseForLoop(): ICommand {
        expect(Symbol.FOR)
        expect(Symbol.LPAREN)
        expect(Symbol.DEFINE)
        val variableName = parseVariable()
        expect(Symbol.ASSIGN)
        val start = parseAdditive()
        expect(Symbol.TO)
        val end = parseAdditive()
        expect(Symbol.RPAREN)
        expect(Symbol.BEGIN)
        val body = parseCommands()
        expect(Symbol.END)
        return ForLoop(Define(variableName, start), end, body)
    }

    private fun parseVariableAssigment(): ICommand {
        expect(Symbol.DEFINE)
        val variable = parseVariable()
        expect(Symbol.ASSIGN)
        val expression = parseAdditive()
        expect(Symbol.TERM)
        return Define(variable, expression)
    }

    private fun parseVariable(): String {
        val variable = currentToken?.lexeme ?: ""
        expect(Symbol.VARIABLE)
        return variable
    }


    private fun parseLine(): ICommand {
        expect(Symbol.LINE)
        expect(Symbol.LPAREN)
        val from = parsePoint()
        expect(Symbol.TO)
        val to = parsePoint()
        expect(Symbol.RPAREN)
        return Line(from, to)
    }

    private fun parseCircle(): ICommand {
        expect(Symbol.CIRCLE)
        expect(Symbol.LPAREN)
        val center = parsePoint()
        expect(Symbol.TO)
        val radius = parseAdditive()
        expect(Symbol.RPAREN)
        return Circle(center, radius as Real)
    }

    private fun parseBox(): ICommand {
        expect(Symbol.BOX)
        expect(Symbol.LPAREN)
        val from = parsePoint()
        expect(Symbol.TO)
        val to = parsePoint()
        expect(Symbol.RPAREN)
        return Box(from, to)
    }

    private fun parseBend(): ICommand {
        expect(Symbol.BEND)
        expect(Symbol.LPAREN)
        val from = parsePoint()
        expect(Symbol.TO)
        val to = parsePoint()
        expect(Symbol.TO)
        val angle = parseAdditive()
        expect(Symbol.RPAREN)
        return Bend(from, to, angle as Real)
    }

    private fun parseRect(): ICommand {
        expect(Symbol.RECT)
        expect(Symbol.LPAREN)
        val bottomLeft = parsePoint()
        expect(Symbol.TO)
        val bottomRight = parsePoint()
        expect(Symbol.TO)
        val topRight = parsePoint()
        expect(Symbol.TO)
        val topLeft = parsePoint()
        expect(Symbol.RPAREN)
        return Rect(bottomLeft, bottomRight, topRight, topLeft)
    }

    private fun parsePoint(): Point {
        expect(Symbol.LPAREN)
        val x = parseAdditive()
        expect(Symbol.TO)
        val y = parseAdditive()
        expect(Symbol.RPAREN)
        return Point(x, y)
    }

    private fun parseAdditive(): Expr {
        var left = parseMultiplicative()
        while (currentToken?.symbol in setOf(Symbol.PLUS, Symbol.MINUS)) {
            val operator = currentToken!!.symbol
            currentToken = scanner.getToken() // Consume the operator token
            val right = parseMultiplicative()
            left = when (operator) {
                Symbol.PLUS -> Plus(left, right)
                Symbol.MINUS -> Minus(left, right)
                else -> throw IllegalArgumentException("Unexpected operator: $operator")
            }
        }
        return left
    }

    private fun parseMultiplicative(): Expr {
        var left = parseExponential()
        while (currentToken?.symbol in setOf(Symbol.TIMES, Symbol.DIVIDES, Symbol.INTEGER_DIVIDES)) {
            val operator = currentToken!!.symbol
            currentToken = scanner.getToken() // Consume the operator token
            val right = parseExponential()
            left = when (operator) {
                Symbol.TIMES -> Times(left, right)
                Symbol.DIVIDES -> Divides(left, right)
                Symbol.INTEGER_DIVIDES -> IntegerDivides(left, right)
                else -> throw IllegalArgumentException("Unexpected operator: $operator")
            }
        }
        return left
    }

    private fun parseExponential(): Expr {
        var left = parseUnary()
        while (currentToken?.symbol == Symbol.POW) {
            currentToken = scanner.getToken() // Consume the POW token
            val right = parseUnary()
            left = buildRightAssociativePow(left, right)
        }
        return left
    }

    private fun buildRightAssociativePow(left: Expr, right: Expr): Expr {
        if (currentToken?.symbol == Symbol.POW) {
            currentToken = scanner.getToken() // Consume the POW token
            val nextRight = parseUnary() // Parse the next operand
            val newLeft = buildRightAssociativePow(right, nextRight)
            return Pow(left, newLeft)
        }
        return Pow(left, right)
    }

    private fun parseUnary(): Expr {
        return when (currentToken?.symbol) {
            Symbol.PLUS -> {
                currentToken = scanner.getToken() // Consume the PLUS token
                UnaryPlus(parsePrimary())
            }

            Symbol.MINUS -> {
                currentToken = scanner.getToken() // Consume the MINUS token
                UnaryMinus(parsePrimary())
            }

            else -> parsePrimary()
        }
    }

    private fun parsePrimary(): Expr {
        return when (currentToken?.symbol) {
            Symbol.REAL -> {
                val value = currentToken!!.lexeme.toDoubleOrNull()
                    ?: throw IllegalArgumentException("Invalid real number: ${currentToken?.lexeme}")
                currentToken = scanner.getToken() // Consume the REAL token
                Real(value)
            }

            Symbol.VARIABLE -> {
                val name = currentToken!!.lexeme
                currentToken = scanner.getToken() // Consume the VARIABLE token
                Variable(name)
            }

            Symbol.LPAREN -> {
                currentToken = scanner.getToken() // Consume the LPAREN token
                val expr = parseAdditive()
                expect(Symbol.RPAREN) // Expect RPAREN to close the expression
                expr
            }

            else -> throw IllegalArgumentException("Unexpected token: ${currentToken?.symbol}")
        }
    }
}


fun main() {
    val geojson = Parser(Scanner(Automaton, File("src/test.txt").inputStream())).parse().eval(emptyMap())
    File("src/test.json").writeText(geojson)
}