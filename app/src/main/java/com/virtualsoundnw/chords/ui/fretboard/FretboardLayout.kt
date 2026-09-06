package com.virtualsoundnw.chords.ui.fretboard

import com.virtualsoundnw.chords.voicing.ChordVoicing

/**
 * Pure geometry for drawing a [ChordVoicing] as a fixed-size fret window —
 * kept free of Compose/Android APIs so it's testable as a plain JVM unit.
 * [FretboardDiagramView] does the actual Canvas drawing from this.
 */
object FretboardLayout {
    /** How many fret rows the diagram shows at once — enough for any hand-playable shape. */
    const val FRET_WINDOW_SIZE = 5

    /**
     * The lowest fret shown. 1 (showing the nut) whenever the voicing fits
     * entirely within the first [FRET_WINDOW_SIZE] frets; otherwise shifted
     * up so the voicing's highest fret lands in the last row.
     */
    fun baseFret(frets: List<Int?>): Int {
        val maxFret = frets.filterNotNull().filter { it > 0 }.maxOrNull() ?: return 1
        return if (maxFret <= FRET_WINDOW_SIZE) 1 else maxFret - FRET_WINDOW_SIZE + 1
    }

    /** What to draw for one string, given the fret it's pressed at and the diagram's [baseFret]. */
    sealed interface StringMarker {
        /** Not played — drawn as "X" above the nut/top of the diagram. */
        data object Muted : StringMarker

        /** Played open — drawn as "O" above the nut/top of the diagram. */
        data object Open : StringMarker

        /** Pressed at fret [row] within the visible window, 0-indexed (0 = the first row shown). */
        data class Fretted(val row: Int) : StringMarker
    }

    fun markerFor(fret: Int?, baseFret: Int): StringMarker = when {
        fret == null -> StringMarker.Muted
        fret == 0 -> StringMarker.Open
        else -> StringMarker.Fretted(fret - baseFret)
    }

    /** A screen-reader-friendly description of [voicing], low string to high string. */
    fun describeVoicing(voicing: ChordVoicing): String {
        val strings = voicing.frets.joinToString(", ") { fret ->
            when (fret) {
                null -> "muted"
                0 -> "open"
                else -> "fret $fret"
            }
        }
        return "Fretboard diagram, strings low to high: $strings. Tap to play."
    }
}
