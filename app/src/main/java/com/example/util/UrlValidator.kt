package com.example.util

import java.net.URI
import java.util.regex.Pattern

object UrlValidator {

    private val ALLOWED_HOSTS = setOf(
        "github.com",
        "www.github.com",
        "gitlab.com",
        "www.gitlab.com",
        "bitbucket.org",
        "www.bitbucket.org"
    )

    private val PRIVATE_IP_PATTERNS = listOf(
        Pattern.compile("^127\\..*"),
        Pattern.compile("^10\\..*"),
        Pattern.compile("^192\\.168\\..*"),
        Pattern.compile("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*"),
        Pattern.compile("^169\\.254\\..*"),
        Pattern.compile("^0\\.0\\.0\\.0$"),
        Pattern.compile("^localhost$", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.internal$", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.local$", Pattern.CASE_INSENSITIVE)
    )

    /**
     * Validates and normalizes repository URL or path.
     * Returns Normalized URL if valid, or null with reason.
     */
    fun validateRepoInput(input: String): Pair<Boolean, String> {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            return false to "Repository input cannot be empty."
        }

        // Shorthand format: owner/repo
        if (trimmed.matches(Regex("^[a-zA-Z0-9_.-]+/[a-zA-Z0-9_.-]+$"))) {
            return true to "https://github.com/$trimmed"
        }

        return try {
            val uri = URI(if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) "https://$trimmed" else trimmed)
            val scheme = uri.scheme?.lowercase() ?: ""
            if (scheme != "https" && scheme != "http") {
                return false to "Only secure https:// repository URLs are allowed (found: $scheme)."
            }

            val host = uri.host?.lowercase() ?: return false to "Invalid repository URL host."

            // Check SSRF & private hosts
            for (pattern in PRIVATE_IP_PATTERNS) {
                if (pattern.matcher(host).matches()) {
                    return false to "Private network or local addresses are forbidden ($host)."
                }
            }

            if (!ALLOWED_HOSTS.contains(host)) {
                return false to "Only public git repositories on GitHub, GitLab, or Bitbucket are supported."
            }

            val path = uri.path?.trimEnd('/') ?: ""
            val segments = path.split('/').filter { it.isNotBlank() }
            if (segments.size < 2) {
                return false to "URL must contain owner and repository name (e.g. github.com/owner/repo)."
            }

            val normalized = "https://$host/${segments[0]}/${segments[1]}"
            true to normalized
        } catch (e: Exception) {
            false to "Invalid URL format: ${e.localizedMessage ?: "malformed URI"}"
        }
    }

    /**
     * Sanitize Markdown / Readme content before converting to rich text or rendering in previews
     */
    fun sanitizeMarkdown(content: String): String {
        return content
            // Remove script tags and event handlers
            .replace(Regex("(?i)<script[\\s\\S]*?</script>"), "")
            .replace(Regex("(?i)javascript:"), "blocked:")
            .replace(Regex("(?i)data:text/html"), "blocked:")
            .replace(Regex("(?i)onload="), "blocked=")
            .replace(Regex("(?i)onerror="), "blocked=")
    }
}
