package com.amowpuzzle.aitricker.data.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Generates the handful of tones the game needs instead of shipping audio files.
 *
 * Everything is deliberately quiet and short - the game is meant to be relaxing, so
 * even the "wrong move" cue is a soft low note rather than a buzzer.
 */
class SoundEngine {

    private val sampleRate = 44100
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    var enabled: Boolean = true

    /** An arrow leaving the board: a short, soft downward glide. */
    fun playEscape() = play(durationMs = 130, peak = 0.22) { progress ->
        val frequency = 540.0 - 260.0 * progress
        sin(2.0 * PI * frequency * progress * 0.13) * (1.0 - progress) * exp(-1.4 * progress)
    }

    /** A blocked tap: one low, rounded note. No harshness, no buzz. */
    fun playBlocked() = play(durationMs = 150, peak = 0.20) { progress ->
        sin(2.0 * PI * 196.0 * progress * 0.15) * exp(-4.5 * progress)
    }

    /** Hint: a gentle two-note lift. */
    fun playHint() = play(durationMs = 260, peak = 0.18) { progress ->
        val frequency = if (progress < 0.5) 784.0 else 1046.5
        sin(2.0 * PI * frequency * progress * 0.26) * (0.35 + 0.65 * sin(PI * progress))
    }

    /** Level complete: a soft major arpeggio. */
    fun playComplete() = play(durationMs = 620, peak = 0.24) { progress ->
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.5)
        val index = (progress * notes.size).toInt().coerceAtMost(notes.size - 1)
        val local = progress * notes.size - index
        sin(2.0 * PI * notes[index] * progress * 0.62) * exp(-2.2 * local)
    }

    /**
     * Renders [durationMs] of mono PCM from [wave], which is called with a 0..1
     * progress value and returns a sample in -1..1.
     */
    private fun play(durationMs: Int, peak: Double, wave: (Double) -> Double) {
        if (!enabled) return
        scope.launch {
            val samples = sampleRate * durationMs / 1000
            val buffer = ShortArray(samples)
            for (i in 0 until samples) {
                val value = wave(i.toDouble() / samples) * peak * Short.MAX_VALUE
                buffer[i] = value.coerceIn(-32768.0, 32767.0).toInt().toShort()
            }
            emit(buffer, durationMs)
        }
    }

    private suspend fun emit(buffer: ShortArray, durationMs: Int) {
        var track: AudioTrack? = null
        try {
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(buffer, 0, buffer.size)
            track.play()
            delay(durationMs + 90L)
        } catch (_: Exception) {
            // Audio is a nicety; never let a device quirk take the game down.
        } finally {
            runCatching {
                track?.stop()
                track?.release()
            }
        }
    }

    fun release() = scope.cancel()
}
