package com.beatflow.app.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

object SoundPlayer {
    @Volatile
    private var tone: ToneGenerator? = null
    @Volatile
    private var toneReady = false

    fun init() {
        if (toneReady) return
        Thread {
            try {
                tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 50)
                toneReady = true
            } catch (_: Exception) {
                toneReady = true
            }
        }.apply { isDaemon = true }.start()
    }

    fun beep(context: Context?) {
        try {
            context?.let { vibrate(it) }
        } catch (_: Exception) { }
        if (toneReady && tone != null) {
            Thread {
                try {
                    tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                } catch (_: Exception) {
                    try { tone?.release() } catch (_: Exception) { }
                    tone = null
                    toneReady = false
                }
            }.apply { isDaemon = true }.start()
        }
    }

    private fun vibrate(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
    }

    fun release() {
        try {
            tone?.release()
        } catch (_: Exception) { }
        tone = null
        toneReady = false
    }
}
