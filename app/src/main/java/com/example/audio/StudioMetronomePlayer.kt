package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Synthétiseur de métronome mobile (AudioTrack PCM 44.1kHz) synchronisé sur 16 doubles-croches (4 temps).
 * - Temps forts (cases 0, 4, 8, 12) : clic aigu prononcé (1150 Hz)
 * - Cases actives du flow : clic léger bois/rim (760 Hz)
 */
class StudioMetronomePlayer {

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentStep = MutableStateFlow(-1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private var playJob: Job? = null

    private val sampleRate = 44100
    private val accentTickPcm: ShortArray by lazy { generateWoodClick(1150.0, 42, 0.85) }
    private val beatTickPcm: ShortArray by lazy { generateWoodClick(860.0, 32, 0.55) }
    private val stepTickPcm: ShortArray by lazy { generateWoodClick(640.0, 22, 0.35) }

    fun start(
        scope: CoroutineScope,
        bpm: Int,
        activeSteps: Set<Int> = setOf(0, 4, 8, 12)
    ) {
        stop()
        val safeBpm = bpm.coerceIn(50, 200)
        val sixteenthMs = (60_000.0 / safeBpm / 4.0).toLong().coerceAtLeast(45L)
        _isPlaying.value = true

        playJob = scope.launch(Dispatchers.Default) {
            var track: AudioTrack? = null
            try {
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(accentTickPcm.size * 2)

                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
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
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setBufferSizeInBytes(minBuf)
                    .build()

                track.play()
                var step = 0
                while (isActive && _isPlaying.value) {
                    val stepStart = System.currentTimeMillis()
                    _currentStep.value = step

                    val pcm = when {
                        step == 0 -> accentTickPcm
                        step % 4 == 0 -> beatTickPcm
                        step in activeSteps -> stepTickPcm
                        else -> null
                    }
                    if (pcm != null) {
                        track.write(pcm, 0, pcm.size, AudioTrack.WRITE_NON_BLOCKING)
                    }

                    step = (step + 1) % 16
                    val elapsed = System.currentTimeMillis() - stepStart
                    val waitMs = (sixteenthMs - elapsed).coerceAtLeast(10L)
                    delay(waitMs)
                }
            } catch (_: Throwable) {
                // Gracefully stop if audio hardware is unavailable (e.g., headless environment)
            } finally {
                _isPlaying.value = false
                _currentStep.value = -1
                try {
                    track?.pause()
                    track?.flush()
                    track?.stop()
                    track?.release()
                } catch (_: Throwable) {
                }
            }
        }
    }

    fun stop() {
        _isPlaying.value = false
        _currentStep.value = -1
        playJob?.cancel()
        playJob = null
    }

    private fun generateWoodClick(freqHz: Double, durationMs: Int, gain: Double): ShortArray {
        val count = (sampleRate * durationMs) / 1000
        return ShortArray(count) { i ->
            val t = i.toDouble() / sampleRate
            val env = exp(-i.toDouble() / (count * 0.24))
            val wave = sin(2.0 * PI * freqHz * t) + 0.35 * sin(2.0 * PI * freqHz * 1.9 * t)
            (wave * env * gain * 0.65 * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
    }
}
