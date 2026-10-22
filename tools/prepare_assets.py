"""Converts the raw art pack (SMOKE_MASTER_COMPLETE_ASSETS.zip, extracted) into
optimized game assets under app/src/main/assets and launcher icons under res/.

usage: python3 tools/prepare_assets.py <extracted_zip_dir>
"""
import os
import sys

from PIL import Image

SRC = sys.argv[1]
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "app/src/main/assets/img")
RES = os.path.join(ROOT, "app/src/main/res")
os.makedirs(OUT, exist_ok=True)

# Opaque full-screen backgrounds -> JPEG.
BACKGROUNDS = {
    "A_splash/splash_bg.png": "splash_bg",
    "B_lobby/lobby_bg.png": "lobby_bg",
    "D_items_effects/gameplay_bg.png": "gameplay_bg",
}

# Puppet layers share a 1024x1024 canvas; they are trimmed and their offset is
# written to layers.txt so the game can place them back exactly.
LAYERS = [
    "char_body", "char_arm_idle", "char_arm_hold", "char_arm_mouth",
    "head_normal", "head_smoking", "head_exhale",
    "head_red_1", "head_red_2", "head_red_3", "head_burnt",
]

# Everything else: trimmed, optionally scaled (name -> max width).
SPRITES = {
    "A_splash/game_logo.png": 1000,
    "B_lobby/character_lobby.png": 520,
    "B_lobby/btn_play.png": None,
    "B_lobby/btn_highscore.png": None,
    "B_lobby/btn_settings.png": None,
    "D_items_effects/cig_box_1.png": 300,
    "D_items_effects/cig_box_2.png": 300,
    "D_items_effects/cig_box_3.png": 300,
    "D_items_effects/cig_box_4.png": 300,
    "D_items_effects/cig_box_5.png": 300,
    "D_items_effects/cigarette_lit.png": None,
    "D_items_effects/smoke_puff_1.png": None,
    "D_items_effects/smoke_puff_2.png": None,
    "D_items_effects/smoke_puff_3.png": None,
    "D_items_effects/explosion.png": 720,
    "D_items_effects/ember_spark.png": None,
    "E_gameplay_ui/btn_pick_hand.png": None,
    "E_gameplay_ui/btn_smoke.png": None,
    "E_gameplay_ui/btn_pause.png": None,
    "E_gameplay_ui/score_panel.png": None,
    "F_popups_settings/popup_panel.png": None,
    "F_popups_settings/btn_resume.png": None,
    "F_popups_settings/btn_home.png": None,
    "F_popups_settings/btn_close.png": None,
    "F_popups_settings/toggle_on.png": None,
    "F_popups_settings/toggle_off.png": None,
    "F_popups_settings/icon_music.png": None,
    "F_popups_settings/icon_sound.png": None,
    "F_popups_settings/icon_vibration.png": None,
    "F_popups_settings/trophy.png": None,
    "G_game_over/char_gameover.png": 640,
    "G_game_over/gameover_title.png": 1000,
    "G_game_over/btn_play_again.png": None,
}


def save_png(im, name):
    im.save(os.path.join(OUT, name + ".png"), optimize=True)


for src, name in BACKGROUNDS.items():
    im = Image.open(os.path.join(SRC, src)).convert("RGB")
    im.save(os.path.join(OUT, name + ".jpg"), quality=90, optimize=True, progressive=False)

with open(os.path.join(OUT, "layers.txt"), "w") as f:
    for name in LAYERS:
        im = Image.open(os.path.join(SRC, "C_character_layers", name + ".png")).convert("RGBA")
        box = im.getchannel("A").point(lambda a: 255 if a > 2 else 0).getbbox()
        save_png(im.crop(box), name)
        f.write(f"{name} {box[0]} {box[1]}\n")

with open(os.path.join(OUT, "sprites.txt"), "w") as f:
    for src, max_w in SPRITES.items():
        name = os.path.splitext(os.path.basename(src))[0]
        im = Image.open(os.path.join(SRC, src)).convert("RGBA")
        box = im.getchannel("A").point(lambda a: 255 if a > 2 else 0).getbbox()
        im = im.crop(box)
        scale = 1.0
        if max_w and im.width > max_w:
            scale = max_w / im.width
            im = im.resize((max_w, round(im.height * scale)), Image.LANCZOS)
        save_png(im, name)
        # crop origin + scale lets the code map original-canvas points (e.g. the
        # cigarette tip in character_lobby) into the trimmed sprite.
        f.write(f"{name} {box[0]} {box[1]} {scale:.5f}\n")

# Launcher icons.
icon = Image.open(os.path.join(SRC, "H_app_icon/app_icon.png")).convert("RGB")
for dpi, px in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
    d = os.path.join(RES, "mipmap-" + dpi)
    os.makedirs(d, exist_ok=True)
    icon.resize((px, px), Image.LANCZOS).save(os.path.join(d, "ic_launcher.png"), optimize=True)
    # Adaptive foreground: 108dp canvas, art fills the 84dp centre so every mask
    # shape shows the face.
    fg_px = px * 108 // 48
    art = icon.resize((fg_px * 84 // 108,) * 2, Image.LANCZOS)
    fg = Image.new("RGBA", (fg_px, fg_px), (0, 0, 0, 0))
    off = (fg_px - art.width) // 2
    fg.paste(art, (off, off))
    fg.save(os.path.join(d, "ic_launcher_foreground.png"), optimize=True)
print("done")
