package com.virtualsoundnw.chords.audio

import com.virtualsoundnw.chords.voicing.ChordVoicing
import org.junit.Assert.assertEquals
import org.junit.Test

class StandardGuitarTuningTest {
    @Test
    fun `open Am7 resolves to its known MIDI notes`() {
        // Am7: X 0 2 0 1 0 -> A2 E3 G3 C4 E4 (skipping the muted low E)
        val notes = StandardGuitarTuning.midiNotes(ChordVoicing(listOf(null, 0, 2, 0, 1, 0)))
        assertEquals(listOf(45, 52, 55, 60, 64), notes)
    }

    @Test
    fun `open E major resolves to its known MIDI notes`() {
        // E: 0 2 2 1 0 0
        val notes = StandardGuitarTuning.midiNotes(ChordVoicing(listOf(0, 2, 2, 1, 0, 0)))
        assertEquals(listOf(40, 47, 52, 56, 59, 64), notes)
    }

    @Test
    fun `every resolved note is a real chord tone of the pitch classes it should be`() {
        // Am7 pitch classes: A=9, C=0, E=4, G=7
        val notes = StandardGuitarTuning.midiNotes(ChordVoicing(listOf(null, 0, 2, 0, 1, 0)))
        val pitchClasses = notes.map { it % 12 }.toSet()
        assertEquals(setOf(9, 0, 4, 7), pitchClasses)
    }

    @Test
    fun `all strings muted resolves to no notes`() {
        assertEquals(emptyList<Int>(), StandardGuitarTuning.midiNotes(ChordVoicing(List(6) { null })))
    }
}
