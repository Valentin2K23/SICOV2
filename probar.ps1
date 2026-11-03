$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    & ./compilar.ps1
    $salidaPruebasSicov = Join-Path ([System.IO.Path]::GetTempPath()) ('sicov-tests-' + [guid]::NewGuid())
    New-Item -ItemType Directory -Path $salidaPruebasSicov | Out-Null
    $fuentesPruebasSicov = @(Get-ChildItem -LiteralPath 'src' -Recurse -Filter '*.java' | ForEach-Object FullName)
    & javac -encoding UTF-8 -d $salidaPruebasSicov $fuentesPruebasSicov tests/com/sicov/PruebasSicov.java tests/com/sicov/PruebasRecuperacion.java tests/com/sicov/PruebasCategorias.java tests/com/sicov/PruebasInteraccionVenta.java tests/com/sicov/PruebasCantidad.java tests/com/sicov/PruebasCancelaciones.java tests/com/sicov/PruebasAtajosCobro.java tests/com/sicov/PruebasAccesosGranel.java tests/com/sicov/PruebasImagenesProductos.java tests/com/sicov/PruebasIconosProductos.java
    if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación de las pruebas.' }
    & java '-Djava.awt.headless=true' -cp $salidaPruebasSicov com.sicov.PruebasSicov
    if ($LASTEXITCODE -ne 0) { throw 'Falló una prueba.' }
    & java '-Djava.awt.headless=true' -cp $salidaPruebasSicov com.sicov.PruebasRecuperacion
    if ($LASTEXITCODE -ne 0) { throw 'Falló una prueba de recuperación.' }
    & java '-Djava.awt.headless=true' -cp $salidaPruebasSicov com.sicov.PruebasCategorias
    if ($LASTEXITCODE -ne 0) { throw 'Falló una prueba de categorías.' }
    & java '-Djava.awt.headless=true' -cp $salidaPruebasSicov com.sicov.PruebasInteraccionVenta
    if ($LASTEXITCODE -ne 0) { throw 'Falló una prueba de interacción de ventas.' }
    & java '-Djava.awt.headless=true' -cp $salidaPruebasSicov com.sicov.PruebasCantidad
    if ($LASTEXITCODE -ne 0) { throw 'Falló una prueba de cantidades a granel.' }
    & java '-Djava.awt.headless=true' -cp $salidaPruebasSicov com.sicov.PruebasCancelaciones
    if ($LASTEXITCODE -ne 0) { throw 'Falló una prueba de cancelaciones.' }
    & java '-Djava.awt.headless=true' -cp $salidaPruebasSicov com.sicov.PruebasAtajosCobro
    if ($LASTEXITCODE -ne 0) { throw 'Falló una prueba de Enter en cobro y ticket.' }
    & java '-Djava.awt.headless=true' -cp $salidaPruebasSicov com.sicov.PruebasAccesosGranel
    if ($LASTEXITCODE -ne 0) { throw 'Falló una prueba de accesos a granel.' }
    & java '-Djava.awt.headless=true' -cp $salidaPruebasSicov com.sicov.PruebasImagenesProductos
    if ($LASTEXITCODE -ne 0) { throw 'Falló una prueba de imágenes opcionales.' }
    & java '-Djava.awt.headless=true' -cp $salidaPruebasSicov com.sicov.PruebasIconosProductos
    if ($LASTEXITCODE -ne 0) { throw 'Falló una prueba de iconos de productos.' }
} finally { Pop-Location }
