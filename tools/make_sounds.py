#!/usr/bin/env python3
"""Synthesizes all game sound effects and music loops (no samples needed).

Outputs OGG Vorbis files into assets/sounds and assets/music.
Run from the repo root:  python3 tools/make_sounds.py   (needs numpy + ffmpeg)
"""
import os
import subprocess
import tempfile
import wave

import numpy as np

SR = 44100
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_SFX = os.path.join(ROOT, "assets", "sounds")
OUT_MUS = os.path.join(ROOT, "assets", "music")
rng = np.random.default_rng(7)


# ------------------------------------------------------------- primitives

def t_(dur):
    return np.arange(int(dur * SR)) / SR


def env(dur, a=0.005, d=0.1, s=0.0, r=0.05, hold=0.0):
    n = int(dur * SR)
    out = np.zeros(n)
    i = 0
    for seg_len, start, end in ((a, 0, 1), (hold, 1, 1), (d, 1, s)):
        k = min(int(seg_len * SR), n - i)
        if k > 0:
            out[i:i + k] = np.linspace(start, end, k, endpoint=False)
            i += k
    out[i:] = s
    k = min(int(r * SR), n)
    if k > 0 and s > 0:
        out[-k:] *= np.linspace(1, 0, k)
    return out


def exp_env(dur, decay, attack=0.002):
    t = t_(dur)
    e = np.exp(-t / decay)
    k = max(1, int(attack * SR))
    e[:k] *= np.linspace(0, 1, k)
    return e


def sweep(f0, f1, dur, curve="exp"):
    t = t_(dur)
    if curve == "exp":
        f = f0 * (f1 / f0) ** (t / dur)
    else:
        f = f0 + (f1 - f0) * (t / dur)
    return 2 * np.pi * np.cumsum(f) / SR


def sine(f, dur, phase=0.0):
    return np.sin(2 * np.pi * f * t_(dur) + phase)


def saw(phase):
    return 2 * ((phase / (2 * np.pi)) % 1.0) - 1


def square(phase, duty=0.5):
    return np.where(((phase / (2 * np.pi)) % 1.0) < duty, 1.0, -1.0)


def noise(dur):
    return rng.uniform(-1, 1, int(dur * SR))


def onepole_lp(x, cutoff):
    cutoff = np.broadcast_to(np.asarray(cutoff, dtype=float), x.shape)
    a = 1 - np.exp(-2 * np.pi * cutoff / SR)
    y = np.empty_like(x)
    acc = 0.0
    for i in range(len(x)):
        acc += a[i] * (x[i] - acc)
        y[i] = acc
    return y


def biquad(x, kind, f, q=0.707):
    """Static RBJ biquad filter."""
    w0 = 2 * np.pi * f / SR
    alpha = np.sin(w0) / (2 * q)
    c = np.cos(w0)
    if kind == "lp":
        b = [(1 - c) / 2, 1 - c, (1 - c) / 2]
    elif kind == "hp":
        b = [(1 + c) / 2, -(1 + c), (1 + c) / 2]
    else:  # band-pass (constant peak)
        b = [alpha, 0, -alpha]
    a = [1 + alpha, -2 * c, 1 - alpha]
    b = [v / a[0] for v in b]
    a = [v / a[0] for v in a]
    y = np.zeros_like(x)
    x1 = x2 = y1 = y2 = 0.0
    for i, xv in enumerate(x):
        yv = b[0] * xv + b[1] * x1 + b[2] * x2 - a[1] * y1 - a[2] * y2
        x2, x1, y2, y1 = x1, xv, y1, yv
        y[i] = yv
    return y


def svf_sweep(x, f0, f1, q=2.0, kind="bp"):
    """State-variable filter with exponentially swept cutoff."""
    n = len(x)
    f = f0 * (f1 / f0) ** (np.arange(n) / max(1, n - 1))
    g = np.tan(np.pi * np.minimum(f, SR * 0.45) / SR)
    k = 1 / q
    ic1 = ic2 = 0.0
    y = np.zeros(n)
    for i in range(n):
        gi = g[i]
        a1 = 1 / (1 + gi * (gi + k))
        a2 = gi * a1
        a3 = gi * a2
        v3 = x[i] - ic2
        v1 = a1 * ic1 + a2 * v3
        v2 = ic2 + a2 * ic1 + a3 * v3
        ic1 = 2 * v1 - ic1
        ic2 = 2 * v2 - ic2
        y[i] = v1 if kind == "bp" else (v2 if kind == "lp" else x[i] - k * v1 - v2)
    return y


def pad_to(x, n):
    return np.pad(x, (0, max(0, n - len(x))))[:n]


def mix(*parts):
    n = max(len(p) for p in parts)
    return sum(pad_to(p, n) for p in parts)


