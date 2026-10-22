#!/usr/bin/env python3
"""Builds runtime game assets from the source art in art/.

- Trims, resizes and alpha-bleeds every sprite, then packs them into
  libGDX texture atlas pages (assets/sprites.atlas + pngs).
- Generates helper sprites (arrows, panels, glows) procedurally.
- Writes the background, launcher icons and bitmap fonts.

Run from the repo root:  python3 tools/build_assets.py
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ART = os.path.join(ROOT, "art")
OUT = os.path.join(ROOT, "assets")
ANDROID_RES = os.path.join(ROOT, "android", "res")

PAGE = 2048
PAD = 6

COLORS = ["red", "blue", "green", "yellow", "purple", "pink", "teal", "orange"]


def load(path):
    return Image.open(os.path.join(ART, path)).convert("RGBA")


def trim(im, margin=2):
    bbox = im.getchannel("A").point(lambda a: 255 if a > 8 else 0).getbbox()
    if not bbox:
        return im
    l, t, r, b = bbox
    l, t = max(0, l - margin), max(0, t - margin)
    r, b = min(im.width, r + margin), min(im.height, b + margin)
    return im.crop((l, t, r, b))


def fit(im, max_side):
    s = max_side / max(im.width, im.height)
    if s >= 1:
        return im
    return im.resize((max(1, round(im.width * s)), max(1, round(im.height * s))), Image.LANCZOS)


def scale(im, factor):
    if factor == 1:
        return im
    return im.resize((max(1, round(im.width * factor)), max(1, round(im.height * factor))), Image.LANCZOS)


def bleed(im, iterations=6):
    """Fill RGB of transparent pixels with neighbour colours so linear
    filtering / mipmaps don't produce dark fringes."""
    a = np.asarray(im, dtype=np.float32)
    rgb, alpha = a[..., :3], a[..., 3:4] / 255.0
    known = (alpha > 0.02).astype(np.float32)
    acc = rgb * known
    for _ in range(iterations):
        pa = np.pad(acc, ((1, 1), (1, 1), (0, 0)), mode="edge")
        pk = np.pad(known, ((1, 1), (1, 1), (0, 0)), mode="edge")
        s = sum(pa[1 + dy:pa.shape[0] - 1 + dy, 1 + dx:pa.shape[1] - 1 + dx]
                for dy in (-1, 0, 1) for dx in (-1, 0, 1))
        k = sum(pk[1 + dy:pk.shape[0] - 1 + dy, 1 + dx:pk.shape[1] - 1 + dx]
                for dy in (-1, 0, 1) for dx in (-1, 0, 1))
        newly = (known == 0) & (k > 0)
        fill = np.where(k > 0, s / np.maximum(k, 1e-6), 0)
        acc = np.where(newly, fill, acc)
        known = np.maximum(known, newly.astype(np.float32))
    out = np.where(alpha > 0.02, rgb, acc)
    res = np.concatenate([out, a[..., 3:4]], axis=-1)
    return Image.fromarray(np.clip(res, 0, 255).astype(np.uint8), "RGBA")


# ---------------------------------------------------------------- generated

def gen_white():
    return Image.new("RGBA", (12, 12), (255, 255, 255, 255))


def gen_glow(size=128):
    y, x = np.mgrid[0:size, 0:size]
    c = (size - 1) / 2
    d = np.sqrt((x - c) ** 2 + (y - c) ** 2) / c
    a = np.clip(1 - d, 0, 1) ** 1.8
    arr = np.zeros((size, size, 4), np.uint8)
    arr[..., :3] = 255
    arr[..., 3] = (a * 255).astype(np.uint8)
    return Image.fromarray(arr, "RGBA")


def gen_circle(size=96, outline=0):
    ss = 4
    im = Image.new("RGBA", (size * ss, size * ss), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    if outline:
        d.ellipse((0, 0, size * ss - 1, size * ss - 1), fill=(60, 30, 20, 255))
        o = outline * ss
        d.ellipse((o, o, size * ss - 1 - o, size * ss - 1 - o), fill=(255, 255, 255, 255))
    else:
        d.ellipse((0, 0, size * ss - 1, size * ss - 1), fill=(255, 255, 255, 255))
    return im.resize((size, size), Image.LANCZOS)


def gen_arrow(size=96):
    """Chunky white arrow pointing right with dark outline + soft shadow."""
    ss = 4
    S = size * ss
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    pts = [(0.14, 0.40), (0.52, 0.40), (0.52, 0.20), (0.88, 0.50),
           (0.52, 0.80), (0.52, 0.60), (0.14, 0.60)]
    poly = [(x * S, y * S) for x, y in pts]
    shadow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).polygon([(x, y + 0.05 * S) for x, y in poly], fill=(0, 0, 0, 110))
    shadow = shadow.filter(ImageFilter.GaussianBlur(S * 0.02))
    im.alpha_composite(shadow)
    d = ImageDraw.Draw(im)
    d.line(poly + [poly[0]], fill=(40, 25, 20, 255), width=int(S * 0.07), joint="curve")
    d.polygon(poly, fill=(40, 25, 20, 255))
    inner = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(inner).polygon(poly, fill=(255, 255, 255, 255))
    im.alpha_composite(inner)
    return im.resize((size, size), Image.LANCZOS)


