# imagegen

Turns short text prompts into the small PNG images the game draws: item icons and adventure-screen scenes.

The game draws an image 1:1, two pixels per character cell (the top and bottom half of a `▀`), so every size below is final and the game never scales anything:

| Kind | Output | Size in pixels | Size on screen |
|---|---|---|---|
| Icon, inventory grid box | `assets/icons/<id>-12.png` | 12 × 12 | 12 × 6 characters |
| Icon, item details | `assets/icons/<id>-24.png` | 24 × 24 | 24 × 12 characters |
| Scene, adventure screen | `assets/scenes/<id>.png` | 150 × 56 | 150 × 28 characters |
| Portrait, paper doll (character column) | `assets/portraits/<id>.png` | at most 32 × 62, standing on the bottom edge | up to 32 × 31 characters |
| Backdrop, behind a whole menu | `assets/backdrops/<id>.png` | 207 wide, up to 98 high | inside the screen frame, resting on its bottom |

## Setup

- Python 3 with Pillow, NumPy and SciPy: `pip install pillow numpy scipy`
- Run every command from the project root.
- Only `generate` uses the network. It needs `CLOUDFLARE_ACCOUNT_ID` and `CLOUDFLARE_API_TOKEN` in `.env` (see `.env.example`); their values are never printed.

## Files

| Path | Contents | Committed |
|---|---|---|
| `tools/imagegen/assets.json` | One entry per image: `id`, `kind` (`icon` or `scene`) and `subject` | yes |
| `tools/imagegen/source/` | One full-size source image per id, `<id>.png` (or `.jpg`, `.jpeg`, `.webp`) | no |
| `assets/icons/`, `assets/scenes/` | The converted images the game loads | yes |

Only `source/<id>.*` is read. Alternatives can be kept in a subfolder such as `source/testing/`.

## Commands

```
python tools/imagegen/imagegen.py prompts  [ids...]
python tools/imagegen/imagegen.py generate [ids...]
python tools/imagegen/imagegen.py convert  [ids...]
python tools/imagegen/imagegen.py check
```

- `prompts` prints each id with its full prompt, ready to paste into an image tool. With ids it prints those ids; without, every asset that has no source yet.
- `generate` asks Cloudflare Workers AI (FLUX.1 schnell) for every missing source and saves it as `source/<id>.png`. It never overwrites a source, and it stops at the first error with the API's message. The free tier allows 10,000 neurons a day and resets at 00:00 UTC.
- `convert` turns sources into game images: all of them, or only the given ids. Icons lose their background (the flat color around the object, including larger patches of it that the object encloses, such as the inside of a knuckle guard; or the image's own transparency), are cropped to the object, padded to a square and shrunk. Portraits lose their background like icons and are shrunk to fit, standing on the bottom edge. Scenes lose any black bars above and below, then are cropped from the middle to the scene area's proportions and shrunk.
- `check` is an offline self-test of the conversion. It also checks that the icons in `assets.json` match the items in `data/weapons.json`, `data/armor.json` and `data/shields.json` one to one.

## Adding or redoing an image

1. Add or edit the entry in `assets.json`. An icon's id must be the item's id in `data/`.
2. Get a source image, either way:
   - `generate <id>` (delete or move the old source first when redoing), or
   - `prompts <id>`, paste the prompt into ChatGPT or Gemini, and save the result as `source/<id>.png` (a JPG works too).
3. `convert <id>` and read any warnings.
4. Look at it in the game: the inventory grid and the item details for icons, the adventure screen for scenes.
5. Commit the changed files in `assets/` and `assets.json`.

## Writing a subject

The shared style text (`ICON_STYLE`, `SCENE_STYLE` in `imagegen.py`) comes first; the subject only describes the object.

- Start with what the item is, by its own name (brigandine, gambeson, scimitar, heater shield), then the visible details. A paraphrase such as "a coat studded with rivets" gets drawn as something else.
- Keep it short. The models follow a few strong words better than a long list, and "no ..." phrases often backfire.
- Describe only what survives at 24 pixels: the shape, the main colors, one or two features. Take them from the item's `description` in `data/` (a buckler "like a dented iron pot lid", a mace head "bolted onto a wooden axle shaft"), and leave out what cannot be seen at that size (a name scratched on the hilt).
- Give proportions by comparison. Every icon is scaled to fill its box, so a dagger comes out as large as a greatsword and only its shape tells them apart: "blade as long as its handle", "very long blade".
- Weapons point to the top right (the models do not always obey; make a new source when one points elsewhere); armor and shields face the front. The Recruit's set uses bright steel, brass and red grips; the Drifter's set uses worn grey metal and brown leather.
- A backdrop is shrunk to the screen's width and kept from the bottom up. Every row is shifted so that its sky becomes `Palette.BG`, which keeps the land's contrast against the sky. Text cells always carry `Palette.BG`, so a backdrop entry also lists its text areas, `"text": [[x, row, width, rows], ...]` in screen characters, and the picture fades to `Palette.BG` around them. Its subject keeps the middle of the picture empty sky and the land low, where the menu goes.
- Scenes are cropped to the scene area's 150:56 (about 8:3) proportions, keeping the middle; a square picture keeps only the middle 37% of its height. The scene prompt asks for 8:3, which ChatGPT makes as 2048 x 768. Do not ask for black bars: the picture inside them comes out even wider and loses its sides.

## Warnings and errors

| Message | Meaning | What to do |
|---|---|---|
| `the background was not removed` | The picture has a frame or a busy background | Make a new source |
| `the object touches the picture's edge` | The object is probably cut off | Make a new source |
| `only N% of the picture fits the scene area` | A scene picture is far from 8:3 | Make an 8:3 source |
| `several sources, using ...` | `source/` holds more than one file for this id | Keep one, move the others to `source/testing/` |
| `HTTP 400 ... NSFW` | A false positive of Cloudflare's filter | Reword the subject ("empty shirt" triggered it once) |
| `HTTP 429 ... daily free allocation` | The day's quota is used up | Wait for 00:00 UTC, or use `prompts` with another image tool |
