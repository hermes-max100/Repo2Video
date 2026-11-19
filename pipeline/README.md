# pipeline/ — the working Repo2Video pipeline

Free/local only. No paid APIs, no keys required.

## Run

```bash
python3 pipeline.py https://github.com/hermes-max100/TradingAgents
python3 pipeline.py hermes-max100/TradingAgents --out out/
python3 pipeline.py <url> --llm   # try the unmetered router for script polish
```

## Requirements

- Python 3 (stdlib + Pillow)
- `ffmpeg` + `ffprobe` on PATH
- DejaVu Sans fonts (present on most Linux systems)

## What it produces

A 30–60s vertical mp4 (1080×1920, H.264, AAC), 5 scenes:

1. Hook — repo name + one-line description
2. What it does — a real sentence from the README
3. How it works — up to 3 feature bullets from README headings
4. Why it matters — language + fork/star line
5. Follow the build — `github.com/owner/repo`

Scenes get a slow Ken Burns zoom, 0.5s crossfades, and a generated
ambient music bed (synthesised chords, no audio files needed). Output is
verified with ffprobe (duration 30–60s, 1080×1920, audio present) before
the script exits.

## Notes

- `--llm` is best-effort: it asks the router at `$REPO2VIDEO_ROUTER`
  (default `http://100.110.185.72:4000`) for punchier scene lines and
  silently falls back to the template when the router is down.
- Only public repos work without a GitHub token (unauthenticated API).
- Nothing is posted anywhere. The mp4 lands in `out/`; posting is manual.
