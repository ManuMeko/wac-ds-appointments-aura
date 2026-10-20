// apps/web/components/home/HeroSection.tsx

import Image from "next/image";

import { ArrowIcon } from "@/components/ui/ArrowIcon";
import { business } from "@/config/business";

/* =========================================================
   WAC DS · MODEL 01 · APPOINTMENTS
   Aura Studio · Hero Section
========================================================= */

const PUBLIC_BOOKING_HREF = "/reservar";

export function HeroSection() {
  return (
    <section
      id="inicio"
      className="relative isolate overflow-hidden"
    >
      {/* =====================================================
          BACKGROUND
      ====================================================== */}
      <div className="absolute inset-0 -z-20 bg-(--hero-bg)" />

      <div className="absolute -right-24 -top-48 -z-10 h-[640px] w-[640px] rounded-full bg-(--p-d9c3ae)/70 blur-3xl" />

      <div className="absolute -bottom-80 -left-56 -z-10 h-[720px] w-[720px] rounded-full bg-(--p-ede6de) blur-3xl" />

      <div className="mx-auto grid w-full max-w-[1440px] grid-cols-1 lg:min-h-[calc(100vh-76px)] lg:grid-cols-[0.98fr_1.02fr]">
        {/* =====================================================
            COPY
        ====================================================== */}
        <div className="flex items-center px-5 py-16 sm:px-8 sm:py-20 lg:px-12 lg:py-20 xl:px-16">
          <div className="max-w-[670px]">
            {/* Eyebrow */}
            <div className="mb-8 flex items-center gap-4">
              <span className="h-px w-12 bg-(--p-8b7463)" />

              <p className="text-[11px] font-semibold uppercase tracking-[0.25em] text-(--p-7d695a)">
                {business.hero.eyebrow}
              </p>
            </div>

            {/* Heading */}
            <h1 className="max-w-[660px] font-serif text-[clamp(3.5rem,7vw,6.8rem)] leading-[0.88] tracking-[-0.055em] text-(--ink)">
              {business.hero.title}

              <span className="block italic text-(--accent)">
                {business.hero.highlightedTitle}
              </span>
            </h1>

            {/* Description */}
            <p className="mt-8 max-w-[560px] text-base leading-7 text-(--p-655a52) sm:text-lg sm:leading-8">
              {business.hero.description}
            </p>

            {/* =================================================
                CTAs
            ================================================== */}
            <div className="mt-10 flex flex-col gap-3 sm:flex-row">
              <a
                href={PUBLIC_BOOKING_HREF}
                className="group inline-flex h-14 items-center justify-center gap-3 rounded-full bg-(--p-2b2521) px-8 text-[11px] font-semibold uppercase tracking-[0.2em] !text-(--on-inverse) shadow-[0_12px_30px_rgba(43,37,33,0.14)] transition-all duration-300 hover:-translate-y-0.5 hover:bg-(--p-4c3f36) hover:shadow-[0_16px_36px_rgba(43,37,33,0.20)]"
              >
                {business.hero.primaryCta.label}

                <span className="transition-transform duration-300 group-hover:translate-x-1">
                  <ArrowIcon />
                </span>
              </a>

              <a
                href={business.hero.secondaryCta.href}
                className="inline-flex h-14 items-center justify-center rounded-full border border-(--p-3b332e)/20 bg-white/10 px-8 text-[11px] font-semibold uppercase tracking-[0.2em] text-(--p-332c27) transition-all duration-300 hover:-translate-y-0.5 hover:border-(--p-3b332e)/35 hover:bg-white/40"
              >
                {business.hero.secondaryCta.label}
              </a>
            </div>

            {/* =================================================
                STATS
            ================================================== */}
            <div className="mt-14 grid max-w-[560px] grid-cols-2 gap-x-6 gap-y-5 border-t border-(--p-3b332e)/15 pt-7 sm:grid-cols-4">
              {business.stats.map((stat) => (
                <div key={`${stat.value}-${stat.label}`}>
                  <strong className="block font-serif text-2xl font-normal text-(--p-332c27)">
                    {stat.value}
                  </strong>

                  <span className="mt-1 block text-[10px] uppercase tracking-[0.13em] text-(--p-82746a)">
                    {stat.label}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* =====================================================
            DESKTOP VISUAL
        ====================================================== */}
        <div className="hidden items-center px-8 py-12 lg:flex xl:px-10">
          <div className="relative aspect-[4/3] w-full overflow-hidden rounded-[2rem] border border-white/30 bg-(--p-bda693) shadow-[0_40px_100px_rgba(52,43,36,0.18)]">
            <Image
              src={business.assets.hero}
              alt={business.imageAlts.hero}
              fill
              priority
              unoptimized={business.assets.hero.endsWith(".svg")}
              quality={92}
              sizes="(min-width: 1280px) 620px, (min-width: 1024px) 48vw, 100vw"
              className="object-cover object-center"
            />

            {/* Suavizado para integrar imagen y overlays */}
            <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-(--p-34271f)/28 via-transparent to-white/[0.03]" />

            <div className="pointer-events-none absolute inset-0 ring-1 ring-inset ring-white/10" />

            {/* =================================================
                LOCATION BADGE
            ================================================== */}
            <div className="absolute right-6 top-6 z-10 rounded-full border border-white/35 bg-(--p-372b23)/20 px-4 py-2 shadow-sm backdrop-blur-md">
              <span className="text-[10px] font-semibold uppercase tracking-[0.16em] !text-white">
                {business.brand.locationLabel}
              </span>
            </div>

            {/* =================================================
                SIGNATURE CARD
            ================================================== */}
            <div className="absolute bottom-6 left-6 right-6 z-10 rounded-[1.4rem] border border-white/30 bg-(--p-f8f1e9)/88 p-6 shadow-[0_16px_50px_rgba(45,34,27,0.16)] backdrop-blur-xl">
              <div className="flex items-end justify-between gap-6">
                <div>
                  <p className="text-[10px] font-semibold uppercase tracking-[0.22em] text-(--p-8c7565)">
                    {business.brand.signature.eyebrow}
                  </p>

                  <p className="mt-2 max-w-[320px] font-serif text-2xl leading-tight text-(--p-322b26)">
                    {business.brand.signature.title}
                  </p>
                </div>

                <a
                  href={PUBLIC_BOOKING_HREF}
                  aria-label={business.hero.primaryCta.label}
                  className="group flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-(--p-2d2723) !text-(--on-inverse) shadow-[0_10px_24px_rgba(45,39,35,0.18)] transition-all duration-300 hover:-translate-y-0.5 hover:bg-(--p-4b3d34)"
                >
                  <span className="transition-transform duration-300 group-hover:translate-x-0.5">
                    <ArrowIcon />
                  </span>
                </a>
              </div>
            </div>
          </div>
        </div>

        {/* =====================================================
            MOBILE / TABLET VISUAL
        ====================================================== */}
        <div className="px-5 pb-16 sm:px-8 lg:hidden">
          <div className="relative aspect-[4/3] overflow-hidden rounded-[1.75rem] border border-white/30 bg-(--p-bda693) shadow-[0_24px_70px_rgba(52,43,36,0.16)]">
            <Image
              src={business.assets.hero}
              alt={business.imageAlts.hero}
              fill
              priority
              unoptimized={business.assets.hero.endsWith(".svg")}
              quality={90}
              sizes="(max-width: 1023px) 100vw"
              className="object-cover object-center"
            />

            <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-(--p-34271f)/38 via-transparent to-transparent" />

            <div className="absolute bottom-5 left-5 right-5 rounded-[1.25rem] border border-white/25 bg-(--p-f8f1e9)/88 p-5 shadow-[0_14px_40px_rgba(45,34,27,0.14)] backdrop-blur-xl">
              <p className="text-[9px] font-semibold uppercase tracking-[0.2em] text-(--p-8c7565)">
                {business.brand.signature.eyebrow}
              </p>

              <p className="mt-2 max-w-[280px] font-serif text-2xl leading-tight text-(--p-322b26)">
                {business.brand.signature.title}
              </p>

              <a
                href={PUBLIC_BOOKING_HREF}
                className="group mt-5 inline-flex h-12 items-center justify-center gap-2 rounded-full bg-(--p-2d2723) px-6 text-[10px] font-semibold uppercase tracking-[0.18em] !text-(--on-inverse) shadow-[0_10px_24px_rgba(45,39,35,0.16)] transition-all duration-300 hover:bg-(--p-4b3d34)"
              >
                {business.hero.primaryCta.label}

                <span className="transition-transform duration-300 group-hover:translate-x-0.5">
                  <ArrowIcon />
                </span>
              </a>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}