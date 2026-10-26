"use client";

import { useEffect, useRef, useState } from "react";

import { Noise } from "@/components/motion/Noise";
import { business } from "@/config/business";
import type { Stat } from "@/data/types";

const STAGGER_MS = 80;

function readFigure(value: string) {
  const match = value.trim().match(/^(\D*?)(\d[\d.\s]*)(.*)$/);
  if (!match) {
    return { prefix: "", target: 0, grouped: false, suffix: "" };
  }

  const digits = match[2].replace(/[.\s]/g, "");
  return {
    prefix: match[1],
    target: Number(digits) || 0,
    grouped: /[.\s]/.test(match[2]),
    suffix: match[3].trim(),
  };
}

function formatFigure(current: number, grouped: boolean) {
  const rounded = Math.round(current);
  const digits = String(rounded);
  if (!grouped) return digits;
  return digits.replace(/\B(?=(\d{3})+(?!\d))/g, ".");
}

function Figure({ stat, index }: { stat: Stat; index: number }) {
  const figure = readFigure(stat.value);
  const [current, setCurrent] = useState(figure.target);
  const nodeRef = useRef<HTMLDivElement>(null);
  const played = useRef(false);

  useEffect(() => {
    const node = nodeRef.current;
    if (!node || played.current) return;

    const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (reduced) return;

    let frame = 0;
    const observer = new IntersectionObserver(
      (entries) => {
        if (!entries.some((entry) => entry.isIntersecting) || played.current) return;
        played.current = true;
        observer.disconnect();

        const start = performance.now();
        const duration = 1200;
        const delay = index * STAGGER_MS;

        const tick = (now: number) => {
          const elapsed = now - start - delay;
          if (elapsed < 0) {
            setCurrent(0);
            frame = requestAnimationFrame(tick);
            return;
          }
          const progress = Math.min(1, elapsed / duration);
          const eased = 1 - (1 - progress) ** 3;
          setCurrent(figure.target * eased);
          if (progress < 1) frame = requestAnimationFrame(tick);
        };

        setCurrent(0);
        frame = requestAnimationFrame(tick);
      },
      { threshold: 0.4 },
    );

    observer.observe(node);
    return () => {
      cancelAnimationFrame(frame);
      observer.disconnect();
    };
  }, [figure.target, index]);

  const unit = stat.unit || figure.suffix;
  const label = `${figure.prefix}${formatFigure(figure.target, figure.grouped)}${unit ? ` ${unit}` : ""} ${stat.label}`;

  return (
    <div ref={nodeRef} className="min-w-0" aria-label={label}>
      <p aria-hidden="true" className="font-serif text-5xl leading-none tracking-[-0.04em] text-white sm:text-6xl">
        {figure.prefix}
        {formatFigure(current, figure.grouped)}
        {unit ? (
          <span className="ml-2 align-baseline text-[0.42em] font-sans font-medium tracking-normal text-[#f97316]">
            {unit}
          </span>
        ) : null}
      </p>
      <p className="mt-3 max-w-[16ch] text-sm leading-5 text-white/70">{stat.label}</p>
    </div>
  );
}

export function StatBand() {
  const stats = business.proofStats;
  if (!stats?.length) return null;

  return (
    <section id="cifras" className="relative overflow-hidden bg-[#1c1917] text-white">
      <Noise id="noise-cifras" />
      <div className="relative mx-auto grid w-full max-w-[1440px] gap-10 px-5 py-16 sm:grid-cols-2 sm:px-8 lg:grid-cols-4 lg:px-12 lg:py-20 xl:px-16">
        {stats.map((stat, index) => (
          <Figure key={`${stat.value}-${stat.label}`} stat={stat} index={index} />
        ))}
      </div>
    </section>
  );
}
