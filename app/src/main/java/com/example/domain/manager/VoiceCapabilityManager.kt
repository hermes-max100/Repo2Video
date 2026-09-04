package com.example.domain.manager

import android.content.Context
import android.speech.tts.TextToSpeech
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceEngineType {
    ELEVENLABS_NEURAL,
    ANDROID_SYSTEM_TTS,
    PROCEDURAL_SYNTH
}

data class VoiceCapabilityState(
    val activeEngine: VoiceEngineType,
    val engineTitle: String,
    val isFallback: Boolean,
    val fallbackReason: String?,
    val isApiKeyPresent: Boolean,
    val supportedActors: List<String>,
    val requiresCloud: Boolean
)

class VoiceCapabilityManager(context: Context) {

    private val _capabilityState = MutableStateFlow(computeInitialState())
    val capabilityState: StateFlow<VoiceCapabilityState> = _capabilityState.asStateFlow()

    private var localTts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        try {
            localTts = TextToSpeech(context.applicationContext) { status ->
                isTtsReady = status == TextToSpeech.SUCCESS
                if (isTtsReady) {
                    localTts?.language = Locale.US
                }
                refreshCapabilities()
            }
        } catch (_: Exception) {
            isTtsReady = false
        }
    }

    private fun computeInitialState(): VoiceCapabilityState {
        val elevenLabsKey = try {
            BuildConfig.ELEVENLABS_API_KEY
        } catch (_: Exception) { "" }

        val hasKey = elevenLabsKey.isNotBlank() && elevenLabsKey != "MY_ELEVENLABS_API_KEY"

        return if (hasKey) {
            VoiceCapabilityState(
                activeEngine = VoiceEngineType.ELEVENLABS_NEURAL,
                engineTitle = "ElevenLabs Neural Pro",
                isFallback = false,
                fallbackReason = null,
                isApiKeyPresent = true,
                supportedActors = listOf("Matilda", "Rachel", "Adam", "Antoni", "Josh", "Bella"),
                requiresCloud = true
            )
        } else {
            VoiceCapabilityState(
                activeEngine = VoiceEngineType.ANDROID_SYSTEM_TTS,
                engineTitle = "Local Android TTS Engine",
                isFallback = true,
                fallbackReason = "ElevenLabs API key not configured. Using local zero-latency Android Text-to-Speech.",
                isApiKeyPresent = false,
                supportedActors = listOf("System Natural (US)", "System Studio (UK)", "System Tech (Neural)"),
                requiresCloud = false
            )
        }
    }

    fun refreshCapabilities() {
        val state = computeInitialState()
        _capabilityState.value = state
    }

    fun forceEngine(engine: VoiceEngineType) {
        val current = _capabilityState.value
        _capabilityState.value = current.copy(
            activeEngine = engine,
            engineTitle = when (engine) {
                VoiceEngineType.ELEVENLABS_NEURAL -> "ElevenLabs Neural Pro"
                VoiceEngineType.ANDROID_SYSTEM_TTS -> "Local Android TTS Engine"
                VoiceEngineType.PROCEDURAL_SYNTH -> "Procedural Synthesizer"
            },
            isFallback = engine != VoiceEngineType.ELEVENLABS_NEURAL,
            fallbackReason = if (engine == VoiceEngineType.ANDROID_SYSTEM_TTS) "User selected offline-first Android TTS" else null
        )
    }
}
