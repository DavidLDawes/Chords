package com.virtualsoundnw.chords.voicing

import kotlinx.serialization.Serializable

/**
 * One playable guitar fingering for a chord. [frets] always has exactly 6
 * entries ordered low-E to high-E (index 0 = low E string, index 5 = high E
 * string): null means muted/not played, 0 means open, otherwise the fret
 * number to press.
 */
@Serializable
data class GuitarVoicing(val frets: List<Int?>) {
    init {
        require(frets.size == STRING_COUNT) {
            "A guitar voicing needs exactly $STRING_COUNT string entries, got ${frets.size}."
        }
    }

    companion object {
        const val STRING_COUNT = 6
    }
}
