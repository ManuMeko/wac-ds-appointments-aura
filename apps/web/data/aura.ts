// apps/web/data/aura.ts

/**
 * ============================================================
 * WAC DS · MODEL 01 · APPOINTMENTS
 * Aura Studio · Demo configuration
 * ============================================================
 *
 * Este archivo centraliza el contenido y la configuración
 * comercial de la demo Aura Studio.
 *
 * OBJETIVO:
 *
 * Evitar que datos propios del negocio queden repartidos entre
 * componentes React.
 *
 * De esta forma podremos reutilizar el modelo WAC DS para futuros
 * clientes sustituyendo configuración/contenido sin modificar
 * innecesariamente la estructura de los componentes.
 */

/* ============================================================
   TYPES
============================================================ */

export type AuraNavigationItem = {
  label: string;
  href: string;
};

export type AuraStat = {
  value: string;
  label: string;
};

export type AuraService = {
  id: string;
  number: string;
  name: string;
  description: string;
  duration: string;
  price: string;
  category: string;
  featured: boolean;
};

export type AuraProfessional = {
  id: string;
  name: string;
  role: string;
  initials: string;
  description: string;
  specialties: string[];
};

export type AuraReview = {
  id: string;
  author: string;
  quote: string;
  rating: number;
};

export type AuraBenefit = {
  id: string;
  label: string;
};

export type AuraOpeningHour = {
  days: string;
  hours: string;
};

export type AuraContact = {
  phoneDisplay: string;
  phoneHref: string;
  whatsappNumber: string;
  email: string;
  addressLine1: string;
  addressLine2: string;
  city: string;
  country: string;
};

export type AuraSocialLink = {
  name: string;
  href: string;
};

export type AuraLegalLink = {
  label: string;
  href: string;
};

export type AuraSeoConfig = {
  siteName: string;
  defaultTitle: string;
  titleTemplate: string;
  description: string;
  locale: string;
  language: string;
  ogImage: string;
  robots: {
    index: boolean;
    follow: boolean;
  };
};

export type AuraAssetsConfig = {
  hero: string;
  experience: string;
  openGraph: string;
  team: {
    lauraMartin: string;
    andreaLopez: string;
    sofiaGarcia: string;
  };
};

export type AuraDemoConfig = {
  enabled: boolean;
  label: string;
  legalNotice: string;
};

/* ============================================================
   DEMO
============================================================ */

export const auraDemo: AuraDemoConfig = {
  enabled: true,

  label: "Demo comercial WAC DS",

  legalNotice:
    "Aura Studio es una empresa ficticia utilizada como demostración del Modelo 01 · Appointments de WAC DS.",
};

/* ============================================================
   BRAND
============================================================ */

export const auraBrand = {
  name: "Aura Studio",

  shortName: "Aura",

  monogram: "A",

  tagline: "Estética · Belleza · Bienestar",

  description:
    "Estética, belleza y bienestar desde una forma más personal de entender el cuidado.",

  locationLabel: "Madrid · España",

  signature: {
    eyebrow: "Aura Signature",

    title: "Cuidarte también es una forma de parar.",
  },
} as const;

/* ============================================================
   SEO / METADATA
============================================================ */

export const auraSeo: AuraSeoConfig = {
  siteName: "Aura Studio",

  defaultTitle: "Aura Studio · Estética, belleza y bienestar",

  titleTemplate: "%s · Aura Studio",

  description:
    "Estudio de estética y bienestar con tratamientos personalizados, atención cercana y una experiencia de cuidado diseñada alrededor de cada persona.",

  locale: "es_ES",

  language: "es",

  ogImage: "/images/aura/og-aura-studio.png",

  robots: {
    index: false,
    follow: false,
  },
};

/* ============================================================
   ASSETS
============================================================ */

export const auraAssets: AuraAssetsConfig = {
  hero: "/images/aura/hero-aura-studio.png",

  experience: "/images/aura/experience-aura-studio.png",

  openGraph: "/images/aura/og-aura-studio.png",

  team: {
    lauraMartin: "/images/aura/team/laura-martin.png",

    andreaLopez: "/images/aura/team/andrea-lopez.png",

    sofiaGarcia: "/images/aura/team/sofia-garcia.png",
  },
};

/* ============================================================
   NAVIGATION
============================================================ */

export const auraNavigation: AuraNavigationItem[] = [
  {
    label: "Servicios",
    href: "/#servicios",
  },
  {
    label: "Experiencia",
    href: "/#experiencia",
  },
  {
    label: "Equipo",
    href: "/#equipo",
  },
  {
    label: "Opiniones",
    href: "/#opiniones",
  },
  {
    label: "Contacto",
    href: "/#contacto",
  },
];

