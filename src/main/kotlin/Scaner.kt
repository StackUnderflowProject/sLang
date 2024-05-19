import java.io.InputStream
import java.io.OutputStream

const val ERROR_STATE = 0

enum class Symbol {
    EOF,
    REAL,
    VARIABLE,
    PLUS,
    MINUS,
    TIMES,
    DIVIDES,
    INTEGER_DIVIDES,
    POW,
    LPAREN,
    RPAREN,
    SKIP,
    ASSIGN,
    DEFINE,
    TERM,
    FOR,
    TO,
    BEGIN,
    END,
    PRINT,
    CITY,
    NAME,
    ROAD,
    BUILDING,
    STADIUM,
    ARENA,
    LINE,
    BEND,
    BOX,
    CIRCLE,
    EQUALS,
    NOT_EQUALS,
    LESS_THAN,
    MORE_THAN,
    LESS_OR_EQUALS,
    MORE_OR_EQUALS,
    DEF,
    OPEN_BRACKET,
    CLOSE_BRACKET,
    NIL
}

const val EOF = -1
const val NEWLINE = '\n'.code

interface DFA {
    val states: Set<Int>
    val alphabet: IntRange
    fun next(state: Int, code: Int): Int
    fun symbol(state: Int): Symbol
    val startState: Int
    val finalStates: Set<Int>
}

object Automaton : DFA {
    override val states = (1..91).toSet()
    override val alphabet = 0..255
    override val startState = 1

    override val finalStates = (2..29).asSequence()
        .plus(31..32)
        .plus(33..36)
        .plus(39..77)
        .plus(79..91).toSet()

    private val numberOfStates = states.max() + 1 // plus the task.ERROR_STATE
    private val numberOfCodes = alphabet.max() + 1 // plus the task.EOF
    private val transitions = Array(numberOfStates) { IntArray(numberOfCodes) }
    private val values = Array(numberOfStates) { Symbol.SKIP }

    private fun setTransition(from: Int, range: CharRange, to: Int) {
        range.forEach {
            setTransition(from, it, to)
        }
    }

    private fun setTransition(from: Int, set: Set<Char>, to: Int) {
        set.forEach {
            if(this.transitions[from][it.code + 1] == 0)
                setTransition(from, it, to)
        }
    }

    private fun setTransition(from: Int, chr: Char, to: Int) {
        transitions[from][chr.code + 1] = to // + 1 because task.EOF is -1 and the array starts at 0
    }

    private fun setTransition(from: Int, code: Int, to: Int) {
        transitions[from][code + 1] = to
    }

    private fun setSymbol(state: Int, symbol: Symbol) {
        values[state] = symbol
    }

    override fun next(state: Int, code: Int): Int {
        return transitions[state][code + 1]
    }

    override fun symbol(state: Int): Symbol {
        return values[state]
    }

    private fun selectedTransition(from: Int, c: Char, to: Int) {
        setTransition(from, c, to)
        setTransition(from, chars.minus(c), 5)
        setTransition(from, digits, 6)
    }

    private val digits = '0'..'9'
    private val chars = ('a'..'z').plus('A'..'Z').toSet()
    private val skip = setOf(' ', '\t', '\n', '\r')
    private val keywordFirst = setOf('v', 'f', 'p', 'c', 'r', 'b', 's', 'a', 'l', 'd', 'n')

    private fun setVariable(from: Int, to: Int){
        for(i in from..to) {
            setSymbol(i, Symbol.VARIABLE)
        }
    }

