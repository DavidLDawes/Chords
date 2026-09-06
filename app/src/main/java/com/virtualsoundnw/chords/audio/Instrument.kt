package com.virtualsoundnw.chords.audio

/**
 * An instrument [MidiChordPlayer] can render a chord as, identified by its
 * General MIDI program number (0-127) so Android's built-in synth knows what
 * to sound like.
 */
enum class Instrument(val generalMidiProgram: Int, val label: String) {
    GUITAR(generalMidiProgram = 24, label = "Guitar"), // Acoustic Guitar (nylon)

    // General MIDI has no dedicated ukulele program; Banjo is the closest
    // commonly-used substitute timbre, not an oversight — see CLAUDE.md.
    UKULELE(generalMidiProgram = 105, label = "Ukulele"), // Banjo
}
