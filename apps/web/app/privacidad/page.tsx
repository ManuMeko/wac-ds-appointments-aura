// apps/web/app/privacidad/page.tsx

import type { Metadata } from "next";
import Link from "next/link";

import { ArrowIcon } from "@/components/ui/ArrowIcon";
import { SiteFooter } from "@/components/layout/SiteFooter";
import { SiteHeader } from "@/components/layout/SiteHeader";
import { business } from "@/config/business";

/* =========================================================
   WAC DS · MODEL 01 · APPOINTMENTS
   Aura Studio · Privacy Policy
========================================================= */

export const metadata: Metadata = {
  title: "Política de privacidad",
  description:
    `Información sobre el tratamiento de datos personales en el sitio web de ${business.brand.name}.`,

  robots: {
    index: business.seo.robots.index,
    follow: business.seo.robots.follow,
  },
};

const sections = [
  {
    id: "responsable",
    number: "01",
    title: "Responsable del tratamiento",
  },
  {
    id: "datos",
    number: "02",
    title: "Datos personales tratados",
  },
  {
    id: "finalidades",
    number: "03",
    title: "Finalidades del tratamiento",
  },
  {
    id: "legitimacion",
    number: "04",
    title: "Base jurídica",
  },
  {
    id: "conservacion",
    number: "05",
    title: "Plazo de conservación",
  },
  {
    id: "destinatarios",
    number: "06",
    title: "Destinatarios y terceros",
  },
  {
    id: "derechos",
    number: "07",
    title: "Derechos de las personas usuarias",
  },
  {
    id: "seguridad",
    number: "08",
    title: "Seguridad de la información",
  },
  {
    id: "menores",
    number: "09",
    title: "Menores de edad",
  },
  {
    id: "cambios",
    number: "10",
    title: "Cambios en esta política",
  },
];