def at(x, offset, total=None):
    k = int(offset * SR)
    y = np.concatenate([np.zeros(k), x])
    return pad_to(y, total) if total else y


def reverb(x, wet=0.25, room=0.82, damp=0.35, predelay=0.012):
    """Freeverb-style mono reverb (comb + allpass)."""
    combs = [1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617]
    aps = [556, 441, 341, 225]
    tail = int(SR * 1.6)
    src = np.concatenate([np.zeros(int(predelay * SR)), x, np.zeros(tail)])
    out = np.zeros_like(src)
    for d in combs:
        buf = np.zeros(d)
        idx = 0
        store = 0.0
        y = np.zeros_like(src)
        for i, v in enumerate(src):
            o = buf[idx]
            store = o * (1 - damp) + store * damp
            buf[idx] = v + store * room
            idx = (idx + 1) % d
            y[i] = o
        out += y
    out *= 0.12
    for d in aps:
        buf = np.zeros(d)
        idx = 0
        y = np.zeros_like(out)
        for i, v in enumerate(out):
            b = buf[idx]
            y[i] = -v + b
            buf[idx] = v + b * 0.5
            idx = (idx + 1) % d
        out = y
    dry = np.concatenate([x, np.zeros(len(src) - len(x))])
    res = dry * (1 - wet * 0.5) + out * wet
    # trim silent tail
    nz = np.nonzero(np.abs(res) > np.max(np.abs(res)) * 2e-3)[0]
    return res[: nz[-1] + 1] if len(nz) else res


def finish(x, peak=0.89, drive=1.0):
    x = np.tanh(x * drive) / np.tanh(drive) if drive != 1.0 else x
    m = np.max(np.abs(x)) or 1
    x = x / m * peak
    f = min(len(x), int(0.004 * SR))
    x[-f:] *= np.linspace(1, 0, f)
    return x


def write_ogg(path, x, stereo=None, quality=5):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tf:
        tmp = tf.name
    data = np.stack([x, stereo], axis=1) if stereo is not None else x[:, None]
    pcm = (np.clip(data, -1, 1) * 32767).astype(np.int16)
    with wave.open(tmp, "wb") as w:
        w.setnchannels(pcm.shape[1])
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", tmp, "-c:a", "libvorbis",
                    "-q:a", str(quality), path], check=True)
    os.remove(tmp)
    print(f"{os.path.relpath(path, ROOT)}: {len(x) / SR:.2f}s")


def bell(f, dur, decay=0.5, bright=1.0):
    t = t_(dur)
    parts = [(1, 1.0, 1.0), (2.0, 0.45 * bright, 0.6), (3.01, 0.25 * bright, 0.4),
             (4.2, 0.12 * bright, 0.25), (5.43, 0.08 * bright, 0.15)]
    out = sum(a * np.sin(2 * np.pi * f * r * t) * np.exp(-t / (decay * dk)) for r, a, dk in parts)
    k = int(0.002 * SR)
    out[:k] *= np.linspace(0, 1, k)
    return out


def marimba(f, dur=0.5):
    t = t_(dur)
    return (np.sin(2 * np.pi * f * t) * np.exp(-t / 0.28)
            + 0.35 * np.sin(2 * np.pi * f * 4 * t) * np.exp(-t / 0.05)
            + 0.15 * np.sin(2 * np.pi * f * 10 * t) * np.exp(-t / 0.012))


def pluck_bass(f, dur):
    ph = 2 * np.pi * f * t_(dur)
    x = 0.6 * saw(ph) + 0.6 * np.sin(ph)
    cutoff = 200 + 1600 * np.exp(-t_(dur) / 0.07)
    return onepole_lp(x, cutoff) * env(dur, 0.004, dur * 0.9, 0.0) * 1.2


def brass(f, dur, vib=0.0):
    t = t_(dur)
    fm = f * (1 + vib * np.sin(2 * np.pi * 5.5 * t) * np.clip(t / 0.25, 0, 1))
    ph = 2 * np.pi * np.cumsum(fm) / SR
    x = saw(ph) + 0.5 * saw(ph * 1.003)
    cutoff = 400 + 2600 * env(dur, 0.04, 0.25, 0.55, 0.08)
    return onepole_lp(x, cutoff) * env(dur, 0.02, 0.2, 0.75, 0.1)


def midi(n):
    return 440.0 * 2 ** ((n - 69) / 12)


# --------------------------------------------------------------------- SFX

def sfx_tap():
    a = np.sin(sweep(1100, 520, 0.07)) * exp_env(0.07, 0.025)
    click = biquad(noise(0.01), "hp", 3000) * np.linspace(1, 0, int(0.01 * SR)) * 0.5
    body = np.sin(sweep(420, 260, 0.09)) * exp_env(0.09, 0.03) * 0.5
    return reverb(finish(mix(a, click, body)), wet=0.12)


