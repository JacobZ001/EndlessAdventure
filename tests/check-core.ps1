$ErrorActionPreference = 'Stop'
$projectDirectory = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$checkDirectory = Join-Path ([IO.Path]::GetTempPath()) ('endless-core-check-' + [guid]::NewGuid())
$classesDirectory = Join-Path $checkDirectory 'classes'
New-Item -ItemType Directory -Path $classesDirectory | Out-Null

$sources = @(Get-ChildItem -LiteralPath (Join-Path $projectDirectory 'src') -Filter '*.java' -Recurse | ForEach-Object FullName)
$sources += Join-Path $PSScriptRoot 'CoreGameCheck.java'
& javac --release 21 -encoding UTF-8 -d $classesDirectory $sources
if ($LASTEXITCODE -ne 0) { throw 'Core check compilation failed' }

Push-Location $checkDirectory
try {
    & java --module-path $classesDirectory --module EndlessAdventure/com.endlessadventure.CoreGameCheck
    if ($LASTEXITCODE -ne 0) { throw 'Core checks failed' }
} finally {
    Pop-Location
}
Write-Output ('Check artifacts: ' + $checkDirectory)
