"""Makes the sound effects and music loops the game plays. See README.md in this folder."""

import io
import json
import math
import sys
from pathlib import Path

import numpy as np
import soundfile
from scipy.signal import butter, resample_poly, sosfilt

ROOT = Path(__file__).resolve().parents[2]
HERE = ROOT / "tools" / "audiogen"
SOURCE = HERE / "source"
OUT = ROOT / "assets" / "sounds"

SYNTH_RATE = 44100
RATES = {"sfx": 22050, "music": 32000}   # mono 16-bit; music costs about 3.7 MB a minute
SOURCE_TYPES = (".wav", ".mp3", ".ogg", ".flac")
LOOP_FADE = 0.5        # seconds of crossfade where a cut music loop wraps around
SFX_PEAK = 0.7         # -3 dBFS
MUSIC_RMS = 0.05       # about -26 dBFS, well under the effects
MUSIC_PEAK = 0.5

# shared style text that comes before each subject in `prompts`, as in imagegen
MUSIC_STYLE = ("instrumental, no vocals, dark fantasy RPG soundtrack, restrained cinematic, "
               "steady tempo and mood throughout, loopable: ")
SFX_STYLE = "short dry game sound effect, single sound, no music, no reverb tail: "


def manifest():
    return json.loads((HERE / "sounds.json").read_text(encoding="utf-8"))


# ---------- synthesis: placeholder sources, all made here, so free to use and share ----------

def t(seconds):
    return np.arange(int(seconds * SYNTH_RATE)) / SYNTH_RATE


def decay(seconds, rate):
    return np.exp(-t(seconds) * rate)


def sweep(f0, f1, seconds):
    """phase of a tone gliding from f0 to f1 Hz"""
    f = np.geomspace(f0, f1, len(t(seconds)))
    return 2 * np.pi * np.cumsum(f) / SYNTH_RATE


def square(phase, duty=0.5):
    return np.where((phase / (2 * np.pi)) % 1 < duty, 1.0, -1.0)


def triangle(phase):
    return 2 * np.abs(2 * ((phase / (2 * np.pi)) % 1) - 1) - 1


def noise(seconds, seed):
    return np.random.default_rng(seed).uniform(-1, 1, len(t(seconds)))


def band(x, low, high):
    return sosfilt(butter(2, [low, high], "bandpass", fs=SYNTH_RATE, output="sos"), x)


def lowpass(x, cutoff):
    return sosfilt(butter(2, cutoff, "lowpass", fs=SYNTH_RATE, output="sos"), x)


def blip(f, seconds, duty=0.5):
    return square(sweep(f, f, seconds), duty) * decay(seconds, 30)


def sfx_ui_move():
    return np.sin(sweep(1400, 1100, 0.03)) * decay(0.03, 120)


def sfx_ui_confirm():
    return lowpass(np.concatenate([blip(660, 0.06, 0.25), blip(990, 0.12, 0.25)]), 4000)


def sfx_ui_back():
    return lowpass(np.concatenate([blip(660, 0.06, 0.25), blip(440, 0.12, 0.25)]), 4000)


def sfx_ui_error():
    return lowpass(square(sweep(150, 140, 0.16), 0.5) * decay(0.16, 12), 2000)


def sfx_hit():
    thump = np.sin(sweep(160, 45, 0.18)) * decay(0.18, 18)
    crack = band(noise(0.18, 1), 800, 5000) * decay(0.18, 40)
    return thump + 0.8 * crack


def sfx_block():
    clang = sum(np.sin(sweep(f, f, 0.35)) * decay(0.35, r) for f, r in
                [(523, 9), (1307, 12), (2213, 16), (3121, 22)])
    return clang + 0.4 * band(noise(0.35, 2), 2000, 8000) * decay(0.35, 60)


def sfx_miss():
    n = len(t(0.22))
    shape = np.sin(np.linspace(0, np.pi, n)) ** 2
    rising = band(noise(0.22, 3), 400, 1500) * shape[::-1] * np.linspace(1, 0, n)
    falling = band(noise(0.22, 4), 1500, 4000) * shape * np.linspace(0, 1, n)
    return rising + falling


