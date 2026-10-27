// apps/web/components/home/PhilosophySection.tsx

import { Noise } from "@/components/motion/Noise";
import { Reveal } from "@/components/motion/Reveal";
import { TradeIcon } from "@/components/ui/TradeIcon";
import { business } from "@/config/business";

/* =========================================================
   WAC DS · MODEL 01 · APPOINTMENTS
   Aura Studio · Philosophy Section
========================================================= */

export function PhilosophySection() {
  if (business.theme === "taller" && business.philosophy.steps) {
    return <TallerPhilosophy />;
  }

  return (
    <section
      id="experiencia"
      data-header-ink="light"
      className="border-y border-(--heading)/10 bg-(--inverse) text-white"
    >
      <Noise id="noise-filosofia" />
      <div className="mx-auto grid w-full max-w-[1440px] grid-cols-1 gap-14 px-5 py-20 sm:px-8 lg:grid-cols-[0.8fr_1.2fr] lg:px-12 lg:py-28 xl:px-16">
        <div>
          <p className="text-[10px] font-semibold uppercase tracking-[0.27em] text-(--p-c8a992)">
            {business.philosophy.eyebrow}
          </p>
        </div>

        <div>
          <Reveal variant="fade-up">
            <h2 className="max-w-[840px] font-serif text-4xl leading-[1.05] tracking-[-0.03em] text-(--p-f3ece5) sm:text-5xl lg:text-6xl">
              {business.philosophy.title}

              <span className="text-(--p-bda08a)">
                {" "}
                {business.philosophy.highlightedTitle}
              </span>
            </h2>
          </Reveal>

          <p className="mt-8 max-w-[720px] text-base leading-8 text-(--p-bfb4ac)">
            {business.philosophy.description}
          </p>
        </div>
      </div>
    </section>
  );
}

function TallerPhilosophy() {
  const steps = business.philosophy.steps ?? [];

  return (
    <section
      id="experiencia"
      data-header-ink="light"
      className="home-philosophy relative overflow-hidden border-y border-(--heading)/10 bg-(--inverse) text-white"
    >
      <Noise id="noise-filosofia" />
      <div className="home-philosophy-glow" aria-hidden="true" />
      <div className="relative mx-auto w-full max-w-[1440px] px-5 py-20 sm:px-8 lg:px-12 lg:py-28 xl:px-16">
        <div className="grid grid-cols-1 items-start gap-8 lg:grid-cols-2 lg:gap-16">
          <div>
            <p className="text-[10px] font-semibold uppercase tracking-[0.27em] text-(--p-c8a992)">
              {business.philosophy.eyebrow}
            </p>
            <Reveal variant="fade-up">
              <h2 className="max-w-[16ch] pt-4 font-serif text-4xl leading-[1.05] tracking-[-0.03em] text-(--p-f3ece5) sm:text-5xl lg:text-6xl">
                {business.philosophy.title}{" "}
                <span className="text-(--p-bda08a)">
                  {business.philosophy.highlightedTitle}
                </span>
              </h2>
            </Reveal>
          </div>
          <p className="max-w-[36rem] text-base leading-8 text-(--p-bfb4ac) lg:pt-10">
            {business.philosophy.description}
          </p>
        </div>

        <div className="relative mt-14 md:mt-20">
          <Reveal
            variant="draw"
            className="pointer-events-none absolute top-[50px] right-[10%] left-[10%] z-0 hidden h-px bg-[#f97316] md:block"
          />
          <div className="relative z-[1] grid gap-4 md:grid-cols-3">
            {steps.map((step, index) => (
              <Reveal key={step.number} delay={index * 120}>
                <article className="home-step">
                  <span className="home-step-num" aria-hidden="true">
                    {step.number}
                  </span>
                  <span className="home-step-icon">
                    <TradeIcon name={step.icon} className="h-5 w-5" />
                  </span>
                  <h3>{step.title}</h3>
                  <p>{step.text}</p>
                </article>
              </Reveal>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
