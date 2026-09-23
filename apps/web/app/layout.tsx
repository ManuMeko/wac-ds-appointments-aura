// apps/web/app/layout.tsx

import type { Metadata, Viewport } from "next";
import { Cormorant_Garamond, Geist } from "next/font/google";

import { auraBrand, auraSeo } from "@/data/aura";

import "./globals.css";

/* =========================================================
   WAC DS · MODEL 01 · APPOINTMENTS
   Aura Studio · Root Layout
========================================================= */

/* =========================================================
   FONTS
========================================================= */

/**
 * Tipografía principal de interfaz.
 *
 * next/font:
 * - optimiza automáticamente las fuentes;
 * - evita peticiones externas desde el navegador;
 * - reduce layout shift;
 * - genera preload cuando corresponde.
 */
const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
  display: "swap",
});

/**
 * Tipografía editorial / premium de Aura Studio.
 *
 * Utilizada principalmente en:
 * - títulos;
 * - textos editoriales;
 * - elementos de identidad visual.
 */
const cormorant = Cormorant_Garamond({
  variable: "--font-aura-serif",
  subsets: ["latin"],
  display: "swap",
  weight: ["400", "500", "600", "700"],
  style: ["normal", "italic"],
});

/* =========================================================
   SITE URL
========================================================= */

/**
 * Durante desarrollo:
 *
 * http://localhost:3000
 *
 * Cuando despleguemos la demo:
 *
 * NEXT_PUBLIC_SITE_URL=https://dominio-de-la-demo.com
 *
 * De esta forma no tendremos que modificar código para:
 *
 * - metadataBase
 * - canonical
 * - Open Graph
 * - Twitter cards
 */
const siteUrl =
  process.env.NEXT_PUBLIC_SITE_URL?.trim() || "http://localhost:3000";

const metadataBase = new URL(siteUrl);

/* =========================================================
   METADATA
========================================================= */

export const metadata: Metadata = {
  /* ---------------------------------------------------------
     BASE URL
  --------------------------------------------------------- */

  metadataBase,

  /* ---------------------------------------------------------
     DOCUMENT TITLE
  --------------------------------------------------------- */

  title: {
    default: auraSeo.defaultTitle,
    template: auraSeo.titleTemplate,
  },

  /* ---------------------------------------------------------
     GENERAL SEO
  --------------------------------------------------------- */

  description: auraSeo.description,

  applicationName: auraSeo.siteName,

  category: "beauty",

  keywords: [
    "Aura Studio",
    "estética",
    "belleza",
    "bienestar",
    "centro de estética",
    "estética Madrid",
    "tratamientos faciales",
    "estética avanzada",
    "masajes",
    "cuidado personal",
    "tratamientos personalizados",
    "bienestar Madrid",
  ],

  creator: "WAC DS",

  publisher: "WAC DS",

  /* ---------------------------------------------------------
     CANONICAL
  --------------------------------------------------------- */

  alternates: {
    canonical: "/",
  },

  /* ---------------------------------------------------------
     FORMAT DETECTION
  --------------------------------------------------------- */

  formatDetection: {
    email: false,
    address: false,
    telephone: false,
  },

  /* ---------------------------------------------------------
     ROBOTS

     Aura Studio es una demo ficticia.

     NO debe indexarse mientras siga siendo una demostración.

     Cuando el starter se utilice para un cliente real:
     auraSeo.robots.index = true
     auraSeo.robots.follow = true
  --------------------------------------------------------- */

  robots: {
    index: auraSeo.robots.index,
    follow: auraSeo.robots.follow,

    googleBot: {
      index: auraSeo.robots.index,
      follow: auraSeo.robots.follow,

      "max-image-preview": "large",
      "max-snippet": -1,
      "max-video-preview": -1,
    },
  },

  /* ---------------------------------------------------------
     OPEN GRAPH
  --------------------------------------------------------- */

  openGraph: {
    type: "website",

    locale: auraSeo.locale,

    url: "/",

    siteName: auraSeo.siteName,

    title: auraSeo.defaultTitle,

    description: auraSeo.description,

    images: [
      {
        url: auraSeo.ogImage,

        width: 1536,

        height: 1024,

        alt: `${auraBrand.name} · ${auraBrand.tagline}`,
      },
    ],
  },

  /* ---------------------------------------------------------
     TWITTER / SOCIAL CARDS
  --------------------------------------------------------- */

  twitter: {
    card: "summary_large_image",

    title: auraSeo.defaultTitle,

    description: auraSeo.description,

    images: [auraSeo.ogImage],
  },

  /* ---------------------------------------------------------
     ICONS

     app/favicon.ico ya existe y Next.js también lo detecta
     automáticamente, pero lo declaramos explícitamente.
  --------------------------------------------------------- */

  icons: {
    icon: "/favicon.ico",
    shortcut: "/favicon.ico",
  },

  /* ---------------------------------------------------------
     OTHER
  --------------------------------------------------------- */

  other: {
    "content-language": "es",
  },
};

/* =========================================================
   VIEWPORT
========================================================= */

export const viewport: Viewport = {
  width: "device-width",

  initialScale: 1,

  viewportFit: "cover",

  themeColor: [
    {
      media: "(prefers-color-scheme: light)",
      color: "#f8f5f0",
    },
    {
      media: "(prefers-color-scheme: dark)",
      color: "#f8f5f0",
    },
  ],

  /**
   * Aura Studio está diseñada exclusivamente con
   * esquema visual claro.
   */
  colorScheme: "light",
};

/* =========================================================
   ROOT LAYOUT
========================================================= */

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang={auraSeo.language}
      className={`${geistSans.variable} ${cormorant.variable}`}
      suppressHydrationWarning
    >
      <body className="min-h-screen bg-background font-sans text-foreground antialiased">
        {children}
      </body>
    </html>
  );
}