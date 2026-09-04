package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.model.EmotionalPreset
import com.example.data.model.VoiceActor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Intelligent Voiceover Manager that prioritizes ElevenLabs studio-quality neural voices
 * with realistic emotion, dynamic intonation, and local audio caching,
 * with graceful fallback to Android TTS.
 */
class VoiceoverManager(
    private val context: Context,
    private val onSpeechStart: (String) -> Unit = {},
    private val onSpeechDone: (String) -> Unit = {},
    private val onWordHighlight: (String, Int, Int) -> Unit = { _, _, _ -> },
    private val onSpeechDuration: (String, Long) -> Unit = { _, _ -> }
) {

    val elevenLabsService = ElevenLabsVoiceService(context)

    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var pendingSpeech: (() -> Unit)? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var activeSpeechJob: Job? = null

    val isElevenLabsConfigured: Boolean
        get() = elevenLabsService.hasApiKey

    init {
        // Initialize Android TTS as fallback engine
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                tts?.language = Locale.US
                setupProgressListener()
                pendingSpeech?.invoke()
                pendingSpeech = null
            } else {
                Log.e("VoiceoverManager", "TTS initialization failed with status: $status")
            }
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                utteranceId?.let { onSpeechStart(it) }
            }

            override fun onDone(utteranceId: String?) {
                utteranceId?.let { onSpeechDone(it) }
            }

            override fun onError(utteranceId: String?) {
                utteranceId?.let { onSpeechDone(it) }
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                if (utteranceId != null) {
                    onWordHighlight(utteranceId, start, end)
                }
            }
        })
    }

    /**
     * Speaks the given script using ElevenLabs HD Neural Voice if available,
     * otherwise falls back smoothly to local TTS.
     */
    fun speak(
        text: String,
        voiceActor: VoiceActor,
        emotionalPreset: EmotionalPreset,
        utteranceId: String = "voiceover_${System.currentTimeMillis()}",
        onSynthesisState: (isElevenLabs: Boolean, isGenerating: Boolean) -> Unit = { _, _ -> }
    ) {
        if (text.isBlank()) {
            onSpeechDone(utteranceId)
            return
        }

        // Cancel previous speech job and stop active players
        stop()

        activeSpeechJob = scope.launch {
            if (elevenLabsService.hasApiKey) {
                onSynthesisState(true, true)
                val result = withContext(Dispatchers.IO) {
                    elevenLabsService.synthesizeSpeech(text, voiceActor, emotionalPreset)
                }

                if (result.isSuccess) {
                    val synthesisResult = result.getOrThrow()
                    onSynthesisState(true, false)
                    onSpeechDuration(utteranceId, synthesisResult.durationMs)
                    playAudioFile(synthesisResult.file, utteranceId)
                    return@launch
                } else {
                    Log.w("VoiceoverManager", "ElevenLabs synthesis failed, falling back to TTS: ${result.exceptionOrNull()?.message}")
                    onSynthesisState(false, false)
                }
            } else {
                onSynthesisState(false, false)
            }

            // Fallback to local TTS
            speakWithTts(text, voiceActor, emotionalPreset, utteranceId)
        }
    }

    private fun playAudioFile(audioFile: File, utteranceId: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(audioFile.absolutePath)
                setOnPreparedListener { mp ->
                    mp.start()
                    onSpeechStart(utteranceId)
                }
                setOnCompletionListener {
                    onSpeechDone(utteranceId)
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("VoiceoverManager", "MediaPlayer playback error: what=$what, extra=$extra")
                    onSpeechDone(utteranceId)
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("VoiceoverManager", "Failed to start MediaPlayer for ${audioFile.name}", e)
            onSpeechDone(utteranceId)
        }
    }

    private fun speakWithTts(
        text: String,
        voiceActor: VoiceActor,
        emotionalPreset: EmotionalPreset,
        utteranceId: String
    ) {
        val action: () -> Unit = {
            val calculatedPitch = (voiceActor.basePitch * emotionalPreset.pitchMultiplier).coerceIn(0.5f, 2.0f)
            val calculatedRate = (voiceActor.baseRate * emotionalPreset.rateMultiplier).coerceIn(0.5f, 2.0f)

            tts?.setPitch(calculatedPitch)
            tts?.setSpeechRate(calculatedRate)

            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            Unit
        }

        if (isTtsInitialized) {
            action()
        } else {
            pendingSpeech = action
        }
    }

    /**
     * Prefetches voiceover audio in the background for a series of scenes
     * to eliminate all latency during video playback.
     */
    fun prefetchScenes(
        scenes: List<Pair<String, EmotionalPreset>>,
        voiceActor: VoiceActor
    ) {
        if (!elevenLabsService.hasApiKey) return

        scope.launch(Dispatchers.IO) {
            scenes.forEach { (script, emotion) ->
                if (script.isNotBlank()) {
                    try {
                        elevenLabsService.synthesizeSpeech(script, voiceActor, emotion)
                    } catch (e: Exception) {
                        Log.d("VoiceoverManager", "Prefetch skipped or failed for scene: ${e.message}")
                    }
                }
            }
        }
    }

    fun stop() {
        activeSpeechJob?.cancel()
        activeSpeechJob = null

        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.reset()
        } catch (_: Exception) {}

        tts?.stop()
    }

    fun release() {
        stop()
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}

        tts?.shutdown()
        tts = null
    }
}