def sfx_whoosh():
    d = 0.32
    n = noise(d)
    x = svf_sweep(n, 500, 3200, q=1.4) * np.sin(np.pi * np.linspace(0, 1, len(n))) ** 1.5
    x2 = svf_sweep(noise(d), 300, 1500, q=0.9) * np.sin(np.pi * np.linspace(0, 1, len(n))) ** 2 * 0.6
    return finish(mix(x, x2), 0.7)


def sfx_bonk():
    thud = np.sin(sweep(190, 70, 0.18)) * exp_env(0.18, 0.06)
    knock = biquad(noise(0.06), "bp", 900, 3) * exp_env(0.06, 0.012) * 3
    rattle = biquad(noise(0.12), "bp", 2400, 6) * exp_env(0.12, 0.03) * 1.5
    return reverb(finish(mix(thud, knock, at(rattle, 0.02)), drive=1.6), wet=0.15)


def sfx_place():
    t = t_(0.5)
    clink = sum(a * np.sin(2 * np.pi * f * t) * np.exp(-t / dk)
                for f, a, dk in ((1320, 0.6, 0.18), (2760, 0.35, 0.09), (4120, 0.2, 0.05), (5980, 0.1, 0.03)))
    thump = np.sin(sweep(160, 60, 0.15)) * exp_env(0.15, 0.05) * 1.2
    pop = np.sin(sweep(500, 900, 0.06)) * exp_env(0.06, 0.02) * 0.6
    return reverb(finish(mix(clink * 0.7, thump, pop)), wet=0.2)


def sfx_shoot():
    boom = onepole_lp(noise(0.35), 900) * exp_env(0.35, 0.07) * 2.2
    sub = np.sin(sweep(150, 45, 0.3)) * exp_env(0.3, 0.09) * 1.4
    pew = np.sin(sweep(1500, 280, 0.16)) * exp_env(0.16, 0.05) * 0.45
    crack = biquad(noise(0.02), "hp", 2500) * exp_env(0.02, 0.004)
    return reverb(finish(mix(boom, sub, pew, crack), drive=1.8), wet=0.18)


def sfx_hit():
    crunch = biquad(noise(0.16), "bp", 1800, 1.2) * exp_env(0.16, 0.04) * 2.5
    pop = np.sin(sweep(700, 180, 0.14)) * exp_env(0.14, 0.045) * 1.2
    sparkle = bell(2600, 0.4, 0.12) * 0.25
    return reverb(finish(mix(crunch, pop, at(sparkle, 0.01)), drive=1.5), wet=0.2)


def sfx_explode():
    d = 1.1
    n = noise(d)
    body = onepole_lp(n, 300 + 2200 * np.exp(-t_(d) / 0.12)) * exp_env(d, 0.28) * 2.5
    sub = np.sin(sweep(90, 32, 0.8)) * exp_env(0.8, 0.25) * 1.6
    debris = biquad(noise(0.6), "bp", 3200, 2) * exp_env(0.6, 0.15) * 0.6
    return reverb(finish(mix(body, sub, at(debris, 0.03)), drive=2.2), wet=0.3)


def sfx_coin():
    a = bell(midi(83), 0.12, 0.08) * 0.8
    b = bell(midi(88), 0.6, 0.25)
    return reverb(finish(mix(a, at(b, 0.075))), wet=0.25)


def sfx_star(i):
    notes = [76, 79, 84]
    x = mix(bell(midi(notes[i]), 1.2, 0.5), 0.4 * bell(midi(notes[i] + 12), 0.8, 0.2))
    shimmer = mix(*[at(bell(midi(notes[i] + 12 + k * 4), 0.3, 0.1) * 0.15, 0.04 * k) for k in range(4)])
    return reverb(finish(mix(x, shimmer)), wet=0.4)


def sfx_win():
    total = 2.6
    out = np.zeros(int(total * SR))
    seq = [(72, 0.0), (76, 0.12), (79, 0.24), (84, 0.36)]
    for n, o in seq:
        out += at(mix(brass(midi(n), 0.2) * 0.5, marimba(midi(n), 0.3) * 0.7), o, len(out))
    for n in (72, 76, 79, 84):
        out += at(brass(midi(n), 1.4, vib=0.006) * 0.35, 0.52, len(out))
    out += at(bell(midi(96), 1.5, 0.6) * 0.4, 0.52, len(out))
    # drum hit
    out += at(np.sin(sweep(140, 50, 0.3)) * exp_env(0.3, 0.1), 0.52, len(out))
    out += at(biquad(noise(0.5), "hp", 5000) * exp_env(0.5, 0.15) * 0.4, 0.52, len(out))
    return reverb(finish(out), wet=0.35)


