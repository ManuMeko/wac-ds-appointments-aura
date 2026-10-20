# Seeds de desarrollo

SQL fuera de Flyway para la base `wac_ds_appointments`. No crea usuarios, contraseñas, tokens, citas ni clientes.

Ejecutar desde la raíz del repositorio, con el contenedor `wac-ds-appointments-postgres` en marcha. `psql` entra por el socket local del contenedor, así que el comando no lleva contraseña.

```powershell
docker cp .\infra\seed\01-aura-studio.sql wac-ds-appointments-postgres:/tmp/01-aura-studio.sql
docker exec wac-ds-appointments-postgres psql -U wacds -d wac_ds_appointments -v ON_ERROR_STOP=1 -f /tmp/01-aura-studio.sql

docker cp .\infra\seed\02-motorline-taller.sql wac-ds-appointments-postgres:/tmp/02-motorline-taller.sql
docker exec wac-ds-appointments-postgres psql -U wacds -d wac_ds_appointments -v ON_ERROR_STOP=1 -f /tmp/02-motorline-taller.sql
```

Cada script se puede ejecutar más de una vez: si la fila ya existe, no la duplica ni la modifica.
