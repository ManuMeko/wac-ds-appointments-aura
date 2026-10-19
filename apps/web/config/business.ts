import { auraConfig } from "@/data/aura";

/**
 * Configuración activa del sitio.
 *
 * El contenido de la demo sigue en data/aura.ts. Este archivo
 * no lo copia: lo reutiliza y añade el slug público del negocio.
 * Para montar otro negocio, cambia NEXT_PUBLIC_BUSINESS_SLUG y
 * sustituye esa demo (o el import de abajo).
 */

export const DEFAULT_BUSINESS_SLUG = "aura-studio";

export const businessSlug =
  process.env.NEXT_PUBLIC_BUSINESS_SLUG?.trim() || DEFAULT_BUSINESS_SLUG;

const teamImagesByProfessionalId: Record<string, string> = {
  "laura-martin": auraConfig.assets.team.lauraMartin,
  "andrea-lopez": auraConfig.assets.team.andreaLopez,
  "sofia-garcia": auraConfig.assets.team.sofiaGarcia,
};

const exampleProfessional = auraConfig.professionals[0];
const [exampleFirstName = "", ...exampleLastNameParts] =
  exampleProfessional?.name.split(" ") ?? [];
const emailDomain = auraConfig.contact.email.split("@")[1] ?? "";

export const business = {
  slug: businessSlug,

  ...auraConfig,

  brandSuffix: auraConfig.brand.name
    .replace(auraConfig.brand.shortName, "")
    .trim(),

  experienceBadge: `Ritual ${auraConfig.brand.shortName}`,

  imageAlts: {
    hero: `Cabina premium de ${auraConfig.brand.name} con camilla de tratamiento, iluminación cálida y elementos de bienestar`,
    experience: `Detalle de la experiencia ${auraConfig.brand.name} con productos de bienestar, textiles suaves y ambiente de tratamiento premium`,
  },

  teamImagesByProfessionalId,

  placeholders: {
    professionalFirstName: exampleFirstName,
    professionalLastName: exampleLastNameParts.join(" "),
    professionalDisplayName: exampleProfessional
      ? `Ej. ${exampleFirstName} · ${exampleProfessional.role}`
      : "",
    professionalEmail: emailDomain
      ? `profesional@${emailDomain}`
      : auraConfig.contact.email,
    professionalPhone: auraConfig.contact.phoneDisplay.replace(
      /^\+\d{1,3}\s*/,
      "",
    ),
  },
};