# A song is 8 bars of 4/4 on an eighth-note grid, so every bar string has 8 steps:
# a note such as "C#5", "-" holds the previous note, "." is a rest.
SONGS = {
    "menu": dict(bpm=76, drums=False, bass="half", chords="Am F C G Am F E E", lead=[
        "E5 - - - D5 - C5 -", "A4 - - - - - . .", "C5 - D5 - E5 - G5 -", "D5 - - - - - . .",
        "E5 - - - A5 - G5 -", "F5 - E5 - C5 - - -", "B4 - - - G#4 - B4 -", "E4 - - - - - . ."]),
    "adventure": dict(bpm=100, drums="light", bass="walk", chords="D G A D Bm G A A", lead=[
        "F#5 - A5 - D5 - E5 F#5", "G5 - F#5 - E5 - D5 -", "E5 - - C#5 A4 - - -", "D5 - F#5 - A5 - - -",
        "B5 - A5 - F#5 - D5 -", "G5 - - - E5 - G5 -", "A5 - G5 - F#5 - E5 -", "C#5 - - - - - . ."]),
    "combat": dict(bpm=140, drums="full", bass="drive", chords="Em Em C D Em Em C B", lead=[
        "E5 . E5 G5 - . B5 -", "A5 - G5 - F#5 - E5 -", "G5 . G5 E5 - . C5 -", "D5 - F#5 - A5 - F#5 -",
        "E5 . E5 G5 - . B5 -", "C6 - B5 - A5 - G5 -", "E5 - G5 - C6 - B5 -", "B5 - - - D#5 - F#5 -"]),
}

NOTE_STEPS = {"C": 0, "D": 2, "E": 4, "F": 5, "G": 7, "A": 9, "B": 11}


def midi(name):
    """'C#5' -> 73; a name without an octave, such as a chord root, is put in octave 2"""
    i = 2 if len(name) > 1 and name[1] in "#b" else 1
    shift = {"#": 1, "b": -1}.get(name[1:i], 0)
    octave = int(name[i:]) if name[i:] else 2
    return 12 * (octave + 1) + NOTE_STEPS[name[0]] + shift


def hz(m):
    return 440 * 2 ** ((m - 69) / 12)


def chord(name):
    """'F#m' -> root midi in octave 2 and the triad's midi notes"""
    minor = name.endswith("m")
    root = midi(name[:-1] if minor else name)
    return root, [root, root + (3 if minor else 4), root + 7]


def tone(m, seconds, wave, release=0.06):
    """one note with a short attack, a fall to 60% and a release after its length"""
    s = t(seconds + release)
    env = np.minimum(1, s / 0.005) * (0.6 + 0.4 * np.exp(-s * 12))
    env *= np.clip((seconds + release - s) / release, 0, 1)
    return wave(2 * np.pi * hz(m) * s) * env


def drum(kind):
    if kind == "kick":
        return np.sin(sweep(120, 45, 0.15)) * decay(0.15, 25)
    if kind == "snare":
        return 0.7 * band(noise(0.12, 5), 1000, 6000) * decay(0.12, 30) + 0.4 * np.sin(sweep(190, 170, 0.12)) * decay(0.12, 40)
    return 0.4 * band(noise(0.03, 6), 6000, 12000) * decay(0.03, 120)


def song(name):
    spec = SONGS[name]
    step = 60 / spec["bpm"] / 2
    length = int(round(64 * step * SYNTH_RATE))
    mix = np.zeros(length + SYNTH_RATE)          # the tail past the loop end wraps to its start

    def put(sound, at_step, gain):
        start = int(round(at_step * step * SYNTH_RATE))
        mix[start:start + len(sound)] += gain * sound[:len(mix) - start]

    lead = np.zeros_like(mix)
    for bar, line in enumerate(spec["lead"]):
        tokens = line.split()
        assert len(tokens) == 8, f"{name} bar {bar + 1} has {len(tokens)} steps"
        for i, tok in enumerate(tokens):
            if tok in "-.":
                continue
            held = 1
            while i + held < 8 and tokens[i + held] == "-":
                held += 1
            start = int(round((bar * 8 + i) * step * SYNTH_RATE))
            note = tone(midi(tok), held * step * 0.9, lambda p: square(p, 0.25))
            lead[start:start + len(note)] += note[:len(lead) - start]
    mix += 0.22 * lowpass(lead, 3500)

    for bar, name_ in enumerate(spec["chords"].split()):
        root, triad = chord(name_)
        for i in range(8):
            put(tone(triad[i % 3] + 24, step * 0.8, triangle), bar * 8 + i, 0.07)   # arpeggio pad
        if spec["bass"] == "half":
            for i in (0, 4):
                put(tone(root + 12, step * 3.8, triangle), bar * 8 + i, 0.35)
        elif spec["bass"] == "walk":
            for i, m in enumerate([root, root + 7] * 2):
                put(tone(m + 12, step * 1.6, triangle), bar * 8 + 2 * i, 0.35)
        else:
            for i in range(8):
                put(tone(root + 12 * (1 + i % 2), step * 0.7, triangle), bar * 8 + i, 0.32)
        if spec["drums"]:
            for i in range(8):
                if i in (0, 4):
                    put(drum("kick"), bar * 8 + i, 0.6)
                if spec["drums"] == "full" and i in (2, 6):
                    put(drum("snare"), bar * 8 + i, 0.4)
                if spec["drums"] == "full" or i % 2:
                    put(drum("hat"), bar * 8 + i, 0.3)

    loop = mix[:length].copy()
    loop[:len(mix) - length] += mix[length:]
    return loop


