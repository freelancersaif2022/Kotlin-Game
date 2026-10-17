#!/usr/bin/env python3
"""Procedural audio generator for Bramble Dash (original, no samples, no network).

Writes short 16-bit mono WAV effects to assets/audio/sfx and seamless OGG Vorbis
loops (via ffmpeg/libvorbis) to assets/audio/music. Loops are rendered on an exact
beat grid and note tails that pass the loop end are wrapped to the start, so the
loop point has no gap or click. Deterministic: fixed RNG seeds.

Usage: python3 -I tools/audio_gen.py [repo_root]
"""
import os, subprocess, sys, wave
import numpy as np

SR = 22050          # effects
MSR = 32000         # music / ambience
rng = np.random.default_rng(7)


def t(d, sr=SR):
    return np.arange(int(d * sr)) / sr


def env(n, a=0.005, r=0.1, sr=SR, curve=3.0):
    e = np.ones(n)
    na = max(1, int(a * sr))
    e[:na] = np.linspace(0, 1, na)
    x = np.linspace(0, 1, n - na)
    e[na:] = (1 - x) ** curve
    return e


def sweep(f0, f1, d, shape="sine", sr=SR, exp=True):
    n = int(d * sr)
    f = np.geomspace(f0, f1, n) if exp else np.linspace(f0, f1, n)
    ph = 2 * np.pi * np.cumsum(f) / sr
    if shape == "sine":
        return np.sin(ph)
    if shape == "square":
        return np.sign(np.sin(ph)) * 0.6
    if shape == "tri":
        return 2 / np.pi * np.arcsin(np.sin(ph))
    if shape == "saw":
        return 2 * ((ph / (2 * np.pi)) % 1) - 1
    raise ValueError(shape)


def tone(f, d, shape="sine", sr=SR):
    return sweep(f, f, d, shape, sr, exp=False)


def noise(d, sr=SR):
    return rng.uniform(-1, 1, int(d * sr))


def lowpass(x, cutoff, sr=SR):
    a = np.exp(-2 * np.pi * cutoff / sr)
    y = np.empty_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc = (1 - a) * v + a * acc
        y[i] = acc
    return y


def highpass(x, cutoff, sr=SR):
    return x - lowpass(x, cutoff, sr)


def seq(*parts, gap=0.0, sr=SR):
    out = []
    for p in parts:
        out.append(p)
        if gap:
            out.append(np.zeros(int(gap * sr)))
    return np.concatenate(out)


def mix(*parts):
    n = max(len(p) for p in parts)
    out = np.zeros(n)
    for p in parts:
        out[: len(p)] += p
    return out


def at(x, offset, total=None, sr=SR):
    pad = np.zeros(int(offset * sr))
    y = np.concatenate([pad, x])
    if total:
        y = np.concatenate([y, np.zeros(max(0, int(total * sr) - len(y)))])
    return y


def finish(x, peak=0.89, sr=SR):
    x = np.asarray(x, dtype=np.float64)
    fade = min(len(x), int(0.004 * sr))
    x[:fade] *= np.linspace(0, 1, fade)
    x[-fade:] *= np.linspace(1, 0, fade)
    m = np.max(np.abs(x)) or 1
    return x / m * peak


def write_wav(path, x, sr=SR):
    data = (np.clip(x, -1, 1) * 32767).astype("<i2")
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(sr)
        w.writeframes(data.tobytes())


def note_hz(n):
    return 440.0 * 2 ** ((n - 69) / 12)


