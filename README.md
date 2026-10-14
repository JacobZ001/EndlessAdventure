# Endless Adventure

A console-based Java RPG course project for CMP 358L at the American University of Sharjah.

## Current status

As of October 11, 2026, the narrative adventure loop is implemented in source.

- **Narrative gameplay:** name-only character creation, LLM-generated opening and continuation scenes, 1–3 suggested choices, and custom text actions. Successful continuation replaces the current scene and records the previous scene with the player's action. Java assigns scene turn numbers; the model receives the current scene, player facts, and accumulated history.
- **Player and persistence foundation:** validated player attributes and an EXP table, plus three save slots with overwrite and delete confirmation. Saves include player attributes, the current scene and choices, and the full story history; loading restores turn numbers from the history.
- **Console presentation:** a 204-column colored text interface with wrapping and screen redraws. Input is read line by line through JLine and still requires Enter. The game needs an interactive terminal (Windows Terminal, maximized).
- **Not implemented:** playable combat, inventory and equipment, usable skills, mechanical rewards, and automatic leveling. Story generation is currently narrative-only; mentioning an item or enemy does not create a gameplay object.

### Known integration gaps

Adventure input checks `S` (save) and `B` (back) first, then maps choice numbers to their text; any other nonblank input is sent as a custom action. Custom actions are lowercased by the shared input method. The help screen mentions `back`, but only `b` is accepted.

History is kept in memory and sent in full with each continuation; there is no persisted summary or context-size limit yet. Save writes directly replace the target file without an atomic temporary-file step. If the opening scene cannot be generated, the game returns to the main menu with an error; a failed continuation keeps the current scene and history. There is no automatic retry, offline story fallback, or main-menu Continue option.

## Requirements

- JDK 21
- Eclipse IDE with Java development support, or another Java development environment

Third-party libraries are included as jars in `lib/` and registered in the Eclipse `.classpath`; no download is needed:

| Library | Version | License | Use |
|---|---|---|---|
| JLine (terminal, reader, terminal-jni, native) | 4.4.6 | BSD-3-Clause | Terminal access and line input |
| Gson | 2.13.2 | Apache-2.0 | Gemini request/response JSON in `LlmClient`; planned for preset game data |

The game must be launched with `lib` on the module path. Narrative generation requires network access and a Gemini API key; opening the menus does not require a key.

## Run in Eclipse

1. Clone this repository or download and extract it.
2. In Eclipse, select **File → Import → General → Existing Projects into Workspace** and choose the repository folder. When using Git, leave **Copy projects into workspace** unchecked so Eclipse works on the same files as the repository.
3. Make sure the project uses a JDK 21 installation for its **JavaSE-21** environment.
4. Create the local `.env` file described below.
5. Build the project, then launch it in Windows Terminal through **External Tools** as described below. The game needs an interactive terminal; if it cannot open one, it prints a terminal error and exits, so launch it in Windows Terminal rather than the IDE console.

The final course submission will include updated run instructions, the requirement-to-code mapping, and any known limitations.

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

The outdated core regression check and its launcher have been removed. LLM configuration and story-generation checks remain. Core gameplay is currently checked manually; automated core checks can be added when the relevant features and interfaces stabilize.

On October 14, 2026, the full source compiled with JDK 21 using `lib` as the module path, and both offline checks below passed on Windows with the listed commands; the story check also passed on the module path. Real-model gameplay and the interactive terminal were not rechecked.

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

## Development assistance

The following LLM classes and verification code were developed with a GenAI coding agent:

- com.endlessadventure.llm.LlmClient.java
- com.endlessadventure.llm.LlmRequestException.java
- tests/LlmConfigCheck.java (LLM configuration verification only)
- com.endlessadventure.story.StoryGenerator.java
- tests/StoryGeneratorCheck.java (offline story-generation verification)

A GenAI coding agent also helped with the following at the author's request:

- the JLine and Gson dependency setup (`lib/jline-*.jar` and `lib/gson-2.13.2.jar`, their `.classpath` entries, and the matching `requires` lines in `module-info.java`)
- `Canvas.java` and the canvas-based main menu in `UIHandler.java`
- `Palette.java` (shared colors) and its use in `UIHandler.java` and `Canvas.java`
- drafting `DESIGN.md` from the author's design decisions

The core game code is written by the author.