def synth(entry):
    if entry["kind"] == "music":
        return song(entry["id"]) if entry["id"] in SONGS else None
    recipe = globals().get("sfx_" + entry["id"])
    return recipe() if recipe else None


# ---------- conversion: any source file -> the game's format ----------

def source_of(entry):
    """the entry's source file in any readable type, or None"""
    found = [SOURCE / (entry["id"] + t) for t in SOURCE_TYPES if (SOURCE / (entry["id"] + t)).exists()]
    if len(found) > 1:
        sys.exit(f"{entry['id']}: several sources ({', '.join(f.name for f in found)}); keep one")
    return found[0] if found else None


def read_source(path):
    """any WAV, MP3, OGG or FLAC -> rate and mono float samples"""
    data, rate = soundfile.read(path, dtype="float64", always_2d=True)
    return rate, data.mean(axis=1)


def cut_loop(x, rate, start, end):
    """the part from start to end seconds, made to loop: its first LOOP_FADE seconds fade in
    while the music just after end fades out, so the wrap from its last sample to its first is continuous"""
    a, b, f = (int(round(v * rate)) for v in (start, end, LOOP_FADE))
    if not 0 <= a < a + f <= b <= len(x) - f:
        raise ValueError(f"loop {start}-{end}s does not fit: the source is {len(x) / rate:.2f}s "
                         f"and needs {LOOP_FADE}s after the end for the crossfade")
    y = x[a:b].copy()
    w = np.linspace(0, np.pi / 2, f)
    y[:f] = x[a:a + f] * np.sin(w) + x[b:b + f] * np.cos(w)   # equal power: no dip in loudness
    return y


