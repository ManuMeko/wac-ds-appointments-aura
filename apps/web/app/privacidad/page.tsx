// apps/web/app/privacidad/page.tsx

import type { Metadata } from "next";
import Link from "next/link";

import { ArrowIcon } from "@/components/ui/ArrowIcon";
import { SiteFooter } from "@/components/layout/SiteFooter";
import { SiteHeader } from "@/components/layout/SiteHeader";
import {
  auraBrand,
  auraContact,
} from "@/data/aura";

/* =========================================================
   WAC DS · MODEL 01 · APPOINTMENTS
   Aura Studio · Privacy Policy
========================================================= */

export const metadata: Metadata = {
  title: "Política de privacidad",
  description:
    "Información sobre el tratamiento de datos personales en el sitio web de Aura Studio.",

  robots: {
    index: false,
    follow: false,
  },
};

/* =========================================================
   DEMO PRIVACY CONFIGURATION

   IMPORTANTE:
   Aura Studio es una demo ficticia de WAC DS.

   Antes de utilizar este starter para un cliente real deben
   sustituirse estos datos por los datos jurídicos y fiscales
   reales del responsable del tratamiento.
========================================================= */

const privacyOwner = {
  businessName: "Aura Studio",
  legalName: "[RAZÓN SOCIAL / NOMBRE COMPLETO DEL RESPONSABLE]",
  taxId: "[NIF / CIF]",
  registeredAddress: "[DOMICILIO FISCAL / SOCIAL]",
  privacyEmail: auraContact.email,
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
    <div className="min-h-screen bg-[#f8f5f0] text-[#25211e]">
      <SiteHeader />

      <main>
        {/* =====================================================
            HERO
        ====================================================== */}
        <section className="relative overflow-hidden border-b border-[#332b26]/10 bg-[#e9dfd4]">
          <div className="pointer-events-none absolute -right-32 -top-48 h-[560px] w-[560px] rounded-full bg-[#d9c3ae]/55 blur-3xl" />

          <div className="pointer-events-none absolute -bottom-60 -left-40 h-[560px] w-[560px] rounded-full bg-white/30 blur-3xl" />

          <div className="relative mx-auto w-full max-w-[1440px] px-5 py-20 sm:px-8 lg:px-12 lg:py-28 xl:px-16">
            <Link
              href="/"
              className="group inline-flex items-center gap-2 text-[10px] font-semibold uppercase tracking-[0.2em] text-[#806b5c] transition-colors duration-200 hover:text-[#4d4037]"
            >
              <span className="rotate-180 transition-transform duration-300 group-hover:-translate-x-1">
                <ArrowIcon />
              </span>

              Volver a {auraBrand.shortName}
            </Link>

            <div className="mt-12 grid gap-10 lg:grid-cols-[0.72fr_1.28fr]">
              <div>
                <p className="text-[10px] font-semibold uppercase tracking-[0.27em] text-[#89715f]">
                  Protección de datos
                </p>
              </div>

              <div>
                <h1 className="max-w-[850px] font-serif text-5xl leading-[0.95] tracking-[-0.04em] text-[#29231f] sm:text-6xl lg:text-7xl">
                  Política de privacidad
                </h1>

                <p className="mt-7 max-w-[700px] text-base leading-8 text-[#6e6158]">
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
        <section className="bg-[#f8f5f0]">
          <div className="mx-auto w-full max-w-[1440px] px-5 pt-12 sm:px-8 lg:px-12 xl:px-16">
            <div className="rounded-[1.4rem] border border-[#9b775e]/20 bg-[#efe4da] p-5 sm:p-6">
              <p className="text-[9px] font-semibold uppercase tracking-[0.22em] text-[#8b6d59]">
                Demo comercial WAC DS
              </p>

              <p className="mt-3 max-w-[900px] text-sm leading-6 text-[#66584f]">
                Aura Studio es una empresa ficticia utilizada como demostración
                comercial. Los datos jurídicos indicados entre corchetes deben
                sustituirse por los datos reales del responsable del tratamiento
                antes de publicar esta web para un cliente.
              </p>
            </div>
          </div>
        </section>

        {/* =====================================================
            PRIVACY CONTENT
        ====================================================== */}
        <section className="bg-[#f8f5f0]">
          <div className="mx-auto grid w-full max-w-[1440px] gap-12 px-5 py-20 sm:px-8 lg:grid-cols-[0.32fr_0.68fr] lg:px-12 lg:py-24 xl:px-16">
            {/* Index */}
            <aside className="lg:sticky lg:top-[108px] lg:self-start">
              <p className="text-[10px] font-semibold uppercase tracking-[0.22em] text-[#9a806d]">
                Contenido
              </p>

              <nav
                aria-label="Índice de la política de privacidad"
                className="mt-6 border-t border-[#332b26]/10"
              >
                {sections.map((section) => (
                  <a
                    key={section.id}
                    href={`#${section.id}`}
                    className="group flex items-start gap-4 border-b border-[#332b26]/10 py-4"
                  >
                    <span className="font-serif text-sm text-[#a18a79]">
                      {section.number}
                    </span>

                    <span className="text-sm leading-5 text-[#62564e] transition-colors duration-200 group-hover:text-[#2d2723]">
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
                className="scroll-mt-28 border-b border-[#332b26]/10 pb-12"
              >
                <LegalSectionHeader
                  number="01"
                  title="Responsable del tratamiento"
                />

                <p className="mt-6 text-base leading-8 text-[#6c5f56]">
                  El responsable del tratamiento de los datos personales
                  facilitados a través de este sitio web será:
                </p>

                <dl className="mt-8 overflow-hidden rounded-[1.4rem] border border-[#332b26]/10 bg-[#f1ebe4]">
                  <LegalDataRow
                    label="Nombre comercial"
                    value={privacyOwner.businessName}
                  />

                  <LegalDataRow
                    label="Responsable"
                    value={privacyOwner.legalName}
                  />

                  <LegalDataRow
                    label="NIF / CIF"
                    value={privacyOwner.taxId}
                  />

                  <LegalDataRow
                    label="Domicilio"
                    value={privacyOwner.registeredAddress}
                  />

                  <LegalDataRow
                    label="Email de contacto"
                    value={privacyOwner.privacyEmail}
                    last
                  />
                </dl>
              </section>

              <section
                id="datos"
                className="scroll-mt-28 border-b border-[#332b26]/10 py-12"
              >
                <LegalSectionHeader
                  number="02"
                  title="Datos personales tratados"
                />

                <div className="mt-6 space-y-5 text-base leading-8 text-[#6c5f56]">
                  <p>
                    Dependiendo del uso que se realice del sitio web, podrán
                    tratarse datos identificativos y de contacto como nombre,
                    número de teléfono, dirección de correo electrónico y la
                    información incluida voluntariamente en comunicaciones o
                    solicitudes.
                  </p>

                  <p>
                    En futuras versiones que incorporen reservas online podrán
                    tratarse además los datos necesarios para gestionar citas,
                    servicios solicitados y comunicaciones relacionadas con la
                    reserva.
                  </p>
                </div>
              </section>

              <section
                id="finalidades"
                className="scroll-mt-28 border-b border-[#332b26]/10 py-12"
              >
                <LegalSectionHeader
                  number="03"
                  title="Finalidades del tratamiento"
                />

                <div className="mt-6 space-y-5 text-base leading-8 text-[#6c5f56]">
                  <p>
                    Los datos podrán utilizarse para atender consultas,
                    solicitudes de información y comunicaciones realizadas a
                    través de los canales disponibles en el sitio.
                  </p>

                  <p>
                    Cuando se habiliten sistemas de reserva, podrán utilizarse
                    para gestionar citas, confirmar disponibilidad, realizar
                    comunicaciones operativas y prestar los servicios
                    solicitados.
                  </p>
                </div>
              </section>

              <section
                id="legitimacion"
                className="scroll-mt-28 border-b border-[#332b26]/10 py-12"
              >
                <LegalSectionHeader
                  number="04"
                  title="Base jurídica"
                />

                <div className="mt-6 space-y-5 text-base leading-8 text-[#6c5f56]">
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
                className="scroll-mt-28 border-b border-[#332b26]/10 py-12"
              >
                <LegalSectionHeader
                  number="05"
                  title="Plazo de conservación"
                />

                <p className="mt-6 text-base leading-8 text-[#6c5f56]">
                  Los datos se conservarán durante el tiempo necesario para
                  cumplir la finalidad para la que fueron recogidos y, cuando
                  corresponda, durante los plazos exigidos por la normativa
                  aplicable.
                </p>
              </section>

              <section
                id="destinatarios"
                className="scroll-mt-28 border-b border-[#332b26]/10 py-12"
              >
                <LegalSectionHeader
                  number="06"
                  title="Destinatarios y terceros"
                />

                <div className="mt-6 space-y-5 text-base leading-8 text-[#6c5f56]">
                  <p>
                    Los datos no se comunicarán a terceros salvo cuando resulte
                    necesario para prestar un servicio solicitado, exista una
                    obligación legal o la persona interesada haya autorizado la
                    comunicación.
                  </p>

                  <p>
                    Determinados proveedores tecnológicos podrán actuar como
                    encargados del tratamiento cuando presten servicios de
                    alojamiento, comunicaciones, reservas, analítica u otros
                    servicios necesarios para la operación del sitio.
                  </p>
                </div>
              </section>

              <section
                id="derechos"
                className="scroll-mt-28 border-b border-[#332b26]/10 py-12"
              >
                <LegalSectionHeader
                  number="07"
                  title="Derechos de las personas usuarias"
                />

                <div className="mt-6 space-y-5 text-base leading-8 text-[#6c5f56]">
                  <p>
                    Las personas interesadas podrán ejercer, cuando proceda,
                    sus derechos de acceso, rectificación, supresión, oposición,
                    limitación del tratamiento y portabilidad.
                  </p>

                  <p>
                    Las solicitudes podrán dirigirse a{" "}
                    <a
                      href={`mailto:${privacyOwner.privacyEmail}`}
                      className="font-medium text-[#5d4b40] underline decoration-[#8b6d59]/35 underline-offset-4 transition-colors hover:text-[#8b6d59]"
                    >
                      {privacyOwner.privacyEmail}
                    </a>
                    .
                  </p>

                  <p>
                    Asimismo, la persona interesada podrá presentar una
                    reclamación ante la autoridad de protección de datos
                    competente cuando considere que el tratamiento no se ajusta
                    a la normativa aplicable.
                  </p>
                </div>
              </section>

              <section
                id="seguridad"
                className="scroll-mt-28 border-b border-[#332b26]/10 py-12"
              >
                <LegalSectionHeader
                  number="08"
                  title="Seguridad de la información"
                />

                <p className="mt-6 text-base leading-8 text-[#6c5f56]">
                  El responsable aplicará medidas técnicas y organizativas
                  razonables para proteger los datos personales frente a pérdida,
                  acceso no autorizado, alteración, divulgación o destrucción.
                </p>
              </section>

              <section
                id="menores"
                className="scroll-mt-28 border-b border-[#332b26]/10 py-12"
              >
                <LegalSectionHeader
                  number="09"
                  title="Menores de edad"
                />

                <p className="mt-6 text-base leading-8 text-[#6c5f56]">
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

                <p className="mt-6 text-base leading-8 text-[#6c5f56]">
                  Esta política podrá actualizarse cuando cambien los servicios
                  ofrecidos, los tratamientos de datos realizados o la normativa
                  aplicable.
                </p>

                <div className="mt-12 rounded-[1.4rem] border border-[#332b26]/10 bg-[#eee5dc] p-6 sm:p-8">
                  <p className="text-[9px] font-semibold uppercase tracking-[0.22em] text-[#967a66]">
                    Última actualización
                  </p>

                  <p className="mt-3 font-serif text-xl text-[#3a302a]">
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
        <section className="bg-[#d8c3b2]">
          <div className="mx-auto w-full max-w-[1440px] px-5 py-16 sm:px-8 lg:px-12 xl:px-16">
            <div className="flex flex-col justify-between gap-8 rounded-[1.8rem] border border-white/25 bg-[#f3ebe3] p-7 sm:p-9 lg:flex-row lg:items-center">
              <div>
                <p className="text-[9px] font-semibold uppercase tracking-[0.22em] text-[#967a66]">
                  Privacidad
                </p>

                <p className="mt-3 max-w-[540px] font-serif text-3xl leading-tight text-[#302925]">
                  Tus datos deben tratarse con el mismo cuidado que ponemos en
                  cada experiencia.
                </p>
              </div>

              <Link
                href="/"
                className="group inline-flex h-14 shrink-0 items-center justify-center gap-3 rounded-full bg-[#2e2824] px-8 text-[11px] font-semibold uppercase tracking-[0.18em] !text-[#fffaf6] transition-all duration-300 hover:-translate-y-0.5 hover:bg-[#4b3e35]"
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
      <span className="font-serif text-lg text-[#a08a79]">
        {number}
      </span>

      <h2 className="font-serif text-3xl leading-tight tracking-[-0.02em] text-[#302925] sm:text-4xl">
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
        last ? "" : "border-b border-[#332b26]/10",
      ].join(" ")}
    >
      <dt className="text-[9px] font-semibold uppercase tracking-[0.18em] text-[#9a806d]">
        {label}
      </dt>

      <dd className="text-sm leading-6 text-[#51453d]">
        {value}
      </dd>
    </div>
  );
}