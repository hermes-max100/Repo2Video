package com.example.service

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.model.EmotionalPreset
import com.example.data.model.VoiceActor
import java.util.Locale

class VoiceoverManager(
    context: Context,
    private val onSpeechStart: (String) -> Unit = {},
    private val onSpeechDone: (String) -> Unit = {},
    private val onWordHighlight: (String, Int, Int) -> Unit = { _, _, _ -> }
) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingSpeech: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
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

    fun speak(
        text: String,
        voiceActor: VoiceActor,
        emotionalPreset: EmotionalPreset,
        utteranceId: String = "voiceover_${System.currentTimeMillis()}"
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

        if (isInitialized) {
            action()
        } else {
            pendingSpeech = action
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
