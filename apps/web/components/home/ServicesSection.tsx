// apps/web/components/home/ServicesSection.tsx

"use client";

import Image from "next/image";

import { PinAside } from "@/components/home/PinAside";
import { MaskLines } from "@/components/motion/MaskLines";
import { Reveal } from "@/components/motion/Reveal";
import { ArrowIcon } from "@/components/ui/ArrowIcon";
import { TradeIcon } from "@/components/ui/TradeIcon";
import { business } from "@/config/business";
import {
  formatPublicPrice,
  useHomeAvailability,
} from "@/lib/homeAvailability";

/* =========================================================
   WAC DS · MODEL 01 · APPOINTMENTS
   Aura Studio · Services Section
========================================================= */

const AURA_SERVICE_STILLS: Record<string, { src: string; alt: string }> = {
  "facial-treatment": {
    src: "/images/aura/hero-aura-studio.png",
    alt: "Cabina de tratamiento de Aura Studio",
  },
  "deep-cleansing": {
    src: "/images/aura/experience-aura-studio.png",
    alt: "Detalle de cosmética y textiles en Aura Studio",
  },
  "relaxing-massage": {
    src: "/images/aura/sequence/frame_0048.webp",
    alt: "Sesión de bienestar en Aura Studio",
  },
  "premium-manicure": {
    src: "/images/aura/sequence/frame_0072.webp",
    alt: "Detalle de cuidado en Aura Studio",
  },
};

export function ServicesSection() {
  const taller = business.theme === "taller";
  const whatsappHref = `https://wa.me/${business.contact.whatsappNumber}`;
  const availability = useHomeAvailability();
  const catalog =
    availability?.status === "ready" ? availability.catalog : null;

  function liveMeta(bookingSlug: string | undefined, fallback: {
    duration: string;
    price: string;
  }) {
    const match = bookingSlug
      ? catalog?.find((service) => service.slug === bookingSlug)
      : undefined;
    if (!match) return fallback;
    const price = formatPublicPrice(match.price, match.currency);
    return {
      duration: `${match.durationMinutes} min`,
      price: price ?? fallback.price,
    };
  }

  const intro = (
    <>
      <p className="text-[10px] font-semibold uppercase tracking-[0.27em] text-(--p-8b725f)">
        {business.servicesIntro.eyebrow}
      </p>

      <Reveal variant="fade-up">
        <h2 className="aura-display mt-5 max-w-[12ch] font-serif text-5xl leading-[0.96] tracking-[-0.035em] text-(--ink) sm:text-6xl">
          {taller ? (
            business.servicesIntro.title
          ) : (
            <MaskLines text={business.servicesIntro.title} />
          )}
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

          <div className={taller ? "min-w-0" : "aura-service-grid"}>
            {business.services.map((service, index) => {
              const meta = liveMeta(service.bookingSlug, {
                duration: service.duration,
                price: service.price,
              });
              const bookingHref = service.bookingSlug
                ? `/reservar?servicio=${encodeURIComponent(service.bookingSlug)}`
                : "/reservar";

              if (!taller) {
                const still = AURA_SERVICE_STILLS[service.id];
                return (
                  <Reveal key={service.id} delay={index * 80}>
                    <a href={bookingHref} className="aura-service group">
                      <div className="aura-service-media">
                        {still ? (
                          <Image
                            src={still.src}
                            alt={still.alt}
                            fill
                            sizes="(min-width: 1024px) 28vw, (min-width: 768px) 46vw, 100vw"
                            className="aura-service-photo object-cover"
                          />
                        ) : null}
                        <div className="aura-service-veil">
                          <span className="aura-rule" aria-hidden="true" />
                          <strong className="mt-4 font-serif text-3xl font-medium tracking-[-0.03em]">
                            {meta.price}
                          </strong>
                          <span className="mt-1 text-[11px] font-medium uppercase tracking-[0.22em]">
                            {meta.duration}
                          </span>
                          <span className="mt-4 text-[10px] font-semibold uppercase tracking-[0.2em]">
                            Reservar cita
                          </span>
                        </div>
                      </div>
                      <div className="px-1 pt-5">
                        <p className="text-[10px] font-medium uppercase tracking-[0.26em] text-(--accent)">
                          {service.number}
                          {service.category ? ` · ${service.category}` : ""}
                        </p>
                        <h3 className="mt-2 font-serif text-[clamp(1.6rem,2vw,2rem)] leading-none tracking-[-0.03em] text-(--ink)">
                          {service.name}
                        </h3>
                        <p className="mt-3 text-sm font-light leading-6 text-(--ink-muted)">
                          {service.description}
                        </p>
                      </div>
                    </a>
                  </Reveal>
                );
              }

              return (
                <Reveal key={service.id} delay={index * 90}>
                  <a href={bookingHref} className="home-service-row group">
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
                      <span className="home-duration">{meta.duration}</span>
                      <strong className="home-price">{meta.price}</strong>
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
                  <a className="home-cta-fill" href="/reservar?servicio=diagnosis">
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
                data-magnetic=""
                className="group col-span-full mt-4 inline-flex h-12 w-fit items-center gap-3 justify-self-start rounded-full bg-(--button-bg) px-7 text-[11px] font-semibold uppercase tracking-[0.18em] !text-(--button-text)"
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