    init {
        // var
        setTransition(1, 'v', 2)
        selectedTransition(2, 'a', 3)
        selectedTransition(3, 'r', 4)

        // variable
        setTransition(1, chars.minus(keywordFirst), 5)
        setTransition(4, chars, 5)
        setTransition(4, digits, 6)
        setTransition(5, chars, 5)
        setTransition(5, digits, 6)

        // for
        setTransition(1, 'f', 7)
        selectedTransition(7, 'o', 8)
        selectedTransition(8, 'r', 9)

        setTransition(9, chars, 5)
        setTransition(9, digits, 6)

        // print
        setTransition(1, 'p', 10)
        selectedTransition(10, 'r', 11)
        selectedTransition(11, 'i', 12)
        selectedTransition(12, 'n', 13)
        selectedTransition(13, 't', 14)

        setTransition(14, chars, 5)
        setTransition(14, digits, 6)
        
        // plus
        setTransition(1, '+', 15)

        // minus
        setTransition(1, '-', 16)

        // times
        setTransition(1, '*', 17)

        // divides
        setTransition(1, '/', 18)

        // integer-divides
        setTransition(18, '/', 19)

        // pow
        setTransition(1, '^', 20)

        // lparan
        setTransition(1, '(', 21)

        // rparan
        setTransition(1, ')', 22)
        
        // skip
        setTransition(1, skip, 23)
        setTransition(23, skip, 23)
        
        // assign
        setTransition(1, '=', 24)

        // term
        setTransition(1, ';', 25)
        
        // to
        setTransition(1, ',', 26)
        
        // begin
        setTransition(1, '{', 27)
        
        // end
        setTransition(1, '}', 28)

        // equals


        // real
        setTransition(1, digits, 29)
        setTransition(29, digits, 29)
        setTransition(29, '.', 30)
        setTransition(30, digits, 31)
        setTransition(31, digits, 31)

        // eof
        setTransition(1, EOF, 32)

        // city
        setTransition(1, 'c', 33)
        selectedTransition(33, 'i', 34)
        selectedTransition(34, 't', 35)
        selectedTransition(35, 'y', 36)

        // name
        setTransition(1, '\"', 37)
        setTransition(37, chars, 38)
        setTransition(38, ' ', 38)
        setTransition(38, chars, 38)
        setTransition(38, '\"', 39)

        // road
        setTransition(1, 'r', 40)
        selectedTransition(40, 'o', 41)
        selectedTransition(41, 'a', 42)
        selectedTransition(42, 'd', 43)

        // building
        setTransition(1, 'b', 44)
        selectedTransition(44, 'u', 45)
        selectedTransition(45, 'i', 46)
        selectedTransition(46, 'l', 47)
        selectedTransition(47, 'd', 48)
        selectedTransition(48, 'i', 49)
        selectedTransition(49, 'n', 50)
        selectedTransition(50, 'g', 51)

        // stadium
        setTransition(1, 's', 52)
        selectedTransition(52, 't', 53)
        selectedTransition(53, 'a', 54)
        selectedTransition(54, 'd', 55)
        selectedTransition(55, 'i', 56)
        selectedTransition(56, 'u', 57)
        selectedTransition(57, 'm', 58)

        // arena
        setTransition(1, 'a', 59)
        selectedTransition(59, 'r', 60)
        selectedTransition(60, 'e', 61)
        selectedTransition(61, 'n', 62)
        selectedTransition(62, 'a', 63)

        // line
        setTransition(1, 'l', 64)
        selectedTransition(64, 'i', 65)
        selectedTransition(65, 'n', 66)
        selectedTransition(66, 'e', 67)

        // bend
        // setTransition(1, 'b', 44)
        selectedTransition(44, 'e', 68)
        selectedTransition(68, 'n', 69)
        selectedTransition(69, 'd', 70)

        // box
        selectedTransition(44, 'o', 71)
        selectedTransition(71, 'x', 72)

        // circle
        selectedTransition(34, 'r', 73)
        selectedTransition(73, 'c', 74)
        selectedTransition(74, 'l', 75)
        selectedTransition(75, 'e', 76)

        // equals
        setTransition(24, '=', 77)

        // not equals
        setTransition(1, '!', 78)
        setTransition(78, '=', 79)

        // less than
        setTransition(1, '<', 80)

        // more than
        setTransition(1, '>', 81)

        // less or equals
        setTransition(80, '=', 82)

        // more or equals
        setTransition(81, '=', 83)

        // def
        setTransition(1, 'd', 84)
        selectedTransition(84, 'e', 85)
        selectedTransition(85, 'f', 86)

        // open bracket
        setTransition(1, '[', 87)

        // close bracket
        setTransition(1, ']', 88)

        // nil
        setTransition(1, 'n', 89)
        selectedTransition(89, 'i', 90)
        selectedTransition(90, 'l', 91)

        setVariable(2,3)
        setSymbol(4, Symbol.DEFINE)

        setVariable(5,8)
        setSymbol(9, Symbol.FOR)

        setVariable(10, 13)
        setSymbol(14, Symbol.PRINT)

        setSymbol(15, Symbol.PLUS)
        setSymbol(16, Symbol.MINUS)
        setSymbol(17, Symbol.TIMES)
        setSymbol(18, Symbol.DIVIDES)
        setSymbol(19, Symbol.INTEGER_DIVIDES)
        setSymbol(20, Symbol.POW)
        setSymbol(21, Symbol.LPAREN)
        setSymbol(22, Symbol.RPAREN)
        setSymbol(23, Symbol.SKIP)
        setSymbol(24, Symbol.ASSIGN)
        setSymbol(25, Symbol.TERM)
        setSymbol(26, Symbol.TO)
        setSymbol(27, Symbol.BEGIN)
        setSymbol(28, Symbol.END)
        setSymbol(29, Symbol.REAL)
        setSymbol(31, Symbol.REAL)
        setSymbol(32, Symbol.EOF)

        setVariable(33, 35)
        setSymbol(36, Symbol.CITY)
        setSymbol(39, Symbol.NAME)

        setVariable(40, 42)
        setSymbol(43, Symbol.ROAD)

        setVariable(44, 50)
        setSymbol(51, Symbol.BUILDING)

        setVariable(52, 57)
        setSymbol(58, Symbol.STADIUM)

        setVariable(59, 62)
        setSymbol(63, Symbol.ARENA)

        setVariable(64, 66)
        setSymbol(67, Symbol.LINE)

        setVariable(68, 69)
        setSymbol(70, Symbol.BEND)

        setSymbol(71, Symbol.VARIABLE)
        setSymbol(72, Symbol.BOX)

        setVariable(73, 75)
        setSymbol(76, Symbol.CIRCLE)

        setSymbol(77, Symbol.EQUALS)

        setSymbol(79, Symbol.NOT_EQUALS)

        setSymbol(80, Symbol.LESS_THAN)

        setSymbol(81, Symbol.MORE_THAN)

        setSymbol(82, Symbol.LESS_OR_EQUALS)

        setSymbol(83, Symbol.MORE_OR_EQUALS)

        setVariable(84, 85)
        setSymbol(86, Symbol.DEF)

        setSymbol(87, Symbol.OPEN_BRACKET)

        setSymbol(88, Symbol.CLOSE_BRACKET)

        setVariable(89, 90)
        setSymbol(91, Symbol.NIL)
    }
}

