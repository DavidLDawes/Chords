package com.virtualsoundnw.chords.voicing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GuitarVoicingParserTest {
    @Test
    fun `parses a chord with one voicing`() {
        val parsed = GuitarVoicingParser.parse(
            """{ "C": [ { "frets": [null, 3, 2, 0, 1, 0] } ] }"""
        )
        assertEquals(listOf(GuitarVoicing(listOf(null, 3, 2, 0, 1, 0))), parsed["C"])
    }

    @Test
    fun `parses multiple voicings for the same chord`() {
        val parsed = GuitarVoicingParser.parse(
            """{
                "E": [
                    { "frets": [0, 2, 2, 1, 0, 0] },
                    { "frets": [null, null, 2, 4, 5, 4] }
                ]
            }"""
        )
        assertEquals(2, parsed["E"]!!.size)
    }

    @Test
    fun `missing chord name returns null from the map`() {
        val parsed = GuitarVoicingParser.parse("""{ "C": [ { "frets": [null, 3, 2, 0, 1, 0] } ] }""")
        assertEquals(null, parsed["Zdim9"])
    }

    @Test
    fun `a voicing with the wrong number of strings is rejected`() {
        assertThrows(Exception::class.java) {
            GuitarVoicingParser.parse("""{ "C": [ { "frets": [0, 0, 0] } ] }""")
        }
    }
}
