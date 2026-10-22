"""Synthesizes every sound effect and both music loops (no samples needed) and
encodes them to OGG in app/src/main/assets/sfx. Requires numpy + ffmpeg.

usage: python3 tools/make_sounds.py
"""
import os
import subprocess
import tempfile
import wave

import numpy as np

SR = 44100
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "app/src/main/assets/sfx")
os.makedirs(OUT, exist_ok=True)
rng = np.random.default_rng(7)


def t_axis(dur):
    return np.arange(int(SR * dur)) / SR


def noise(dur):
    return rng.uniform(-1, 1, int(SR * dur))


def band(x, lo, hi):
    """Brick-wall-ish band-pass via FFT with soft edges."""
    spec = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    lo_g = 1 / (1 + (lo / np.maximum(f, 1)) ** 4) if lo > 0 else 1
    hi_g = 1 / (1 + (f / hi) ** 4)
    return np.fft.irfft(spec * lo_g * hi_g, len(x))


def env(n, a, d, sustain=0.0, curve=4.0):
    """Attack (seconds) then exponential-ish decay over the remaining samples."""
    x = np.zeros(n)
    na = max(1, int(a * SR))
    x[:na] = np.linspace(0, 1, na)
    rest = n - na
    if rest > 0:
        k = np.linspace(0, 1, rest)
        x[na:] = sustain + (1 - sustain) * np.exp(-curve * k * (len(k) / SR) / max(d, 1e-3))
    return x


def norm(x, peak=0.9):
    m = np.max(np.abs(x))
    return x * (peak / m) if m > 0 else x


def fade(x, fin=0.005, fout=0.02):
    x = x.copy()
    a, b = int(fin * SR), int(fout * SR)
    if a:
        x[:a] *= np.linspace(0, 1, a)
    if b:
        x[-b:] *= np.linspace(1, 0, b)
    return x


def write(name, x, stereo=False, q=5):
    x = np.clip(x, -1, 1)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        path = tmp.name
    with wave.open(path, "wb") as w:
        w.setnchannels(2 if stereo else 1)
        w.setsampwidth(2)
        w.setframerate(SR)
        data = (x.T if stereo else x)
        w.writeframes((data * 32767).astype("<i2").tobytes())
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", path, "-c:a", "libvorbis",
                    "-q:a", str(q), os.path.join(OUT, name + ".ogg")], check=True)
    os.remove(path)


# ---------------------------------------------------------------- effects
def click():
    t = t_axis(0.09)
    x = np.sin(2 * np.pi * (1800 - 9000 * t) * t) * env(len(t), 0.001, 0.02)
    x += band(noise(0.09), 2000, 8000) * env(len(t), 0.0005, 0.008) * 0.6
    return fade(norm(x, 0.7))


def pickup():
    t = t_axis(0.22)
    f = 380 + 900 * t / 0.22
    x = np.sin(2 * np.pi * np.cumsum(f) / SR) * env(len(t), 0.004, 0.08)
    x += band(noise(0.22), 300, 2500) * env(len(t), 0.002, 0.03) * 0.5  # cardboard rustle
    return fade(norm(x, 0.75))


def whoosh():
    d = 0.35
    t = t_axis(d)
    shape = np.sin(np.pi * t / d) ** 2
    x = band(noise(d), 400, 3000) * shape
    return fade(norm(x, 0.5))


def lighter():
    # flint wheel scratch, a click, then the gas flame roar
    scratch = band(noise(0.12), 2500, 9000) * env(int(0.12 * SR), 0.002, 0.05)
    clk = np.zeros(int(0.02 * SR))
    clk[:60] = rng.uniform(-1, 1, 60)
    flame_d = 0.6
    ft = t_axis(flame_d)
    flame = band(noise(flame_d), 150, 1400) * np.minimum(1, ft / 0.04) * np.exp(-ft * 3.5)
    x = np.concatenate([scratch * 0.8, clk, flame * 1.1])
    return fade(norm(x, 0.8))


def inhale():
    d = 0.55
    t = t_axis(d)
    shape = np.minimum(1, t / 0.35) * np.minimum(1, (d - t) / 0.08)
    x = band(noise(d), 600, 4200) * shape
    # faint crackle of tobacco burning
    crack = np.zeros(len(t))
    for i in rng.integers(0, len(t) - 200, 14):
        crack[i:i + 120] += rng.uniform(-1, 1, 120) * np.linspace(1, 0, 120)
    x = x + band(crack, 2000, 10000) * 0.35
    return fade(norm(x, 0.6))


