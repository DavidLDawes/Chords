package com.virtualsoundnw.chords.audio

import com.virtualsoundnw.chords.voicing.ChordVoicing

/** Standard guitar tuning (E A D G B E) as MIDI note numbers for each open string, low to high. */
internal object StandardGuitarTuning {
    private val openStringMidiNotes = listOf(40, 45, 50, 55, 59, 64) // E2 A2 D3 G3 B3 E4

    /** The MIDI note each pressed (non-muted) string of [voicing] sounds. */
    fun midiNotes(voicing: ChordVoicing): List<Int> =
        voicing.frets.mapIndexedNotNull { stringIndex, fret -> fret?.let { openStringMidiNotes[stringIndex] + it } }
}
