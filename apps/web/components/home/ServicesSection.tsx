// apps/web/components/home/ServicesSection.tsx

import { ArrowIcon } from "@/components/ui/ArrowIcon";
import { business } from "@/config/business";

/* =========================================================
   WAC DS · MODEL 01 · APPOINTMENTS
   Aura Studio · Services Section
========================================================= */

export function ServicesSection() {
  return (
    <section id="servicios" className="bg-(--canvas)">
      <div className="mx-auto w-full max-w-[1440px] px-5 py-20 sm:px-8 lg:px-12 lg:py-28 xl:px-16">
        <div className="grid gap-10 lg:grid-cols-[0.8fr_1.2fr]">
          {/* =====================================================
              INTRO
          ====================================================== */}
          <div>
            <p className="text-[10px] font-semibold uppercase tracking-[0.27em] text-(--p-8b725f)">
              {business.servicesIntro.eyebrow}
            </p>

            <h2 className="mt-5 max-w-[430px] font-serif text-5xl leading-[0.96] tracking-[-0.035em] text-(--ink) sm:text-6xl">
              {business.servicesIntro.title}
            </h2>

            <p className="mt-7 max-w-[430px] text-sm leading-7 text-(--p-72665e)">
              {business.servicesIntro.description}
            </p>
          </div>

          {/* =====================================================
              SERVICES LIST
          ====================================================== */}
          <div className="border-t border-(--p-342d28)/15">
            {business.services.map((service) => (
              <article
                key={service.id}
                className="group grid gap-5 border-b border-(--p-342d28)/15 py-7 transition-all duration-300 sm:grid-cols-[60px_1fr_auto] sm:items-center"
              >
                {/* Number */}
                <span className="font-serif text-lg text-(--p-a08a79)">
                  {service.number}
                </span>

                {/* Service content */}
                <div>
                  <div className="flex flex-wrap items-center gap-3">
                    <h3 className="font-serif text-2xl text-(--p-2f2925) transition-colors duration-300 group-hover:text-(--accent-line)">
                      {service.name}
                    </h3>

                    {service.featured && (
                      <span className="rounded-full border border-(--accent-line)/20 bg-(--accent-line)/[0.06] px-2.5 py-1 text-[9px] font-semibold uppercase tracking-[0.14em] text-(--accent-strong)">
                        {business.servicesIntro.featuredLabel}
                      </span>
                    )}
                  </div>

                  <p className="mt-2 max-w-[620px] text-sm leading-6 text-(--p-756a61)">
                    {service.description}
                  </p>

                  <p className="mt-3 text-[10px] font-semibold uppercase tracking-[0.16em] text-(--p-9b897b)">
                    {service.category}
                  </p>
                </div>

                {/* Duration + price */}
                <div className="flex items-center gap-6 sm:flex-col sm:items-end sm:gap-1">
                  <span className="text-sm text-(--p-665a52)">
                    {service.duration}
                  </span>

                  <strong className="font-serif text-xl font-normal text-(--p-302925)">
                    {service.price}
                  </strong>
                </div>
              </article>
            ))}

            {/* =================================================
                CTA
            ================================================== */}
            <a
              href={business.servicesIntro.cta.href}
              className="group mt-8 inline-flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.18em] text-(--p-463a32) transition-colors duration-300 hover:text-(--accent-line)"
            >
              {business.servicesIntro.cta.label}

              <span className="transition-transform duration-300 group-hover:translate-x-1">
                <ArrowIcon />
              </span>
            </a>
          </div>
        </div>
      </div>
    </section>
  );
}