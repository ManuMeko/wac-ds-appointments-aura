"use client";

import { useCallback, useEffect, useRef, useState } from "react";

import {
  ScrollSequence,
  type ScrollSequenceHandle,
} from "@/components/home/ScrollSequence";
import { usePrefersReducedMotion } from "@/components/sequence/useFrameSequence";
import { business } from "@/config/business";
import type { ScrollSequenceConfig, StudioPin as StudioPinCopy } from "@/data/types";

const KEY_FADE = 0.06;
const KEY_RANGES: ReadonlyArray<readonly [number, number]> = [
  [0, 0.25],
  [0.25, 0.5],
  [0.5, 0.75],
  [0.75, 1],
];

function firstFrameSrc(sequence: ScrollSequenceConfig) {
  const base = sequence.basePath.replace(/\/$/, "");
  return `${base}/frame_0001.${sequence.fileExtension}`;
}

function trackHeightVh(sequence: ScrollSequenceConfig) {
  const hold = Number.isFinite(sequence.holdVh) ? (sequence.holdVh ?? 0) : 0;
  const height = Number.isFinite(sequence.heightVh) ? sequence.heightVh : 100;
  return height + Math.max(0, hold);
}

/** Misma lectura que ScrollSequence: el hold deja el último fotograma quieto. */
function readTrackProgress(section: HTMLElement, holdVh: number) {
  const viewport = window.innerHeight;
  const holdPx = (viewport * Math.max(0, holdVh)) / 100;
  const scrollable = section.offsetHeight - viewport - holdPx;
  if (scrollable <= 0) return 0;

  const scrolled = -section.getBoundingClientRect().top;
  if (scrolled <= 0) return 0;
  if (scrolled >= scrollable) return 1;
  return scrolled / scrollable;
}

function keyOpacity(progress: number, index: number, count: number) {
  const slot = KEY_RANGES[index];
  const start = slot?.[0] ?? index / Math.max(1, count);
  const end = slot?.[1] ?? (index + 1) / Math.max(1, count);
  const isFirst = index === 0;
  const isLast = index === count - 1;
  const fadeInFrom = isFirst ? start : start - KEY_FADE;
  const fadeOutTo = isLast ? end : end + KEY_FADE;

  if (progress < fadeInFrom || progress > fadeOutTo) return 0;
  if (isFirst && progress <= start) return 1;
  if (isLast && progress >= end) return 1;
  if (!isFirst && progress < start + KEY_FADE) {
    return Math.min(1, Math.max(0, (progress - fadeInFrom) / (KEY_FADE * 2)));
  }
  if (!isLast && progress > end - KEY_FADE) {
    return Math.min(1, Math.max(0, (fadeOutTo - progress) / (KEY_FADE * 2)));
  }
  return 1;
}

function pinStage(section: HTMLElement, stage: HTMLDivElement) {
  const rect = section.getBoundingClientRect();
  const viewport = window.innerHeight;
  const inView = rect.top <= 0 && rect.bottom >= viewport;

  stage.style.zIndex = "0";
  stage.style.height = `${viewport}px`;

  if (inView) {
    stage.style.position = "fixed";
    stage.style.top = "0px";
    stage.style.bottom = "auto";
    stage.style.left = `${rect.left}px`;
    stage.style.width = `${rect.width}px`;
    return;
  }

  stage.style.position = "absolute";
  stage.style.left = "0px";
  stage.style.width = "100%";

  if (rect.top > 0) {
    stage.style.top = "0px";
    stage.style.bottom = "auto";
    return;
  }

  stage.style.top = "auto";
  stage.style.bottom = "0px";
}

function PinTitle({ pin }: { pin: StudioPinCopy }) {
  return (
    <div>
      <span className="aura-rule" aria-hidden="true" />
      <p className="mt-6 text-[10px] font-medium uppercase tracking-[0.32em] text-(--accent)">
        {pin.eyebrow}
      </p>
      <h2 className="aura-display mt-5 max-w-[10ch] font-serif text-(--ink)">
        {pin.title}
        <span className="mt-[0.08em] block italic text-(--accent)">{pin.italic}</span>
      </h2>
    </div>
  );
}

