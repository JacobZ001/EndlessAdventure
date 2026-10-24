"""Offline asset pipeline for Endless Adventure: text-to-image -> cleanup -> game-sized PNGs.

    python tools/imagegen/imagegen.py generate [ids...]   # call the image API for sources that are missing
    python tools/imagegen/imagegen.py convert  [ids...]   # turn sources into assets/icons, scenes and backdrops
    python tools/imagegen/imagegen.py prompts  [ids...]   # print the full prompts of missing sources
    python tools/imagegen/imagegen.py check                # self-test of the conversion, no network

Run from the project root. Needs Python 3 with Pillow, NumPy and SciPy. The API (Cloudflare Workers AI, FLUX.1 schnell)
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
from scipy import ndimage

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
SOURCE = HERE / "source"
ASSETS = ROOT / "assets"
SPECS = HERE / "assets.json"
MODEL = "@cf/black-forest-labs/flux-1-schnell"

ICON_SIZES = (12, 24)          # grid box (12 x 6 characters) and details panel (24 x 12 characters)
SCENE_SIZE = (150, 56)         # adventure scene area: 150 x 28 characters
BACKDROP_SIZE = (207, 98)      # a whole screen inside its frame: 207 x 49 characters
PORTRAIT_SIZE = (32, 62)       # the paper doll between the equipment slots: at most 32 x 31 characters
BG = (0x0C, 0x10, 0x17)        # Palette.BG: text cells carry it, so a backdrop's sky and the ground under text become it
DIM = 0.2                      # a "dim" backdrop keeps this share of the picture's contrast against BG
ICON_FILL = 0.94               # the object's longer side fills this much of the square before shrinking
HOLE_MIN = 0.001               # an enclosed background-colored area this share of the picture or larger is a hole;
                               # measured: a knuckle guard's hole 0.7 %, highlights and chainmail gaps under 0.01 %

# short on purpose: the model follows a few strong words better than a long list of don'ts.
# the black outline also walls the object off from the white background, so the matte cannot eat into it
ICON_STYLE = ("16x16 pixel art RPG item icon, chunky bold shapes, thick black outline, 4 flat colors, "
              "centered on plain white background: ")
# the scene area is 150:56 (about 8:3); a picture with other proportions loses its sides or its top and bottom
SCENE_STYLE = ("8:3 ultra-wide panorama, medium resolution pixel art RPG background, large simple shapes, "
               "limited muted palette, no people: ")
# a backdrop's subject also says where it stays empty, because menus are drawn over it
BACKDROP_STYLE = "Wide pixel art fantasy RPG title screen background at night, no text, no characters. "
# a whole standing figure in the paper doll's proportions (32 x 62 is about 1:2), so it scales straight into place;
# at this size the face is a few pixels anyway: the silhouette and the large color areas carry the character
# the pose belongs to each character's subject: "standing straight, front view" here made every figure stand at attention
PORTRAIT_STYLE = ("1:2 tall portrait, full body pixel art RPG character, "
                  "whole figure from head to feet filling the height, flat colors, centered on plain white background: ")
STYLES = {"icon": ICON_STYLE, "scene": SCENE_STYLE, "backdrop": BACKDROP_STYLE, "portrait": PORTRAIT_STYLE}


def load_specs():
    return json.loads(SPECS.read_text(encoding="utf-8"))


def selected(specs, ids):
    return [s for s in specs if not ids or s["id"] in ids]


def prompt(spec):
    return STYLES[spec["kind"]] + spec["subject"]


def source_of(spec):
    """the saved source image, whatever format the image tool gave (Gemini saves JPG), or None.
    an entry with "source" reuses the picture of the entry it names"""
    name = spec.get("source", spec["id"])
    found = [p for p in (SOURCE / (name + ext) for ext in (".png", ".jpg", ".jpeg", ".webp")) if p.exists()]
    if len(found) > 1:
        print(f"{spec['id']}: WARNING several sources, using {found[0].name}")
    return found[0] if found else None


def missing(ids):
    return [s for s in selected(load_specs(), ids) if source_of(s) is None]


def prompts(ids):
    """the named assets (to redo them), or every asset that has no source yet"""
    for spec in selected(load_specs(), ids) if ids else missing(ids):
        print(f"{spec['id']}\n" + (f"(made from the picture of {spec['source']})" if "source" in spec else prompt(spec)) + "\n")


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
    """RGBA with the flat background made transparent: every background-colored area that touches the picture's edge,
    and every enclosed one big enough to be a hole (inside a knuckle guard). small enclosed ones are highlights and stay"""
    if image.mode in ("RGBA", "LA", "P") and image.convert("RGBA").getchannel("A").getextrema()[0] < 128:
        return image.convert("RGBA")    # the tool already gave a transparent background (e.g. ChatGPT)
    a = np.asarray(image.convert("RGB")).astype(np.int16)
    h, w, _ = a.shape
    border = np.concatenate([a[0], a[-1], a[:, 0], a[:, -1]])
    color = np.median(border, axis=0)
    near = np.abs(a - color).max(axis=2) <= 24
    labels, _ = ndimage.label(near)     # connected background-colored areas, numbered from 1; 0 is the object
    edge = np.unique(np.concatenate([labels[0], labels[-1], labels[:, 0], labels[:, -1]]))
    hole = np.bincount(labels.ravel()) >= HOLE_MIN * h * w
    clear = (np.isin(labels, edge) | hole[labels]) & (labels > 0)
    rgba = np.dstack([a.astype(np.uint8), np.where(clear, 0, 255).astype(np.uint8)])
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


def to_portrait(source):
    """the figure without its background, shrunk to fit PORTRAIT_SIZE and standing on its bottom edge"""
    rgba = matte(source)
    box = rgba.getchannel("A").getbbox()
    if box is None:
        raise ValueError("no figure found against the background")
    figure = rgba.crop(box)
    k = min(PORTRAIT_SIZE[0] / figure.width, PORTRAIT_SIZE[1] / figure.height)
    small = shrink(figure, (max(1, round(figure.width * k)), max(1, round(figure.height * k))))
    out = Image.new("RGBA", PORTRAIT_SIZE, (0, 0, 0, 0))
    out.paste(small, ((PORTRAIT_SIZE[0] - small.width) // 2, PORTRAIT_SIZE[1] - small.height))
    return out


def unletterbox(source):
    """the picture without the near-black bars above and below it"""
    rgb = source.convert("RGB")
    content = np.flatnonzero(np.asarray(rgb).max(axis=(1, 2)) > 24)    # rows with anything brighter than near-black
    return rgb.crop((0, content[0], rgb.width, content[-1] + 1)) if content.size else rgb


def to_scene(source):
    """the middle of the picture with the scene area's proportions, shrunk to the scene size"""
    w, h = source.size
    cw = min(w, round(h * SCENE_SIZE[0] / SCENE_SIZE[1]))   # too wide: cut the sides
    ch = min(h, round(w * SCENE_SIZE[1] / SCENE_SIZE[0]))   # too tall: cut the top and bottom
    left, top = (w - cw) // 2, (h - ch) // 2
    return source.convert("RGB").crop((left, top, left + cw, top + ch)).resize(SCENE_SIZE, Image.Resampling.BOX)


