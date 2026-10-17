# Endless Adventure

A console-based Java RPG course project for CMP 358L at the American University of Sharjah.

## Current status

As of October 17, 2026, the narrative adventure loop and all menus run on a full-screen terminal interface.

- **Interface:** every screen (main menu, character creation, save/load, adventure, confirmation dialogs) is drawn as one 209×51 frame and can be used with the keyboard or the mouse (click and hover). The game needs Windows Terminal, maximized.
- **Character creation:** name, then step-by-step allocation of seven attributes, combat skills and general skills within the starting budgets, then a review step.
- **Narrative gameplay:** LLM-generated opening and continuation scenes with 1–3 suggested choices or a typed custom action (up to two lines). Successful continuation replaces the current scene and records the previous scene with the player's action. Java assigns turn numbers; the model receives the current scene, player facts and the full history.
- **Saves:** three slots with overwrite and delete confirmation, storing the player, the current scene and choices, and the story history.
- **Not implemented:** combat, inventory and equipment, usable skills, mechanical rewards and leveling. Story generation is narrative-only; mentioning an item or enemy does not create a gameplay object.

### Known gaps

History is sent in full with each continuation; there is no summary or context-size limit yet. Saves do not yet store attributes and skills, and a save write replaces the target file directly, without an atomic temporary-file step. If the opening scene cannot be generated, the game returns to the main menu with an error; a failed continuation keeps the current scene. There is no automatic retry or offline story fallback.

## Requirements

- JDK 21
- Eclipse IDE with Java development support, or another Java development environment

Third-party libraries are included as jars in `lib/` and registered in the Eclipse `.classpath`; no download is needed:

| Library | Version | License | Use |
|---|---|---|---|
| JLine (terminal, reader, terminal-jni, native) | 4.4.6 | BSD-3-Clause | Terminal access, key and mouse input |
| Gson | 2.13.2 | Apache-2.0 | Gemini request/response JSON in `LlmClient`; planned for preset game data |

The game must be launched with `lib` on the module path. Narrative generation requires network access and a Gemini API key; opening the menus does not require a key.

## Run in Eclipse

1. Clone this repository or download and extract it.
2. In Eclipse, select **File → Import → General → Existing Projects into Workspace** and choose the repository folder. When using Git, leave **Copy projects into workspace** unchecked so Eclipse works on the same files as the repository.
3. Make sure the project uses a JDK 21 installation for its **JavaSE-21** environment.
4. Create the local `.env` file described below.
5. Build the project, then launch it in Windows Terminal through **External Tools** as described below. The game needs an interactive terminal; if it cannot open one, it prints a terminal error and exits, so launch it in Windows Terminal rather than the IDE console.

The final course submission will include updated run instructions and any known limitations; the requirement mapping is below.

`references/` holds static design reference images (AI-generated prototype renders, not game assets).

## Local LLM configuration

Create `.env` in the project root (next to `.project` and `README.md`), using `.env.example` as a template:

```dotenv
ENDLESS_ADVENTURE_API_KEY=your_gemini_api_key
# Optional overrides; omit these lines to use the client's defaults.
# ENDLESS_ADVENTURE_MODEL=your_model_id
# ENDLESS_ADVENTURE_API_URL=your_generate_content_url
```

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

On October 17, 2026, the full source compiled with JDK 21 using `lib` as the module path. The two offline checks last passed on October 14, 2026 and were not rerun after the interface changes; real-model gameplay was not rechecked.

## Story generation

`com.endlessadventure.story` contains the abstract `Scene`, `CurrentScene` with choices, `SceneRecord` with a completed action, and `StoryGenerator`. The generator is connected to `GameEngine`. `generateOpening(gameState)` returns turn 1; `generateNext(gameState, playerAction)` returns the next turn using the selected option's text or a custom action. Each method makes one request and returns a validated `CurrentScene` without mutating the supplied state. The engine commits the scene and appends the completed scene/action to history only after successful generation. Generation does not alter player stats or write saves.

The body protocol uses `location`, `description`, `option.count`, and consecutive zero-based `option.N.text` fields. The game retains 1–3 choices; extra supplied choices are validated before being discarded. Location length is capped at 30 characters; descriptions and choices are truncated to 500 and 120 characters respectively. Text values are single-line printable ASCII after cleanup and preserve literal equals signs and backslashes. Invalid required fields report `LlmRequestException.Stage.INVALID_RESPONSE` without retries or offline story generation. Turn numbers are assigned locally, not supplied by the model.

The offline story check uses dummy credentials and a local HTTP server, without reading `.env` or using personal saves. The command below compiles all source files, so the jars in `lib` must be on its classpath. Run it from the project root in PowerShell:

```powershell
$storyCheckDirectory = Join-Path ([IO.Path]::GetTempPath()) ('endless-story-check-' + [guid]::NewGuid())
New-Item -ItemType Directory -Path $storyCheckDirectory | Out-Null
$storySources = @(Get-ChildItem -LiteralPath src -Filter '*.java' -Recurse | Where-Object Name -ne 'module-info.java' | ForEach-Object FullName)
javac --release 21 -encoding UTF-8 --add-modules jdk.httpserver -cp 'lib/*' -d $storyCheckDirectory $storySources tests/StoryGeneratorCheck.java
if ($LASTEXITCODE -ne 0) { throw 'Story check compilation failed' }
java --add-modules jdk.httpserver -cp "$storyCheckDirectory;lib/*" com.endlessadventure.llm.StoryGeneratorCheck
if ($LASTEXITCODE -ne 0) { throw 'Story check failed' }
```

## Course requirement mapping

Status as of October 17, 2026. "Planned" items are designed into the equipment and combat systems that are being built; file and line references will be added for the final submission.

| Requirement | Status | Where |
|---|---|---|
| At least 8 classes | Met | 30+ classes across `entity`, `story`, `save`, `llm`, `ui` |
| Fields private or protected; invalid values rejected | Met | Setters and constructors validate, e.g. `Entity.setHp`, `Player.setExp`, `Player.Builder` |
| Overloaded constructors, one delegating with `this(...)` | Met | `GameState`, `Player`, `Button` |
| Meaningful static members | Met | `StatRules` (growth rules), `SaveManager.SLOT_COUNT`, `Canvas.wrapText` |
| `toString()` overridden in at least 3 classes | Partly met | `Canvas` (ANSI frame). Planned: `Item` and `Entity` (names in combat log lines), combat events (log text) |
| `equals()` and `hashCode()` overridden together | Planned | `Item`, equal by catalog definition ID |
| Inheritance at least two levels deep | Met | `Entity` → `GameCharacter` → `Player` |
| Abstract class with at least 2 abstract methods | Planned | `Item` (`getCategory`, `getDetails`); `Equipment` (`getSlot`, `applyTo`) |
| At least 3 concrete subclasses of that abstract class | Planned | `Weapon`, `Armor`, `Shield` (via `Equipment`) |
| `@Override` on every override | Met | All current overrides |
| Loop over a supertype collection with type-dependent behavior | Planned | `Player` applies each equipped `Equipment` to its combat stats; the combat action bar loops over `CombatAction` |
| At least 2 interfaces, one implemented by unrelated classes | Planned | `Describable` (items and characters, which share no parent); `CombatAction` |
| An interface with a default method | Planned | `Describable.getDetails()`, `CombatAction.needsTarget()` |
| Behavior passed as a parameter and invoked later | Planned | Enemy turn takes a `Consumer` callback that the combat screen uses to play each step |
| A class implementing `Comparable`, and a `Comparator` in use | Planned | Initiative order (`Comparable`); equipment lists sorted with a `Comparator` |
| Two custom exceptions, one checked and one unchecked | Met | `BadSaveException` (checked), `PropertyNotFoundException` (unchecked) |
| Each custom exception carries data beyond the message | Met | Slot number; missing property name |
| At least one exception chain preserving the cause | Met | `BadSaveException(slot, cause)` in `SaveManager` |
| No empty catch blocks | Met | — |
| `List` and `Map` used where appropriate | Met | Story history (`List`); attributes and skills (`EnumMap`); planned catalog index by ID (`Map`) |
| Reading a file with try-with-resources; missing or malformed files handled | Met | `SaveManager` (missing file = empty slot, unreadable file = corrupt slot); planned equipment catalog |
| Writing a file | Met | `SaveManager.writeSave` |
| A design pattern from Lab 9, used deliberately | Pending | To be chosen once Lab 9 is covered; `Player.Builder` already follows the Builder pattern |
| Composition chosen over inheritance at least once | Met | `Player` holds an `Inventory`; planned equipment slots instead of subclasses per loadout |
| A class depending only on abstractions | Planned | The combat engine depends only on `CombatAction` and unit abstractions; concrete units and actions are created by `GameEngine` |

## Development assistance

### Implemented by a GenAI coding agent

The LLM module was implemented by a GenAI coding agent:

- `com.endlessadventure.llm.LlmClient.java`
- `com.endlessadventure.llm.LlmRequestException.java`
- `com.endlessadventure.story.StoryGenerator.java` (model requests, prompts, response parsing and validation)
- `tests/LlmConfigCheck.java` (LLM configuration verification only)
- `tests/StoryGeneratorCheck.java` (offline story-generation verification)

At the author's request, the agent also:

- set up the JLine and Gson dependencies (`lib/jline-*.jar` and `lib/gson-2.13.2.jar`, their `.classpath` entries, and the matching `requires` lines in `module-info.java`)
- wrote the first versions of `Canvas.java` (frame buffer and drawing) and `Palette.java` (shared colors)
- drafted `DESIGN.md` and its module files in `design/` from the author's design decisions

### Guided by a GenAI agent

For the following parts, the agent proposed screen designs and interaction rules, explained implementation approaches and reviewed the code; the author wrote the code:

- terminal key and mouse input in `UIHandler.java` (key bindings, click areas, hover)
- the main menu, character creation, save/load and adventure screens, and the confirmation dialog
- the shared screen structure: the `Screen` base class, list focus handling, `TextField` and `Button`

The core game code is written by the author.
