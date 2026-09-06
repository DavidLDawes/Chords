package com.virtualsoundnw.chords.voicing

import kotlinx.serialization.json.Json

/**
 * Parses the bundled guitar-voicing JSON (assets/chords/guitar_voicings.json)
 * into a lookup by [com.virtualsoundnw.chords.theory.ChordSymbol.canonicalName].
 * Kept free of Android APIs so the real bundled data can be parsed and
 * validated directly from a JVM unit test.
 */
object ChordVoicingParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(source: String): Map<String, List<ChordVoicing>> =
        json.decodeFromString(source)
}
