package com.virtualsoundnw.chords.voicing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ChordVoicingParserTest {
    @Test
    fun `parses a chord with one voicing`() {
        val parsed = ChordVoicingParser.parse(
            """{ "C": [ { "frets": [null, 3, 2, 0, 1, 0] } ] }"""
        )
        assertEquals(listOf(ChordVoicing(listOf(null, 3, 2, 0, 1, 0))), parsed["C"])
    }

    @Test
    fun `parses multiple voicings for the same chord`() {
        val parsed = ChordVoicingParser.parse(
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
        val parsed = ChordVoicingParser.parse("""{ "C": [ { "frets": [null, 3, 2, 0, 1, 0] } ] }""")
        assertEquals(null, parsed["Zdim9"])
    }

    @Test
    fun `a 4-string voicing parses fine (string count is instrument-dependent, not fixed)`() {
        val parsed = ChordVoicingParser.parse("""{ "C": [ { "frets": [0, 0, 0, 3] } ] }""")
        assertEquals(listOf(ChordVoicing(listOf(0, 0, 0, 3))), parsed["C"])
    }

    @Test
    fun `an empty frets list is rejected`() {
        assertThrows(Exception::class.java) {
            ChordVoicingParser.parse("""{ "C": [ { "frets": [] } ] }""")
        }
    }
}