/* ============================================================
   HERO
============================================================ */

export const auraHero = {
  eyebrow: "Estética · Belleza · Bienestar",

  title: "Tu momento.",

  highlightedTitle: "Tu espacio.",

  description:
    "Un estudio de estética y bienestar donde cada tratamiento comienza escuchándote. Belleza personalizada, atención honesta y tiempo dedicado exclusivamente a ti.",

  primaryCta: {
    label: "Solicitar cita",
    href: "#contacto",
  },

  secondaryCta: {
    label: "Ver tratamientos",
    href: "#servicios",
  },
} as const;

/* ============================================================
   HERO STATS
============================================================ */

export const auraStats: AuraStat[] = [
  {
    value: "+8",
    label: "Años experiencia",
  },
  {
    value: "4.9",
    label: "Valoración",
  },
  {
    value: "1.2K",
    label: "Clientes",
  },
  {
    value: "100%",
    label: "Personalizado",
  },
];

/* ============================================================
   PHILOSOPHY
============================================================ */

export const auraPhilosophy = {
  eyebrow: "Nuestra filosofía",

  title: "No creemos en tratamientos genéricos.",

  highlightedTitle: "Creemos en entender qué necesitas.",

  description:
    "Por eso cada experiencia en Aura comienza con una conversación. Analizamos, escuchamos y diseñamos un tratamiento adaptado a tu piel, tus objetivos y tu momento.",
} as const;

/* ============================================================
   SERVICES
============================================================ */

export const auraServicesIntro = {
  eyebrow: "Tratamientos",

  title: "Cuidado pensado para ti.",

  description:
    "Protocolos personalizados, productos seleccionados y el tiempo necesario para hacer las cosas bien.",

  cta: {
    label: "Consultar todos los tratamientos",
    href: "#contacto",
  },
} as const;

export const auraServices: AuraService[] = [
  {
    id: "facial-treatment",
    number: "01",
    name: "Tratamiento facial",
    description:
      "Ritual personalizado para devolver luminosidad, hidratación y equilibrio a tu piel.",
    duration: "45 min",
    price: "49 €",
    category: "Facial",
    featured: true,
  },
  {
    id: "deep-cleansing",
    number: "02",
    name: "Limpieza profunda",
    description:
      "Tratamiento intensivo pensado para purificar, renovar y mejorar visiblemente la textura de la piel.",
    duration: "60 min",
    price: "65 €",
    category: "Facial",
    featured: true,
  },
  {
    id: "relaxing-massage",
    number: "03",
    name: "Masaje relajante",
    description:
      "Una experiencia de desconexión completa para liberar tensión y recuperar el bienestar físico.",
    duration: "60 min",
    price: "55 €",
    category: "Bienestar",
    featured: true,
  },
  {
    id: "premium-manicure",
    number: "04",
    name: "Manicura premium",
    description:
      "Cuidado completo de manos y uñas con acabado elegante, preciso y duradero.",
    duration: "45 min",
    price: "35 €",
    category: "Estética",
    featured: true,
  },
];

/* ============================================================
   EXPERIENCE
============================================================ */

export const auraExperience = {
  eyebrow: "La experiencia Aura",

  title: "Los detalles también forman parte del tratamiento.",

  description:
    "Desde la primera consulta hasta el último minuto de tu sesión, buscamos que cada paso tenga sentido: sin prisas, sin protocolos impersonales y sin tratamientos innecesarios.",

  visualTitle: "Menos ruido. Más cuidado.",

  visualDescription:
    "Un entorno diseñado para que puedas desconectar del exterior durante un rato.",
} as const;

/* ============================================================
   BENEFITS
============================================================ */

export const auraBenefits: AuraBenefit[] = [
  {
    id: "personal-diagnosis",
    label: "Diagnóstico personalizado",
  },
  {
    id: "specialized-professionals",
    label: "Profesionales especializados",
  },
  {
    id: "selected-products",
    label: "Productos seleccionados",
  },
  {
    id: "unhurried-care",
    label: "Atención sin prisas",
  },
];

/* ============================================================
   TEAM
============================================================ */

export const auraTeamIntro = {
  eyebrow: "Nuestro equipo",

  title: "Profesionales que escuchan antes de actuar.",

  description:
    "Formación continua, criterio profesional y una forma cercana de entender el cuidado personal.",
} as const;

