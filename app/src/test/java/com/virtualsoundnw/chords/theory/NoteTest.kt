package com.virtualsoundnw.chords.theory

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteTest {
    @Test
    fun `transposedBy wraps forward across the octave`() {
        assertEquals(Note.C, Note.B.transposedBy(1))
    }

    @Test
    fun `transposedBy wraps backward across the octave`() {
        assertEquals(Note.B, Note.C.transposedBy(-1))
    }

    @Test
    fun `transposedBy zero is a no-op`() {
        assertEquals(Note.F_SHARP, Note.F_SHARP.transposedBy(0))
    }

    @Test
    fun `transposedBy handles multiple full octaves`() {
        assertEquals(Note.D, Note.D.transposedBy(24))
    }

    @Test
    fun `symbol strips the flat alternative`() {
        assertEquals("C#", Note.C_SHARP.symbol)
        assertEquals("A", Note.A.symbol)
    }
}
