package com.virtualsoundnw.chords.audio

import android.media.MediaDataSource
import android.media.MediaPlayer
import com.virtualsoundnw.chords.voicing.GuitarVoicing

/**
 * Plays a chord by building a tiny in-memory MIDI file ([MidiSequenceBuilder])
 * and handing it to [MediaPlayer], which decodes MIDI using Android's
 * built-in Sonivox General-MIDI synth — no audio samples of our own are
 * needed. See CLAUDE.md's Audio decision for why this approach was chosen
 * over recorded samples.
 */
class MidiChordPlayer : ChordAudioSource {
    private var mediaPlayer: MediaPlayer? = null

    override fun play(voicing: GuitarVoicing, instrument: Instrument) {
        val midiNotes = StandardGuitarTuning.midiNotes(voicing)
        if (midiNotes.isEmpty()) return

        release()
        val bytes = MidiSequenceBuilder.build(midiNotes, instrument.generalMidiProgram)
        mediaPlayer = MediaPlayer().apply {
            setDataSource(InMemoryMidiDataSource(bytes))
            setOnPreparedListener { it.start() }
            setOnCompletionListener { release() }
            prepareAsync()
        }
    }

    override fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

private class InMemoryMidiDataSource(private val data: ByteArray) : MediaDataSource() {
    override fun close() = Unit

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (position >= data.size) return -1
        val length = minOf(size.toLong(), data.size - position).toInt()
        System.arraycopy(data, position.toInt(), buffer, offset, length)
        return length
    }

    override fun getSize(): Long = data.size.toLong()
}
