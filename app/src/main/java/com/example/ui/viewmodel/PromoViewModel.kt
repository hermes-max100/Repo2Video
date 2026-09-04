package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PromoDatabase
import com.example.data.local.PromoRepository
import com.example.data.model.AspectRatioFormat
import com.example.data.model.BrandProfile
import com.example.data.model.EmotionalPreset
import com.example.data.model.MusicTrackOption
import com.example.data.model.NarrativeTemplate
import com.example.data.model.PromoProject
import com.example.data.model.PromoScene
import com.example.data.model.SceneVisualType
import com.example.data.model.TransitionStyle
import com.example.data.model.VoiceActor
import com.example.service.AudioSynthEngine
import com.example.service.GeminiPromoService
import com.example.service.VoiceoverManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class PromoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PromoRepository
    private val geminiService = GeminiPromoService()
    private val audioSynthEngine = AudioSynthEngine()
    private val voiceoverManager: VoiceoverManager

    val isElevenLabsConfigured: Boolean
        get() = voiceoverManager.isElevenLabsConfigured

    private val _isVoiceSynthesizing = MutableStateFlow(false)
    val isVoiceSynthesizing: StateFlow<Boolean> = _isVoiceSynthesizing.asStateFlow()

    init {
        val database = PromoDatabase.getDatabase(application)
        repository = PromoRepository(database.promoDao())

        voiceoverManager = VoiceoverManager(
            context = application,
            onSpeechStart = { _ ->
                audioSynthEngine.setDucking(true)
            },
            onSpeechDone = { _ ->
                audioSynthEngine.setDucking(false)
            },
            onSpeechDuration = { utteranceId, durationMs ->
                // Scene duration synchronization if needed
                Log.d("PromoViewModel", "Speech duration for $utteranceId: ${durationMs}ms")
            }
        )

        viewModelScope.launch {
            repository.seedDefaultsIfEmpty()
        }
    }

    val projects: StateFlow<List<PromoProject>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active project state
    private val _currentProject = MutableStateFlow<PromoProject?>(null)
    val currentProject: StateFlow<PromoProject?> = _currentProject.asStateFlow()

    // Active brand profile
    private val _brandProfile = MutableStateFlow(
        BrandProfile(
            name = "PromoVideo",
            tagline = "Turn any codebase into high-impact promo videos",
            description = "Point Claude Code or AI at your repo. Render landscape and portrait videos in seconds.",
            logoIcon = "movie_filter",
            primaryColorHex = "#6366F1",
            secondaryColorHex = "#8B5CF6",
            accentColorHex = "#F59E0B",
            techStack = listOf("TypeScript", "React", "Remotion", "ElevenLabs", "Tailwind"),
            keyFeatures = listOf(
                "One command generation",
                "Dual landscape & portrait renders",
                "5 emotional voiceover presets",
                "Automated brand color discovery"
            ),
            targetAudience = "Developers, founders & creators",
            repoPathOrUrl = "AKCodez/promo-video-skill"
        )
    )
    val brandProfile: StateFlow<BrandProfile> = _brandProfile.asStateFlow()

    // Creative Direction state
    private val _narrativeTemplate = MutableStateFlow(NarrativeTemplate.RAGE_HOOK)
    val narrativeTemplate: StateFlow<NarrativeTemplate> = _narrativeTemplate.asStateFlow()

    private val _targetDurationSeconds = MutableStateFlow(60)
    val targetDurationSeconds: StateFlow<Int> = _targetDurationSeconds.asStateFlow()

    private val _voiceActor = MutableStateFlow(VoiceActor.MATILDA)
    val voiceActor: StateFlow<VoiceActor> = _voiceActor.asStateFlow()

    private val _musicTrack = MutableStateFlow(MusicTrackOption.INSPIRED_AMBIENT)
    val musicTrack: StateFlow<MusicTrackOption> = _musicTrack.asStateFlow()

    private val _transitionStyle = MutableStateFlow(TransitionStyle.METALLIC_SWOOSH)
    val transitionStyle: StateFlow<TransitionStyle> = _transitionStyle.asStateFlow()

    private val _activeAspectRatio = MutableStateFlow(AspectRatioFormat.LANDSCAPE_16_9)
    val activeAspectRatio: StateFlow<AspectRatioFormat> = _activeAspectRatio.asStateFlow()

    // Scenes list for active project
    private val _scenes = MutableStateFlow<List<PromoScene>>(emptyList())
    val scenes: StateFlow<List<PromoScene>> = _scenes.asStateFlow()

    // Playback state
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _activeSceneIndex = MutableStateFlow(0)
    val activeSceneIndex: StateFlow<Int> = _activeSceneIndex.asStateFlow()

    // Async operations state
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Director AI chat history
    private val _directorChatHistory = MutableStateFlow<List<Pair<String, String>>>(
        listOf(
            "model" to "Hello! I'm your AI Promo Video Director. Point me at your repo or tell me what style you want — like '60s dark mode promo with Rage Hook narrative and metallic swoosh transitions'."
        )
    )
    val directorChatHistory: StateFlow<List<Pair<String, String>>> = _directorChatHistory.asStateFlow()

    init {
        // Load default scenes from sample
        val defaultProjects = PromoRepository.getSampleProjects()
        val first = defaultProjects.firstOrNull()
        if (first != null) {
            loadProject(first)
        }
    }

    fun loadProject(project: PromoProject) {
        _currentProject.value = project
        _brandProfile.value = BrandProfile(
            name = project.brandName,
            tagline = project.tagline,
            description = project.title,
            primaryColorHex = project.primaryColorHex,
            secondaryColorHex = project.secondaryColorHex,
            accentColorHex = project.accentColorHex,
            techStack = parseJsonList(project.techStackJson),
            keyFeatures = parseJsonList(project.featuresJson),
            repoPathOrUrl = project.repoUrl
        )

        _narrativeTemplate.value = try {
            NarrativeTemplate.valueOf(project.narrativeTemplate)
        } catch (_: Exception) { NarrativeTemplate.RAGE_HOOK }

        _targetDurationSeconds.value = project.targetDurationSeconds

        _voiceActor.value = try {
            VoiceActor.valueOf(project.voiceActor)
        } catch (_: Exception) { VoiceActor.MATILDA }

        _musicTrack.value = try {
            MusicTrackOption.valueOf(project.musicTrack)
        } catch (_: Exception) { MusicTrackOption.INSPIRED_AMBIENT }

        _transitionStyle.value = try {
            TransitionStyle.valueOf(project.transitionStyle)
        } catch (_: Exception) { TransitionStyle.METALLIC_SWOOSH }

        _activeAspectRatio.value = try {
            AspectRatioFormat.valueOf(project.activeAspectRatio)
        } catch (_: Exception) { AspectRatioFormat.LANDSCAPE_16_9 }

        _scenes.value = PromoRepository.deserializeScenes(project.scenesJson)
        _activeSceneIndex.value = 0
    }

    fun scanCodebase(repoInput: String) {
        viewModelScope.launch {
            _isScanning.value = true
            _statusMessage.value = "Phase 1: Scanning codebase for brand assets & features..."

            val result = geminiService.discoverBrand(repoInput)
            result.onSuccess { discovered ->
                _brandProfile.value = discovered
                _statusMessage.value = "Discovered: ${discovered.name} (${discovered.techStack.joinToString(", ")})"
                // Auto generate new script for discovered brand
                generateScript()
            }.onFailure { err ->
                _statusMessage.value = "Scan error: ${err.message}"
            }
            _isScanning.value = false
        }
    }

    fun generateScript() {
        viewModelScope.launch {
            _isGenerating.value = true
            _statusMessage.value = "Phase 2 & 3: Building Remotion scenes with ${_narrativeTemplate.value.title}..."

            val result = geminiService.generatePromoScript(
                brand = _brandProfile.value,
                template = _narrativeTemplate.value,
                durationSeconds = _targetDurationSeconds.value
            )

            result.onSuccess { generatedScenes ->
                _scenes.value = generatedScenes
                _activeSceneIndex.value = 0
                _statusMessage.value = "Generated ${generatedScenes.size} scenes ready for preview!"
                saveCurrentProject()
            }.onFailure { err ->
                _statusMessage.value = "Error generating script: ${err.message}"
            }
            _isGenerating.value = false
        }
    }

    fun updateBrandProfile(updated: BrandProfile) {
        _brandProfile.value = updated
    }

    fun setNarrativeTemplate(template: NarrativeTemplate) {
        _narrativeTemplate.value = template
    }

    fun setTargetDuration(seconds: Int) {
        _targetDurationSeconds.value = seconds
    }

    fun setVoiceActor(actor: VoiceActor) {
        _voiceActor.value = actor
        voiceoverManager.prefetchScenes(_scenes.value.map { it.voiceover to it.emotionalPreset }, actor)
    }

    fun setMusicTrack(track: MusicTrackOption) {
        _musicTrack.value = track
        if (_isPlaying.value) {
            audioSynthEngine.startTrack(track)
        }
    }

    fun setTransitionStyle(style: TransitionStyle) {
        _transitionStyle.value = style
    }

    fun setAspectRatio(format: AspectRatioFormat) {
        _activeAspectRatio.value = format
    }

    fun togglePlayback(playing: Boolean) {
        _isPlaying.value = playing
        if (playing) {
            audioSynthEngine.startTrack(_musicTrack.value)
            speakActiveScene()
        } else {
            audioSynthEngine.stop()
            voiceoverManager.stop()
        }
    }

    fun onSceneChanged(index: Int) {
        _activeSceneIndex.value = index
        if (_isPlaying.value) {
            speakActiveScene()
        }
    }

    private fun speakActiveScene() {
        val scene = _scenes.value.getOrNull(_activeSceneIndex.value) ?: return
        voiceoverManager.speak(
            text = scene.voiceover,
            voiceActor = _voiceActor.value,
            emotionalPreset = scene.emotionalPreset,
            onSynthesisState = { isElevenLabs, isGenerating ->
                _isVoiceSynthesizing.value = isGenerating
                if (isElevenLabs && isGenerating) {
                    _statusMessage.value = "🎙️ ElevenLabs: Generating HD voice for ${_voiceActor.value.voiceName}..."
                } else if (isElevenLabs && !isGenerating) {
                    _statusMessage.value = "🎙️ Playing ElevenLabs voice (${_voiceActor.value.voiceName})"
                }
            }
        )
    }

    fun playMetallicSwoosh() {
        audioSynthEngine.playMetallicSwoosh()
    }

    fun auditionVoice(actor: VoiceActor, preset: EmotionalPreset) {
        _statusMessage.value = "🎙️ ElevenLabs: Auditioning ${actor.voiceName}..."
        voiceoverManager.speak(
            text = preset.sampleQuote,
            voiceActor = actor,
            emotionalPreset = preset,
            onSynthesisState = { isElevenLabs, isGenerating ->
                _isVoiceSynthesizing.value = isGenerating
                if (!isGenerating && isElevenLabs) {
                    _statusMessage.value = "🎙️ ElevenLabs ${actor.voiceName} (${preset.label})"
                }
            }
        )
    }

    fun speakSceneText(text: String, actor: VoiceActor, preset: EmotionalPreset) {
        if (text.isBlank()) return
        _statusMessage.value = "🎙️ ElevenLabs: Synthesizing scene voiceover..."
        voiceoverManager.speak(
            text = text,
            voiceActor = actor,
            emotionalPreset = preset,
            onSynthesisState = { isElevenLabs, isGenerating ->
                _isVoiceSynthesizing.value = isGenerating
                if (!isGenerating && isElevenLabs) {
                    _statusMessage.value = "🎙️ Playing scene voiceover (${actor.voiceName})"
                }
            }
        )
    }

    fun updateScene(index: Int, updatedScene: PromoScene) {
        val current = _scenes.value.toMutableList()
        if (index in current.indices) {
            current[index] = updatedScene
            _scenes.value = current
            saveCurrentProject()
        }
    }

    fun addScene(scene: PromoScene) {
        val current = _scenes.value.toMutableList()
        current.add(scene)
        _scenes.value = current
        saveCurrentProject()
    }

    fun deleteScene(index: Int) {
        val current = _scenes.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            // Re-index
            val reindexed = current.mapIndexed { idx, s -> s.copy(orderIndex = idx) }
            _scenes.value = reindexed
            if (_activeSceneIndex.value >= reindexed.size) {
                _activeSceneIndex.value = (reindexed.size - 1).coerceAtLeast(0)
            }
            saveCurrentProject()
        }
    }

    fun saveCurrentProject() {
        viewModelScope.launch {
            val brand = _brandProfile.value
            val project = PromoProject(
                id = _currentProject.value?.id ?: 0,
                title = "${brand.name} Promo Video",
                tagline = brand.tagline,
                repoUrl = brand.repoPathOrUrl,
                brandName = brand.name,
                primaryColorHex = brand.primaryColorHex,
                secondaryColorHex = brand.secondaryColorHex,
                accentColorHex = brand.accentColorHex,
                techStackJson = JSONArray(brand.techStack).toString(),
                featuresJson = JSONArray(brand.keyFeatures).toString(),
                narrativeTemplate = _narrativeTemplate.value.name,
                targetDurationSeconds = _targetDurationSeconds.value,
                voiceActor = _voiceActor.value.name,
                musicTrack = _musicTrack.value.name,
                transitionStyle = _transitionStyle.value.name,
                scenesJson = PromoRepository.serializeScenes(_scenes.value),
                activeAspectRatio = _activeAspectRatio.value.name,
                updatedAt = System.currentTimeMillis()
            )
            val newId = repository.saveProject(project)
            _currentProject.value = project.copy(id = if (project.id == 0L) newId else project.id)
        }
    }

    fun deleteProject(project: PromoProject) {
        viewModelScope.launch {
            repository.deleteProject(project)
            if (_currentProject.value?.id == project.id) {
                val remaining = projects.value.filter { it.id != project.id }
                if (remaining.isNotEmpty()) {
                    loadProject(remaining.first())
                }
            }
        }
    }

    fun sendDirectorMessage(message: String) {
        if (message.isBlank()) return
        val currentHistory = _directorChatHistory.value.toMutableList()
        currentHistory.add("user" to message)
        _directorChatHistory.value = currentHistory

        viewModelScope.launch {
            val result = geminiService.chatWithDirector(
                history = currentHistory,
                userMessage = message,
                currentProject = _brandProfile.value
            )
            result.onSuccess { reply ->
                val updated = _directorChatHistory.value.toMutableList()
                updated.add("model" to reply)
                _directorChatHistory.value = updated

                // Parse quick intent from message
                val lower = message.lowercase()
                if (lower.contains("adam")) _voiceActor.value = VoiceActor.ADAM
                if (lower.contains("daniel")) _voiceActor.value = VoiceActor.DANIEL
                if (lower.contains("matilda")) _voiceActor.value = VoiceActor.MATILDA
                if (lower.contains("rachel")) _voiceActor.value = VoiceActor.RACHEL
                if (lower.contains("josh")) _voiceActor.value = VoiceActor.JOSH

                if (lower.contains("rage")) _narrativeTemplate.value = NarrativeTemplate.RAGE_HOOK
                if (lower.contains("demo")) _narrativeTemplate.value = NarrativeTemplate.DEMO_FIRST
                if (lower.contains("problem")) _narrativeTemplate.value = NarrativeTemplate.PROBLEM_STACK
                if (lower.contains("transformation")) _narrativeTemplate.value = NarrativeTemplate.TRANSFORMATION

                if (lower.contains("metallic") || lower.contains("swoosh")) {
                    _transitionStyle.value = TransitionStyle.METALLIC_SWOOSH
                    playMetallicSwoosh()
                }
            }
        }
    }

    fun generateVeoVideoForScene(sceneIndex: Int) {
        viewModelScope.launch {
            val scene = _scenes.value.getOrNull(sceneIndex) ?: return@launch
            _statusMessage.value = "Generating Veo 3 video clip for Scene ${sceneIndex + 1}..."

            val prompt = "${brandProfile.value.name} promo video background: ${scene.title}. ${scene.subtitle}. High resolution cinematic motion graphic, glowing neon elements, studio lighting."
            val result = geminiService.generateVeoVideo(prompt, _activeAspectRatio.value)

            result.onSuccess { opOrUrl ->
                val updatedScene = scene.copy(
                    visualType = SceneVisualType.VEO_AI_VIDEO,
                    generatedVideoUrl = opOrUrl
                )
                updateScene(sceneIndex, updatedScene)
                _statusMessage.value = "Veo video clip attached to Scene ${sceneIndex + 1}!"
            }.onFailure {
                _statusMessage.value = "Veo generated simulated animation."
            }
        }
    }

    private fun parseJsonList(jsonStr: String): List<String> {
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) list.add(array.getString(i))
        } catch (_: Exception) {}
        return list
    }

    override fun onCleared() {
        super.onCleared()
        audioSynthEngine.stop()
        voiceoverManager.release()
    }
}
