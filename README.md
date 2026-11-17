# Endless Adventure

A console-based Java RPG course project for CMP 358L at the American University of Sharjah.

Repository: https://github.com/JacobZ001/EndlessAdventure

## Current status

As of November 12, 2026, the minimal playable version (v0) is complete: character creation with starting equipment, an inventory and character screen, the narrative adventure loop, encounters started by the story, grid battles against one or two enemies whose result returns to the story, resting, and saves.

- **Interface:** every screen (main menu, character creation, save/load, adventure, inventory, character, battle, confirmation dialogs) is drawn as one 209×51 frame and can be used with the keyboard or the mouse (click and hover). The game needs Windows Terminal, maximized.
- **Character creation:** name, step-by-step allocation of seven attributes, combat skills and general skills within the starting budgets, a starting-equipment step (main-hand weapon and armor required, off hand optional: shield or a light weapon), then a review step.
- **Equipment and inventory:** main hand, armor and off-hand slots with equip rules (two-handed weapons occupy the off hand, light weapons can be dual-wielded). Equipment determines attack power, off-hand attack power, armor, shield value, speed and whether the character can dodge. The inventory screen equips and unequips items and compares them with what is worn; the character screen shows the full character.
- **Combat:** a 10×7 grid battle on one of two preset maps, with initiative order, 3 action points per turn, split movement with path preview, normal and off-hand attacks, three defenses (simple, shield, dodge), evacuation from the map edge, and the weapon properties versatile, pierce, break guard, lethal and assassinate. Enemies (bandits, up to two per battle) are played by a rule-table AI. Each action shows a preview (expected damage, hit chance against a dodging target) before it is confirmed, and a battle log lists what happened.
- **Narrative gameplay:** LLM-generated opening and continuation scenes with 1–3 suggested choices or a typed custom action (up to two lines). Successful continuation replaces the current scene and records the previous scene with the player's action. Java assigns turn numbers; the model receives the current scene, player facts and the full history. The story moves the player through a world map of locations (settlement, wilderness, ruin) and the rooms inside them; each room shows a backdrop picture chosen from `data/backdrops.json`. When the story places enemies in a scene, a Fight command starts the battle; afterwards the story narrates the result. With no enemies present and HP below maximum, the player can rest: a settlement restores full HP and is never interrupted, the wilderness and ruins restore half and may be interrupted by enemies (then only half of that is restored). After a defeat, the next rest is never interrupted.
- **Saves:** three JSON slots with overwrite and delete confirmation, storing the player (attributes, skills, equipment and items), the current scene with its choices and enemies, the story history, the world map with the current room, and the rest protection after a defeat. Saves from older versions are reported as unsupported.
- **Images and sound:** equipment icons, scene pictures, battle figures and portraits are drawn in the terminal from PNG files in `assets/`; background music changes by screen, and the interface and battles play sound effects. The images and placeholder sounds are produced offline by the tools in `tools/imagegen` and `tools/audiogen` (not part of the game).
- **Data files:** the equipment catalog (`data/weapons.json`, `armor.json`, `shields.json`) and enemy templates (`data/enemies.json`) are read at startup; a missing or invalid entry stops the game with the file name and entry number.

### Known gaps

- Not implemented: experience and leveling, loot, usable skills and checks, potions and special actions, inventory categories and paging.
- Returning to a known room that is not next to the current one creates a second room with the same name.
- History is sent in full with each continuation; there is no summary or context-size limit yet. If the opening scene cannot be generated, the game returns to the main menu with an error; a failed continuation keeps the current scene. If the story after a battle cannot be generated, the scene before the battle stays, so its enemies can be fought again (as new enemies); a failed rest request undoes the healing. An HTTP 503 (Gemini overloaded) is retried twice, after 2 s and 4 s; other failures are not retried, and there is no offline story fallback.

## Requirements

- JDK 21
- Eclipse IDE with Java development support, or another Java development environment

Third-party libraries are included as jars in `lib/` and registered in the Eclipse `.classpath`; no download is needed:

| Library | Version | License | Use |
|---|---|---|---|
| JLine (terminal, reader, terminal-jni, native) | 4.4.6 | BSD-3-Clause | Terminal access, key and mouse input |
| Gson | 2.13.2 | Apache-2.0 | Gemini request/response JSON in `LlmClient`; equipment and enemy catalogs in `data/`; save files |

The game must be launched with `lib` on the module path. Narrative generation requires network access and a Gemini API key; opening the menus does not require a key.

## Run in Eclipse

