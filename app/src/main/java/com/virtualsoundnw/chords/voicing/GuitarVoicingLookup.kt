package com.virtualsoundnw.chords.voicing

/**
 * Looks up curated guitar voicings by chord name. Exists so
 * [com.virtualsoundnw.chords.ui.selector.ChordSelectionViewModel] can depend
 * on this instead of the Android-asset-backed [GuitarVoicingRepository]
 * directly, keeping the ViewModel testable as a plain JVM unit.
 */
interface GuitarVoicingLookup {
    /** Returns the known playable shapes for [canonicalName], or an empty list if none are curated yet. */
    fun voicingsFor(canonicalName: String): List<GuitarVoicing>
}
