package com.virtualsoundnw.chords.audio

import com.virtualsoundnw.chords.voicing.ChordVoicing

/**
 * Standard ukulele tuning: reentrant G-C-E-A. Unlike guitar, this is *not*
 * ascending in pitch — the G string (physically the "top" string, as
 * ukulele chord charts and [ChordVoicing] both order it) is tuned higher
 * than the C string next to it. Getting this order backwards would silently
 * produce wrong-sounding chords despite looking plausible, so it's called
 * out explicitly here rather than left implicit.
 */
internal object StandardUkuleleTuning {
    private val openStringMidiNotes = listOf(67, 60, 64, 69) // G4 C4 E4 A4

    /** The MIDI note each pressed (non-muted) string of [voicing] sounds. */
    fun midiNotes(voicing: ChordVoicing): List<Int> =
        voicing.frets.mapIndexedNotNull { stringIndex, fret -> fret?.let { openStringMidiNotes[stringIndex] + it } }
}