1. Clone this repository or download and extract it.
2. In Eclipse, select **File → Import → General → Existing Projects into Workspace** and choose the repository folder. When using Git, leave **Copy projects into workspace** unchecked so Eclipse works on the same files as the repository.
3. Make sure the project uses a JDK 21 installation for its **JavaSE-21** environment.
4. Create the local `.env` file described below.
5. Build the project, then launch it in Windows Terminal through **External Tools** as described below. The game needs an interactive terminal; if it cannot open one, it prints a terminal error and exits, so launch it in Windows Terminal rather than the IDE console.

## Run without Eclipse

From the project root in Windows Terminal (maximized), with JDK 21 on the path:

```powershell
javac --release 21 -encoding UTF-8 --module-path lib -d bin (Get-ChildItem -Recurse src -Filter *.java).FullName
java --module-path "bin;lib" --module EndlessAdventure/com.endlessadventure.Driver
```

The first command is only needed after the source changes (Eclipse's build writes the same `bin`). Story requests need the `.env` file described below.

`references/` holds static design reference images (AI-generated prototype renders, not game assets). Game images and sounds are in `assets/`; the game reads `assets/` and `data/` relative to the project root, so launch it from there.

## Local LLM configuration

Create `.env` in the project root (next to `.project` and `README.md`), using `.env.example` as a template:

```dotenv
ENDLESS_ADVENTURE_API_KEY=your_gemini_api_key
# Optional overrides; omit these lines to use the client's defaults.
# ENDLESS_ADVENTURE_MODEL=your_model_id
# ENDLESS_ADVENTURE_API_URL=your_generate_content_url
# Optional debug log; omit to log nothing.
# ENDLESS_ADVENTURE_LLM_LOG=logs/llm.log
```

With `ENDLESS_ADVENTURE_LLM_LOG` set, every story request is appended to that file in full: the system instructions, the context sent as the user message, the model's reply, how the game parsed it (the scene's enemies and whether they changed), and complete error bodies that the game's status line shortens. The API key is never written, and `logs/` is ignored by Git.

`LlmClient` reads this file when it is constructed. `.env` values take precedence over process environment variables; settings absent from the file use process environment variables, followed by the client's existing defaults. An explicitly empty local API key disables LLM requests even if Eclipse supplies a key. Restart the game after editing the file.

Use UTF-8, one `NAME=VALUE` entry per line, with optional single or double quotes. Blank lines and lines beginning with `#` are supported. Values are literal: there is no variable expansion, escape processing, multiline syntax, or inline comment syntax. Put comments on separate lines. Malformed entries or an unreadable file produce a startup error identifying the problem without printing secret values.

Launch the game with the project root as the working directory:

- **Eclipse External Tools:** set Working Directory to `${workspace_loc:/EndlessAdventure}` and retain `wt.exe -d "${workspace_loc:/EndlessAdventure}"` in the terminal launch arguments. The terminal then runs `java --module-path "bin;lib" --module EndlessAdventure/com.endlessadventure.Driver` from that directory. `bin` holds Eclipse's compiled classes and `lib` holds the JLine and Gson jars.

The file is read by Java; it does not change Windows environment settings. `.env` is ignored by Git, while `.env.example` contains no credentials. Starting an adventure or submitting a story action triggers a model request.

### Offline configuration check

This check uses only dummy credentials and a local HTTP server, with output in a fresh temporary directory. Run from the project root in PowerShell:

```powershell
$checkDirectory = Join-Path ([IO.Path]::GetTempPath()) ('endless-llm-check-' + [guid]::NewGuid())
New-Item -ItemType Directory -Path $checkDirectory | Out-Null
$gsonJar = (Resolve-Path lib/gson-2.13.2.jar).Path
javac --add-modules jdk.httpserver -cp $gsonJar -d $checkDirectory src/com/endlessadventure/llm/LlmClient.java src/com/endlessadventure/llm/LlmRequestException.java tests/LlmConfigCheck.java
if ($LASTEXITCODE -ne 0) { throw 'Configuration check compilation failed' }
$previousKey = $env:ENDLESS_ADVENTURE_API_KEY
$previousModel = $env:ENDLESS_ADVENTURE_MODEL
Push-Location $checkDirectory
try {
    $env:ENDLESS_ADVENTURE_API_KEY = 'environment-test-key'
    $env:ENDLESS_ADVENTURE_MODEL = 'environment-test-model'
    java --add-modules jdk.httpserver -cp "$checkDirectory;$gsonJar" com.endlessadventure.llm.LlmConfigCheck
    if ($LASTEXITCODE -ne 0) { throw 'Configuration check failed' }
} finally {
    $env:ENDLESS_ADVENTURE_API_KEY = $previousKey
    $env:ENDLESS_ADVENTURE_MODEL = $previousModel
    Pop-Location
}
```