# ---------------------------------------------------------------- effects
def sfx():
    s = {}
    s["ui_click"] = tone(1400, 0.05, "square") * env(int(0.05 * SR), 0.001, curve=4) * 0.5 + tone(2800, 0.05) * env(int(0.05 * SR), 0.001, curve=6) * 0.3
    s["ui_back"] = sweep(900, 500, 0.09, "square") * env(int(0.09 * SR), 0.002, curve=3) * 0.5
    up = lambda f: tone(f, 0.08, "tri") * env(int(0.08 * SR), 0.003, curve=2)
    s["purchase_ok"] = seq(up(784), up(988), up(1175), tone(1568, 0.25, "tri") * env(int(0.25 * SR), 0.003, curve=2))
    s["purchase_fail"] = seq(tone(220, 0.12, "square") * env(int(0.12 * SR), 0.002, curve=1), tone(185, 0.2, "square") * env(int(0.2 * SR), 0.002, curve=2), gap=0.03)
    s["jump"] = sweep(280, 760, 0.16, "square") * env(int(0.16 * SR), 0.003, curve=2) * 0.6
    s["double_jump"] = mix(sweep(420, 1200, 0.18, "square") * env(int(0.18 * SR), 0.003, curve=2) * 0.5,
                           at(sweep(840, 2000, 0.12, "tri") * env(int(0.12 * SR), 0.002, curve=2) * 0.4, 0.05))
    s["slide"] = lowpass(noise(0.3), 1800) * env(int(0.3 * SR), 0.02, curve=1.5) * 1.2
    s["land"] = mix(sweep(160, 50, 0.12) * env(int(0.12 * SR), 0.001, curve=3), lowpass(noise(0.08), 900) * env(int(0.08 * SR), 0.001, curve=4) * 0.6)
    coin = lambda: seq(tone(988, 0.06, "square") * 0.45, tone(1319, 0.22, "square") * env(int(0.22 * SR), 0.001, curve=3) * 0.45)
    s["coin"] = coin()
    s["gem"] = mix(*[at(tone(f, 0.3, "tri") * env(int(0.3 * SR), 0.002, curve=3) * 0.5, i * 0.045) for i, f in enumerate([1319, 1568, 1976, 2637])])
    s["powerup_pickup"] = sweep(330, 1320, 0.35, "tri") * (0.6 + 0.4 * np.sin(2 * np.pi * 18 * t(0.35))) * env(int(0.35 * SR), 0.005, curve=1.5)
    s["powerup_activate"] = mix(*[at(tone(note_hz(n), 0.18, "square") * env(int(0.18 * SR), 0.002, curve=2) * 0.35, i * 0.06) for i, n in enumerate([72, 76, 79, 84])])
    s["shield_break"] = mix(highpass(noise(0.4), 2500) * env(int(0.4 * SR), 0.001, curve=4), sweep(1800, 300, 0.3, "saw") * env(int(0.3 * SR), 0.001, curve=3) * 0.4)
    s["damage"] = mix(sweep(600, 120, 0.25, "square") * env(int(0.25 * SR), 0.001, curve=2) * 0.6, lowpass(noise(0.15), 2500) * env(int(0.15 * SR), 0.001, curve=3) * 0.5)
    s["death"] = seq(*[tone(note_hz(n), 0.16, "square") * env(int(0.16 * SR), 0.003, curve=1) * 0.5 for n in [67, 63, 60]],
                     sweep(note_hz(55), note_hz(43), 0.6, "square") * env(int(0.6 * SR), 0.003, curve=1.5) * 0.5)
    s["revive"] = mix(*[at(tone(note_hz(n), 0.35, "tri") * env(int(0.35 * SR), 0.01, curve=2) * 0.4, i * 0.08) for i, n in enumerate([60, 64, 67, 72, 76, 79])])
    s["enemy_attack"] = mix(sweep(300, 90, 0.22, "saw") * env(int(0.22 * SR), 0.004, curve=2) * 0.5, lowpass(noise(0.2), 1200) * env(int(0.2 * SR), 0.01, curve=2) * 0.4)
    s["enemy_defeat"] = mix(sweep(900, 150, 0.25, "square") * env(int(0.25 * SR), 0.001, curve=2) * 0.4, lowpass(noise(0.3), 3000) * env(int(0.3 * SR), 0.001, curve=3) * 0.6)
    s["energy_shot"] = mix(sweep(2200, 400, 0.2, "saw") * env(int(0.2 * SR), 0.001, curve=2) * 0.35, sweep(1100, 200, 0.2, "sine") * env(int(0.2 * SR), 0.001, curve=2) * 0.5)
    s["boss_warning"] = seq(*[(tone(440, 0.22, "square") * 0.4 + tone(466, 0.22, "square") * 0.3) * env(int(0.22 * SR), 0.005, curve=0.5) for _ in range(3)], gap=0.12)
    s["boss_attack"] = mix(sweep(140, 45, 0.5, "saw") * env(int(0.5 * SR), 0.005, curve=1.5) * 0.6, lowpass(noise(0.5), 700) * env(int(0.5 * SR), 0.01, curve=2) * 0.7)
    s["explosion"] = mix(lowpass(noise(0.8), 900) * env(int(0.8 * SR), 0.001, curve=2.5), sweep(120, 30, 0.6) * env(int(0.6 * SR), 0.001, curve=2) * 0.8)
    s["boss_defeat"] = mix(s["explosion"] * 0.8, at(seq(*[tone(note_hz(n), 0.14, "square") * env(int(0.14 * SR), 0.002, curve=1) * 0.35 for n in [72, 76, 79, 84, 88]]), 0.35))
    s["chest_open"] = mix(lowpass(noise(0.18), 1500) * env(int(0.18 * SR), 0.01, curve=1) * 0.5,
                          at(mix(*[at(tone(note_hz(n), 0.4, "tri") * env(int(0.4 * SR), 0.003, curve=2) * 0.4, i * 0.05) for i, n in enumerate([79, 83, 86, 91])]), 0.15))
    s["unlock"] = mix(*[at(tone(note_hz(n), 0.5, "square") * env(int(0.5 * SR), 0.005, curve=2) * 0.3, i * 0.1) for i, n in enumerate([67, 72, 76, 79, 84])])
    s["mission_complete"] = seq(tone(note_hz(76), 0.1, "square") * 0.35, tone(note_hz(79), 0.1, "square") * 0.35, tone(note_hz(84), 0.45, "square") * env(int(0.45 * SR), 0.003, curve=1.5) * 0.4)
    fan = [72, 72, 72, 72, 68, 70, 72, 70, 72]
    durs = [0.1, 0.1, 0.1, 0.3, 0.3, 0.3, 0.2, 0.1, 0.6]
    s["victory"] = seq(*[(tone(note_hz(n), d, "square") * 0.3 + tone(note_hz(n - 12), d, "tri") * 0.3) * env(int(d * SR), 0.003, curve=0.7) for n, d in zip(fan, durs)])
    s["spring"] = sweep(200, 900, 0.25, "sine") * (1 + 0.5 * np.sin(2 * np.pi * 30 * t(0.25))) * env(int(0.25 * SR), 0.002, curve=1.5) * 0.5
    return s


