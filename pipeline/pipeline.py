#!/usr/bin/env python3
"""
repo2video — GitHub URL in, 30-60s vertical IG clip out.

Pipeline: repo metadata + README -> 5-scene script -> 1080x1920 PNG cards
(PIL) -> slow-zoom segments + crossfades + synth music bed (ffmpeg) -> mp4.

Free/local only: no paid APIs. An optional --llm flag tries Carmelo's
unmetered OpenAI-compatible router (penguin) to polish the script lines and
falls back to the deterministic template when the router is unreachable.

Usage:
    python3 pipeline.py https://github.com/hermes-max100/TradingAgents
    python3 pipeline.py hermes-max100/TradingAgents --out out/
    python3 pipeline.py <url> --llm   # try router polish, fallback to template
"""
import json
import math
import os
import re
import subprocess
import sys
import textwrap
import urllib.request
from datetime import date

W, H = 1080, 1920
FPS = 30
BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
REG = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
MONO = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf"
ROUTER = os.environ.get("REPO2VIDEO_ROUTER", "http://100.110.185.72:4000")

# (top RGB, bottom RGB, accent RGB) per scene
PALETTES = [
    ((16, 24, 64), (4, 6, 18), (0, 210, 255)),
    ((40, 16, 64), (8, 4, 18), (190, 80, 255)),
    ((8, 48, 40), (3, 14, 12), (0, 230, 170)),
    ((64, 28, 8), (18, 8, 3), (255, 170, 0)),
    ((16, 24, 64), (4, 6, 18), (0, 210, 255)),
]

SKIP_HEADINGS = {
    "news", "installation", "install", "usage", "quick start", "quickstart",
    "contributing", "license", "citation", "references", "acknowledgements",
    "acknowledgments", "table of contents", "contents", "documentation",
    "community and support", "development", "changelog", "faq",
}


def parse_repo(arg):
    """Accept a GitHub URL or owner/repo, return (owner, repo)."""
    arg = arg.strip().rstrip("/").removesuffix(".git")
    m = re.match(r"(?:https?://)?(?:www\.)?github\.com/([^/]+)/([^/]+)", arg)
    if m:
        return m.group(1), m.group(2)
    if re.match(r"^[^/]+/[^/]+$", arg):
        return tuple(arg.split("/"))
    raise SystemExit(f"cannot parse repo from: {arg}")


def http_get(url, timeout=20):
    req = urllib.request.Request(url, headers={"User-Agent": "repo2video/1.0"})
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return r.read().decode("utf-8", "replace")


def fetch_repo(owner, repo):
    meta = json.loads(http_get(f"https://api.github.com/repos/{owner}/{repo}"))
    readme = ""
    for branch in ("main", "master"):
        try:
            readme = http_get(
                f"https://raw.githubusercontent.com/{owner}/{repo}/{branch}/README.md")
            break
        except Exception:
            continue
    return meta, readme


def clean_heading(h):
    h = re.sub(r"[#*_`\[\]()!]", "", h).strip()
    h = re.sub(r"\s+", " ", h)
    return h


def extract_bullets(readme, limit=3):
    """Feature bullets from README headings, falling back to first sentences."""
    bullets = []
    for line in readme.splitlines():
        m = re.match(r"#{2,4}\s+(.+)", line.strip())
        if not m:
            continue
        h = clean_heading(m.group(1))
        if not h or len(h) > 48:
            continue
        if h.lower() in SKIP_HEADINGS or any(s in h.lower() for s in ("install", "usage", "license")):
            continue
        if h not in bullets:
            bullets.append(h)
        if len(bullets) >= limit:
            break
    return bullets


