$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    if (-not (Test-Path -LiteralPath 'dist/SICOV.jar')) { & ./compilar.ps1 }
    & java -jar dist/SICOV.jar
} finally { Pop-Location }
