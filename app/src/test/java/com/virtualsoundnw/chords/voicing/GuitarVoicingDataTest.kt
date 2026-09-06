package com.virtualsoundnw.chords.voicing

import com.virtualsoundnw.chords.theory.ChordQuality
import com.virtualsoundnw.chords.theory.ChordSymbol
import com.virtualsoundnw.chords.theory.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates the real bundled `chords/guitar_voicings.json` — not a synthetic
 * sample — against the chord each entry claims to be. Every fretted note is
 * checked to actually be a pitch class of that chord in standard tuning
 * (E A D G B E), so a typo'd fret number (the main risk called out in
 * PLAN.md's voicing-data section) fails a test instead of shipping a wrong
 * diagram.
 */
class GuitarVoicingDataTest {
    /** Open-string pitch classes, low E to high E (Note's ordinal doubles as its pitch class). */
    private val openStringPitchClasses = listOf(Note.E, Note.A, Note.D, Note.G, Note.B, Note.E).map { it.ordinal }

    /** What each bundled chord name is supposed to be: root + qualities. */
    private val expectedChords: Map<String, Pair<Note, Set<ChordQuality>>> = buildMap {
        val roots = Note.entries
        for (root in roots) {
            put(root.symbol, root to emptySet())
            put(root.symbol + "m", root to setOf(ChordQuality.MINOR))
            put(root.symbol + "7", root to setOf(ChordQuality.SEVENTH))
        }
        put("Dsus2", Note.D to setOf(ChordQuality.SUS2))
        put("Dsus4", Note.D to setOf(ChordQuality.SUS4))
        put("Asus2", Note.A to setOf(ChordQuality.SUS2))
        put("Asus4", Note.A to setOf(ChordQuality.SUS4))
        put("Esus4", Note.E to setOf(ChordQuality.SUS4))
        put("Csus4", Note.C to setOf(ChordQuality.SUS4))
        put("Gsus4", Note.G to setOf(ChordQuality.SUS4))
    }

    private fun loadBundledVoicings(): Map<String, List<GuitarVoicing>> {
        val stream = javaClass.classLoader!!.getResourceAsStream("chords/guitar_voicings.json")
            ?: error("chords/guitar_voicings.json not found on the test classpath")
        return GuitarVoicingParser.parse(stream.bufferedReader().use { it.readText() })
    }

    @Test
    fun `every bundled chord name matches a known root and quality combination`() {
        val voicings = loadBundledVoicings()
        for (name in voicings.keys) {
            assertTrue("Unrecognized chord name in guitar_voicings.json: $name", name in expectedChords)
        }
    }

    @Test
    fun `every expected chord is actually present in the bundled file`() {
        val voicings = loadBundledVoicings()
        for (name in expectedChords.keys) {
            assertTrue("Expected chord missing from guitar_voicings.json: $name", name in voicings)
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