def first_sentences(text, limit=2):
    """Plain-prose sentences from a README, skipping HTML/badges/code."""
    paras = []
    for line in text.splitlines():
        s = line.strip()
        if not s or s[0] in "<#!|>`-":
            continue
        if s.startswith("[") or "img.shields.io" in s or "badge" in s.lower():
            continue
        s = re.sub(r"<[^>]+>", " ", s)
        s = re.sub(r"\[([^\]]*)\]\([^)]*\)", r"\1", s)
        s = re.sub(r"[#*_`>|]", " ", s)
        s = re.sub(r"https?://\S+", " ", s)
        s = re.sub(r"\s+", " ", s).strip()
        if len(s) > 30:
            paras.append(s)
    out = []
    nav_words = ("installation", "contributing", "citation", "package usage",
                 "quick start", "table of contents")
    for p in paras:
        # skip nav bars / emoji link rows
        if len(re.findall(r"[\U0001F300-\U0001FAFF\u2600-\u27BF]", p)) >= 2:
            continue
        if any(w in p.lower() for w in nav_words):
            continue
        for sent in re.split(r"(?<=[.!])\s+", p):
            sent = sent.strip()
            if len(sent) > 30 and "  " not in sent and sent not in out:
                out.append(sent)
            if len(out) >= limit:
                return out
    return out


def clean_desc(desc, name):
    """Strip a leading 'Name:' prefix repos often put in their description."""
    d = (desc or "").strip()
    d = re.sub(rf"^{re.escape(name)}\s*:\s*", "", d, flags=re.I)
    return d


def build_scenes(meta, readme, owner, repo):
    """Deterministic 5-scene script from repo metadata. Total ~35s after fades."""
    name = meta.get("name") or repo
    desc = clean_desc(meta.get("description") or "", name)
    lang = meta.get("language")
    if not lang:
        low = readme.lower()
        lang = "Python" if "python" in low else "Open source"
    stars = meta.get("stargazers_count", 0)
    bullets = extract_bullets(readme)
    short_name = re.sub(r"[-_]", " ", name).title()

    hook = (desc.split(".")[0] if desc else f"{short_name} — open source, built in public.")
    if len(hook) > 90:
        hook = hook[:87] + "..."

    if not bullets:
        sents = first_sentences(readme)
        bullets = [s.split(".")[0][:48] for s in sents[:3]]
    if not bullets:
        bullets = [lang, "Open source", "Built in public"]

    # scene 2: a real sentence from the README, not the description again
    what = ""
    for s in first_sentences(readme, limit=4):
        if s.lower() not in hook.lower() and len(s) > 30:
            what = s
            break
    if not what:
        what = desc or hook

    proof_bits = [lang]
    proof_bits.append(f"{stars} stars on our fork" if stars else "Forked in our lab")
    proof = "  ·  ".join(proof_bits)
    return [
        {"kind": "hook", "title": short_name.upper(), "body": hook, "dur": 7},
        {"kind": "what", "title": "WHAT IT DOES", "body": what, "dur": 7},
        {"kind": "how", "title": "HOW IT WORKS", "body": "", "bullets": bullets, "dur": 8},
        {"kind": "proof", "title": "WHY IT MATTERS", "body": proof, "dur": 8},
        {"kind": "cta", "title": "FOLLOW THE BUILD", "body": f"github.com/{owner}/{repo}", "dur": 7},
    ]


def try_llm_polish(scenes, meta):
    """Best-effort: ask the unmetered router to tighten scene lines. Never fatal."""
    try:
        import urllib.request as u
        models = json.loads(http_get(f"{ROUTER}/v1/models", timeout=8))
        ids = [m["id"] for m in models.get("data", []) if "id" in m]
        if not ids:
            return scenes
        model = next((i for i in ids if "glm" in i.lower() or "qwen" in i.lower()), ids[0])
        prompt = ("Rewrite these 5 promo-video scene lines to be punchier for a "
                  "30-second Instagram clip. Keep each line under 90 characters, "
                  "keep the same order and meaning, return JSON list of 5 strings.\n"
                  + json.dumps([s.get("body", "") for s in scenes]))
        body = json.dumps({
            "model": model,
            "messages": [{"role": "user", "content": prompt}],
            "temperature": 0.7,
        }).encode()
        req = u.Request(f"{ROUTER}/v1/chat/completions", data=body,
                        headers={"Content-Type": "application/json"})
        with u.urlopen(req, timeout=25) as r:
            out = json.loads(r.read())
        lines = json.loads(out["choices"][0]["message"]["content"].strip().strip("`"))
        if isinstance(lines, list) and len(lines) == 5:
            for s, line in zip(scenes, lines):
                if isinstance(line, str) and line.strip():
                    s["body"] = line.strip()[:120]
            print(f"[llm] polished with {model}")
    except Exception as e:
        print(f"[llm] router unavailable ({e}) — using template script")
    return scenes


