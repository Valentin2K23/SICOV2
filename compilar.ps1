$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
$salidaCompilacionSicov = Join-Path ([System.IO.Path]::GetTempPath()) ('sicov-build-' + [guid]::NewGuid())
try {
    New-Item -ItemType Directory -Force -Path 'bin','dist' | Out-Null
    New-Item -ItemType Directory -Path $salidaCompilacionSicov | Out-Null
    $fuentesSicov = @(Get-ChildItem -LiteralPath 'src' -Recurse -Filter '*.java' | ForEach-Object FullName)
    & javac -encoding UTF-8 --release 23 -d $salidaCompilacionSicov $fuentesSicov
    if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación de SICOV.' }
    Copy-Item -LiteralPath 'src/recursos' -Destination $salidaCompilacionSicov -Recurse -Force
    $dependenciasFaltantesSicov = @(& jdeps --missing-deps --recursive $salidaCompilacionSicov)
    if ($LASTEXITCODE -ne 0 -or $dependenciasFaltantesSicov.Count -gt 0) {
        throw ('La compilación tiene clases faltantes: ' + ($dependenciasFaltantesSicov -join [Environment]::NewLine))
    }
    & jar --create --file dist/SICOV.jar --main-class com.sicov.App -C $salidaCompilacionSicov .
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo generar el JAR.' }
    Get-ChildItem -LiteralPath $salidaCompilacionSicov | Copy-Item -Destination 'bin' -Recurse -Force
    Write-Output 'Compilación correcta: dist/SICOV.jar'
} finally {
    if (Test-Path -LiteralPath $salidaCompilacionSicov) { Remove-Item -LiteralPath $salidaCompilacionSicov -Recurse -Force }
    Pop-Location
}
