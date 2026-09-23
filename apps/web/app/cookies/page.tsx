// apps/web/app/cookies/page.tsx

import type { Metadata } from "next";
import Link from "next/link";

import { ArrowIcon } from "@/components/ui/ArrowIcon";
import { SiteFooter } from "@/components/layout/SiteFooter";
import { SiteHeader } from "@/components/layout/SiteHeader";
import { auraBrand, auraContact } from "@/data/aura";

/* =========================================================
   WAC DS · MODEL 01 · APPOINTMENTS
   Aura Studio · Cookies Policy
========================================================= */

export const metadata: Metadata = {
  title: "Política de cookies",
  description:
    "Información sobre el uso de cookies y tecnologías similares en el sitio web de Aura Studio.",

  robots: {
    index: false,
    follow: false,
  },
};

/* =========================================================
   DEMO COOKIES CONFIGURATION

   IMPORTANTE:
   Aura Studio es una demo ficticia de WAC DS.

   Esta página debe adaptarse al cliente real y, sobre todo,
   a las cookies y servicios realmente utilizados en producción.

   No deben declararse cookies o proveedores inexistentes,
   ni omitirse tecnologías que sí estén activas.
========================================================= */

const cookieOwner = {
  businessName: "Aura Studio",
  legalName: "[RAZÓN SOCIAL / NOMBRE COMPLETO DEL TITULAR]",
  privacyEmail: auraContact.email,
};

const sections = [
  {
    id: "que-son",
    number: "01",
    title: "Qué son las cookies",
  },
  {
    id: "tipos",
    number: "02",
    title: "Tipos de cookies",
  },
  {
    id: "cookies-utilizadas",
    number: "03",
    title: "Cookies utilizadas en este sitio",
  },
  {
    id: "terceros",
    number: "04",
    title: "Cookies y servicios de terceros",
  },
  {
    id: "gestion",
    number: "05",
    title: "Cómo gestionar las cookies",
  },
  {
    id: "consentimiento",
    number: "06",
    title: "Consentimiento",
  },
  {
    id: "actualizaciones",
    number: "07",
    title: "Actualizaciones de la política",
  },
  {
    id: "contacto",
    number: "08",
    title: "Contacto",
  },
];

/* =========================================================
   COOKIE TABLE DATA

   Esta demo se plantea inicialmente como sitio estático y no
   presupone el uso de cookies analíticas, publicitarias o de
   personalización.

   Cuando el cliente real incorpore Google Analytics, Meta Pixel,
   chat externo, reservas, mapas, vídeos embebidos, etc., esta
   tabla deberá actualizarse.
========================================================= */

const cookieCategories = [
  {
    category: "Técnicas / necesarias",
    purpose:
      "Permiten el funcionamiento esencial, la seguridad y la correcta prestación de funcionalidades básicas del sitio.",
    consent: "No cuando sean estrictamente necesarias",
    status: "Según implementación",
  },
  {
    category: "Preferencias",
    purpose:
      "Permiten recordar determinadas elecciones realizadas por la persona usuaria.",
    consent: "Sí, salvo excepciones legalmente aplicables",
    status: "No previstas en la demo inicial",
  },
  {
    category: "Analíticas",
    purpose:
      "Permiten medir el uso del sitio web y obtener estadísticas agregadas para mejorar su funcionamiento.",
    consent: "Sí",
    status: "No previstas en la demo inicial",
  },
  {
    category: "Publicidad / marketing",
    purpose:
      "Permiten crear perfiles, medir campañas o mostrar publicidad personalizada.",
    consent: "Sí",
    status: "No previstas en la demo inicial",
  },
];

