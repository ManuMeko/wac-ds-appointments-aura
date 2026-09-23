// apps/web/components/home/PhilosophySection.tsx

import { auraPhilosophy } from "@/data/aura";

/* =========================================================
   WAC DS · MODEL 01 · APPOINTMENTS
   Aura Studio · Philosophy Section
========================================================= */

export function PhilosophySection() {
  return (
    <section
      id="experiencia"
      className="border-y border-[#332b26]/10 bg-[#25211e] text-white"
    >
      <div className="mx-auto grid w-full max-w-[1440px] grid-cols-1 gap-14 px-5 py-20 sm:px-8 lg:grid-cols-[0.8fr_1.2fr] lg:px-12 lg:py-28 xl:px-16">
        {/* =====================================================
            EYEBROW
        ====================================================== */}
        <div>
          <p className="text-[10px] font-semibold uppercase tracking-[0.27em] text-[#c8a992]">
            {auraPhilosophy.eyebrow}
          </p>
        </div>

        {/* =====================================================
            COPY
        ====================================================== */}
        <div>
          <h2 className="max-w-[840px] font-serif text-4xl leading-[1.05] tracking-[-0.03em] text-[#f3ece5] sm:text-5xl lg:text-6xl">
            {auraPhilosophy.title}

            <span className="text-[#bda08a]">
              {" "}
              {auraPhilosophy.highlightedTitle}
            </span>
          </h2>

          <p className="mt-8 max-w-[720px] text-base leading-8 text-[#bfb4ac]">
            {auraPhilosophy.description}
          </p>
        </div>
      </div>
    </section>
  );
}