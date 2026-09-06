package com.virtualsoundnw.chords.theory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChordSymbolTest {
    private fun chord(root: Note, vararg qualities: ChordQuality) =
        ChordSymbol.of(root, qualities.toSet()).getOrThrow()

    @Test
    fun `plain major triad`() {
        val c = chord(Note.C)
        assertEquals("C", c.canonicalName)
        assertEquals(listOf(0, 4, 7), c.pitchClasses)
    }

    @Test
    fun `minor triad`() {
        val am = chord(Note.A, ChordQuality.MINOR)
        assertEquals("Am", am.canonicalName)
        assertEquals(listOf(0, 3, 7), am.pitchClasses)
    }

    @Test
    fun `dominant seventh`() {
        val g7 = chord(Note.G, ChordQuality.SEVENTH)
        assertEquals("G7", g7.canonicalName)
        assertEquals(listOf(0, 4, 7, 10), g7.pitchClasses)
    }

    @Test
    fun `minor seventh`() {
        val dm7 = chord(Note.D, ChordQuality.MINOR, ChordQuality.SEVENTH)
        assertEquals("Dm7", dm7.canonicalName)
        assertEquals(listOf(0, 3, 7, 10), dm7.pitchClasses)
    }

    @Test
    fun `dominant ninth implies the seventh without it being checked`() {
        val e9 = chord(Note.E, ChordQuality.NINTH)
        assertEquals("E9", e9.canonicalName)
        assertEquals(listOf(0, 2, 4, 7, 10), e9.pitchClasses)
    }

    @Test
    fun `explicit seventh plus ninth is the same chord as ninth alone`() {
        val explicit = chord(Note.E, ChordQuality.SEVENTH, ChordQuality.NINTH)
        val implicit = chord(Note.E, ChordQuality.NINTH)
        assertEquals(implicit.pitchClasses, explicit.pitchClasses)
        assertEquals(implicit.canonicalName, explicit.canonicalName)
    }

    @Test
    fun `six nine chord has no seventh`() {
        val c69 = chord(Note.C, ChordQuality.SIXTH, ChordQuality.NINTH)
        assertEquals("C6/9", c69.canonicalName)
        assertEquals(listOf(0, 2, 4, 7, 9), c69.pitchClasses)
    }

    @Test
    fun `sixth chord`() {
        val c6 = chord(Note.C, ChordQuality.SIXTH)
        assertEquals("C6", c6.canonicalName)
        assertEquals(listOf(0, 4, 7, 9), c6.pitchClasses)
    }

    @Test
    fun `sus2 and sus4`() {
        assertEquals("Csus2", chord(Note.C, ChordQuality.SUS2).canonicalName)
        assertEquals("Csus4", chord(Note.C, ChordQuality.SUS4).canonicalName)
        assertEquals(listOf(0, 2, 7), chord(Note.C, ChordQuality.SUS2).pitchClasses)
        assertEquals(listOf(0, 5, 7), chord(Note.C, ChordQuality.SUS4).pitchClasses)
    }

    @Test
    fun `seventh sus4 puts the extension before sus per convention`() {
        val chord = chord(Note.C, ChordQuality.SUS4, ChordQuality.SEVENTH)
        assertEquals("C7sus4", chord.canonicalName)
        assertEquals(listOf(0, 5, 7, 10), chord.pitchClasses)
    }

    @Test
    fun `augmented triad`() {
        val aug = chord(Note.C, ChordQuality.AUGMENTED)
        assertEquals("Caug", aug.canonicalName)
        assertEquals(listOf(0, 4, 8), aug.pitchClasses)
    }

    @Test
    fun `diminished triad`() {
        val dim = chord(Note.C, ChordQuality.DIMINISHED)
        assertEquals("Cdim", dim.canonicalName)
        assertEquals(listOf(0, 3, 6), dim.pitchClasses)
    }

    @Test
    fun `diminished seventh uses a diminished 7th, not a minor 7th`() {
        val dim7 = chord(Note.C, ChordQuality.DIMINISHED, ChordQuality.SEVENTH)
        assertEquals("Cdim7", dim7.canonicalName)
        assertEquals(listOf(0, 3, 6, 9), dim7.pitchClasses)
    }

    @Test
    fun `conflicting triad qualities are rejected`() {
        val result = ChordSymbol.of(Note.C, setOf(ChordQuality.AUGMENTED, ChordQuality.DIMINISHED))
        assertTrue(result.isFailure)
    }

    @Test
    fun `sus2 and sus4 together are rejected`() {
        val result = ChordSymbol.of(Note.C, setOf(ChordQuality.SUS2, ChordQuality.SUS4))
        assertTrue(result.isFailure)
    }

    @Test
    fun `minor and diminished together are rejected as redundant-but-ambiguous`() {
        val result = ChordSymbol.of(Note.C, setOf(ChordQuality.MINOR, ChordQuality.DIMINISHED))
        assertTrue(result.isFailure)
    }

    @Test
    fun `sixth and seventh together are rejected`() {
        val result = ChordSymbol.of(Note.C, setOf(ChordQuality.SIXTH, ChordQuality.SEVENTH))
        assertTrue(result.isFailure)
    }

    @Test
    fun `findConflict returns null for a valid combination`() {
        assertEquals(null, ChordSymbol.findConflict(setOf(ChordQuality.MINOR, ChordQuality.SEVENTH)))
    }
}
