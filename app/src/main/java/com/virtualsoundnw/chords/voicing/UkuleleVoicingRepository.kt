package com.virtualsoundnw.chords.voicing

import android.content.Context

/**
 * Looks up curated ukulele voicings by chord name (e.g. "Am7", from
 * [com.virtualsoundnw.chords.theory.ChordSymbol.canonicalName]). Backed by a
 * bundled JSON asset so voicing data can be corrected or extended without
 * touching lookup logic — see [ChordVoicingParser] for the actual parsing.
 */
class UkuleleVoicingRepository(context: Context) : VoicingLookup {
    private val appContext = context.applicationContext

    private val voicingsByChordName: Map<String, List<ChordVoicing>> by lazy {
        val json = appContext.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
        ChordVoicingParser.parse(json)
    }

    override fun voicingsFor(canonicalName: String): List<ChordVoicing> =
        voicingsByChordName[canonicalName].orEmpty()

    private companion object {
        const val ASSET_PATH = "chords/ukulele_voicings.json"
    }
}