def rounded(size, radius, fill, outline=None, width=0, ss=4):
    w, h = size
    im = Image.new("RGBA", (w * ss, h * ss), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    if outline:
        d.rounded_rectangle((0, 0, w * ss - 1, h * ss - 1), radius * ss, fill=outline)
        o = width * ss
        d.rounded_rectangle((o, o, w * ss - 1 - o, h * ss - 1 - o), (radius - width) * ss, fill=fill)
    else:
        d.rounded_rectangle((0, 0, w * ss - 1, h * ss - 1), radius * ss, fill=fill)
    return im.resize((w, h), Image.LANCZOS)


def gen_panel():
    """Cream popup panel (9-patch, splits 40px)."""
    w = h = 120
    im = rounded((w, h), 36, (255, 247, 226, 255), outline=(70, 38, 20, 255), width=6)
    # top highlight band
    hl = rounded((w - 20, 30), 14, (255, 255, 255, 120))
    im.alpha_composite(hl, (10, 10))
    return im


def gen_board_panel():
    return rounded((120, 120), 34, (20, 60, 25, 120), outline=(255, 255, 255, 90), width=4)


def gen_pill():
    return rounded((120, 64), 31, (40, 25, 20, 170))


def vgrad(w, h, top, bottom):
    """Vertical colour gradient image."""
    t = np.linspace(0, 1, h)[:, None, None]
    a = np.array(top, np.float32)[None, None, :]
    b = np.array(bottom, np.float32)[None, None, :]
    arr = np.repeat(a * (1 - t) + b * t, w, axis=1)
    return Image.fromarray(arr.astype(np.uint8), "RGB").convert("RGBA")


def masked(im, mask):
    out = im.copy()
    out.putalpha(mask)
    return out


def rr_mask(w, h, r, ss=4):
    m = Image.new("L", (w * ss, h * ss), 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, w * ss - 1, h * ss - 1), r * ss, fill=255)
    return m.resize((w, h), Image.LANCZOS)


def lighten(c, k):
    return tuple(int(v + (255 - v) * k) for v in c)


def darken(c, k):
    return tuple(int(v * (1 - k)) for v in c)


def gen_button(color, dark, w=300, h=100):
    """Glossy pill button: dark base lip, gradient face, top shine, outline."""
    r = h // 2 - 4
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    im.alpha_composite(rounded((w, h), r, darken(dark, 0.2) + (255,)))
    face_h = h - 16
    face = masked(vgrad(w - 10, face_h, lighten(color, 0.25), color), rr_mask(w - 10, face_h, r - 5))
    im.alpha_composite(face, (5, 4))
    shine = rounded((w - 70, 20), 10, (255, 255, 255, 120))
    im.alpha_composite(shine, (35, 11))
    im.alpha_composite(rounded((w, h), r, (0, 0, 0, 0), outline=(45, 26, 16, 255), width=5))
    return im


def gen_round_button(color, dark, size=120):
    ss = 4
    S = size * ss
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.ellipse((0, 0, S - 1, S - 1), fill=(45, 26, 16, 255))
    o = 5 * ss
    d.ellipse((o, o, S - 1 - o, S - 1 - o), fill=darken(dark, 0.1) + (255,))
    face = masked(vgrad(S - 2 * o, S - 2 * o - 7 * ss, lighten(color, 0.3), color),
                  Image.new("L", (S - 2 * o, S - 2 * o - 7 * ss), 0))
    m = Image.new("L", face.size, 0)
    ImageDraw.Draw(m).ellipse((0, 0, face.width - 1, face.height - 1), fill=255)
    face.putalpha(m)
    im.alpha_composite(face, (o, o))
    sh = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(sh).ellipse((int(S * 0.24), int(S * 0.12), int(S * 0.76), int(S * 0.34)), fill=(255, 255, 255, 110))
    im.alpha_composite(sh)
    return im.resize((size, size), Image.LANCZOS)


