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


def gen_button(color, dark):
    w, h = 240, 96
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    base = rounded((w, h), 40, dark + (255,))
    top = rounded((w - 12, h - 20), 34, color + (255,))
    im.alpha_composite(base)
    im.alpha_composite(top, (6, 5))
    shine = rounded((w - 60, 22), 11, (255, 255, 255, 110))
    im.alpha_composite(shine, (30, 12))
    outline = rounded((w, h), 40, (0, 0, 0, 0), outline=(40, 25, 20, 255), width=5)
    im.alpha_composite(outline)
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
    """Simple shelf packer producing multiple pages."""

    def __init__(self):
        self.pages = []

    def pack(self, items):
        items = sorted(items, key=lambda it: (-it[1].height, -it[1].width))
        page = None
        placements = []
        for name, im in items:
            w, h = im.width + PAD * 2, im.height + PAD * 2
            placed = False
            for p in self.pages:
                pos = self._place(p, w, h)
                if pos:
                    placed = True
                    placements.append((name, im, p, pos))
                    break
            if not placed:
                p = {"shelves": [], "y": 0, "index": len(self.pages)}
                self.pages.append(p)
                pos = self._place(p, w, h)
                assert pos, f"{name} too big"
                placements.append((name, im, p, pos))
        return placements

    @staticmethod
    def _place(p, w, h):
        for sh in p["shelves"]:
            if h <= sh["h"] and sh["x"] + w <= PAGE:
                pos = (sh["x"], sh["y"])
                sh["x"] += w
                return pos
        if p["y"] + h <= PAGE:
            sh = {"x": w, "y": p["y"], "h": h}
            p["shelves"].append(sh)
            p["y"] += h
            return (0, sh["y"])
        return None


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
        if ch != " ":
            d.text((ox, oy + shadow), ch, font=font, anchor="ls", fill=(40, 20, 10, 150),
                   stroke_width=stroke, stroke_fill=(40, 20, 10, 150))
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


# -------------------------------------------------------------------- main

def main():
    os.makedirs(os.path.join(OUT, "fonts"), exist_ok=True)
    os.makedirs(os.path.join(OUT, "icons"), exist_ok=True)
    items = []

    def add(name, im, max_side=None, factor=None):
        im = trim(im)
        if max_side:
            im = fit(im, max_side)
        if factor:
            im = scale(im, factor)
        items.append((name, bleed(im)))

    for c in COLORS:
        add(f"block_{c}", load(f"main/block_{c}.png"), 136)
        add(f"cannon_{c}", load(f"main/cannon_{c}.png"), 168)
        add(f"segment_{c}", load(f"main/segment_{c}.png"), 140)
    add("banner_level", load("main/banner_level.png"), 460)
    add("btn_play", load("main/btn_play.png"), 420)
    add("btn_settings", load("main/btn_settings.png"), 140)
    add("coin", load("main/coin.png"), 110)
    add("explosion", load("main/explosion.png"), 300)
    add("fireball", load("main/fireball.png"), 150)
    add("skull_fail", load("main/skull_fail.png"), 380)
    add("slot_empty", load("main/slot_empty.png"), 168)
    add("slot_locked", load("main/slot_locked.png"), 168)
    add("snake_head_attack", load("main/snake_head_attack.png"), 420)
    add("star", load("main/star.png"), 190)

    anim = os.path.join(ART, "anim")
    factors = {"girl": 0.62, "girl_faces": 0.62, "dragon": 0.85, "tail": 0.6,
               "effects": 0.8, "ui": 0.5}
    for folder, f in factors.items():
        for fn in sorted(os.listdir(os.path.join(anim, folder))):
            if fn.endswith(".png"):
                ff = f
                if folder == "ui" and fn.startswith(("heart", "hint", "undo", "star_sparkle")):
                    ff = 0.45
                if folder == "effects" and fn.startswith("sunburst"):
                    ff = 1.0
                add(fn[:-4], load(f"anim/{folder}/{fn}"), factor=ff)

    gen = {
        "white": gen_white(), "glow": gen_glow(), "circle": gen_circle(96),
        "badge": gen_circle(64, outline=6), "arrow": gen_arrow(96), "panel": gen_panel(),
        "board_panel": gen_board_panel(), "pill": gen_pill(), "ring": gen_ring(),
        "ray": gen_ray(),
        "btn_green": gen_button((88, 204, 40), (40, 120, 20)),
        "btn_orange": gen_button((255, 160, 30), (190, 90, 10)),
        "btn_blue": gen_button((40, 150, 255), (20, 80, 180)),
        "btn_red": gen_button((240, 70, 70), (150, 30, 30)),
        "btn_grey": gen_button((170, 170, 180), (100, 100, 110)),
    }
    for name, im in gen.items():
        items.append((name, bleed(im) if name not in ("white",) else im))

    packer = Packer()
    placements = packer.pack(items)
    write_atlas(placements, packer.pages)

    # background (opaque jpg)
    bg = Image.open(os.path.join(ART, "main", "bg_level.png")).convert("RGB")
    bg = bg.resize((1080, 1620), Image.LANCZOS)
    bg.save(os.path.join(OUT, "bg_level.jpg"), quality=88, optimize=True)

    # icons
    icon = Image.open(os.path.join(ART, "main", "app_icon.png")).convert("RGBA")
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
    # adaptive icon foreground (full-bleed art, 108dp with 18dp inset safe zone)
    for dens, px in densities.items():
        full = int(px * 108 / 48)
        d = os.path.join(ANDROID_RES, f"mipmap-{dens}")
        icon.resize((full, full), Image.LANCZOS).save(os.path.join(d, "ic_launcher_foreground.png"))

    make_font("font_big", 84, 6, 5)
    make_font("font_small", 46, 4, 3)
    print("done")


if __name__ == "__main__":
    sys.exit(main())
