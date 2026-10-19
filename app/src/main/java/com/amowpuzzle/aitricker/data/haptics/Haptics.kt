package com.amowpuzzle.aitricker.data.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * The three taps of feedback the game asks for from the device.
 *
 * Kept behind an interface so the view model can be built and tested without a
 * Context, and so a device with no vibrator is not a special case anywhere.
 */
interface Haptics {

    var enabled: Boolean

    /** An arrow accepted the tap and is leaving. */
    fun tap()

    /** The tap was refused - a blocked arrow, or a button that cannot act. */
    fun reject()

    /** The level is clear. */
    fun success()

    /** Used in tests and previews. */
    object None : Haptics {
        override var enabled: Boolean = false
        override fun tap() = Unit
        override fun reject() = Unit
        override fun success() = Unit
    }
}

class SystemHaptics(context: Context) : Haptics {

    override var enabled: Boolean = true

    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()?.takeIf { it.hasVibrator() }

    override fun tap() = buzz(durationMs = 12, amplitude = 60)

    override fun reject() = buzz(durationMs = 42, amplitude = 150)

    override fun success() = pattern(longArrayOf(0, 18, 70, 26))

    private fun buzz(durationMs: Long, amplitude: Int) {
        val device = vibrator ?: return
        if (!enabled) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val strength = if (device.hasAmplitudeControl()) {
                    amplitude
                } else {
                    VibrationEffect.DEFAULT_AMPLITUDE
                }
                device.vibrate(VibrationEffect.createOneShot(durationMs, strength))
            } else {
                @Suppress("DEPRECATION")
                device.vibrate(durationMs)
            }
        }
    }

    private fun pattern(timings: LongArray) {
        val device = vibrator ?: return
        if (!enabled) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                device.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                @Suppress("DEPRECATION")
                device.vibrate(timings, -1)
            }
        }
    }
}