def resample(x, rate, target, loop):
    if rate == target:
        return x
    g = math.gcd(target, rate)
    if not loop:
        return resample_poly(x, target // g, rate // g)
    # a loop is resampled between copies of itself so its seam stays smooth
    y = resample_poly(np.tile(x, 3), target // g, rate // g)
    n = len(y) // 3
    return y[n:2 * n]


def finish(x, rate, kind):
    """mono float samples at any rate -> game samples, with levels shared by every sound of the kind"""
    x = resample(np.asarray(x, dtype=np.float64), rate, RATES[kind], kind == "music")
    if kind == "music":
        rms = math.sqrt(np.mean(x ** 2)) or 1
        x = x * MUSIC_RMS / rms
        peak = np.max(np.abs(x))
        return x * (MUSIC_PEAK / peak if peak > MUSIC_PEAK else 1)
    loud = np.nonzero(np.abs(x) > np.max(np.abs(x)) * 0.003)[0]   # trim silence below -50 dB
    if len(loud) == 0:
        raise ValueError("silent")
    x = x[loud[0]:loud[-1] + 1]
    fade = min(len(x) // 2, int(0.003 * RATES[kind]))
    if fade:
        x[:fade] *= np.linspace(0, 1, fade)
        x[-fade:] *= np.linspace(1, 0, fade)
    return x * SFX_PEAK / np.max(np.abs(x))


def write_wav(path, x, rate):
    path.parent.mkdir(parents=True, exist_ok=True)
    soundfile.write(path, np.clip(x, -1, 1), rate, subtype="PCM_16")


# ---------- commands ----------

def pick(ids):
    entries = manifest()
    unknown = set(ids) - {e["id"] for e in entries}
    if unknown:
        sys.exit("not in sounds.json: " + ", ".join(sorted(unknown)))
    return [e for e in entries if not ids or e["id"] in ids]


def cmd_prompts(ids):
    """prints the full prompt of every chosen sound, ready to paste into Suno (music) or a sound effect generator"""
    for e in pick(ids):
        style = MUSIC_STYLE if e["kind"] == "music" else SFX_STYLE
        print(f"{e['id']} ({e['kind']}, {e['use']}):\n  {style}{e['subject']}\n")


def cmd_synth(ids):
    """writes a synthesized source for every chosen sound that has no source yet"""
    for e in pick(ids):
        if source_of(e):
            print(f"{e['id']}: has a source, kept")
            continue
        x = synth(e)
        if x is None:
            print(f"{e['id']}: no recipe; put a source at {(SOURCE / e['id']).relative_to(ROOT)}.wav (or .mp3, .ogg, .flac)")
            continue
        write_wav(SOURCE / f"{e['id']}.wav", x / np.max(np.abs(x)), SYNTH_RATE)
        print(f"{e['id']}: synthesized {len(x) / SYNTH_RATE:.2f}s")


def cmd_convert(ids):
    for e in pick(ids):
        src = source_of(e)
        if not src:
            print(f"{e['id']}: no source, skipped")
            continue
        rate, x = read_source(src)
        if "loop" in e:
            x = cut_loop(x, rate, *e["loop"])
        y = finish(x, rate, e["kind"])
        write_wav(OUT / f"{e['id']}.wav", y, RATES[e["kind"]])
        print(f"{e['id']}: {src.name} -> {len(y) / RATES[e['kind']]:.2f}s, {len(y) * 2 // 1024} KB")


def cmd_check(_ids):
    # every song bar parses and loops to the planned length
    for name, spec in SONGS.items():
        x = song(name)
        assert len(x) == int(round(64 * 30 / spec["bpm"] * SYNTH_RATE)), name
    # a looping sine stays continuous across the seam after resampling
    sine = np.sin(2 * np.pi * 441 * np.arange(SYNTH_RATE) / SYNTH_RATE)
    y = finish(sine, SYNTH_RATE, "music")
    assert len(y) == RATES["music"]
    assert abs(y[0] - y[-1]) < np.max(np.abs(np.diff(y))) * 1.5, "seam jumps"
    assert abs(math.sqrt(np.mean(y ** 2)) - MUSIC_RMS) < 1e-3
    # a cut loop wraps around without a jump: its first sample continues its last
    ramp = np.arange(10 * 100) / 100.0                 # 10 s at 100 Hz, rising 1 per second
    y = cut_loop(ramp, 100, 2, 6)
    assert len(y) == 400 and abs(y[0] - 6.0) < 1e-9 and abs(y[-1] - 5.99) < 1e-9
    assert abs(y[60] - ramp[260]) < 1e-9                # after the crossfade it is the source again
    try:
        cut_loop(ramp, 100, 2, 9.8)
        raise AssertionError("a loop with no room for the crossfade was accepted")
    except ValueError:
        pass
    # effects lose their silence and hit the shared peak
    padded = np.concatenate([np.zeros(1000), sfx_ui_confirm(), np.zeros(1000)])
    y = finish(padded, SYNTH_RATE, "sfx")
    assert abs(np.max(np.abs(y)) - SFX_PEAK) < 1e-9
    assert len(y) < len(sfx_ui_confirm()) / 2 + 10
    # 8-bit stereo and MP3 sources read as mono floats
    buf = io.BytesIO()
    soundfile.write(buf, np.array([[-1.0, 1.0], [0.5, 0.5]]), 8000, format="WAV", subtype="PCM_U8")
    buf.seek(0)
    rate, x = read_source(buf)
    assert rate == 8000 and x.shape == (2,) and abs(x[0]) < 0.01 and abs(x[1] - 0.5) < 0.01
    buf = io.BytesIO()
    soundfile.write(buf, sine[:SYNTH_RATE // 2], SYNTH_RATE, format="MP3")
    buf.seek(0)
    rate, x = read_source(buf)
    assert rate == SYNTH_RATE and abs(len(x) - SYNTH_RATE // 2) < 3000
    # every manifest entry has a recipe, a subject and a converted file
    for e in manifest():
        assert synth_exists(e), f"{e['id']}: no recipe"
        assert e.get("subject"), f"{e['id']}: no subject"
        if not (OUT / f"{e['id']}.wav").exists():
            print(f"warning: {e['id']} has no file in assets/sounds")
    print("check passed")


def synth_exists(e):
    return e["id"] in SONGS if e["kind"] == "music" else "sfx_" + e["id"] in globals()


COMMANDS = {"prompts": cmd_prompts, "synth": cmd_synth, "convert": cmd_convert, "check": cmd_check}

if __name__ == "__main__":
    if len(sys.argv) < 2 or sys.argv[1] not in COMMANDS:
        sys.exit("usage: python tools/audiogen/audiogen.py prompts|synth|convert|check [ids...]")
    COMMANDS[sys.argv[1]](sys.argv[2:])
