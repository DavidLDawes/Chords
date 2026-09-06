package com.virtualsoundnw.chords.ui.selector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.virtualsoundnw.chords.audio.ChordAudioSource
import com.virtualsoundnw.chords.data.ChordSelectionStore
import com.virtualsoundnw.chords.data.SavedSelection
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
import kotlinx.coroutines.launch

class ChordSelectionViewModel(
    private val voicingLookup: GuitarVoicingLookup,
    private val audioSource: ChordAudioSource,
    private val selectionStore: ChordSelectionStore,
) : ViewModel() {
    private val root = MutableStateFlow(Note.C)
    private val qualities = MutableStateFlow<Set<ChordQuality>>(emptySet())

    val uiState: StateFlow<ChordSelectionUiState> =
        combine(root, qualities, ::resolve).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            resolve(Note.C, emptySet()),
        )

    init {
        viewModelScope.launch {
            selectionStore.load()?.let { saved ->
                root.value = saved.root
                qualities.value = saved.qualities
            }
        }
    }

    fun selectRoot(note: Note) {
        root.value = note
        persistSelection()
    }

    fun toggleQuality(quality: ChordQuality) {
        qualities.update { current ->
            when {
                quality in current -> current - quality
                ChordSymbol.findConflict(current + quality) == null -> current + quality
                else -> current
            }
        }
        persistSelection()
    }

    fun playCurrentChord() {
        // Resolved directly from root/qualities rather than uiState.value:
        // uiState is a WhileSubscribed StateFlow, so its cached value only
        // tracks root/qualities while something is actively collecting it.
        resolve(root.value, qualities.value).voicing?.let { audioSource.play(it) }
    }

    override fun onCleared() {
        audioSource.release()
    }

    private fun persistSelection() {
        viewModelScope.launch {
            selectionStore.save(SavedSelection(root.value, qualities.value))
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
