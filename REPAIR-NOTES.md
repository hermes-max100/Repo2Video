# Repo2Video repair — REPAIR-NOTES.md

Branch: `fix/openmontage-render-repair` (parented on remote main head
8fb8ca6 — see "Rebase" below). **NOT merged to main.**
Date: 2026-11-20. Vet verdict this repair answers: NO-GO as a production tool;
CONDITIONAL GO on repair + a human-supervised Take2Studio pilot.

## Rebase — main moved while this repair was being written (Nov 20 ~01:20)

The local audit clone sat at 4ab5ef7, but remote main had two newer commits:

- 7b805af "feat: working repo-to-video pipeline (free/local, ffmpeg)" —
  only added `pipeline/` + READMEs; no Kotlin changes.
- 8fb8ca6 "feat(auth): integrate Firebase Authentication and Firestore" —
  rewrote `GeminiPromoService` (OkHttp + BuildConfig key → Firebase AI Logic,
  structured schemas, streaming Director Chat) and added cloud sync to
  `PromoViewModel`.

The repair was rebased onto 8fb8ca6. Two of the repair's files had moved
upstream, and blindly pushing the 4ab5ef7-based versions would have silently
reverted the Firebase work — caught by comparing local HEAD blob SHAs against
remote main before pushing (10 of 12 edited files were byte-identical;
only `GeminiPromoService.kt` and `PromoViewModel.kt` differed).

What the upstream rewrite already fixed (so this branch does not re-do it):

- The dead Veo video call is GONE upstream — replaced by
  `generateVeoShotDirection`, which returns a one-sentence text motion
  directive, not a video. No `ForBiggerBlazes.mp4` fallback remains.
