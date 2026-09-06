package com.virtualsoundnw.chords.ui.selector

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.virtualsoundnw.chords.audio.Instrument
import com.virtualsoundnw.chords.theme.ChordsTheme
import com.virtualsoundnw.chords.theory.ChordQuality
import com.virtualsoundnw.chords.theory.ChordSymbol
import com.virtualsoundnw.chords.theory.Note
import com.virtualsoundnw.chords.voicing.ChordVoicing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented Compose UI tests for the selector -> render flow, driving the
 * stateless [ChordSelectorScreen] overload directly (controlled state in,
 * recorded callbacks out) rather than the real ViewModel — that's already
 * covered by [ChordSelectionViewModelTest]'s plain-JVM tests. These tests
 * exist to catch UI-wiring regressions a unit test can't see: is the right
 * composable actually clickable, does the right callback actually fire.
 */
@RunWith(AndroidJUnit4::class)
class ChordSelectorScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun state(
        root: Note = Note.C,
        qualities: Set<ChordQuality> = emptySet(),
        instrument: Instrument = Instrument.GUITAR,
        voicing: ChordVoicing? = ChordVoicing(listOf(null, 3, 2, 0, 1, 0)),
    ) = ChordSelectionUiState(root, qualities, instrument, ChordSymbol.of(root, qualities).getOrThrow(), voicing)

    @Test
    fun displaysTheResolvedChordName() {
        composeRule.setContent {
            ChordsTheme {
                ChordSelectorScreen(
                    state = state(root = Note.A, qualities = setOf(ChordQuality.MINOR)),
                    onRootSelected = {},
                    onQualityToggled = {},
                    onInstrumentSelected = {},
                    onPlay = {},
                )
            }
        }

        composeRule.onNodeWithText("Am").assertIsDisplayed()
    }

    @Test
    fun clickingAQualityCheckboxInvokesTheCallbackWithThatQuality() {
        var toggled: ChordQuality? = null
        composeRule.setContent {
            ChordsTheme {
                ChordSelectorScreen(
                    state = state(),
                    onRootSelected = {},
                    onQualityToggled = { toggled = it },
                    onInstrumentSelected = {},
                    onPlay = {},
                )
            }
        }

        composeRule.onNodeWithText("Minor").performClick()

        assertEquals(ChordQuality.MINOR, toggled)
    }

    @Test
    fun clickingADisabledQualityCheckboxDoesNotInvokeTheCallback() {
        var toggled: ChordQuality? = null
        composeRule.setContent {
            ChordsTheme {
                ChordSelectorScreen(
                    // Aug selected -> Dim is disabled (same triad-quality group).
                    state = state(qualities = setOf(ChordQuality.AUGMENTED)),
                    onRootSelected = {},
                    onQualityToggled = { toggled = it },
                    onInstrumentSelected = {},
                    onPlay = {},
                )
            }
        }

        composeRule.onNodeWithText("Dim").performClick()

        assertEquals(null, toggled)
    }

    @Test
    fun selectingARootFromTheDropdownInvokesTheCallback() {
        var selected: Note? = null
        composeRule.setContent {
            ChordsTheme {
                ChordSelectorScreen(
                    state = state(),
                    onRootSelected = { selected = it },
                    onQualityToggled = {},
                    onInstrumentSelected = {},
                    onPlay = {},
                )
            }
        }

        composeRule.onNodeWithText("Root").performClick()
        composeRule.onNodeWithText("G").performClick()

        assertEquals(Note.G, selected)
    }

    @Test
    fun selectingUkuleleInvokesTheInstrumentCallback() {
        var selected: Instrument? = null
        composeRule.setContent {
            ChordsTheme {
                ChordSelectorScreen(
                    state = state(),
                    onRootSelected = {},
                    onQualityToggled = {},
                    onInstrumentSelected = { selected = it },
                    onPlay = {},
                )
            }
        }

        composeRule.onNodeWithText("Ukulele").performClick()

        assertEquals(Instrument.UKULELE, selected)
    }

    @Test
    fun clickingThePlayButtonInvokesOnPlay() {
        var played = false
        composeRule.setContent {
            ChordsTheme {
                ChordSelectorScreen(
                    state = state(root = Note.G),
                    onRootSelected = {},
                    onQualityToggled = {},
                    onInstrumentSelected = {},
                    onPlay = { played = true },
                )
            }
        }

        composeRule.onNodeWithText("Play G").performClick()

        assertTrue(played)
    }

    @Test
    fun tappingTheFretboardDiagramInvokesOnPlay() {
        var played = false
        composeRule.setContent {
            ChordsTheme {
                ChordSelectorScreen(
                    state = state(),
                    onRootSelected = {},
                    onQualityToggled = {},
                    onInstrumentSelected = {},
                    onPlay = { played = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Fretboard diagram", substring = true).performClick()

        assertTrue(played)
    }

    @Test
    fun anUncuratedChordShowsAFallbackMessageInsteadOfAFretboard() {
        composeRule.setContent {
            ChordsTheme {
                ChordSelectorScreen(
                    state = state(root = Note.B, qualities = setOf(ChordQuality.DIMINISHED), voicing = null),
                    onRootSelected = {},
                    onQualityToggled = {},
                    onInstrumentSelected = {},
                    onPlay = {},
                )
            }
        }

        composeRule.onNodeWithText("No guitar shape curated yet for Bdim").assertIsDisplayed()
    }
}
