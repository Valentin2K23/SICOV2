$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    & ./compilar.ps1
    $salidaPruebasSicov = Join-Path ([System.IO.Path]::GetTempPath()) ('sicov-tests-' + [guid]::NewGuid())
    New-Item -ItemType Directory -Path $salidaPruebasSicov | Out-Null
    # Prueba las mismas clases que ejecuta el IDE, sin recompilar src en otra carpeta.
    $fuentesPruebasSicov = @(Get-ChildItem -LiteralPath 'tests' -Recurse -Filter 'Pruebas*.java')
    & javac -encoding UTF-8 --release 23 -cp bin -d $salidaPruebasSicov @($fuentesPruebasSicov | ForEach-Object FullName)
    if ($LASTEXITCODE -ne 0) {
        throw 'Falló la compilación de las pruebas.'
    }
    $rutaPruebasSicov = 'bin' + [System.IO.Path]::PathSeparator + $salidaPruebasSicov
    foreach ($fuenteSicov in $fuentesPruebasSicov) {
        & java '-Djava.awt.headless=true' -cp $rutaPruebasSicov ('com.sicov.' + $fuenteSicov.BaseName)
        if ($LASTEXITCODE -ne 0) {
            throw ('Falló la prueba ' + $fuenteSicov.BaseName + '.')
        }
    }
} finally {
    Pop-Location
}
