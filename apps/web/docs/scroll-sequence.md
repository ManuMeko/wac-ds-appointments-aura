# Secuencia de scroll

Bloque reutilizable de la home: una secuencia de imágenes en un canvas que avanza con el scroll. Cada demo lo activa con `scrollSequence` en su `DemoConfig`. Si el campo no está, la home no monta el bloque y el resto de la página no cambia. Aura no lo define.

No usa librerías de animación. El script de extracción llama a `ffmpeg` instalado en el sistema; no forma parte de las dependencias npm.

## 1. Grabar o generar el vídeo

Sirve un plano continuo del oficio: el coche entrando, el elevador, un detalle de la diagnosis. Un solo clip, sin cortes bruscos, de unos 6–12 segundos. El encuadre horizontal aguanta mejor el recorte `cover` de un canvas a pantalla completa.

También vale un vídeo generado. Lo que importa es el movimiento, no el origen: la web solo publica fotogramas.

Guarda el máster fuera de `public/`. En la web entran solo los `.webp`.

## 2. Extraer los fotogramas

Hace falta `ffmpeg` (y `ffprobe`, que viaja con él) en el PATH, con el codificador `libwebp`. Si no está, el script lo dice y se detiene. En Windows:

```powershell
winget install Gyan.FFmpeg
```

Abre una terminal nueva después de instalarlo. Desde `apps/web`:

```powershell
.\scripts\extract-frames.ps1 -Video .\clips\taller.mp4 -OutputDir .\public\images\taller\sequence
```

`FrameCount` es opcional y vale 120 si no se indica:

```powershell
.\scripts\extract-frames.ps1 -Video .\clips\taller.mp4 -OutputDir .\public\images\taller\sequence -FrameCount 120
```

El script genera `frame_0001.webp`, `frame_0002.webp`… a 1600 px de ancho y calidad 70. Sustituye los `frame_*.webp` que ya hubiera en esa carpeta.

La carpeta bajo `public/` se sirve tal cual. `public/images/taller/sequence/frame_0001.webp` corresponde a `/images/taller/sequence/frame_0001.webp`.

## 3. Configurar la demo

En el `DemoConfig` de la demo (`data/taller.ts`, o el archivo de una demo nueva):

```ts
scrollSequence: {
  basePath: "/images/taller/sequence",
  frameCount: 120,
  fileExtension: "webp",
  heightVh: 400,
  overlays: [
    {
      from: 0,
      to: 0.36,
      title: "Primero, el coche.",
      text: "Identificamos el síntoma y el vehículo antes de tocar una pieza.",
    },
  ],
},
```

- `basePath` es la ruta pública, sin nombre de archivo y sin barra final obligatoria.
- `frameCount` tiene que coincidir con los archivos `frame_0001.webp` … `frame_0120.webp`.
- `heightVh` es la altura del recorrido en vh. El canvas queda fijo a pantalla completa mientras se recorre.
- `overlays` es opcional. `from` y `to` van de 0 a 1. El texto entra y sale con el progreso. Son HTML real, no texto pintado en el canvas.

La home (`app/page.tsx`) inserta el bloque justo después del hero, y solo cuando `business.scrollSequence` existe.

Si la carpeta no tiene imágenes, o alguna petición falla, el bloque no rompe la página: el canvas se queda en el color `--hero-bg` del tema activo.

Con `prefers-reduced-motion: reduce` no hay recorrido sticky. Se muestra el primer fotograma y los textos uno debajo de otro.

## Peso recomendado

Menos de **8 MB** en total para toda la secuencia.

120 fotogramas a 1600 px y calidad 70 suelen quedar dentro de ese margen si el plano no es un ruido fino (follaje, chispas, grano). Si el script avisa de que se pasa de 8 MB:

- baja `FrameCount` a 80 o 90 y vuelve a extraer;
- o acorta el clip antes de extraer, para que cada fotograma aporte movimiento.

No subas el máster de vídeo a `public/`. No bajes de una calidad en la que el texto de los overlays se lea sobre el plano: el presupuesto es de peso, y también de lectura.
