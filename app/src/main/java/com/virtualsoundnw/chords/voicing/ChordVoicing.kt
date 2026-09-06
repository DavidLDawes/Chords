package com.virtualsoundnw.chords.voicing

import kotlinx.serialization.Serializable

/**
 * One playable fingering for a chord on a fretted string instrument (guitar,
 * ukulele, ...). [frets] is ordered by physical string position — null means
 * muted/not played, 0 means open, otherwise the fret number to press. String
 * count is instrument-dependent (6 for guitar, 4 for ukulele), so read it
 * from `frets.size` rather than assuming a fixed value. Note this ordering is
 * "as strung," not necessarily ascending pitch: ukulele's standard reentrant
 * tuning means its physical string order isn't pitch-ascending.
 */
@Serializable
data class ChordVoicing(val frets: List<Int?>) {
    init {
        require(frets.isNotEmpty()) { "A voicing needs at least one string." }
    }
}