export default function PrivacyPage() {
  return (
    <div className="min-h-screen bg-(--canvas) text-(--inverse)">
      <SiteHeader />

      <main>
        {/* =====================================================
            HERO
        ====================================================== */}
        <section className="relative overflow-hidden border-b border-(--heading)/10 bg-(--hero-bg)">
          <div className="pointer-events-none absolute -right-32 -top-48 h-[560px] w-[560px] rounded-full bg-(--p-d9c3ae)/55 blur-3xl" />

          <div className="pointer-events-none absolute -bottom-60 -left-40 h-[560px] w-[560px] rounded-full bg-white/30 blur-3xl" />

          <div className="relative mx-auto w-full max-w-[1440px] px-5 py-20 sm:px-8 lg:px-12 lg:py-28 xl:px-16">
            <Link
              href="/"
              className="group inline-flex items-center gap-2 text-[10px] font-semibold uppercase tracking-[0.2em] text-(--p-806b5c) transition-colors duration-200 hover:text-(--p-4d4037)"
            >
              <span className="rotate-180 transition-transform duration-300 group-hover:-translate-x-1">
                <ArrowIcon />
              </span>

              Volver a {business.brand.shortName}
            </Link>

            <div className="mt-12 grid gap-10 lg:grid-cols-[0.72fr_1.28fr]">
              <div>
                <p className="text-[10px] font-semibold uppercase tracking-[0.27em] text-(--p-89715f)">
                  Protección de datos
                </p>
              </div>

              <div>
                <h1 className="max-w-[850px] font-serif text-5xl leading-[0.95] tracking-[-0.04em] text-(--ink) sm:text-6xl lg:text-7xl">
                  Política de privacidad
                </h1>

                <p className="mt-7 max-w-[700px] text-base leading-8 text-(--p-6e6158)">
                  Información sobre cómo se recopilan, utilizan, conservan y
                  protegen los datos personales de las personas usuarias.
                </p>
              </div>
            </div>
          </div>
        </section>

        {/* =====================================================
            DEMO NOTICE
        ====================================================== */}
        <section className="bg-(--canvas)">
          <div className="mx-auto w-full max-w-[1440px] px-5 pt-12 sm:px-8 lg:px-12 xl:px-16">
            <div className="rounded-[1.4rem] border border-(--p-9b775e)/20 bg-(--notice-bg) p-5 sm:p-6">
              <p className="text-[9px] font-semibold uppercase tracking-[0.22em] text-(--accent)">
                {business.demo.label}
              </p>

              <p className="mt-3 max-w-[900px] text-sm leading-6 text-(--p-66584f)">
                {business.legalCopy.demoNotices.privacy}
              </p>
            </div>
          </div>
        </section>

        {/* =====================================================
            PRIVACY CONTENT
        ====================================================== */}
        <section className="bg-(--canvas)">
          <div className="mx-auto grid w-full max-w-[1440px] gap-12 px-5 py-20 sm:px-8 lg:grid-cols-[0.32fr_0.68fr] lg:px-12 lg:py-24 xl:px-16">
            {/* Index */}
            <aside className="lg:sticky lg:top-[108px] lg:self-start">
              <p className="text-[10px] font-semibold uppercase tracking-[0.22em] text-(--p-9a806d)">
                Contenido
              </p>

              <nav
                aria-label="Índice de la política de privacidad"
                className="mt-6 border-t border-(--heading)/10"
              >
                {sections.map((section) => (
                  <a
                    key={section.id}
                    href={`#${section.id}`}
                    className="group flex items-start gap-4 border-b border-(--heading)/10 py-4"
                  >
                    <span className="font-serif text-sm text-(--p-a18a79)">
                      {section.number}
                    </span>

                    <span className="text-sm leading-5 text-(--p-62564e) transition-colors duration-200 group-hover:text-(--p-2d2723)">
                      {section.title}
                    </span>
                  </a>
                ))}
              </nav>
            </aside>

            {/* Document */}
            <article className="max-w-[860px]">
              <section
                id="responsable"
                className="scroll-mt-28 border-b border-(--heading)/10 pb-12"
              >
                <LegalSectionHeader
                  number="01"
                  title="Responsable del tratamiento"
                />

                <p className="mt-6 text-base leading-8 text-(--ink-muted)">
                  El responsable del tratamiento de los datos personales
                  facilitados a través de este sitio web será:
                </p>

                <dl className="mt-8 overflow-hidden rounded-[1.4rem] border border-(--heading)/10 bg-(--p-f1ebe4)">
                  <LegalDataRow
                    label="Nombre comercial"
                    value={business.brand.name}
                  />

                  <LegalDataRow
                    label="Responsable"
                    value={business.legal.legalName}
                  />

                  <LegalDataRow
                    label="NIF / CIF"
                    value={business.legal.taxId}
                  />

                  <LegalDataRow
                    label="Domicilio"
                    value={business.legal.address}
                  />

                  <LegalDataRow
                    label="Email de contacto"
                    value={business.legal.privacyEmail}
                    last
                  />
                </dl>
              </section>

              <section
                id="datos"
                className="scroll-mt-28 border-b border-(--heading)/10 py-12"
              >
                <LegalSectionHeader
                  number="02"
                  title="Datos personales tratados"
                />

                <div className="mt-6 space-y-5 text-base leading-8 text-(--ink-muted)">
                  <p>
                    Al reservar una cita se tratan el nombre, los apellidos, el
                    correo electrónico, el teléfono y las notas de la reserva.
                  </p>

                  {business.bookingExtraFields.length > 0 ? (
                    <p>
                      También se tratan{" "}
                      {formatFieldLabels(business.bookingExtraFields)} del
                      vehículo, que se guardan junto con las notas de la
                      reserva.
                    </p>
                  ) : null}

                  <p>
                    Si la persona escribe por teléfono, correo electrónico o
                    WhatsApp, se tratan los datos que incluya en esa
                    comunicación.
                  </p>
                </div>
              </section>

              <section
                id="finalidades"
                className="scroll-mt-28 border-b border-(--heading)/10 py-12"
              >
                <LegalSectionHeader
                  number="03"
                  title="Finalidades del tratamiento"
                />

                <div className="mt-6 space-y-5 text-base leading-8 text-(--ink-muted)">
                  <p>
                    Los datos se utilizan para gestionar la cita y para enviar
                    los correos de confirmación, cambio, cancelación y
                    recordatorio.
                  </p>

                  <p>
                    También pueden utilizarse para atender consultas y
                    solicitudes enviadas por los canales de contacto del sitio.
                  </p>
                </div>
              </section>

              <section
                id="legitimacion"
                className="scroll-mt-28 border-b border-(--heading)/10 py-12"
              >
                <LegalSectionHeader
                  number="04"
                  title="Base jurídica"
                />

                <div className="mt-6 space-y-5 text-base leading-8 text-(--ink-muted)">
                  <p>
                    La base jurídica dependerá de cada tratamiento y podrá
                    consistir en el consentimiento de la persona interesada, la
                    aplicación de medidas precontractuales solicitadas por ella,
                    la ejecución de una relación contractual o el cumplimiento
                    de obligaciones legales.
                  </p>
                </div>
              </section>

              <section
                id="conservacion"
                className="scroll-mt-28 border-b border-(--heading)/10 py-12"
              >
                <LegalSectionHeader
                  number="05"
                  title="Plazo de conservación"
                />

                <p className="mt-6 text-base leading-8 text-(--ink-muted)">
                  Los datos de la reserva se conservan durante {business.legal.retention}. Si una norma exige un plazo mayor, se aplica ese plazo.
                </p>
              </section>

              <section
                id="destinatarios"
                className="scroll-mt-28 border-b border-(--heading)/10 py-12"
              >
                <LegalSectionHeader
                  number="06"
                  title="Destinatarios y terceros"
                />

                <div className="mt-6 space-y-5 text-base leading-8 text-(--ink-muted)">
                  <p>
                    Los datos no se comunicarán a terceros salvo cuando resulte
                    necesario para prestar un servicio solicitado, exista una
                    obligación legal o la persona interesada haya autorizado la
                    comunicación.
                  </p>

                  <p>
                    El envío de los correos de la cita lo realiza {business.legal.emailProvider}. Si ese proveedor trata los datos fuera del Espacio Económico Europeo, la transferencia internacional se ampara en garantías adecuadas.
                  </p>

                  <p>
                    El alojamiento del sitio y de la base de datos corresponde a {business.legal.hostingProvider} ({business.legal.hostingCountry}).
                  </p>
                </div>
              </section>

              <section
                id="derechos"
                className="scroll-mt-28 border-b border-(--heading)/10 py-12"
              >
                <LegalSectionHeader
                  number="07"
                  title="Derechos de las personas usuarias"
                />

                <div className="mt-6 space-y-5 text-base leading-8 text-(--ink-muted)">
                  <p>
                    Las personas interesadas podrán ejercer, cuando proceda,
                    sus derechos de acceso, rectificación, supresión, oposición,
                    limitación del tratamiento y portabilidad.
                  </p>

                  <p>
                    Las solicitudes podrán dirigirse a{" "}
                    <a
                      href={`mailto:${business.legal.privacyEmail}`}
                      className="font-medium text-(--p-5d4b40) underline decoration-(--accent)/35 underline-offset-4 transition-colors hover:text-(--accent)"
                    >
                      {business.legal.privacyEmail}
                    </a>
                    .
                  </p>

                  <p>
                    También puede presentar una reclamación ante la Agencia
                    Española de Protección de Datos (AEPD) si considera que el
                    tratamiento no se ajusta a la normativa.
                  </p>
                </div>
              </section>

              <section
                id="seguridad"
                className="scroll-mt-28 border-b border-(--heading)/10 py-12"
              >
                <LegalSectionHeader
                  number="08"
                  title="Seguridad de la información"
                />

                <p className="mt-6 text-base leading-8 text-(--ink-muted)">
                  El responsable aplicará medidas técnicas y organizativas
                  razonables para proteger los datos personales frente a pérdida,
                  acceso no autorizado, alteración, divulgación o destrucción.
                </p>
              </section>

              <section
                id="menores"
                className="scroll-mt-28 border-b border-(--heading)/10 py-12"
              >
                <LegalSectionHeader
                  number="09"
                  title="Menores de edad"
                />

                <p className="mt-6 text-base leading-8 text-(--ink-muted)">
                  Las personas menores de edad deberán utilizar los servicios de
                  acuerdo con la normativa aplicable y, cuando sea necesario,
                  contar con la autorización de sus representantes legales.
                </p>
              </section>

              <section
                id="cambios"
                className="scroll-mt-28 pt-12"
              >
                <LegalSectionHeader
                  number="10"
                  title="Cambios en esta política"
                />

                <p className="mt-6 text-base leading-8 text-(--ink-muted)">
                  Esta política podrá actualizarse cuando cambien los servicios
                  ofrecidos, los tratamientos de datos realizados o la normativa
                  aplicable.
                </p>

                <div className="mt-12 rounded-[1.4rem] border border-(--heading)/10 bg-(--section-warm) p-6 sm:p-8">
                  <p className="text-[9px] font-semibold uppercase tracking-[0.22em] text-(--p-967a66)">
                    Última actualización
                  </p>

                  <p className="mt-3 font-serif text-xl text-(--p-3a302a)">
                    Septiembre de 2026
                  </p>
                </div>
              </section>
            </article>
          </div>
        </section>

        {/* =====================================================
            BACK CTA
        ====================================================== */}
        <section className="bg-(--section-sand)">
          <div className="mx-auto w-full max-w-[1440px] px-5 py-16 sm:px-8 lg:px-12 xl:px-16">
            <div className="flex flex-col justify-between gap-8 rounded-[1.8rem] border border-white/25 bg-(--p-f3ebe3) p-7 sm:p-9 lg:flex-row lg:items-center">
              <div>
                <p className="text-[9px] font-semibold uppercase tracking-[0.22em] text-(--p-967a66)">
                  Privacidad
                </p>

                <p className="mt-3 max-w-[540px] font-serif text-3xl leading-tight text-(--p-302925)">
                  {business.legalCopy.privacyClosing}
                </p>
              </div>

              <Link
                href="/"
                className="group inline-flex h-14 shrink-0 items-center justify-center gap-3 rounded-full bg-(--p-2e2824) px-8 text-[11px] font-semibold uppercase tracking-[0.18em] !text-(--on-inverse) transition-all duration-300 hover:-translate-y-0.5 hover:bg-(--p-4b3e35)"
              >
                Volver al inicio

                <span className="transition-transform duration-300 group-hover:translate-x-1">
                  <ArrowIcon />
                </span>
              </Link>
            </div>
          </div>
        </section>
      </main>

      <SiteFooter />
    </div>
  );
}

