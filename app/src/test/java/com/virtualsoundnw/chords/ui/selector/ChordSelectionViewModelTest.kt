package com.virtualsoundnw.chords.ui.selector

import com.virtualsoundnw.chords.audio.ChordAudioSource
import com.virtualsoundnw.chords.audio.Instrument
import com.virtualsoundnw.chords.theory.ChordQuality
import com.virtualsoundnw.chords.theory.Note
import com.virtualsoundnw.chords.voicing.GuitarVoicing
import com.virtualsoundnw.chords.voicing.GuitarVoicingLookup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChordSelectionViewModelTest {
    // ChordSelectionViewModel's uiState is built with viewModelScope.stateIn,
    // which needs a Main dispatcher even in a plain JVM unit test.
    // Unconfined so the combine()/stateIn() coroutine runs eagerly on each
    // update instead of needing manual scheduler advancing.
    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is a plain C major`() = runTest {
        val viewModel = ChordSelectionViewModel(FakeGuitarVoicingLookup, FakeChordAudioSource())
        val state = viewModel.uiState.first()
        assertEquals(Note.C, state.root)
        assertEquals(emptySet<ChordQuality>(), state.qualities)
        assertEquals("C", state.chordSymbol.canonicalName)
    }

    @Test
    fun `selecting a root updates the resolved chord`() = runTest {
        val viewModel = ChordSelectionViewModel(FakeGuitarVoicingLookup, FakeChordAudioSource())
        viewModel.selectRoot(Note.G)
        assertEquals("G", viewModel.uiState.first().chordSymbol.canonicalName)
    }

    @Test
    fun `toggling a quality on then off returns to the plain triad`() = runTest {
        val viewModel = ChordSelectionViewModel(FakeGuitarVoicingLookup, FakeChordAudioSource())
        viewModel.selectRoot(Note.A)
        viewModel.toggleQuality(ChordQuality.MINOR)
        assertEquals("Am", viewModel.uiState.first().chordSymbol.canonicalName)

        viewModel.toggleQuality(ChordQuality.MINOR)
        assertEquals("A", viewModel.uiState.first().chordSymbol.canonicalName)
    }

    @Test
    fun `combining compatible qualities resolves the combined chord`() = runTest {
        val viewModel = ChordSelectionViewModel(FakeGuitarVoicingLookup, FakeChordAudioSource())
        viewModel.selectRoot(Note.D)
        viewModel.toggleQuality(ChordQuality.MINOR)
        viewModel.toggleQuality(ChordQuality.SEVENTH)
        assertEquals("Dm7", viewModel.uiState.first().chordSymbol.canonicalName)
    }

    @Test
    fun `toggling a quality that conflicts with the current selection is a no-op`() = runTest {
        val viewModel = ChordSelectionViewModel(FakeGuitarVoicingLookup, FakeChordAudioSource())
        viewModel.toggleQuality(ChordQuality.AUGMENTED)
        viewModel.toggleQuality(ChordQuality.DIMINISHED) // conflicts with Augmented, should be ignored

        val state = viewModel.uiState.first()
        assertEquals(setOf(ChordQuality.AUGMENTED), state.qualities)
        assertEquals("Caug", state.chordSymbol.canonicalName)
    }

    @Test
    fun `a quality conflicting with the current selection is reported disabled`() = runTest {
        val viewModel = ChordSelectionViewModel(FakeGuitarVoicingLookup, FakeChordAudioSource())
        viewModel.toggleQuality(ChordQuality.SEVENTH)

        val state = viewModel.uiState.first()
        assertFalse(state.isQualityEnabled(ChordQuality.SIXTH))
        assertTrue(state.isQualityEnabled(ChordQuality.SEVENTH)) // already selected, can still uncheck
        assertTrue(state.isQualityEnabled(ChordQuality.MINOR)) // unrelated, stays enabled
    }

    @Test
    fun `a curated voicing is resolved into state`() = runTest {
        val viewModel = ChordSelectionViewModel(FakeGuitarVoicingLookup, FakeChordAudioSource())
        // Default state is plain C, which FakeGuitarVoicingLookup has a voicing for.
        assertEquals(FakeGuitarVoicingLookup.C_VOICING, viewModel.uiState.first().voicing)
    }

    @Test
    fun `an uncurated chord resolves to a null voicing`() = runTest {
        val viewModel = ChordSelectionViewModel(FakeGuitarVoicingLookup, FakeChordAudioSource())
        viewModel.selectRoot(Note.B)
        viewModel.toggleQuality(ChordQuality.DIMINISHED) // Bdim isn't in the fake lookup
        assertEquals(null, viewModel.uiState.first().voicing)
    }

    @Test
    fun `playCurrentChord plays the currently resolved voicing`() = runTest {
        val audioSource = FakeChordAudioSource()
        val viewModel = ChordSelectionViewModel(FakeGuitarVoicingLookup, audioSource)

        viewModel.playCurrentChord()

        assertEquals(listOf(FakeGuitarVoicingLookup.C_VOICING), audioSource.playedVoicings)
    }

    @Test
    fun `playCurrentChord does nothing when there's no curated voicing`() = runTest {
        val audioSource = FakeChordAudioSource()
        val viewModel = ChordSelectionViewModel(FakeGuitarVoicingLookup, audioSource)
        viewModel.selectRoot(Note.B)
        viewModel.toggleQuality(ChordQuality.DIMINISHED) // Bdim isn't in the fake lookup

        viewModel.playCurrentChord()

        assertEquals(emptyList<GuitarVoicing>(), audioSource.playedVoicings)
    }
}

private object FakeGuitarVoicingLookup : GuitarVoicingLookup {
    val C_VOICING = GuitarVoicing(listOf(null, 3, 2, 0, 1, 0))

    override fun voicingsFor(canonicalName: String): List<GuitarVoicing> =
        if (canonicalName == "C") listOf(C_VOICING) else emptyList()
}

private class FakeChordAudioSource : ChordAudioSource {
    val playedVoicings = mutableListOf<GuitarVoicing>()
    var released = false

    override fun play(voicing: GuitarVoicing, instrument: Instrument) {
        playedVoicings += voicing
    }

    override fun release() {
        released = true
    }
}
