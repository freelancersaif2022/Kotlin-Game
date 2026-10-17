#!/usr/bin/env python3
"""Bramble Dash asset pipeline.

Extracts the supplied assets.zip safely, then repacks the source atlases into small
per-group runtime atlases so the game only loads what the current run needs
(the source pack keeps e.g. all 17 environment pages in a single .atlas, ~272 MiB of GPU memory).

* Region names are preserved exactly (e.g. "characters/max/running/03").
* Transparent borders are trimmed; the trim is stored as the standard LibGDX atlas
  "orig"/"offset" fields (bottom-left offset), so pivots/collisions stay in the
  original canvas coordinate system documented by metadata/sprites.json.
* Pixel-identical regions are stored once and aliased.
* Every packed rect gets 2 px edge extrusion to avoid linear-filter bleeding.
* The raw 9-patch (ui/components/panel.9.png) is converted into a proper atlas
  region with "split"/"pad" values measured from its marker border.

Outputs (committed, consumed by the game):
  assets/atlas/<group>.atlas + pages
  assets/data/sprites.json   compact pivots / collisions / ordered animations
  assets/fonts/*.fnt + png   bitmap fonts rasterised from DejaVu Sans Bold
  docs/asset_inventory.json  inventory used by the asset-usage report

Usage: python3 -I tools/asset_pipeline.py [assets.zip] [repo_root]
"""
import hashlib, json, os, shutil, sys, tempfile, zipfile
from PIL import Image, ImageDraw, ImageFont, ImageFilter

PAD = 2          # extrusion on each side
GAP = 2          # extra transparent gap between rects
MAX_PAGE = 2048


def safe_extract(zip_path, dest):
    dest = os.path.realpath(dest)
    with zipfile.ZipFile(zip_path) as z:
        for info in z.infolist():
            name = info.filename
            target = os.path.realpath(os.path.join(dest, name))
            if os.path.isabs(name) or not (target == dest or target.startswith(dest + os.sep)):
                raise SystemExit("Refusing archive entry outside destination: " + name)
            if (info.external_attr >> 16) & 0o170000 == 0o120000:
                raise SystemExit("Refusing symlink entry: " + name)
        z.extractall(dest)


def find_pack_root(base):
    for root, dirs, files in os.walk(base):
        if "manifest.json" in files and os.path.basename(root) == "metadata":
            return os.path.dirname(root)
    raise SystemExit("metadata/manifest.json not found inside archive")


def group_of(name):
    p = name.split("/")
    if p[0] in ("characters", "enemies", "bosses"):
        return {"characters": "char_", "enemies": "enemy_", "bosses": "boss_"}[p[0]] + p[1]
    if p[0] == "environments":
        return "world_" + p[1]
    if p[0] in ("collectibles", "powerups"):
        return "items"
    if p[0] in ("obstacles", "effects"):
        return p[0]
    if p[0] == "ui":
        return "ui_panels" if p[1] == "screens" else "ui"
    raise ValueError(name)


class Skyline:
    """Skyline bottom-left rectangle packer for a single page."""

    def __init__(self, w, h):
        self.w, self.h = w, h
        self.sky = [(0, 0, w)]  # (x, y, width) segments; y measured from top
        self.used_h = 0

    def fit(self, rw, rh):
        best = None
        for i in range(len(self.sky)):
            x = self.sky[i][0]
            if x + rw > self.w:
                break
            y, width_left, j = 0, rw, i
            while width_left > 0:
                y = max(y, self.sky[j][1])
                width_left -= self.sky[j][2]
                j += 1
                if j >= len(self.sky) and width_left > 0:
                    y = None
                    break
            if y is None or y + rh > self.h:
                continue
            if best is None or y + rh < best[1] + rh or (y + rh == best[1] + rh and x < best[0]):
                best = (x, y)
        return best

    def place(self, x, y, rw, rh):
        new = (x, y + rh, rw)
        out = []
        for sx, sy, sw in self.sky:
            ex = sx + sw
            if ex <= x or sx >= x + rw:
                out.append((sx, sy, sw))
            else:
                if sx < x:
                    out.append((sx, sy, x - sx))
                if ex > x + rw:
                    out.append((x + rw, sy, ex - (x + rw)))
        out.append(new)
        out.sort()
        merged = []
        for seg in out:
            if merged and merged[-1][1] == seg[1] and merged[-1][0] + merged[-1][2] == seg[0]:
                merged[-1] = (merged[-1][0], seg[1], merged[-1][2] + seg[2])
            else:
                merged.append(seg)
        self.sky = merged
        self.used_h = max(self.used_h, y + rh)