# ---------- rendering ----------

def gradient(size, top, bottom):
    from PIL import Image
    img = Image.new("RGB", size)
    px = img.load()
    for y in range(size[1]):
        t = y / max(size[1] - 1, 1)
        px_line = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
        for x in range(size[0]):
            px[x, y] = px_line
    return img


def fit_font(draw, text, path, start, max_w, min_size=28):
    from PIL import ImageFont
    size = start
    while size > min_size:
        f = ImageFont.truetype(path, size)
        if draw.textlength(text, font=f) <= max_w:
            return f
        size -= 2
    return ImageFont.truetype(path, min_size)


def wrap(draw, text, font, max_w):
    lines, cur = [], ""
    for word in text.split():
        trial = (cur + " " + word).strip()
        if draw.textlength(trial, font=font) <= max_w:
            cur = trial
        else:
            if cur:
                lines.append(cur)
            cur = word
    if cur:
        lines.append(cur)
    return lines


def render_scene(scene, idx, total, path):
    from PIL import Image, ImageDraw, ImageFont
    top, bottom, accent = PALETTES[idx % len(PALETTES)]
    img = gradient((W, H), top, bottom)
    d = ImageDraw.Draw(img)
    # accent bar + scene kicker (content block vertically centred)
    d.rectangle([72, 680, 72 + 140, 696], fill=accent)
    kick = ImageFont.truetype(MONO, 40)
    d.text((72, 600), f"SCENE {idx + 1}/{total}", font=kick, fill=accent)
    # title
    title_font = fit_font(d, scene["title"], BOLD, 118, W - 144)
    d.text((72, 740), scene["title"], font=title_font, fill=(255, 255, 255))
    y = 940
    body_font = ImageFont.truetype(REG, 54)
    if scene.get("bullets"):
        for b in scene["bullets"]:
            lines = wrap(d, "▸  " + b, body_font, W - 144)
            for ln in lines:
                d.text((72, y), ln, font=body_font, fill=(235, 240, 255))
                y += 78
            y += 40
    elif scene["body"]:
        for ln in wrap(d, scene["body"], body_font, W - 144):
            d.text((72, y), ln, font=body_font, fill=(235, 240, 255))
            y += 82
    # progress dots
    for i in range(total):
        x0 = 72 + i * 56
        d.ellipse([x0, H - 180, x0 + 28, H - 152],
                  fill=accent if i == idx else (90, 100, 130))
    foot = ImageFont.truetype(MONO, 34)
    d.text((72, H - 120), "TAKE2STUDIO  ·  GITHUB TO VIDEO", font=foot, fill=(150, 160, 190))
    img.save(path)


def run(cmd):
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode != 0:
        raise RuntimeError(f"ffmpeg failed: {' '.join(cmd)}\n{r.stderr[-1500:]}")
    return r


