import type { MetadataRoute } from "next";

import { business } from "@/config/business";

export default function manifest(): MetadataRoute.Manifest {
  const mark =
    business.theme === "taller" ? "/icons/taller.svg" : "/icons/aura.svg";

  return {
    name: business.brand.name,
    short_name: business.brand.shortName,
    description: business.seo.description,
    start_url: "/",
    display: "standalone",
    background_color: business.seo.backgroundColor ?? business.seo.themeColor,
    theme_color: business.seo.themeColor,
    lang: business.seo.language,
    icons: [
      { src: mark, sizes: "any", type: "image/svg+xml", purpose: "any" },
      { src: "/icon", sizes: "32x32", type: "image/png" },
      { src: "/apple-icon", sizes: "180x180", type: "image/png" },
    ],
  };
}
