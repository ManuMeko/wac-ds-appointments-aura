import { auraConfig } from "@/data/aura";
import { tallerConfig } from "@/data/taller";
import type { DemoConfig, DemoId } from "@/data/types";

/**
 * Configuración activa del sitio.
 *
 * NEXT_PUBLIC_DEMO elige el contenido y el tema (aura | taller).
 * Cada demo declara su slug. NEXT_PUBLIC_BUSINESS_SLUG, si existe,
 * sustituye solo ese slug al hablar con el API.
 *
 * `demos.taller` es Motorline. Si la variable pide una demo
 * que no está en el mapa, se usa Aura.
 */

const demos: Record<DemoId, DemoConfig | null> = {
  aura: auraConfig,
  taller: tallerConfig,
};

function selectDemo(): DemoConfig {
  const requested = process.env.NEXT_PUBLIC_DEMO?.trim();

  if (requested === "taller" && demos.taller) {
    return demos.taller;
  }

  return demos.aura ?? auraConfig;
}

const activeDemo = selectDemo();

export const DEFAULT_BUSINESS_SLUG = activeDemo.slug;

export const businessSlug =
  process.env.NEXT_PUBLIC_BUSINESS_SLUG?.trim() || DEFAULT_BUSINESS_SLUG;

const exampleProfessional = activeDemo.professionals[0];
const [exampleFirstName = "", ...exampleLastNameParts] =
  exampleProfessional?.name.split(" ") ?? [];
const emailDomain = activeDemo.contact.email.split("@")[1] ?? "";

export const business = {
  ...activeDemo,

  slug: businessSlug,

  placeholders: {
    professionalFirstName: exampleFirstName,
    professionalLastName: exampleLastNameParts.join(" "),
    professionalDisplayName: exampleProfessional
      ? `Ej. ${exampleFirstName} · ${exampleProfessional.role}`
      : "",
    professionalEmail: emailDomain
      ? `profesional@${emailDomain}`
      : activeDemo.contact.email,
    professionalPhone: activeDemo.contact.phoneDisplay.replace(
      /^\+\d{1,3}\s*/,
      "",
    ),
  },
};