## Verification status

Core gameplay is checked manually in Windows Terminal; automated core checks can be added when the relevant features and interfaces stabilize. The LLM configuration and story-generation checks below remain.

- The full source compiles with JDK 21 using `lib` as the module path (checked October 30, 2026).
- The offline story-generation check, including the enemy list, after-combat narration and resting, passed on November 12, 2026. The offline configuration check passed on November 12, 2026.
- The battle screen, inventory, character creation with starting equipment, audio, and save/load were tested by the author in Windows Terminal between October 22 and October 31, 2026.
- The offline story-generation check (JSON request and response schema, moves, encounters, interrupted rests) passed on November 16, 2026, and the full source compiles with `lib` as the module path. A scratch save check (not kept in the repository) round-tripped a version 5 save with its map and rejected nine kinds of corrupt files with errors the save manager reports as corrupt (November 16, 2026). The author saved and loaded a game with its world map in Windows Terminal on November 17, 2026.
- Moves with the JSON protocol (staying, a new room, a known room, a new location) were tried with the real model on November 14, 2026; the author's playtest that day confirmed moves.
- The v0 path (encounters with one and two bandits, battle, after-combat story, resting, saving and loading a scene with enemies) was played with the real model by the author in Windows Terminal on November 12, 2026.

## Story generation

`com.endlessadventure.story` contains the abstract `Scene`, `CurrentScene` with choices and present enemies, `PastScene` with a completed action, and `StoryGenerator`. The generator is connected to `GameEngine`. `generateOpening(gameState)` returns turn 1; `generateNext(gameState, playerAction)` returns the next turn using the selected option's text or a custom action. Each method makes one request and returns a validated `CurrentScene` without mutating the supplied state. The engine commits the scene and appends the completed scene/action to history only after successful generation. Generation does not alter player stats or write saves.

The request is the game state as JSON (`history`, `current`, `here` with the location, room and exits, `player`, and `enemyTypes`, `action`, `combat` or `rest` as the request needs). The reply is JSON that follows a response schema built for each kind of request and sent as Gemini structured output (`responseMimeType` `application/json` with `responseJsonSchema`), so required fields and enum values (location kinds, backdrop and enemy IDs) are enforced by the model API; Java validates them again because Gemini does not enforce lengths. The reply holds `description`, an optional `newLocation`, `room`, `enemies` when the scene may hold them, and 1–3 `options`; extra options are discarded. Descriptions, choices and names are truncated to their limits (500, 120, 30 characters; themes 120). Text values are single-line printable ASCII after cleanup. Invalid replies report `LlmRequestException.Stage.INVALID_RESPONSE` without retries or offline story generation. Turn numbers are assigned locally, not supplied by the model. `enemies` is required in continuations with an `EnemyCatalog`: the template IDs present when the scene ends (repeat an ID for several of one kind, `[]` for none, at most `Battle.MAX_ENEMY_COUNT`); unknown IDs or too many reject the reply. Openings, after-combat and after-rest scenes have no enemies field. `CurrentScene.getEnemyIds()` returns them (empty when none). `generateAfterCombat(gameState, outcome)` narrates the settled result of a fight with the current scene's enemies and `combatSummary(...)` gives the action text to record in history; `generateAfterRest(gameState, hpBefore)` and `restSummary(...)` do the same for an undisturbed rest, and `generateAfterInterruptedRest(...)` with `interruptedRestSummary(...)` for a rest that 1–2 required enemies interrupt.

Each method returns a `StoryResult`: the scene and a `Move` (`Stay`, `ToRoom`, `NewRoom`, `NewLocation`). `room` (`name` and `backdrop`) is where the scene ends, written after the description and before the options. With `newLocation` (`name`, `kind`, `theme`, `danger` 1–20) it is the first room of a new location; otherwise its name is matched ignoring case against the current room (stay) and its exits (a known room, whose own backdrop is kept), and any other name is a new room in the current location. Names are matched, not IDs, because a model asked for exit IDs wrote one for an option it offered while staying put. A new room's backdrop is suggested from its location's kind but may be of any kind, so a picture never rejects a scene. The opening must create a new location; rest schemas have no `room`, so rests stay. The generator only checks the move; applying it to the world map is the caller's job.

The offline story check uses dummy credentials and a local HTTP server, without reading `.env` or using personal saves. The command below compiles all source files, so the jars in `lib` must be on its classpath. Run it from the project root in PowerShell:

