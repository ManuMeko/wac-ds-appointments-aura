// apps/web/next.config.ts

import type { NextConfig } from "next";

/* =========================================================
   WAC DS · MODEL 01 · APPOINTMENTS
   Next.js configuration
========================================================= */

const nextConfig: NextConfig = {
  /* =======================================================
     REACT COMPILER
  ======================================================= */

  reactCompiler: true,

  /* =======================================================
     STATIC EXPORT

     Aura Studio Essential V1 se desplegará inicialmente
     como demo estática.

     Esto hace que `next build` genere:
     apps/web/out/
  ======================================================= */

  output: "export",

  /* =======================================================
     IMAGES

     En un export estático no tendremos el optimizador
     de imágenes de Next.js ejecutándose en servidor.

     Por eso desactivamos la optimización runtime.
  ======================================================= */

  images: {
    unoptimized: true,
  },

  /* =======================================================
     TRAILING SLASH

     Facilita la compatibilidad con hosting estático y
     generación de rutas como:

     /aviso-legal/
     /privacidad/
     /cookies/
  ======================================================= */

  trailingSlash: true,

  /* =======================================================
     BUILD QUALITY

     No ocultamos errores de TypeScript ni ESLint.
     Si algo falla, queremos que el build falle también.
  ======================================================= */

  typescript: {
    ignoreBuildErrors: false,
  },
};

export default nextConfig;