package com.virtualsoundnw.chords.ui.fretboard

import org.junit.Assert.assertEquals
import org.junit.Test

class FretboardLayoutTest {
    @Test
    fun `an open-position voicing keeps the nut visible`() {
        // Am7: X 0 2 0 1 0
        assertEquals(1, FretboardLayout.baseFret(listOf(null, 0, 2, 0, 1, 0)))
    }

    @Test
    fun `a voicing that fits within the first 5 frets keeps the nut visible`() {
        // Cm: X 3 5 5 4 3
        assertEquals(1, FretboardLayout.baseFret(listOf(null, 3, 5, 5, 4, 3)))
    }

    @Test
    fun `a voicing above fret 5 shifts the window so the highest fret is the last row`() {
        // D#7 barre shape: X 6 8 6 8 6 -> max fret 8, window should be 4..8
        assertEquals(4, FretboardLayout.baseFret(listOf(null, 6, 8, 6, 8, 6)))
    }

    @Test
    fun `an all-muted-or-open voicing defaults to showing the nut`() {
        assertEquals(1, FretboardLayout.baseFret(listOf(null, null, 0, 0, null, 0)))
    }

    @Test
    fun `markerFor reports muted for a null fret`() {
        assertEquals(FretboardLayout.StringMarker.Muted, FretboardLayout.markerFor(null, baseFret = 1))
    }

    @Test
    fun `markerFor reports open for fret zero`() {
        assertEquals(FretboardLayout.StringMarker.Open, FretboardLayout.markerFor(0, baseFret = 1))
    }

    @Test
    fun `markerFor reports the row relative to baseFret`() {
        assertEquals(FretboardLayout.StringMarker.Fretted(0), FretboardLayout.markerFor(6, baseFret = 6))
        assertEquals(FretboardLayout.StringMarker.Fretted(2), FretboardLayout.markerFor(8, baseFret = 6))
    }
}
