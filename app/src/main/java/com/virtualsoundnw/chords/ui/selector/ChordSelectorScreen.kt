package com.virtualsoundnw.chords.ui.selector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.virtualsoundnw.chords.audio.Instrument
import com.virtualsoundnw.chords.audio.MidiChordPlayer
import com.virtualsoundnw.chords.data.DataStoreChordSelectionStore
import com.virtualsoundnw.chords.theme.ChordsTheme
import com.virtualsoundnw.chords.theory.ChordQuality
import com.virtualsoundnw.chords.theory.ChordSymbol
import com.virtualsoundnw.chords.theory.Note
import com.virtualsoundnw.chords.ui.fretboard.FretboardDiagramView
import com.virtualsoundnw.chords.voicing.ChordVoicing
import com.virtualsoundnw.chords.voicing.GuitarVoicingRepository
import com.virtualsoundnw.chords.voicing.UkuleleVoicingRepository

@Composable
fun ChordSelectorScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: ChordSelectionViewModel =
        viewModel {
            ChordSelectionViewModel(
                mapOf(
                    Instrument.GUITAR to GuitarVoicingRepository(context),
                    Instrument.UKULELE to UkuleleVoicingRepository(context),
                ),
                MidiChordPlayer(),
                DataStoreChordSelectionStore(context),
            )
        }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChordSelectorScreen(
        state = state,
        onRootSelected = viewModel::selectRoot,
        onQualityToggled = viewModel::toggleQuality,
        onInstrumentSelected = viewModel::selectInstrument,
        onPlay = viewModel::playCurrentChord,
        modifier = modifier,
    )
}

@Composable
internal fun ChordSelectorScreen(
    state: ChordSelectionUiState,
    onRootSelected: (Note) -> Unit,
    onQualityToggled: (ChordQuality) -> Unit,
    onInstrumentSelected: (Instrument) -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        InstrumentPicker(selected = state.instrument, onSelected = onInstrumentSelected)
        RootNoteDropdown(selected = state.root, onSelected = onRootSelected)
        QualityCheckboxGroup(state = state, onToggle = onQualityToggled)
        Text(
            text = state.chordSymbol.canonicalName,
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        val voicing = state.voicing
        if (voicing != null) {
            FretboardDiagramView(voicing = voicing, modifier = Modifier.fillMaxWidth(), onTap = onPlay)
            Button(onClick = onPlay, modifier = Modifier.fillMaxWidth()) {
                Text("Play ${state.chordSymbol.canonicalName}")
            }
        } else {
            Text(
                text = "No ${state.instrument.label.lowercase()} shape curated yet for ${state.chordSymbol.canonicalName}",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstrumentPicker(selected: Instrument, onSelected: (Instrument) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        Instrument.entries.forEachIndexed { index, instrument ->
            SegmentedButton(
                selected = instrument == selected,
                onClick = { onSelected(instrument) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = Instrument.entries.size),
            ) {
                Text(instrument.label)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RootNoteDropdown(selected: Note, onSelected: (Note) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        TextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Root") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Note.entries.forEach { note ->
                DropdownMenuItem(
                    text = { Text(note.displayName) },
                    onClick = {
                        onSelected(note)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun QualityCheckboxGroup(state: ChordSelectionUiState, onToggle: (ChordQuality) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        ChordQuality.entries.forEach { quality ->
            val checked = quality in state.qualities
            val enabled = state.isQualityEnabled(quality)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = checked,
                        enabled = enabled,
                        role = Role.Checkbox,
                        onValueChange = { onToggle(quality) },
                    ),
            ) {
                // onCheckedChange = null: the row's toggleable above already
                // handles the click and announces the checkbox role, so the
                // checkbox itself shouldn't also be an independent tap target.
                Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
                Text(quality.label)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChordSelectorScreenPreview() {
    val root = Note.A
    val qualities = setOf(ChordQuality.MINOR, ChordQuality.SEVENTH)
    val voicing = ChordVoicing(listOf(null, 0, 2, 0, 1, 0))
    ChordsTheme {
        ChordSelectorScreen(
            state = ChordSelectionUiState(root, qualities, Instrument.GUITAR, ChordSymbol.of(root, qualities).getOrThrow(), voicing),
            onRootSelected = {},
            onQualityToggled = {},
            onInstrumentSelected = {},
            onPlay = {},
        )
    }
}
