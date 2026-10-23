"""Offline asset pipeline for Endless Adventure: text-to-image -> cleanup -> game-sized PNGs.

    python tools/imagegen/imagegen.py generate [ids...]   # call the image API for sources that are missing
    python tools/imagegen/imagegen.py convert  [ids...]   # turn sources into assets/icons and assets/scenes
    python tools/imagegen/imagegen.py prompts  [ids...]   # print the full prompts of missing sources
    python tools/imagegen/imagegen.py check                # self-test of the conversion, no network

Run from the project root. Needs Python 3 with Pillow and NumPy. The API (Cloudflare Workers AI, FLUX.1 schnell)
reads CLOUDFLARE_ACCOUNT_ID and CLOUDFLARE_API_TOKEN from the project's .env; their values are never printed.
Source images are kept in tools/imagegen/source/ (not committed); only the small converted PNGs are committed.
A source can also come from any other image tool: paste the printed prompt there and save the result as
source/<id>.png. Icons need a flat background or a transparent one.
The game draws the PNGs 1:1 with half-block characters, so every size here is a final pixel size.
"""
import base64
import json
import sys
import time
import urllib.error
import urllib.request
from io import BytesIO
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
SOURCE = HERE / "source"
ASSETS = ROOT / "assets"
SPECS = HERE / "assets.json"
MODEL = "@cf/black-forest-labs/flux-1-schnell"

ICON_SIZES = (12, 24)          # grid box (12 x 6 characters) and details panel (24 x 12 characters)
SCENE_SIZE = (150, 56)         # adventure scene area: 150 x 28 characters
ICON_FILL = 0.94               # the object's longer side fills this much of the square before shrinking

# short on purpose: the model follows a few strong words better than a long list of don'ts.
# the black outline also walls the object off from the white background, so the matte cannot eat into it
ICON_STYLE = ("16x16 pixel art RPG item icon, chunky stubby proportions, thick black outline, 4 flat colors, "
              "centered on plain white background: ")
SCENE_STYLE = ("Wide panoramic fantasy RPG background scene, important shapes large and "
               "simple, strong silhouettes, muted earthy palette with one warm accent, "
               "painted pixel-art game background style, no people, no text, no frame, no interface. Scene: ")


def load_specs():
    return json.loads(SPECS.read_text(encoding="utf-8"))


def selected(specs, ids):
    return [s for s in specs if not ids or s["id"] in ids]


def prompt(spec):
    return (ICON_STYLE if spec["kind"] == "icon" else SCENE_STYLE) + spec["subject"]


def missing(ids):
    return [s for s in selected(load_specs(), ids) if not (SOURCE / (s["id"] + ".png")).exists()]


def prompts(ids):
    for spec in missing(ids):
        print(f"{spec['id']}\n{prompt(spec)}\n")


# ---------------------------------------------------------------- generation

def read_env():
    values = {}
    env = ROOT / ".env"
    if env.exists():
        for line in env.read_text(encoding="utf-8-sig").splitlines():
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                key, value = line.split("=", 1)
                values[key.strip()] = value.strip().strip('"').strip("'")
    return values


def generate(ids):
    env = read_env()
    account, token = env.get("CLOUDFLARE_ACCOUNT_ID"), env.get("CLOUDFLARE_API_TOKEN")
    if not account or not token:
        sys.exit("Set CLOUDFLARE_ACCOUNT_ID and CLOUDFLARE_API_TOKEN in .env first; no request was sent.")
    url = f"https://api.cloudflare.com/client/v4/accounts/{account}/ai/run/{MODEL}"
    SOURCE.mkdir(exist_ok=True)
    for spec in missing(ids):
        target = SOURCE / (spec["id"] + ".png")
        # schnell is distilled for 4 steps; more steps cost more of the daily quota for little gain
        body = json.dumps({"prompt": prompt(spec), "steps": 4}).encode()
        request = urllib.request.Request(url, data=body, method="POST",
                                         headers={"Authorization": "Bearer " + token,
                                                  "Content-Type": "application/json"})
        start = time.perf_counter()
        try:
            with urllib.request.urlopen(request, timeout=120) as response:
                result = json.load(response)
            raw = base64.b64decode(result["result"]["image"], validate=True)
            Image.open(BytesIO(raw)).convert("RGB").save(target)
            print(f"{spec['id']}: ok {time.perf_counter() - start:.1f} s", flush=True)
        except urllib.error.HTTPError as e:
            # the response body only holds the API's error messages, never the request
            errors = [m.get("message") for m in json.loads(e.read() or b"{}").get("errors", [])]
            sys.exit(f"{spec['id']}: HTTP {e.code} {errors}; stopped")
        except Exception as e:  # never echo the request, it carries the token
            sys.exit(f"{spec['id']}: {type(e).__name__}; stopped")