- `createLocalBrandProfile` (keyword-invented brand names: "FastSolve
  Extension", "Remotion Video", "PromoVideo AI", "FastShip Dev") is GONE —
  `discoverBrand` is now model-driven with a structured schema. Its
  fallbacks are generic placeholders ("My Product", "Kotlin / Jetpack
  Compose"), not repo-specific fabrications.
- The hardcoded "Proven Traction" scene and "Free & MIT" / "50,000+" claims
  are GONE — `generatePromoScript` is model-driven.

What the repair still adds on top of 8fb8ca6:

- `generatePromoScript` passed the model's `metricNumber`/`metricLabel`
  through unchecked — the model could invent a metric. Now gated: the prompt
  tells the model to include a metric only when verified, and parsing drops
  the model's numbers entirely — a METRIC_COUNTER scene shows only
  `brand.verifiedMetricNumber`/`verifiedMetricLabel` (null unless verified).
- `PromoViewModel.executeIngestionWithConsent` STILL fed the scanner the
  fabricated `sampleContent` ("Config: Remotion 4.0 / Framework: React 19 /
  License: MIT") — fixed the same way (scan what the user actually supplied).
- `generateVeoVideoForScene` STILL existed — it called
  `generateVeoShotDirection`, stuffed the directive text into the subtitle,
  and marked the scene `VEO_AI_VIDEO`, while the StudioEditorScreen button
  still promised "Generate Veo 3 Video Clip for Scene". Replaced by
  `generateOpenMontageClipForScene` (real VPS render, honest failure).
- The `VEO_AI_VIDEO` enum value ("Veo AI Clip") is renamed
  `OPENMONTAGE_RENDER` — nothing in the rebased tree produces it anymore,
  and the VideoPlayerView `when` stays exhaustive (7 values, 7 branches).

## What was wrong (vet findings, confirmed in source)

1. `LocalVideoDownloadManager.synthesizeMp4File` wrote a SYNTHETIC MP4 — fake
   ftyp/moov/mdat boxes, a text watermark ("DevDirector Autonomous Remotion
   Render"), a variable literally named `dummyFrame`, fixed payload sizes
   (900,000 / 1,800,000 / 3,500,000 bytes), empty sample tables. It was not
   a rendered video.
2. `SanitizedRepoScannerUseCase.createDeterministicManifest` hardcoded
   "verified" evidence: `isVerified = true`, Remotion ^4.0.0, `src/Root.tsx`
   compositions, "npx remotion render…", regardless of the repo's contents.
   `PromoViewModel.executeIngestionWithConsent` fed the scanner a FABRICATED
   `sampleContent` string ("Config: Remotion 4.0 / Framework: React 19 /
   License: MIT") — the sanitizer never saw real repo text.
3. `GeminiPromoService.generateVeoVideo` called the Veo 3.1 fast-preview
   endpoint, retired Oct 22, 2026. With no key it returned Google's public
   `ForBiggerBlazes.mp4` sample — unrelated to the repo.
4. Fallback scripts invented claims: "Join over 50,000 developers…",
   metricNumber "50,000+" / "Videos Rendered on Remotion", badge "Free & MIT".
5. `ScriptwriterUseCase` defaulted an unlinked line to verified
   (`claim?.isVerified ?: true`); `CreativePlannerUseCase` fabricated a
   claim list when the manifest had none.

## What changed (12 files modified, 1 new)

| File | Change |
| --- | --- |
| `service/OpenMontageRenderService.kt` (NEW) | Interface + HTTP client for the VPS render service. POST /render on 100.83.164.96:8891, poll every 3s (15 min cap), download MP4, verify non-empty. Rejects non-16:9 and empty scenes. Metric scenes with a null metricNumber fail at the boundary instead of inventing a number. |
| `domain/manager/LocalVideoDownloadManager.kt` | `synthesizeMp4File` + `buildMoovBox` + `writeEmptyTableBox` + `writeBox` DELETED. `saveVideoToDeviceStorage` now renders via the injected `OpenMontageRenderService` and saves the real MP4; render failure is an honest failure result. Fake `delay()` calls removed. |
| `domain/usecase/SanitizedRepoScannerUseCase.kt` | `createDeterministicManifest(sourceText)` — claims are verified against the supplied source text (evidence token must actually appear) or dropped from the manifest. No more invented defaults. |
| `ui/viewmodel/PromoViewModel.kt` | Rebased onto upstream 8fb8ca6 (cloud sync added). Ingestion scans the content the user actually supplied (pasted text or URL) — no fabricated `sampleContent`. `generateVeoVideoForScene` → `generateOpenMontageClipForScene` (renders one scene on the VPS, attaches the local file path, honest failure message). The old version called `generateVeoShotDirection`, stuffed a text directive into the subtitle, and marked the scene `VEO_AI_VIDEO` — no video was ever made. |
| `service/GeminiPromoService.kt` | Rebased onto upstream 8fb8ca6 (Firebase AI Logic rewrite), which already removed the dead Veo call, the `ForBiggerBlazes.mp4` fallback, the "Proven Traction" scene, and the keyword-fabricated `createLocalBrandProfile`. This branch adds the metric guard: the script prompt requires verified metrics only, and parsing drops the model's `metricNumber`/`metricLabel` — a METRIC_COUNTER scene shows only `brand.verifiedMetricNumber`/`verifiedMetricLabel` (null unless verified). |
| `data/model/BrandProfile.kt` | Added `verifiedMetricNumber` / `verifiedMetricLabel` (nullable). A metric may only be set when verified from the repo. |
| `data/model/Enums.kt` | WARM sampleQuote "Join over 50,000 developers building faster." → "Turn your repo into a launch video." `VEO_AI_VIDEO` → `OPENMONTAGE_RENDER`. |
| `data/local/PromoRepository.kt` | Seeded scene: title "100% Production Ready" → "Ready to Render"; voiceover "Join thousands of developers…" → "Turn a git repository into a high-converting launch video."; metricNumber/metricLabel → null/null. |
| `ui/player/VideoPlayerView.kt` | Metric shown only when `metricNumber != null` (no "50,000+" default, no invented label). `VEO_AI_VIDEO` block → `OPENMONTAGE_RENDER`, shows the rendered clip file name when attached. |
| `ui/screens/StudioEditorScreen.kt` | Button "Generate Veo 3 Video Clip for Scene" → "Render Scene with OpenMontage", testTag `generate_veo_clip_button` → `render_openmontage_clip_button`. Metric text field default "50,000+" → empty. |
| `domain/usecase/CreativePlannerUseCase.kt` | Removed the ifEmpty fabrication fallback — empty claims stay empty. (Unused `ClaimEvidence` import removed.) |
| `domain/usecase/ScriptwriterUseCase.kt` | `claim?.isVerified ?: true` → `?: false`. A line with no linked claim is NOT verified by default. |
| `test/.../LocalVideoDownloadManagerTest.kt` | Rewritten around a fake render service: asserts the saved file contains exactly the renderer's marker bytes (provenance), portrait/square fail honestly with an error message, a render failure propagates honestly, batch = 1 success + 2 failures. |

## The render service (OpenMontage on the IONOS VPS)

- Location: `/opt/openmontage` on the VPS (tailnet 100.83.164.96). Clone remote
  is **OpenMontageInc/OpenMontage** (the task brief said calesthio — that is
  wrong; HEAD 2e003e6, cloned Nov 19 18:57). Node v24.22.3, ffmpeg 7.1.5.
- Service: `/opt/openmontage/render-service/app.py` (FastAPI, uvicorn) +
  systemd unit `openmontage-render.service`, listening on
  **100.83.164.96:8891 (tailnet only, not public)**. Health endpoint verified.
- Contract: POST /render → 202 {job_id}; GET /render/{id} → status / error /
  output_url / duration / fps / resolution / frames / has_audio / size_bytes;
  GET /render/{id}/download → video/mp4; GET /health.
- Rendering: Remotion `Explainer` composition (1920×1080, 30fps, h264) +
  Piper TTS narration (`en_US-lessac-medium`, one wav per voiced scene,
  mixed with ffmpeg adelay+amix). Output ffprobe-verified before marking done.
- Limits: max 10 scenes, max 15 s/scene, max 120 s total. **16:9 only** —
  9:16 and 1:1 return HTTP 400 (no vertical Explainer variant exists yet).
  One job at a time.
- The 5 Nov 19 demos in `/opt/openmontage/projects/demos/renders/` were
  verified real on Nov 20 ~01:00 (ffprobe: h264 1920×1080 30fps + AAC audio,
  630–750 frames each, 2.1–4.4 MB) — they are genuine renders, not synthetic.
- Audio fix applied Nov 20: Remotion's asset downloader rejects `file://`
  URLs, so narration is staged under `remotion-composer/public/` as
  `narration-{job_id}.wav` and referenced by relative name (staticFile),
  then deleted after the job.
- Connectivity note: the build host cannot reach 100.83.164.96:8891 directly
  (no direct tailnet TCP from this VM) — jobs must be submitted from the VPS
  over SSH. The service itself is healthy from the VPS.

## End-to-end test — Take2Studio (this is a pipeline test, NOT the final promo)

Job: `efda40909a54`, submitted Nov 20 ~01:08 PST. Honest copy only — brand
Take2Studio, tagline "AI video remakes" (verified positioning). No prices,
no 48-hour claim, no user counts (all unconfirmed). 5 scenes, 30 s total:
hero 5 s, feature 6 s, feature 6 s, comparison 7 s (Before/Raw clips vs
After/Finished promo), CTA 6 s. Voice: Piper.

Evidence:

- **Job 1 `efda40909a54` FAILED** — Remotion hit the service's 900 s
  subprocess timeout (`npx remotion render … timed out after 900 seconds`).
  The 30 s / 900-frame render never finished because the VPS was heavily
  loaded (load 12 → 17 → 31 during the render) by concurrent install work
  from other lanes: `npm ci`, mem0 / MoneyPrinterTurbo / promptfoo pip +
  tsc builds, clickhouse-server, node workers. The render itself was
  progressing (Chrome renderer processes at ~60% + 21% + 21% CPU) — it was
  starved, not stuck. No `out.mp4` was produced.
- **Timeout fix applied**: `RENDER_TIMEOUT_SECONDS` 900 → 3600 in
  `app.py`, synced to the VPS and the service restarted (verified active,
  PID 2212624, 09:31 UTC). The 900 s cap was set when the box was quiet;
  a 30 s render on the shared 4-CPU box legitimately needs longer under load.
- **Job 2 NOT YET SUBMITTED — VPS went unresponsive ~09:33 PST.**
  SSH times out during banner exchange (10 s, 30 s, and 60 s connect
  timeouts all fail) and the AtoZ site on the same box times out
  (curl via egress proxy: http=000 after 20 s), while the egress proxy
  itself is healthy (example.com 200 in 0.63 s). The VPS is either
  thrashing under the install wave or down — indistinguishable from here.
  **Retry is ready and waiting on VPS recovery**: the job JSON is
  `/tmp/take2-e2e-job.json` on the VPS; re-submit with
  `curl -X POST http://100.83.164.96:8891/render -H 'Content-Type:
  application/json' --data @/tmp/take2-e2e-job.json`, then poll
  `GET /render/{job_id}` until `done` and download
  `GET /render/{job_id}/download`.
- Verification to run when the render lands: ffprobe (h264 + AAC streams,
  duration ≈ 30 s, 1920×1080, 30 fps, ~900 frames), 3 extracted frames
  with differing pixel hashes, audio stream present, size sane (not a
  900k/1.8M/3.5M fixed synthetic size), no "dummyFrame" / watermark
  strings in the bytes. Target path:
  `~/workspace/repo2video-openmontage/take2studio-e2e-test-20261120.mp4`.
- **No rendered test video exists yet.** Nothing in this branch claims
  otherwise. The pipeline proof (5 Nov 19 demos verified real by ffprobe)
  stands; the Take2Studio end-to-end run is the outstanding item.

The delivered Take2 promo was REJECTED by Carmelo on Nov 19 and a rebuild to
an "Ari Coles" style is ordered. This test proves the pipeline renders real
video; it is not the final promo.

## What still needs his review

1. **Compile** — no Android SDK / kotlinc / java on the build host, so no
   compile was run. Brace/paren/bracket balance verified on all 13 edited
   files; every referenced symbol was cross-checked by grep. A compile on an
   SDK machine (or an AI Studio import) is still owed before this branch is
   trusted.
2. **Portrait / square** — the app fails honestly for 9:16 and 1:1 until a
   vertical Explainer variant is built in the Remotion composer.
3. **`createLocalBrandProfile` — MOOT, fixed upstream.** The keyword-invented
   brand names ("FastSolve Extension", "Remotion Video", "PromoVideo AI",
   "FastShip Dev") are gone in 8fb8ca6; `discoverBrand` is model-driven now.
   Remaining nit: its fallbacks are generic placeholders, and
   `generatePromoScript`'s model can still invent non-metric copy (titles,
   voiceovers) — that is the model's job in a promo generator, not a
   verified-fact claim, but the HardClaimsGate is the backstop for claims.
4. **Scanning a bare repo URL cannot verify claims** — the scanner verifies
   against text the user pastes. A URL alone yields zero verified claims
   (honest, but thin). A real fetch-and-scan step is a follow-up, not in
   this branch.
5. **DeepGram voice swap** — pending his key; Piper is the voice for now.
6. **Branch is NOT merged.** Push is via the Git Database API
   (`~/workspace/repo2video-openmontage/push-branch-via-api.py`) using the
   stored custom.github credential; the audit clone's git remote URL had a
   GitHub PAT embedded in plaintext and was scrubbed to
   https://github.com/hermes-max100/Repo2Video.git on Nov 20 ~01:05
   (do not reproduce the token value anywhere).
