package com.nestordelrio.generala

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/** Vibración según el momento del juego: tocar un dado, tirar, anotar y ganar. */
object Haptics {
    fun perform(view: View, kind: String) {
        when (kind) {
            "tap" -> view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            "score" -> view.performHapticFeedback(
                if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM
                else HapticFeedbackConstants.VIRTUAL_KEY
            )
            "roll" -> vibrate(view.context, longArrayOf(0, 12, 40, 12, 40, 18), intArrayOf(0, 90, 0, 120, 0, 160))
            "win" -> vibrate(view.context, longArrayOf(0, 30, 60, 30, 60, 80), intArrayOf(0, 160, 0, 200, 0, 255))
            else -> view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    private fun vibrate(context: Context, timings: LongArray, amplitudes: IntArray) {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator == null || !vibrator.hasVibrator()) return
        val effect = if (vibrator.hasAmplitudeControl()) {
            VibrationEffect.createWaveform(timings, amplitudes, -1)
        } else {
            VibrationEffect.createWaveform(timings, -1)
        }
        vibrator.vibrate(effect)
    }
}