export default function CookiesPage() {
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
                  Preferencias y navegación
                </p>
              </div>

              <div>
                <h1 className="max-w-[850px] font-serif text-5xl leading-[0.95] tracking-[-0.04em] text-[#29231f] sm:text-6xl lg:text-7xl">
                  Política de cookies
                </h1>

                <p className="mt-7 max-w-[700px] text-base leading-8 text-[#6e6158]">
                  Información sobre el uso de cookies y tecnologías similares
                  que pueden emplearse durante la navegación por este sitio web.
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

              <p className="mt-3 max-w-[950px] text-sm leading-6 text-[#66584f]">
                Aura Studio es una empresa ficticia utilizada como demostración
                del Modelo 01 · Appointments de WAC DS. Esta política deberá
                adaptarse a las cookies, proveedores y servicios realmente
                utilizados por cada cliente antes de publicar la web.
              </p>
            </div>
          </div>
        </section>

        {/* =====================================================
            COOKIES CONTENT
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
                aria-label="Índice de la política de cookies"
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
                  01
              ================================================ */}
              <section
                id="que-son"
                className="scroll-mt-28 border-b border-[#332b26]/10 pb-12"
              >
                <LegalSectionHeader
                  number="01"
                  title="Qué son las cookies"
                />

                <div className="mt-6 space-y-5 text-base leading-8 text-[#6c5f56]">
                  <p>
                    Las cookies son pequeños archivos o fragmentos de
                    información que pueden almacenarse en el dispositivo de la
                    persona usuaria cuando visita una página web.
                  </p>

                  <p>
                    Pueden utilizarse para permitir determinadas funcionalidades,
                    recordar preferencias, obtener información estadística o
                    facilitar la integración con servicios de terceros.
                  </p>
                </div>
              </section>

              {/* ===============================================
                  02
              ================================================ */}
              <section
                id="tipos"
                className="scroll-mt-28 border-b border-[#332b26]/10 py-12"
              >
                <LegalSectionHeader
                  number="02"
                  title="Tipos de cookies"
                />

                <p className="mt-6 text-base leading-8 text-[#6c5f56]">
                  Las cookies pueden clasificarse según su finalidad, duración
                  y entidad responsable de su gestión.
                </p>

                <div className="mt-8 grid gap-4">
                  {cookieCategories.map((item) => (
                    <div
                      key={item.category}
                      className="rounded-[1.25rem] border border-[#332b26]/10 bg-[#f1ebe4] p-5 sm:p-6"
                    >
                      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                        <h3 className="font-serif text-2xl text-[#332c27]">
                          {item.category}
                        </h3>

                        <span className="w-fit rounded-full border border-[#8b6d59]/15 bg-[#8b6d59]/[0.05] px-3 py-1.5 text-[9px] font-semibold uppercase tracking-[0.14em] text-[#856c59]">
                          {item.status}
                        </span>
                      </div>

                      <p className="mt-4 text-sm leading-6 text-[#6c5f56]">
                        {item.purpose}
                      </p>

                      <div className="mt-5 border-t border-[#332b26]/10 pt-4">
                        <p className="text-[9px] font-semibold uppercase tracking-[0.18em] text-[#9a806d]">
                          Consentimiento
                        </p>

                        <p className="mt-2 text-sm text-[#51453d]">
                          {item.consent}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              </section>

              {/* ===============================================
                  03
              ================================================ */}
              <section
                id="cookies-utilizadas"
                className="scroll-mt-28 border-b border-[#332b26]/10 py-12"
              >
                <LegalSectionHeader
                  number="03"
                  title="Cookies utilizadas en este sitio"
                />

                <div className="mt-6 space-y-5 text-base leading-8 text-[#6c5f56]">
                  <p>
                    La versión inicial de esta demo no presupone la instalación
                    de cookies analíticas, publicitarias o de personalización.
                  </p>

                  <p>
                    En una implementación real deberán identificarse y
                    documentarse únicamente las cookies y tecnologías que se
                    encuentren efectivamente activas en el sitio web.
                  </p>
                </div>

                <div className="mt-8 overflow-hidden rounded-[1.4rem] border border-[#332b26]/10">
                  <div className="grid grid-cols-1 gap-0 bg-[#eee5dc] sm:grid-cols-4">
                    <CookieTableHeader label="Cookie / tecnología" />
                    <CookieTableHeader label="Proveedor" />
                    <CookieTableHeader label="Finalidad" />
                    <CookieTableHeader label="Duración" />
                  </div>

                  <div className="grid grid-cols-1 bg-[#f7f2ec] sm:grid-cols-4">
                    <CookieTableCell value="Por determinar según producción" />
                    <CookieTableCell value="Por determinar" />
                    <CookieTableCell value="Según servicios activados" />
                    <CookieTableCell value="Según cada cookie" last />
                  </div>
                </div>

                <div className="mt-6 rounded-[1.2rem] border border-[#9b775e]/15 bg-[#efe4da]/60 p-5">
                  <p className="text-sm leading-6 text-[#66584f]">
                    Antes de desplegar este starter para un cliente real deberá
                    realizarse una revisión técnica de las cookies efectivamente
                    instaladas y actualizar esta sección.
                  </p>
                </div>
              </section>

              {/* ===============================================
                  04
              ================================================ */}
              <section
                id="terceros"
                className="scroll-mt-28 border-b border-[#332b26]/10 py-12"
              >
                <LegalSectionHeader
                  number="04"
                  title="Cookies y servicios de terceros"
                />

                <div className="mt-6 space-y-5 text-base leading-8 text-[#6c5f56]">
                  <p>
                    Determinadas funcionalidades pueden depender de servicios
                    externos que, cuando se incorporen, podrían utilizar sus
                    propias cookies o tecnologías similares.
                  </p>

                  <p>
                    Esto puede ocurrir, por ejemplo, al integrar herramientas de
                    analítica, mapas, reproductores multimedia, sistemas de
                    reservas, chats, redes sociales u otros servicios de
                    terceros.
                  </p>

                  <p>
                    Cuando dichos servicios requieran consentimiento, no deberán
                    activarse antes de que la persona usuaria haya realizado la
                    elección correspondiente.
                  </p>
                </div>
              </section>

              {/* ===============================================
                  05
              ================================================ */}
              <section
                id="gestion"
                className="scroll-mt-28 border-b border-[#332b26]/10 py-12"
              >
                <LegalSectionHeader
                  number="05"
                  title="Cómo gestionar las cookies"
                />

                <div className="mt-6 space-y-5 text-base leading-8 text-[#6c5f56]">
                  <p>
                    Las personas usuarias pueden configurar su navegador para
                    bloquear, limitar o eliminar cookies almacenadas en su
                    dispositivo.
                  </p>

                  <p>
                    La configuración concreta depende del navegador y del
                    dispositivo utilizado. La desactivación de determinadas
                    cookies técnicas podría afectar al funcionamiento de algunas
                    funcionalidades del sitio.
                  </p>

                  <p>
                    Cuando el sitio incorpore cookies sujetas a consentimiento,
                    deberá ofrecerse además un mecanismo específico que permita
                    aceptar, rechazar o configurar dichas cookies.
                  </p>
                </div>
              </section>

              {/* ===============================================
                  06
              ================================================ */}
              <section
                id="consentimiento"
                className="scroll-mt-28 border-b border-[#332b26]/10 py-12"
              >
                <LegalSectionHeader
                  number="06"
                  title="Consentimiento"
                />

                <div className="mt-6 space-y-5 text-base leading-8 text-[#6c5f56]">
                  <p>
                    Las cookies que no sean estrictamente necesarias deberán
                    utilizarse únicamente cuando exista una base válida para
                    ello y, cuando corresponda, después de obtener el
                    consentimiento de la persona usuaria.
                  </p>

                  <p>
                    El consentimiento deberá poder retirarse o modificarse con
                    una facilidad equivalente a la utilizada para otorgarlo.
                  </p>
                </div>
              </section>

              {/* ===============================================
                  07
              ================================================ */}
              <section
                id="actualizaciones"
                className="scroll-mt-28 border-b border-[#332b26]/10 py-12"
              >
                <LegalSectionHeader
                  number="07"
                  title="Actualizaciones de la política"
                />

                <p className="mt-6 text-base leading-8 text-[#6c5f56]">
                  Esta política podrá actualizarse cuando cambien las cookies,
                  proveedores, servicios integrados, funcionalidades del sitio o
                  la normativa aplicable.
                </p>
              </section>

              {/* ===============================================
                  08
              ================================================ */}
              <section
                id="contacto"
                className="scroll-mt-28 pt-12"
              >
                <LegalSectionHeader
                  number="08"
                  title="Contacto"
                />

                <div className="mt-6 space-y-5 text-base leading-8 text-[#6c5f56]">
                  <p>
                    Para cualquier consulta relacionada con el uso de cookies
                    puede contactar con:
                  </p>

                  <dl className="overflow-hidden rounded-[1.4rem] border border-[#332b26]/10 bg-[#f1ebe4]">
                    <LegalDataRow
                      label="Nombre comercial"
                      value={cookieOwner.businessName}
                    />

                    <LegalDataRow
                      label="Titular"
                      value={cookieOwner.legalName}
                    />

                    <LegalDataRow
                      label="Email"
                      value={cookieOwner.privacyEmail}
                      last
                    />
                  </dl>
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
                  Navegación transparente
                </p>

                <p className="mt-3 max-w-[560px] font-serif text-3xl leading-tight text-[#302925]">
                  Tú decides cómo quieres que se utilicen las tecnologías no
                  esenciales durante tu visita.
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

type CookieTableHeaderProps = {
  label: string;
};

function CookieTableHeader({
  label,
}: CookieTableHeaderProps) {
  return (
    <div className="border-b border-[#332b26]/10 px-4 py-3 sm:border-b-0 sm:border-r sm:last:border-r-0">
      <p className="text-[9px] font-semibold uppercase tracking-[0.16em] text-[#8c7565]">
        {label}
      </p>
    </div>
  );
}

type CookieTableCellProps = {
  value: string;
  last?: boolean;
};

function CookieTableCell({
  value,
  last = false,
}: CookieTableCellProps) {
  return (
    <div
      className={[
        "border-b border-[#332b26]/10 px-4 py-4 sm:border-b-0 sm:border-r",
        last ? "sm:border-r-0" : "",
      ].join(" ")}
    >
      <p className="text-sm leading-6 text-[#51453d]">
        {value}
      </p>
    </div>
  );
}