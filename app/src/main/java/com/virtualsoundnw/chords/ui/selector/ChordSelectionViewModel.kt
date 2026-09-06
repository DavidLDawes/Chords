package com.virtualsoundnw.chords.ui.selector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.virtualsoundnw.chords.theory.ChordQuality
import com.virtualsoundnw.chords.theory.ChordSymbol
import com.virtualsoundnw.chords.theory.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class ChordSelectionViewModel : ViewModel() {
    private val root = MutableStateFlow(Note.C)
    private val qualities = MutableStateFlow<Set<ChordQuality>>(emptySet())

    val uiState: StateFlow<ChordSelectionUiState> =
        combine(root, qualities) { root, qualities ->
            // Only ever reachable via selectRoot/toggleQuality below, both of
            // which keep qualities inside what ChordSymbol.findConflict allows.
            ChordSelectionUiState(root, qualities, ChordSymbol.of(root, qualities).getOrThrow())
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            ChordSelectionUiState(Note.C, emptySet(), ChordSymbol.of(Note.C, emptySet()).getOrThrow()),
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
}
