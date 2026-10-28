$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    & ./compilar.ps1
    $salidaPruebasSicov = Join-Path ([System.IO.Path]::GetTempPath()) ('sicov-tests-' + [guid]::NewGuid())
    New-Item -ItemType Directory -Path $salidaPruebasSicov | Out-Null
    $fuentesPruebasSicov = @(Get-ChildItem -LiteralPath 'src' -Recurse -Filter '*.java' | ForEach-Object FullName)
    & javac -encoding UTF-8 -d $salidaPruebasSicov $fuentesPruebasSicov tests/com/sicov/PruebasSicov.java tests/com/sicov/PruebasRecuperacion.java tests/com/sicov/PruebasCategorias.java
    if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación de las pruebas.' }
    & java '-Djava.awt.headless=true' -cp $salidaPruebasSicov com.sicov.PruebasSicov
    if ($LASTEXITCODE -ne 0) { throw 'Falló una prueba.' }
    & java '-Djava.awt.headless=true' -cp $salidaPruebasSicov com.sicov.PruebasRecuperacion
    if ($LASTEXITCODE -ne 0) { throw 'Falló una prueba de recuperación.' }
    & java '-Djava.awt.headless=true' -cp $salidaPruebasSicov com.sicov.PruebasCategorias
    if ($LASTEXITCODE -ne 0) { throw 'Falló una prueba de categorías.' }
} finally { Pop-Location }
