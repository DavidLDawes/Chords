package com.virtualsoundnw.chords.ui.selector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.virtualsoundnw.chords.theory.ChordQuality
import com.virtualsoundnw.chords.theory.ChordSymbol
import com.virtualsoundnw.chords.theory.Note
import com.virtualsoundnw.chords.voicing.GuitarVoicingLookup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class ChordSelectionViewModel(private val voicingLookup: GuitarVoicingLookup) : ViewModel() {
    private val root = MutableStateFlow(Note.C)
    private val qualities = MutableStateFlow<Set<ChordQuality>>(emptySet())

    val uiState: StateFlow<ChordSelectionUiState> =
        combine(root, qualities, ::resolve).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            resolve(Note.C, emptySet()),
        )

    fun selectRoot(note: Note) {
        root.value = note
    }

    fun toggleQuality(quality: ChordQuality) {
        qualities.update { current ->
            when {
                quality in current -> current - quality
                ChordSymbol.findConflict(current + quality) == null -> current + quality
                else -> current
            }
        }
    }

    private fun resolve(root: Note, qualities: Set<ChordQuality>): ChordSelectionUiState {
        // Only ever reachable via selectRoot/toggleQuality above, both of
        // which keep qualities inside what ChordSymbol.findConflict allows.
        val symbol = ChordSymbol.of(root, qualities).getOrThrow()
        val voicing = voicingLookup.voicingsFor(symbol.canonicalName).firstOrNull()
        return ChordSelectionUiState(root, qualities, symbol, voicing)
    }
}
