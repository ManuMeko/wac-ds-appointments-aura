// apps/web/components/home/ServicesSection.tsx

import { PinAside } from "@/components/home/PinAside";
import { Reveal } from "@/components/motion/Reveal";
import { ArrowIcon } from "@/components/ui/ArrowIcon";
import { TradeIcon } from "@/components/ui/TradeIcon";
import { business } from "@/config/business";

/* =========================================================
   WAC DS · MODEL 01 · APPOINTMENTS
   Aura Studio · Services Section
========================================================= */

export function ServicesSection() {
  const taller = business.theme === "taller";
  const whatsappHref = `https://wa.me/${business.contact.whatsappNumber}`;

  const intro = (
    <>
      <p className="text-[10px] font-semibold uppercase tracking-[0.27em] text-(--p-8b725f)">
        {business.servicesIntro.eyebrow}
      </p>

      <Reveal variant="fade-up">
        <h2 className="mt-5 max-w-[430px] font-serif text-5xl leading-[0.96] tracking-[-0.035em] text-(--ink) sm:text-6xl">
          {business.servicesIntro.title}
        </h2>
      </Reveal>

      <p className="mt-7 max-w-[430px] text-sm leading-7 text-(--p-72665e)">
        {business.servicesIntro.description}
      </p>
    </>
  );

  return (
    <section id="servicios" className="bg-(--canvas)">
      <div className="mx-auto w-full max-w-[1440px] px-5 py-20 sm:px-8 lg:px-12 lg:py-28 xl:px-16">
        <div className="grid gap-10 lg:grid-cols-[0.8fr_1.2fr]">
          {taller ? (
            <PinAside>{intro}</PinAside>
          ) : (
            <div>{intro}</div>
          )}

          <div className={taller ? "min-w-0" : "border-t border-(--p-342d28)/15"}>
            {business.services.map((service, index) => {
              if (!taller) {
                return (
                  <article
                    key={service.id}
                    className="group grid gap-5 border-b border-(--p-342d28)/15 py-7 transition-all duration-300 sm:grid-cols-[60px_1fr_auto] sm:items-center"
                  >
                    <span className="font-serif text-lg text-(--p-a08a79)">
                      {service.number}
                    </span>

                    <div>
                      <div className="flex flex-wrap items-center gap-3">
                        {service.icon ? (
                          <TradeIcon
                            name={service.icon}
                            className="h-5 w-5 shrink-0 text-(--accent)"
                          />
                        ) : null}
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

                    <div className="flex items-center gap-6 sm:flex-col sm:items-end sm:gap-1">
                      <span className="text-sm text-(--p-665a52)">
                        {service.duration}
                      </span>

                      <strong className="font-serif text-xl font-normal text-(--p-302925)">
                        {service.price}
                      </strong>
                    </div>
                  </article>
                );
              }

              return (
                <Reveal key={service.id} delay={index * 90}>
                  <a href="#contacto" className="home-service-row group">
                    <span className="home-service-num">{service.number}</span>

                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-3">
                        {service.icon ? (
                          <TradeIcon
                            name={service.icon}
                            className="h-5 w-5 shrink-0 text-(--accent)"
                          />
                        ) : null}
                        <h3 className="font-serif text-2xl text-(--heading)">
                          {service.name}
                        </h3>
                        {service.featured ? (
                          <span className="home-habit">
                            <i aria-hidden="true" />
                            {business.servicesIntro.featuredLabel}
                          </span>
                        ) : null}
                      </div>
                      <p className="pt-2 text-sm leading-6 text-(--ink-muted)">
                        {service.description}
                      </p>
                    </div>

                    <div className="home-service-meta">
                      <span className="home-duration">{service.duration}</span>
                      <strong className="home-price">{service.price}</strong>
                      <span className="home-service-arrow" aria-hidden="true">
                        <ArrowIcon />
                      </span>
                    </div>
                  </a>
                </Reveal>
              );
            })}

            {taller ? (
              <div className="home-service-help">
                <h3>¿No sabes qué necesita tu coche?</h3>
                <p>Cuéntanos el síntoma y te orientamos.</p>
                <div className="home-service-help-actions">
                  <a className="home-cta-fill" href="#contacto">
                    Reservar diagnosis
                  </a>
                  <a
                    className="home-cta-ghost"
                    href={whatsappHref}
                    target="_blank"
                    rel="noreferrer"
                  >
                    {business.contactSection.whatsappCta.label}
                  </a>
                </div>
              </div>
            ) : (
              <a
                href={business.servicesIntro.cta.href}
                className="group mt-8 inline-flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.18em] text-(--p-463a32) transition-colors duration-300 hover:text-(--accent-line)"
              >
                {business.servicesIntro.cta.label}

                <span className="transition-transform duration-300 group-hover:translate-x-1">
                  <ArrowIcon />
                </span>
              </a>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}
