# Seeds de desarrollo

SQL fuera de Flyway para la base `wac_ds_appointments`. No crea usuarios, contraseñas ni tokens: el owner sale de `DevelopmentAuthBootstrap` y del entorno.

Cada script se puede ejecutar más de una vez. El catálogo no se duplica. Los clientes y las citas demo solo se insertan si ese email y esa hora de inicio aún no existen. Las citas se anclan al lunes de la semana actual en `Europe/Madrid`, desde la semana pasada hasta la siguiente.

Ejecutar desde la raíz del repositorio, con el contenedor `wac-ds-appointments-postgres` en marcha. `psql` entra por el socket local del contenedor, así que el comando no lleva contraseña.

```powershell
docker cp .\infra\seed\01-aura-studio.sql wac-ds-appointments-postgres:/tmp/01-aura-studio.sql
docker exec wac-ds-appointments-postgres psql -U wacds -d wac_ds_appointments -v ON_ERROR_STOP=1 -f /tmp/01-aura-studio.sql

docker cp .\infra\seed\02-motorline-taller.sql wac-ds-appointments-postgres:/tmp/02-motorline-taller.sql
docker exec wac-ds-appointments-postgres psql -U wacds -d wac_ds_appointments -v ON_ERROR_STOP=1 -f /tmp/02-motorline-taller.sql
```

## Reset

`infra/seed/reset-dev.ps1` pide escribir `RESET`. Si la confirmación no coincide, no hace nada. Después guarda un `pg_dump` en `infra/seed/backups/` (ignorado en git), borra solo clientes, citas y recordatorios de `aura-studio` y `motorline-taller`, y aplica los dos seeds. No lo lances desde un entorno que no sea el de desarrollo local.

```powershell
powershell -NoProfile -File .\infra\seed\reset-dev.ps1
```