# ---------------------------------------------------------------- conversion

def matte(image):
    """RGBA with the flat, edge-connected background made transparent; enclosed dark parts of the object stay."""
    if image.mode in ("RGBA", "LA", "P") and image.convert("RGBA").getchannel("A").getextrema()[0] < 128:
        return image.convert("RGBA")    # the tool already gave a transparent background (e.g. ChatGPT)
    a = np.asarray(image.convert("RGB")).astype(np.int16)
    h, w, _ = a.shape
    border = np.concatenate([a[0], a[-1], a[:, 0], a[:, -1]])
    color = np.median(border, axis=0)
    near = np.abs(a - color).max(axis=2) <= 24
    # flood the near-background region from outside the picture, so only edge-connected background is removed
    grid = Image.fromarray(np.pad(near, 1, constant_values=True).astype(np.uint8) * 255).copy()  # a writable copy: floodfill does not change an image that only wraps an array
    ImageDraw.floodfill(grid, (0, 0), 128)
    outside = np.asarray(grid)[1:-1, 1:-1] == 128
    rgba = np.dstack([a.astype(np.uint8), np.where(outside, 0, 255).astype(np.uint8)])
    return Image.fromarray(rgba, "RGBA")


def shrink(rgba, size):
    """area-average with premultiplied alpha, so transparent pixels do not darken the edges"""
    return rgba.convert("RGBa").resize(size, Image.Resampling.BOX).convert("RGBA")


def to_icon(source):
    rgba = matte(source)
    box = rgba.getchannel("A").getbbox()
    if box is None:
        raise ValueError("no object found against the background")
    item = rgba.crop(box)
    side = round(max(item.size) / ICON_FILL)
    square = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    square.paste(item, ((side - item.width) // 2, (side - item.height) // 2))
    return {n: shrink(square, (n, n)) for n in ICON_SIZES}


def to_scene(source):
    w, h = source.size
    band = round(w * SCENE_SIZE[1] / SCENE_SIZE[0])    # the widest crop with the scene area's proportions
    top = max(0, (h - band) // 2)
    return source.convert("RGB").crop((0, top, w, top + band)).resize(SCENE_SIZE, Image.Resampling.BOX)


def convert(ids):
    (ASSETS / "icons").mkdir(parents=True, exist_ok=True)
    (ASSETS / "scenes").mkdir(parents=True, exist_ok=True)
    for spec in selected(load_specs(), ids):
        path = SOURCE / (spec["id"] + ".png")
        if not path.exists():
            print(f"{spec['id']}: no source yet, skipped")
            continue
        with Image.open(path) as source:
            if spec["kind"] == "icon":
                icons = to_icon(source)
                for n, image in icons.items():
                    image.save(ASSETS / "icons" / f"{spec['id']}-{n}.png")
                corners = [icons[12].getpixel(p)[3] for p in ((0, 0), (11, 0), (0, 11), (11, 11))]
                if min(corners) >= 128:
                    print(f"{spec['id']}: WARNING the background was not removed (a frame or a busy background?);"
                          " delete its source and generate again")
            else:
                to_scene(source).save(ASSETS / "scenes" / f"{spec['id']}.png")
        print(f"{spec['id']}: converted")


def check():
    bg = (10, 14, 30)
    im = Image.new("RGB", (200, 200), bg)
    d = ImageDraw.Draw(im)
    d.ellipse((60, 20, 140, 180), fill=(220, 220, 225))        # the object
    d.rectangle((90, 90, 110, 110), fill=bg)                   # an enclosed dark part of it
    icons = to_icon(im)
    small = icons[12]
    assert small.size == (12, 12)
    assert small.getpixel((0, 0))[3] == 0, "background should be transparent"
    assert small.getpixel((6, 1))[3] > 128, "object should be opaque"
    assert icons[24].getpixel((12, 12))[3] > 128, "enclosed dark part must stay part of the object"
    clear = Image.new("RGBA", (200, 200), (255, 255, 255, 0))
    ImageDraw.Draw(clear).ellipse((60, 20, 140, 180), fill=(20, 20, 20, 255))  # a dark object on transparency
    assert to_icon(clear)[12].getpixel((6, 6))[3] > 128, "a transparent source keeps its own alpha"
    scene = to_scene(Image.new("RGB", (1024, 1024), bg))
    assert scene.size == SCENE_SIZE
    specs = load_specs()
    assert len({s["id"] for s in specs}) == len(specs), "duplicate asset ids"
    print("check passed")


if __name__ == "__main__":
    command, args = (sys.argv[1], sys.argv[2:]) if len(sys.argv) > 1 else ("", [])
    {"generate": generate, "convert": convert, "prompts": prompts}.get(command, lambda _: check())(args)