def exhale():
    d = 0.85
    t = t_axis(d)
    shape = np.minimum(1, t / 0.05) * np.exp(-t * 2.4)
    x = band(noise(d), 250, 2400) * shape
    return fade(norm(x, 0.65), fout=0.1)


def cough():
    parts = []
    for i, d in enumerate([0.16, 0.13, 0.19]):
        t = t_axis(d)
        n = band(noise(d), 300, 2800) * env(len(t), 0.005, 0.06)
        voice = np.sign(np.sin(2 * np.pi * (170 - i * 15) * t)) * env(len(t), 0.004, 0.05) * 0.25
        parts.append(n + band(voice, 100, 1800))
        parts.append(np.zeros(int(0.06 * SR)))
    return fade(norm(np.concatenate(parts), 0.8))


def heartbeat():
    def thump(f, d, amp):
        t = t_axis(d)
        return np.sin(2 * np.pi * f * t * (1 - 0.3 * t / d)) * env(len(t), 0.004, 0.06) * amp
    x = np.concatenate([thump(58, 0.16, 1.0), np.zeros(int(0.09 * SR)), thump(50, 0.2, 0.75)])
    return fade(norm(x, 0.95))


def explosion():
    d = 2.2
    t = t_axis(d)
    boom = np.sin(2 * np.pi * np.cumsum(90 * np.exp(-t * 2.2) + 28) / SR) * np.exp(-t * 2.0)
    body = band(noise(d), 30, 900) * np.exp(-t * 2.4)
    crack = band(noise(d), 1500, 9000) * np.exp(-t * 14)
    debris = np.zeros(len(t))
    for i in rng.integers(int(0.2 * SR), len(t) - 800, 40):
        debris[i:i + 600] += rng.uniform(-1, 1, 600) * np.linspace(1, 0, 600) * rng.uniform(0.1, 0.4)
    x = boom * 1.1 + body * 0.9 + crack * 0.6 + band(debris, 1500, 7000) * 0.5
    x = np.tanh(x * 1.8)
    return fade(norm(x, 0.95), fout=0.3)


def sizzle():
    d = 1.0
    t = t_axis(d)
    x = np.zeros(len(t))
    for i in rng.integers(0, len(t) - 300, 120):
        x[i:i + 200] += rng.uniform(-1, 1, 200) * np.linspace(1, 0, 200)
    x = band(x, 2500, 12000) * np.minimum(1, (d - t) / 0.2)
    return fade(norm(x, 0.45))


def score_ding():
    t = t_axis(0.35)
    x = sum(np.sin(2 * np.pi * f * t) * a for f, a in [(1318.5, 1), (2637, 0.3), (1975.5, 0.25)])
    return fade(norm(x * env(len(t), 0.002, 0.12), 0.55))


def perfect():
    notes = [1046.5, 1318.5, 1568, 2093]
    parts = []
    for f in notes:
        t = t_axis(0.08)
        parts.append((np.sin(2 * np.pi * f * t) + 0.3 * np.sin(4 * np.pi * f * t)) * env(len(t), 0.002, 0.05))
    t = t_axis(0.3)
    parts.append((np.sin(2 * np.pi * 2093 * t) + 0.3 * np.sin(4 * np.pi * 2093 * t)) * env(len(t), 0.002, 0.12))
    return fade(norm(np.concatenate(parts), 0.55))


def miss():
    t = t_axis(0.25)
    f = 300 - 140 * t / 0.25
    x = np.sign(np.sin(2 * np.pi * np.cumsum(f) / SR)) * env(len(t), 0.003, 0.1)
    return fade(norm(band(x, 80, 1500), 0.45))


def gameover():
    # sad trombone: four descending notes, last one wobbles
    notes = [(392, 0.32), (370, 0.32), (349, 0.32), (330, 1.1)]
    parts = []
    for i, (f, d) in enumerate(notes):
        t = t_axis(d)
        vib = 1 + (0.025 * np.sin(2 * np.pi * 6 * t) * np.minimum(1, t / 0.2) if i == 3 else 0)
        ph = np.cumsum(f * vib) / SR
        saw = 2 * (ph % 1) - 1
        e = np.minimum(1, t / 0.03) * np.minimum(1, (d - t) / 0.06)
        parts.append(band(saw, 80, 1600) * e)
    return fade(norm(np.concatenate(parts), 0.7))


