$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    New-Item -ItemType Directory -Force -Path 'bin','dist' | Out-Null
    $fuentesSicov = @(Get-ChildItem -LiteralPath 'src' -Recurse -Filter '*.java' | ForEach-Object FullName)
    & javac -encoding UTF-8 --release 23 -d bin $fuentesSicov
    if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación de SICOV.' }
    Copy-Item -LiteralPath 'src/recursos' -Destination 'bin' -Recurse -Force
    & jar --create --file dist/SICOV.jar --main-class com.sicov.App -C bin .
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo generar el JAR.' }
    Write-Output 'Compilación correcta: dist/SICOV.jar'
} finally { Pop-Location }
