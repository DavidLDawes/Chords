package com.virtualsoundnw.chords.theory

/**
 * The 12 pitch classes of 12-tone equal temperament, ordered by semitone
 * distance from C. The UI doesn't ask the player to pick a spelling, so each
 * accidental shows both names (e.g. "C# / Db") and [symbol] just uses the
 * sharp spelling for building chord names.
 */
enum class Note(val displayName: String) {
    C("C"),
    C_SHARP("C# / Db"),
    D("D"),
    D_SHARP("D# / Eb"),
    E("E"),
    F("F"),
    F_SHARP("F# / Gb"),
    G("G"),
    G_SHARP("G# / Ab"),
    A("A"),
    A_SHARP("A# / Bb"),
    B("B");

    val symbol: String get() = displayName.substringBefore(" /")

    /** The note [semitones] above (or, if negative, below) this one, wrapping across the octave. */
    fun transposedBy(semitones: Int): Note {
        val notes = entries
        val index = ((ordinal + semitones) % notes.size + notes.size) % notes.size
        return notes[index]
    }
}
