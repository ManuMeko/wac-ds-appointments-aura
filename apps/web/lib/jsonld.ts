import type { DemoConfig, OpeningHour } from "@/data/types";

import { getSiteUrl } from "@/lib/site";

const WEEKDAYS = [
  "Monday",
  "Tuesday",
  "Wednesday",
  "Thursday",
  "Friday",
] as const;

const DAY_RANGES: Record<string, readonly string[]> = {
  "Lunes — Viernes": WEEKDAYS,
  Sábado: ["Saturday"],
  Domingo: ["Sunday"],
};

function openingHoursSpecification(rows: readonly OpeningHour[]) {
  return rows.flatMap((row) => {
    const days = DAY_RANGES[row.days];
    const match = row.hours.match(/(\d{1,2}:\d{2})\s*[—–-]\s*(\d{1,2}:\d{2})/);
    if (!days || !match) return [];

    return [
      {
        "@type": "OpeningHoursSpecification",
        dayOfWeek: [...days],
        opens: match[1],
        closes: match[2],
      },
    ];
  });
}

function postalAddress(config: DemoConfig) {
  const postal = config.contact.addressLine2.match(/^(\d{5})\b/);
  const country =
    config.contact.country === "España" ? "ES" : config.contact.country;

  return {
    "@type": "PostalAddress",
    streetAddress: config.contact.addressLine1,
    addressLocality: config.contact.city,
    ...(postal ? { postalCode: postal[1] } : {}),
    addressCountry: country,
  };
}

export function buildLocalBusiness(config: DemoConfig) {
  const schemaType = config.seo.schemaType;
  if (!schemaType) return null;

  const origin = getSiteUrl();
  const hours = openingHoursSpecification(config.openingHours);
  const image = new URL(config.seo.ogImage, origin).href;

  return {
    "@context": "https://schema.org",
    "@type": Array.isArray(schemaType) ? [...schemaType] : schemaType,
    name: config.brand.name,
    url: origin,
    image,
    telephone: config.contact.phoneDisplay,
    address: postalAddress(config),
    ...(config.seo.priceRange ? { priceRange: config.seo.priceRange } : {}),
    ...(config.seo.areaServed
      ? {
          areaServed: {
            "@type": "City",
            name: config.seo.areaServed,
          },
        }
      : {}),
    ...(config.seo.geo
      ? {
          geo: {
            "@type": "GeoCoordinates",
            latitude: config.seo.geo.latitude,
            longitude: config.seo.geo.longitude,
          },
        }
      : {}),
    ...(hours.length > 0 ? { openingHoursSpecification: hours } : {}),
  };
}
