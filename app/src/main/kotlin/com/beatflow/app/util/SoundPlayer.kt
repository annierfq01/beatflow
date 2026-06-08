package com.beatflow.app.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

object SoundPlayer {
    private var tone: ToneGenerator? = null
    private var toneFailed = false

    fun beep(context: Context? = null) {
        try {
            if (tone == null && !toneFailed) {
                tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 50)
            }
            tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
        } catch (_: Exception) {
            if (!toneFailed) {
                toneFailed = true
                tone?.release()
                tone = null
            }
            context?.let { vibrate(it) }
        }
    }

    private fun vibrate(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(200)
        }
    }

    fun release() {
        tone?.release()
        tone = null
        toneFailed = false
    }
}
