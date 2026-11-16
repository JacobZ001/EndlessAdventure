# audiogen

Makes the sound effects and music loops the game plays, as WAV files in `assets/sounds/` that `com.endlessadventure.audio.Sound` loads by id.

| Kind | Output | Format |
|---|---|---|
| Sound effect | `assets/sounds/<id>.wav` | mono, 16-bit, 22050 Hz, silence trimmed, peak at -3 dBFS |
| Music loop | `assets/sounds/<id>.wav` | mono, 16-bit, 32000 Hz (about 3.7 MB a minute), every track at the same loudness (RMS about -26 dBFS), under the effects |

Levels are set here, not in the game. The game loops a music file from its last sample straight back to its first.

The committed sounds are placeholders synthesized by `audiogen.py` itself. Better ones replace them one by one: music from Suno, effects from a sound library or a sound effect generator.

## Setup

- Python 3 with NumPy, SciPy and soundfile: `pip install numpy scipy soundfile` (soundfile reads MP3, OGG and FLAC without ffmpeg)
- Run every command from the project root. Nothing uses the network.

## Files

| Path | Contents | Committed |
|---|---|---|
| `tools/audiogen/sounds.json` | One entry per sound: `id`, `kind` (`sfx` or `music`), `use` (where the game plays it), `subject` (what it should sound like), and for music an optional `loop` | yes |
| `tools/audiogen/source/` | One source per id, `<id>.wav`, `.mp3`, `.ogg` or `.flac`, any rate or channel count | no |
| `assets/sounds/` | The converted sounds the game loads | yes |

## Commands

```
python tools/audiogen/audiogen.py prompts [ids...]
python tools/audiogen/audiogen.py synth   [ids...]
python tools/audiogen/audiogen.py convert [ids...]
python tools/audiogen/audiogen.py check
```

- `prompts` prints each sound's full prompt: the shared style text (`MUSIC_STYLE`, `SFX_STYLE` in `audiogen.py`) and then its `subject`.
- `synth` writes a synthesized placeholder source for every chosen sound that has none. It never overwrites a source.
- `convert` turns sources into game sounds: all of them, or only the given ids. Stereo becomes mono, the rate becomes the kind's rate, effects lose silence at both ends and get a 3 ms fade, music is cut to its `loop`, and the levels above are applied.
- `check` is an offline self-test of the songs, the loop cut and the conversion, and warns about ids with no file in `assets/sounds/`.

## Replacing a sound

1. Get a source:
   - **Music:** run `prompts <id>`, paste the text into Suno's style box with Instrumental on, and download the take you like. Free-plan songs are for personal, non-commercial use; credit Suno in the main README.
   - **Effects:** pick one from a CC0 library (Kenney's UI Audio, RPG Audio and Impact Sounds packs; freesound.org filtered to CC0), or paste the prompt into a sound effect generator such as ElevenLabs. Credit the source in the main README.
2. Save it as `source/<id>.<type>`, and remove the old source of that id (only one may exist).
3. For music, choose the loop: listen for a start after the intro and an end where the music returns to that same place (best on a bar line, after the same number of bars), then add `"loop": [start, end]` in seconds to its entry. An easy way is to try the seam in a video editor such as Clipchamp and read both times off the original's timeline. If you cut a copy at its start only, the start is the original's length minus the copy's length (not the loop end minus it). The converter cuts there and blends the first 20 ms with the music just after the end, which only removes the click. If the seam still jumps, a third value sets a longer crossfade, e.g. `[start, end, 0.5]`; the source needs that much music after the end. Without `loop`, the whole file is looped as it is.
4. `convert <id>`, then listen to `assets/sounds/<id>.wav`, its end running into its start in particular. Adjust `loop` and convert again if the seam jumps.
5. Commit the changed files in `assets/sounds/` and `sounds.json`.

A synthesized placeholder is an `sfx_<id>` function or a `SONGS` entry (8 bars of 4/4 on an eighth-note grid, written as note names).