# ---------------------------------------------------------------- music
def drum_kick(sr):
    d = 0.25
    return sweep(150, 40, d, sr=sr) * env(int(d * sr), 0.001, sr=sr, curve=2.5)


def drum_snare(sr):
    d = 0.18
    return mix(highpass(noise(d, sr), 1500, sr) * env(int(d * sr), 0.001, sr=sr, curve=3) * 0.7,
               tone(190, d, sr=sr) * env(int(d * sr), 0.001, sr=sr, curve=5) * 0.4)


def drum_hat(sr):
    d = 0.05
    return highpass(noise(d, sr), 6000, sr) * env(int(d * sr), 0.001, sr=sr, curve=4) * 0.35


def render_loop(bpm, bars, events, sr=MSR):
    """events: list of (start_beat, samples). Tails wrap to loop start."""
    beat = 60.0 / bpm
    n = int(round(bars * 4 * beat * sr))
    out = np.zeros(n)
    for start, x in events:
        i = int(round(start * beat * sr)) % n
        end = i + len(x)
        if end <= n:
            out[i:end] += x
        else:
            first = n - i
            out[i:] += x[:first]
            rest = x[first:]
            while len(rest):
                k = min(n, len(rest))
                out[:k] += rest[:k]
                rest = rest[k:]
    return out


