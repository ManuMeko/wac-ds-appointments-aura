/**
 * Contrato compartido por las demos del Modelo 01.
 * Aura lo cumple hoy. El taller usará el mismo tipo.
 */

export type DemoId = "aura" | "taller";

export type NavigationItem = {
  label: string;
  href: string;
};

export type Stat = {
  value: string;
  label: string;
  unit?: string;
};

export type TradeIconName =
  | "revision"
  | "oil"
  | "itv"
  | "tires"
  | "brakes"
  | "diagnosis"
  | "quote"
  | "parts"
  | "schedule";

export type Service = {
  id: string;
  number: string;
  name: string;
  description: string;
  duration: string;
  price: string;
  category: string;
  featured: boolean;
  icon?: TradeIconName;
};

export type Professional = {
  id: string;
  name: string;
  role: string;
  initials: string;
  description: string;
  specialties: string[];
  image?: string;
};

export type Review = {
  id: string;
  author: string;
  quote: string;
  rating: number;
};

export type Benefit = {
  id: string;
  label: string;
  icon?: TradeIconName;
};

export type OpeningHour = {
  days: string;
  hours: string;
};

export type Contact = {
  phoneDisplay: string;
  phoneHref: string;
  whatsappNumber: string;
  email: string;
  addressLine1: string;
  addressLine2: string;
  city: string;
  country: string;
};

export type SocialLink = {
  name: string;
  href: string;
};

export type LegalLink = {
  label: string;
  href: string;
};

export type SeoConfig = {
  siteName: string;
  defaultTitle: string;
  titleTemplate: string;
  description: string;
  locale: string;
  language: string;
  ogImage: string;
  category: string;
  keywords: readonly string[];
  themeColor: string;
  backgroundColor?: string;
  schemaType?: string | readonly string[];
  priceRange?: string;
  areaServed?: string;
  geo?: {
    latitude: number;
    longitude: number;
  };
  robots: {
    index: boolean;
    follow: boolean;
  };
};

export type AssetsConfig = {
  hero: string;
  experience: string;
  openGraph: string;
  team: Record<string, string>;
};

export type DemoMeta = {
  enabled: boolean;
  label: string;
  legalNotice: string;
};

export type ImageAlts = {
  hero: string;
  experience: string;
};

export type Vocabulary = {
  service: string;
  services: string;
  serviceTitle: string;
};

export type HeaderCopy = {
  bookingCta: string;
};

export type BookingExtraField = {
  id: string;
  label: string;
  placeholder: string;
  notesLabel: string;
};

export type LegalCopy = {
  noticeObject: string;
  noticeClosing: string;
  demoNotices: {
    legal: string;
    privacy: string;
    cookies: string;
  };
};

export type BookingStatuses = {
  CONFIRMED: string;
  PENDING: string;
  COMPLETED: string;
  CANCELLED: string;
  NO_SHOW: string;
};

export type BookingHeroSequence = {
  basePath: string;
  frameCount: number;
  durationMs: number;
  fileExtension?: "webp";
};

export type BookingCopy = {
  eyebrow: string;
  titleLead: string;
  titleAccent: string;
  description: string;
  /** Fotogramas de la cabecera. Si no está, la cabecera gris se mantiene. */
  heroSequence?: BookingHeroSequence;
  /** Fracciones 0–1 del desmontaje: pasos 1–4 y la confirmación. */
  stepFrames?: readonly number[];
  /** Rótulos de la miniatura, uno por paso. */
  stepPhaseLabels?: readonly string[];
  successVisualTitle?: string;
  backPrefix: string;
  steps: readonly string[];
  stepHeadings: readonly string[];
  chooseServiceTitle: string;
  chooseServiceDescription: string;
  loadingServices: string;
  emptyServicesTitle: string;
  emptyServicesBody: string;
  changeService: string;
  chooseProfessionalTitle: string;
  chooseProfessionalDescription: string;
  loadingProfessionals: string;
  emptyProfessionalsTitle: string;
  emptyProfessionalsBody: string;
  changeProfessional: string;
  dateTimeTitle: string;
  withProfessional: string;
  dateTimePrompt: string;
  chooseDateLabel: string;
  chooseDateHint: string;
  timesLabel: string;
  slotSingular: string;
  slotPlural: string;
  selectDateTitle: string;
  selectDateHint: string;
  loadingAvailability: string;
  emptyTimesTitle: string;
  emptyTimesHint: string;
  selectTimeHint: string;
  selectedTimeLabel: string;
  continueLabel: string;
  changeDateTime: string;
  previousMonth: string;
  nextMonth: string;
  selectDay: string;
  today: string;
  noDate: string;
  customerTitle: string;
  customerDescription: string;
  firstNameLabel: string;
  lastNameLabel: string;
  emailLabel: string;
  phoneLabel: string;
  contactHint: string;
  notesLabel: string;
  notesPlaceholder: string;
  submitting: string;
  submit: string;
  summaryEyebrow: string;
  summaryProfessional: string;
  summaryDate: string;
  summaryTime: string;
  summaryDuration: string;
  summaryPrice: string;
  minuteShort: string;
  minutes: string;
  successEyebrow: string;
  successTitle: string;
  successFallback: string;
  successService: string;
  successProfessional: string;
  successDateTime: string;
  successStatus: string;
  successReference: string;
  appointmentRef: string;
  manageCta: string;
  bookAnother: string;
  errorLoadServices: string;
  errorLoadProfessionals: string;
  errorLoadAvailabilityDate: string;
  errorLoadAvailability: string;
  errorMissingSelection: string;
  errorFirstName: string;
  errorContact: string;
  errorCreate: string;
  statuses: BookingStatuses;
};