function StudioStatic({
  pin,
  poster,
  onPosterError,
}: {
  pin: StudioPinCopy;
  poster?: string;
  onPosterError: () => void;
}) {
  const sectionRef = useRef<HTMLElement>(null);
  const [showPoster, setShowPoster] = useState(false);
  const keys = pin.keys;

  useEffect(() => {
    if (!poster) return;
    const node = sectionRef.current;
    if (!node) return;

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry?.isIntersecting) setShowPoster(true);
      },
      { rootMargin: "100% 0px" },
    );
    observer.observe(node);
    return () => observer.disconnect();
  }, [poster]);

  return (
    <section
      id="estudio"
      ref={sectionRef}
      className="relative overflow-hidden bg-(--canvas)"
    >
      {showPoster && poster ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={poster}
          alt=""
          decoding="async"
          fetchPriority="low"
          onError={onPosterError}
          className="aura-studio-still"
        />
      ) : null}
      {showPoster && poster ? (
        <div aria-hidden="true" className="aura-studio-veil pointer-events-none absolute inset-0 z-[1]" />
      ) : null}
      <div className="relative z-[2] mx-auto w-full max-w-[1440px] px-5 py-20 sm:px-8 lg:px-12 lg:py-28 xl:px-16">
        <p className="text-[10px] font-medium uppercase tracking-[0.32em] text-(--accent)">
          {pin.eyebrow}
        </p>
        <h2 className="aura-display mt-5 max-w-[12ch] font-serif text-(--ink)">
          {pin.title} <span className="italic text-(--accent)">{pin.italic}</span>
        </h2>
        <div className="mt-14 grid gap-10">
          {keys.map((key, index) => (
            <article key={key.id} className="max-w-[36rem] border-t border-[#c6a37a]/70 pt-6">
              <p className="text-[10px] font-medium uppercase tracking-[0.28em] text-(--accent)">
                {String(index + 1).padStart(2, "0")}
              </p>
              <h3 className="mt-3 font-serif text-[clamp(1.8rem,3vw,2.6rem)] leading-none tracking-[-0.03em] text-(--ink)">
                {key.title}
              </h3>
              <p className="mt-4 text-base font-light leading-7 text-(--ink-muted)">{key.text}</p>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}

function StudioFilm({
  pin,
  sequence,
  onFail,
}: {
  pin: StudioPinCopy;
  sequence: ScrollSequenceConfig;
  onFail: () => void;
}) {
  const keys = pin.keys;
  const sectionRef = useRef<HTMLElement>(null);
  const stageRef = useRef<HTMLDivElement>(null);
  const filmRef = useRef<ScrollSequenceHandle | null>(null);
  const [loadFrames, setLoadFrames] = useState(false);
  const holdVh = Number.isFinite(sequence.holdVh) ? (sequence.holdVh ?? 0) : 0;

  useEffect(() => {
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;

    const section = sectionRef.current;
    if (!section) return;

    let cancelled = false;
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (!entry?.isIntersecting || cancelled) return;
        observer.disconnect();

        const image = new Image();
        image.decoding = "async";
        image.fetchPriority = "low";
        image.onload = () => {
          if (!cancelled) setLoadFrames(true);
        };
        image.onerror = () => {
          if (!cancelled) onFail();
        };
        image.src = firstFrameSrc(sequence);
      },
      { rootMargin: "100% 0px" },
    );
    observer.observe(section);

    return () => {
      cancelled = true;
      observer.disconnect();
    };
  }, [onFail, sequence]);

  useEffect(() => {
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;

    const section = sectionRef.current;
    const stage = stageRef.current;
    if (!section || !stage) return;

    let rafId = 0;
    let scheduled = false;

    const paint = () => {
      const currentSection = sectionRef.current;
      const currentStage = stageRef.current;
      if (!currentSection || !currentStage) return;

      pinStage(currentSection, currentStage);
      const progress = readTrackProgress(currentSection, holdVh);
      filmRef.current?.setProgress(progress);

      const nodes = currentStage.querySelectorAll<HTMLElement>("[data-pin-key]");
      const count = nodes.length;
      nodes.forEach((node, index) => {
        const shown = keyOpacity(progress, index, count);
        node.style.opacity = shown.toFixed(3);
        node.style.pointerEvents = shown > 0.4 ? "auto" : "none";
      });
    };

    const onScroll = () => {
      if (scheduled) return;
      scheduled = true;
      rafId = requestAnimationFrame(() => {
        scheduled = false;
        paint();
      });
    };

    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    document.addEventListener("scroll", onScroll, { passive: true, capture: true });
    window.addEventListener("resize", onScroll, { passive: true });

    return () => {
      cancelAnimationFrame(rafId);
      window.removeEventListener("scroll", onScroll);
      document.removeEventListener("scroll", onScroll, true);
      window.removeEventListener("resize", onScroll);
    };
  }, [holdVh]);

  return (
    <section
      id="estudio"
      ref={sectionRef}
      className="relative bg-(--canvas)"
      style={{ height: `${trackHeightVh(sequence)}vh` }}
    >
      <div
        ref={stageRef}
        className="absolute top-0 left-0 h-screen w-full overflow-hidden bg-(--canvas)"
      >
        <ScrollSequence
          sequence={sequence}
          skin="aura"
          backdrop
          load={loadFrames}
          filmRef={filmRef}
        />
        <div
          aria-hidden="true"
          className="aura-studio-veil pointer-events-none absolute inset-0 z-[1]"
        />
        <div className="relative z-[2] mx-auto grid h-full w-full max-w-[1440px] items-end gap-8 px-5 pt-28 pb-16 sm:px-8 lg:grid-cols-[0.9fr_1.1fr] lg:items-center lg:gap-20 lg:px-12 lg:pb-20 xl:px-16">
          <PinTitle pin={pin} />
          <div className="relative min-h-[220px]">
            {keys.map((key, index) => (
              <article
                key={key.id}
                data-pin-key=""
                className="absolute inset-0 flex flex-col justify-center"
                style={{ opacity: index === 0 ? 1 : 0 }}
              >
                <p className="text-[10px] font-medium uppercase tracking-[0.32em] text-(--accent)">
                  {String(index + 1).padStart(2, "0")} · {keys.length.toString().padStart(2, "0")}
                </p>
                <h3 className="mt-4 max-w-[14ch] font-serif text-[clamp(2.2rem,4.6vw,4.2rem)] leading-[0.92] tracking-[-0.035em] text-(--ink)">
                  {key.title}
                </h3>
                <span className="aura-rule mt-6" aria-hidden="true" />
                <p className="mt-6 max-w-[34rem] text-base font-light leading-8 text-(--ink-muted) sm:text-lg">
                  {key.text}
                </p>
              </article>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}

export function StudioPin({ framesReady = false }: { framesReady?: boolean }) {
  const pin = business.studioPin;
  const reduced = usePrefersReducedMotion();
  const [failed, setFailed] = useState(false);
  const fail = useCallback(() => setFailed(true), []);

  if (!pin) return null;

  const sequence = pin.sequence;
  const poster =
    sequence && framesReady && !failed ? firstFrameSrc(sequence) : undefined;
  const canPlay = Boolean(sequence && framesReady && !reduced && !failed);

  if (!canPlay || !sequence) {
    return <StudioStatic pin={pin} poster={poster} onPosterError={fail} />;
  }

  return <StudioFilm pin={pin} sequence={sequence} onFail={fail} />;
}
