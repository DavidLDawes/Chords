package com.virtualsoundnw.chords.audio

import com.virtualsoundnw.chords.voicing.ChordVoicing
import org.junit.Assert.assertEquals
import org.junit.Test

class StandardUkuleleTuningTest {
    @Test
    fun `open C major resolves to its known MIDI notes`() {
        // C: 0 0 0 3 (G C E A)
        val notes = StandardUkuleleTuning.midiNotes(ChordVoicing(listOf(0, 0, 0, 3)))
        assertEquals(listOf(67, 60, 64, 72), notes)
    }

    @Test
    fun `the reentrant G string sounds higher than the C string next to it`() {
        // The defining quirk of standard ukulele tuning: unlike guitar,
        // physical string order isn't pitch-ascending.
        val notes = StandardUkuleleTuning.midiNotes(ChordVoicing(listOf(0, 0, 0, 0)))
        val (openG, openC) = notes[0] to notes[1]
        assertEquals(67, openG)
        assertEquals(60, openC)
        assert(openG > openC) { "Expected reentrant tuning: open G ($openG) should sound higher than open C ($openC)" }
    }

    @Test
    fun `every resolved note is a real chord tone of the pitch classes it should be`() {
        // C major pitch classes: C=0, E=4, G=7
        val notes = StandardUkuleleTuning.midiNotes(ChordVoicing(listOf(0, 0, 0, 3)))
        val pitchClasses = notes.map { it % 12 }.toSet()
        assertEquals(setOf(7, 0, 4), pitchClasses)
    }

    @Test
    fun `all strings muted resolves to no notes`() {
        assertEquals(emptyList<Int>(), StandardUkuleleTuning.midiNotes(ChordVoicing(List(4) { null })))
    }
}
