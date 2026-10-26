import type { MetadataRoute } from "next";

import { business } from "@/config/business";
import { getSiteUrl } from "@/lib/site";

export default function robots(): MetadataRoute.Robots {
  const indexable = business.seo.robots.index;

  return {
    rules: {
      userAgent: "*",
      ...(indexable ? { allow: "/" } : { disallow: "/" }),
      ...(indexable ? { disallow: "/business" } : {}),
    },
    sitemap: `${getSiteUrl()}/sitemap.xml`,
  };
}
