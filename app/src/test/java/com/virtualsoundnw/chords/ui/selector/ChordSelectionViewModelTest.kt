package com.virtualsoundnw.chords.ui.selector

import com.virtualsoundnw.chords.audio.ChordAudioSource
import com.virtualsoundnw.chords.audio.Instrument
import com.virtualsoundnw.chords.data.ChordSelectionStore
import com.virtualsoundnw.chords.data.SavedSelection
import com.virtualsoundnw.chords.theory.ChordQuality
import com.virtualsoundnw.chords.theory.Note
import com.virtualsoundnw.chords.voicing.ChordVoicing
import com.virtualsoundnw.chords.voicing.VoicingLookup
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

    private fun createViewModel(
        audioSource: ChordAudioSource = FakeChordAudioSource(),
        store: ChordSelectionStore = FakeChordSelectionStore(),
    ) = ChordSelectionViewModel(
        mapOf(Instrument.GUITAR to FakeGuitarVoicingLookup, Instrument.UKULELE to FakeUkuleleVoicingLookup),
        audioSource,
        store,
    )

    @Test
    fun `initial state is a plain C major on guitar`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.first()
        assertEquals(Note.C, state.root)
        assertEquals(emptySet<ChordQuality>(), state.qualities)
        assertEquals(Instrument.GUITAR, state.instrument)
        assertEquals("C", state.chordSymbol.canonicalName)
    }

    @Test
    fun `selecting a root updates the resolved chord`() = runTest {
        val viewModel = createViewModel()
        viewModel.selectRoot(Note.G)
        assertEquals("G", viewModel.uiState.first().chordSymbol.canonicalName)
    }

    @Test
    fun `toggling a quality on then off returns to the plain triad`() = runTest {
        val viewModel = createViewModel()
        viewModel.selectRoot(Note.A)
        viewModel.toggleQuality(ChordQuality.MINOR)
        assertEquals("Am", viewModel.uiState.first().chordSymbol.canonicalName)

        viewModel.toggleQuality(ChordQuality.MINOR)
        assertEquals("A", viewModel.uiState.first().chordSymbol.canonicalName)
    }

    @Test
    fun `combining compatible qualities resolves the combined chord`() = runTest {
        val viewModel = createViewModel()
        viewModel.selectRoot(Note.D)
        viewModel.toggleQuality(ChordQuality.MINOR)
        viewModel.toggleQuality(ChordQuality.SEVENTH)
        assertEquals("Dm7", viewModel.uiState.first().chordSymbol.canonicalName)
    }

    @Test
    fun `toggling a quality that conflicts with the current selection is a no-op`() = runTest {
        val viewModel = createViewModel()
        viewModel.toggleQuality(ChordQuality.AUGMENTED)
        viewModel.toggleQuality(ChordQuality.DIMINISHED) // conflicts with Augmented, should be ignored

        val state = viewModel.uiState.first()
        assertEquals(setOf(ChordQuality.AUGMENTED), state.qualities)
        assertEquals("Caug", state.chordSymbol.canonicalName)
    }

    @Test
    fun `a quality conflicting with the current selection is reported disabled`() = runTest {
        val viewModel = createViewModel()
        viewModel.toggleQuality(ChordQuality.SEVENTH)

        val state = viewModel.uiState.first()
        assertFalse(state.isQualityEnabled(ChordQuality.SIXTH))
        assertTrue(state.isQualityEnabled(ChordQuality.SEVENTH)) // already selected, can still uncheck
        assertTrue(state.isQualityEnabled(ChordQuality.MINOR)) // unrelated, stays enabled
    }

    @Test
    fun `a curated voicing is resolved into state`() = runTest {
        val viewModel = createViewModel()
        // Default state is plain C on guitar, which FakeGuitarVoicingLookup has a voicing for.
        assertEquals(FakeGuitarVoicingLookup.C_VOICING, viewModel.uiState.first().voicing)
    }

    @Test
    fun `an uncurated chord resolves to a null voicing`() = runTest {
        val viewModel = createViewModel()
        viewModel.selectRoot(Note.B)
        viewModel.toggleQuality(ChordQuality.DIMINISHED) // Bdim isn't in the fake lookup
        assertEquals(null, viewModel.uiState.first().voicing)
    }

    @Test
    fun `playCurrentChord plays the currently resolved voicing`() = runTest {
        val audioSource = FakeChordAudioSource()
        val viewModel = createViewModel(audioSource = audioSource)

        viewModel.playCurrentChord()

        assertEquals(listOf(FakeGuitarVoicingLookup.C_VOICING), audioSource.playedVoicings)
    }

    @Test
    fun `playCurrentChord does nothing when there's no curated voicing`() = runTest {
        val audioSource = FakeChordAudioSource()
        val viewModel = createViewModel(audioSource = audioSource)
        viewModel.selectRoot(Note.B)
        viewModel.toggleQuality(ChordQuality.DIMINISHED) // Bdim isn't in the fake lookup

        viewModel.playCurrentChord()

        assertEquals(emptyList<ChordVoicing>(), audioSource.playedVoicings)
    }

    @Test
    fun `a previously saved selection is restored on init`() = runTest {
        val store = FakeChordSelectionStore(SavedSelection(Note.G, setOf(ChordQuality.MINOR), Instrument.UKULELE))
        val viewModel = createViewModel(store = store)

        val state = viewModel.uiState.first()
        assertEquals(Note.G, state.root)
        assertEquals(setOf(ChordQuality.MINOR), state.qualities)
        assertEquals(Instrument.UKULELE, state.instrument)
        assertEquals("Gm", state.chordSymbol.canonicalName)
    }

    @Test
    fun `selecting a root persists the new selection`() = runTest {
        val store = FakeChordSelectionStore()
        val viewModel = createViewModel(store = store)

        viewModel.selectRoot(Note.D)

        assertEquals(SavedSelection(Note.D, emptySet(), Instrument.GUITAR), store.savedSelections.last())
    }

    @Test
    fun `toggling a quality persists the new selection`() = runTest {
        val store = FakeChordSelectionStore()
        val viewModel = createViewModel(store = store)

        viewModel.toggleQuality(ChordQuality.MINOR)

        assertEquals(SavedSelection(Note.C, setOf(ChordQuality.MINOR), Instrument.GUITAR), store.savedSelections.last())
    }

    @Test
    fun `selecting an instrument switches which voicing is resolved`() = runTest {
        val viewModel = createViewModel()

        viewModel.selectInstrument(Instrument.UKULELE)

        val state = viewModel.uiState.first()
        assertEquals(Instrument.UKULELE, state.instrument)
        assertEquals(FakeUkuleleVoicingLookup.C_VOICING, state.voicing)
    }

    @Test
    fun `selecting an instrument persists it`() = runTest {
        val store = FakeChordSelectionStore()
        val viewModel = createViewModel(store = store)

        viewModel.selectInstrument(Instrument.UKULELE)

        assertEquals(SavedSelection(Note.C, emptySet(), Instrument.UKULELE), store.savedSelections.last())
    }

    @Test
    fun `playCurrentChord plays on the currently selected instrument`() = runTest {
        val audioSource = FakeChordAudioSource()
        val viewModel = createViewModel(audioSource = audioSource)

        viewModel.selectInstrument(Instrument.UKULELE)
        viewModel.playCurrentChord()

        assertEquals(listOf(FakeUkuleleVoicingLookup.C_VOICING), audioSource.playedVoicings)
        assertEquals(listOf(Instrument.UKULELE), audioSource.playedInstruments)
    }
}

