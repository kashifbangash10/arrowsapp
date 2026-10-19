package com.amowpuzzle.aitricker.data.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/**
 * The background pad.
 *
 * Rather than shipping an audio file, this renders two seconds of a soft A-major
 * drone and hands it to [AudioTrack] as a hardware loop. Every partial completes a
 * whole number of cycles inside the buffer, so the seam is inaudible and the loop
 * costs nothing to keep running - once it is playing, no code and no CPU is involved
 * until the player turns it off.
 */
class MusicEngine {

    private val sampleRate = 22050
    private val seconds = 2
    private var track: AudioTrack? = null

    @Volatile
    var enabled: Boolean = true
        private set

    fun setEnabled(value: Boolean) {
        enabled = value
        if (value) start() else stop()
    }

    fun start() {
        if (!enabled || track != null) return
        val existing = runCatching { build() }.getOrNull() ?: return
        track = existing
        runCatching { existing.play() }
    }

    fun stop() {
        val current = track ?: return
        track = null
        runCatching {
            current.pause()
            current.flush()
            current.release()
        }
    }

    fun release() = stop()

    private fun build(): AudioTrack {
        val frames = sampleRate * seconds
        val buffer = ShortArray(frames)
        // A2, E3, A3, C#4 - each an exact number of cycles over two seconds.
        val partials = doubleArrayOf(110.0, 165.0, 220.0, 275.0)
        val weights = doubleArrayOf(0.42, 0.26, 0.20, 0.12)
        val peak = 0.085

        for (i in 0 until frames) {
            val t = i.toDouble() / sampleRate
            // One full LFO cycle across the buffer keeps the loop seam silent.
            val swell = 0.72 + 0.28 * sin(2.0 * PI * t / seconds)
            var value = 0.0
            for (p in partials.indices) {
                value += weights[p] * sin(2.0 * PI * partials[p] * t)
            }
            buffer[i] = (value * swell * peak * Short.MAX_VALUE)
                .coerceIn(-32768.0, 32767.0)
                .toInt()
                .toShort()
        }

        val created = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
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
        created.write(buffer, 0, buffer.size)
        // -1 loops forever; loop points are counted in frames, and this is mono.
        created.setLoopPoints(0, frames, -1)
        return created
    }
}