def build_video(scenes, workdir, out_path):
    total_dur = sum(s["dur"] for s in scenes)
    # 1. per-scene zoom segments
    segs = []
    for i, s in enumerate(scenes):
        frames = s["dur"] * FPS
        seg = os.path.join(workdir, f"seg{i}.mp4")
        run(["ffmpeg", "-y", "-v", "error", "-loop", "1", "-i",
             os.path.join(workdir, f"scene{i}.png"),
             "-vf", f"zoompan=z='1+0.10*on/{frames}':x='iw/2-(iw/zoom/2)':"
                    f"y='ih/2-(ih/zoom/2)':d={frames}:s={W}x{H}:fps={FPS},format=yuv420p",
             "-frames:v", str(frames), "-c:v", "libx264", "-preset", "veryfast",
             "-crf", "20", seg])
        segs.append(seg)
    # 2. xfade chain (0.5s fades)
    fade = 0.5
    inputs, filt = [], []
    for seg in segs:
        inputs += ["-i", seg]
    n = len(segs)
    # xfade offset = end of the running timeline MINUS the fade: the fade must
    # start while the first input still has frames left to blend with.
    offset = scenes[0]["dur"] - fade
    cur = "0:v"
    for i in range(1, n):
        out_label = f"x{i}"
        filt.append(f"[{cur}][{i}:v]xfade=transition=fade:duration={fade}:offset={offset:.2f}[{out_label}]")
        cur = out_label
        offset += scenes[i]["dur"] - fade
    final_dur = total_dur - fade * (n - 1)
    # 3. synth music bed: warm Am pad, slow pulse, fade in/out
    pad = (f"aevalsrc='0.14*sin(2*PI*110*t)*(0.7+0.3*sin(2*PI*0.5*t))"
           f"+0.10*sin(2*PI*164.81*t)*(0.7+0.3*sin(2*PI*0.5*t+1))"
           f"+0.07*sin(2*PI*220*t)+0.04*sin(2*PI*329.63*t)':s=44100:d={final_dur:.2f},"
           f"lowpass=f=1200,afade=t=in:st=0:d=1.5,afade=t=out:st={final_dur - 2:.2f}:d=2,"
           f"volume=0.9[a]")
    cmd = (["ffmpeg", "-y", "-v", "error"] + inputs +
           ["-filter_complex", ";".join(filt) + ";" + pad,
            "-map", f"[{cur}]", "-map", "[a]",
            "-c:v", "libx264", "-preset", "veryfast", "-crf", "20",
            "-c:a", "aac", "-b:a", "128k", "-movflags", "+faststart",
            "-t", f"{final_dur:.2f}", out_path])
    run(cmd)
    return final_dur


def verify(path):
    out = subprocess.run(["ffprobe", "-v", "quiet", "-print_format", "json",
                          "-show_format", "-show_streams", path],
                         capture_output=True, text=True).stdout
    j = json.loads(out)
    v = next(s for s in j["streams"] if s["codec_type"] == "video")
    return {
        "duration": float(j["format"]["duration"]),
        "width": v["width"], "height": v["height"],
        "codec": v["codec_name"],
        "size": int(j["format"]["size"]),
        "has_audio": any(s["codec_type"] == "audio" for s in j["streams"]),
    }


def main():
    if len(sys.argv) < 2:
        raise SystemExit("usage: pipeline.py <github-url-or-owner/repo> [--llm] [--out DIR]")
    owner, repo = parse_repo(sys.argv[1])
    use_llm = "--llm" in sys.argv
    outdir = "out"
    if "--out" in sys.argv:
        outdir = sys.argv[sys.argv.index("--out") + 1]
    os.makedirs(outdir, exist_ok=True)
    workdir = os.path.join(outdir, f".work-{repo}")
    os.makedirs(workdir, exist_ok=True)

    print(f"[fetch] {owner}/{repo}")
    meta, readme = fetch_repo(owner, repo)
    scenes = build_scenes(meta, readme, owner, repo)
    if use_llm:
        scenes = try_llm_polish(scenes, meta)
    for i, s in enumerate(scenes):
        print(f"[scene {i + 1}] {s['title']}: {s.get('body') or '; '.join(s.get('bullets', []))}")
        render_scene(s, i, len(scenes), os.path.join(workdir, f"scene{i}.png"))

    stamp = date.today().isoformat()
    out_path = os.path.join(outdir, f"{repo}-promo-{stamp}.mp4")
    dur = build_video(scenes, workdir, out_path)
    info = verify(out_path)
    print(f"[done] {out_path}")
    print(f"[verify] {info['duration']:.1f}s, {info['width']}x{info['height']}, "
          f"{info['codec']}, {info['size'] / 1e6:.1f} MB, audio={info['has_audio']}")
    assert 29 <= info["duration"] <= 62, "duration outside 30-60s target"
    assert (info["width"], info["height"]) == (1080, 1920), "not 9:16 vertical"
    print("[verify] PASS — 30-60s vertical clip")


if __name__ == "__main__":
    main()