def sfx_lose():
    total = 2.6
    out = np.zeros(int(total * SR))
    notes = [(67, 0.0, 0.35), (66, 0.38, 0.35), (65, 0.76, 0.35), (64, 1.14, 1.2)]
    for n, o, d in notes:
        x = brass(midi(n) / 2, d, vib=0.02 if d > 1 else 0.004)
        # wah: closing filter
        x = onepole_lp(x, 300 + 1500 * np.exp(-t_(d) / (0.15 if d < 1 else 0.5)))
        out += at(x, o, len(out))
    return reverb(finish(out, drive=1.3), wet=0.25)


def sfx_roar():
    d = 1.4
    t = t_(d)
    f0 = 95 * (1 - 0.25 * t / d) * (1 + 0.04 * np.sin(2 * np.pi * 7 * t))
    ph = 2 * np.pi * np.cumsum(f0) / SR
    growl = saw(ph) * (0.7 + 0.3 * np.sin(2 * np.pi * 31 * t))
    breath = noise(d)
    voice = mix(growl * 0.8, breath * 0.6)
    form = (svf_sweep(voice, 500, 380, q=4) * 1.2 + svf_sweep(voice, 1100, 800, q=5)
            + svf_sweep(voice, 2400, 1900, q=6) * 0.5)
    shape = env(d, 0.12, 0.3, 0.75, 0.6)
    sub = np.sin(ph * 0.5) * shape * 0.5
    return reverb(finish(mix(form * shape, sub), drive=2.5), wet=0.35)


def sfx_bite():
    out = np.zeros(int(0.5 * SR))
    for o in (0.0, 0.12):
        cr = biquad(noise(0.1), "bp", 1400, 1.5) * exp_env(0.1, 0.02) * 2
        th = np.sin(sweep(220, 80, 0.12)) * exp_env(0.12, 0.04)
        out += at(mix(cr, th), o, len(out))
    return reverb(finish(out, drive=1.8), wet=0.15)


def sfx_click():
    x = np.sin(sweep(1800, 1200, 0.03)) * exp_env(0.03, 0.008)
    y = np.sin(sweep(600, 400, 0.05)) * exp_env(0.05, 0.015) * 0.5
    return finish(mix(x, y), 0.6)


def sfx_unlock():
    out = np.zeros(int(1.6 * SR))
    for k, n in enumerate((72, 76, 79, 84, 88, 91)):
        out += at(bell(midi(n), 0.9, 0.35) * (0.6 + 0.08 * k), 0.06 * k, len(out))
    return reverb(finish(out), wet=0.45)


def sfx_error():
    out = np.zeros(int(0.42 * SR))
    for o in (0.0, 0.18):
        x = onepole_lp(square(2 * np.pi * 150 * t_(0.14)), 1500) * env(0.14, 0.005, 0.1, 0.6, 0.03)
        out += at(x, o, len(out))
    return finish(out, 0.6)


def sfx_combo():
    out = np.zeros(int(0.7 * SR))
    for k, n in enumerate((79, 83, 86)):
        out += at(marimba(midi(n), 0.4) * 0.8 + bell(midi(n + 12), 0.4, 0.15) * 0.3, 0.05 * k, len(out))
    return reverb(finish(out), wet=0.3)


def sfx_swoosh_in():
    d = 0.5
    x = svf_sweep(noise(d), 4000, 300, q=1.2) * np.sin(np.pi * np.linspace(0, 1, int(d * SR))) ** 1.2
    thump = at(np.sin(sweep(120, 50, 0.2)) * exp_env(0.2, 0.06) * 0.8, 0.38)
    return finish(mix(x, thump), 0.75)


def sfx_scream():
    """Short cartoon 'eek' – a squeaky formant chirp."""
    d = 0.45
    t = t_(d)
    f = 900 + 500 * np.sin(np.pi * t / d) + 30 * np.sin(2 * np.pi * 9 * t)
    ph = 2 * np.pi * np.cumsum(f) / SR
    x = saw(ph) * 0.5 + np.sin(ph) * 0.5
    x = biquad(x, "bp", 2400, 1.5) * 2 + biquad(x, "bp", 1100, 2)
    return reverb(finish(x * env(d, 0.03, 0.1, 0.8, 0.12), 0.6), wet=0.2)


# ------------------------------------------------------------ v2 SFX

def sfx_bomb():
    d = 1.6
    body = onepole_lp(noise(d), 200 + 3000 * np.exp(-t_(d) / 0.08)) * exp_env(d, 0.35) * 3
    sub = np.sin(sweep(70, 25, 1.2)) * exp_env(1.2, 0.4) * 2
    crack = biquad(noise(0.05), "hp", 1500) * exp_env(0.05, 0.01) * 2
    debris = mix(*[at(biquad(noise(0.08), "bp", 2000 + 800 * k, 4) * exp_env(0.08, 0.02) * 0.5, 0.12 + 0.07 * k)
                   for k in range(6)])
    return reverb(finish(mix(body, sub, crack, debris), drive=2.6), wet=0.35)


