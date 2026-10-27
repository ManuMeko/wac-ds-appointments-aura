# Modelo 01 en un VPS

Stack de producción: PostgreSQL 17, la API Spring Boot, dos frontales Next.js (Aura y Taller) y Caddy con HTTPS automático. Postgres, la API y los frontales no publican puertos. Solo Caddy escucha en 80 y 443.

`infra/docker-compose.yml` sigue siendo el Postgres local de desarrollo. Este directorio es el que se usa en el VPS.

## 1. Preparar Ubuntu

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker "$USER"
```

Cierra la sesión y vuelve a entrar para que el grupo `docker` aplique.

Zona horaria del VPS, la que corresponda al país:

```bash
sudo timedatectl set-timezone Europe/Madrid
```

Firewall. No abras 5432, 8080 ni 3000.

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

## 2. Código y variables

```bash
sudo mkdir -p /opt/wac-ds
sudo chown "$USER":"$USER" /opt/wac-ds
git clone <url-del-repositorio> /opt/wac-ds
cd /opt/wac-ds/deploy
cp .env.example .env
chmod +x backup.sh
openssl rand -base64 48
```

Pega la salida de `openssl` en `JWT_SECRET`. Rellena `POSTGRES_PASSWORD`, los tres dominios, `ACME_EMAIL` y `APP_SECURITY_CORS_ALLOWED_ORIGINS` con los dos orígenes `https://` reales, sin barra final.

`APP_BOOTSTRAP_DEV_OWNER_ENABLED` se queda en `false`. Con el perfil `prod`, si vale `true` la API no arranca.

Deja `REMINDER_EMAIL_REAL_DELIVERY_ENABLED` y `REMINDER_SCHEDULER_ENABLED` en `false` hasta tener el dominio verificado en Resend. El simulador de correo solo existe en los perfiles `default` y `dev`, así que en `prod` un scheduler activo sin Resend no tiene proveedor. Cuando toque activarlo: `RESEND_API_KEY`, `RESEND_FROM_EMAIL` de un dominio verificado, `REMINDER_EMAIL_REAL_DELIVERY_ENABLED=true` y `REMINDER_SCHEDULER_ENABLED=true`.

`PUBLIC_FRONTEND_BASE_URL` es un solo origen. Los enlaces de gestión que manda la API usan ese valor.

Crea los registros DNS A o AAAA de `AURA_DOMAIN`, `TALLER_DOMAIN` y `API_DOMAIN` apuntando al VPS antes de levantar Caddy. Sin eso, la petición de certificado falla.

## 3. Arranque

```bash
cd /opt/wac-ds/deploy
docker compose up -d --build
docker compose ps
```

`NEXT_PUBLIC_DEMO`, `NEXT_PUBLIC_API_URL` y `NEXT_PUBLIC_SITE_URL` se hornean en la imagen. La web lee `NEXT_PUBLIC_API_BASE_URL`; el Dockerfile copia ahí el build arg `NEXT_PUBLIC_API_URL`. Cambiar un dominio obliga a reconstruir:

```bash
docker compose up -d --build web-aura web-taller
```

Comprueba, desde el VPS:

```bash
docker compose exec api bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080 && printf "GET /actuator/health HTTP/1.0\r\nHost: localhost\r\nConnection: close\r\n\r\n" >&3 && cat <&3'
```

Desde fuera, `https://api.example.com/actuator/health` debe responder `UP` cuando el DNS y el certificado ya están listos.

## 4. Copia diaria

```bash
crontab -e
```

```
15 3 * * * cd /opt/wac-ds/deploy && ./backup.sh >> /var/log/wac-ds-backup.log 2>&1
```

`backup.sh` hace `pg_dump` por el socket del contenedor, comprime con gzip y borra los ficheros `appointments-*.sql.gz` con más de 7 días. Las copias quedan en `deploy/backups/`, que no se versiona.

Restaurar una copia, con la API parada:

```bash
cd /opt/wac-ds/deploy
docker compose stop api
gunzip -c backups/appointments-YYYYMMDDTHHMMSSZ.sql.gz \
  | docker compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"
docker compose start api
```

Sustituye el nombre del fichero. `POSTGRES_USER` y `POSTGRES_DB` salen de `.env` si antes haces `set -a && source .env && set +a`.

## 5. Qué queda cerrado en producción

- El perfil Spring es `prod`. Sin `JWT_SECRET`, o con un secreto corto, repetitivo o igual al de desarrollo, la API no arranca.
- El bootstrap del OWNER de desarrollo queda apagado. Encenderlo también impide el arranque.
- `POST /api/v1/auth/login` y `POST /api/v1/public/bookings` admiten 10 peticiones por minuto y por IP. El contador vive en memoria de ese proceso. Cancelar o reprogramar una reserva no entra en ese límite.
- La salud pública es `GET /actuator/health`, sin detalles de la base.
