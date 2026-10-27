# Development reset for aura-studio and motorline-taller.
# Asks for RESET, dumps the database, deletes demo customers,
# appointments and reminders, then applies the two seeds.
# Does not create users or passwords. Does not run by itself.
# Launch from the repository root:
#   powershell -NoProfile -File .\infra\seed\reset-dev.ps1

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$container = "wac-ds-appointments-postgres"
$database = "wac_ds_appointments"
$dbUser = "wacds"

Write-Host ""
Write-Host "Reset de desarrollo de aura-studio y motorline-taller."
Write-Host "1. pg_dump en infra/seed/backups/"
Write-Host "2. Borra clientes, citas y recordatorios de esos dos negocios."
Write-Host "3. Aplica 01-aura-studio.sql y 02-motorline-taller.sql."
Write-Host "No toca usuarios ni contrasenas."
Write-Host "Los enlaces de gestion de esas citas desaparecen con la cita."
Write-Host ""

$answer = Read-Host "Escribe RESET para continuar"
if ($answer -cne "RESET") {
    Write-Host "Cancelado. No se ha cambiado nada."
    exit 1
}

$running = docker inspect -f "{{.State.Running}}" $container 2>$null
if ($LASTEXITCODE -ne 0 -or $running -ne "true") {
    Write-Error "El contenedor $container no esta en marcha. No se borra nada."
    exit 1
}

$backupDir = Join-Path $PSScriptRoot "backups"
New-Item -ItemType Directory -Force -Path $backupDir | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupFile = Join-Path $backupDir "wac_ds_appointments-$stamp.sql"
$remoteBackup = "/tmp/wac-ds-backup-$stamp.sql"

docker exec $container pg_dump -U $dbUser -d $database --no-owner --no-privileges -f $remoteBackup
if ($LASTEXITCODE -ne 0) {
    docker exec $container rm -f $remoteBackup | Out-Null
    Write-Error "pg_dump ha fallado. No se borra nada."
    exit 1
}

docker cp "${container}:${remoteBackup}" $backupFile
$copyExit = $LASTEXITCODE
docker exec $container rm -f $remoteBackup | Out-Null
if ($copyExit -ne 0) {
    Write-Error "No se pudo copiar la copia de seguridad. No se borra nada."
    exit 1
}

$backupSize = (Get-Item $backupFile).Length
if ($backupSize -lt 1000) {
    Write-Error "La copia de seguridad no es valida. No se borra nada."
    exit 1
}

Write-Host "Copia guardada en $backupFile"

$deleteSql = @"
BEGIN;
DELETE FROM appointment_reminders AS ar
USING appointments AS a, businesses AS b
WHERE ar.appointment_id = a.id
  AND a.business_id = b.id
  AND b.slug IN ('aura-studio', 'motorline-taller');

DELETE FROM appointments AS a
USING businesses AS b
WHERE a.business_id = b.id
  AND b.slug IN ('aura-studio', 'motorline-taller');

DELETE FROM customers AS c
USING businesses AS b
WHERE c.business_id = b.id
  AND b.slug IN ('aura-studio', 'motorline-taller');
COMMIT;
"@

$deleteSql | docker exec -i $container psql -U $dbUser -d $database -v ON_ERROR_STOP=1
if ($LASTEXITCODE -ne 0) {
    Write-Error "El borrado ha fallado. La copia sigue en $backupFile"
    exit 1
}

foreach ($name in @("01-aura-studio.sql", "02-motorline-taller.sql")) {
    $local = Join-Path $PSScriptRoot $name
    $remote = "/tmp/$name"
    docker cp $local "${container}:${remote}"
    if ($LASTEXITCODE -ne 0) {
        Write-Error "No se pudo copiar $name. La copia sigue en $backupFile"
        exit 1
    }

    docker exec $container psql -U $dbUser -d $database -v ON_ERROR_STOP=1 -f $remote
    $applyExit = $LASTEXITCODE
    docker exec $container rm -f $remote | Out-Null
    if ($applyExit -ne 0) {
        Write-Error "Fallo al aplicar $name. La copia sigue en $backupFile"
        exit 1
    }
}

Write-Host "Reset terminado."