def sfx_freeze():
    d = 1.4
    shimmer = mix(*[at(bell(2000 + 450 * k, 0.6, 0.18) * 0.25, 0.035 * k) for k in range(10)])
    crackle = biquad(noise(0.6), "hp", 4000) * (np.random.default_rng(3).random(len(noise(0.6))) > 0.985) * 3
    wind = svf_sweep(noise(d), 3000, 800, q=2) * env(d, 0.05, 0.4, 0.3, 0.5) * 0.6
    return reverb(finish(mix(shimmer, crackle, wind)), wet=0.45)


def sfx_freeze_end():
    out = mix(*[at(biquad(noise(0.07), "bp", 2500 + 600 * k, 3) * exp_env(0.07, 0.02), 0.05 * k) for k in range(5)])
    return reverb(finish(out), wet=0.3)


def sfx_rainbow():
    notes = [72, 76, 79, 83, 84, 88, 91, 96]
    out = mix(*[at(bell(midi(n), 0.7, 0.25) * 0.6, 0.05 * k) for k, n in enumerate(notes)])
    gliss = np.sin(sweep(600, 2400, 0.5)) * env(0.5, 0.02, 0.3, 0.3, 0.15) * 0.25
    return reverb(finish(mix(out, gliss)), wet=0.45)


def sfx_shuffle():
    sw = svf_sweep(noise(0.45), 400, 4000, q=1.5) * np.sin(np.pi * np.linspace(0, 1, int(0.45 * SR))) ** 1.5
    clicks = mix(*[at(np.sin(sweep(1800, 900, 0.03)) * exp_env(0.03, 0.008) * 0.6, 0.04 * k + 0.1) for k in range(8)])
    return reverb(finish(mix(sw, clicks)), wet=0.2)


def sfx_armor():
    t = t_(0.6)
    metal = sum(a * np.sin(2 * np.pi * f * t) * np.exp(-t / dk)
                for f, a, dk in ((620, 0.7, 0.25), (1430, 0.5, 0.15), (2210, 0.4, 0.1), (3550, 0.3, 0.06)))
    hit = biquad(noise(0.05), "bp", 3000, 2) * exp_env(0.05, 0.01) * 1.5
    return reverb(finish(mix(metal, hit), drive=1.4), wet=0.25)


def sfx_armor_break():
    shards = mix(*[at(bell(2500 + 900 * (k % 4), 0.25, 0.06) * 0.5, 0.025 * k) for k in range(12)])
    crunch = biquad(noise(0.3), "bp", 2200, 1.2) * exp_env(0.3, 0.07) * 2
    return reverb(finish(mix(shards, crunch, sfx_armor() * 0.5), drive=1.5), wet=0.3)


def sfx_boss_roar():
    d = 2.2
    t = t_(d)
    f0 = 60 * (1 - 0.3 * t / d) * (1 + 0.05 * np.sin(2 * np.pi * 6 * t))
    ph = 2 * np.pi * np.cumsum(f0) / SR
    growl = saw(ph) * (0.7 + 0.3 * np.sin(2 * np.pi * 23 * t))
    voice = mix(growl, noise(d) * 0.7)
    form = svf_sweep(voice, 420, 300, q=4) * 1.4 + svf_sweep(voice, 900, 650, q=5) + svf_sweep(voice, 2000, 1500, q=6) * 0.4
    shape = env(d, 0.2, 0.4, 0.8, 0.8)
    sub = np.sin(ph * 0.5) * shape * 0.8
    return reverb(finish(mix(form * shape, sub), drive=3), wet=0.4)


def sfx_heart_lose():
    out = mix(*[at(np.sin(2 * np.pi * midi(n) * t_(0.25)) * env(0.25, 0.01, 0.2, 0, 0.02) * 0.6, 0.18 * k)
                for k, n in enumerate((76, 72, 67))])
    return reverb(finish(out), wet=0.3)


def sfx_spin_tick():
    return finish(mix(np.sin(sweep(2200, 1500, 0.025)) * exp_env(0.025, 0.006),
                      biquad(noise(0.015), "hp", 3000) * exp_env(0.015, 0.003) * 0.4), 0.5)


def sfx_reward():
    out = mix(sfx_coin(), at(mix(*[at(bell(midi(n), 0.8, 0.3) * 0.5, 0.07 * k) for k, n in enumerate((79, 84, 88, 91))]), 0.1))
    return reverb(finish(out), wet=0.4)


