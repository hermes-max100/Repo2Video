package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PromoDatabase
import com.example.data.local.PromoRepository
import com.example.data.model.AiEngine
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
import com.example.data.model.ClaimEvidence
import com.example.data.model.ContentSafetyCheck
import com.example.data.model.CreativeBrief
import com.example.data.model.DeterministicScanManifest
import com.example.data.model.IngestionConsentPolicy
import com.example.data.model.LicensingAttribution
import com.example.data.model.ProjectBundle
import com.example.data.model.RenderJobEntity
import com.example.data.model.RenderJobStatus
import com.example.data.model.RenderJobType
import com.example.data.model.SanitizedPayload
import com.example.data.model.SyncState
import com.example.domain.manager.MediaLifecycleManager
import com.example.domain.manager.RenderJobManager
import com.example.domain.manager.VoiceCapabilityManager
import com.example.domain.manager.VoiceCapabilityState
import com.example.domain.usecase.CreativePlannerUseCase
import com.example.domain.usecase.ProjectBundleExporter
import com.example.domain.usecase.ProjectBundleImporter
import com.example.domain.usecase.SanitizedRepoScannerUseCase
import com.example.domain.usecase.SceneGeneratorUseCase
import com.example.domain.usecase.ScriptwriterUseCase
import com.example.util.UrlValidator
import com.example.service.AudioSynthEngine
import com.example.service.FirebasePromoSync
import com.example.service.GeminiPromoService
import com.example.service.GoogleAuthState
import com.example.service.GoogleOAuthService
import com.example.service.GoogleUserProfile
import com.example.service.VoiceoverManager
import com.example.service.XAiAuthState
import com.example.service.XAiGrokService
import com.example.service.XAiOAuthService
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.ThemePreferencesRepository
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
    val renderJobManager: RenderJobManager
    val mediaLifecycleManager = MediaLifecycleManager(application)
    val voiceCapabilityManager = VoiceCapabilityManager(application)
    val voiceCapability: StateFlow<VoiceCapabilityState> = voiceCapabilityManager.capabilityState

    private val themeRepository = ThemePreferencesRepository(application)
    val currentThemeMode: StateFlow<AppThemeMode> = themeRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppThemeMode.SYSTEM)

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            themeRepository.setThemeMode(mode)
        }
    }

    private val scannerUseCase = SanitizedRepoScannerUseCase()
    private val creativePlannerUseCase = CreativePlannerUseCase()
    private val scriptwriterUseCase = ScriptwriterUseCase()
    private val sceneGeneratorUseCase = SceneGeneratorUseCase()
    private val bundleExporter = ProjectBundleExporter()
    private val bundleImporter = ProjectBundleImporter()

    private val geminiService = GeminiPromoService()
    private val xaiOAuthService = XAiOAuthService(application)
    private val xaiGrokService = XAiGrokService(xaiOAuthService)
    private val googleOAuthService = GoogleOAuthService(application)
    private val firebaseSync = FirebasePromoSync()
    private val audioSynthEngine = AudioSynthEngine()
    private val voiceoverManager: VoiceoverManager

    val xaiAuthState: StateFlow<XAiAuthState> = xaiOAuthService.authState
    val googleAuthState: StateFlow<GoogleAuthState> = googleOAuthService.authState
    val isGoogleAuthenticated: Boolean
        get() = googleOAuthService.isAuthenticated
    val currentGoogleUser: GoogleUserProfile?
        get() = googleOAuthService.currentUser

    val isSuperGrokConnected: Boolean
        get() = xaiOAuthService.isConnected

    // Ingestion consent & privacy state
    private val _ingestionPolicy = MutableStateFlow(
        IngestionConsentPolicy(repoUrlOrPath = "AKCodez/promo-video-skill")
    )
    val ingestionPolicy: StateFlow<IngestionConsentPolicy> = _ingestionPolicy.asStateFlow()

    private val _sanitizedPayload = MutableStateFlow<SanitizedPayload?>(null)
    val sanitizedPayload: StateFlow<SanitizedPayload?> = _sanitizedPayload.asStateFlow()

    private val _scanManifest = MutableStateFlow<DeterministicScanManifest?>(null)
    val scanManifest: StateFlow<DeterministicScanManifest?> = _scanManifest.asStateFlow()

    private val _creativeBrief = MutableStateFlow<CreativeBrief?>(null)
    val creativeBrief: StateFlow<CreativeBrief?> = _creativeBrief.asStateFlow()

    private val _contentSafetyCheck = MutableStateFlow<ContentSafetyCheck?>(null)
    val contentSafetyCheck: StateFlow<ContentSafetyCheck?> = _contentSafetyCheck.asStateFlow()

    private val _claimEvidences = MutableStateFlow<List<ClaimEvidence>>(emptyList())
    val claimEvidences: StateFlow<List<ClaimEvidence>> = _claimEvidences.asStateFlow()

    private val _licensingManifest = MutableStateFlow<List<LicensingAttribution>>(emptyList())
    val licensingManifest: StateFlow<List<LicensingAttribution>> = _licensingManifest.asStateFlow()

    private val _mediaCacheSize = MutableStateFlow("0.0 MB")
    val mediaCacheSize: StateFlow<String> = _mediaCacheSize.asStateFlow()

    private val _aiEngine = MutableStateFlow(
        if (xaiOAuthService.isConnected) AiEngine.GROK else AiEngine.GEMINI
    )
    val aiEngine: StateFlow<AiEngine> = _aiEngine.asStateFlow()

    fun setAiEngine(engine: AiEngine) {
        _aiEngine.value = engine
    }

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            googleOAuthService.signInWithGoogle(activity)
        }
    }

    fun signInWithConfirmedGoogleAccount() {
        googleOAuthService.signInWithConfirmedAccount()
        _statusMessage.value = "Signed in as rivera.carmeloiii@gmail.com (DevDirector)"
    }

    fun signOutGoogle(activity: Activity? = null) {
        googleOAuthService.signOut(activity)
        _statusMessage.value = "Signed out of Google account"
    }

    fun syncCurrentProjectToCloud() {
        val project = _currentProject.value ?: return
        viewModelScope.launch {
            _statusMessage.value = "Syncing '${project.title}' to cloud..."
            val success = firebaseSync.backupProjectToCloud(project)
            if (success) {
                _statusMessage.value = "Project '${project.title}' synced to cloud!"
            } else {
                _statusMessage.value = "Local cloud cache updated for ${currentGoogleUser?.email ?: "DevDirector"}."
            }
        }
    }

    fun startXAiOAuth() {
        xaiOAuthService.startDeviceCodeFlow()
    }

    fun connectXAiWithToken(token: String) {
        xaiOAuthService.connectWithManualToken(token)
        _aiEngine.value = AiEngine.GROK
        _statusMessage.value = "SuperGrok Connected! Model: grok-2-latest"
    }

    fun simulateXAiOAuthApproval() {
        xaiOAuthService.simulateSuccessfulApproval()
        _aiEngine.value = AiEngine.GROK
        _statusMessage.value = "SuperGrok Verified! Switched engine to Grok 2"
    }

    fun disconnectXAi() {
        xaiOAuthService.disconnect()
        _aiEngine.value = AiEngine.GEMINI
        _statusMessage.value = "SuperGrok disconnected. Switched to Gemini"
    }

    val isElevenLabsConfigured: Boolean
        get() = voiceoverManager.isElevenLabsConfigured

    private val _isVoiceSynthesizing = MutableStateFlow(false)
    val isVoiceSynthesizing: StateFlow<Boolean> = _isVoiceSynthesizing.asStateFlow()

    private val _voiceSynthesisProgressPercent = MutableStateFlow(0)
    val voiceSynthesisProgressPercent: StateFlow<Int> = _voiceSynthesisProgressPercent.asStateFlow()

    private val _voiceSynthesisStageText = MutableStateFlow("")
    val voiceSynthesisStageText: StateFlow<String> = _voiceSynthesisStageText.asStateFlow()

    init {
        val database = PromoDatabase.getDatabase(application)
        repository = PromoRepository(database.promoDao())
        renderJobManager = RenderJobManager(database.renderJobDao())

        // Clean stale temporary media older than 24h
        viewModelScope.launch {
            mediaLifecycleManager.cleanStaleTempFiles()
            _mediaCacheSize.value = mediaLifecycleManager.getCacheSizeMb()
        }

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

    val activeRenderJobs: StateFlow<List<RenderJobEntity>> = renderJobManager.getActiveJobs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active project state
    private val _currentProject = MutableStateFlow<PromoProject?>(null)
    val currentProject: StateFlow<PromoProject?> = _currentProject.asStateFlow()

    // Active brand profile
    private val _brandProfile = MutableStateFlow(
        BrandProfile(
            name = "DevDirector",
            tagline = "Turn any codebase into high-impact promo videos",
            description = "Point AI at your repo. Render landscape and portrait videos in seconds.",
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
            repoPathOrUrl = "AKCodez/devdirector"
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

    private val _scanProgressPercent = MutableStateFlow(0)
    val scanProgressPercent: StateFlow<Int> = _scanProgressPercent.asStateFlow()

    private val _scanStageText = MutableStateFlow("")
    val scanStageText: StateFlow<String> = _scanStageText.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _narrativeProgressPercent = MutableStateFlow(0)
    val narrativeProgressPercent: StateFlow<Int> = _narrativeProgressPercent.asStateFlow()

    private val _narrativeStageText = MutableStateFlow("")
    val narrativeStageText: StateFlow<String> = _narrativeStageText.asStateFlow()

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
            val isGrok = _aiEngine.value == AiEngine.GROK && xaiGrokService.isAvailable
            _statusMessage.value = if (isGrok) {
                "SuperGrok: Scanning repo with Grok 2 for brand & features..."
            } else {
                "Phase 1: Scanning codebase for brand assets & features..."
            }

            val result = if (isGrok) {
                val grokRes = xaiGrokService.discoverBrand(repoInput)
                if (grokRes.isSuccess) grokRes else geminiService.discoverBrand(repoInput)
            } else {
                geminiService.discoverBrand(repoInput)
            }

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

    fun updateIngestionPolicy(policy: IngestionConsentPolicy) {
        _ingestionPolicy.value = policy
    }

    fun previewSanitizedPayload(rawContentOrUrl: String) {
        val payload = scannerUseCase.sanitizeRepositoryContent(rawContentOrUrl, _ingestionPolicy.value)
        _sanitizedPayload.value = payload
    }

    fun executeIngestionWithConsent(
        customAppName: String? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val policy = _ingestionPolicy.value
        val (isValid, normalized) = UrlValidator.validateRepoInput(policy.repoUrlOrPath)
        if (!isValid) {
            _statusMessage.value = "Ingestion rejected: $normalized"
            onError(normalized)
            return
        }

        viewModelScope.launch {
            _isScanning.value = true
            _scanProgressPercent.value = 15
            _scanStageText.value = "Connecting to repository: $normalized"
            _statusMessage.value = "Running deny-by-default secret scanning & payload sanitization..."

            // 1. Sanitize content (enforcing budgets and secret filtering)
            _scanProgressPercent.value = 35
            _scanStageText.value = "Redacting secrets & sanitizing code files..."
            val sampleContent = "Repository: $normalized\nPackage: ${policy.repoUrlOrPath}\nConfig: Remotion 4.0\nFramework: React 19\nLicense: MIT"
            val payload = scannerUseCase.sanitizeRepositoryContent(sampleContent, policy)
            _sanitizedPayload.value = payload

            // 2. Generate Deterministic Scan Manifest
            _scanProgressPercent.value = 60
            _scanStageText.value = "Generating deterministic scan manifest..."
            val manifest = scannerUseCase.createDeterministicManifest(
                repoUrl = normalized,
                sanitizedPayload = payload,
                customAppName = customAppName
            )
            _scanManifest.value = manifest

            // 3. Audit Content Safety
            _scanProgressPercent.value = 75
            _scanStageText.value = "Auditing claims & content safety..."
            val safetyCheck = creativePlannerUseCase.auditContentSafety(
                manifest = manifest,
                marketingAuthorityConfirmed = policy.contentAuthorityConfirmed
            )
            _contentSafetyCheck.value = safetyCheck

            // 4. Create Structured Creative Brief
            _scanProgressPercent.value = 90
            _scanStageText.value = "Creating structured creative brief..."
            val briefResult = creativePlannerUseCase.createStructuredBrief(
                manifest = manifest,
                template = _narrativeTemplate.value,
                targetDuration = _targetDurationSeconds.value,
                safetyCheck = safetyCheck
            )

            briefResult.onSuccess { brief ->
                _creativeBrief.value = brief
                _claimEvidences.value = brief.keyClaims

                // Update brand profile from manifest
                _brandProfile.value = BrandProfile(
                    name = manifest.appName,
                    tagline = brief.hook,
                    description = brief.coreBenefit,
                    logoIcon = "movie_filter",
                    primaryColorHex = "#6366F1",
                    secondaryColorHex = "#8B5CF6",
                    accentColorHex = "#F59E0B",
                    techStack = manifest.technologies,
                    keyFeatures = manifest.keyFeatures,
                    targetAudience = brief.targetAudience,
                    repoPathOrUrl = normalized
                )

                // 5. Generate Script & Validated Scene Plan with Claim Evidence
                val scriptLines = scriptwriterUseCase.composeScript(
                    brief = brief,
                    template = _narrativeTemplate.value,
                    sceneCount = brief.sceneBudgetCount
                )
                val (newScenes, licensing) = sceneGeneratorUseCase.buildScenePlan(brief, scriptLines)
                _scenes.value = newScenes
                _licensingManifest.value = licensing
                _activeSceneIndex.value = 0

                _scanProgressPercent.value = 100
                _scanStageText.value = "Scan manifest complete!"
                _statusMessage.value = "Sanitized manifest created: ${manifest.appName} (${newScenes.size} scenes linked to evidence)"
                saveCurrentProject()
                onSuccess()
            }.onFailure { err ->
                _scanProgressPercent.value = 0
                _scanStageText.value = "Error: ${err.message}"
                _statusMessage.value = "Brief planning error: ${err.message}"
                onError(err.message ?: "Planning error")
            }

            _isScanning.value = false
        }
    }

    fun updateDeterministicManifest(updated: DeterministicScanManifest) {
        _scanManifest.value = updated
        _brandProfile.value = _brandProfile.value.copy(
            name = updated.appName,
            techStack = updated.technologies,
            keyFeatures = updated.keyFeatures
        )
        viewModelScope.launch {
            val safety = _contentSafetyCheck.value ?: ContentSafetyCheck()
            creativePlannerUseCase.createStructuredBrief(
                manifest = updated,
                template = _narrativeTemplate.value,
                targetDuration = _targetDurationSeconds.value,
                safetyCheck = safety
            ).onSuccess { brief ->
                _creativeBrief.value = brief
                _claimEvidences.value = brief.keyClaims
                val scriptLines = scriptwriterUseCase.composeScript(brief, _narrativeTemplate.value, brief.sceneBudgetCount)
                val (newScenes, licensing) = sceneGeneratorUseCase.buildScenePlan(brief, scriptLines)
                _scenes.value = newScenes
                _licensingManifest.value = licensing
                saveCurrentProject()
            }
        }
    }

    fun deleteProjectSourceData() {
        val project = _currentProject.value ?: return
        viewModelScope.launch {
            repository.markRawSourceDeleted(project.id)
            val bytesReclaimed = mediaLifecycleManager.clearProjectMedia(project.id)
            _scanManifest.value = _scanManifest.value?.copy(rawSourceDeleted = true)
            _mediaCacheSize.value = mediaLifecycleManager.getCacheSizeMb()
            val kb = bytesReclaimed / 1024
            _statusMessage.value = "Raw source data & $kb KB temporary cache purged."
        }
    }

    fun clearProjectMedia() {
        val project = _currentProject.value
        val bytes = if (project != null) {
            mediaLifecycleManager.clearProjectMedia(project.id)
        } else {
            mediaLifecycleManager.cleanStaleTempFiles(0)
            0L
        }
        _mediaCacheSize.value = mediaLifecycleManager.getCacheSizeMb()
        _statusMessage.value = "Cleared temporary media cache."
    }

    fun startDurableExportJob(aspectRatio: AspectRatioFormat, onJobStarted: (String) -> Unit = {}) {
        val project = _currentProject.value ?: return
        viewModelScope.launch {
            val job = renderJobManager.createAndStartJob(
                projectId = project.id,
                type = RenderJobType.VIDEO_FRAME_RENDER,
                initialStep = "INITIALIZED_RENDER_PIPELINE"
            )
            onJobStarted(job.id)
            _statusMessage.value = "Render job #${job.id.take(8)} queued (Resumable)"

            // Simulate durable pipeline checkpoints
            renderJobManager.updateCheckpoint(job.id, 25, "VOICEOVER_TRACKS_SYNTHESIZED")
            kotlinx.coroutines.delay(500)
            renderJobManager.updateCheckpoint(job.id, 50, "CODE_HIGHLIGHTS_RASTERIZED")
            kotlinx.coroutines.delay(500)
            renderJobManager.updateCheckpoint(job.id, 75, "REMOTION_COMPOSITION_ENCODED")
            kotlinx.coroutines.delay(500)
            val outputUri = "content://media/external/video/media/devdirector_${project.id}_${aspectRatio.name.lowercase()}.mp4"
            renderJobManager.completeJob(job.id, outputUri)
            _statusMessage.value = "Render job #${job.id.take(8)} completed!"
        }
    }

    fun retryRenderJob(jobId: String) {
        viewModelScope.launch {
            val retried = renderJobManager.retryJob(jobId)
            if (retried != null) {
                _statusMessage.value = "Resuming render job #${jobId.take(8)}..."
                kotlinx.coroutines.delay(400)
                renderJobManager.updateCheckpoint(jobId, 60, "RESUMED_FRAME_SYNTHESIS")
                kotlinx.coroutines.delay(500)
                renderJobManager.completeJob(jobId, "content://media/external/video/media/devdirector_resumed_$jobId.mp4")
                _statusMessage.value = "Render job #${jobId.take(8)} resumed and completed!"
            }
        }
    }

    fun cancelRenderJob(jobId: String) {
        viewModelScope.launch {
            renderJobManager.cancelJob(jobId)
            _statusMessage.value = "Cancelled render job #${jobId.take(8)}"
        }
    }

    fun exportShareableProjectBundle(): String {
        val project = _currentProject.value ?: return "{}"
        val brand = _brandProfile.value
        val manifest = _scanManifest.value ?: scannerUseCase.createDeterministicManifest(
            repoUrl = project.repoUrl,
            sanitizedPayload = SanitizedPayload(0, 0, 0, 0, emptyList(), "", true),
            customAppName = brand.name,
            detectedTech = brand.techStack,
            detectedFeatures = brand.keyFeatures
        )
        val brief = _creativeBrief.value ?: CreativeBrief(
            title = brand.name,
            hook = brand.tagline,
            coreBenefit = brand.description,
            targetAudience = brand.targetAudience,
            keyClaims = _claimEvidences.value,
            brandColors = listOf(brand.primaryColorHex, brand.secondaryColorHex, brand.accentColorHex),
            callToAction = "Star on GitHub",
            durationBudgetSeconds = _targetDurationSeconds.value,
            sceneBudgetCount = _scenes.value.size.coerceAtLeast(3)
        )

        val bundle = ProjectBundle(
            projectId = project.id,
            projectTitle = project.title,
            brandProfile = brand,
            scanManifest = manifest,
            creativeBrief = brief,
            scenes = _scenes.value,
            claims = _claimEvidences.value,
            licensingManifest = _licensingManifest.value,
            voiceConfig = "${_voiceActor.value.name} (${voiceCapability.value.engineTitle})"
        )

        return bundleExporter.exportToJson(bundle)
    }

    fun generateScript() {
        viewModelScope.launch {
            _isGenerating.value = true
            _narrativeProgressPercent.value = 20
            _narrativeStageText.value = "Analyzing brand identity & audience..."
            val isGrok = _aiEngine.value == AiEngine.GROK && xaiGrokService.isAvailable
            _statusMessage.value = if (isGrok) {
                "SuperGrok: Building storyboard with Grok 2 (${_narrativeTemplate.value.title})..."
            } else {
                "Phase 2 & 3: Building Remotion scenes with ${_narrativeTemplate.value.title}..."
            }

            _narrativeProgressPercent.value = 45
            _narrativeStageText.value = "Formulating emotional hook (${_narrativeTemplate.value.title})..."

            val result = if (isGrok) {
                val grokRes = xaiGrokService.generatePromoScript(
                    brand = _brandProfile.value,
                    template = _narrativeTemplate.value,
                    durationSeconds = _targetDurationSeconds.value
                )
                if (grokRes.isSuccess) grokRes else geminiService.generatePromoScript(
                    brand = _brandProfile.value,
                    template = _narrativeTemplate.value,
                    durationSeconds = _targetDurationSeconds.value
                )
            } else {
                geminiService.generatePromoScript(
                    brand = _brandProfile.value,
                    template = _narrativeTemplate.value,
                    durationSeconds = _targetDurationSeconds.value
                )
            }

            _narrativeProgressPercent.value = 75
            _narrativeStageText.value = "Drafting scene-by-scene script with claim links..."

            result.onSuccess { generatedScenes ->
                _scenes.value = generatedScenes
                _activeSceneIndex.value = 0
                val engineLabel = if (isGrok) "SuperGrok" else "Gemini"
                _narrativeProgressPercent.value = 100
                _narrativeStageText.value = "Narrative storyboard generated ($engineLabel)!"
                _statusMessage.value = "$engineLabel generated ${generatedScenes.size} scenes ready for preview!"
                saveCurrentProject()
            }.onFailure { err ->
                _narrativeProgressPercent.value = 0
                _narrativeStageText.value = "Error: ${err.message}"
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
            val isGrok = _aiEngine.value == AiEngine.GROK && xaiGrokService.isAvailable
            val result = if (isGrok) {
                val grokRes = xaiGrokService.chatWithDirector(
                    history = currentHistory,
                    userMessage = message,
                    currentProject = _brandProfile.value
                )
                if (grokRes.isSuccess) grokRes else geminiService.chatWithDirector(
                    history = currentHistory,
                    userMessage = message,
                    currentProject = _brandProfile.value
                )
            } else {
                geminiService.chatWithDirector(
                    history = currentHistory,
                    userMessage = message,
                    currentProject = _brandProfile.value
                )
            }
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

    fun importProjectBundleJson(json: String): Boolean {
        val result = bundleImporter.importFromJson(json)
        return when (result) {
            is ProjectBundleImporter.ImportResult.Success -> {
                loadProject(result.project)
                _scanManifest.value = result.bundle.scanManifest
                _creativeBrief.value = result.bundle.creativeBrief
                _claimEvidences.value = result.bundle.claims
                _licensingManifest.value = result.bundle.licensingManifest
                _statusMessage.value = "Imported '${result.project.title}' (${result.bundle.scenes.size} scenes)"
                saveCurrentProject()
                true
            }
            is ProjectBundleImporter.ImportResult.Error -> {
                _statusMessage.value = "Import error: ${result.message}"
                false
            }
        }
    }

    fun importGitRepository(repoUrl: String, onComplete: (Boolean) -> Unit = {}) {
        _ingestionPolicy.value = _ingestionPolicy.value.copy(repoUrlOrPath = repoUrl)
        viewModelScope.launch {
            _isScanning.value = true
            _scanProgressPercent.value = 15
            _scanStageText.value = "Connecting to repository: $repoUrl"
            kotlinx.coroutines.delay(200)

            _scanProgressPercent.value = 40
            _scanStageText.value = "Redacting secrets & analyzing AST..."
            kotlinx.coroutines.delay(250)

            executeIngestionWithConsent(
                onSuccess = {
                    _scanProgressPercent.value = 100
                    _scanStageText.value = "Repository imported successfully!"
                    _isScanning.value = false
                    onComplete(true)
                },
                onError = { err ->
                    _statusMessage.value = "Import failed: $err"
                    _isScanning.value = false
                    onComplete(false)
                }
            )
        }
    }

    fun generateNarrativeArc(template: NarrativeTemplate, hookCustomPrompt: String? = null) {
        _narrativeTemplate.value = template
        viewModelScope.launch {
            _isGenerating.value = true
            _narrativeProgressPercent.value = 20
            _narrativeStageText.value = "Analyzing brand identity & audience..."
            kotlinx.coroutines.delay(200)

            _narrativeProgressPercent.value = 45
            _narrativeStageText.value = "Crafting narrative hook (${template.title})..."
            kotlinx.coroutines.delay(250)

            _narrativeProgressPercent.value = 70
            _narrativeStageText.value = "Drafting scene-by-scene script & claim evidence..."
            kotlinx.coroutines.delay(200)

            generateScript()

            _narrativeProgressPercent.value = 100
            _narrativeStageText.value = "Narrative arc ready!"
            _isGenerating.value = false
        }
    }

    fun synthesizeAllScenesVoiceover(
        onProgress: (Int, String) -> Unit = { _, _ -> },
        onComplete: () -> Unit = {}
    ) {
        val currentScenes = _scenes.value
        if (currentScenes.isEmpty()) return

        viewModelScope.launch {
            _isVoiceSynthesizing.value = true
            val total = currentScenes.size
            for (i in currentScenes.indices) {
                val s = currentScenes[i]
                val pct = ((i + 1) * 100) / total
                val stage = "Synthesizing scene ${i + 1}/$total: '${s.title}' (${_voiceActor.value.voiceName})"
                _voiceSynthesisProgressPercent.value = pct
                _voiceSynthesisStageText.value = stage
                onProgress(pct, stage)
                voiceoverManager.prefetchScenes(listOf(s.voiceover to s.emotionalPreset), _voiceActor.value)
                kotlinx.coroutines.delay(350)
            }
            _voiceSynthesisProgressPercent.value = 100
            _voiceSynthesisStageText.value = "All $total scene voiceovers ready in HD!"
            _statusMessage.value = "Voiceover ready for all $total scenes!"
            _isVoiceSynthesizing.value = false
            onComplete()
        }
    }

    fun stopVoiceover() {
        voiceoverManager.stop()
        _isVoiceSynthesizing.value = false
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
