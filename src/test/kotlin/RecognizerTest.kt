import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecognizerTest {
    @Test
    fun recognizeStartValid() {
        val examples = arrayOf(
            """
                city "Fiesa" {
                    road "Oljcna pot" {
                        bend ((1, 1), (2, 2), 20)
                        bend ((2, 2), (3, 1), 40)
                        line ((2, 2), (2, 5))
                        bend ((2, 3), (5, 4), 20)
                        line ((5, 4), (6, 4))
                    };
                    building "Soncne terase" {
                        line ((3, 2), (5, 3))
                    };
                    arena "Dom Fiesa" {
                        box ((0.5, 3.5), (1.5, 2.5))
                    };
                    building "LaRocca" {
                        box ((0.5, 5), (1.5, 4))
                    };
                    building "Hotel Fiesa" {
                        box ((1.75, 1.5), (2.25, 1))
                    };
                }
            """.trimIndent(),
            """
                city "Maribor" {
                    stadium "Ljudski vrt" {
                        rect((0, 0), (1,0), (1,1), (0,1))
                    };
                    arena "Tabor" {
                        circle ((1, 1), 2)
                        box ((2.5, 2.5), (4.5, 4.5))
                    };
                }
            """.trimIndent(),
            """
                city "Ljubljana" {
                    road "Slovenska cesta" {
                        line ((1, 1), (2, 2))
                        line ((2, 2), (3, 3))
                        line ((3, 3), (4, 4))
                    };
                    building "Neboticnik" {
                        box ((1, 1), (2, 2))
                    };
                    building "Kozolec" {
                        box ((2, 2), (3, 3))
                    };
                    building "Stolpnica" {
                        box ((3, 3), (4, 4))
                    };
                }
                city "Celje" {
                    road "Mariborska cesta" {
                        line ((1, 1), (2, 2))
                        line ((2, 2), (3, 3))
                        line ((3, 3), (4, 4))
                    };
                    building "Katedrala" {
                        box ((1, 1), (2, 2))
                    };
                    building "Trgovski center" {
                        box ((2, 2), (3, 3))
                    };
                    building "Gostilna" {
                        box ((3, 3), (4, 4))
                    };
                }
            """.trimIndent()
        )

        examples.forEach { example ->
            assertTrue(Recognizer(Scanner(Automaton, example.byteInputStream())).recognizeStart())
        }
    }

    @Test
    fun recognizeStartInvalid() {
        val examples = arrayOf(
            """
                city Maribor { 
                    road "Slovenska cesta" {
                        line ((1, 1), (2, 2))
                        line ((2, 2), (3, 3))
                        line ((3, 3), (4, 4))
                    };
                }
            """.trimIndent(),
            """
                city "Maribor" {
                    road "Slovenska cesta" {
                        box((1.5, 0.5))
                    }
                }
            """.trimIndent(),
            """
                city "Maribor" {
                    road "Slovenska cesta" {
                        line ((3, 3), (4, 4), (5, 5))
                    };
                }
            """.trimIndent(),
            """
                city "Celje" {
                    office {
                        box ((1, 1), (2, 2))
                    };
                }
            """.trimIndent()
        )

        examples.forEach { example ->
            assertFalse(Recognizer(Scanner(Automaton, example.byteInputStream())).recognizeStart())
        }
    }

}