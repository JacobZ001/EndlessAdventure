# audiogen

Makes the sound effects and music loops the game plays, as WAV files in `assets/sounds/` that `com.endlessadventure.audio.Sound` loads by id.

Every sound is a mono, 16-bit, 22050 Hz WAV (music costs about 44 KB per second). Levels are set here, not in the game: effects peak at -3 dBFS; music is set to the same loudness for every track (RMS about -26 dBFS), so it sits under the effects.

The current sounds are placeholders synthesized by `audiogen.py` itself (chiptune blips and 8-bar loops), so they are free to use and share. Better sounds can replace them one by one.

## Setup

- Python 3 with NumPy and SciPy: `pip install numpy scipy`
- Run every command from the project root. Nothing uses the network.

## Files

| Path | Contents | Committed |
|---|---|---|
| `tools/audiogen/sounds.json` | One entry per sound: `id`, `kind` (`sfx` or `music`) and `use` (where the game plays it) | yes |
| `tools/audiogen/source/` | One source per id, `<id>.wav`, any rate, bit depth or channel count | no |
| `assets/sounds/` | The converted sounds the game loads | yes |

## Commands

```
python tools/audiogen/audiogen.py synth   [ids...]
python tools/audiogen/audiogen.py convert [ids...]
python tools/audiogen/audiogen.py check
```

- `synth` writes a synthesized source for every chosen sound that has none yet. It never overwrites a source. Effects are the `sfx_<id>` functions; songs are the `SONGS` table (8 bars of 4/4 on an eighth-note grid, written as note names).
- `convert` turns sources into game sounds: all of them, or only the given ids. Stereo becomes mono, the rate becomes 22050 Hz, effects lose silence at both ends and get a 3 ms fade, and the levels above are applied. Music is resampled as a loop, so its seam stays smooth.
- `check` is an offline self-test of the songs and the conversion, and warns about ids with no file in `assets/sounds/`.

## Adding or replacing a sound

1. Add or edit the entry in `sounds.json`.
2. Get a source, either way:
   - write a `sfx_<id>` function or a `SONGS` entry and run `synth <id>` (delete the old source first when redoing), or
   - save a WAV as `source/<id>.wav`. Only use sounds whose license allows sharing them in a public repository (CC0 is simplest; Kenney and OpenGameArt have CC0 packs), and add the source and license to the main README. A music source should already loop cleanly.
3. `convert <id>` and listen to `assets/sounds/<id>.wav`.
4. Commit the changed files in `assets/sounds/` and `sounds.json`.
