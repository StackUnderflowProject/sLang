import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ParserTest {

    @Test
    fun `parse returns correct city when input is valid`() {
        val input = """
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
        """.byteInputStream()

        val parser = Parser(Scanner(Automaton, input))
        val result = parser.parse()

        assertEquals("\"Maribor\"", result.name)
        // Further assertions to validate the structure of the city
    }

    @Test
    fun `parse throws IllegalArgumentException when input is invalid`() {
        val input = """
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
        """.trimIndent().replace("\"", "").byteInputStream() // Removing semicolon to make the input invalid

        val parser = Parser(Scanner(Automaton, input))

        assertThrows<IllegalArgumentException> {
            parser.parse()
        }
    }

    @Test
    fun `parse handles empty city correctly`() {
        val input = """
            city "Maribor" {
            }
        """.trimIndent().byteInputStream()

        val parser = Parser(Scanner(Automaton, input))
        val result = parser.parse()

        // Further assertions to validate the structure of the city
        assertEquals("\"Maribor\"", result.name)
    }

    fun `parse handles variable assign correctly`() {
        val input = """
            city "Maribor" {
                var a = 5;
                var b = 10;
                var c = a + b;
            }
        """.trimIndent().byteInputStream()

        val parser = Parser(Scanner(Automaton, input))
        val result = parser.parse()

        // Further assertions to validate the structure of the city
        assertEquals("\"Maribor\"", result.name)
    }
}