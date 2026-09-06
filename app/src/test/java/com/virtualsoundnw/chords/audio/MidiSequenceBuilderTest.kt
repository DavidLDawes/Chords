package com.virtualsoundnw.chords.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MidiSequenceBuilderTest {
    @Test
    fun `variableLengthQuantity encodes single-byte values unchanged`() {
        assertArrayEquals(byteArrayOf(0x00), MidiSequenceBuilder.variableLengthQuantity(0))
        assertArrayEquals(byteArrayOf(0x7F), MidiSequenceBuilder.variableLengthQuantity(127))
    }

    @Test
    fun `variableLengthQuantity sets the continuation bit past 7 bits`() {
        assertArrayEquals(byteArrayOf(0x81.toByte(), 0x00), MidiSequenceBuilder.variableLengthQuantity(128))
    }

    @Test
    fun `variableLengthQuantity encodes a multi-byte value correctly`() {
        // 1440 = 0x8B 0x20 -> (0x0B * 128) + 0x20 = 1408 + 32 = 1440
        assertArrayEquals(byteArrayOf(0x8B.toByte(), 0x20), MidiSequenceBuilder.variableLengthQuantity(1440))
    }

    @Test
    fun `build rejects an empty note list`() {
        assertThrows(IllegalArgumentException::class.java) { MidiSequenceBuilder.build(emptyList(), 24) }
    }

    @Test
    fun `build produces the exact expected bytes for a single note`() {
        // Hand-computed per the Standard MIDI File spec: header (format 0, 1
        // track, division 480) + one track containing a tempo meta event,
        // a program change, one Note On, one Note Off after 1440 ticks, and
        // an end-of-track meta event.
        val expected = byteArrayOf(
            // "MThd" length=6 format=0 ntrks=1 division=480(0x01E0)
            'M'.code.toByte(), 'T'.code.toByte(), 'h'.code.toByte(), 'd'.code.toByte(),
            0, 0, 0, 6,
            0, 0,
            0, 1,
            0x01, 0xE0.toByte(),
            // "MTrk" length=23
            'M'.code.toByte(), 'T'.code.toByte(), 'r'.code.toByte(), 'k'.code.toByte(),
            0, 0, 0, 23,
            // delta=0, Set Tempo (500000 us/quarter = 0x07A120)
            0x00, 0xFF.toByte(), 0x51, 0x03, 0x07, 0xA1.toByte(), 0x20,
            // delta=0, Program Change channel 0, program 24
            0x00, 0xC0.toByte(), 24,
            // delta=0, Note On channel 0, note 64, velocity 100
            0x00, 0x90.toByte(), 64, 100,
            // delta=1440 (0x8B 0x20), Note Off channel 0, note 64, velocity 0
            0x8B.toByte(), 0x20, 0x80.toByte(), 64, 0,
            // delta=0, End of Track
            0x00, 0xFF.toByte(), 0x2F, 0x00,
        )

        assertArrayEquals(expected, MidiSequenceBuilder.build(listOf(64), generalMidiProgram = 24))
    }

    @Test
    fun `build encodes every note as a simultaneous Note On and Note Off`() {
        val notes = listOf(45, 52, 55, 60, 64) // Am7
        val bytes = MidiSequenceBuilder.build(notes, generalMidiProgram = 24)
        val decoded = decodeTrackEvents(bytes)

        assertEquals(24, decoded.program)
        assertEquals(notes, decoded.noteOns)
        assertEquals(notes, decoded.noteOffs)
    }

    private data class DecodedTrack(val program: Int, val noteOns: List<Int>, val noteOffs: List<Int>)

    /** Minimal generic MIDI event decoder, used only to verify [MidiSequenceBuilder]'s output independently of its own construction logic. */
    private fun decodeTrackEvents(bytes: ByteArray): DecodedTrack {
        var pos = 14 // skip the 14-byte MThd chunk
        pos += 8 // skip the MTrk chunk header (id + length)

        fun readByte(): Int = bytes[pos++].toInt() and 0xFF
        fun readVlq(): Int {
            var value = 0
            while (true) {
                val b = readByte()
                value = (value shl 7) or (b and 0x7F)
                if (b and 0x80 == 0) return value
            }
        }

        var program = -1
        val noteOns = mutableListOf<Int>()
        val noteOffs = mutableListOf<Int>()

        while (pos < bytes.size) {
            readVlq() // delta time, unused here
            val status = readByte()
            when {
                status == 0xFF -> {
                    val type = readByte()
                    val length = readVlq()
                    pos += length
                    if (type == 0x2F) break // end of track
                }
                status and 0xF0 == 0xC0 -> program = readByte()
                status and 0xF0 == 0x90 -> {
                    val note = readByte()
                    readByte() // velocity
                    noteOns += note
                }
                status and 0xF0 == 0x80 -> {
                    val note = readByte()
                    readByte() // velocity
                    noteOffs += note
                }
                else -> error("Unexpected status byte 0x${status.toString(16)}")
            }
        }
        return DecodedTrack(program, noteOns, noteOffs)
    }
}
