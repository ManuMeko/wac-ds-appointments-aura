// apps/web/components/home/HeroSection.tsx

import Image from "next/image";

import { ArrowIcon } from "@/components/ui/ArrowIcon";
import { auraBrand, auraHero, auraStats } from "@/data/aura";

/* =========================================================
   WAC DS · MODEL 01 · APPOINTMENTS
   Aura Studio · Hero Section
========================================================= */

export function HeroSection() {
  return (
    <section
      id="inicio"
      className="relative isolate overflow-hidden"
    >
      {/* =====================================================
          BACKGROUND
      ====================================================== */}
      <div className="absolute inset-0 -z-20 bg-[#e9dfd4]" />

      <div className="absolute -right-24 -top-48 -z-10 h-[640px] w-[640px] rounded-full bg-[#d9c3ae]/70 blur-3xl" />

      <div className="absolute -bottom-80 -left-56 -z-10 h-[720px] w-[720px] rounded-full bg-[#ede6de] blur-3xl" />

      <div className="mx-auto grid w-full max-w-[1440px] grid-cols-1 lg:min-h-[calc(100vh-76px)] lg:grid-cols-[0.98fr_1.02fr]">
        {/* =====================================================
            COPY
        ====================================================== */}
        <div className="flex items-center px-5 py-16 sm:px-8 sm:py-20 lg:px-12 lg:py-20 xl:px-16">
          <div className="max-w-[670px]">
            {/* Eyebrow */}
            <div className="mb-8 flex items-center gap-4">
              <span className="h-px w-12 bg-[#8b7463]" />

              <p className="text-[11px] font-semibold uppercase tracking-[0.25em] text-[#7d695a]">
                {auraHero.eyebrow}
              </p>
            </div>

            {/* Heading */}
            <h1 className="max-w-[660px] font-serif text-[clamp(3.5rem,7vw,6.8rem)] leading-[0.88] tracking-[-0.055em] text-[#29231f]">
              {auraHero.title}

              <span className="block italic text-[#8b6d59]">
                {auraHero.highlightedTitle}
              </span>
            </h1>

            {/* Description */}
            <p className="mt-8 max-w-[560px] text-base leading-7 text-[#655a52] sm:text-lg sm:leading-8">
              {auraHero.description}
            </p>

            {/* =================================================
                CTAs
            ================================================== */}
            <div className="mt-10 flex flex-col gap-3 sm:flex-row">
              <a
                href={auraHero.primaryCta.href}
                className="group inline-flex h-14 items-center justify-center gap-3 rounded-full bg-[#2b2521] px-8 text-[11px] font-semibold uppercase tracking-[0.2em] !text-[#fffaf6] shadow-[0_12px_30px_rgba(43,37,33,0.14)] transition-all duration-300 hover:-translate-y-0.5 hover:bg-[#4c3f36] hover:shadow-[0_16px_36px_rgba(43,37,33,0.20)]"
              >
                {auraHero.primaryCta.label}

                <span className="transition-transform duration-300 group-hover:translate-x-1">
                  <ArrowIcon />
                </span>
              </a>

              <a
                href={auraHero.secondaryCta.href}
                className="inline-flex h-14 items-center justify-center rounded-full border border-[#3b332e]/20 bg-white/10 px-8 text-[11px] font-semibold uppercase tracking-[0.2em] text-[#332c27] transition-all duration-300 hover:-translate-y-0.5 hover:border-[#3b332e]/35 hover:bg-white/40"
              >
                {auraHero.secondaryCta.label}
              </a>
            </div>

            {/* =================================================
                STATS
            ================================================== */}
            <div className="mt-14 grid max-w-[560px] grid-cols-2 gap-x-6 gap-y-5 border-t border-[#3b332e]/15 pt-7 sm:grid-cols-4">
              {auraStats.map((stat) => (
                <div key={`${stat.value}-${stat.label}`}>
                  <strong className="block font-serif text-2xl font-normal text-[#332c27]">
                    {stat.value}
                  </strong>

                  <span className="mt-1 block text-[10px] uppercase tracking-[0.13em] text-[#82746a]">
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
          <div className="relative aspect-[4/3] w-full overflow-hidden rounded-[2rem] border border-white/30 bg-[#bda693] shadow-[0_40px_100px_rgba(52,43,36,0.18)]">
            <Image
              src="/images/aura/hero-aura-studio.png"
              alt="Cabina premium de Aura Studio con camilla de tratamiento, iluminación cálida y elementos de bienestar"
              fill
              priority
              quality={92}
              sizes="(min-width: 1280px) 620px, (min-width: 1024px) 48vw, 100vw"
              className="object-cover object-center"
            />

            {/* Suavizado para integrar imagen y overlays */}
            <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-[#34271f]/28 via-transparent to-white/[0.03]" />

            <div className="pointer-events-none absolute inset-0 ring-1 ring-inset ring-white/10" />

            {/* =================================================
                LOCATION BADGE
            ================================================== */}
            <div className="absolute right-6 top-6 z-10 rounded-full border border-white/35 bg-[#372b23]/20 px-4 py-2 shadow-sm backdrop-blur-md">
              <span className="text-[10px] font-semibold uppercase tracking-[0.16em] !text-white">
                {auraBrand.locationLabel}
              </span>
            </div>

            {/* =================================================
                SIGNATURE CARD
            ================================================== */}
            <div className="absolute bottom-6 left-6 right-6 z-10 rounded-[1.4rem] border border-white/30 bg-[#f8f1e9]/88 p-6 shadow-[0_16px_50px_rgba(45,34,27,0.16)] backdrop-blur-xl">
              <div className="flex items-end justify-between gap-6">
                <div>
                  <p className="text-[10px] font-semibold uppercase tracking-[0.22em] text-[#8c7565]">
                    {auraBrand.signature.eyebrow}
                  </p>

                  <p className="mt-2 max-w-[320px] font-serif text-2xl leading-tight text-[#322b26]">
                    {auraBrand.signature.title}
                  </p>
                </div>

                <a
                  href={auraHero.primaryCta.href}
                  aria-label={auraHero.primaryCta.label}
                  className="group flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-[#2d2723] !text-[#fffaf6] shadow-[0_10px_24px_rgba(45,39,35,0.18)] transition-all duration-300 hover:-translate-y-0.5 hover:bg-[#4b3d34]"
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
          <div className="relative aspect-[4/3] overflow-hidden rounded-[1.75rem] border border-white/30 bg-[#bda693] shadow-[0_24px_70px_rgba(52,43,36,0.16)]">
            <Image
              src="/images/aura/hero-aura-studio.png"
              alt="Cabina premium de Aura Studio con camilla de tratamiento, iluminación cálida y elementos de bienestar"
              fill
              priority
              quality={90}
              sizes="(max-width: 1023px) 100vw"
              className="object-cover object-center"
            />

            <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-[#34271f]/38 via-transparent to-transparent" />

            <div className="absolute bottom-5 left-5 right-5 rounded-[1.25rem] border border-white/25 bg-[#f8f1e9]/88 p-5 shadow-[0_14px_40px_rgba(45,34,27,0.14)] backdrop-blur-xl">
              <p className="text-[9px] font-semibold uppercase tracking-[0.2em] text-[#8c7565]">
                {auraBrand.signature.eyebrow}
              </p>

              <p className="mt-2 max-w-[280px] font-serif text-2xl leading-tight text-[#322b26]">
                {auraBrand.signature.title}
              </p>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}