def sfx_chest_open():
    creak = svf_sweep(saw(2 * np.pi * np.cumsum(140 + 60 * np.sin(np.linspace(0, 9, int(0.5 * SR)))) / SR), 600, 1200, q=6) * env(0.5, 0.05, 0.3, 0.5, 0.1) * 0.6
    thud = np.sin(sweep(150, 60, 0.2)) * exp_env(0.2, 0.06)
    sparkle = mix(*[at(bell(2000 + 300 * k, 0.5, 0.15) * 0.3, 0.45 + 0.04 * k) for k in range(10)])
    return reverb(finish(mix(creak, at(thud, 0.45), sparkle)), wet=0.4)


def sfx_purchase():
    ding = mix(bell(midi(91), 0.8, 0.3), at(bell(midi(96), 0.9, 0.35), 0.09))
    coins = mix(*[at(bell(3000 + 400 * (k % 3), 0.15, 0.04) * 0.3, 0.03 * k) for k in range(8)])
    drawer = biquad(noise(0.12), "bp", 900, 2) * exp_env(0.12, 0.04)
    return reverb(finish(mix(drawer, at(ding, 0.05), at(coins, 0.08))), wet=0.3)


def sfx_achievement():
    out = np.zeros(int(1.6 * SR))
    for k, n in enumerate((67, 72, 76, 79)):
        out += at(brass(midi(n), 0.16) * 0.5, 0.09 * k, len(out))
    for n in (72, 76, 79, 84):
        out += at(brass(midi(n), 0.9, vib=0.005) * 0.3, 0.38, len(out))
    out += at(bell(midi(96), 1.0, 0.5) * 0.4, 0.38, len(out))
    return reverb(finish(out), wet=0.35)


def sfx_page():
    x = svf_sweep(noise(0.35), 1200, 5000, q=0.8) * env(0.35, 0.02, 0.2, 0.3, 0.1)
    return finish(x, 0.6)


def sfx_node_pop():
    x = np.sin(sweep(400, 1100, 0.08)) * exp_env(0.08, 0.03)
    return reverb(finish(mix(x, at(bell(1760, 0.3, 0.1) * 0.3, 0.03))), wet=0.2)


def sfx_hammer():
    thud = np.sin(sweep(200, 70, 0.2)) * exp_env(0.2, 0.06) * 1.5
    wood = biquad(noise(0.1), "bp", 700, 2) * exp_env(0.1, 0.02) * 2
    return reverb(finish(mix(thud, wood, sfx_armor() * 0.3), drive=1.6), wet=0.2)


def sfx_no_lives():
    out = mix(*[at(onepole_lp(square(2 * np.pi * midi(n) * t_(0.22)), 1800) * env(0.22, 0.01, 0.15, 0.4, 0.05) * 0.5, 0.2 * k)
                for k, n in enumerate((64, 60))])
    return reverb(finish(out), wet=0.2)


V2_SFX = {
    "bomb": sfx_bomb, "freeze": sfx_freeze, "freeze_end": sfx_freeze_end, "rainbow": sfx_rainbow,
    "shuffle": sfx_shuffle, "armor": sfx_armor, "armor_break": sfx_armor_break, "boss_roar": sfx_boss_roar,
    "heart_lose": sfx_heart_lose, "spin_tick": sfx_spin_tick, "reward": sfx_reward, "chest_open": sfx_chest_open,
    "purchase": sfx_purchase, "achievement": sfx_achievement, "page": sfx_page, "node_pop": sfx_node_pop,
    "hammer": sfx_hammer, "no_lives": sfx_no_lives,
}


# -------------------------------------------------------------------- music

def drum_kick():
    return mix(np.sin(sweep(150, 45, 0.25)) * exp_env(0.25, 0.08) * 1.1,
               biquad(noise(0.01), "hp", 3000) * np.linspace(0.4, 0, int(0.01 * SR)))


def drum_snare():
    return mix(biquad(noise(0.2), "hp", 1200) * exp_env(0.2, 0.05) * 0.7,
               np.sin(sweep(260, 180, 0.1)) * exp_env(0.1, 0.03) * 0.5)


def drum_hat(open_=False):
    d = 0.18 if open_ else 0.05
    return biquad(noise(d), "hp", 7000) * exp_env(d, 0.06 if open_ else 0.015) * 0.35


def chord_stab(notes, dur):
    t = t_(dur)
    x = sum(np.sin(2 * np.pi * midi(n) * t) + 0.3 * np.sin(4 * np.pi * midi(n) * t) for n in notes)
    return x * env(dur, 0.005, dur * 0.8, 0.0) / len(notes)


def pad(notes, dur):
    t = t_(dur)
    x = 0
    for n in notes:
        for det in (-0.004, 0.004):
            ph = 2 * np.pi * midi(n) * (1 + det) * t
            x = x + saw(ph)
    x = onepole_lp(x / (len(notes) * 2), 1400)
    return x * env(dur, 0.3, 0.2, 0.8, 0.4)


