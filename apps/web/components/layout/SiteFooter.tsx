// apps/web/components/layout/SiteFooter.tsx

import {
  auraBrand,
  auraFooter,
  auraLegalLinks,
  auraNavigation,
  auraSocialLinks,
} from "@/data/aura";

/* =========================================================
   WAC DS · MODEL 01 · APPOINTMENTS
   Aura Studio · Site Footer
========================================================= */

export function SiteFooter() {
  return (
    <footer className="bg-[#201c19] text-white">
      <div className="mx-auto w-full max-w-[1440px] px-5 py-12 sm:px-8 lg:px-12 xl:px-16">
        {/* =====================================================
            MAIN FOOTER
        ====================================================== */}
        <div className="flex flex-col justify-between gap-12 border-b border-white/10 pb-10 lg:flex-row lg:items-end">
          {/* Brand */}
          <div className="max-w-[430px]">
            <a
              href="#inicio"
              aria-label={`${auraBrand.name} - Inicio`}
              className="group inline-flex items-center gap-3"
            >
              <span className="flex h-11 w-11 items-center justify-center rounded-full border border-white/15 bg-white/[0.04] font-serif text-xl text-[#d6c0ae] transition-all duration-300 group-hover:border-white/25 group-hover:bg-white/[0.07]">
                {auraBrand.monogram}
              </span>

              <span className="leading-none">
                <span className="block font-serif text-[22px] tracking-[0.14em] text-[#f0e7df]">
                  {auraBrand.shortName.toUpperCase()}
                </span>

                <span className="mt-1 block text-[9px] font-medium uppercase tracking-[0.34em] text-[#8e837c]">
                  Studio
                </span>
              </span>
            </a>

            <p className="mt-5 max-w-[390px] text-sm leading-6 text-[#948a84]">
              {auraBrand.description}
            </p>

            <p className="mt-5 text-[10px] font-semibold uppercase tracking-[0.23em] text-[#766d68]">
              {auraBrand.locationLabel}
            </p>
          </div>

          {/* Navigation + social */}
          <div className="grid gap-10 sm:grid-cols-2 lg:min-w-[430px]">
            <div>
              <p className="text-[10px] font-semibold uppercase tracking-[0.22em] text-[#736a65]">
                Navegación
              </p>

              <nav
                aria-label="Navegación del pie de página"
                className="mt-5 flex flex-col gap-3"
              >
                {auraNavigation.map((item) => (
                  <a
                    key={item.href}
                    href={item.href}
                    className="w-fit text-sm text-[#aca19a] transition-colors duration-200 hover:text-white"
                  >
                    {item.label}
                  </a>
                ))}
              </nav>
            </div>

            <div>
              <p className="text-[10px] font-semibold uppercase tracking-[0.22em] text-[#736a65]">
                Síguenos
              </p>

              <div className="mt-5 flex flex-col gap-3">
                {auraSocialLinks.map((item) => (
                  <a
                    key={item.name}
                    href={item.href}
                    className="w-fit text-sm text-[#aca19a] transition-colors duration-200 hover:text-white"
                  >
                    {item.name}
                  </a>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* =====================================================
            LEGAL / COPYRIGHT
        ====================================================== */}
        <div className="flex flex-col justify-between gap-5 pt-7 text-[11px] text-[#776f6a] sm:flex-row sm:items-center">
          <p>{auraFooter.copyright}</p>

          <div className="flex flex-wrap gap-x-5 gap-y-2">
            {auraLegalLinks.map((item) => (
              <a
                key={item.label}
                href={item.href}
                className="transition-colors duration-200 hover:text-[#bcb1aa]"
              >
                {item.label}
              </a>
            ))}
          </div>
        </div>

        {/* =====================================================
            WAC DS CREDIT
        ====================================================== */}
        <div className="mt-7 border-t border-white/10 pt-7">
          <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
            <p className="text-[10px] uppercase tracking-[0.2em] text-[#625b57]">
              {auraFooter.credit}
            </p>

            <p className="text-[10px] uppercase tracking-[0.18em] text-[#4f4945]">
              Model 01 · Appointments
            </p>
          </div>
        </div>
      </div>
    </footer>
  );
}