```powershell
$storyCheckDirectory = Join-Path ([IO.Path]::GetTempPath()) ('endless-story-check-' + [guid]::NewGuid())
New-Item -ItemType Directory -Path $storyCheckDirectory | Out-Null
$storySources = @(Get-ChildItem -LiteralPath src -Filter '*.java' -Recurse | Where-Object Name -ne 'module-info.java' | ForEach-Object FullName)
javac --release 21 -encoding UTF-8 --add-modules jdk.httpserver -cp 'lib/*' -d $storyCheckDirectory $storySources tests/WorldFixture.java tests/StoryGeneratorCheck.java
if ($LASTEXITCODE -ne 0) { throw 'Story check compilation failed' }
java --add-modules jdk.httpserver -cp "$storyCheckDirectory;lib/*" com.endlessadventure.llm.StoryGeneratorCheck
if ($LASTEXITCODE -ne 0) { throw 'Story check failed' }
```

## Course requirement mapping

Status as of November 17, 2026. Paths are relative to `src/com/endlessadventure/`; line numbers refer to the submitted commit. Only code written by the author is cited as evidence; code implemented by the GenAI agent (the LLM module, listed under Development assistance) does not count toward a requirement. Requirements that are not met yet will be covered by a feature that needs them, not added for their own sake.

| Requirement | Status | Where |
|---|---|---|
| At least 8 classes | Met | More than 50 author-written classes across `entity`, `item`, `data`, `combat`, `save`, `audio`, `ui` |
| Fields private or protected; invalid values rejected | Met | `Entity.setHp` (`entity/Entity.java:61`), `Player.setExp` (`entity/Player.java:37`), `Player.PlayerBuilder` (`entity/Player.java:98`), item, equipment and enemy constructors |
| Overloaded constructors, one delegating with `this(...)` | Met | `GameState` (`GameState.java:20`, `:25`, `:42`), `Player` (`entity/Player.java:14`, `:22`), `Button` (`ui/Button.java:12`, `:17`) |
| Meaningful static members | Met | `StatRules` growth and combat rules (`StatRules.java:8`), `SaveManager.SLOT_COUNT` (`save/SaveManager.java:22`) |
| `toString()` overridden in at least 3 classes | Met | `Item` (`item/Item.java:60`) and `Entity` (`entity/Entity.java:103`), their names, used in log lines and error messages; `Position` (`combat/Position.java:9`), `(x, y)`, used in map errors; display names of enums such as `Equipment.Quality` (`item/Equipment.java:85`) and `Weapon.Type` (`item/Weapon.java:110`) |
| `equals()` and `hashCode()` overridden together | Met | `Item`, equal by catalog definition ID (`item/Item.java:43`, `:55`) |
| Inheritance at least two levels deep | Met | `Entity` → `GameCharacter` → `Player` / `Enemy` (`entity/GameCharacter.java:14`, `entity/Player.java:8`, `entity/Enemy.java:9`); `Item` → `Equipment` → `Weapon` (`item/Equipment.java:8`, `item/Weapon.java:10`) |
| Abstract class with at least 2 abstract methods | Met | `Item` (`getCategoryLabel`, `getDetails`: `item/Item.java:36`, `:40`); `Equipment` (`fits`, `addStats`: `item/Equipment.java:22`, `:54`) |
| At least 3 concrete subclasses of that abstract class | Met | `Weapon`, `Armor`, `Shield` (`item/Weapon.java:10`, `item/Armor.java:7`, `item/Shield.java:5`) |
| `@Override` on every override | Met | All current overrides |
| Loop over a supertype collection with type-dependent behavior | Met | `GameCharacter` sums armor, shield value and speed penalty, and checks dodging, over its equipped `Equipment`, each subclass answering differently (`entity/GameCharacter.java:212`–`240`) |
| At least 2 interfaces, one implemented by unrelated classes | Met | `Displayable` (`Displayable.java:6`), implemented by `Item` and `Entity`, which share no parent (`item/Item.java:9`, `entity/Entity.java:8`); `Battle.CombatEvent` (`combat/Battle.java:20`) and `Displayable.Detail` (`Displayable.java:32`), sealed interfaces implemented by records |
| An interface with a default method | Met | `Displayable.getNameMeaning()` (`Displayable.java:91`) |
| Behavior passed as a parameter and invoked later | Met | `Catalog.load` takes a `Function` that builds each entry from a file row (`data/Catalog.java:26`); `BattleMap.searchPaths` takes a `Predicate` for occupied cells (`combat/BattleMap.java:128`); battle actions in `CombatScreen` hold their check and execution as functions (`ui/CombatScreen.java:142`) |
| A class implementing `Comparable`, and a `Comparator` in use | Met | `Position` is `Comparable`, top to bottom, then left to right (`combat/Position.java:15`), the drawing order of the battle map (`ui/CombatScreen.java:914`); `Comparator`: initiative order in `Battle` with `comparingInt`, `reversed`, `thenComparing` (`combat/Battle.java:83`), nearest target in `EnemyAI` (`combat/EnemyAI.java:34`, `:58`) |
| Two custom exceptions, one checked and one unchecked | Met | `BadSaveException`, checked (`save/BadSaveException.java:3`); `MissingFieldException`, unchecked (`data/MissingFieldException.java:3`) |
| Each custom exception carries data beyond the message | Met | Slot number (`save/BadSaveException.java:6`); missing field name (`data/MissingFieldException.java:6`) |
| At least one exception chain preserving the cause | Met | `BadSaveException(slot, cause)` (`save/BadSaveException.java:8`), thrown in `SaveManager` (`save/SaveManager.java:84`, `:105`); `Catalog` wraps parse errors in `IOException` with the cause (`data/Catalog.java:34`–`50`) |
| No empty catch blocks | Met | — |
| `List` and `Map` used where appropriate | Met | Story history (`GameState.java:15`) and battle participants (`combat/Battle.java:58`) as `List`; attributes, skills and equipment slots as `EnumMap` (`entity/GameCharacter.java:15`–`17`); catalog index by ID as `LinkedHashMap` (`data/Catalog.java:18`) |
| Reading a file with try-with-resources; missing or malformed files handled | Met | `SaveManager.loadSave` (`save/SaveManager.java:98`; missing file = empty slot, unreadable file = corrupt slot, `:53`–`56`); `Catalog.load` (`data/Catalog.java:28`; missing or malformed data stops startup with the file name and entry) |
| Writing a file | Met | `SaveManager.writeSave` (`save/SaveManager.java:72`): temporary file, then atomic replace |
| A design pattern from Lab 9, used deliberately | Met | Factory: `EnemyCatalog.createFromId` (`data/EnemyCatalog.java:43`, called at `GameEngine.java:260`) builds a fresh `Enemy` from a template for every battle, since enemies carry their own HP and names (confirmed with the instructor, November 10). Also `Player.PlayerBuilder` (Builder, `entity/Player.java:98`) and `Equipment.getDetails` (template method, `item/Equipment.java:37`) |
| Composition chosen over inheritance at least once | Met | `GameCharacter` holds its equipment in slots instead of subclasses per loadout (`entity/GameCharacter.java:17`); `Combatant` wraps a `GameCharacter` with battle-only state (`combat/Combatant.java:34`) |
| A class depending only on abstractions | Met | `Catalog.load` (`data/Catalog.java:26`) reads any row type and builds entries through a `Function`, without knowing the concrete catalog (confirmed with the instructor, November 10) |

