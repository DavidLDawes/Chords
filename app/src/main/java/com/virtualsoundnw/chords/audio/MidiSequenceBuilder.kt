package com.virtualsoundnw.chords.audio

import java.io.ByteArrayOutputStream

/**
 * Builds a minimal Standard MIDI File (format 0, single track) that plays a
 * chord: a Program Change to the target instrument, then every note in
 * [build]'s `midiNotes` struck together and held for a fixed duration. Kept
 * free of Android APIs — [MidiChordPlayer] hands the resulting bytes to
 * `MediaPlayer`, which decodes them with Android's built-in General-MIDI
 * synth — so the byte-level format logic here is plain-JVM testable.
 */
internal object MidiSequenceBuilder {
    private const val TICKS_PER_QUARTER_NOTE = 480
    private const val CHORD_DURATION_TICKS = TICKS_PER_QUARTER_NOTE * 3 // 3 beats
    private const val TEMPO_MICROSECONDS_PER_QUARTER_NOTE = 500_000 // 120 BPM
    private const val NOTE_VELOCITY = 100
    private const val CHANNEL = 0

    fun build(midiNotes: List<Int>, generalMidiProgram: Int): ByteArray {
        require(midiNotes.isNotEmpty()) { "A chord needs at least one note." }
        return headerChunk() + trackChunk(midiNotes, generalMidiProgram)
    }

    private fun headerChunk(): ByteArray = chunk(
        id = "MThd",
        data = byteArrayOf(
            0, 0, // format 0
            0, 1, // 1 track
            (TICKS_PER_QUARTER_NOTE shr 8).toByte(), TICKS_PER_QUARTER_NOTE.toByte(),
        ),
    )

    private fun trackChunk(midiNotes: List<Int>, generalMidiProgram: Int): ByteArray {
        val out = ByteArrayOutputStream()

        out.writeDelta(0)
        out.write(0xFF); out.write(0x51); out.write(0x03) // Set Tempo meta event
        out.write(TEMPO_MICROSECONDS_PER_QUARTER_NOTE shr 16)
        out.write(TEMPO_MICROSECONDS_PER_QUARTER_NOTE shr 8)
        out.write(TEMPO_MICROSECONDS_PER_QUARTER_NOTE)

        out.writeDelta(0)
        out.write(0xC0 or CHANNEL) // Program Change
        out.write(generalMidiProgram)

        midiNotes.forEach { note ->
            out.writeDelta(0)
            out.write(0x90 or CHANNEL) // Note On
            out.write(note)
            out.write(NOTE_VELOCITY)
        }

        midiNotes.forEachIndexed { index, note ->
            out.writeDelta(if (index == 0) CHORD_DURATION_TICKS else 0) // hold the chord, then release together
            out.write(0x80 or CHANNEL) // Note Off
            out.write(note)
            out.write(0)
        }

        out.writeDelta(0)
        out.write(0xFF); out.write(0x2F); out.write(0x00) // End of Track meta event

        return chunk("MTrk", out.toByteArray())
    }

    private fun chunk(id: String, data: ByteArray): ByteArray {
        val length = data.size
        val lengthBytes = byteArrayOf(
            (length ushr 24).toByte(), (length ushr 16).toByte(), (length ushr 8).toByte(), length.toByte(),
        )
        return id.toByteArray(Charsets.US_ASCII) + lengthBytes + data
    }

    private fun ByteArrayOutputStream.writeDelta(ticks: Int) = write(variableLengthQuantity(ticks))

    /** MIDI variable-length quantity: 7 data bits per byte, continuation bit set on every byte but the last. */
    internal fun variableLengthQuantity(value: Int): ByteArray {
        require(value >= 0)
        val sevenBitGroups = mutableListOf<Int>()
        var remaining = value
        do {
            sevenBitGroups.add(0, remaining and 0x7F)
            remaining = remaining ushr 7
        } while (remaining > 0)
        return ByteArray(sevenBitGroups.size) { index ->
            val group = sevenBitGroups[index]
            (if (index == sevenBitGroups.lastIndex) group else group or 0x80).toByte()
        }
    }
}
