// apps/web/components/home/TeamSection.tsx

import Image from "next/image";

import { TeamPortrait } from "@/components/home/TeamPortrait";
import { Reveal } from "@/components/motion/Reveal";
import { business } from "@/config/business";

/* =========================================================
   WAC DS · MODEL 01 · APPOINTMENTS
   Aura Studio · Team Section
========================================================= */

export function TeamSection() {
  const taller = business.theme === "taller";

  return (
    <section id="equipo" className="bg-(--canvas)">
      <div className="mx-auto w-full max-w-[1440px] px-5 py-20 sm:px-8 lg:px-12 lg:py-28 xl:px-16">
        {/* =====================================================
            INTRO
        ====================================================== */}
        <div className="flex flex-col justify-between gap-8 lg:flex-row lg:items-end">
          <div>
            <p className="text-[10px] font-semibold uppercase tracking-[0.27em] text-(--p-89715f)">
              {business.teamIntro.eyebrow}
            </p>

            <Reveal variant="fade-up">
              <h2 className="mt-5 max-w-[650px] font-serif text-5xl leading-[0.97] tracking-[-0.035em] text-(--p-2d2723) sm:text-6xl">
                {business.teamIntro.title}
              </h2>
            </Reveal>
          </div>

          <p className="max-w-[440px] text-sm leading-7 text-(--p-74685f)">
            {business.teamIntro.description}
          </p>
        </div>

        {/* =====================================================
            PROFESSIONALS GRID
        ====================================================== */}
        <div
          className={
            taller
              ? "mt-14 flex snap-x snap-mandatory gap-4 overflow-x-auto pb-4 md:grid md:grid-cols-3 md:overflow-visible md:pb-0"
              : "mt-14 grid gap-6 md:grid-cols-3"
          }
        >
          {business.professionals.map((professional, index) => {
            const imageSrc = professional.image ?? business.assets.team[professional.id];
            const alt = `${professional.name}, ${professional.role} en ${business.brand.name}`;

            const card = (
              <article
                key={professional.id}
                className={
                  taller
                    ? "group w-[min(82vw,22rem)] shrink-0 snap-center overflow-hidden rounded-[1.7rem] border border-transparent bg-white shadow-[0_22px_60px_rgba(28,25,23,0.1)] transition-[transform,border-color,box-shadow] duration-300 hover:-translate-y-1 hover:border-[#f97316] hover:shadow-[0_28px_70px_rgba(28,25,23,0.16)] md:w-auto"
                    : "group overflow-hidden rounded-[1.7rem] border border-(--p-352e29)/10 bg-(--p-eee7df) shadow-[0_20px_60px_rgba(56,46,40,0.06)] transition-all duration-300 hover:-translate-y-1 hover:shadow-[0_28px_80px_rgba(56,46,40,0.11)]"
                }
              >
                {/* =================================================
                    VISUAL
                ================================================== */}
                <div className="relative aspect-[4/5] overflow-hidden bg-(--p-d8c6b7)">
                  {taller ? (
                    <TeamPortrait src={imageSrc ?? ""} alt={alt} initials={professional.initials} />
                  ) : imageSrc ? (
                    <Image
                      src={imageSrc}
                      alt={alt}
                      fill
                      unoptimized={imageSrc.endsWith(".svg")}
                      quality={90}
                      sizes="(min-width: 768px) 33vw, 100vw"
                      className="object-cover object-center transition-transform duration-700 group-hover:scale-[1.025]"
                    />
                  ) : (
                    <div className="flex h-full w-full items-center justify-center bg-[linear-gradient(145deg,var(--p-d8c6b7),var(--p-9e8370))]">
                      <span className="font-serif text-6xl text-white/80">
                        {professional.initials}
                      </span>
                    </div>
                  )}

                  {/* Soft treatment */}
                  <div
                    className={
                      taller
                        ? "pointer-events-none absolute inset-0 bg-gradient-to-t from-[#1c1917]/75 via-[#1c1917]/5 to-transparent"
                        : "pointer-events-none absolute inset-0 bg-gradient-to-t from-(--p-2d211b)/38 via-transparent to-white/[0.03]"
                    }
                  />

                  <div className="pointer-events-none absolute inset-0 ring-1 ring-inset ring-white/10" />

                  {/* Role badge */}
                  <div className="absolute bottom-5 left-5 z-10 rounded-full border border-white/25 bg-(--p-382b24)/25 px-3.5 py-1.5 backdrop-blur-md">
                    <span className="text-[9px] font-semibold uppercase tracking-[0.18em] !text-white">
                      {professional.role}
                    </span>
                  </div>
                </div>

                {/* =================================================
                    CONTENT
                ================================================== */}
                <div className="p-6">
                  <p className="text-[10px] font-semibold uppercase tracking-[0.19em] text-(--p-9a806c)">
                    {professional.role}
                  </p>

                  <h3 className="mt-2 font-serif text-2xl text-(--p-302925)">
                    {professional.name}
                  </h3>

                  <p className="mt-4 text-sm leading-6 text-(--p-76695f)">
                    {professional.description}
                  </p>

                  {/* =================================================
                      SPECIALTIES
                  ================================================== */}
                  <div className="mt-5 flex flex-wrap gap-2">
                    {professional.specialties.map((specialty) => (
                      <span
                        key={specialty}
                        className="rounded-full border border-(--accent-line)/15 bg-(--accent-line)/[0.05] px-3 py-1.5 text-[9px] font-semibold uppercase tracking-[0.12em] text-(--p-856c59)"
                      >
                        {specialty}
                      </span>
                    ))}
                  </div>
                </div>
              </article>
            );

            return taller ? (
              <Reveal key={professional.id} variant="scale-in" delay={index * 80}>
                {card}
              </Reveal>
            ) : (
              card
            );
          })}
        </div>
      </div>
    </section>
  );
}