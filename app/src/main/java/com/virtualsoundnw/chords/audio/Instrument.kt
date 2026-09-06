package com.virtualsoundnw.chords.audio

/**
 * An instrument [MidiChordPlayer] can render a chord as, identified by its
 * General MIDI program number (0-127) so Android's built-in synth knows what
 * to sound like. GUITAR is the only entry for v1 — see CLAUDE.md's non-goals.
 */
enum class Instrument(val generalMidiProgram: Int) {
    GUITAR(generalMidiProgram = 24), // Acoustic Guitar (nylon)
}
