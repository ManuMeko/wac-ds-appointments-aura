// apps/web/data/taller.ts

import type { DemoConfig } from "@/data/types";

/**
 * Motorline Taller · Demo del Modelo 01.
 * Misma forma que Aura. El catálogo de /reservar sale del API.
 */

export const tallerConfig: DemoConfig = {
  slug: "motorline-taller",
  theme: "taller",
  brandSuffix: "Taller",

  imageAlts: {
    hero: "Taller Motorline con un coche en el elevador y el banco de diagnosis al fondo",
    experience:
      "Coche en el elevador de Motorline, con el tren de rodaje y el trabajo a la vista",
  },

  vocabulary: {
    service: "servicio",
    services: "servicios",
    serviceTitle: "Servicio",
  },

  header: {
    bookingCta: "Reservar cita en el taller",
  },

  legal: {
    legalName: "[RAZÓN SOCIAL]",
    taxId: "[NIF / CIF]",
    address: "[DOMICILIO FISCAL / SOCIAL]",
    registry: "[DATOS REGISTRALES]",
    privacyEmail: "hola@motorline-taller.example",
    hostingProvider: "[PROVEEDOR DE HOSTING]",
    hostingCountry: "[PAÍS DEL HOSTING]",
    emailProvider: "[PROVEEDOR DE EMAIL]",
    retention: "[PLAZO DE CONSERVACIÓN]",
  },

  bookingExtraFields: [
    {
      id: "plate",
      label: "Matrícula",
      placeholder: "1234 ABC",
      notesLabel: "Matrícula",
    },
    {
      id: "model",
      label: "Marca y modelo",
      placeholder: "Seat León",
      notesLabel: "Modelo",
    },
  ],

  legalCopy: {
    noticeObject:
      "El sitio web tiene como finalidad ofrecer información sobre Motorline Taller, sus servicios de mecánica, profesionales, horarios, medios de contacto y otras cuestiones relacionadas con su actividad.",
    noticeClosing:
      "Vuelve cuando quieras: te explicamos el trabajo antes de hacerlo y el precio antes de empezar.",
    privacyClosing:
      "Tus datos se usan para gestionar la cita y avisarte de lo que corresponda.",
    demoNotices: {
      legal:
        "Motorline Taller es una empresa ficticia utilizada como demostración del Modelo 01 · Appointments de WAC DS. Los datos jurídicos que aparecen entre corchetes deben sustituirse por los datos reales del titular antes de publicar este proyecto para un cliente.",
      privacy:
        "Motorline Taller es una empresa ficticia utilizada como demostración comercial. Los datos jurídicos indicados entre corchetes deben sustituirse por los datos reales del responsable del tratamiento antes de publicar esta web para un cliente.",
      cookies:
        "Motorline Taller es una empresa ficticia utilizada como demostración del Modelo 01 · Appointments de WAC DS. Esta política deberá adaptarse a las cookies, proveedores y servicios realmente utilizados por cada cliente antes de publicar la web.",
    },
  },

  booking: {
    eyebrow: "Cita en el taller",
    titleLead: "Reserva tu",
    titleAccent: "cita.",
    description:
      "Elige servicio, profesional, fecha y hora. Te confirmamos el trabajo antes de tocar el coche.",
    heroSequence: {
      basePath: "/images/taller/sequence",
      frameCount: 180,
      durationMs: 6000,
      fileExtension: "webp" as const,
    },
    stepFrames: [0, 0.2, 0.42, 0.64, 1],
    stepPhaseLabels: [
      "Fase 1/4 · Recepción",
      "2/4 · Ruedas y frenos",
      "3/4 · Carrocería",
      "4/4 · Mecánica",
    ],
    successVisualTitle: "Cada pieza, a la vista.",
    backPrefix: "Volver a",
    steps: ["Servicio", "Profesional", "Fecha y hora", "Tus datos"],
    stepHeadings: ["Paso 1 de 4", "Paso 2 de 4", "Paso 3 de 4", "Paso 4 de 4"],
    chooseServiceTitle: "Elige tu servicio",
    chooseServiceDescription:
      "Elige qué necesita tu coche. Te confirmamos precio y tiempo antes de empezar.",
    loadingServices: "Cargando servicios...",
    emptyServicesTitle: "No hay servicios disponibles",
    emptyServicesBody:
      "En este momento no hay servicios habilitados para reserva online.",
    changeService: "← Cambiar servicio",
    chooseProfessionalTitle: "Elige profesional",
    chooseProfessionalDescription:
      "Elige quién quieres que se encargue, o deja que asignemos al primero disponible.",
    loadingProfessionals: "Cargando profesionales...",
    emptyProfessionalsTitle: "Sin profesionales disponibles",
    emptyProfessionalsBody:
      "No hay profesionales habilitados actualmente para este servicio.",
    changeProfessional: "← Cambiar profesional",
    dateTimeTitle: "Fecha y hora",
    withProfessional: "con",
    dateTimePrompt: "Elige el día y la hora que mejor te vengan.",
    chooseDateLabel: "Elige fecha",
    chooseDateHint: "Selecciona el día que mejor te venga.",
    timesLabel: "Horarios",
    slotSingular: "hueco libre",
    slotPlural: "huecos libres",
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
      "Último paso. Solo necesitamos tus datos y la matrícula para preparar la bahía.",
    firstNameLabel: "Nombre *",
    lastNameLabel: "Apellidos",
    emailLabel: "Email",
    phoneLabel: "Teléfono",
    contactHint:
      "Con uno basta. Te enviaremos la confirmación y podrás gestionar la cita.",
    notesLabel: "Notas para el taller",
    notesPlaceholder: "Opcional. Por ejemplo, un ruido, un testigo o desde cuándo lo notas.",
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
    successService: "Servicio",
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
    statuses: {
      CONFIRMED: "Confirmada",
      PENDING: "Pendiente de confirmación",
      COMPLETED: "Completada",
      CANCELLED: "Cancelada",
      NO_SHOW: "No asistió",
    },
  },

  manageBooking: {
    backToBooking: "← Volver a reservas",
    eyebrow: "Cita en el taller",
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
      "Puedes elegir otro hueco disponible manteniendo el mismo servicio y profesional.",
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
  },

  demo: {
    enabled: true,
    label: "Demo comercial WAC DS",
    legalNotice:
      "Motorline Taller es una empresa ficticia utilizada como demostración del Modelo 01 · Appointments de WAC DS.",
  },

  brand: {
    name: "Motorline Taller",
    shortName: "Motorline",
    monogram: "M",
    tagline: "Taller · Mecánica · Confianza",
    description:
      "Mecánica de confianza en Madrid. Te decimos qué tiene el coche, qué cuesta y cuándo lo tienes.",
    locationLabel: "Madrid · España",
    signature: {
      eyebrow: "Motorline",
      title: "Sin sorpresas en la factura.",
    },
  },

  seo: {
    siteName: "Motorline Taller",
    defaultTitle: "Motorline Taller · Mecánica de confianza en Madrid",
    titleTemplate: "%s · Motorline Taller",
    description:
      "Taller mecánico en Madrid para revisiones, mantenimiento, frenos, neumáticos y diagnosis. Cita online y presupuesto claro.",
    locale: "es_ES",
    language: "es",
    ogImage: "/images/taller/og.webp",
    category: "automotive",
    keywords: [
      "Motorline Taller",
      "taller mecánico",
      "taller Madrid",
      "revisión del coche",
      "cambio de aceite",
      "pre-ITV",
      "neumáticos",
      "frenos",
      "diagnosis electrónica",
      "cita taller",
    ],
    themeColor: "#f4f4f5",
    backgroundColor: "#1c1917",
    schemaType: "AutoRepair",
    priceRange: "€€",
    areaServed: "Madrid",
    robots: {
      index: false,
      follow: false,
    },
  },

  assets: {
    hero: "/images/taller/hero-motorline.svg",
    experience: "/images/taller/elevador.webp",
    openGraph: "/images/taller/og.webp",
    team: {
      "javier-ruiz": "/images/taller/team-javier.webp",
      "carlos-mendez": "/images/taller/team-carlos.webp",
      "lucia-torres": "/images/taller/team-lucia.webp",
    },
  },

  navigation: [
    { label: "Servicios", href: "/#servicios" },
    { label: "Experiencia", href: "/#experiencia" },
    { label: "Equipo", href: "/#equipo" },
    { label: "Opiniones", href: "/#opiniones" },
    { label: "Contacto", href: "/#contacto" },
  ],

  hero: {
    eyebrow: "Taller · Mecánica · Madrid",
    title: "Tu coche,",
    highlightedTitle: "en manos de confianza.",
    description:
      "Revisiones, mantenimiento y diagnosis con un criterio claro: te explicamos el trabajo, el plazo y el precio antes de empezar.",
    primaryCta: { label: "Reservar cita en el taller", href: "/reservar" },
    secondaryCta: { label: "Ver servicios", href: "#servicios" },
  },

  scrollSequence: {
    basePath: "/images/taller/sequence",
    frameCount: 180,
    fileExtension: "webp",
    heightVh: 280,
    holdVh: 10,
    overlays: [
      {
        from: 0,
        to: 0.18,
        align: "start",
        level: "h1",
        title: "Tu coche, en manos de confianza.",
        text: "Taller de mecánica en Madrid. Revisiones, mantenimiento y diagnosis.",
        primaryCta: { label: "Reservar cita", href: "/reservar" },
        secondaryCta: { label: "Ver servicios", href: "#servicios" },
      },
      {
        from: 0.2,
        to: 0.38,
        align: "end",
        title: "Ruedas, frenos y suspensión.",
        text: "Revisamos discos, pinzas y amortiguadores pieza a pieza.",
      },
      {
        from: 0.4,
        to: 0.58,
        align: "start",
        title: "Carrocería fuera, sin prisas.",
        text: "Accedemos a lo que hace falta, nada más.",
      },
      {
        from: 0.6,
        to: 0.8,
        align: "end",
        title: "Motor, caja y refrigeración.",
        text: "Diagnosis electrónica y mecánica antes de tocar nada.",
      },
      {
        from: 0.82,
        to: 1,
        align: "start",
        title: "Cada pieza, a la vista. Sin sorpresas en la factura.",
        text: "Te enseñamos qué se ha cambiado y por qué.",
        primaryCta: { label: "Reservar cita", href: "/reservar" },
      },
    ],
  },

  stats: [
    { value: "+15", label: "Años de taller" },
    { value: "4.8", label: "Valoración" },
    { value: "3", label: "Especialistas" },
    { value: "0", label: "Cargos ocultos" },
  ],

  proofStats: [
    { value: "18", label: "años en el taller" },
    { value: "+4.200", label: "coches revisados al año" },
    { value: "24", unit: "meses", label: "de garantía en reparaciones" },
    { value: "98", unit: "%", label: "de clientes que repiten" },
  ],

  philosophy: {
    eyebrow: "Cómo trabajamos",
    title: "Primero el diagnóstico.",
    highlightedTitle: "Después, el presupuesto.",
    description:
      "No cambiamos una pieza porque sí. Si el coche solo necesita un ajuste, eso es lo que hacemos. Y si hace falta una reparación, te decimos por qué.",
    steps: [
      {
        number: "01",
        title: "Diagnóstico",
        text: "Revisamos y te explicamos qué tiene el coche.",
        icon: "diagnosis",
      },
      {
        number: "02",
        title: "Presupuesto",
        text: "Precio cerrado antes de tocar nada.",
        icon: "quote",
      },
      {
        number: "03",
        title: "Reparación",
        text: "Solo lo necesario, con las piezas a la vista.",
        icon: "parts",
      },
    ],
  },

  servicesIntro: {
    eyebrow: "Servicios",
    title: "Trabajo de taller, explicado.",
    description:
      "Mantenimiento, seguridad y diagnosis. Precios de demostración, para que veas el modelo con un taller real.",
    featuredLabel: "Habitual",
    cta: { label: "Pedir cita", href: "/reservar" },
  },

  services: [
    {
      id: "revision-oficial",
      number: "01",
      name: "Revisión oficial",
      description:
        "Puntos de control del fabricante, niveles y un informe claro de lo que está bien y de lo que conviene vigilar.",
      duration: "120 min",
      price: "149 €",
      category: "Mantenimiento",
      featured: true,
      icon: "revision",
    },
    {
      id: "aceite-filtros",
      number: "02",
      name: "Cambio de aceite y filtros",
      description:
        "Aceite, filtro de aceite y filtro de habitáculo. Te decimos la especificación que corresponde a tu motor.",
      duration: "45 min",
      price: "69 €",
      category: "Mantenimiento",
      featured: true,
      icon: "oil",
    },
    {
      id: "pre-itv",
      number: "03",
      name: "Pre-ITV",
      description:
        "Revisión previa de luces, frenos, neumáticos y emisiones para llegar a la ITV sin improvisar.",
      duration: "30 min",
      price: "35 €",
      category: "Inspección",
      featured: false,
      icon: "itv",
    },
    {
      id: "neumaticos",
      number: "04",
      name: "Neumáticos: montaje y equilibrado",
      description:
        "Desmontaje, montaje, equilibrado y apriete. Comprobamos la presión y el desgaste del eje.",
      duration: "45 min",
      price: "40 €",
      category: "Neumáticos",
      featured: false,
      icon: "tires",
    },
    {
      id: "frenos",
      number: "05",
      name: "Frenos: pastillas y discos",
      description:
        "Medimos espesor y sustitución de pastillas o discos cuando el desgaste lo pide, no antes.",
      duration: "90 min",
      price: "120 €",
      category: "Seguridad",
      featured: true,
      icon: "brakes",
    },
    {
      id: "diagnosis",
      number: "06",
      name: "Diagnosis electrónica",
      description:
        "Lectura de averías, datos en vivo y una explicación en claro de qué significa el testigo.",
      duration: "30 min",
      price: "45 €",
      category: "Diagnosis",
      featured: true,
      icon: "diagnosis",
    },
  ],

  experience: {
    eyebrow: "En el taller",
    title: "Sabes qué se ha hecho y por qué.",
    description:
      "Recibes el coche, lo revisamos y te contamos el hallazgo antes de seguir. Si aparece algo que no estaba en la cita, paramos y te llamamos.",
    visualTitle: "El elevador, a la vista.",
    visualDescription:
      "Puedes ver el trabajo. No hace falta adivinar qué se ha tocado.",
    badge: "Presupuesto antes de reparar",
    note: "Cada cita empieza por identificar el coche y el motivo de la visita, no por desmontar.",
    image: "/images/taller/elevador.webp",
  },

  benefits: [
    { id: "presupuesto", label: "Presupuesto antes de reparar", icon: "quote" },
    { id: "diagnosis", label: "Diagnosis explicada", icon: "diagnosis" },
    { id: "piezas", label: "Piezas que se pueden ver", icon: "parts" },
    { id: "plazo", label: "Plazo de entrega concreto", icon: "schedule" },
  ],

  teamIntro: {
    eyebrow: "El equipo",
    title: "Quien toca el coche te lo explica.",
    description:
      "Tres especialistas. Cada uno con su área, y todos con el mismo criterio: no vender una reparación que el coche no necesita.",
  },

  professionals: [
    {
      id: "javier-ruiz",
      name: "Javier Ruiz",
      role: "Jefe de taller",
      initials: "JR",
      description:
        "Coordina el taller y se ocupa de la diagnosis. Traduce los códigos de avería a una decisión concreta.",
      specialties: ["Diagnosis", "Revisión oficial", "Organización del taller"],
      image: "/images/taller/team-javier.webp",
    },
    {
      id: "carlos-mendez",
      name: "Carlos Méndez",
      role: "Mantenimiento y frenos",
      initials: "CM",
      description:
        "Mantenimiento periódico y sistema de frenos. Mide antes de sustituir y deja el recambio a la vista.",
      specialties: ["Aceite y filtros", "Frenos", "Revisiones"],
      image: "/images/taller/team-carlos.webp",
    },
    {
      id: "lucia-torres",
      name: "Lucía Torres",
      role: "Neumáticos y pre-ITV",
      initials: "LT",
      description:
        "Neumáticos, equilibrado y la revisión previa a la ITV. Te dice qué fallaría y qué puede esperar.",
      specialties: ["Neumáticos", "Pre-ITV", "Seguridad"],
      image: "/images/taller/team-lucia.webp",
    },
  ],

  reviewsIntro: {
    eyebrow: "Opiniones",
    title: "Lo que cuenta es salir sabiendo qué se ha hecho.",
    aside:
      "Ejemplos de demostración. En un taller real, estas opiniones serían de clientes.",
  },

  reviews: [
    {
      id: "andres-p",
      author: "Andrés P.",
      quote:
        "Me enseñaron la pastilla gastada antes de cambiarla. El precio fue el que habían dicho por teléfono.",
      rating: 5,
    },
    {
      id: "nuria-s",
      author: "Nuria S.",
      quote:
        "Fui por un testigo del motor. Me explicaron qué era, qué no era urgente y cuánto costaba mirarlo.",
      rating: 5,
    },
    {
      id: "diego-l",
      author: "Diego L.",
      quote:
        "La pre-ITV me ahorró la segunda visita. Luces y un neumático, nada más. Sin lista de extras.",
      rating: 5,
    },
  ],

  contact: {
    phoneDisplay: "+34 910 000 000",
    phoneHref: "+34910000000",
    whatsappNumber: "34910000000",
    email: "hola@motorline-taller.example",
    addressLine1: "Calle del Taller, 8",
    addressLine2: "28045 Madrid",
    city: "Madrid",
    country: "España",
  },

  openingHours: [
    { days: "Lunes — Viernes", hours: "8:00 — 18:00" },
    { days: "Sábado", hours: "9:00 — 13:00" },
    { days: "Domingo", hours: "Cerrado" },
  ],

  contactSection: {
    eyebrow: "Cita y contacto",
    title: "Cuéntanos qué le pasa al coche.",
    description:
      "Llama, escribe o reserva hora. Si nos dices la matrícula y el síntoma, preparamos la bahía antes de que llegues.",
    whatsappCta: { label: "Escribir por WhatsApp" },
    phoneCta: { label: "Llamar al taller" },
    phoneLabel: "Teléfono",
    emailLabel: "Email",
    addressLabel: "Dirección",
    hoursLabel: "Horario",
    directContactLabel: "Contacto directo",
    closingNote:
      "La cita empieza por el síntoma y el coche, no por una reparación cerrada.",
    guidanceTitle: "Si no sabes qué pedir",
    guidanceBody:
      "Describe el ruido, el testigo o desde cuándo lo notas. Te orientamos al servicio que corresponde.",
    guidanceCta: "Consultar el síntoma",
  },

  socialLinks: [
    { name: "Instagram", href: "#" },
    { name: "TikTok", href: "#" },
  ],

  legalLinks: [
    { label: "Aviso legal", href: "/aviso-legal" },
    { label: "Privacidad", href: "/privacidad" },
    { label: "Cookies", href: "/cookies" },
  ],

  footer: {
    copyright: `© ${new Date().getFullYear()} Motorline Taller. Todos los derechos reservados.`,
    credit: "Digital experience by WAC DS",
  },
};