def new_record():
    seq = [(523.25, .1), (659.25, .1), (783.99, .1), (1046.5, .35)]
    parts = []
    for f, d in seq:
        t = t_axis(d)
        tone = sum(np.sin(2 * np.pi * f * k * t) / k for k in (1, 2, 3))
        parts.append(tone * env(len(t), 0.004, d * 0.6))
    return fade(norm(np.concatenate(parts), 0.6))


def pop():
    t = t_axis(0.1)
    x = np.sin(2 * np.pi * (900 - 5000 * t) * t) * env(len(t), 0.001, 0.03)
    return fade(norm(x, 0.6))


# ---------------------------------------------------------------- music
def midi(n):
    return 440.0 * 2 ** ((n - 69) / 12)


def ep_note(f, d, vel=1.0):
    """Electric piano: sine + bell partial, tremolo, soft decay."""
    t = t_axis(d)
    x = np.sin(2 * np.pi * f * t) + 0.35 * np.sin(2 * np.pi * 2 * f * t) * np.exp(-t * 6)
    x += 0.12 * np.sin(2 * np.pi * 7.01 * f * t) * np.exp(-t * 18)
    x *= np.exp(-t * 1.1) * np.minimum(1, t / 0.006) * np.minimum(1, (d - t) / 0.05)
    x *= 1 + 0.15 * np.sin(2 * np.pi * 4.5 * t)
    return x * vel


def bass_note(f, d, vel=1.0):
    t = t_axis(d)
    x = np.sin(2 * np.pi * f * t) + 0.25 * np.sin(4 * np.pi * f * t) + 0.08 * np.sin(6 * np.pi * f * t)
    x *= np.exp(-t * 2.5) * np.minimum(1, t / 0.004) * np.minimum(1, (d - t) / 0.03)
    return x * vel


def add(buf, x, at):
    i = int(at * SR)
    j = min(len(buf), i + len(x))
    buf[i:j] += x[: j - i]


def drum_kick(d=0.3):
    t = t_axis(d)
    return np.sin(2 * np.pi * np.cumsum(50 + 110 * np.exp(-t * 30)) / SR) * np.exp(-t * 9)


def drum_snare(d=0.2):
    t = t_axis(d)
    return band(noise(d), 1200, 8000) * np.exp(-t * 20) + np.sin(2 * np.pi * 190 * t) * np.exp(-t * 25) * 0.5


def hat(d=0.06, open_=False):
    t = t_axis(d)
    return band(noise(d), 6000, 14000) * np.exp(-t * (12 if open_ else 60))


def brush(d=0.25):
    t = t_axis(d)
    return band(noise(d), 2500, 9000) * np.sin(np.pi * t / d) ** 2


def lounge_loop():
    """Smooth lounge jazz, 84 bpm swing, 8 bars ii-V-I-vi progression."""
    bpm = 84
    beat = 60 / bpm
    bars = 8
    total = bars * 4 * beat
    L = np.zeros(int(total * SR) + SR)
    R = np.zeros_like(L)
    prog = [  # (bass root midi, chord voicing midi)
        (38, [53, 57, 60, 64]),  # Dm9
        (43, [53, 57, 59, 64]),  # G13
        (36, [52, 55, 59, 62]),  # Cmaj9
        (45, [55, 58, 61, 64]),  # A7b9-ish
        (38, [53, 57, 60, 64]),
        (43, [53, 57, 59, 64]),
        (41, [52, 57, 60, 64]),  # Fmaj7
        (40, [50, 55, 59, 62]),  # Em7 -> back to Dm
    ]
    swing = beat * 0.66
    for b, (root, chord) in enumerate(prog):
        t0 = b * 4 * beat
        # comping: on beat 1 and the swung "and" of 2
        for at, vel in [(0, 0.8), (beat + swing, 0.55)]:
            for k, n in enumerate(chord):
                x = ep_note(midi(n), beat * 2.2, vel * 0.22)
                add(L, x * (0.6 + 0.1 * k), t0 + at + k * 0.008)
                add(R, x * (0.9 - 0.1 * k), t0 + at + k * 0.008)
        # walking bass
        walk = [root, root + 4, root + 7, root + 9] if b % 2 == 0 else [root, root + 3, root + 5, root + 6]
        for i, n in enumerate(walk):
            x = bass_note(midi(n), beat * 0.95, 0.55)
            add(L, x, t0 + i * beat)
            add(R, x, t0 + i * beat)
        # brushes and ride
        for i in range(4):
            add(L, brush(beat * 0.9) * 0.05, t0 + i * beat)
            add(R, brush(beat * 0.9) * 0.05, t0 + i * beat)
            add(R, hat(0.25, True) * 0.05, t0 + i * beat)
            if i % 2 == 1:
                add(L, hat(0.12, True) * 0.04, t0 + i * beat + swing)
    # simple melodic line on top (vibraphone-ish)
    melody = [(76, 0, 1.5), (74, 1.66, .34), (72, 2, 2), (71, 4, 1.5), (72, 5.66, .34), (74, 6, 2),
              (76, 8, 1), (79, 9, 1), (76, 10, 2), (73, 12, 3), (76, 16, 1.5), (74, 17.66, .34),
              (72, 18, 2), (71, 20, 3), (72, 24, 2), (74, 26, 2), (71, 28, 4)]
    for n, at, d in melody:
        x = ep_note(midi(n), d * beat + 0.4, 0.17)
        add(L, x * 0.8, at * beat)
        add(R, x * 1.0, at * beat)
    n = int(total * SR)
    # wrap the tail into the start so the loop is seamless
    L[: len(L) - n] += L[n:]
    R[: len(R) - n] += R[n:]
    st = np.stack([L[:n], R[:n]])
    return norm(st, 0.6)