export type ManageBookingCopy = {
  backToBooking: string;
  eyebrow: string;
  titleLead: string;
  titleAccent: string;
  description: string;
  loading: string;
  missingTitle: string;
  invalidTitle: string;
  errorTitle: string;
  invalidBody: string;
  missingBody: string;
  credentialNote: string;
  bookCta: string;
  summaryEyebrow: string;
  withProfessional: string;
  dateTimeLabel: string;
  durationLabel: string;
  minutes: string;
  customerLabel: string;
  referenceLabel: string;
  appointmentRef: string;
  rescheduleEyebrow: string;
  rescheduleTitle: string;
  rescheduleBody: string;
  rescheduleDisabled: string;
  rescheduleClosed: string;
  rescheduleCta: string;
  cancelEyebrow: string;
  cancelTitle: string;
  cancelBody: string;
  cancelDisabled: string;
  cancelClosed: string;
  cancelCta: string;
  confirmCancelTitle: string;
  confirmCancelBody: string;
  cancelling: string;
  confirmCancel: string;
  back: string;
  newDateEyebrow: string;
  rescheduleFlowTitle: string;
  dateLabel: string;
  timesLabel: string;
  pickDateHint: string;
  loadingAvailability: string;
  emptyTimes: string;
  cancelChange: string;
  confirmNewTime: string;
  cancelledEyebrow: string;
  cancelledTitle: string;
  cancelledBody: string;
  backHomePrefix: string;
  bookAnother: string;
  errorLoad: string;
  errorCredentialMismatch: string;
  errorAvailabilityMismatch: string;
  errorAvailability: string;
  errorSession: string;
  errorCancel: string;
  errorRescheduleVerify: string;
  errorRescheduleMismatch: string;
  errorReschedule: string;
  invalidDate: string;
  rescheduleRegionLabel: string;
};

/**
 * Fotogramas que avanzan con el scroll.
 * Si una demo no lo define, la home no monta el bloque.
 * `from` y `to` van de 0 a 1 sobre el recorrido de la sección.
 */
export type ScrollSequenceOverlay = {
  from: number;
  to: number;
  title: string;
  text?: string;
  align?: "start" | "end";
  level?: "h1" | "h2";
  primaryCta?: { label: string; href: string };
  secondaryCta?: { label: string; href: string };
};

export type ScrollSequenceConfig = {
  basePath: string;
  frameCount: number;
  fileExtension: "webp";
  heightVh: number;
  /** Recorrido extra, en vh, con el último fotograma quieto. */
  holdVh?: number;
  overlays?: readonly ScrollSequenceOverlay[];
};

export type DemoConfig = {
  slug: string;
  theme: DemoId;
  brandSuffix: string;
  imageAlts: ImageAlts;
  vocabulary: Vocabulary;
  header: HeaderCopy;
  bookingExtraFields: readonly BookingExtraField[];
  legalCopy: LegalCopy;
  booking: BookingCopy;
  manageBooking: ManageBookingCopy;
  demo: DemoMeta;
  brand: {
    name: string;
    shortName: string;
    monogram: string;
    tagline: string;
    description: string;
    locationLabel: string;
    signature: {
      eyebrow: string;
      title: string;
    };
  };
  seo: SeoConfig;
  assets: AssetsConfig;
  navigation: readonly NavigationItem[];
  hero: {
    eyebrow: string;
    title: string;
    highlightedTitle: string;
    description: string;
    primaryCta: { label: string; href: string };
    secondaryCta: { label: string; href: string };
  };
  scrollSequence?: ScrollSequenceConfig;
  stats: readonly Stat[];
  /** Franja de cifras entre experiencia y equipo. Si no se define, no se monta. */
  proofStats?: readonly Stat[];
  philosophy: {
    eyebrow: string;
    title: string;
    highlightedTitle: string;
    description: string;
    steps?: readonly {
      number: string;
      title: string;
      text: string;
      icon: TradeIconName;
    }[];
  };
  servicesIntro: {
    eyebrow: string;
    title: string;
    description: string;
    featuredLabel: string;
    cta: { label: string; href: string };
  };
  services: readonly Service[];
  experience: {
    eyebrow: string;
    title: string;
    description: string;
    visualTitle: string;
    visualDescription: string;
    badge: string;
    note: string;
    image?: string;
  };
  benefits: readonly Benefit[];
  teamIntro: {
    eyebrow: string;
    title: string;
    description: string;
  };
  professionals: readonly Professional[];
  reviewsIntro: {
    eyebrow: string;
    title: string;
    aside: string;
  };
  reviews: readonly Review[];
  contact: Contact;
  openingHours: readonly OpeningHour[];
  contactSection: {
    eyebrow: string;
    title: string;
    description: string;
    whatsappCta: { label: string };
    phoneCta: { label: string };
    phoneLabel: string;
    emailLabel: string;
    addressLabel: string;
    hoursLabel: string;
    directContactLabel: string;
    closingNote: string;
    guidanceTitle: string;
    guidanceBody: string;
    guidanceCta: string;
  };
  socialLinks: readonly SocialLink[];
  legalLinks: readonly LegalLink[];
  footer: {
    copyright: string;
    credit: string;
  };
};