export const auraProfessionals: AuraProfessional[] = [
  {
    id: "laura-martin",
    name: "Laura Martín",
    role: "Especialista facial",
    initials: "LM",
    description:
      "Especializada en diagnóstico estético y tratamientos faciales personalizados.",
    specialties: [
      "Diagnóstico facial",
      "Tratamientos faciales",
      "Cuidado de la piel",
    ],
  },
  {
    id: "andrea-lopez",
    name: "Andrea López",
    role: "Estética avanzada",
    initials: "AL",
    description:
      "Experta en técnicas de estética avanzada y protocolos personalizados de cuidado.",
    specialties: [
      "Estética avanzada",
      "Manicura",
      "Tratamientos personalizados",
    ],
  },
  {
    id: "sofia-garcia",
    name: "Sofía García",
    role: "Masajes & bienestar",
    initials: "SG",
    description:
      "Especialista en bienestar corporal, relajación y experiencias sensoriales.",
    specialties: [
      "Masajes",
      "Relajación",
      "Bienestar corporal",
    ],
  },
];

/* ============================================================
   REVIEWS
============================================================ */

export const auraReviewsIntro = {
  eyebrow: "Opiniones",

  title: "Lo que más valoramos es que quieras volver.",
} as const;

export const auraReviews: AuraReview[] = [
  {
    id: "claudia-m",
    author: "Claudia M.",
    quote:
      "Desde que entras se nota que cuidan absolutamente cada detalle. El tratamiento fue excelente y la atención aún mejor.",
    rating: 5,
  },
  {
    id: "marina-r",
    author: "Marina R.",
    quote:
      "Un espacio precioso, profesional y tranquilo. Por fin he encontrado un centro al que realmente quiero volver.",
    rating: 5,
  },
  {
    id: "elena-v",
    author: "Elena V.",
    quote:
      "Me explicaron todo antes del tratamiento y adaptaron el servicio exactamente a lo que necesitaba mi piel.",
    rating: 5,
  },
];

/* ============================================================
   CONTACT
============================================================ */

export const auraContact: AuraContact = {
  phoneDisplay: "+34 600 000 000",

  phoneHref: "+34600000000",

  whatsappNumber: "34600000000",

  email: "hola@aurastudio.es",

  addressLine1: "Calle Velázquez, 120",

  addressLine2: "28006 Madrid",

  city: "Madrid",

  country: "España",
};

/* ============================================================
   OPENING HOURS
============================================================ */

export const auraOpeningHours: AuraOpeningHour[] = [
  {
    days: "Lunes — Viernes",
    hours: "10:00 — 20:00",
  },
  {
    days: "Sábado",
    hours: "10:00 — 15:00",
  },
  {
    days: "Domingo",
    hours: "Cerrado",
  },
];

/* ============================================================
   CONTACT SECTION
============================================================ */

export const auraContactSection = {
  eyebrow: "Reserva tu momento",

  title: "Cuéntanos qué necesitas.",

  description:
    "Escríbenos y nuestro equipo te ayudará a elegir el tratamiento más adecuado y encontrar el mejor momento para tu cita.",

  whatsappCta: {
    label: "Hablar por WhatsApp",
  },

  phoneCta: {
    label: "Llamar ahora",
  },
} as const;

/* ============================================================
   SOCIAL
============================================================ */

export const auraSocialLinks: AuraSocialLink[] = [
  {
    name: "Instagram",
    href: "#",
  },
  {
    name: "TikTok",
    href: "#",
  },
];

/* ============================================================
   LEGAL
============================================================ */

export const auraLegalLinks: AuraLegalLink[] = [
  {
    label: "Aviso legal",
    href: "/aviso-legal",
  },
  {
    label: "Privacidad",
    href: "/privacidad",
  },
  {
    label: "Cookies",
    href: "/cookies",
  },
];

/* ============================================================
   FOOTER
============================================================ */

export const auraFooter = {
  copyright: `© ${new Date().getFullYear()} Aura Studio. Todos los derechos reservados.`,

  credit: "Digital experience by WAC DS",
} as const;

/* ============================================================
   COMPLETE CONFIGURATION
============================================================ */

export const auraConfig = {
  demo: auraDemo,

  brand: auraBrand,

  seo: auraSeo,

  assets: auraAssets,

  navigation: auraNavigation,

  hero: auraHero,

  stats: auraStats,

  philosophy: auraPhilosophy,

  servicesIntro: auraServicesIntro,

  services: auraServices,

  experience: auraExperience,

  benefits: auraBenefits,

  teamIntro: auraTeamIntro,

  professionals: auraProfessionals,

  reviewsIntro: auraReviewsIntro,

  reviews: auraReviews,

  contact: auraContact,

  openingHours: auraOpeningHours,

  contactSection: auraContactSection,

  socialLinks: auraSocialLinks,

  legalLinks: auraLegalLinks,

  footer: auraFooter,
} as const;