def game_loop():
    """Upbeat funky groove, 112 bpm, 8 bars."""
    bpm = 112
    beat = 60 / bpm
    bars = 8
    total = bars * 4 * beat
    L = np.zeros(int(total * SR) + SR)
    R = np.zeros_like(L)
    roots = [40, 40, 43, 45, 40, 40, 38, 43]  # E E G A E E D G
    s = beat / 4
    bass_pat = [0, None, 12, None, 0, 0, None, 10, 0, None, 7, None, 10, None, 12, None]
    for b, root in enumerate(roots):
        t0 = b * 4 * beat
        for i, off in enumerate(bass_pat):
            if off is None:
                continue
            x = bass_note(midi(root + off), s * 1.6, 0.5)
            add(L, x, t0 + i * s)
            add(R, x, t0 + i * s)
        for i in range(4):
            add(L, drum_kick() * 0.5, t0 + i * beat) if i in (0, 2) else None
            add(R, drum_kick() * 0.5, t0 + i * beat) if i in (0, 2) else None
            if i in (1, 3):
                add(L, drum_snare() * 0.25, t0 + i * beat)
                add(R, drum_snare() * 0.25, t0 + i * beat)
        for i in range(16):
            v = 0.07 if i % 2 == 0 else 0.04
            add(R, hat(0.05) * v, t0 + i * s)
            add(L, hat(0.05) * v * 0.6, t0 + i * s)
        # chord stabs on the off-beats
        chord = [root + 24 + 3, root + 24 + 7, root + 24 + 10]
        for i in (2, 6, 10, 14):
            for n in chord:
                x = ep_note(midi(n), s * 1.4, 0.13)
                add(L, x * 1.0, t0 + i * s)
                add(R, x * 0.7, t0 + i * s)
    lead = [(64, 0, .5), (67, .75, .25), (69, 1, .5), (67, 1.5, .5), (64, 2, 1), (62, 3.5, .5),
            (64, 4, .5), (67, 4.75, .25), (71, 5, .5), (69, 5.5, .5), (67, 6, 2)]
    for rep in (0, 16):
        for n, at, d in lead:
            x = ep_note(midi(n + 12), d * beat + 0.15, 0.12)
            add(L, x * 0.7, (rep + at) * beat)
            add(R, x, (rep + at) * beat)
    n = int(total * SR)
    L[: len(L) - n] += L[n:]
    R[: len(R) - n] += R[n:]
    return norm(np.stack([np.tanh(L[:n] * 1.3), np.tanh(R[:n] * 1.3)]), 0.55)


EFFECTS = {
    "click": click, "pickup": pickup, "whoosh": whoosh, "lighter": lighter,
    "inhale": inhale, "exhale": exhale, "cough": cough, "heartbeat": heartbeat,
    "explosion": explosion, "sizzle": sizzle, "ding": score_ding, "perfect": perfect,
    "miss": miss, "gameover": gameover, "new_record": new_record, "pop": pop,
}
for name, fn in EFFECTS.items():
    write(name, fn())
write("music_lobby", lounge_loop(), stereo=True, q=3)
write("music_game", game_loop(), stereo=True, q=3)
print("done")