def gen_icon(kind, size=96):
    """White icons with a dark outline: home, restart, next, close."""
    ss = 4
    S = size * ss
    shape = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(shape)
    u = S / 100.0
    if kind == "home":
        d.polygon([(50 * u, 14 * u), (88 * u, 48 * u), (78 * u, 48 * u), (78 * u, 86 * u), (22 * u, 86 * u),
                   (22 * u, 48 * u), (12 * u, 48 * u)], fill=255)
        d.rectangle((42 * u, 60 * u, 58 * u, 86 * u), fill=0)
    elif kind == "restart":
        d.arc((18 * u, 18 * u, 82 * u, 82 * u), 300, 230, fill=255, width=int(15 * u))
        d.polygon([(10 * u, 30 * u), (40 * u, 30 * u), (22 * u, 56 * u)], fill=255)
    elif kind == "next":
        d.polygon([(30 * u, 16 * u), (82 * u, 50 * u), (30 * u, 84 * u)], fill=255)
    elif kind == "close":
        d.line((24 * u, 24 * u, 76 * u, 76 * u), fill=255, width=int(16 * u))
        d.line((76 * u, 24 * u, 24 * u, 76 * u), fill=255, width=int(16 * u))
    outline = shape.filter(ImageFilter.MaxFilter(int(9 * u) | 1))
    im = Image.new("RGBA", (S, S), (45, 26, 16, 0))
    im.putalpha(outline)
    white = Image.new("RGBA", (S, S), (255, 255, 255, 0))
    white.putalpha(shape)
    im.alpha_composite(white)
    return im.resize((size, size), Image.LANCZOS)


