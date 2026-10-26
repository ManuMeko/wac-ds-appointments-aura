"use client";

import Link from "next/link";

import { BookingCanvas } from "@/components/booking/BookingCanvas";
import type { BookingHeroSequence } from "@/data/types";

type BookingHeroProps = {
  eyebrow: string;
  titleLead: string;
  titleAccent: string;
  description: string;
  backPrefix: string;
  brandName: string;
  sequence?: BookingHeroSequence;
  stepFrames?: readonly number[];
  mode: "loop" | "hold";
  target: number;
};

export function BookingHero({
  eyebrow,
  titleLead,
  titleAccent,
  description,
  backPrefix,
  brandName,
  sequence,
  stepFrames,
  mode,
  target,
}: BookingHeroProps) {
  if (!sequence) {
    return (
      <section className="border-b border-(--p-3a312c)/10 bg-(--hero-bg)">
        <div className="mx-auto w-full max-w-[1180px] px-5 py-14 sm:px-8 sm:py-16 lg:px-12">
          <Link
            href="/"
            className="inline-flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.18em] text-(--p-856f5f) transition hover:text-(--p-392f29)"
          >
            <span aria-hidden="true">
              ←
            </span>

            {backPrefix} {brandName}
          </Link>

          <div className="mt-8 max-w-[760px]">
            <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-(--accent)">
              {eyebrow}
            </p>

            <h1 className="mt-4 font-serif text-[clamp(2.8rem,6vw,5.6rem)] leading-[0.95] tracking-[-0.04em] text-(--p-2b2521)">
              {titleLead}

              <span className="block italic text-(--accent)">
                {titleAccent}
              </span>
            </h1>

            <p className="mt-6 max-w-[620px] text-base leading-7 text-(--p-6b5f56) sm:text-lg">
              {description}
            </p>
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className="relative h-[45vh] min-h-[520px] overflow-hidden bg-[#1c1917] text-white">
      <BookingCanvas
        sequence={sequence}
        stepFrames={stepFrames}
        mode={mode}
        target={target}
        preload
      />
      <div className="pointer-events-none absolute inset-0 bg-gradient-to-r from-[#1c1917] via-[#1c1917]/80 to-transparent" />
      <div className="pointer-events-none absolute inset-0 bg-[linear-gradient(to_top_right,rgba(20,18,16,0.85),transparent_62%)]" />
      <div className="relative z-[1] mx-auto flex h-full w-full max-w-[1180px] flex-col justify-end px-5 pt-24 pb-32 sm:px-8 lg:px-12">
        <Link
          href="/"
          className="inline-flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.18em] text-white/75 transition hover:text-white"
        >
          <span aria-hidden="true">←</span>
          {backPrefix} {brandName}
        </Link>
        <div className="mt-7 max-w-[520px]">
          <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-[#f97316]">
            {eyebrow}
          </p>
          <h1 className="pt-3 font-serif text-[clamp(2.5rem,5vw,4.4rem)] leading-[0.95] text-white">
            {titleLead}{" "}
            <span className="text-[#f97316]">{titleAccent}</span>
          </h1>
          <p className="pt-4 max-w-[520px] text-[1.125rem] leading-[1.6] text-white/80">
            {description}
          </p>
        </div>
      </div>
    </section>
  );
}
