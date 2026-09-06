package com.virtualsoundnw.chords.voicing

import com.virtualsoundnw.chords.theory.ChordQuality
import com.virtualsoundnw.chords.theory.ChordSymbol
import com.virtualsoundnw.chords.theory.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates the real bundled `chords/ukulele_voicings.json` — not a
 * synthetic sample — against the chord each entry claims to be. Every
 * fretted note is checked to actually be a pitch class of that chord in
 * standard reentrant tuning (G4 C4 E4 A4), the same self-verification
 * approach [GuitarVoicingDataTest] uses.
 */
class UkuleleVoicingDataTest {
    /** Open-string pitch classes, physical string order (G C E A — reentrant, not pitch-ascending). */
    private val openStringPitchClasses = listOf(Note.G, Note.C, Note.E, Note.A).map { it.ordinal }

    /** What each bundled chord name is supposed to be: root + qualities. */
    private val expectedChords: Map<String, Pair<Note, Set<ChordQuality>>> = buildMap {
        for (root in Note.entries) {
            put(root.symbol, root to emptySet())
            put(root.symbol + "m", root to setOf(ChordQuality.MINOR))
            put(root.symbol + "7", root to setOf(ChordQuality.SEVENTH))
        }
    }

    private fun loadBundledVoicings(): Map<String, List<ChordVoicing>> {
        val stream = javaClass.classLoader!!.getResourceAsStream("chords/ukulele_voicings.json")
            ?: error("chords/ukulele_voicings.json not found on the test classpath")
        return ChordVoicingParser.parse(stream.bufferedReader().use { it.readText() })
    }

    @Test
    fun `every bundled chord name matches a known root and quality combination`() {
        val voicings = loadBundledVoicings()
        for (name in voicings.keys) {
            assertTrue("Unrecognized chord name in ukulele_voicings.json: $name", name in expectedChords)
        }
    }

    @Test
    fun `every expected chord is actually present in the bundled file`() {
        val voicings = loadBundledVoicings()
        for (name in expectedChords.keys) {
            assertTrue("Expected chord missing from ukulele_voicings.json: $name", name in voicings)
        }
    }

    @Test
    fun `every voicing has exactly 4 strings`() {
        val voicings = loadBundledVoicings()
        for ((name, chordVoicings) in voicings) {
            for (voicing in chordVoicings) {
                assertEquals("$name: expected 4 strings", 4, voicing.frets.size)
            }
        }
    }

    @Test
    fun `every fretted note in every voicing is a real chord tone`() {
        val voicings = loadBundledVoicings()
        for ((name, rootAndQualities) in expectedChords) {
            val (root, qualities) = rootAndQualities
            val symbol = ChordSymbol.of(root, qualities).getOrThrow()
            assertEquals("Table entry for \"$name\" doesn't match ChordSymbol's own naming", name, symbol.canonicalName)

            // ChordSymbol.pitchClasses are semitone offsets from the root, not
            // absolute pitch classes - transpose by the root before comparing
            // against what a fretted string actually sounds.
            val absolutePitchClasses = symbol.pitchClasses.map { (root.ordinal + it) % 12 }.toSet()

            val chordVoicings = voicings[name] ?: error("No voicing curated for $name")
            for (voicing in chordVoicings) {
                voicing.frets.forEachIndexed { stringIndex, fret ->
                    if (fret == null) return@forEachIndexed
                    val soundingPitchClass = (openStringPitchClasses[stringIndex] + fret) % 12
                    assertTrue(
                        "$name: string $stringIndex fretted at $fret sounds pitch class " +
                            "$soundingPitchClass, which isn't in $absolutePitchClasses (${symbol.canonicalName})",
                        soundingPitchClass in absolutePitchClasses,
                    )
                }
            }
        }
    }

    @Test
    fun `every voicing includes the root note somewhere`() {
        val voicings = loadBundledVoicings()
        for ((name, rootAndQualities) in expectedChords) {
            val (root, _) = rootAndQualities
            val rootPitchClass = root.ordinal
            val chordVoicings = voicings[name] ?: error("No voicing curated for $name")
            for (voicing in chordVoicings) {
                val includesRoot = voicing.frets.withIndex().any { (stringIndex, fret) ->
                    fret != null && (openStringPitchClasses[stringIndex] + fret) % 12 == rootPitchClass
                }
                assertTrue("$name: no string actually plays the root note", includesRoot)
            }
        }
    }
}
