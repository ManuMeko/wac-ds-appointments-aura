// apps/web/data/aura.ts

import type { DemoConfig } from "@/data/types";

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
   DEMO
============================================================ */

export const auraDemo = {
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

export const auraSeo = {
  siteName: "Aura Studio",

  defaultTitle: "Aura Studio · Estética, belleza y bienestar",

  titleTemplate: "%s · Aura Studio",

  description:
    "Estudio de estética y bienestar con tratamientos personalizados, atención cercana y una experiencia de cuidado diseñada alrededor de cada persona.",

  locale: "es_ES",

  language: "es",

  ogImage: "/images/aura/og-aura-studio.png",

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

  themeColor: "#f8f5f0",

  robots: {
    index: false,
    follow: false,
  },
};

/* ============================================================
   ASSETS
============================================================ */

export const auraAssets: DemoConfig["assets"] = {
  hero: "/images/aura/hero-aura-studio.png",

  experience: "/images/aura/experience-aura-studio.png",

  openGraph: "/images/aura/og-aura-studio.png",

  team: {
    "laura-martin": "/images/aura/team/laura-martin.png",
    "andrea-lopez": "/images/aura/team/andrea-lopez.png",
    "sofia-garcia": "/images/aura/team/sofia-garcia.png",
  },
};

/* ============================================================
   NAVIGATION
============================================================ */

export const auraNavigation = [
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

export const auraStats = [
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

  featuredLabel: "Destacado",

  cta: {
    label: "Consultar todos los tratamientos",
    href: "#contacto",
  },
} as const;

export const auraServices = [
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

  badge: "Ritual Aura",

  note: "Cada sesión está diseñada para que el tratamiento empiece antes incluso de entrar en cabina.",
} as const;

/* ============================================================
   BENEFITS
============================================================ */

export const auraBenefits = [
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

export const auraProfessionals = [
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

  aside:
    "La experiencia se mide también en cómo te sientes al salir y en las ganas de volver.",
} as const;

export const auraReviews = [
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

export const auraContact = {
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

export const auraOpeningHours = [
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

  phoneLabel: "Teléfono",

  emailLabel: "Email",

  addressLabel: "Dirección",

  hoursLabel: "Horario",

  directContactLabel: "Contacto directo",

  closingNote:
    "Tu cita empieza con una conversación, no con un tratamiento.",

  guidanceTitle: "Atención personalizada",

  guidanceBody:
    "Si no sabes qué tratamiento elegir, escríbenos. Nuestro equipo puede orientarte antes de reservar.",

  guidanceCta: "Resolver dudas",
} as const;

/* ============================================================
   SOCIAL
============================================================ */

export const auraSocialLinks = [
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

export const auraLegalLinks = [
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
   COPY
============================================================ */

const auraVocabulary = {
  service: "tratamiento",
  services: "tratamientos",
  serviceTitle: "Tratamiento",
} as const;

const auraHeader = {
  bookingCta: "Pedir cita",
} as const;

const auraImageAlts = {
  hero: "Cabina premium de Aura Studio con camilla de tratamiento, iluminación cálida y elementos de bienestar",
  experience:
    "Detalle de la experiencia Aura Studio con productos de bienestar, textiles suaves y ambiente de tratamiento premium",
} as const;

const auraLegalCopy = {
  noticeObject:
    "El sitio web tiene como finalidad ofrecer información sobre Aura Studio, sus servicios, tratamientos, profesionales, horarios, medios de contacto y otras cuestiones relacionadas con su actividad.",
  noticeClosing:
    "Vuelve a descubrir nuestros tratamientos y nuestra forma de entender el bienestar.",
  demoNotices: {
    legal:
      "Aura Studio es una empresa ficticia utilizada como demostración del Modelo 01 · Appointments de WAC DS. Los datos jurídicos que aparecen entre corchetes deben sustituirse por los datos reales del titular antes de publicar este proyecto para un cliente.",
    privacy:
      "Aura Studio es una empresa ficticia utilizada como demostración comercial. Los datos jurídicos indicados entre corchetes deben sustituirse por los datos reales del responsable del tratamiento antes de publicar esta web para un cliente.",
    cookies:
      "Aura Studio es una empresa ficticia utilizada como demostración del Modelo 01 · Appointments de WAC DS. Esta política deberá adaptarse a las cookies, proveedores y servicios realmente utilizados por cada cliente antes de publicar la web.",
  },
} as const;

const auraBookingStatuses = {
  CONFIRMED: "Confirmada",
  PENDING: "Pendiente de confirmación",
  COMPLETED: "Completada",
  CANCELLED: "Cancelada",
  NO_SHOW: "No asistió",
} as const;

const auraBooking = {
  eyebrow: "Reserva online",
  titleLead: "Reserva tu",
  titleAccent: "momento.",
  description:
    "Elige tratamiento, profesional, fecha y hora. Nosotros nos encargamos del resto.",
  backPrefix: "Volver a",
  steps: ["Tratamiento", "Profesional", "Fecha y hora", "Tus datos"],
  stepHeadings: ["Paso 1 de 4", "Paso 2 de 4", "Paso 3 de 4", "Paso 4 de 4"],
  chooseServiceTitle: "Elige tu tratamiento",
  chooseServiceDescription:
    "Mostramos únicamente servicios que actualmente admiten nuevas reservas.",
  loadingServices: "Cargando tratamientos...",
  emptyServicesTitle: "No hay tratamientos disponibles",
  emptyServicesBody:
    "En este momento no hay servicios habilitados para reserva online.",
  changeService: "← Cambiar tratamiento",
  chooseProfessionalTitle: "Elige profesional",
  loadingProfessionals: "Cargando profesionales...",
  emptyProfessionalsTitle: "Sin profesionales disponibles",
  emptyProfessionalsBody:
    "No hay profesionales habilitados actualmente para este tratamiento.",
  changeProfessional: "← Cambiar profesional",
  dateTimeTitle: "Fecha y hora",
  withProfessional: "con",
  chooseDateLabel: "Elige fecha",
  chooseDateHint: "Selecciona el día que mejor te venga.",
  timesLabel: "Horarios",
  slotSingular: "hora",
  slotPlural: "horas",
  selectDateTitle: "Selecciona una fecha",
  selectDateHint: "Los horarios disponibles aparecerán aquí automáticamente.",
  loadingAvailability: "Consultando disponibilidad...",
  emptyTimesTitle: "Sin horarios disponibles",
  emptyTimesHint: "Prueba con otro día del calendario.",
  selectTimeHint: "Selecciona una hora para continuar.",
  selectedTimeLabel: "Hora seleccionada",
  continueLabel: "Continuar",
  changeDateTime: "← Cambiar fecha u hora",
  previousMonth: "Mes anterior",
  nextMonth: "Mes siguiente",
  selectDay: "Selecciona un día",
  today: "Hoy",
  noDate: "Sin fecha seleccionada",
  customerTitle: "Tus datos",
  customerDescription:
    "Solo necesitamos los datos necesarios para gestionar tu cita.",
  firstNameLabel: "Nombre *",
  lastNameLabel: "Apellidos",
  emailLabel: "Email",
  phoneLabel: "Teléfono",
  contactHint:
    "Introduce al menos un email o un teléfono para que podamos identificar tu reserva y enviarte la información de gestión.",
  notesLabel: "Notas para el centro",
  notesPlaceholder: "Opcional",
  submitting: "Reservando...",
  submit: "Confirmar reserva",
  summaryEyebrow: "Tu cita",
  summaryProfessional: "Profesional",
  summaryDate: "Fecha",
  summaryTime: "Hora",
  summaryDuration: "Duración",
  summaryPrice: "Precio",
  minuteShort: "min",
  minutes: "minutos",
  successEyebrow: "Reserva realizada",
  successTitle: "Tu cita está registrada",
  successFallback: "Hemos registrado correctamente tu reserva.",
  successService: "Tratamiento",
  successProfessional: "Profesional",
  successDateTime: "Fecha y hora",
  successStatus: "Estado",
  successReference: "Referencia",
  appointmentRef: "Cita #",
  manageCta: "Gestionar mi cita",
  bookAnother: "Reservar otra cita",
  errorLoadServices: "No se pudieron cargar los servicios disponibles.",
  errorLoadProfessionals: "No se pudieron cargar los profesionales disponibles.",
  errorLoadAvailabilityDate:
    "No se pudo consultar la disponibilidad para esa fecha.",
  errorLoadAvailability: "No se pudo consultar la disponibilidad.",
  errorMissingSelection:
    "Faltan datos de la reserva. Vuelve a seleccionar fecha y hora.",
  errorFirstName: "Introduce tu nombre para continuar.",
  errorContact:
    "Introduce al menos un email o un teléfono para poder gestionar tu cita.",
  errorCreate: "No se pudo crear la reserva.",
  statuses: auraBookingStatuses,
} as const;

const auraManageBooking = {
  backToBooking: "← Volver a reservas",
  eyebrow: "Reserva online",
  titleLead: "Gestiona tu",
  titleAccent: "cita.",
  description:
    "Consulta los detalles de tu reserva y, cuando esté permitido, cambia el horario o cancélala.",
  loading: "Recuperando tu cita...",
  missingTitle: "No encontramos una cita accesible",
  invalidTitle: "El acceso a esta cita no está disponible",
  errorTitle: "No pudimos cargar tu cita",
  invalidBody:
    "El enlace de gestión puede haber caducado, haber sido revocado o no corresponder a esta reserva. Por seguridad, no podemos mostrar otra cita.",
  missingBody:
    "Para gestionar una reserva necesitas acceder desde su enlace privado o desde el navegador donde realizaste la reserva, siempre que la credencial siga vigente.",
  credentialNote:
    "Cada credencial permite acceder únicamente a la reserva para la que fue emitida.",
  bookCta: "Reservar una cita",
  summaryEyebrow: "Tu reserva",
  withProfessional: "con",
  dateTimeLabel: "Fecha y hora",
  durationLabel: "Duración",
  minutes: "minutos",
  customerLabel: "Cliente",
  referenceLabel: "Referencia",
  appointmentRef: "Cita #",
  rescheduleEyebrow: "Reprogramar",
  rescheduleTitle: "Cambiar fecha y hora",
  rescheduleBody:
    "Puedes elegir otro hueco disponible manteniendo el mismo tratamiento y profesional.",
  rescheduleDisabled:
    "La reprogramación online no está habilitada para este negocio.",
  rescheduleClosed: "Esta cita ya no puede reprogramarse online.",
  rescheduleCta: "Cambiar horario",
  cancelEyebrow: "Cancelación",
  cancelTitle: "Cancelar la cita",
  cancelBody: "La cancelación es definitiva. El horario volverá a quedar libre.",
  cancelDisabled: "La cancelación online no está habilitada.",
  cancelClosed: "Esta cita ya no puede cancelarse online.",
  cancelCta: "Cancelar cita",
  confirmCancelTitle: "¿Cancelar esta cita?",
  confirmCancelBody: "Esta acción no se puede deshacer desde esta página.",
  cancelling: "Cancelando...",
  confirmCancel: "Sí, cancelar",
  back: "Volver",
  newDateEyebrow: "Nueva fecha",
  rescheduleFlowTitle: "Reprogramar tu cita",
  dateLabel: "Fecha",
  timesLabel: "Horarios",
  pickDateHint: "Selecciona una fecha para consultar la disponibilidad.",
  loadingAvailability: "Consultando...",
  emptyTimes: "No hay horarios disponibles para esta fecha.",
  cancelChange: "Cancelar cambio",
  confirmNewTime: "Confirmar nuevo horario",
  cancelledEyebrow: "Reserva cancelada",
  cancelledTitle: "Esta cita ya no está activa",
  cancelledBody:
    "El horario ha quedado liberado. Si quieres volver a visitarnos, puedes crear una nueva reserva.",
  backHomePrefix: "Volver a",
  bookAnother: "Reservar otra cita",
  errorLoad: "No se pudo recuperar la cita.",
  errorCredentialMismatch:
    "La credencial no corresponde a la reserva solicitada.",
  errorAvailabilityMismatch:
    "La disponibilidad recibida no corresponde a la selección.",
  errorAvailability: "No se pudo consultar la disponibilidad.",
  errorSession: "La sesión de gestión no corresponde a esta cita.",
  errorCancel: "No se pudo cancelar la cita.",
  errorRescheduleVerify:
    "El cambio se ha enviado, pero no se pudo verificar la información actualizada. Recarga la página antes de intentarlo de nuevo.",
  errorRescheduleMismatch: "La respuesta no corresponde a la cita solicitada.",
  errorReschedule: "No se pudo reprogramar la cita.",
  invalidDate: "Fecha no disponible",
  rescheduleRegionLabel: "Reprogramar cita",
} as const;

/* ============================================================
   COMPLETE CONFIGURATION
============================================================ */

export const auraConfig: DemoConfig = {
  slug: "aura-studio",

  theme: "aura",

  brandSuffix: "Studio",

  imageAlts: auraImageAlts,

  vocabulary: auraVocabulary,

  header: auraHeader,

  legalCopy: auraLegalCopy,

  booking: auraBooking,

  manageBooking: auraManageBooking,

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
};