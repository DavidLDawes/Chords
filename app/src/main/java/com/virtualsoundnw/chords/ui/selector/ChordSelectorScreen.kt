package com.virtualsoundnw.chords.ui.selector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.virtualsoundnw.chords.theme.ChordsTheme
import com.virtualsoundnw.chords.theory.ChordQuality
import com.virtualsoundnw.chords.theory.ChordSymbol
import com.virtualsoundnw.chords.theory.Note
import com.virtualsoundnw.chords.ui.fretboard.FretboardDiagramView
import com.virtualsoundnw.chords.voicing.GuitarVoicing
import com.virtualsoundnw.chords.voicing.GuitarVoicingRepository

@Composable
fun ChordSelectorScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: ChordSelectionViewModel = viewModel { ChordSelectionViewModel(GuitarVoicingRepository(context)) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChordSelectorScreen(
        state = state,
        onRootSelected = viewModel::selectRoot,
        onQualityToggled = viewModel::toggleQuality,
        modifier = modifier,
    )
}

@Composable
internal fun ChordSelectorScreen(
    state: ChordSelectionUiState,
    onRootSelected: (Note) -> Unit,
    onQualityToggled: (ChordQuality) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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
            FretboardDiagramView(voicing = voicing, modifier = Modifier.fillMaxWidth())
        } else {
            Text(
                text = "No guitar shape curated yet for ${state.chordSymbol.canonicalName}",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = quality in state.qualities,
                    onCheckedChange = { onToggle(quality) },
                    enabled = state.isQualityEnabled(quality),
                )
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
    val voicing = GuitarVoicing(listOf(null, 0, 2, 0, 1, 0))
    ChordsTheme {
        ChordSelectorScreen(
            state = ChordSelectionUiState(root, qualities, ChordSymbol.of(root, qualities).getOrThrow(), voicing),
            onRootSelected = {},
            onQualityToggled = {},
        )
    }
}