data class Token(val symbol: Symbol, val lexeme: String, val startRow: Int, val startColumn: Int)

class Scanner(private val automaton: DFA, private val stream: InputStream) {
    private var last: Int? = null
    private var row = 1
    private var column = 1

    private fun updatePosition(code: Int) {
        if (code == NEWLINE) {
            row += 1
            column = 1
        } else {
            column += 1
        }
    }

    fun getToken(): Token {
        val startRow = row
        val startColumn = column
        val buffer = mutableListOf<Char>()

        var code = last ?: stream.read()
        var state = automaton.startState
        while (true) {
            val nextState = automaton.next(state, code)
            if (nextState == ERROR_STATE) break // Longest match

            state = nextState
            updatePosition(code)
            buffer.add(code.toChar())
            code = stream.read()
        }
        last = code // The code following the current lexeme is the first code of the next lexeme

        if (automaton.finalStates.contains(state)) {
            val symbol = automaton.symbol(state)
            return if (symbol == Symbol.SKIP) {
                getToken()
            } else {
                val lexeme = String(buffer.toCharArray())
                Token(symbol, lexeme, startRow, startColumn)
            }
        } else {
            throw Error("Invalid pattern at ${row}:${column}")
        }
    }
}

fun name(symbol: Symbol) =
    when (symbol) {
        Symbol.EOF -> "eof"
        Symbol.REAL -> "real"
        Symbol.VARIABLE -> "variable"
        Symbol.PLUS -> "plus"
        Symbol.MINUS -> "minus"
        Symbol.TIMES -> "times"
        Symbol.DIVIDES -> "divides"
        Symbol.INTEGER_DIVIDES -> "integer-divides"
        Symbol.POW -> "pow"
        Symbol.LPAREN -> "lparen"
        Symbol.RPAREN -> "rparen"
        Symbol.SKIP -> "skip"
        Symbol.ASSIGN -> "assign"
        Symbol.DEFINE -> "define"
        Symbol.TERM -> "term"
        Symbol.FOR -> "for"
        Symbol.TO -> "to"
        Symbol.BEGIN -> "begin"
        Symbol.END -> "end"
        Symbol.PRINT -> "print"
        Symbol.CITY -> "city"
        Symbol.NAME -> "name"
        Symbol.ROAD -> "road"
        Symbol.BUILDING -> "building"
        Symbol.STADIUM -> "stadium"
        Symbol.ARENA -> "arena"
        Symbol.LINE -> "line"
        Symbol.BEND -> "bend"
        Symbol.BOX -> "box"
        Symbol.CIRCLE -> "circle"
        Symbol.EQUALS -> "equals"
        Symbol.NOT_EQUALS -> "not_equals"
        Symbol.LESS_THAN -> "less_than"
        Symbol.MORE_THAN -> "more_than"
        Symbol.LESS_OR_EQUALS -> "less_or_equals"
        Symbol.MORE_OR_EQUALS -> "more_or_equals"
        Symbol.DEF -> "def"
        Symbol.OPEN_BRACKET -> "open_bracket"
        Symbol.CLOSE_BRACKET -> "close_bracket"
        Symbol.NIL -> "nil"
    }

fun printTokens(scanner: Scanner, output: OutputStream) {
    val writer = output.writer(Charsets.UTF_8)

    var token = scanner.getToken()
    while (token.symbol != Symbol.EOF) {
        writer.append("${name(token.symbol)}(\"${token.lexeme}\") ") // The output ends with a space!
        token = scanner.getToken()
    }
    writer.appendLine()
    writer.flush()
}