def render_song(bpm, bars, prog, melody, drums=True, stabs=True, lead="marimba", swing=0.0):
    beat = 60.0 / bpm
    bar = beat * 4
    total = bar * bars
    n = int(total * SR)
    L = np.zeros(n + int(4 * SR))
    R = np.zeros_like(L)

    def put(sig, time, pan=0.0, gain=1.0):
        i = int(time * SR)
        j = min(len(L), i + len(sig))
        s = sig[: j - i] * gain
        L[i:j] += s * (1 - max(0, pan))
        R[i:j] += s * (1 + min(0, pan))

    kick, snare = drum_kick(), drum_snare()
    hat, ohat = drum_hat(), drum_hat(True)
    for b in range(bars):
        root, chord = prog[b % len(prog)]
        t0 = b * bar
        # bass: root on 1, octave/fifth movement
        for k, (off, iv, dur) in enumerate(((0, 0, 0.9), (1.5, 0, 0.4), (2, 7, 0.9), (3, 12, 0.4), (3.5, 7, 0.4))):
            put(pluck_bass(midi(root - 24 + iv), beat * dur), t0 + off * beat, gain=0.55)
        put(pad([n_ - 12 for n_ in chord], bar), t0, gain=0.10)
        if stabs:
            for off in (0.5, 1.5, 2.5, 3.5):
                put(chord_stab(chord, beat * 0.35), t0 + off * beat, pan=0.3, gain=0.16)
        if drums:
            for off in (0, 2, 2.5):
                put(kick, t0 + off * beat, gain=0.7)
            for off in (1, 3):
                put(snare, t0 + off * beat, gain=0.45)
            for k in range(8):
                sw = swing * beat if k % 2 else 0
                put(ohat if k == 7 else hat, t0 + k * beat / 2 + sw, pan=-0.3, gain=0.5)
    # melody: list of (bar, beat, midi, length_beats)
    for (b, off, note, ln) in melody:
        t = b * bar + off * beat
        if lead == "marimba":
            sig = marimba(midi(note), max(0.4, ln * beat))
            put(sig, t, pan=-0.15, gain=0.32)
            put(bell(midi(note + 12), 0.5, 0.15), t, pan=0.25, gain=0.05)
        else:
            put(bell(midi(note), max(0.6, ln * beat), 0.35), t, gain=0.22)
    # reverb send per channel, then wrap tail to make a seamless loop
    Lr = reverb(L[: n + int(2 * SR)], wet=0.22)
    Rr = reverb(R[: n + int(2 * SR)], wet=0.22, room=0.8)
    out = []
    for ch in (Lr, Rr):
        ch = pad_to(ch, n + int(3 * SR))
        loop = ch[:n].copy()
        tail = ch[n:]
        loop[: len(tail)] += tail[: n]
        out.append(loop)
    peak = max(np.max(np.abs(out[0])), np.max(np.abs(out[1])))
    return [np.tanh(o / peak * 1.1) * 0.82 for o in out]


def song_game():
    # A minor adventure: Am F C G
    prog = [(57, [69, 72, 76]), (53, [65, 69, 72]), (60, [67, 72, 76]), (55, [67, 71, 74])]
    m = []
    phrase_a = [(0, 76, 0.5), (0.5, 79, 0.5), (1, 81, 1), (2, 79, 0.5), (2.5, 76, 0.5), (3, 74, 1)]
    phrase_b = [(0, 72, 0.5), (0.5, 74, 0.5), (1, 76, 0.5), (1.5, 72, 0.5), (2, 69, 1.5), (3.5, 72, 0.5)]
    phrase_c = [(0, 76, 0.5), (0.5, 76, 0.5), (1, 79, 0.5), (1.5, 81, 0.5), (2, 84, 1), (3, 83, 1)]
    phrase_d = [(0, 79, 1), (1, 76, 0.5), (1.5, 74, 0.5), (2, 71, 1), (3, 74, 0.5), (3.5, 76, 0.5)]
    order = [phrase_a, phrase_b, phrase_c, phrase_d, phrase_a, phrase_b, phrase_c,
             [(0, 81, 1), (1, 79, 0.5), (1.5, 76, 0.5), (2, 74, 0.5), (2.5, 71, 0.5), (3, 69, 1)]]
    for b, ph in enumerate(order):
        for off, note, ln in ph:
            m.append((b, off, note, ln))
    # second half: same melody an octave up sparkle every other note
    for b, ph in enumerate(order):
        for k, (off, note, ln) in enumerate(ph):
            m.append((b + 8, off, note, ln))
            if k % 2 == 0:
                m.append((b + 8, off, note + 12, ln * 0.5))
    return render_song(128, 16, prog, m, swing=0.0)