def to_backdrop(source, text_cells, look=None):
    """a full-width picture for behind a menu: shrunk to the screen's width and kept from the bottom up, the sky
    recolored to BG, and faded to BG around the text cells [x, row, width, rows] that the menu draws over it.
    look "dim" keeps only a trace of the picture, for screens where it should stay in the background"""
    w, h = BACKDROP_SIZE
    picture = unletterbox(source)
    height = round(w * picture.height / picture.width)
    a = np.asarray(picture.resize((w, height), Image.Resampling.BOX)).astype(float)[-h:]
    height = a.shape[0]
    # the sky's color in each row, from the middle third of the top 40 %, which the prompt keeps empty; every pixel of
    # a row moves by the same amount, so the sky becomes BG and the land keeps its contrast against it. below 40 %
    # the shift of the last measured row is kept, and the sky low between the hills stays a little lighter, like haze
    top = max(2, int(height * 0.4))
    sky = np.median(a[:top, w // 3:2 * w // 3], axis=1)
    sky = np.vstack([sky, np.repeat(sky[-1:], height - top, axis=0)])
    a += (np.array(BG) - sky)[:, None]
    if look == "dim":
        a = np.array(BG) + (a - np.array(BG)) * DIM
    # the picture's top row is screen pixel row 100 - height; its left column is screen column 1
    ys, xs = np.mgrid[0:height, 0:w]
    ys, xs = ys + 100 - height, xs + 1
    fade = np.zeros((height, w))
    for x, row, cw, ch in text_cells:
        dx = np.maximum(np.maximum(x - 3 - xs, xs - (x + cw - 1) - 3), 0)
        dy = np.maximum(np.maximum(row * 2 - 3 - ys, ys - (row + ch) * 2 + 1 - 3), 0)
        fade = np.maximum(fade, 1 - np.minimum(1, np.hypot(dx, dy * 1.6) / 12))
    a += (np.array(BG) - a) * (fade ** 1.5)[..., None]
    return Image.fromarray(np.clip(np.rint(a), 0, 255).astype(np.uint8), "RGB")


def convert(ids):
    for folder in ("icons", "scenes", "backdrops", "portraits"):
        (ASSETS / folder).mkdir(parents=True, exist_ok=True)
    for spec in selected(load_specs(), ids):
        path = source_of(spec)
        if path is None:
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
                left, top, right, bottom = matte(source).getchannel("A").getbbox()
                if min(left, top) == 0 or right == source.width or bottom == source.height:
                    print(f"{spec['id']}: WARNING the object touches the picture's edge and may be cut off")
            elif spec["kind"] == "portrait":
                to_portrait(source).save(ASSETS / "portraits" / f"{spec['id']}.png")
            elif spec["kind"] == "backdrop":
                to_backdrop(source, spec["text"], spec.get("look")).save(ASSETS / "backdrops" / f"{spec['id']}.png")
            else:
                picture = unletterbox(source)
                to_scene(picture).save(ASSETS / "scenes" / f"{spec['id']}.png")
                ratio = picture.width / picture.height / (SCENE_SIZE[0] / SCENE_SIZE[1])
                if min(ratio, 1 / ratio) < 0.7:
                    print(f"{spec['id']}: WARNING only {min(ratio, 1 / ratio):.0%} of the picture fits the scene area;"
                          " make an 8:3 picture")
        print(f"{spec['id']}: converted")


def check():
    bg = (10, 14, 30)
    im = Image.new("RGB", (200, 200), bg)
    d = ImageDraw.Draw(im)
    d.ellipse((60, 20, 140, 180), fill=(220, 220, 225))        # the object
    d.rectangle((90, 90, 110, 110), fill=bg)                   # a hole through it (1 % of the picture)
    d.point((100, 40), fill=bg)                                # a highlight in the background's color
    icons = to_icon(im)
    small = icons[12]
    assert small.size == (12, 12)
    assert small.getpixel((0, 0))[3] == 0, "background should be transparent"
    assert small.getpixel((6, 1))[3] > 128, "object should be opaque"
    assert icons[24].getpixel((12, 12))[3] < 128, "a hole should be transparent"
    assert matte(im).getpixel((100, 40))[3] == 255, "a small enclosed spot is part of the object"
    clear = Image.new("RGBA", (200, 200), (255, 255, 255, 0))
    ImageDraw.Draw(clear).ellipse((60, 20, 140, 180), fill=(20, 20, 20, 255))  # a dark object on transparency
    assert to_icon(clear)[12].getpixel((6, 6))[3] > 128, "a transparent source keeps its own alpha"
    letterboxed = Image.new("RGB", (1536, 1024), (0, 0, 0))
    letterboxed.paste(Image.new("RGB", (1536, 576), bg), (0, 224))        # an 8:3 picture between black bars
    assert unletterbox(letterboxed).size == (1536, 576), "black bars should be cut off"
    scene = to_scene(Image.new("RGB", (1024, 1024), bg))
    assert scene.size == SCENE_SIZE
    assert to_scene(Image.new("RGB", (2048, 476), bg)).getpixel((75, 55)) == bg, "a too wide picture loses its sides"
    night = Image.new("RGB", (2048, 768), (6, 17, 37))
    ImageDraw.Draw(night).rectangle((0, 560, 2047, 767), fill=(40, 70, 40))   # the land along the bottom
    backdrop = to_backdrop(night, [[100, 40, 10, 1]])                           # one line of text at row 40
    assert backdrop.width == BACKDROP_SIZE[0] and backdrop.height <= BACKDROP_SIZE[1]
    assert backdrop.getpixel((5, 0)) == BG, "the sky becomes BG"
    assert backdrop.getpixel((104, 80 - (100 - backdrop.height))) == BG, "the picture fades to BG under text"
    dim = np.asarray(to_backdrop(night, [], "dim")).astype(int)
    full = np.asarray(to_backdrop(night, [])).astype(int)
    assert np.abs((dim - BG) - (full - BG) * DIM).max() <= 1, "a dim backdrop keeps DIM of the contrast against BG"
    shift = tuple(b - s for b, s in zip(BG, (6, 17, 37)))
    assert backdrop.getpixel((5, backdrop.height - 1)) == tuple(c + d for c, d in zip((40, 70, 40), shift)), \
        "land away from the text moves with the sky, keeping its contrast"
    tall = Image.new("RGB", (400, 1000), (255, 255, 255))
    ImageDraw.Draw(tall).rectangle((150, 100, 250, 900), fill=(40, 40, 40))       # a standing figure
    portrait = to_portrait(tall)
    assert portrait.size == PORTRAIT_SIZE and portrait.getpixel((16, 61))[3] > 128, "the figure stands on the bottom edge"
    assert portrait.getpixel((0, 0))[3] == 0, "the background around the figure is transparent"
    specs = load_specs()
    assert len({s["id"] for s in specs}) == len(specs), "duplicate asset ids"
    gear = [ROOT / "data" / f for f in ("weapons.json", "armor.json", "shields.json")]  # every item gets an icon
    catalog = {row["id"] for f in gear for row in json.loads(f.read_text(encoding="utf-8"))}
    icons = {s["id"] for s in specs if s["kind"] == "icon"}
    assert icons == catalog, f"icons without an item: {icons - catalog}, items without an icon: {catalog - icons}"
    print("check passed")


if __name__ == "__main__":
    command, args = (sys.argv[1], sys.argv[2:]) if len(sys.argv) > 1 else ("", [])
    {"generate": generate, "convert": convert, "prompts": prompts}.get(command, lambda _: check())(args)
