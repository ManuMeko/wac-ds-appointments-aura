// apps/web/app/aviso-legal/page.tsx

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
   Aura Studio · Legal Notice
========================================================= */

export const metadata: Metadata = {
  title: "Aviso legal",
  description:
    "Información legal y condiciones generales de acceso y utilización del sitio web de Aura Studio.",

  robots: {
    index: false,
    follow: false,
  },
};

/* =========================================================
   DEMO LEGAL CONFIGURATION

   IMPORTANTE:
   Aura Studio es una demo ficticia de WAC DS.

   Antes de utilizar este starter para un cliente real deben
   sustituirse estos datos por los datos jurídicos y fiscales
   reales del titular del sitio web.
========================================================= */

const legalOwner = {
  businessName: "Aura Studio",
  legalName: "[RAZÓN SOCIAL / NOMBRE COMPLETO DEL TITULAR]",
  taxId: "[NIF / CIF]",
  registeredAddress: "[DOMICILIO FISCAL / SOCIAL]",
  registryInformation: "[DATOS REGISTRALES, SI PROCEDE]",
  contactEmail: auraContact.email,
};

const sections = [
  {
    id: "titular",
    number: "01",
    title: "Identificación del titular",
  },
  {
    id: "objeto",
    number: "02",
    title: "Objeto del sitio web",
  },
  {
    id: "usuarios",
    number: "03",
    title: "Condiciones de acceso y utilización",
  },
  {
    id: "propiedad-intelectual",
    number: "04",
    title: "Propiedad intelectual e industrial",
  },
  {
    id: "responsabilidad",
    number: "05",
    title: "Responsabilidad",
  },
  {
    id: "enlaces",
    number: "06",
    title: "Enlaces externos",
  },
  {
    id: "modificaciones",
    number: "07",
    title: "Modificaciones",
  },
  {
    id: "legislacion",
    number: "08",
    title: "Legislación aplicable",
  },
];

