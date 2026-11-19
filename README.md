# Repo2Video — GitHub URL in, 30–60s IG clip out

Turn any public GitHub repo into a vertical (9:16, 1080×1920) promo video
for Instagram Stories/Reels. Paste a repo URL, get an mp4.

## Two parts of this repo

- **`pipeline/` — the working pipeline (NEW, Nov 2026).** A Python + ffmpeg
  script that runs on any Linux box. This is the part that actually works
  today. See `pipeline/README.md`.
- **`app/` — the original Android app (Sep 2026).** A Jetpack Compose app
  that scans a repo, writes a script with Gemini, renders video with Veo
  and voiceover with ElevenLabs. It is a real 21k-line skeleton, but it has
  **no local video encoder** — video generation is a paid Veo 3.1 API call,
  and without keys it returns a stock Google sample video. It is kept here
  for the mobile UI; the pipeline is the working path until the app gets a
  real render backend.

## The original idea

`Repo-to-Promo` (same org, created the same morning) is just an AI Studio
banner README — no code. **This repo is the canonical home** for the idea.
Repo-to-Promo's README now points here.

## Quick start (pipeline)

```bash
python3 pipeline/pipeline.py https://github.com/hermes-max100/TradingAgents
# -> out/TradingAgents-promo-2026-11-19.mp4  (35s, 1080x1920, H.264 + AAC)
```

Requirements: Python 3, `ffmpeg`/`ffprobe`, Pillow (`pip install pillow`).
No API keys needed. Optional `--llm` tries Carmelo's unmetered router
(`REPO2VIDEO_ROUTER`, default `http://100.110.185.72:4000`) to polish the
script lines; it falls back to the deterministic template when the router
is unreachable.

## How the pipeline works

1. Fetch repo metadata + README from the public GitHub API.
2. Build a 5-scene script (hook / what it does / how it works / why it
   matters / follow-the-build) from the description and README headings.
3. Render each scene as a 1080×1920 PNG card (PIL, gradient + bold type).
4. ffmpeg: slow zoom per scene, 0.5s crossfades, synth music bed, H.264.
5. Verify with ffprobe: 30–60s, 1080×1920, audio present.

## Roadmap

- [ ] Voiceover: local TTS (Piper) so scenes are narrated, not just scored.
- [ ] Repo screenshots / code snippets as scene backgrounds.
- [ ] `--llm` polish wired to the VPS Bifrost router once it lands.
- [ ] Android app: replace the Veo call with this pipeline as the render
      backend, or drop the Veo path entirely.
- [ ] Auto-post to IG (needs his call — nothing auto-posts today).
