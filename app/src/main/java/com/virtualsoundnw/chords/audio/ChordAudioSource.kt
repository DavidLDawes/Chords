package com.virtualsoundnw.chords.audio

import com.virtualsoundnw.chords.voicing.ChordVoicing

/** Plays a chord's voicing out loud. */
interface ChordAudioSource {
    fun play(voicing: ChordVoicing, instrument: Instrument = Instrument.GUITAR)

    /** Releases any playback resources. Call when the owner (e.g. a ViewModel) is done with this source. */
    fun release()
}