def song_menu():
    prog = [(60, [64, 67, 72]), (55, [62, 67, 71]), (57, [64, 69, 72]), (53, [65, 69, 72])]
    m = []
    mel = [[(0, 79, 1), (1, 76, 1), (2, 72, 1), (3, 76, 1)],
           [(0, 74, 1.5), (1.5, 79, 0.5), (2, 79, 2)],
           [(0, 76, 1), (1, 72, 1), (2, 69, 1), (3, 72, 1)],
           [(0, 77, 1), (1, 76, 1), (2, 74, 2)]]
    for rep in range(2):
        for b, ph in enumerate(mel):
            for off, note, ln in ph:
                m.append((rep * 4 + b, off, note + (12 if rep else 0) * 0, ln))
    return render_song(100, 8, prog, m, drums=True, stabs=False, lead="bell", swing=0.12)


def song_map():
    # bright, bouncy C major adventure for the world map
    prog = [(60, [64, 67, 72]), (57, [64, 69, 72]), (53, [65, 69, 72]), (55, [67, 71, 74])]
    mel = [[(0, 72, 0.5), (0.5, 76, 0.5), (1, 79, 1), (2, 76, 0.5), (2.5, 74, 0.5), (3, 72, 1)],
           [(0, 69, 0.5), (0.5, 72, 0.5), (1, 76, 1), (2, 74, 1), (3, 72, 1)],
           [(0, 77, 0.5), (0.5, 76, 0.5), (1, 74, 0.5), (1.5, 72, 0.5), (2, 69, 1), (3, 72, 1)],
           [(0, 74, 1), (1, 79, 0.5), (1.5, 77, 0.5), (2, 76, 0.5), (2.5, 74, 0.5), (3, 71, 1)]]
    m = []
    for rep in range(2):
        for b, ph in enumerate(mel):
            for off, note, ln in ph:
                m.append((rep * 4 + b, off, note + (12 if rep and off % 1 == 0 else 0), ln))
    return render_song(116, 8, prog, m, swing=0.1)


def song_boss():
    # driving minor boss theme
    prog = [(57, [69, 72, 76]), (57, [69, 72, 76]), (53, [65, 69, 72]), (52, [64, 68, 71])]
    riff = [(0, 69, 0.5), (0.5, 69, 0.25), (0.75, 72, 0.25), (1, 76, 0.5), (1.5, 75, 0.5), (2, 74, 0.5),
            (2.5, 72, 0.5), (3, 71, 0.5), (3.5, 68, 0.5)]
    m = []
    for b in range(8):
        for off, note, ln in riff:
            m.append((b, off, note + (0 if b < 4 else 12), ln))
    return render_song(144, 8, prog, m)


def main():
    import sys
    if len(sys.argv) > 1 and sys.argv[1] == "v2":
        for name, fn in V2_SFX.items():
            write_ogg(os.path.join(OUT_SFX, f"{name}.ogg"), fn(), quality=4)
        l, r = song_map()
        write_ogg(os.path.join(OUT_MUS, "map.ogg"), l, r, quality=4)
        l, r = song_boss()
        write_ogg(os.path.join(OUT_MUS, "boss.ogg"), l, r, quality=4)
        return
    sfx = {
        "tap": sfx_tap, "whoosh": sfx_whoosh, "bonk": sfx_bonk, "place": sfx_place,
        "shoot": sfx_shoot, "hit": sfx_hit, "explode": sfx_explode, "coin": sfx_coin,
        "win": sfx_win, "lose": sfx_lose, "roar": sfx_roar, "bite": sfx_bite,
        "click": sfx_click, "unlock": sfx_unlock, "error": sfx_error, "combo": sfx_combo,
        "swoosh_in": sfx_swoosh_in, "scream": sfx_scream,
    }
    for name, fn in sfx.items():
        write_ogg(os.path.join(OUT_SFX, f"{name}.ogg"), fn(), quality=4)
    for i in range(3):
        write_ogg(os.path.join(OUT_SFX, f"star{i + 1}.ogg"), sfx_star(i), quality=4)
    l, r = song_game()
    write_ogg(os.path.join(OUT_MUS, "game.ogg"), l, r, quality=4)
    l, r = song_menu()
    write_ogg(os.path.join(OUT_MUS, "menu.ogg"), l, r, quality=4)
    for name, fn in V2_SFX.items():
        write_ogg(os.path.join(OUT_SFX, f"{name}.ogg"), fn(), quality=4)
    l, r = song_map()
    write_ogg(os.path.join(OUT_MUS, "map.ogg"), l, r, quality=4)
    l, r = song_boss()
    write_ogg(os.path.join(OUT_MUS, "boss.ogg"), l, r, quality=4)


if __name__ == "__main__":
    main()
