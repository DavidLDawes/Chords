package com.virtualsoundnw.chords.data

import com.virtualsoundnw.chords.audio.Instrument
import com.virtualsoundnw.chords.theory.ChordQuality
import com.virtualsoundnw.chords.theory.Note

/** A chord selection worth remembering across app launches. */
data class SavedSelection(val root: Note, val qualities: Set<ChordQuality>, val instrument: Instrument)

/**
 * Persists the last chord selection. Exists as an interface (like
 * [com.virtualsoundnw.chords.voicing.VoicingLookup]) so
 * [com.virtualsoundnw.chords.ui.selector.ChordSelectionViewModel] stays
 * testable as a plain JVM unit instead of depending on DataStore directly.
 */
interface ChordSelectionStore {
    /** The last saved selection, or null if nothing has been saved yet. */
    suspend fun load(): SavedSelection?

    suspend fun save(selection: SavedSelection)
}
