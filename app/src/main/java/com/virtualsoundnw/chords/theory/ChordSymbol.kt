package com.virtualsoundnw.chords.theory

/**
 * A resolved chord: a root [Note] plus a validated combination of
 * [ChordQuality] checkboxes. Always construct via [ChordSymbol.of] — it runs
 * the compatibility checks so an invalid combination (e.g. Aug + Dim) never
 * becomes a [ChordSymbol] in the first place.
 *
 * [pitchClasses] are semitone offsets from the root (0-11, deduped, sorted,
 * always including 0) — this is what voicing lookup and audio playback key
 * off of. [canonicalName] is a display label; it aims to match common chord
 * notation for the combinations this app supports, but isn't guaranteed to
 * match every jazz-notation convention for combinations outside that set.
 */
@ConsistentCopyVisibility
data class ChordSymbol private constructor(
    val root: Note,
    val qualities: Set<ChordQuality>,
    val canonicalName: String,
    val pitchClasses: List<Int>,
) {
    companion object {
        /** At most one of these can be active — they all define the chord's basic triad shape. */
        private val TRIAD_GROUP = setOf(
            ChordQuality.MINOR,
            ChordQuality.SUS2,
            ChordQuality.SUS4,
            ChordQuality.AUGMENTED,
            ChordQuality.DIMINISHED,
        )

        fun of(root: Note, qualities: Set<ChordQuality>): Result<ChordSymbol> {
            findConflict(qualities)?.let { return Result.failure(IllegalArgumentException(it)) }

            val triadQuality = TRIAD_GROUP.firstOrNull { it in qualities }
            val third = when (triadQuality) {
                ChordQuality.MINOR, ChordQuality.DIMINISHED -> 3
                ChordQuality.SUS2 -> 2
                ChordQuality.SUS4 -> 5
                else -> 4
            }
            val fifth = when (triadQuality) {
                ChordQuality.DIMINISHED -> 6
                ChordQuality.AUGMENTED -> 8
                else -> 7
            }

            val explicitSeventh = ChordQuality.SEVENTH in qualities
            val sixth = ChordQuality.SIXTH in qualities
            val ninth = ChordQuality.NINTH in qualities
            // "9" on its own conventionally implies the dominant/minor 7th is
            // also present (e.g. "C9" = C7 + a 9th) — unless a 6th is also
            // checked, in which case it's a 6/9 chord and there's no 7th at all.
            val hasSeventh = explicitSeventh || (ninth && !sixth)

            val pitchClasses = sortedSetOf(0, third, fifth)
            if (hasSeventh) {
                // A diminished triad's "7th" is a diminished 7th (9 semitones,
                // the classic symmetric dim7 chord), not a minor 7th (10) —
                // otherwise Dim+7 would silently produce a half-diminished
                // chord instead of the dim7 most players mean by "Cdim7".
                pitchClasses += if (triadQuality == ChordQuality.DIMINISHED) 9 else 10
            }
            if (sixth) pitchClasses += 9
            if (ninth) pitchClasses += 2

            val triadToken = when (triadQuality) {
                ChordQuality.MINOR -> "m"
                ChordQuality.DIMINISHED -> "dim"
                ChordQuality.SUS2 -> "sus2"
                ChordQuality.SUS4 -> "sus4"
                ChordQuality.AUGMENTED -> "aug"
                else -> ""
            }
            val extensionToken = when {
                ninth && sixth -> "6/9"
                ninth -> "9"
                sixth -> "6"
                explicitSeventh -> "7"
                else -> ""
            }
            // Convention puts the extension before "sus" ("7sus4", "9sus4")
            // but after everything else ("m7", "dim7", "aug9", "6").
            val suffix = if (triadQuality == ChordQuality.SUS2 || triadQuality == ChordQuality.SUS4) {
                extensionToken + triadToken
            } else {
                triadToken + extensionToken
            }

            return Result.success(
                ChordSymbol(
                    root = root,
                    qualities = qualities,
                    canonicalName = root.symbol + suffix,
                    pitchClasses = pitchClasses.toList(),
                )
            )
        }

        /** Returns a human-readable reason [qualities] is invalid, or null if it's fine. */
        fun findConflict(qualities: Set<ChordQuality>): String? {
            val triadPicks = qualities intersect TRIAD_GROUP
            if (triadPicks.size > 1) {
                return "Only one of ${TRIAD_GROUP.joinToString { it.label }} can be selected at a time."
            }
            if (ChordQuality.SIXTH in qualities && ChordQuality.SEVENTH in qualities) {
                return "6th and 7th can't both be selected (that would be a 13-type chord, not supported yet)."
            }
            return null
        }
    }
}
