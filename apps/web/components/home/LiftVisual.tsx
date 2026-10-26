"use client";

import Image from "next/image";
import { useEffect, useRef, useState } from "react";

export function LiftVisual({
  src,
  alt,
  badge,
  title,
  description,
}: {
  src: string;
  alt: string;
  badge: string;
  title: string;
  description: string;
}) {
  const frameRef = useRef<HTMLDivElement>(null);
  const photoRef = useRef<HTMLDivElement>(null);
  const barRef = useRef<HTMLSpanElement>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    const frame = frameRef.current;
    const photo = photoRef.current;
    const bar = barRef.current;
    if (!frame || !photo || !bar) return;

    const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (reduced) {
      frame.classList.add("is-open");
      bar.style.transform = "scaleX(1)";
      return;
    }

    let frameId = 0;
    let scheduled = false;

    const paint = () => {
      scheduled = false;
      const rect = frame.getBoundingClientRect();
      const viewport = window.innerHeight;
      const total = rect.height + viewport;
      const progress = Math.min(1, Math.max(0, (viewport - rect.top) / total));
      const shift = (progress - 0.5) * 40;
      photo.style.transform = `translate3d(0, ${shift.toFixed(2)}px, 0)`;
      bar.style.transform = `scaleX(${progress.toFixed(3)})`;
      if (progress > 0.08) frame.classList.add("is-open");
    };

    const onScroll = () => {
      if (scheduled) return;
      scheduled = true;
      frameId = requestAnimationFrame(paint);
    };

    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    document.addEventListener("scroll", onScroll, { passive: true, capture: true });
    window.addEventListener("resize", onScroll, { passive: true });

    return () => {
      cancelAnimationFrame(frameId);
      window.removeEventListener("scroll", onScroll);
      document.removeEventListener("scroll", onScroll, true);
      window.removeEventListener("resize", onScroll);
    };
  }, []);

  return (
    <div className="relative">
      <div
        ref={frameRef}
        className="lift-mask relative aspect-[4/5] overflow-hidden rounded-[2rem] border border-white/10 bg-[#1c1917] shadow-[0_30px_80px_rgba(12,10,9,0.28)] sm:aspect-[4/3] lg:aspect-[4/5] xl:aspect-[5/6]"
      >
        <div ref={photoRef} className="absolute inset-x-0 -top-[8%] h-[116%] will-change-transform">
          {failed ? (
            <div className="h-full w-full bg-[linear-gradient(165deg,#44403c_0%,#1c1917_100%)]" />
          ) : (
            <Image
              src={src}
              alt={alt}
              fill
              quality={85}
              sizes="(min-width: 1280px) 560px, (min-width: 1024px) 45vw, 100vw"
              className="object-cover object-center"
              onError={() => setFailed(true)}
            />
          )}
        </div>

        <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-[#1c1917]/55 via-transparent to-black/10" />

        <div className="absolute left-5 top-5 z-10 rounded-full border border-white/25 bg-black/25 px-4 py-2 backdrop-blur-md sm:left-6 sm:top-6">
          <span className="text-[9px] font-semibold uppercase tracking-[0.2em] text-white sm:text-[10px]">
            {badge}
          </span>
        </div>

        <div className="absolute bottom-5 left-5 right-5 z-10 rounded-[1.4rem] border border-white/10 bg-[linear-gradient(165deg,#292524_0%,#1c1917_72%)] p-5 shadow-[0_18px_50px_rgba(12,10,9,0.35)] sm:bottom-7 sm:left-7 sm:right-7 sm:p-7">
          <div className="mb-4 h-px w-28 overflow-hidden bg-white/20">
            <span
              ref={barRef}
              className="block h-full w-full origin-left bg-[#f97316] will-change-transform"
              style={{ transform: "scaleX(0)" }}
            />
          </div>
          <p className="max-w-[390px] font-serif text-3xl leading-[1.02] tracking-[-0.025em] text-white sm:text-4xl">
            {title}
          </p>
          <p className="mt-4 max-w-md text-sm leading-6 text-white/75">{description}</p>
        </div>
      </div>
    </div>
  );
}