def extrude(img, pad):
    w, h = img.size
    out = Image.new("RGBA", (w + 2 * pad, h + 2 * pad), (0, 0, 0, 0))
    out.paste(img, (pad, pad))
    for i in range(pad):
        out.paste(img.crop((0, 0, w, 1)), (pad, i))
        out.paste(img.crop((0, h - 1, w, h)), (pad, pad + h + i))
    left = out.crop((pad, 0, pad + 1, h + 2 * pad))
    right = out.crop((pad + w - 1, 0, pad + w, h + 2 * pad))
    for i in range(pad):
        out.paste(left, (i, 0))
        out.paste(right, (pad + w + i, 0))
    return out


def pack_group(group, entries, out_dir):
    """entries: list of dict(name, img, orig(w,h), offset(x,y bottom-left), split, pad)."""
    unique = {}
    for e in entries:
        key = hashlib.sha1(e["img"].tobytes() + bytes(str(e["img"].size), "ascii")).hexdigest()
        e["key"] = key
        unique.setdefault(key, e["img"])
    order = sorted(unique.items(), key=lambda kv: (-kv[1].size[1], -kv[1].size[0]))
    pages, placed = [], {}
    for key, img in order:
        rw, rh = img.size[0] + 2 * PAD + GAP, img.size[1] + 2 * PAD + GAP
        if rw > MAX_PAGE or rh > MAX_PAGE:
            raise SystemExit(f"region too large: {group} {img.size}")
        for pi, pk in enumerate(pages):
            spot = pk.fit(rw, rh)
            if spot:
                break
        else:
            pk = Skyline(MAX_PAGE, MAX_PAGE)
            pages.append(pk)
            pi = len(pages) - 1
            spot = pk.fit(rw, rh)
        pk.place(spot[0], spot[1], rw, rh)
        placed[key] = (pi, spot[0] + PAD, spot[1] + PAD)
    page_imgs = []
    for pk in pages:
        used_w = max(sx for sx, sy, sw in pk.sky if sy > 0) if False else MAX_PAGE
        h = (pk.used_h + 3) // 4 * 4
        w = MAX_PAGE
        # shrink width to used extent
        maxx = 0
        page_imgs.append([Image.new("RGBA", (w, h), (0, 0, 0, 0)), 0])
    for key, (pi, x, y) in placed.items():
        img = unique[key]
        page_imgs[pi][0].alpha_composite(extrude(img, PAD), (x - PAD, y - PAD))
        page_imgs[pi][1] = max(page_imgs[pi][1], x + img.size[0] + PAD + GAP)
    files = []
    lines = []
    for pi, (img, maxx) in enumerate(page_imgs):
        w = min(MAX_PAGE, (maxx + 3) // 4 * 4)
        img = img.crop((0, 0, w, img.size[1]))
        fname = f"{group}.png" if len(page_imgs) == 1 else f"{group}_{pi}.png"
        img.save(os.path.join(out_dir, fname), optimize=True)
        files.append((fname, img.size))
        lines.append(fname)
        lines.append(f"size: {img.size[0]}, {img.size[1]}")
        lines.append("format: RGBA8888")
        lines.append("filter: Linear, Linear")
        lines.append("repeat: none")
        for e in sorted(entries, key=lambda e: e["name"]):
            epi, x, y = placed[e["key"]]
            if epi != pi:
                continue
            w_, h_ = e["img"].size
            lines.append(e["name"])
            lines.append("  rotate: false")
            lines.append(f"  xy: {x}, {y}")
            lines.append(f"  size: {w_}, {h_}")
            if e.get("split"):
                lines.append("  split: %d, %d, %d, %d" % e["split"])
                lines.append("  pad: %d, %d, %d, %d" % e["pad"])
            lines.append(f"  orig: {e['orig'][0]}, {e['orig'][1]}")
            lines.append(f"  offset: {e['offset'][0]}, {e['offset'][1]}")
            lines.append("  index: -1")
        lines.append("")
    with open(os.path.join(out_dir, group + ".atlas"), "w") as f:
        f.write("\n" + "\n".join(lines))
    return files, len(unique)


def make_font(out_dir, name, size, outline, ttf):
    """Rasterise an AngelCode BMFont (text format) with a dark outline."""
    font = ImageFont.truetype(ttf, size)
    chars = [chr(c) for c in range(32, 127)] + ["×", "•", "★", "♥", "→", "←", "✓", "—", "–", "’", "…"]
    glyphs = []
    for ch in chars:
        try:
            bbox = font.getbbox(ch)
        except Exception:
            continue
        adv = font.getlength(ch)
        w = max(1, bbox[2] - bbox[0]) + outline * 2
        h = max(1, bbox[3] - bbox[1]) + outline * 2
        img = Image.new("L", (w + 2, h + 2), 0)
        ImageDraw.Draw(img).text((1 + outline - bbox[0], 1 + outline - bbox[1]), ch, font=font, fill=255)
        glyphs.append((ch, img, bbox, adv))
    page_w = 1024
    x = y = 0
    row_h = 0
    pos = []
    for ch, img, bbox, adv in glyphs:
        if x + img.size[0] + 2 > page_w:
            x = 0
            y += row_h + 2
            row_h = 0
        pos.append((x, y))
        x += img.size[0] + 2
        row_h = max(row_h, img.size[1])
    page_h = 1
    while page_h < y + row_h + 2:
        page_h *= 2
    page = Image.new("RGBA", (page_w, page_h), (0, 0, 0, 0))
    ascent, descent = font.getmetrics()
    line_h = ascent + descent + outline * 2
    entries = []
    for (ch, img, bbox, adv), (px, py) in zip(glyphs, pos):
        mask = img
        if outline:
            out = mask.filter(ImageFilter.MaxFilter(outline * 2 + 1))
            dark = Image.new("RGBA", img.size, (22, 30, 48, 255))
            dark.putalpha(out)
            white = Image.new("RGBA", img.size, (255, 255, 255, 255))
            white.putalpha(mask)
            dark.alpha_composite(white)
            g = dark
        else:
            g = Image.new("RGBA", img.size, (255, 255, 255, 255))
            g.putalpha(mask)
        page.alpha_composite(g, (px, py))
        entries.append(
            f"char id={ord(ch)} x={px} y={py} width={img.size[0]} height={img.size[1]} "
            f"xoffset={bbox[0] - outline - 1} yoffset={bbox[1] - outline - 1} xadvance={round(adv)} page=0 chnl=15"
        )
    png = name + ".png"
    page.save(os.path.join(out_dir, png), optimize=True)
    with open(os.path.join(out_dir, name + ".fnt"), "w", encoding="utf-8") as f:
        f.write(f'info face="DejaVu Sans Bold" size={size} bold=1 italic=0 charset="" unicode=1 stretchH=100 smooth=1 aa=1 padding=0,0,0,0 spacing=2,2\n')
        f.write(f"common lineHeight={line_h} base={ascent + outline} scaleW={page_w} scaleH={page_h} pages=1 packed=0\n")
        f.write(f'page id=0 file="{png}"\n')
        f.write(f"chars count={len(entries)}\n")
        f.write("\n".join(entries) + "\n")


def main():
    repo = os.path.abspath(sys.argv[2] if len(sys.argv) > 2 else os.path.join(os.path.dirname(__file__), ".."))
    zip_path = sys.argv[1] if len(sys.argv) > 1 else os.path.join(repo, "assets.zip")
    tmp = tempfile.mkdtemp(prefix="bramble_assets_")
    try:
        safe_extract(zip_path, tmp)
        root = find_pack_root(tmp)
        meta = lambda n: json.load(open(os.path.join(root, "metadata", n)))
        manifest, sprites, anims, colls = meta("manifest.json"), meta("sprites.json"), meta("animations.json"), meta("collisions.json")

        out_atlas = os.path.join(repo, "assets", "atlas")
        out_data = os.path.join(repo, "assets", "data")
        out_fonts = os.path.join(repo, "assets", "fonts")
        for d in (out_atlas, out_fonts):
            shutil.rmtree(d, ignore_errors=True)
        for d in (out_atlas, out_data, out_fonts, os.path.join(repo, "docs")):
            os.makedirs(d, exist_ok=True)

        page_cache = {}

        def page(p):
            if p not in page_cache:
                page_cache[p] = Image.open(os.path.join(root, p)).convert("RGBA")
            return page_cache[p]

        groups = {}
        region_data = {}
        trim_skip = ("environments/",)
        for name, v in sprites.items():
            x, y = v["xy"]
            w, h = v["size"]
            img = page(v["page"]).crop((x, y, x + w, y + h))
            off = (0, 0)
            if not name.startswith(trim_skip) and not name.startswith("ui/"):
                bb = img.getchannel("A").getbbox()
                if bb is None:
                    bb = (0, 0, 1, 1)
                img = img.crop(bb)
                off = (bb[0], h - bb[3])  # bottom-left offset
            groups.setdefault(group_of(name), []).append({"name": name, "img": img, "orig": (w, h), "offset": off})
            c = colls.get(name)
            region_data[name] = [w, h, v["pivot"][0], v["pivot"][1]] + (
                [c["x"], c["y"], c["width"], c["height"], 1 if c["enabled"] else 0] if c else [0, 0, 0, 0, 0])

        # nine-patch: interior of the marker image with measured splits
        np_path = os.path.join(root, "ui", "components", "panel.9.png")
        np_img = Image.open(np_path).convert("RGBA")
        W, H = np_img.size
        top = [i for i in range(1, W - 1) if np_img.getpixel((i, 0))[3] > 0]
        left = [i for i in range(1, H - 1) if np_img.getpixel((0, i))[3] > 0]
        bottom = [i for i in range(1, W - 1) if np_img.getpixel((i, H - 1))[3] > 0]
        right = [i for i in range(1, H - 1) if np_img.getpixel((W - 1, i))[3] > 0]
        iw, ih = W - 2, H - 2
        split = (top[0] - 1, iw - top[-1], left[0] - 1, ih - left[-1])
        padv = (bottom[0] - 1, iw - bottom[-1], right[0] - 1, ih - right[-1])
        groups["ui"].append({"name": "ui/components/panel9", "img": np_img.crop((1, 1, W - 1, H - 1)),
                             "orig": (iw, ih), "offset": (0, 0), "split": split, "pad": padv})

        # World previews for the world-selection screen: downscaled composites of each world's
        # four parallax layers plus ground, so menus never need a full 17 MiB world atlas.
        previews = []
        for wid in sorted({n.split("/")[1] for n in sprites if n.startswith("environments/")}):
            def reg(part):
                v = sprites[f"environments/{wid}/{part}"]
                x, y = v["xy"]; w, h = v["size"]
                return page(v["page"]).crop((x, y, x + w, y + h))
            comp = Image.new("RGBA", (1280, 720))
            for i in range(4):
                comp.alpha_composite(reg(f"layer_{i}"))
            g = reg("ground")
            for gx in range(0, 1280, 256):
                comp.alpha_composite(g, (gx, 720 - 128))
            previews.append({"name": f"previews/{wid}", "img": comp.resize((320, 180), Image.LANCZOS), "orig": (320, 180), "offset": (0, 0)})
        groups["previews"] = previews

        atlases = {}
        page_report = []
        for g, entries in sorted(groups.items()):
            files, uniq = pack_group(g, entries, out_atlas)
            atlases[g] = f"atlas/{g}.atlas"
            for fname, size in files:
                page_report.append({"group": g, "file": fname, "size": size, "regions": len(entries), "unique": uniq,
                                    "gpuMiB": round(size[0] * size[1] * 4 / 1048576, 2)})

        data = {
            "version": 1,
            "source": manifest["title"],
            "regionFields": "origW,origH,pivotX,pivotY,collX,collY,collW,collH,collEnabled (bottom-left local px)",
            "regions": region_data,
            "anims": {k: [v["frameDuration"], 1 if v["loop"] else 0, v["frames"]] for k, v in anims.items()},
            "atlases": atlases,
        }
        with open(os.path.join(out_data, "sprites.json"), "w") as f:
            json.dump(data, f, separators=(",", ":"))

        ttf = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
        make_font(out_fonts, "title", 64, 4, ttf)
        make_font(out_fonts, "body", 34, 3, ttf)
        make_font(out_fonts, "small", 24, 2, ttf)

        inv = {"manifestCounts": manifest["counts"], "spriteRegions": len(sprites), "animations": len(anims),
               "packedPages": page_report,
               "totalPackedGpuMiB": round(sum(p["gpuMiB"] for p in page_report), 1),
               "sourceGpuMiB": round(sum(p["size"][0] * p["size"][1] * 4 for p in manifest["pages"]) / 1048576, 1)}
        with open(os.path.join(repo, "docs", "asset_inventory.json"), "w") as f:
            json.dump(inv, f, indent=1)
        print(json.dumps({k: inv[k] for k in ("spriteRegions", "animations", "totalPackedGpuMiB", "sourceGpuMiB")}))
        for p in page_report:
            print(f"  {p['file']:28s} {p['size'][0]}x{p['size'][1]}  {p['gpuMiB']:6.2f} MiB  regions={p['regions']} unique={p['unique']}")
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    main()
