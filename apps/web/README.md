# WAC DS · Modelo 01 · Essentials

Web estática de Next.js para Cloudflare Pages. Una build publica una sola demo, Aura Studio o Motorline Taller, según `NEXT_PUBLIC_DEMO`.

No hay API, panel ni reserva en servidor. La cita se pide en `#contacto` (WhatsApp, teléfono y email).

## Desarrollo

Desde `apps/web`:

```bash
npm install
npm run dev
```

Copia `apps/web/.env.example` a un archivo local si quieres fijar la demo. No subas secretos.

```bash
# PowerShell
$env:NEXT_PUBLIC_DEMO="aura"
npm run dev

$env:NEXT_PUBLIC_DEMO="taller"
npm run dev
```

## Export estático

`next.config.ts` usa `output: "export"`, `trailingSlash: true` e `images.unoptimized: true`.

```bash
$env:NEXT_PUBLIC_DEMO="aura"
npm run build

$env:NEXT_PUBLIC_DEMO="taller"
npm run build
```

La salida queda en `apps/web/out/`.

## Secuencia de scroll de Aura

El máster no se commitea. Créalo en `apps/web/clips/aura.mp4` (esa carpeta está en `.gitignore`).

Desde `apps/web`, con `ffmpeg` y `ffprobe` en el PATH:

```powershell
.\scripts\extract-frames.ps1 -Video .\clips\aura.mp4 -OutputDir .\public\images\aura\sequence -FrameCount 96
```

El script genera `frame_0001.webp`, `frame_0002.webp`… a **1600 px** de ancho y **calidad 70**. Si la carpeta supera **8 MB**, el script avisa: baja `FrameCount` y vuelve a extraer.

Hasta que existan esos archivos, Aura muestra el hero de siempre.

Después de extraer, abre `apps/web/data/aura.ts` y cambia `frameCount: 0` por el número real de archivos `frame_####.webp`. Con el comando de arriba, 96 fotogramas, el valor es **96**.
