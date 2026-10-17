#!/usr/bin/env python3
"""Measures the generated audio: decodes every file with ffmpeg and reports duration, peak,
RMS, clipping and (for loops) the sample discontinuity across the loop point.
Usage: python3 -I tools/validate_audio.py [repo_root]   (exit code 1 on failure)"""
import os, subprocess, sys
import numpy as np

def decode(path, sr):
    raw = subprocess.run(["ffmpeg", "-v", "error", "-i", path, "-f", "s16le", "-ac", "1", "-ar", str(sr), "-"],
                         check=True, capture_output=True).stdout
    return np.frombuffer(raw, dtype="<i2").astype(np.float64) / 32768.0

def main():
    repo = os.path.abspath(sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), ".."))
    ok = True
    rows = []
    for sub, sr in (("sfx", 22050), ("music", 32000)):
        d = os.path.join(repo, "assets", "audio", sub)
        for f in sorted(os.listdir(d)):
            x = decode(os.path.join(d, f), sr)
            dur = len(x) / sr
            peak = float(np.max(np.abs(x))) if len(x) else 0.0
            rms = float(np.sqrt(np.mean(x * x))) if len(x) else 0.0
            clipped = int(np.sum(np.abs(x) >= 0.999))
            seam = abs(x[-1] - x[0]) if sub == "music" else 0.0
            # typical step between neighbouring samples, to judge the loop seam against
            step = float(np.percentile(np.abs(np.diff(x)), 99)) if len(x) > 2 else 0.0
            problems = []
            if dur <= 0.02: problems.append("empty")
            if peak < 0.05: problems.append("silent")
            if clipped > 0: problems.append(f"{clipped} clipped samples")
            if sub == "music" and seam > max(0.05, step * 2): problems.append(f"loop seam jump {seam:.3f}")
            if sub == "sfx" and dur > 3.0: problems.append("effect longer than 3 s")
            ok &= not problems
            rows.append(f"| {sub}/{f} | {dur:.2f} s | {peak:.2f} | {rms:.3f} | {seam:.4f} | {'OK' if not problems else ', '.join(problems)} |")
    print("| file | duration | peak | RMS | loop seam | result |")
    print("|---|---|---|---|---|---|")
    print("\n".join(rows))
    sys.exit(0 if ok else 1)

if __name__ == "__main__":
    main()
