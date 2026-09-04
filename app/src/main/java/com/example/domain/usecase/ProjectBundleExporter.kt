package com.example.domain.usecase

import com.example.data.model.ProjectBundle
import org.json.JSONArray
import org.json.JSONObject

class ProjectBundleExporter {

    fun exportToJson(bundle: ProjectBundle): String {
        val root = JSONObject()
        root.put("formatVersion", bundle.formatVersion)
        root.put("generator", "DevDirector / Repo2Video v2.0")
        root.put("projectId", bundle.projectId)
        root.put("projectTitle", bundle.projectTitle)
        root.put("exportedAt", bundle.exportedAt)
        root.put("voiceConfig", bundle.voiceConfig)

        // Brand Profile
        val brandObj = JSONObject().apply {
            put("name", bundle.brandProfile.name)
            put("tagline", bundle.brandProfile.tagline)
            put("primaryColorHex", bundle.brandProfile.primaryColorHex)
            put("secondaryColorHex", bundle.brandProfile.secondaryColorHex)
            put("accentColorHex", bundle.brandProfile.accentColorHex)
            put("targetAudience", bundle.brandProfile.targetAudience)
            put("repoUrl", bundle.brandProfile.repoPathOrUrl)
        }
        root.put("brandProfile", brandObj)

        // Scan Manifest
        val manifestObj = JSONObject().apply {
            put("appName", bundle.scanManifest.appName)
            put("rawRepoUrl", bundle.scanManifest.rawRepoUrl)
            put("technologies", JSONArray(bundle.scanManifest.technologies))
            put("keyFeatures", JSONArray(bundle.scanManifest.keyFeatures))
            put("userPersonas", JSONArray(bundle.scanManifest.userPersonas))
            put("notableFiles", JSONArray(bundle.scanManifest.notableFiles))
            put("excludedFiles", JSONArray(bundle.scanManifest.excludedFiles))
            put("rawSourceDeleted", bundle.scanManifest.rawSourceDeleted)
        }
        root.put("scanManifest", manifestObj)

        // Creative Brief
        val briefObj = JSONObject().apply {
            put("title", bundle.creativeBrief.title)
            put("hook", bundle.creativeBrief.hook)
            put("coreBenefit", bundle.creativeBrief.coreBenefit)
            put("targetAudience", bundle.creativeBrief.targetAudience)
            put("callToAction", bundle.creativeBrief.callToAction)
            put("durationBudgetSeconds", bundle.creativeBrief.durationBudgetSeconds)
            put("sceneBudgetCount", bundle.creativeBrief.sceneBudgetCount)
            put("contentSafetyVerified", bundle.creativeBrief.contentSafetyVerified)
            put("licenseStatus", bundle.creativeBrief.licenseStatus)
        }
        root.put("creativeBrief", briefObj)

        // Scenes
        val scenesArr = JSONArray()
        for (scene in bundle.scenes) {
            val sObj = JSONObject().apply {
                put("id", scene.id)
                put("orderIndex", scene.orderIndex)
                put("title", scene.title)
                put("subtitle", scene.subtitle)
                put("voiceover", scene.voiceover)
                put("emotionalPreset", scene.emotionalPreset.name)
                put("durationSeconds", scene.durationSeconds)
                put("visualType", scene.visualType.name)
                scene.codeSnippet?.let { put("codeSnippet", it) }
            }
            scenesArr.put(sObj)
        }
        root.put("scenes", scenesArr)

        // Claims with evidence links
        val claimsArr = JSONArray()
        for (claim in bundle.claims) {
            val cObj = JSONObject().apply {
                put("id", claim.id)
                put("claimText", claim.claimText)
                put("sourceFile", claim.sourceFile)
                put("lineReference", claim.lineReference)
                put("evidenceSnippet", claim.evidenceSnippet)
                put("isVerified", claim.isVerified)
                put("verificationNotes", claim.verificationNotes)
            }
            claimsArr.put(cObj)
        }
        root.put("claimEvidenceAuditTrail", claimsArr)

        // Licensing manifest
        val licenseArr = JSONArray()
        for (item in bundle.licensingManifest) {
            val lObj = JSONObject().apply {
                put("assetName", item.assetName)
                put("assetType", item.assetType)
                put("source", item.source)
                put("licenseType", item.licenseType)
                put("publishableStatus", item.publishableStatus)
                put("attributionNotice", item.attributionNotice)
            }
            licenseArr.put(lObj)
        }
        root.put("licensingAttributions", licenseArr)

        return root.toString(2)
    }
}
