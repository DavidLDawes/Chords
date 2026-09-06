package com.virtualsoundnw.chords.voicing

/**
 * Looks up curated voicings by chord name for one instrument. Exists so
 * [com.virtualsoundnw.chords.ui.selector.ChordSelectionViewModel] can depend
 * on this instead of an Android-asset-backed repository (e.g.
 * [GuitarVoicingRepository], [UkuleleVoicingRepository]) directly, keeping
 * the ViewModel testable as a plain JVM unit.
 */
interface VoicingLookup {
    /** Returns the known playable shapes for [canonicalName], or an empty list if none are curated yet. */
    fun voicingsFor(canonicalName: String): List<ChordVoicing>
}
