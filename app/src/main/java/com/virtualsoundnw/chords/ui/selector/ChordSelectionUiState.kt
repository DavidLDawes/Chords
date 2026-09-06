package com.virtualsoundnw.chords.ui.selector

import com.virtualsoundnw.chords.theory.ChordQuality
import com.virtualsoundnw.chords.theory.ChordSymbol
import com.virtualsoundnw.chords.theory.Note
import com.virtualsoundnw.chords.voicing.GuitarVoicing

/**
 * The chord picker's current selection. [chordSymbol] is always a valid,
 * resolved chord — [ChordSelectionViewModel] never lets [qualities] reach a
 * combination [ChordSymbol.findConflict] would reject, so there's no
 * "invalid selection" state to represent here. [voicing] is null when no
 * guitar shape has been curated yet for this chord (see PLAN.md Phase 2).
 */
data class ChordSelectionUiState(
    val root: Note,
    val qualities: Set<ChordQuality>,
    val chordSymbol: ChordSymbol,
    val voicing: GuitarVoicing?,
) {
    /** Whether [quality] can be toggled on right now, given the rest of the current selection. */
    fun isQualityEnabled(quality: ChordQuality): Boolean =
        quality in qualities || ChordSymbol.findConflict(qualities + quality) == null
}
