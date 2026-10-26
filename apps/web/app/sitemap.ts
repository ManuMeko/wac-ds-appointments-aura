import type { MetadataRoute } from "next";

import { business } from "@/config/business";
import { getSiteUrl } from "@/lib/site";

export default function sitemap(): MetadataRoute.Sitemap {
  const origin = getSiteUrl();
  const paths = [
    "/",
    "/reservar",
    ...business.legalLinks.map((link) => link.href),
  ].filter((path) => path.startsWith("/") && !path.startsWith("/business"));

  return [...new Set(paths)].map((path) => ({
    url: path === "/" ? `${origin}/` : `${origin}${path}`,
    changeFrequency: path === "/" ? "weekly" : "monthly",
    priority: path === "/" ? 1 : 0.6,
  }));
}