## Design pattern

**Factory** (Lab 9): `EnemyCatalog.createFromId` (`src/com/endlessadventure/data/EnemyCatalog.java:43`), called when a battle starts (`src/com/endlessadventure/GameEngine.java:260`).

- **What it solved:** a battle needs its own `Enemy` objects, because each enemy carries its own HP and name while it fights. The story only names enemy template IDs, so callers ask the catalog for an enemy by ID and never construct `Enemy` themselves or look up its equipment. Each call builds a fresh enemy from the template, so two bandits in one battle, or the same bandit in a later battle, never share state.
- **Also present:** `Player.PlayerBuilder` (Builder, `src/com/endlessadventure/entity/Player.java:98`) collects the points spent during character creation and only builds a `Player` when every point is spent; `Equipment.getDetails` (template method, `src/com/endlessadventure/item/Equipment.java:37`) fixes the order of the detail lines and lets each subclass add its own stats.

## Development assistance

### Implemented by a GenAI coding agent

The LLM module was implemented by a GenAI coding agent:

- `com.endlessadventure.llm.LlmClient.java` (Gemini requests, configuration and the optional debug log)
- `com.endlessadventure.llm.LlmRequestException.java`
- `com.endlessadventure.story.StoryGenerator.java` (model requests, prompts, response parsing and validation: the enemy list, after-combat and after-rest narration)
- `tests/LlmConfigCheck.java` and `tests/StoryGeneratorCheck.java` (offline checks of the LLM module)

### Other assistance

A GenAI coding agent also helped outside the course content (the offline image and audio tools in `tools/`, image drawing and shading in the terminal, and parts of the screen layout), and it explained, reviewed and in places typed code that the author designed and fully understands. The game logic (characters, items, combat, data loading and saves) and its structure are the author's. Some of the author's commits carry a `Co-Authored-By` trailer because a local agent session made the commit.
