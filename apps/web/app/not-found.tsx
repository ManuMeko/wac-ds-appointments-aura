// apps/web/app/not-found.tsx

import Link from "next/link";

import { ArrowIcon } from "@/components/ui/ArrowIcon";
import { SiteFooter } from "@/components/layout/SiteFooter";
import { SiteHeader } from "@/components/layout/SiteHeader";
import { auraBrand } from "@/data/aura";

/* =========================================================
   WAC DS · MODEL 01 · APPOINTMENTS
   Aura Studio · 404
========================================================= */

export default function NotFound() {
  return (
    <div className="min-h-screen bg-[#f8f5f0] text-[#25211e]">
      <SiteHeader />

      <main>
        <section className="relative isolate flex min-h-[calc(100vh-76px)] items-center overflow-hidden bg-[#e9dfd4]">
          {/* =====================================================
              AMBIENT BACKGROUND
          ====================================================== */}
          <div className="pointer-events-none absolute -right-40 -top-52 -z-10 h-[620px] w-[620px] rounded-full bg-[#d7c0ac]/60 blur-3xl" />

          <div className="pointer-events-none absolute -bottom-72 -left-52 -z-10 h-[700px] w-[700px] rounded-full bg-white/35 blur-3xl" />

          <div className="mx-auto w-full max-w-[1440px] px-5 py-20 sm:px-8 lg:px-12 lg:py-28 xl:px-16">
            <div className="grid gap-12 lg:grid-cols-[0.72fr_1.28fr] lg:items-center">
              {/* =================================================
                  CODE
              ================================================== */}
              <div>
                <p className="font-serif text-[clamp(7rem,16vw,13rem)] leading-[0.75] tracking-[-0.07em] text-[#ccb29d]">
                  404
                </p>

                <p className="mt-8 text-[10px] font-semibold uppercase tracking-[0.28em] text-[#8b725f]">
                  Página no encontrada
                </p>
              </div>

              {/* =================================================
                  COPY
              ================================================== */}
              <div>
                <h1 className="max-w-[760px] font-serif text-5xl leading-[0.95] tracking-[-0.04em] text-[#2d2723] sm:text-6xl lg:text-7xl">
                  Parece que este espacio todavía no existe.
                </h1>

                <p className="mt-7 max-w-[620px] text-base leading-8 text-[#6e6158]">
                  La página que buscas puede haber cambiado de dirección, haber
                  sido eliminada o no estar disponible en este momento.
                </p>

                <div className="mt-10 flex flex-col gap-3 sm:flex-row">
                  <Link
                    href="/"
                    className="group inline-flex h-14 items-center justify-center gap-3 rounded-full bg-[#2d2723] px-8 text-[11px] font-semibold uppercase tracking-[0.18em] !text-[#fffaf6] shadow-[0_12px_30px_rgba(45,39,35,0.14)] transition-all duration-300 hover:-translate-y-0.5 hover:bg-[#4b3d34]"
                  >
                    Volver al inicio

                    <span className="transition-transform duration-300 group-hover:translate-x-1">
                      <ArrowIcon />
                    </span>
                  </Link>

                  <Link
                    href="/#contacto"
                    className="inline-flex h-14 items-center justify-center rounded-full border border-[#3e352f]/20 px-8 text-[11px] font-semibold uppercase tracking-[0.18em] text-[#413730] transition-all duration-300 hover:-translate-y-0.5 hover:border-[#3e352f]/35 hover:bg-white/30"
                  >
                    Contactar
                  </Link>
                </div>

                <div className="mt-12 border-t border-[#332b26]/10 pt-7">
                  <p className="text-[10px] font-semibold uppercase tracking-[0.22em] text-[#9a806d]">
                    {auraBrand.name}
                  </p>

                  <p className="mt-3 max-w-[460px] font-serif text-xl leading-7 text-[#625248]">
                    {auraBrand.description}
                  </p>
                </div>
              </div>
            </div>
          </div>
        </section>
      </main>

      <SiteFooter />
    </div>
  );
}