package com.example.service

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.example.data.model.MusicTrackOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Real-time audio synthesizer for background music tracks (Inspired Ambient, Motivational Day,
 * Upbeat Corporate) and the signature "Metallic Swoosh" sound effect using native Android AudioTrack.
 */
class AudioSynthEngine {

    private val sampleRate = 44100
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var synthJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private var currentTrack: MusicTrackOption = MusicTrackOption.INSPIRED_AMBIENT
    private var masterVolume = 0.65f
    private var isDucked = false

    fun startTrack(track: MusicTrackOption) {
        if (track == MusicTrackOption.NONE) {
            stop()
            currentTrack = track
            return
        }

        currentTrack = track
        stop()

        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBufferSize, sampleRate * 2)

            audioTrack = AudioTrack.Builder()
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
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
        } catch (e: Throwable) {
            Log.w("AudioSynthEngine", "AudioTrack init skipped in headless/test environment: ${e.message}")
        }
        isPlaying = true

        synthJob = scope.launch {
            synthesizeLoop()
        }
    }

    fun setDucking(ducked: Boolean) {
        isDucked = ducked
        val vol = if (ducked) masterVolume * 0.22f else masterVolume
        try {
            audioTrack?.setVolume(vol)
        } catch (_: Exception) {}
    }

    /**
     * Safety Fallback: Guarantees that voiceover failures never leave the video in silence.
     * Restores music from ducking to full volume, or starts an audible ambient bed if music was stopped/none.
     */
    fun ensureAudibleSafetyBed() {
        setDucking(false)
        if (!isPlaying || currentTrack == MusicTrackOption.NONE) {
            Log.i("AudioSynthEngine", "Voice fail detected: Engaging audible fallback safety bed to prevent silence")
            startTrack(MusicTrackOption.INSPIRED_AMBIENT)
        }
    }

    fun isAudioActive(): Boolean = isPlaying

    fun stop() {
        isPlaying = false
        synthJob?.cancel()
        synthJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }

    /**
     * Synthesizes the signature "Metallic Swoosh" transition SFX:
     * A frequency sweep from 400Hz up to 2800Hz with high-harmonic resonance
     * and metallic shimmer, followed by a smooth velocity decay.
     */
    fun playMetallicSwoosh() {
        scope.launch {
            try {
                val sfxSampleRate = 44100
                val durationMs = 650
                val totalSamples = (sfxSampleRate * (durationMs / 1000f)).toInt()
                val buffer = ShortArray(totalSamples)

                for (i in 0 until totalSamples) {
                    val progress = i.toFloat() / totalSamples
                    // Exponential frequency sweep: start low, peak at 60%, then drop with metallic harmonics
                    val baseFreq = when {
                        progress < 0.6f -> 350f + (2600f * (progress / 0.6f))
                        else -> 2950f - (2200f * ((progress - 0.6f) / 0.4f))
                    }

                    // Multi-harmonic metallic ring (metallic sheen)
                    val t = i.toDouble() / sfxSampleRate
                    val wave1 = sin(2.0 * PI * baseFreq * t)
                    val wave2 = sin(2.0 * PI * (baseFreq * 1.58) * t) * 0.45 // inharmonic metal overtone
                    val wave3 = sin(2.0 * PI * (baseFreq * 2.76) * t) * 0.25 // high shimmer
                    val noise = ((Math.random() * 2.0 - 1.0) * 0.15) // air swoosh noise

                    // Envelope: fast rise, resonant tail
                    val env = when {
                        progress < 0.2f -> (progress / 0.2f)
                        progress < 0.5f -> 1.0f
                        else -> (1.0f - (progress - 0.5f) / 0.5f)
                    }

                    val sampleValue = (wave1 + wave2 + wave3 + noise) * env * 0.55
                    buffer[i] = (sampleValue.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
                }

                val sfxTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sfxSampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                sfxTrack.write(buffer, 0, buffer.size)
                sfxTrack.play()
                delay(durationMs.toLong() + 100)
                sfxTrack.stop()
                sfxTrack.release()
            } catch (e: Exception) {
                Log.e("AudioSynthEngine", "Error playing metallic swoosh: ${e.message}")
            }
        }
    }

    private suspend fun synthesizeLoop() {
        val chunkSamples = 2048
        val buffer = ShortArray(chunkSamples)
        var sampleIndex: Long = 0

        val chords = when (currentTrack) {
            MusicTrackOption.INSPIRED_AMBIENT -> listOf(
                // C minor9 -> Ab maj7 -> Bb sus4 -> Eb maj7 (Ambient atmospheric chords)
                listOf(130.81f, 196.00f, 246.94f, 311.13f, 392.00f),
                listOf(103.83f, 155.56f, 207.65f, 261.63f, 329.63f),
                listOf(116.54f, 174.61f, 233.08f, 293.66f, 349.23f),
                listOf(155.56f, 196.00f, 233.08f, 311.13f, 392.00f)
            )
            MusicTrackOption.MOTIVATIONAL_DAY -> listOf(
                // D major -> A major -> B minor -> G major (Uplifting pop energy)
                listOf(146.83f, 220.00f, 293.66f, 369.99f, 440.00f),
                listOf(110.00f, 164.81f, 220.00f, 277.18f, 329.63f),
                listOf(123.47f, 185.00f, 246.94f, 293.66f, 369.99f),
                listOf(98.00f, 146.83f, 196.00f, 246.94f, 293.66f)
            )
            MusicTrackOption.UPBEAT_CORPORATE -> listOf(
                // E major -> C# minor -> A major -> B major (Inspiring tech pulse)
                listOf(164.81f, 246.94f, 329.63f, 415.30f, 493.88f),
                listOf(138.59f, 207.65f, 277.18f, 329.63f, 415.30f),
                listOf(110.00f, 164.81f, 220.00f, 277.18f, 329.63f),
                listOf(123.47f, 185.00f, 246.94f, 311.13f, 369.99f)
            )
            MusicTrackOption.NONE -> emptyList()
        }

        if (chords.isEmpty()) return

        val chordDurationSamples = (sampleRate * 2.8).toInt() // 2.8 seconds per chord

        while (isPlaying && scope.isActive) {
            val chordIndex = ((sampleIndex / chordDurationSamples) % chords.size).toInt()
            val currentChord = chords[chordIndex]
            val chordProgress = (sampleIndex % chordDurationSamples).toFloat() / chordDurationSamples

            for (i in 0 until chunkSamples) {
                val t = (sampleIndex + i).toDouble() / sampleRate
                var sum = 0.0

                // Generate harmonic synth chord pad with gentle chorus LFO
                val lfo = 1.0 + 0.03 * sin(2.0 * PI * 0.45 * t)
                for ((noteIdx, freq) in currentChord.withIndex()) {
                    val weight = when (noteIdx) {
                        0 -> 0.35 // Bass foundation
                        1 -> 0.25 // Fifth
                        2 -> 0.20 // Third
                        else -> 0.15 // Ninth / octave shimmer
                    }
                    val detune = if (noteIdx % 2 == 0) 1.002 else 0.998
                    sum += sin(2.0 * PI * freq * detune * lfo * t) * weight
                }

                // Add subtle rhythmic arpeggiated melodic pulse for Motivational/Corporate
                if (currentTrack == MusicTrackOption.MOTIVATIONAL_DAY || currentTrack == MusicTrackOption.UPBEAT_CORPORATE) {
                    val arpStep = (((sampleIndex + i) / (sampleRate / 8)) % currentChord.size).toInt()
                    val arpFreq = currentChord[arpStep] * 2.0 // 1 octave higher
                    val arpEnv = sin(PI * (((sampleIndex + i) % (sampleRate / 8)).toDouble() / (sampleRate / 8))).coerceAtLeast(0.0)
                    sum += sin(2.0 * PI * arpFreq * t) * arpEnv * 0.18
                }

                // Soft attack and release envelope per chord
                val envelope = when {
                    chordProgress < 0.15f -> chordProgress / 0.15f
                    chordProgress > 0.85f -> (1.0f - chordProgress) / 0.15f
                    else -> 1.0f
                }

                val vol = if (isDucked) 0.16f else 0.42f
                val sampleValue = sum * envelope * vol
                buffer[i] = (sampleValue.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
            }

            sampleIndex += chunkSamples
            try {
                audioTrack?.write(buffer, 0, chunkSamples)
            } catch (_: Exception) {
                break
            }
        }
    }
}