private object FakeGuitarVoicingLookup : VoicingLookup {
    val C_VOICING = ChordVoicing(listOf(null, 3, 2, 0, 1, 0))

    override fun voicingsFor(canonicalName: String): List<ChordVoicing> =
        if (canonicalName == "C") listOf(C_VOICING) else emptyList()
}

private object FakeUkuleleVoicingLookup : VoicingLookup {
    val C_VOICING = ChordVoicing(listOf(0, 0, 0, 3))

    override fun voicingsFor(canonicalName: String): List<ChordVoicing> =
        if (canonicalName == "C") listOf(C_VOICING) else emptyList()
}

private class FakeChordAudioSource : ChordAudioSource {
    val playedVoicings = mutableListOf<ChordVoicing>()
    val playedInstruments = mutableListOf<Instrument>()
    var released = false

    override fun play(voicing: ChordVoicing, instrument: Instrument) {
        playedVoicings += voicing
        playedInstruments += instrument
    }

    override fun release() {
        released = true
    }
}

private class FakeChordSelectionStore(initial: SavedSelection? = null) : ChordSelectionStore {
    private var current = initial
    val savedSelections = mutableListOf<SavedSelection>()

    override suspend fun load(): SavedSelection? = current

    override suspend fun save(selection: SavedSelection) {
        current = selection
        savedSelections += selection
    }
}