def inst(n, dur, kind, sr=MSR, vol=0.3):
    f = note_hz(n)
    d = dur
    if kind == "bass":
        x = tone(f, d, "tri", sr) * 0.8 + tone(f, d, "square", sr) * 0.2
        return x * env(len(x), 0.005, sr=sr, curve=1.2) * vol
    if kind == "pluck":
        x = tone(f, d, "square", sr) * 0.5 + tone(f * 2, d, "tri", sr) * 0.3
        return x * env(len(x), 0.002, sr=sr, curve=4) * vol
    if kind == "lead":
        tt = t(d, sr)
        x = np.sign(np.sin(2 * np.pi * f * tt + 0.15 * np.sin(2 * np.pi * 5.5 * tt))) * 0.5
        x = lowpass(x, 3200, sr)
        return x * env(len(x), 0.01, sr=sr, curve=0.8) * vol
    if kind == "pad":
        x = sum(tone(f * m, d, "saw", sr) for m in (1, 1.004, 0.996)) / 3
        x = lowpass(x, 1400, sr)
        e = np.minimum(1, np.minimum(t(d, sr) / 0.3, (d - t(d, sr)) / 0.4 + 0.05))
        return x * np.clip(e, 0, 1) * vol
    raise ValueError(kind)


def song(bpm, bars, progression, scale, seed, style):
    r = np.random.default_rng(seed)
    ev = []
    kick, snare, hat = drum_kick(MSR), drum_snare(MSR), drum_hat(MSR)
    motif = [int(r.integers(0, 5)) for _ in range(8)]
    for bar in range(bars):
        root = progression[bar % len(progression)]
        b0 = bar * 4
        chord = [root, root + (3 if style != "menu" else 4), root + 7]
        if style == "menu" and root in (57, 64):  # minor chords in major key
            chord[1] = root + 3
        ev.append((b0, inst(chord[0] + 12, 4 * 60 / bpm, "pad", vol=0.12)))
        ev.append((b0, inst(chord[1] + 12, 4 * 60 / bpm, "pad", vol=0.09)))
        ev.append((b0, inst(chord[2] + 12, 4 * 60 / bpm, "pad", vol=0.09)))
        bass_pat = [0, 2] if style == "menu" else ([0, 1, 2, 3] if style == "run" else [0, 0.5, 1, 1.5, 2, 2.5, 3, 3.5])
        for k, bb in enumerate(bass_pat):
            nn = root - 12 + (7 if (style != "menu" and k % 4 == 3) else 0)
            ev.append((b0 + bb, inst(nn, (60 / bpm) * (1.6 if style == "menu" else 0.45), "bass", vol=0.32)))
        arp_step = 0.5 if style != "boss" else 0.25
        for k in range(int(4 / arp_step)):
            nn = chord[k % 3] + 24
            ev.append((b0 + k * arp_step, inst(nn, 0.18, "pluck", vol=0.09 if style == "menu" else 0.1)))
        # melody: motif-based, phrase repeats every 2 bars with variation
        if style != "boss" or bar % 2 == 1:
            steps = [0, 1, 1.5, 2, 3] if style == "menu" else [0, 0.5, 1, 1.5, 2, 3, 3.5]
            for k, bb in enumerate(steps):
                deg = (motif[(k + bar) % len(motif)] + (bar // 2) % 3) % len(scale)
                nn = root + 12 + scale[deg]
                if (bar % 4 == 3) and k == len(steps) - 1:
                    nn = root + 24
                dur = (60 / bpm) * (0.9 if style == "menu" else 0.45)
                ev.append((b0 + bb, inst(nn, dur, "lead", vol=0.13)))
        # drums
        if style == "menu":
            for bb in (0, 2):
                ev.append((b0 + bb, kick * 0.5))
            for bb in (1, 3):
                ev.append((b0 + bb, snare * 0.25))
            for k in range(8):
                ev.append((b0 + k * 0.5, hat * 0.5))
        else:
            kicks = (0, 1, 2, 3) if style == "boss" else (0, 1.5, 2, 3.5) if bar % 2 else (0, 2, 2.5)
            for bb in kicks:
                ev.append((b0 + bb, kick * 0.8))
            for bb in (1, 3):
                ev.append((b0 + bb, snare * 0.6))
            for k in range(16 if style == "boss" else 8):
                ev.append((b0 + k * (0.25 if style == "boss" else 0.5), hat * (0.6 if k % 2 == 0 else 0.35)))
    x = render_loop(bpm, bars, ev)
    x = np.tanh(x * 1.6) / np.tanh(1.6)
    return x / (np.max(np.abs(x)) or 1) * 0.8


def ambience(kind, seconds=12.0):
    n = int(seconds * MSR)
    tt = np.arange(n) / MSR
    if kind == "wind":
        base = lowpass(rng.uniform(-1, 1, n), 500, MSR)
        gust = 0.6 + 0.4 * np.sin(2 * np.pi * tt / seconds * 2) * np.sin(2 * np.pi * tt / seconds * 3)
        x = base * gust
    elif kind == "nature":
        x = lowpass(rng.uniform(-1, 1, n), 300, MSR) * 0.3
        for _ in range(14):
            st = rng.uniform(0, seconds - 0.5)
            f0 = rng.uniform(2200, 3800)
            chirp = sweep(f0, f0 * rng.uniform(1.1, 1.4), 0.12, "sine", MSR) * env(int(0.12 * MSR), 0.01, sr=MSR, curve=2) * 0.25
            for rep in range(int(rng.integers(1, 4))):
                i = int((st + rep * 0.16) * MSR)
                if i + len(chirp) < n:
                    x[i:i + len(chirp)] += chirp
    elif kind == "rumble":
        x = lowpass(rng.uniform(-1, 1, n), 120, MSR) * 3 + np.sin(2 * np.pi * 38 * tt) * 0.15
        for _ in range(10):
            i = int(rng.uniform(0, seconds - 0.3) * MSR)
            pop = highpass(rng.uniform(-1, 1, int(0.05 * MSR)), 1500, MSR) * env(int(0.05 * MSR), 0.001, sr=MSR, curve=4) * 0.2
            x[i:i + len(pop)] += pop
    elif kind == "hum":
        x = (np.sin(2 * np.pi * 60 * tt) * 0.2 + np.sin(2 * np.pi * 120 * tt) * 0.1
             + lowpass(rng.uniform(-1, 1, n), 900, MSR) * 0.6 * (0.7 + 0.3 * np.sin(2 * np.pi * tt / seconds)))
    elif kind == "eerie":
        x = sum(np.sin(2 * np.pi * f * tt + np.sin(2 * np.pi * 0.25 * tt) * 2) for f in (110, 164.8, 233.1)) * 0.15
        x += lowpass(rng.uniform(-1, 1, n), 400, MSR) * 0.5
    else:
        raise ValueError(kind)
    # crossfade the end into the start for a seamless loop
    xf = int(1.0 * MSR)
    head, tail = x[:xf].copy(), x[-xf:]
    ramp = np.linspace(0, 1, xf)
    x = x[:-xf]
    x[:xf] = head * ramp + tail * (1 - ramp)
    return x / (np.max(np.abs(x)) or 1) * 0.6


def write_ogg(path, x, sr=MSR):
    tmp = path + ".tmp.wav"
    write_wav(tmp, x, sr)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", tmp, "-c:a", "libvorbis", "-q:a", "3", path], check=True)
    os.remove(tmp)


def main():
    repo = os.path.abspath(sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), ".."))
    sfx_dir = os.path.join(repo, "assets", "audio", "sfx")
    mus_dir = os.path.join(repo, "assets", "audio", "music")
    os.makedirs(sfx_dir, exist_ok=True)
    os.makedirs(mus_dir, exist_ok=True)
    for name, x in sfx().items():
        write_wav(os.path.join(sfx_dir, name + ".wav"), finish(x))
    major = [0, 2, 4, 7, 9, 12, 14]
    minor = [0, 3, 5, 7, 10, 12, 15]
    write_ogg(os.path.join(mus_dir, "menu.ogg"), song(100, 16, [60, 57, 53, 55], major, 11, "menu"))
    write_ogg(os.path.join(mus_dir, "run.ogg"), song(140, 16, [57, 53, 48, 55], minor, 23, "run"))
    write_ogg(os.path.join(mus_dir, "boss.ogg"), song(160, 16, [52, 52, 48, 50], minor, 37, "boss"))
    for k in ("nature", "wind", "rumble", "hum", "eerie"):
        write_ogg(os.path.join(mus_dir, "amb_" + k + ".ogg"), ambience(k))
    for d in (sfx_dir, mus_dir):
        for f in sorted(os.listdir(d)):
            print(f"{os.path.relpath(os.path.join(d, f), repo):40s} {os.path.getsize(os.path.join(d, f)) // 1024:5d} KiB")


if __name__ == "__main__":
    main()
