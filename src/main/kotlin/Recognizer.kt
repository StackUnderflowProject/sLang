class Recognizer(private val scanner: Scanner) {
    private var last: Token? = null

    fun recognizeStart(): Boolean {
        last = scanner.getToken()
        val result = recognizeCities()
        return when (last?.symbol) {
            Symbol.EOF -> result
            else -> false
        }
    }

    private fun recognizeTerminal(symbol: Symbol): Boolean {
        val res = last?.symbol == symbol
        last = scanner.getToken()
        return res
    }

    private fun recognizeCities(): Boolean {
        return when (last?.symbol) {
            Symbol.CITY -> {
                recognizeTerminal(Symbol.CITY) && recognizeName()
                        && recognizeTerminal(Symbol.BEGIN) && recognizeBlocks()
                        && recognizeTerminal(Symbol.END) && recognizeCities()
            }

            Symbol.EOF -> true
            else -> false
        }
    }

    private fun recognizeName(): Boolean {
        return when (last?.symbol) {
            Symbol.NAME -> {
                recognizeTerminal(Symbol.NAME)
            }

            else -> false
        }
    }

    private fun recognizeBlocks(): Boolean {
        return when (last?.symbol) {
            Symbol.DEFINE -> {
                recognizeTerminal(Symbol.DEFINE) && recognizeTerminal(Symbol.VARIABLE)
                        && recognizeTerminal(Symbol.ASSIGN) && recognizeExpression()
                        && recognizeTerminal(Symbol.TERM) && recognizeBlocks()
            }

            Symbol.ROAD, Symbol.BUILDING, Symbol.STADIUM, Symbol.ARENA -> {
                recognizeNamedBlock() && recognizeName() && recognizeTerminal(Symbol.BEGIN)
                        && recognizeCommands() && recognizeTerminal(Symbol.END)
                        && recognizeTerminal(Symbol.TERM)
                        && recognizeBlocks()
            }

            Symbol.END -> true
            else -> false
        }
    }

    private fun recognizeNamedBlock(): Boolean {
        return when (last?.symbol) {
            Symbol.ROAD -> recognizeTerminal(Symbol.ROAD)
            Symbol.BUILDING -> recognizeTerminal(Symbol.BUILDING)
            Symbol.STADIUM -> recognizeTerminal(Symbol.STADIUM)
            Symbol.ARENA -> recognizeTerminal(Symbol.ARENA)
            else -> false
        }
    }

    private fun recognizeCommands(): Boolean {
        return when (last?.symbol) {
            Symbol.DEFINE -> {
                recognizeTerminal(Symbol.DEFINE) && recognizeTerminal(Symbol.VARIABLE)
                        && recognizeTerminal(Symbol.ASSIGN) && recognizeExpression()
                        && recognizeTerminal(Symbol.TERM) && recognizeCommands()
            }

            Symbol.LINE -> {
                recognizeTerminal(Symbol.LINE) && recognizeTerminal(Symbol.LPAREN)
                        && recognizePoint() && recognizeTerminal(Symbol.TO)
                        && recognizePoint() && recognizeTerminal(Symbol.RPAREN)
                        && recognizeCommands()
            }

            Symbol.BEND -> {
                recognizeTerminal(Symbol.BEND) && recognizeTerminal(Symbol.LPAREN)
                        && recognizePoint() && recognizeTerminal(Symbol.TO)
                        && recognizePoint() && recognizeTerminal(Symbol.TO)
                        && recognizeTerminal(Symbol.REAL) && recognizeTerminal(Symbol.RPAREN)
                        && recognizeCommands()
            }

            Symbol.BOX -> {
                recognizeTerminal(Symbol.BOX) && recognizeTerminal(Symbol.LPAREN)
                        && recognizePoint() && recognizeTerminal(Symbol.TO)
                        && recognizePoint() && recognizeTerminal(Symbol.RPAREN)
                        && recognizeCommands()
            }

            Symbol.CIRCLE -> {
                recognizeTerminal(Symbol.CIRCLE) && recognizeTerminal(Symbol.LPAREN)
                        && recognizePoint() && recognizeTerminal(Symbol.TO)
                        && recognizeTerminal(Symbol.REAL) && recognizeTerminal(Symbol.RPAREN)
                        && recognizeCommands()
            }
            
            Symbol.RECT -> {
                recognizeTerminal(Symbol.RECT) && recognizeTerminal(Symbol.LPAREN)
                        && recognizePoint() && recognizeTerminal(Symbol.TO)
                        && recognizePoint() && recognizeTerminal(Symbol.TO)
                        && recognizePoint() && recognizeTerminal(Symbol.TO)
                        && recognizePoint() && recognizeTerminal(Symbol.RPAREN)
                        && recognizeCommands()
            }

            Symbol.END -> true
            else -> false
        }
    }

    private fun recognizePoint(): Boolean {
        return when (last?.symbol) {
            Symbol.LPAREN -> {
                recognizeTerminal(Symbol.LPAREN) && recognizeTerminal(Symbol.REAL) && recognizeTerminal(Symbol.TO)
                        && recognizeTerminal(Symbol.REAL) && recognizeTerminal(Symbol.RPAREN)
            }

            else -> false
        }
    }

    private fun recognizeExpression(): Boolean {
        return when (last?.symbol) {
            Symbol.REAL -> {
                recognizeTerminal(Symbol.REAL)
            }

            Symbol.VARIABLE -> {
                recognizeTerminal(Symbol.VARIABLE)
            }

            else -> false
        }
    }
}

fun main() {
    println(
        Recognizer(Scanner(Automaton, """
            city "Maribor" {
                var x = 1.0;
                var y = 1.0;
                stadium "Ljudski vrt" {
                    var z = 2.0;
                    line((0.0, 0.0), (1.0, 1.0))
                };
                var w = 3.0;
            }
        """.trimIndent().byteInputStream())).recognizeStart()
    )
}