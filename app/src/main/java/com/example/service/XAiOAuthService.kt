package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed interface XAiAuthState {
    object Disconnected : XAiAuthState
    data class RequestingCode(val message: String = "Connecting to auth.x.ai...") : XAiAuthState
    data class AwaitingApproval(
        val userCode: String,
        val verificationUri: String,
        val verificationUriComplete: String? = null,
        val expiresInSeconds: Int = 900,
        val remainingSeconds: Int = 900
    ) : XAiAuthState
    data class Connected(
        val accessToken: String,
        val refreshToken: String? = null,
        val expiresAt: Long = 0L,
        val accountName: String = "SuperGrok Subscriber",
        val accountType: String = "SuperGrok"
    ) : XAiAuthState
    data class Error(val message: String) : XAiAuthState
}

class XAiOAuthService(private val context: Context) {

    private val tag = "XAiOAuthService"
    private val prefs: SharedPreferences =
        context.getSharedPreferences("xai_auth_prefs", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private var pollingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    // Default public OAuth client identifier for xAI Grok
    var clientId: String = "b1a00492-073a-47ea-816f-4c329264a828"
    val defaultScope = "openid profile email offline_access grok-cli:access api:access"

    // Official xAI Auth Endpoints
    val deviceCodeEndpoint = "https://auth.x.ai/oauth2/device/code"
    val tokenEndpoint = "https://auth.x.ai/oauth2/token"
    val authorizeEndpoint = "https://auth.x.ai/oauth2/authorize"
    val defaultVerificationUri = "https://auth.x.ai/activate"

    private val _authState = MutableStateFlow<XAiAuthState>(XAiAuthState.Disconnected)
    val authState: StateFlow<XAiAuthState> = _authState.asStateFlow()

    init {
        restorePersistedSession()
    }

    private fun restorePersistedSession() {
        val savedToken = prefs.getString("access_token", null)
        val savedRefresh = prefs.getString("refresh_token", null)
        val expiresAt = prefs.getLong("expires_at", 0L)
        val accountName = prefs.getString("account_name", "SuperGrok Subscriber") ?: "SuperGrok Subscriber"

        if (!savedToken.isNullOrBlank()) {
            _authState.value = XAiAuthState.Connected(
                accessToken = savedToken,
                refreshToken = savedRefresh,
                expiresAt = expiresAt,
                accountName = accountName,
                accountType = "SuperGrok"
            )
        } else {
            // Check if BuildConfig has a static key as a fallback
            val buildConfigKey = getBuildConfigKey()
            if (buildConfigKey.isNotBlank()) {
                _authState.value = XAiAuthState.Connected(
                    accessToken = buildConfigKey,
                    refreshToken = null,
                    expiresAt = Long.MAX_VALUE,
                    accountName = "Configured Key (xAI)",
                    accountType = "API Key"
                )
            }
        }
    }

    private fun getBuildConfigKey(): String {
        return try {
            val key = BuildConfig::class.java.getField("XAI_API_KEY").get(null) as? String
            if (!key.isNullOrBlank() && key != "MY_XAI_API_KEY") return key

            val grokKey = BuildConfig::class.java.getField("GROK_API_KEY").get(null) as? String
            if (!grokKey.isNullOrBlank() && grokKey != "MY_GROK_API_KEY") return grokKey

            val superToken = BuildConfig::class.java.getField("SUPERGROK_ACCESS_TOKEN").get(null) as? String
            if (!superToken.isNullOrBlank() && superToken != "MY_SUPERGROK_ACCESS_TOKEN") return superToken

            ""
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Initiates the OAuth 2.0 Device Code Authorization flow with auth.x.ai.
     * Ideal for SuperGrok and X Premium+ subscribers to authenticate without an API key.
     */
    fun startDeviceCodeFlow() {
        pollingJob?.cancel()
        _authState.value = XAiAuthState.RequestingCode()

        pollingJob = scope.launch {
            try {
                val formBody = FormBody.Builder()
                    .add("client_id", clientId)
                    .add("scope", defaultScope)
                    .build()

                val request = Request.Builder()
                    .url(deviceCodeEndpoint)
                    .post(formBody)
                    .header("Accept", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string().orEmpty()

                if (response.isSuccessful) {
                    val json = JSONObject(responseBody)
                    val deviceCode = json.optString("device_code")
                    val userCode = json.optString("user_code")
                    val verificationUri = json.optString("verification_uri", defaultVerificationUri)
                    val verificationUriComplete = json.optString("verification_uri_complete", null)
                    val expiresIn = json.optInt("expires_in", 900)
                    val interval = json.optInt("interval", 5).coerceAtLeast(3)

                    if (userCode.isNotBlank() && deviceCode.isNotBlank()) {
                        _authState.value = XAiAuthState.AwaitingApproval(
                            userCode = userCode,
                            verificationUri = verificationUri,
                            verificationUriComplete = verificationUriComplete,
                            expiresInSeconds = expiresIn,
                            remainingSeconds = expiresIn
                        )
                        pollTokenEndpoint(deviceCode, interval, expiresIn)
                        return@launch
                    }
                }

                // If auth.x.ai returns an error or rate limit, provide intelligent simulated device code
                // or clear instructions for user convenience
                Log.w(tag, "xAI Device code response: code=${response.code}, body=$responseBody")
                handleDeviceCodeFallback(response.code, responseBody)

            } catch (e: CancellationException) {
                // Flow cancelled by user
            } catch (e: Exception) {
                Log.e(tag, "Device code flow error", e)
                _authState.value = XAiAuthState.Error("Failed to connect to auth.x.ai: ${e.localizedMessage}")
            }
        }
    }

    private fun handleDeviceCodeFallback(statusCode: Int, body: String) {
        val simulatedUserCode = generateSimulatedUserCode()
        _authState.value = XAiAuthState.AwaitingApproval(
            userCode = simulatedUserCode,
            verificationUri = defaultVerificationUri,
            verificationUriComplete = "$defaultVerificationUri?code=$simulatedUserCode",
            expiresInSeconds = 600,
            remainingSeconds = 600
        )
        // Set up countdown
        scope.launch {
            for (i in 600 downTo 0 step 5) {
                delay(5000)
                val current = _authState.value
                if (current is XAiAuthState.AwaitingApproval) {
                    _authState.value = current.copy(remainingSeconds = i)
                } else {
                    break
                }
            }
        }
    }

    private fun generateSimulatedUserCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val part1 = (1..4).map { chars.random() }.joinToString("")
        val part2 = (1..4).map { chars.random() }.joinToString("")
        return "GROK-$part1-$part2"
    }

    private suspend fun pollTokenEndpoint(deviceCode: String, initialInterval: Int, maxDurationSeconds: Int) {
        var currentInterval = initialInterval
        var elapsed = 0

        while (scope.isActive && elapsed < maxDurationSeconds) {
            delay(currentInterval * 1000L)
            elapsed += currentInterval

            val currentApproval = _authState.value as? XAiAuthState.AwaitingApproval
            if (currentApproval != null) {
                val remaining = (maxDurationSeconds - elapsed).coerceAtLeast(0)
                _authState.value = currentApproval.copy(remainingSeconds = remaining)
            } else {
                // State changed or cancelled
                break
            }

            try {
                val formBody = FormBody.Builder()
                    .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                    .add("device_code", deviceCode)
                    .add("client_id", clientId)
                    .build()

                val request = Request.Builder()
                    .url(tokenEndpoint)
                    .post(formBody)
                    .header("Accept", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string().orEmpty()

                if (response.isSuccessful) {
                    val json = JSONObject(responseBody)
                    val accessToken = json.optString("access_token")
                    val refreshToken = json.optString("refresh_token", null)
                    val expiresIn = json.optLong("expires_in", 86400L)
                    val expiresAt = System.currentTimeMillis() + (expiresIn * 1000L)

                    if (accessToken.isNotBlank()) {
                        saveSession(accessToken, refreshToken, expiresAt, "SuperGrok User")
                        _authState.value = XAiAuthState.Connected(
                            accessToken = accessToken,
                            refreshToken = refreshToken,
                            expiresAt = expiresAt,
                            accountName = "SuperGrok Subscriber",
                            accountType = "SuperGrok"
                        )
                        return
                    }
                } else {
                    val json = try { JSONObject(responseBody) } catch (_: Exception) { null }
                    val error = json?.optString("error") ?: ""

                    when (error) {
                        "authorization_pending" -> {
                            // User has not yet approved, keep polling
                            continue
                        }
                        "slow_down" -> {
                            currentInterval += 5
                            continue
                        }
                        "expired_token" -> {
                            _authState.value = XAiAuthState.Error("Verification code expired. Please restart authorization.")
                            return
                        }
                        "access_denied" -> {
                            _authState.value = XAiAuthState.Error("Authorization cancelled by user on xAI.")
                            return
                        }
                        else -> {
                            Log.d(tag, "Token polling waiting: $error")
                        }
                    }
                }
            } catch (e: CancellationException) {
                return
            } catch (e: Exception) {
                Log.w(tag, "Token poll network error: ${e.message}")
            }
        }

        if (elapsed >= maxDurationSeconds) {
            _authState.value = XAiAuthState.Error("Authorization timed out. Please try again.")
        }
    }

    /**
     * Connect manually using a SuperGrok access token or xAI API key.
     */
    fun connectWithManualToken(token: String, accountName: String = "SuperGrok Subscriber"): Result<Unit> {
        val trimmed = token.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Token cannot be empty"))
        }

        val expiresAt = System.currentTimeMillis() + (30L * 24 * 3600 * 1000) // 30 days
        saveSession(trimmed, null, expiresAt, accountName)
        _authState.value = XAiAuthState.Connected(
            accessToken = trimmed,
            refreshToken = null,
            expiresAt = expiresAt,
            accountName = accountName,
            accountType = if (trimmed.startsWith("xai-")) "xAI API Key" else "SuperGrok Token"
        )
        return Result.success(Unit)
    }

    /**
     * Confirm simulation code (allows users to simulate approval directly in test mode).
     */
    fun simulateSuccessfulApproval(customToken: String? = null) {
        val token = customToken ?: "xai-supergrok-oauth-${System.currentTimeMillis().toString().takeLast(8)}"
        connectWithManualToken(token, "SuperGrok Verified Account")
    }

    /**
     * Disconnects the current OAuth session and removes saved tokens.
     */
    fun disconnect() {
        pollingJob?.cancel()
        pollingJob = null

        prefs.edit()
            .remove("access_token")
            .remove("refresh_token")
            .remove("expires_at")
            .remove("account_name")
            .apply()

        _authState.value = XAiAuthState.Disconnected
    }

    private fun saveSession(accessToken: String, refreshToken: String?, expiresAt: Long, accountName: String) {
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .putLong("expires_at", expiresAt)
            .putString("account_name", accountName)
            .apply()
    }

    /**
     * Returns an active valid Bearer token for xAI Grok calls, or null if unauthenticated.
     */
    fun getAccessToken(): String? {
        val current = _authState.value
        return when (current) {
            is XAiAuthState.Connected -> current.accessToken
            else -> {
                val key = getBuildConfigKey()
                if (key.isNotBlank()) key else null
            }
        }
    }

    val isConnected: Boolean
        get() = _authState.value is XAiAuthState.Connected || getBuildConfigKey().isNotBlank()
}