export default function LegalNoticePage() {
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
                  Información legal
                </p>
              </div>

              <div>
                <h1 className="max-w-[850px] font-serif text-5xl leading-[0.95] tracking-[-0.04em] text-[#29231f] sm:text-6xl lg:text-7xl">
                  Aviso legal
                </h1>

                <p className="mt-7 max-w-[700px] text-base leading-8 text-[#6e6158]">
                  Información relativa al titular, condiciones de acceso,
                  utilización y responsabilidades asociadas a este sitio web.
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
                del Modelo 01 · Appointments de WAC DS. Los datos jurídicos que
                aparecen entre corchetes deben sustituirse por los datos reales
                del titular antes de publicar este proyecto para un cliente.
              </p>
            </div>
          </div>
        </section>

        {/* =====================================================
            LEGAL CONTENT
        ====================================================== */}
        <section className="bg-[#f8f5f0]">
          <div className="mx-auto grid w-full max-w-[1440px] gap-12 px-5 py-20 sm:px-8 lg:grid-cols-[0.32fr_0.68fr] lg:px-12 lg:py-24 xl:px-16">
            {/* =================================================
                INDEX
            ================================================== */}
            <aside className="lg:sticky lg:top-[108px] lg:self-start">
              <p className="text-[10px] font-semibold uppercase tracking-[0.22em] text-[#9a806d]">
                Contenido
              </p>

              <nav
                aria-label="Índice del aviso legal"
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

            {/* =================================================
                DOCUMENT
            ================================================== */}
            <article className="max-w-[860px]">
              {/* ===============================================
                  01 · OWNER
              ================================================ */}
              <section
                id="titular"
                className="scroll-mt-28 border-b border-[#332b26]/10 pb-12"
              >
                <LegalSectionHeader
                  number="01"
                  title="Identificación del titular"
                />

                <p className="mt-6 text-base leading-8 text-[#6c5f56]">
                  En cumplimiento de la normativa aplicable, se informa a las
                  personas usuarias de los datos identificativos del titular
                  responsable de este sitio web.
                </p>

                <dl className="mt-8 overflow-hidden rounded-[1.4rem] border border-[#332b26]/10 bg-[#f1ebe4]">
                  <LegalDataRow
                    label="Nombre comercial"
                    value={legalOwner.businessName}
                  />

                  <LegalDataRow
                    label="Titular / razón social"
                    value={legalOwner.legalName}
                  />

                  <LegalDataRow
                    label="NIF / CIF"
                    value={legalOwner.taxId}
                  />

                  <LegalDataRow
                    label="Domicilio"
                    value={legalOwner.registeredAddress}
                  />

                  <LegalDataRow
                    label="Datos registrales"
                    value={legalOwner.registryInformation}
                  />

                  <LegalDataRow
                    label="Correo electrónico"
                    value={legalOwner.contactEmail}
                    last
                  />
                </dl>
              </section>

              {/* ===============================================
                  02 · OBJECT
              ================================================ */}
              <section
                id="objeto"
                className="scroll-mt-28 border-b border-[#332b26]/10 py-12"
              >
                <LegalSectionHeader
                  number="02"
                  title="Objeto del sitio web"
                />

                <div className="mt-6 space-y-5 text-base leading-8 text-[#6c5f56]">
                  <p>
                    El sitio web tiene como finalidad ofrecer información sobre
                    {` ${auraBrand.name}`}, sus servicios, tratamientos,
                    profesionales, horarios, medios de contacto y otras
                    cuestiones relacionadas con su actividad.
                  </p>

                  <p>
                    Asimismo, el sitio puede permitir a las personas usuarias
                    contactar con el negocio mediante teléfono, correo
                    electrónico, WhatsApp u otros canales habilitados.
                  </p>
                </div>
              </section>

              {/* ===============================================
                  03 · ACCESS
              ================================================ */}
              <section
                id="usuarios"
                className="scroll-mt-28 border-b border-[#332b26]/10 py-12"
              >
                <LegalSectionHeader
                  number="03"
                  title="Condiciones de acceso y utilización"
                />

                <div className="mt-6 space-y-5 text-base leading-8 text-[#6c5f56]">
                  <p>
                    El acceso al sitio web implica la aceptación de las
                    condiciones recogidas en este aviso legal.
                  </p>

                  <p>
                    La persona usuaria se compromete a utilizar el sitio de
                    forma diligente y conforme a la legislación aplicable, la
                    buena fe y el orden público.
                  </p>

                  <p>
                    No podrá utilizarse el sitio para realizar actividades
                    ilícitas, fraudulentas o que puedan perjudicar los derechos,
                    intereses, sistemas o servicios del titular o de terceros.
                  </p>
                </div>
              </section>

              {/* ===============================================
                  04 · INTELLECTUAL PROPERTY
              ================================================ */}
              <section
                id="propiedad-intelectual"
                className="scroll-mt-28 border-b border-[#332b26]/10 py-12"
              >
                <LegalSectionHeader
                  number="04"
                  title="Propiedad intelectual e industrial"
                />

                <div className="mt-6 space-y-5 text-base leading-8 text-[#6c5f56]">
                  <p>
                    Los contenidos del sitio web, incluyendo textos,
                    fotografías, imágenes, elementos gráficos, diseño,
                    identidad visual, logotipos, estructura y demás elementos,
                    se encuentran protegidos por la normativa aplicable en
                    materia de propiedad intelectual e industrial cuando
                    corresponda.
                  </p>

                  <p>
                    Queda prohibida su reproducción, distribución,
                    transformación, comunicación pública o explotación sin la
                    autorización correspondiente, salvo en los casos permitidos
                    por la legislación vigente.
                  </p>
                </div>
              </section>

              {/* ===============================================
                  05 · LIABILITY
              ================================================ */}
              <section
                id="responsabilidad"
                className="scroll-mt-28 border-b border-[#332b26]/10 py-12"
              >
                <LegalSectionHeader
                  number="05"
                  title="Responsabilidad"
                />

                <div className="mt-6 space-y-5 text-base leading-8 text-[#6c5f56]">
                  <p>
                    El titular procura que la información publicada sea correcta
                    y se mantenga actualizada, aunque no puede garantizar la
                    ausencia absoluta de errores, interrupciones o incidencias
                    técnicas.
                  </p>

                  <p>
                    La información disponible en el sitio tiene carácter
                    general y no sustituye la valoración profesional
                    individualizada que pueda ser necesaria en cada caso.
                  </p>
                </div>
              </section>

              {/* ===============================================
                  06 · EXTERNAL LINKS
              ================================================ */}
              <section
                id="enlaces"
                className="scroll-mt-28 border-b border-[#332b26]/10 py-12"
              >
                <LegalSectionHeader
                  number="06"
                  title="Enlaces externos"
                />

                <p className="mt-6 text-base leading-8 text-[#6c5f56]">
                  El sitio web puede contener enlaces hacia plataformas o
                  servicios gestionados por terceros. El titular no controla
                  necesariamente el contenido, disponibilidad o funcionamiento
                  de esos servicios externos, que estarán sujetos a sus propias
                  condiciones.
                </p>
              </section>

              {/* ===============================================
                  07 · MODIFICATIONS
              ================================================ */}
              <section
                id="modificaciones"
                className="scroll-mt-28 border-b border-[#332b26]/10 py-12"
              >
                <LegalSectionHeader
                  number="07"
                  title="Modificaciones"
                />

                <p className="mt-6 text-base leading-8 text-[#6c5f56]">
                  El titular podrá modificar este aviso legal cuando resulte
                  necesario para adaptarlo a cambios en el sitio web, en los
                  servicios ofrecidos o en la normativa aplicable.
                </p>
              </section>

              {/* ===============================================
                  08 · APPLICABLE LAW
              ================================================ */}
              <section
                id="legislacion"
                className="scroll-mt-28 pt-12"
              >
                <LegalSectionHeader
                  number="08"
                  title="Legislación aplicable"
                />

                <div className="mt-6 space-y-5 text-base leading-8 text-[#6c5f56]">
                  <p>
                    Las relaciones derivadas del acceso y utilización de este
                    sitio web se regirán por la legislación que resulte
                    aplicable.
                  </p>

                  <p>
                    Cuando legalmente corresponda, cualquier controversia se
                    someterá a los juzgados y tribunales competentes conforme a
                    las reglas aplicables de jurisdicción y competencia.
                  </p>
                </div>

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
                  Aura Studio
                </p>

                <p className="mt-3 max-w-[540px] font-serif text-3xl leading-tight text-[#302925]">
                  Vuelve a descubrir nuestros tratamientos y nuestra forma de
                  entender el bienestar.
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