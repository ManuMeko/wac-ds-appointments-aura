// apps/web/components/home/TeamSection.tsx

import Image from "next/image";

import {
  auraProfessionals,
  auraTeamIntro,
} from "@/data/aura";

/* =========================================================
   WAC DS · MODEL 01 · APPOINTMENTS
   Aura Studio · Team Section
========================================================= */

const professionalImages: Record<string, string> = {
  "laura-martin": "/images/aura/team/laura-martin.png",
  "andrea-lopez": "/images/aura/team/andrea-lopez.png",
  "sofia-garcia": "/images/aura/team/sofia-garcia.png",
};

export function TeamSection() {
  return (
    <section id="equipo" className="bg-[#f8f5f0]">
      <div className="mx-auto w-full max-w-[1440px] px-5 py-20 sm:px-8 lg:px-12 lg:py-28 xl:px-16">
        {/* =====================================================
            INTRO
        ====================================================== */}
        <div className="flex flex-col justify-between gap-8 lg:flex-row lg:items-end">
          <div>
            <p className="text-[10px] font-semibold uppercase tracking-[0.27em] text-[#89715f]">
              {auraTeamIntro.eyebrow}
            </p>

            <h2 className="mt-5 max-w-[650px] font-serif text-5xl leading-[0.97] tracking-[-0.035em] text-[#2d2723] sm:text-6xl">
              {auraTeamIntro.title}
            </h2>
          </div>

          <p className="max-w-[440px] text-sm leading-7 text-[#74685f]">
            {auraTeamIntro.description}
          </p>
        </div>

        {/* =====================================================
            PROFESSIONALS GRID
        ====================================================== */}
        <div className="mt-14 grid gap-6 md:grid-cols-3">
          {auraProfessionals.map((professional) => {
            const imageSrc = professionalImages[professional.id];

            return (
              <article
                key={professional.id}
                className="group overflow-hidden rounded-[1.7rem] border border-[#352e29]/10 bg-[#eee7df] shadow-[0_20px_60px_rgba(56,46,40,0.06)] transition-all duration-300 hover:-translate-y-1 hover:shadow-[0_28px_80px_rgba(56,46,40,0.11)]"
              >
                {/* =================================================
                    VISUAL
                ================================================== */}
                <div className="relative aspect-[4/5] overflow-hidden bg-[#d8c6b7]">
                  {imageSrc ? (
                    <Image
                      src={imageSrc}
                      alt={`${professional.name}, ${professional.role} en Aura Studio`}
                      fill
                      quality={90}
                      sizes="(min-width: 768px) 33vw, 100vw"
                      className="object-cover object-center transition-transform duration-700 group-hover:scale-[1.025]"
                    />
                  ) : (
                    <div className="flex h-full w-full items-center justify-center bg-[linear-gradient(145deg,#d8c6b7,#9e8370)]">
                      <span className="font-serif text-6xl text-white/80">
                        {professional.initials}
                      </span>
                    </div>
                  )}

                  {/* Soft treatment */}
                  <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-[#2d211b]/38 via-transparent to-white/[0.03]" />

                  <div className="pointer-events-none absolute inset-0 ring-1 ring-inset ring-white/10" />

                  {/* Role badge */}
                  <div className="absolute bottom-5 left-5 z-10 rounded-full border border-white/25 bg-[#382b24]/25 px-3.5 py-1.5 backdrop-blur-md">
                    <span className="text-[9px] font-semibold uppercase tracking-[0.18em] !text-white">
                      {professional.role}
                    </span>
                  </div>
                </div>

                {/* =================================================
                    CONTENT
                ================================================== */}
                <div className="p-6">
                  <p className="text-[10px] font-semibold uppercase tracking-[0.19em] text-[#9a806c]">
                    {professional.role}
                  </p>

                  <h3 className="mt-2 font-serif text-2xl text-[#302925]">
                    {professional.name}
                  </h3>

                  <p className="mt-4 text-sm leading-6 text-[#76695f]">
                    {professional.description}
                  </p>

                  {/* =================================================
                      SPECIALTIES
                  ================================================== */}
                  <div className="mt-5 flex flex-wrap gap-2">
                    {professional.specialties.map((specialty) => (
                      <span
                        key={specialty}
                        className="rounded-full border border-[#8b6e59]/15 bg-[#8b6e59]/[0.05] px-3 py-1.5 text-[9px] font-semibold uppercase tracking-[0.12em] text-[#856c59]"
                      >
                        {specialty}
                      </span>
                    ))}
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      </div>
    </section>
  );
}