#Requires -Version 5.1
<#
.SYNOPSIS
  Extrae fotogramas WebP de un vídeo para la secuencia de scroll.

.DESCRIPTION
  Usa ffmpeg del sistema. Genera frame_0001.webp, frame_0002.webp…
  a 1600 px de ancho y calidad 70. No descarga ffmpeg ni añade
  dependencias al proyecto.

.PARAMETER Video
  Ruta del vídeo de origen.

.PARAMETER OutputDir
  Carpeta de salida. Se crea si no existe. Los frame_*.webp
  anteriores de esa carpeta se sustituyen.

.PARAMETER FrameCount
  Cantidad de fotogramas. Por defecto, 120.

.EXAMPLE
  .\scripts\extract-frames.ps1 -Video .\clips\taller.mp4 -OutputDir .\public\images\taller\sequence

.EXAMPLE
  .\scripts\extract-frames.ps1 -Video .\clips\taller.mp4 -OutputDir .\public\images\taller\sequence -FrameCount 90
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$Video,

    [Parameter(Mandatory = $true)]
    [string]$OutputDir,

    [Parameter()]
    [int]$FrameCount = 120
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Continue"

function Test-CommandAvailable {
    param([Parameter(Mandatory = $true)][string]$Name)
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

function Fail {
    param([Parameter(Mandatory = $true)][string]$Message)
    [Console]::Error.WriteLine($Message)
    exit 1
}

if ($FrameCount -lt 1) {
    Fail "FrameCount tiene que ser al menos 1. Recibido: $FrameCount"
}

if (-not (Test-CommandAvailable "ffmpeg")) {
    Fail @"
ffmpeg no está instalado o no está en el PATH.
La secuencia de scroll lo necesita para extraer los fotogramas; este script no lo instala.
Instálalo, abre una terminal nueva y vuelve a ejecutarlo.
En Windows: winget install Gyan.FFmpeg
También vale cualquier build que incluya el ejecutable ffmpeg y el codificador libwebp.
"@
}

if (-not (Test-CommandAvailable "ffprobe")) {
    Fail @"
ffprobe no está en el PATH.
Suele instalarse junto a ffmpeg. Revisa que la carpeta bin de ffmpeg esté en el PATH y abre una terminal nueva.
"@
}

if (-not (Test-Path -LiteralPath $Video -PathType Leaf)) {
    Fail "No se encuentra el vídeo: $Video"
}

$resolvedVideo = (Resolve-Path -LiteralPath $Video).Path

$encoderList = & ffmpeg -hide_banner -encoders 2>&1 | Out-String
if ($LASTEXITCODE -ne 0) {
    Fail "No se pudo consultar los codificadores de ffmpeg. Salida: $encoderList"
}

if ($encoderList -notmatch "libwebp") {
    Fail @"
ffmpeg está instalado, pero esta build no incluye el codificador libwebp.
Hace falta para generar los .webp de la secuencia. Instala una build completa (por ejemplo Gyan.FFmpeg) y vuelve a intentarlo.
"@
}

$durationText = (& ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 $resolvedVideo 2>&1 | Out-String).Trim()
$durationText = $durationText.Replace(",", ".")
$duration = 0.0
$parsed = [double]::TryParse(
    $durationText,
    [System.Globalization.NumberStyles]::Float,
    [System.Globalization.CultureInfo]::InvariantCulture,
    [ref]$duration
)

if (-not $parsed -or $duration -le 0) {
    Fail "No se pudo leer la duración del vídeo. ffprobe respondió: $durationText"
}

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
$resolvedOutput = (Resolve-Path -LiteralPath $OutputDir).Path

Get-ChildItem -LiteralPath $resolvedOutput -Filter "frame_*.webp" -File -ErrorAction SilentlyContinue |
    Remove-Item -Force

# Un poco por encima de la tasa exacta para no quedarnos cortos;
# -frames:v recorta al número pedido.
$fps = (($FrameCount + 0.5) / $duration).ToString(
    "0.######",
    [System.Globalization.CultureInfo]::InvariantCulture
)
$filter = "fps=${fps},scale=1600:-2:flags=lanczos"
$pattern = Join-Path $resolvedOutput "frame_%04d.webp"

Write-Host "Vídeo: $resolvedVideo"
Write-Host "Salida: $resolvedOutput"
Write-Host "Fotogramas: $FrameCount · ancho 1600 px · calidad 70"

& ffmpeg -y -hide_banner -i $resolvedVideo -an -vf $filter -frames:v $FrameCount -start_number 1 -c:v libwebp -quality 70 -compression_level 4 -pix_fmt yuv420p $pattern
if ($LASTEXITCODE -ne 0) {
    Fail "ffmpeg terminó con código $LASTEXITCODE y no se generó la secuencia."
}

$frames = @(Get-ChildItem -LiteralPath $resolvedOutput -Filter "frame_*.webp" -File)
$totalBytes = 0L
foreach ($frame in $frames) {
    $totalBytes += $frame.Length
}

$totalMb = $totalBytes / 1MB
Write-Host ("Listo: {0} archivos, {1:N2} MB" -f $frames.Count, $totalMb)

if ($frames.Count -ne $FrameCount) {
    Write-Warning "Se pedían $FrameCount fotogramas y hay $($frames.Count). Revisa el vídeo de origen."
}

if ($totalBytes -gt 8MB) {
    Write-Warning "La secuencia pesa más de 8 MB. Baja FrameCount, o vuelve a extraer con menos fotogramas, antes de publicarla."
}

exit 0
