package com.beatflow.app.util

import android.media.AudioManager
import android.media.ToneGenerator

object SoundPlayer {
    private var tone: ToneGenerator? = null

    fun beep() {
        try {
            if (tone == null) tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 50)
            tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
        } catch (_: Exception) { }
    }

    fun release() {
        tone?.release()
        tone = null
    }
}
