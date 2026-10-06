# Endless Adventure

A console-based Java RPG course project for CMP 358L at the American University of Sharjah.

## Current status

As of October 6, 2026, the narrative adventure loop is implemented in source.

- **Narrative gameplay:** character creation, LLM-generated opening and continuation scenes, 1–3 suggested choices, and custom text actions. Successful continuation replaces the current scene and records the previous scene with the player's action. Java assigns scene turn numbers; the model receives the current scene, player facts, and accumulated history.
- **Player and persistence foundation:** validated player attributes and an EXP table, plus three save slots with overwrite confirmation. Save code includes player attributes, current scene, choices, and turn, but does not yet persist story history; loading still needs the constructor integration noted above.
- **Console presentation:** a 120-column colored text interface with wrapping and screen redraws. Input is line-based and requires Enter. Use a terminal wide enough for the interface.
- **Not implemented:** playable combat, inventory and equipment, usable skills, mechanical rewards, and automatic leveling. Story generation is currently narrative-only; mentioning an item or enemy does not create a gameplay object.

### Known integration gaps

Adventure input currently treats every nonblank value other than a matching choice number as a custom action. This also captures `S` and `B` before save/back routing, so those advertised commands do not currently open their intended screens. Custom actions are also lowercased by the shared input method.

History is kept in memory and sent in full with each continuation; there is no persisted summary or context-size limit yet. Save writes directly replace the target file without an atomic temporary-file step. Model failures return to the main menu with an error; there is no automatic retry, offline story fallback, or main-menu Continue option.

## Requirements

- JDK 21
- Eclipse IDE with Java development support, or another Java development environment

The current source uses only the Java standard library. Narrative generation requires network access and a Gemini API key; opening the menus does not require a key. Fix the compilation blocker above before launching the game.

## Run in Eclipse

1. Clone this repository or download and extract it.
2. In Eclipse, select **File → Import → General → Existing Projects into Workspace** and choose the repository folder. When using Git, leave **Copy projects into workspace** unchecked so Eclipse works on the same files as the repository.
3. Make sure the project uses a JDK 21 installation for its **JavaSE-21** environment.
4. Create the local `.env` file described below and set the run configuration's working directory to the repository root.
5. Once the project builds successfully, open `src/com/endlessadventure/Driver.java`, then select **Run As → Java Application**.

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

Both launch methods must use the project root as the working directory:

- **Eclipse Java Application:** Run Configurations → Arguments → Working directory → Other: `${workspace_loc:/EndlessAdventure}`.
- **Eclipse External Tools:** set Working Directory to `${workspace_loc:/EndlessAdventure}` and retain `wt.exe -d "${workspace_loc:/EndlessAdventure}"` in the terminal launch arguments. The terminal then runs `java --module-path bin --module EndlessAdventure/com.endlessadventure.Driver` from that directory.

The file is read by Java; it does not change Windows environment settings. `.env` is ignored by Git, while `.env.example` contains no credentials. Starting an adventure or submitting a story action triggers a model request.

### Offline configuration check

This check uses only dummy credentials and a local HTTP server, with output in a fresh temporary directory. Run from the project root in PowerShell:

```powershell
$checkDirectory = Join-Path ([IO.Path]::GetTempPath()) ('endless-llm-check-' + [guid]::NewGuid())
New-Item -ItemType Directory -Path $checkDirectory | Out-Null
javac --add-modules jdk.httpserver -d $checkDirectory src/com/endlessadventure/llm/LlmClient.java src/com/endlessadventure/llm/LlmRequestException.java tests/LlmConfigCheck.java
if ($LASTEXITCODE -ne 0) { throw 'Configuration check compilation failed' }
$previousKey = $env:ENDLESS_ADVENTURE_API_KEY
$previousModel = $env:ENDLESS_ADVENTURE_MODEL
Push-Location $checkDirectory
try {
    $env:ENDLESS_ADVENTURE_API_KEY = 'environment-test-key'
    $env:ENDLESS_ADVENTURE_MODEL = 'environment-test-model'
    java --add-modules jdk.httpserver -cp $checkDirectory com.endlessadventure.llm.LlmConfigCheck
    if ($LASTEXITCODE -ne 0) { throw 'Configuration check failed' }
} finally {
    $env:ENDLESS_ADVENTURE_API_KEY = $previousKey
    $env:ENDLESS_ADVENTURE_MODEL = $previousModel
    Pop-Location
}
```

## Verification status

The outdated core regression check and its launcher have been removed. LLM configuration and story-generation checks remain. Core gameplay is currently checked manually; automated core checks can be added when the relevant features and interfaces stabilize.

The earlier full-source compilation check on October 6, 2026 failed at the `SaveManager` constructor call described above. Removing the core tests does not resolve that source error; compilation and real-model gameplay were not rechecked during this cleanup.

## Story generation

`com.endlessadventure.story` contains the abstract `Scene`, `CurrentScene` with choices, `SceneRecord` with a completed action, and `StoryGenerator`. The generator is connected to `GameEngine`. `generateOpening(gameState)` returns turn 1; `generateNext(gameState, playerAction)` returns the next turn using the selected option's text or a custom action. Each method makes one request and returns a validated `CurrentScene` without mutating the supplied state. The engine commits the scene and appends the completed scene/action to history only after successful generation. Generation does not alter player stats or write saves.

The body protocol uses `protocol.version=2`, `location`, `description`, `option.count`, and consecutive zero-based `option.N.text` fields. The game retains 1–3 choices; extra supplied choices are validated before being discarded. Location length is capped at 30 characters; descriptions and choices are truncated to 500 and 120 characters respectively. Text values are single-line printable ASCII after cleanup and preserve literal equals signs and backslashes. Invalid required fields report `LlmRequestException.Stage.INVALID_RESPONSE` without retries or offline story generation. Turn numbers are assigned locally, not supplied by the model.

The offline story check uses dummy credentials and a local HTTP server, without reading `.env` or using personal saves. The command below compiles all source files, so it is currently blocked by the full-source compilation error. After that integration issue is resolved, run it from the project root in PowerShell:

```powershell
$storyCheckDirectory = Join-Path ([IO.Path]::GetTempPath()) ('endless-story-check-' + [guid]::NewGuid())
New-Item -ItemType Directory -Path $storyCheckDirectory | Out-Null
$storySources = @(Get-ChildItem -LiteralPath src -Filter '*.java' -Recurse | Where-Object Name -ne 'module-info.java' | ForEach-Object FullName)
javac --release 21 -encoding UTF-8 --add-modules jdk.httpserver -d $storyCheckDirectory $storySources tests/StoryGeneratorCheck.java
if ($LASTEXITCODE -ne 0) { throw 'Story check compilation failed' }
java --add-modules jdk.httpserver -cp $storyCheckDirectory com.endlessadventure.llm.StoryGeneratorCheck
if ($LASTEXITCODE -ne 0) { throw 'Story check failed' }
```

## Development assistance

The following LLM classes and verification code were developed with a GenAI coding agent:

- com.endlessadventure.llm.LlmClient.java
- com.endlessadventure.llm.LlmRequestException.java
- tests/LlmConfigCheck.java (LLM configuration verification only)
- com.endlessadventure.story.StoryGenerator.java
- tests/StoryGeneratorCheck.java (offline story-generation verification)

The core game code were written by the author.