def gen_ribbon(color, w=560, h=120):
    """Banner ribbon with folded tails for popup titles."""
    ss = 2
    W2, H2 = w * ss, h * ss
    im = Image.new("RGBA", (W2, H2), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    tail_w = int(W2 * 0.13)
    band_top, band_bot = int(H2 * 0.08), int(H2 * 0.78)
    tail_top, tail_bot = int(H2 * 0.3), H2 - 2
    dk = darken(color, 0.38)
    for side in (0, 1):
        x0 = 0 if side == 0 else W2 - tail_w - int(W2 * 0.04)
        x1 = x0 + tail_w + int(W2 * 0.04)
        notch = x0 + int(tail_w * 0.35) if side == 0 else x1 - int(tail_w * 0.35)
        pts = ([(x0, tail_top), (x1, tail_top), (x1, tail_bot), (x0, tail_bot), (notch, (tail_top + tail_bot) // 2)]
               if side == 0 else
               [(x0, tail_top), (x1, tail_top), (notch, (tail_top + tail_bot) // 2), (x1, tail_bot), (x0, tail_bot)])
        d.polygon(pts, fill=(45, 26, 16, 255))
        inner = [(px + (6 if px < W2 / 2 else -6) * ss * (1 if px in (x0, x1) else 0), py) for px, py in pts]
        d.polygon(inner, fill=dk + (255,))
    # folds
    for fx in (tail_w, W2 - tail_w):
        d.polygon([(fx - 2, band_bot - 4), (fx + (20 if fx < W2 / 2 else -20) * ss, band_bot - 4),
                   (fx + (20 if fx < W2 / 2 else -20) * ss, tail_bot - 10)], fill=darken(color, 0.6) + (255,))
    x0, x1 = int(W2 * 0.09), W2 - int(W2 * 0.09)
    d.rounded_rectangle((x0, band_top, x1, band_bot), 14 * ss, fill=(45, 26, 16, 255))
    face = masked(vgrad(x1 - x0 - 10 * ss, band_bot - band_top - 10 * ss, lighten(color, 0.3), color),
                  rr_mask(x1 - x0 - 10 * ss, band_bot - band_top - 10 * ss, 10 * ss, 1))
    im.alpha_composite(face, (x0 + 5 * ss, band_top + 5 * ss))
    shine = rounded((x1 - x0 - 50 * ss, 12 * ss), 6 * ss, (255, 255, 255, 90), ss=1)
    im.alpha_composite(shine, (x0 + 25 * ss, band_top + 10 * ss))
    return im.resize((w, h), Image.LANCZOS)


def gen_frame_panel():
    """Popup panel 9-patch: brown outline, warm frame, cream inset (splits 46)."""
    w = h = 150
    im = rounded((w, h), 40, (255, 196, 92, 255), outline=(60, 33, 18, 255), width=6)
    inner = rounded((w - 36, h - 36), 26, (255, 249, 233, 255), outline=(196, 128, 52, 255), width=4)
    im.alpha_composite(inner, (18, 18))
    hl = rounded((w - 30, 10), 5, (255, 255, 255, 110))
    im.alpha_composite(hl, (15, 9))
    return im


def gen_switch(on, w=120, h=60):
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    col = (88, 204, 40) if on else (175, 170, 165)
    im.alpha_composite(rounded((w, h), h // 2, col + (255,), outline=(45, 26, 16, 255), width=4))
    im.alpha_composite(rounded((w - 20, 10), 5, (255, 255, 255, 80)), (10, 8))
    knob = gen_circle(h - 12, outline=4)
    im.alpha_composite(knob, (w - h + 6 if on else 6, 6))
    return im


def gen_ring(size=160, width=10):
    ss = 4
    S = size * ss
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.ellipse((0, 0, S - 1, S - 1), outline=(255, 255, 255, 255), width=width * ss)
    return im.resize((size, size), Image.LANCZOS).filter(ImageFilter.GaussianBlur(1))


def gen_ray(w=64, h=512):
    """Single soft light ray (for rotating sunbursts)."""
    y, x = np.mgrid[0:h, 0:w]
    cx = (w - 1) / 2
    spread = (y / h) * (w / 2) + 1
    a = np.clip(1 - np.abs(x - cx) / spread, 0, 1) * (1 - y / h) ** 0.6
    arr = np.zeros((h, w, 4), np.uint8)
    arr[..., :3] = 255
    arr[..., 3] = (a * 230).astype(np.uint8)
    return Image.fromarray(arr[::-1].copy(), "RGBA")


def gen_check(size=80, on=True):
    im = gen_circle(size, outline=5)
    return im




# ------------------------------------------------------------------ packing

class Packer:
    """MaxRects bin packer (best short side fit), multiple pages if needed."""

    def __init__(self):
        self.pages = []

    def pack(self, items):
        items = sorted(items, key=lambda it: -(max(it[1].width, it[1].height) * 10000 + min(it[1].width, it[1].height)))
        placements = []
        for name, im in items:
            w, h = im.width + PAD * 2, im.height + PAD * 2
            pos = None
            for p in self.pages:
                pos = self._place(p, w, h)
                if pos:
                    placements.append((name, im, p, pos))
                    break
            if not pos:
                p = {"free": [(0, 0, PAGE, PAGE)], "index": len(self.pages)}
                self.pages.append(p)
                pos = self._place(p, w, h)
                assert pos, f"{name} too big"
                placements.append((name, im, p, pos))
        return placements

    @staticmethod
    def _place(p, w, h):
        best = None
        for fx, fy, fw, fh in p["free"]:
            if w <= fw and h <= fh:
                score = (min(fw - w, fh - h), max(fw - w, fh - h))
                if best is None or score < best[0]:
                    best = (score, fx, fy)
        if best is None:
            return None
        _, x, y = best
        new_free = []
        for fx, fy, fw, fh in p["free"]:
            if x >= fx + fw or x + w <= fx or y >= fy + fh or y + h <= fy:
                new_free.append((fx, fy, fw, fh))
                continue
            if x > fx:
                new_free.append((fx, fy, x - fx, fh))
            if x + w < fx + fw:
                new_free.append((x + w, fy, fx + fw - x - w, fh))
            if y > fy:
                new_free.append((fx, fy, fw, y - fy))
            if y + h < fy + fh:
                new_free.append((fx, y + h, fw, fy + fh - y - h))
        # drop rectangles contained in others
        pruned = []
        for a in new_free:
            if not any(a != b and a[0] >= b[0] and a[1] >= b[1] and a[0] + a[2] <= b[0] + b[2]
                       and a[1] + a[3] <= b[1] + b[3] for b in new_free):
                pruned.append(a)
        p["free"] = list(dict.fromkeys(pruned))
        return (x, y)


def write_atlas(placements, pages, name="sprites"):
    page_imgs = [Image.new("RGBA", (PAGE, PAGE), (0, 0, 0, 0)) for _ in pages]
    lines = []
    by_page = {}
    for nm, im, p, (x, y) in placements:
        page_imgs[p["index"]].paste(im, (x + PAD, y + PAD))
        by_page.setdefault(p["index"], []).append((nm, im, x + PAD, y + PAD))
    for i, img in enumerate(page_imgs):
        used_h = max(y + im.height for _, im, _, y in by_page[i]) + PAD
        hgt = 1 << math.ceil(math.log2(max(used_h, 64)))
        img = img.crop((0, 0, PAGE, hgt))
        fn = f"{name}{i}.png"
        img.save(os.path.join(OUT, fn), optimize=True)
        lines += ["", fn, f"size: {PAGE},{hgt}", "format: RGBA8888",
                  "filter: MipMapLinearLinear,Linear", "repeat: none"]
        for nm, im, x, y in sorted(by_page[i]):
            lines += [nm, "  rotate: false", f"  xy: {x}, {y}", f"  size: {im.width}, {im.height}",
                      f"  orig: {im.width}, {im.height}", "  offset: 0, 0", "  index: -1"]
    with open(os.path.join(OUT, f"{name}.atlas"), "w") as f:
        f.write("\n".join(lines) + "\n")
    print(f"atlas: {len(placements)} regions on {len(page_imgs)} page(s)")


# ------------------------------------------------------------------- fonts

def make_font(name, size, stroke, shadow):
    font = ImageFont.truetype(os.path.join(ART, "fonts", "LilitaOne-Regular.ttf"), size)
    ascent, descent = font.getmetrics()
    chars = [chr(c) for c in range(32, 127)] + ["×"]
    glyphs = []
    pad = stroke + shadow + 4
    for ch in chars:
        cw = int(font.getlength(ch)) + pad * 2 + 8
        chh = ascent + descent + pad * 2
        im = Image.new("RGBA", (cw, chh), (0, 0, 0, 0))
        d = ImageDraw.Draw(im)
        ox, oy = pad, pad + ascent
        if ch != " " and shadow > 0:
            d.text((ox, oy + shadow), ch, font=font, anchor="ls", fill=(40, 20, 10, 150),
                   stroke_width=stroke, stroke_fill=(40, 20, 10, 150))
        if ch != " ":
            d.text((ox, oy), ch, font=font, anchor="ls", fill=(255, 255, 255, 255),
                   stroke_width=stroke, stroke_fill=(55, 30, 18, 255))
        bbox = im.getchannel("A").getbbox()
        adv = int(round(font.getlength(ch))) + max(1, stroke // 2)
        if not bbox:
            glyphs.append((ch, None, 0, 0, adv))
            continue
        crop = im.crop(bbox)
        glyphs.append((ch, crop, bbox[0] - pad, bbox[1] - pad, adv))
    # pack glyphs in rows
    W = 1024
    x = y = 0
    row_h = 0
    pos = {}
    for ch, im, *_ in glyphs:
        if im is None:
            continue
        if x + im.width + 2 > W:
            x, y = 0, y + row_h + 2
            row_h = 0
        pos[ch] = (x, y)
        x += im.width + 2
        row_h = max(row_h, im.height)
    H = 1 << math.ceil(math.log2(y + row_h + 2))
    sheet = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    for ch, im, *_ in glyphs:
        if im is not None:
            sheet.paste(im, pos[ch])
    sheet = bleed(sheet, 3)
    sheet.save(os.path.join(OUT, "fonts", f"{name}.png"), optimize=True)
    line_h = ascent + descent
    out = [f'info face="LilitaOne" size={size} bold=0 italic=0 charset="" unicode=1 stretchH=100 smooth=1 aa=1 padding=0,0,0,0 spacing=1,1',
           f"common lineHeight={line_h} base={ascent} scaleW={W} scaleH={H} pages=1 packed=0",
           f'page id=0 file="{name}.png"',
           f"chars count={len(glyphs)}"]
    for ch, im, xo, yo, adv in glyphs:
        if im is None:
            out.append(f"char id={ord(ch)} x=0 y=0 width=0 height=0 xoffset=0 yoffset=0 xadvance={adv} page=0 chnl=15")
        else:
            px, py = pos[ch]
            out.append(f"char id={ord(ch)} x={px} y={py} width={im.width} height={im.height} "
                       f"xoffset={xo} yoffset={yo} xadvance={adv} page=0 chnl=15")
    with open(os.path.join(OUT, "fonts", f"{name}.fnt"), "w") as f:
        f.write("\n".join(out) + "\n")
    print(f"font {name}: {W}x{H}")


# -------------------------------------------------------------------- v2 helpers

V2 = "v2"


def v2(folder, name):
    return load(f"{V2}/{folder}/{name}.png")


def fix_head(im):
    """The generated heads come with a square skin block under the chin (a slicing
    artefact). Remove it; the rig draws a proper neck behind the head instead."""
    a = np.asarray(im.convert("RGBA")).copy()
    h, w = a.shape[:2]
    sy, sx = h / 286.0, w / 216.0
    y0 = int(206 * sy)
    x0, x1 = int(52 * sx), int(164 * sx)
    a[y0:, x0:x1, 3] = 0
    # soften the new bottom edge of the chin
    for k in range(4):
        y = y0 - 1 - k
        a[y, x0:x1, 3] = (a[y, x0:x1, 3].astype(np.float32) * (0.35 + 0.16 * k)).astype(np.uint8)
    return Image.fromarray(a, "RGBA")


def gen_neck(w=76, h=90):
    """Skin-coloured neck with a soft shadow under the chin and an outline."""
    ss = 4
    im = Image.new("RGBA", (w * ss, h * ss), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rounded_rectangle((0, 0, w * ss - 1, h * ss - 1), 14 * ss, fill=(110, 52, 30, 255))
    o = 3 * ss
    face = masked(vgrad(w * ss - 2 * o, h * ss - 2 * o, (226, 150, 112), (250, 196, 160)),
                  rr_mask(w * ss - 2 * o, h * ss - 2 * o, 11 * ss, 1))
    im.alpha_composite(face, (o, o))
    return im.resize((w, h), Image.LANCZOS)


def v3(folder, name):
    return load(f"v3/{folder}/{name}.png")


def largest_component(im, thresh=40):
    """Keeps only the biggest connected opaque blob (drops stray slicing fragments)."""
    a = np.asarray(im.convert("RGBA")).copy()
    solid = a[..., 3] > thresh
    h, w = solid.shape
    labels = np.zeros((h, w), np.int32)
    best, best_n, cur = 0, 0, 0
    for y0 in range(h):
        for x0 in np.where(solid[y0] & (labels[y0] == 0))[0]:
            cur += 1
            stack = [(y0, x0)]
            labels[y0, x0] = cur
            n = 0
            while stack:
                y, x = stack.pop()
                n += 1
                for yy, xx in ((y + 1, x), (y - 1, x), (y, x + 1), (y, x - 1)):
                    if 0 <= yy < h and 0 <= xx < w and solid[yy, xx] and labels[yy, xx] == 0:
                        labels[yy, xx] = cur
                        stack.append((yy, xx))
            if n > best_n:
                best, best_n = cur, n
    keep = labels == best
    # keep the soft edge pixels next to the blob too
    grown = np.asarray(Image.fromarray((keep * 255).astype(np.uint8)).filter(ImageFilter.MaxFilter(5))) > 0
    a[..., 3] = np.where(grown, a[..., 3], 0)
    return Image.fromarray(a, "RGBA")


def clean_neck(im):
    """The generated neck connector sits on a dark brown backdrop; cut it out by colour."""
    a = np.asarray(im.convert("RGBA")).copy()
    hsv = np.asarray(im.convert("RGB").convert("HSV")).astype(np.float32) / 255.0
    sat, val = hsv[..., 1], hsv[..., 2]
    body = (val > 0.42) & ((sat > 0.35) | (val > 0.75)) & (a[..., 3] > 40)
    m = Image.fromarray((body * 255).astype(np.uint8)).filter(ImageFilter.MedianFilter(7))
    for _ in range(3):
        m = m.filter(ImageFilter.MaxFilter(5))     # close gaps between scales
    for _ in range(2):
        m = m.filter(ImageFilter.MinFilter(5))
    m = m.filter(ImageFilter.MaxFilter(5))         # include the dark outline ring
    alpha = np.asarray(m.filter(ImageFilter.GaussianBlur(1.2))).astype(np.float32)
    a[..., 3] = np.minimum(a[..., 3], alpha).astype(np.uint8)
    return largest_component(Image.fromarray(a, "RGBA"))


def pack_atlas(name, items):
    packer = Packer()
    placements = packer.pack(items)
    write_atlas(placements, packer.pages, name)
    assert len(packer.pages) == 1, f"atlas {name} needs {len(packer.pages)} pages"


def save_jpg(im, path, size, quality=86):
    im.convert("RGB").resize(size, Image.LANCZOS).save(os.path.join(OUT, path), quality=quality, optimize=True)


# -------------------------------------------------------------------- main

def main():
    for d in ("fonts", "icons", "bg", "map", "story", "data"):
        os.makedirs(os.path.join(OUT, d), exist_ok=True)

    def adder(items):
        def add(name, im, max_side=None, factor=None, keep=False):
            if not keep:
                im = trim(im)
            if max_side:
                im = fit(im, max_side)
            if factor:
                im = scale(im, factor)
            items.append((name, bleed(im)))
        return add

    # ================= gameplay atlas =================
    items = []
    add = adder(items)
    for c in COLORS:
        add(f"block_{c}", load(f"main/block_{c}.png"), 128)
        add(f"cannon_{c}", load(f"main/cannon_{c}.png"), 156)
        add(f"segment_{c}", load(f"main/segment_{c}.png"), 128)
    add("banner_level", load("main/banner_level.png"), 400)
    add("btn_settings", load("main/btn_settings.png"), 130)
    add("btn_play", load("main/btn_play.png"), 380)
    add("coin", load("main/coin.png"), 100)
    add("explosion", load("main/explosion.png"), 256)
    add("fireball", load("main/fireball.png"), 140)
    add("skull_fail", load("main/skull_fail.png"), 300)
    add("slot_empty", load("main/slot_empty.png"), 156)
    add("slot_locked", load("main/slot_locked.png"), 156)
    add("star", load("main/star.png"), 170)

    anim = os.path.join(ART, "anim")
    keep_anim = {
        "dragon": None, "tail": ["tail_piece_1", "tail_piece_2", "tail_piece_3", "segment_shadow", "segment_shine_overlay"],
        "effects": None, "ui": ["hint_lightbulb", "star_sparkle"], "girl_faces": ["speech_bubble_help"],
    }
    factors = {"dragon": 0.85, "tail": 0.6, "effects": 0.7, "ui": 0.45, "girl_faces": 0.62}
    for folder, names in keep_anim.items():
        for fn in sorted(os.listdir(os.path.join(anim, folder))):
            if not fn.endswith(".png") or (names is not None and fn[:-4] not in names):
                continue
            ff = 1.0 if fn.startswith("sunburst") else factors[folder]
            add(fn[:-4], load(f"anim/{folder}/{fn}"), factor=ff)

    # girl v2 rig (all parts share one scale), heads/faces cleaned up
    G = 0.5
    add("g_head", fix_head(v2("girl_rig_main", "girl_head")), factor=G, keep=True)
    for fn in sorted(os.listdir(os.path.join(ART, V2, "girl_faces"))):
        add("g_" + fn[:-4], fix_head(v2("girl_faces", fn[:-4])), factor=G, keep=True)
    add("g_ponytail", v2("girl_rig_main", "girl_ponytail"), factor=G)
    # v3 limbs: slim one-piece arms and legs with small hands (same 1000px scale)
    for n in ("girl_arm_left", "girl_arm_right", "girl_leg_left", "girl_leg_right"):
        add("g_" + n[5:], largest_component(v3("girl_rig_v3", n)), factor=G)
    add("g_neck", largest_component(v3("girl_body_v3", "girl_neck")), factor=G)
    # dragon neck connector (red; recoloured per skin by the hue shader like the head)
    add("dragon_neck_connector", clean_neck(v3("dragon_neck", "dragon_neck_connector")), 200)
    add("video_ad_badge_small", v3("video_ads", "video_ad_badge_small"), 56)

    # boosters, boss, special balls, world particles, lives
    for n in ("booster_bomb", "booster_freeze", "booster_rainbow", "booster_shuffle"):
        add(n, v2("boosters", n), 110)
    add("booster_button_frame", v2("boosters", "booster_button_frame"), 140)
    add("booster_count_badge", v2("boosters", "booster_count_badge"), 50)
    add("bomb_explosion_big", v2("boosters", "bomb_explosion_big"), 300)
    add("freeze_ice_overlay", v2("boosters", "freeze_ice_overlay"), 128)
    add("shuffle_swirl_effect", v2("boosters", "shuffle_swirl_effect"), 220)
    add("block_rainbow", v2("special_blocks", "block_rainbow"), 128)
    for c in ("red", "blue", "green", "yellow"):
        add(f"segment_armored_{c}", v2("boss_and_special_balls", f"segment_armored_{c}"), 128)
    add("armor_crack_overlay", v2("boss_and_special_balls", "armor_crack_overlay"), 120)
    add("armor_break_pieces", v2("boss_and_special_balls", "armor_break_pieces"), 140)
    add("boss_health_bar_frame", v2("boss_and_special_balls", "boss_health_bar_frame"), 420)
    add("boss_skull_badge", v2("boss_and_special_balls", "boss_skull_badge"), 96)
    add("boss_crown", v2("dragon_skins", "boss_crown"), 180)
    for n in ("ember_particle", "firefly_particle", "leaf_particle", "sand_particle", "snowflake_particle", "rain_drop_particle"):
        add(n, v2("world_icons", n), 56)

    gen = {
        "white": gen_white(), "glow": gen_glow(), "circle": gen_circle(96),
        "badge": gen_circle(64, outline=6), "arrow": gen_arrow(96), "panel": gen_panel(),
        "board_panel": gen_board_panel(), "pill": gen_pill(), "ray": gen_ray(),
        "btn_green": gen_button((88, 204, 40), (40, 120, 20)),
        "btn_orange": gen_button((255, 160, 30), (190, 90, 10)),
        "btn_blue": gen_button((40, 150, 255), (20, 80, 180)),
        "btn_grey": gen_button((170, 170, 180), (100, 100, 110)),
        "btn_purple": gen_button((170, 90, 240), (100, 40, 170)),
        "rbtn_green": gen_round_button((88, 204, 40), (40, 120, 20)),
        "rbtn_orange": gen_round_button((255, 160, 30), (190, 90, 10)),
        "rbtn_blue": gen_round_button((40, 150, 255), (20, 80, 180)),
        "rbtn_red": gen_round_button((240, 70, 70), (150, 30, 30)),
        "icon_home": gen_icon("home"), "icon_restart": gen_icon("restart"),
        "icon_next": gen_icon("next"), "icon_close": gen_icon("close"),
        "ribbon_green": gen_ribbon((80, 196, 40)), "ribbon_red": gen_ribbon((232, 64, 52)),
        "ribbon_blue": gen_ribbon((40, 140, 245)), "ribbon_orange": gen_ribbon((255, 150, 30)),
        "ribbon_purple": gen_ribbon((160, 80, 230)),
        "frame_panel": gen_frame_panel(), "switch_on": gen_switch(True), "switch_off": gen_switch(False),
    }
    for name, im in gen.items():
        items.append((name, bleed(im) if name != "white" else im))
    pack_atlas("sprites", items)

    # ================= outfits atlas (girl bodies + hats) =================
    items = []
    add = adder(items)
    add("body_default", v2("girl_rig_main", "girl_body"), factor=G)
    for fn in sorted(os.listdir(os.path.join(ART, V2, "girl_outfits"))):
        add(fn[:-4], v2("girl_outfits", fn[:-4]), factor=G)
    pack_atlas("outfits", items)

    # ================= menu / meta UI atlas =================
    items = []
    add = adder(items)
    ui = {
        "map_and_ui": 120, "daily_reward": 150, "lives_and_currency": 110, "shop_ui": 150,
        "achievements": 130, "world_icons": 150, "spin_wheel": 150,
    }
    big = {"spin_wheel_8_slices": 560, "spin_wheel_stand": 300, "map_world_sign_board": 300,
           "calendar_day7_big_card": 230, "chest_gold_open": 210, "chest_gold_closed": 210,
           "world_banner_frame": 420, "tab_button_active": 240, "tab_button_inactive": 240,
           "shop_item_card": 200, "shop_item_card_selected": 200, "price_tag_coin": 170,
           "map_node_current_marker": 90, "map_star_small": 40, "girl_map_avatar": 110,
           "glow_burst": 260, "confetti_burst": 200, "star_burst": 200}
    skip = {"ember_particle", "firefly_particle", "leaf_particle", "sand_particle", "snowflake_particle", "rain_drop_particle"}
    for folder, size in ui.items():
        for fn in sorted(os.listdir(os.path.join(ART, V2, folder))):
            n = fn[:-4]
            if n in skip:
                continue
            add("ui_" + n, v2(folder, n), big.get(n, size))
    for fn in sorted(os.listdir(os.path.join(ART, V2, "boosters"))):
        if fn.startswith("booster_") and fn[:-4] not in ("booster_button_frame", "booster_count_badge"):
            add("ui_" + fn[:-4], v2("boosters", fn[:-4]), 110)
    for fn in sorted(os.listdir(os.path.join(ART, V2, "dragon_skins"))):
        if fn.startswith("dragon_head"):
            add("ui_" + fn[:-4], v2("dragon_skins", fn[:-4]), 170)
    for fn in sorted(os.listdir(os.path.join(ART, V2, "girl_outfits"))):
        add("ui_" + fn[:-4], v2("girl_outfits", fn[:-4]), 150)
    add("ui_skull_fail", load("main/skull_fail.png"), 120)
    for fn in sorted(os.listdir(os.path.join(ART, "v3", "buttons_ui"))):
        n = fn[:-4]
        add("ui_" + n, v3("buttons_ui", n), 330 if n == "btn_play_big" else (300 if n == "coin_counter_frame" else 120))
    for fn in sorted(os.listdir(os.path.join(ART, "v3", "coin_packs"))):
        n = fn[:-4]
        add("ui_" + n, v3("coin_packs", n), 210 if n in ("coin_pack_card", "price_button_green") else 170)
    for fn in sorted(os.listdir(os.path.join(ART, "v3", "video_ads"))):
        n = fn[:-4]
        add("ui_" + n, v3("video_ads", n), 300 if n == "video_ad_button_green" else 130)
    add("ui_body_default", v2("girl_rig_main", "girl_body"), 150)
    pack_atlas("ui", items)

    # ================= backgrounds, map, story =================
    for w in ("forest", "desert", "snow", "volcano", "night"):
        save_jpg(v2("backgrounds", f"bg_{w}"), f"bg/bg_{w}.jpg", (1080, 1620), 84)
    m = v2("map", "level_map").convert("RGB")
    for i in range(3):  # three 1024x1024 tiles, top to bottom
        m.crop((0, i * 1024, 1024, (i + 1) * 1024)).save(os.path.join(OUT, "map", f"map_{i}.jpg"), quality=84, optimize=True)
    for i, n in enumerate(("story_1_kidnap", "story_2_desert", "story_3_snow", "story_4_volcano", "story_5_night")):
        save_jpg(v2("story", n), f"story/story_{i + 1}.jpg", (1024, 768), 85)
    import shutil
    shutil.copy(os.path.join(ART, V2, "data", "paths.json"), os.path.join(OUT, "data", "paths.json"))

    # ================= launcher icons =================
    icon = v2("promo", "app_icon_v2").convert("RGBA")
    icon.resize((64, 64), Image.LANCZOS).save(os.path.join(OUT, "icons", "icon_64.png"))
    densities = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for dens, px in densities.items():
        d = os.path.join(ANDROID_RES, f"mipmap-{dens}")
        os.makedirs(d, exist_ok=True)
        sq = icon.resize((px * 4, px * 4), Image.LANCZOS)
        mask = Image.new("L", sq.size, 0)
        ImageDraw.Draw(mask).rounded_rectangle((0, 0, sq.width - 1, sq.height - 1), int(sq.width * 0.18), fill=255)
        sq.putalpha(mask)
        sq.resize((px, px), Image.LANCZOS).save(os.path.join(d, "ic_launcher.png"))
        mask = Image.new("L", (px * 4, px * 4), 0)
        ImageDraw.Draw(mask).ellipse((0, 0, px * 4 - 1, px * 4 - 1), fill=255)
        rd = icon.resize((px * 4, px * 4), Image.LANCZOS)
        rd.putalpha(mask)
        rd.resize((px, px), Image.LANCZOS).save(os.path.join(d, "ic_launcher_round.png"))
        full = int(px * 108 / 48)
        icon.resize((full, full), Image.LANCZOS).save(os.path.join(d, "ic_launcher_foreground.png"))

    make_font("font_big", 84, 6, 5)
    make_font("font_small", 46, 4, 3)
    make_font("font_plain", 52, 0, 0)
    print("done")


if __name__ == "__main__":
    sys.exit(main())
