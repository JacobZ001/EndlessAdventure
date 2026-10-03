# Endless Adventure

A console-based Java RPG course project for CMP 358L at the American University of Sharjah.

## Current status

Development has just started. The planned game combines turn-based combat, character growth, items and equipment, saved progress, and LLM-generated story developments within supported game rules.

## Requirements

- JDK 21
- Eclipse IDE with Java development support, or another Java development environment

The project currently uses only the Java standard library. No API key is needed to run the current version.

## Run in Eclipse

1. Clone this repository or download and extract it.
2. In Eclipse, select **File → Import → General → Existing Projects into Workspace** and choose the repository folder. When using Git, leave **Copy projects into workspace** unchecked so Eclipse works on the same files as the repository.
3. Make sure the project uses a JDK 21 installation for its **JavaSE-21** environment.
4. Open `src/com/endlessadventure/Driver.java`, then select **Run As → Java Application**

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

The file is read by Java; it does not change Windows environment settings. `.env` is ignored by Git, while `.env.example` contains no credentials. LLM requests require a real key; this change does not call Gemini automatically.

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

## Core regression check

Run `.\tests\check-core.ps1` from PowerShell. It compiles the project with JDK 21 and checks save/load, corrupt saves, failed writes, overwrite confirmation, navigation, input validation, and attribute bounds. Test saves and compiled classes stay in a fresh temporary directory; the check does not use personal saves or call the LLM.

Adventure scenes, combat, rewards, and automatic leveling are still pending; these checks cover the current menu and player-state implementation.

## Story generation prototype

`com.endlessadventure.Story` contains `Scene` and `StoryGenerator`. Construct the generator with an existing `LlmClient`, then call `generateOpening(gameState)` or `generateNext(gameState, playerAction)`. Pass the chosen option's text, not its number. Each method makes one request and returns a validated new `Scene`; the caller commits it with `gameState.setScene(scene)`. Generation does not change player stats, advance turns, or write saves. These classes are not yet connected to the adventure screen, and scene persistence is not implemented.

The body protocol uses `protocol.version=1`, `location`, `description`, `option.count`, and zero-based `option.0.text` through `option.3.text`. Text values are single-line and preserve literal equals signs and backslashes. Invalid required fields report `LlmRequestException.Stage.INVALID_RESPONSE` without retries or offline story generation.

Run the offline story check from the project root in PowerShell. It uses dummy credentials and a local HTTP server, without reading `.env` or using personal saves:

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

The following LLM classes and verification code are developed using GenAI coding agent:
- com.endlessadventure.llm.LlmClient.java
- com.endlessadventure.llm.LlmRequestException.java
- tests/LlmConfigCheck.java (LLM configuration verification only)
- com.endlessadventure.Story.StoryGenerator.java
- tests/StoryGeneratorCheck.java (offline story-generation verification)

The core game code was initially written by the author. On 2026-10-03, AI-assisted bug fixes were applied to `GameEngine`, `SaveManager`, `UIHandler`, `Entity`, `Combatant`, and `Player`. `tests/CoreGameCheck.java` and `tests/check-core.ps1` were also added with AI assistance to verify those fixes.
