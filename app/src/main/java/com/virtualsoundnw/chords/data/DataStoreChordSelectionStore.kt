package com.virtualsoundnw.chords.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.virtualsoundnw.chords.theory.ChordQuality
import com.virtualsoundnw.chords.theory.Note
import kotlinx.coroutines.flow.first

private val Context.chordSelectionDataStore by preferencesDataStore(name = "chord_selection")

/** [ChordSelectionStore] backed by Jetpack DataStore Preferences. */
class DataStoreChordSelectionStore(context: Context) : ChordSelectionStore {
    private val appContext = context.applicationContext

    override suspend fun load(): SavedSelection? {
        val preferences = appContext.chordSelectionDataStore.data.first()
        val root = preferences[ROOT_KEY]?.let { name -> Note.entries.find { it.name == name } } ?: return null
        val qualities = preferences[QUALITIES_KEY].orEmpty()
            .mapNotNull { name -> ChordQuality.entries.find { it.name == name } }
            .toSet()
        return SavedSelection(root, qualities)
    }

    override suspend fun save(selection: SavedSelection) {
        appContext.chordSelectionDataStore.edit { preferences ->
            preferences[ROOT_KEY] = selection.root.name
            preferences[QUALITIES_KEY] = selection.qualities.map { it.name }.toSet()
        }
    }

    private companion object {
        val ROOT_KEY: Preferences.Key<String> = stringPreferencesKey("root")
        val QUALITIES_KEY: Preferences.Key<Set<String>> = stringSetPreferencesKey("qualities")
    }
}
