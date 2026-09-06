package com.virtualsoundnw.chords.theory

/**
 * One selectable chord-quality checkbox. [ChordSymbol.of] combines a set of
 * these into a validated chord — see its kdoc for which combinations
 * conflict and why.
 */
enum class ChordQuality(val label: String) {
    MINOR("Minor"),
    SEVENTH("7th"),
    NINTH("9th"),
    SUS2("Sus2"),
    SUS4("Sus4"),
    SIXTH("6th"),
    AUGMENTED("Aug"),
    DIMINISHED("Dim"),
}
