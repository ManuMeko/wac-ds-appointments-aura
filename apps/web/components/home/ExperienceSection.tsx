// apps/web/components/home/ExperienceSection.tsx

import Image from "next/image";

import { CheckIcon } from "@/components/ui/CheckIcon";
import { auraBenefits, auraExperience } from "@/data/aura";

/* =========================================================
   WAC DS · MODEL 01 · APPOINTMENTS
   Aura Studio · Experience Section
========================================================= */

export function ExperienceSection() {
  return (
    <section className="bg-[#eee5dc]">
      <div className="mx-auto grid w-full max-w-[1440px] gap-12 px-5 py-20 sm:px-8 lg:grid-cols-[0.95fr_1.05fr] lg:items-center lg:gap-16 lg:px-12 lg:py-28 xl:px-16">
        {/* =====================================================
            VISUAL
        ====================================================== */}
        <div className="relative">
          <div className="relative aspect-[4/5] overflow-hidden rounded-[2rem] border border-white/25 bg-[#bda693] shadow-[0_30px_80px_rgba(72,56,45,0.14)] sm:aspect-[4/3] lg:aspect-[4/5] xl:aspect-[5/6]">
            <Image
              src="/images/aura/experience-aura-studio.png"
              alt="Detalle de la experiencia Aura Studio con productos de bienestar, textiles suaves y ambiente de tratamiento premium"
              fill
              quality={92}
              sizes="(min-width: 1280px) 560px, (min-width: 1024px) 45vw, 100vw"
              className="object-cover object-center"
            />

            {/* Tratamiento visual suave */}
            <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-[#32261f]/35 via-transparent to-white/[0.03]" />

            <div className="pointer-events-none absolute inset-0 ring-1 ring-inset ring-white/10" />

            {/* =================================================
                EXPERIENCE LABEL
            ================================================== */}
            <div className="absolute left-5 top-5 z-10 rounded-full border border-white/25 bg-[#3b2e26]/20 px-4 py-2 backdrop-blur-md sm:left-6 sm:top-6">
              <span className="text-[9px] font-semibold uppercase tracking-[0.2em] !text-white sm:text-[10px]">
                Ritual Aura
              </span>
            </div>

            {/* =================================================
                VISUAL COPY CARD
            ================================================== */}
            <div className="absolute bottom-5 left-5 right-5 z-10 rounded-[1.4rem] border border-white/30 bg-[#f6ede4]/88 p-5 shadow-[0_16px_45px_rgba(45,34,27,0.14)] backdrop-blur-xl sm:bottom-7 sm:left-7 sm:right-7 sm:p-7">
              <p className="max-w-[390px] font-serif text-3xl leading-[1.02] tracking-[-0.025em] text-[#302925] sm:text-4xl">
                {auraExperience.visualTitle}
              </p>

              <p className="mt-4 max-w-md text-sm leading-6 text-[#685a51]">
                {auraExperience.visualDescription}
              </p>
            </div>
          </div>

          {/* =====================================================
              DECORATIVE DETAIL
          ====================================================== */}
          <div className="pointer-events-none absolute -bottom-5 -right-5 -z-10 hidden h-28 w-28 rounded-full border border-[#8b6d59]/15 lg:block" />

          <div className="pointer-events-none absolute -left-7 top-16 -z-10 hidden h-20 w-20 rounded-full bg-[#d6c0ad]/45 blur-2xl lg:block" />
        </div>

        {/* =====================================================
            CONTENT
        ====================================================== */}
        <div className="flex items-center">
          <div className="w-full">
            {/* Eyebrow */}
            <p className="text-[10px] font-semibold uppercase tracking-[0.27em] text-[#89705e]">
              {auraExperience.eyebrow}
            </p>

            {/* Heading */}
            <h2 className="mt-5 max-w-[600px] font-serif text-5xl leading-[0.97] tracking-[-0.035em] text-[#2d2723] sm:text-6xl">
              {auraExperience.title}
            </h2>

            {/* Description */}
            <p className="mt-7 max-w-[590px] text-base leading-8 text-[#70635b]">
              {auraExperience.description}
            </p>

            {/* =================================================
                BENEFITS
            ================================================== */}
            <div className="mt-10 grid gap-5 sm:grid-cols-2">
              {auraBenefits.map((benefit) => (
                <div
                  key={benefit.id}
                  className="group flex items-center gap-3 border-t border-[#41372f]/15 pt-5"
                >
                  <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[#d9c7b8] text-[#5a493d] transition-all duration-300 group-hover:scale-105 group-hover:bg-[#ceb7a4]">
                    <CheckIcon />
                  </span>

                  <span className="text-sm font-medium text-[#40362f]">
                    {benefit.label}
                  </span>
                </div>
              ))}
            </div>

            {/* =================================================
                SUPPORTING NOTE
            ================================================== */}
            <div className="mt-10 border-l border-[#8b6d59]/35 pl-5">
              <p className="max-w-[510px] font-serif text-xl leading-7 text-[#5d4d42]">
                Cada sesión está diseñada para que el tratamiento empiece antes
                incluso de entrar en cabina.
              </p>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}