/* =========================================================
   LOCAL COMPONENTS
========================================================= */

function formatFieldLabels(
  fields: readonly { label: string }[],
): string {
  const labels = fields.map(
    (field) => `la ${field.label.toLocaleLowerCase("es")}`,
  );

  if (labels.length === 1) {
    return labels[0] ?? "";
  }

  const head = labels.slice(0, -1).join(", ");
  const tail = labels[labels.length - 1] ?? "";

  return `${head} y ${tail}`;
}

type LegalSectionHeaderProps = {
  number: string;
  title: string;
};

function LegalSectionHeader({
  number,
  title,
}: LegalSectionHeaderProps) {
  return (
    <div className="grid gap-3 sm:grid-cols-[54px_1fr] sm:items-start">
      <span className="font-serif text-lg text-(--p-a08a79)">
        {number}
      </span>

      <h2 className="font-serif text-3xl leading-tight tracking-[-0.02em] text-(--p-302925) sm:text-4xl">
        {title}
      </h2>
    </div>
  );
}

type LegalDataRowProps = {
  label: string;
  value: string;
  last?: boolean;
};

function LegalDataRow({
  label,
  value,
  last = false,
}: LegalDataRowProps) {
  return (
    <div
      className={[
        "grid gap-2 px-5 py-4 sm:grid-cols-[190px_1fr] sm:gap-6 sm:px-6",
        last ? "" : "border-b border-(--heading)/10",
      ].join(" ")}
    >
      <dt className="text-[9px] font-semibold uppercase tracking-[0.18em] text-(--p-9a806d)">
        {label}
      </dt>

      <dd className="text-sm leading-6 text-(--p-51453d)">
        {value}
      </dd>
    </div>
  );
}