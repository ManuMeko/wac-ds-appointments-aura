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
     RUNTIME DEPLOYMENT

     El proyecto ya no es únicamente una demo estática.

     El panel Business utiliza:

     - autenticación JWT;
     - rutas dinámicas;
     - datos reales;
     - navegación a recursos por ID;
     - futura gestión de citas en tiempo real.

     Por tanto NO utilizamos:

     output: "export"

     Esto permite rutas dinámicas como:

     /business/appointments/19
     /business/appointments/25
     /business/appointments/103
  ======================================================= */

  /* =======================================================
     IMAGES

     Dejamos habilitado el optimizador de Next.js porque
     el proyecto se desplegará con runtime Next.js.

     También declaramos explícitamente las calidades que
     ya utiliza Aura Studio para evitar warnings como:

     quality "90" is not configured
     quality "92" is not configured
  ======================================================= */

  images: {
    qualities: [
      75,
      90,
      92,
    ],
  },

  /* =======================================================
     TRAILING SLASH

     Ya no necesitamos forzar trailingSlash para un hosting
     estático basado en carpetas.

     Las rutas Business dinámicas funcionarán de forma normal:

     /business/appointments/19
  ======================================================= */

  trailingSlash: false,

  /* =======================================================
     BUILD QUALITY

     No ocultamos errores de TypeScript.

     Si existe un error real de tipado, queremos que el build
     falle antes de desplegar.
  ======================================================= */

  typescript: {
    ignoreBuildErrors: false,
  },
};

export default nextConfig;