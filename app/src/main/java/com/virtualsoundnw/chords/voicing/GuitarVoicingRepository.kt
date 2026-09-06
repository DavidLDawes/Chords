package com.virtualsoundnw.chords.voicing

import android.content.Context

/**
 * Looks up curated guitar voicings by chord name (e.g. "Am7", from
 * [com.virtualsoundnw.chords.theory.ChordSymbol.canonicalName]). Backed by a
 * bundled JSON asset so voicing data can be corrected or extended without
 * touching lookup logic — see [GuitarVoicingParser] for the actual parsing.
 */
class GuitarVoicingRepository(context: Context) : GuitarVoicingLookup {
    private val appContext = context.applicationContext

    private val voicingsByChordName: Map<String, List<GuitarVoicing>> by lazy {
        val json = appContext.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
        GuitarVoicingParser.parse(json)
    }

    override fun voicingsFor(canonicalName: String): List<GuitarVoicing> =
        voicingsByChordName[canonicalName].orEmpty()

    private companion object {
        const val ASSET_PATH = "chords/guitar_voicings.json"
    }
}
