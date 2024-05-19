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

    private fun parseName(): String {
        val name = currentToken?.lexeme
        expect(Symbol.NAME)
        return name!!
    }

    private fun parseBlocks(): IBlock {
        if(currentToken?.symbol == Symbol.END) {
            return Nil()
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
            is Stadium -> Stadium(first.name, first.command, second)
            is Arena -> Arena(first.name, first.command, second)
            else -> Nil()
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
        val name = parseName()
        expect(Symbol.BEGIN)
        val command = parseCommands()
        expect(Symbol.END)
        expect(Symbol.TERM)
        return Stadium(name, command)
    }

    private fun parseArena(): Arena {
        expect(Symbol.ARENA)
        val name = parseName()
        expect(Symbol.BEGIN)
        val command = parseCommands()
        expect(Symbol.END)
        expect(Symbol.TERM)
        return Arena(name, command)
    }

    private fun parseCommands(): ICommand {
        val command = parseCommand()
        return if (currentToken?.symbol == Symbol.END) {
            command
        } else {
            val nextCommand = parseCommands()
            combineCommands(command, nextCommand)
        }
    }

    private fun parseCommand(): ICommand {
        return when (currentToken?.symbol) {
            Symbol.LINE -> parseLine()
            Symbol.CIRCLE -> parseCircle()
            Symbol.BOX -> parseBox()
            Symbol.BEND -> parseBend()
            else -> throw IllegalArgumentException("Expected command, but got ${currentToken?.symbol}")
        }
    }

    private fun combineCommands(first: ICommand, second: ICommand): ICommand {
        return when (first) {
            is Line -> Line(first.start, first.end, second)
            is Box -> Box(first.start, first.end, second)
            is Circle -> Circle(first.center, first.radius, second)
            is Bend -> Bend(first.start, first.end, first.angle, second)
            else -> Nil()
        }
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
        val radius = parseReal()
        expect(Symbol.RPAREN)
        return Circle(center, radius)
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
        val angle = parseReal()
        expect(Symbol.RPAREN)
        return Bend(from, to, angle)
    }

    private fun parsePoint(): Point {
        expect(Symbol.LPAREN)
        val x = parseReal()
        expect(Symbol.TO)
        val y = parseReal()
        expect(Symbol.RPAREN)
        return Point(x, y)
    }

    private fun parseReal(): Real {
        val real = currentToken?.lexeme?.toDouble()?.let { Real(it) }
        expect(Symbol.REAL)
        return real!!
    }
}


fun main() {
    val result = Parser(
        Scanner(Automaton,"""
            city "Maribor" {
                stadium "Ljudski vrt" {
                    box((0, 0), (10.5, 10.5))
                    line((10.5, 10.5), (20, 20))
                };
                road "Ulica heroja Staneta" {
                    line((0, 0), (10, 10))
                    line((10, 10), (20, 20))
                };
            }
        """.trimIndent().byteInputStream())
    ).parse()
    println(result)
}