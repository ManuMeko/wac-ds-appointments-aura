"use client";

import { useEffect, useRef, type RefObject } from "react";

import {
  type FrameStatus,
  loadFrameSequence,
  paintCanvasFrame,
} from "@/components/sequence/frames";
import { usePrefersReducedMotion } from "@/components/sequence/useFrameSequence";
import type {
  ScrollSequenceConfig,
  ScrollSequenceOverlay,
  Stat,
} from "@/data/types";
import { ArrowIcon } from "@/components/ui/ArrowIcon";

/* =========================================================
   WAC DS · MODEL 01 · APPOINTMENTS
   Secuencia de fotogramas que avanza con el scroll
========================================================= */

const EMPTY_OVERLAYS: NonNullable<ScrollSequenceConfig["overlays"]> = [];
const LEAD_UNTIL = 0.35;

export type ScrollSequenceLead = {
  eyebrow: string;
  title: string;
  highlightedTitle: string;
  description: string;
  primaryCta: { label: string; href: string };
  secondaryCta: { label: string; href: string };
  stats: readonly Stat[];
};

function overlayOpacity(progress: number, from: number, to: number) {
  if (progress < from || progress > to) return 0;

  const span = to - from;
  if (span <= 0) return progress === from ? 1 : 0;

  const fade = Math.min(0.08, span * 0.45);
  let opacity = 1;

  if (from > 0 && fade > 0 && progress < from + fade) {
    opacity = (progress - from) / fade;
  }

  if (to < 1 && fade > 0 && progress > to - fade) {
    opacity = Math.min(opacity, (to - progress) / fade);
  }

  return Math.max(0, Math.min(1, opacity));
}

function readProgress(section: HTMLElement, holdVh: number) {
  const viewport = window.innerHeight;
  const holdPx = (viewport * Math.max(0, holdVh)) / 100;
  const scrollable = section.offsetHeight - viewport - holdPx;
  if (scrollable <= 0) return 0;

  const scrolled = -section.getBoundingClientRect().top;
  if (scrolled <= 0) return 0;
  if (scrolled >= scrollable) return 1;
  return scrolled / scrollable;
}

/**
 * El contenedor de la home usa overflow-x: hidden, y eso convierte
 * el eje vertical en scrollport. `position: sticky` deja de fijarse
 * al viewport (le pasa igual al header). Fijamos el escenario con
 * `fixed` mientras la sección ocupa la pantalla.
 */
function pinStage(
  section: HTMLElement,
  stage: HTMLDivElement,
  reducedMotion: boolean,
) {
  if (reducedMotion) {
    stage.style.position = "relative";
    stage.style.top = "0px";
    stage.style.bottom = "auto";
    stage.style.left = "0px";
    stage.style.width = "100%";
    stage.style.height = "";
    stage.style.zIndex = "0";
    return;
  }

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

function neutralFill(
  section: HTMLElement,
  preferDark: boolean,
  tokenOverride?: string,
) {
  const token = tokenOverride ?? (preferDark ? "--inverse" : "--hero-bg");
  const value = getComputedStyle(section).getPropertyValue(token).trim();
  return value || (preferDark ? "#1c1917" : "#d6d3d1");
}

type ScrollSkin = "aura" | "taller";

export type ScrollSequenceHandle = {
  setProgress: (progress: number) => void;
};

type ScrollSequenceProps = {
  sequence: ScrollSequenceConfig;
  lead?: ScrollSequenceLead;
  /** Aura usa cristal crema. Sin este prop, el bloque sigue el de taller. */
  skin?: ScrollSkin;
  /**
   * Solo el lienzo, sin sección, pin ni textos.
   * El padre lleva el scroll y llama a setProgress.
   * Con load en false no se pide ningún fotograma.
   */
  backdrop?: boolean;
  load?: boolean;
  filmRef?: RefObject<ScrollSequenceHandle | null>;
};

function OverlayCopy({
  overlay,
  onDark,
  skin = "taller",
}: {
  overlay: ScrollSequenceOverlay;
  onDark: boolean;
  skin?: ScrollSkin;
}) {
  const Heading = overlay.level === "h1" ? "h1" : "h2";
  const end = overlay.align === "end";
  const aura = skin === "aura";
  const titleClass = aura
    ? "max-w-[14ch] font-serif text-[clamp(2.05rem,5.4vh,4.35rem)] leading-[0.9] tracking-[-0.035em] text-(--ink)"
    : onDark
    ? "max-w-[18ch] font-serif text-[clamp(1.7rem,4.8vh,3.35rem)] leading-[0.95] tracking-[-0.04em] text-white"
    : "max-w-[18ch] font-serif text-4xl leading-[1.02] tracking-[-0.03em] text-(--ink) sm:text-5xl";
  const bodyClass = aura
    ? "mt-4 max-w-[28rem] text-[0.98rem] font-light leading-7 text-(--ink)"
    : onDark
    ? "mt-4 max-w-[28rem] text-sm leading-6 text-white/88 sm:text-base sm:leading-7"
    : "mt-4 max-w-[34rem] text-base leading-7 text-(--ink-muted) sm:text-lg";
  const secondaryClass = onDark
    ? "scroll-ghost-cta backdrop-blur-[8px]"
    : "inline-flex h-12 items-center justify-center rounded-full border border-(--inverse)/20 px-7 text-[11px] font-semibold uppercase text-(--ink) transition-colors duration-200 hover:bg-(--inverse)/5";

  return (
    <div className={end && onDark ? "flex flex-col items-end text-right" : ""}>
      <div
        className={
          onDark ? "mb-5 h-px w-12 bg-(--accent-line)" : "mb-5 h-px w-12 bg-(--accent)"
        }
      />
      <Heading className={titleClass}>
        {overlay.title}
        {overlay.italic ? (
          <span className="mt-[0.06em] block font-medium italic tracking-[-0.03em]">
            {overlay.italic}
          </span>
        ) : null}
      </Heading>
      {overlay.text ? <p className={bodyClass}>{overlay.text}</p> : null}
      {overlay.primaryCta || overlay.secondaryCta ? (
        <div
          className={`mt-6 flex flex-col gap-3 sm:flex-row ${end && onDark ? "sm:justify-end" : ""}`}
        >
          {overlay.primaryCta ? (
            <a
              href={overlay.primaryCta.href}
              data-magnetic={aura ? "" : undefined}
              className={`group inline-flex h-12 items-center justify-center gap-3 rounded-full bg-(--button-bg) px-7 text-[11px] font-semibold uppercase !text-(--button-text) transition-transform duration-200 hover:-translate-y-0.5${aura ? " aura-scroll-pill" : ""}`}
            >
              {overlay.primaryCta.label}
              <span className="transition-transform duration-300 group-hover:translate-x-0.5">
                <ArrowIcon />
              </span>
            </a>
          ) : null}
          {overlay.secondaryCta ? (
            <a
              href={overlay.secondaryCta.href}
              className={secondaryClass}
            >
              {overlay.secondaryCta.label}
            </a>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}

function LeadCopy({
  lead,
  onDark,
}: {
  lead: ScrollSequenceLead;
  onDark: boolean;
}) {
  const titleClass = onDark
    ? "max-w-[16ch] font-serif text-[clamp(1.85rem,6.2vh,4.5rem)] leading-[0.92] tracking-[-0.04em] text-white"
    : "max-w-[12ch] font-serif text-4xl leading-[0.95] tracking-[-0.03em] text-(--ink) sm:text-5xl";
  const accentClass = onDark ? "text-(--accent-line)" : "text-(--accent)";
  const bodyClass = onDark
    ? "mt-3 max-w-[36rem] text-sm leading-6 text-white/88 sm:text-base sm:leading-7"
    : "mt-4 max-w-[560px] text-base leading-7 text-(--ink-muted) sm:text-lg";
  const eyebrowClass = onDark
    ? "text-[11px] font-semibold uppercase text-white/75"
    : "text-[11px] font-semibold uppercase text-(--ink-muted)";
  const ruleClass = onDark ? "mb-5 h-px w-12 bg-(--accent-line)" : "mb-5 h-px w-12 bg-(--accent)";
  const secondaryClass = onDark
    ? "inline-flex h-12 items-center justify-center rounded-full border border-white/45 px-7 text-[11px] font-semibold uppercase text-white transition-colors duration-200 hover:bg-white/10"
    : "inline-flex h-12 items-center justify-center rounded-full border border-(--inverse)/20 px-7 text-[11px] font-semibold uppercase text-(--ink) transition-colors duration-200 hover:bg-(--inverse)/5";
  const statValueClass = onDark
    ? "block font-serif text-xl font-normal text-white"
    : "block font-serif text-2xl font-normal text-(--ink)";
  const statLabelClass = onDark
    ? "mt-1 block text-[10px] uppercase text-white/70"
    : "mt-1 block text-[10px] uppercase text-(--ink-muted)";
  const statRule = onDark ? "border-white/25" : "border-(--inverse)/15";

  return (
    <div className="mx-auto w-full max-w-[1440px]">
      <div className={ruleClass} />
      <p className={eyebrowClass}>{lead.eyebrow}</p>
      <h1 className={`mt-3 ${titleClass}`}>
        {lead.title}{" "}
        <span className={`block ${accentClass}`}>{lead.highlightedTitle}</span>
      </h1>
      <p className={bodyClass}>{lead.description}</p>
      <div className="mt-5 flex flex-col gap-3 sm:flex-row">
        <a
          href={lead.primaryCta.href}
          className="group inline-flex h-12 items-center justify-center gap-3 rounded-full bg-(--button-bg) px-7 text-[11px] font-semibold uppercase !text-(--button-text) transition-transform duration-200 hover:-translate-y-0.5"
        >
          {lead.primaryCta.label}
          <span className="transition-transform duration-300 group-hover:translate-x-0.5">
            <ArrowIcon />
          </span>
        </a>
        <a href={lead.secondaryCta.href} className={secondaryClass}>
          {lead.secondaryCta.label}
        </a>
      </div>
      <div
        className={`mt-5 grid max-w-[640px] grid-cols-2 gap-x-6 gap-y-3 border-t pt-3 sm:grid-cols-4 ${statRule}`}
      >
        {lead.stats.map((stat) => (
          <div key={`${stat.value}-${stat.label}`}>
            <strong className={statValueClass}>{stat.value}</strong>
            <span className={statLabelClass}>{stat.label}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

export function ScrollSequence({
  sequence,
  lead,
  skin = "taller",
  backdrop = false,
  load = true,
  filmRef,
}: ScrollSequenceProps) {
  const reducedMotion = usePrefersReducedMotion();
  const sectionRef = useRef<HTMLElement>(null);
  const stageRef = useRef<HTMLDivElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const imagesRef = useRef<Array<HTMLImageElement | null>>([]);
  const statusRef = useRef<FrameStatus[]>([]);
  const progressRef = useRef(0);
  const paintRef = useRef<() => void>(() => {});

  const overlays = sequence.overlays ?? EMPTY_OVERLAYS;
  const holdVh = Number.isFinite(sequence.holdVh) ? (sequence.holdVh ?? 0) : 0;
  const trackHeight =
    (Number.isFinite(sequence.heightVh) ? sequence.heightVh : 100) +
    Math.max(0, holdVh);
  const onVideo = Boolean(lead) || overlays.length > 0;

  useEffect(() => {
    if (backdrop) {
      const paint = () => {
        const stage = stageRef.current;
        const canvas = canvasRef.current;
        if (!stage || !canvas) return;

        paintCanvasFrame(
          canvas,
          imagesRef.current,
          statusRef.current,
          reducedMotion ? 0 : progressRef.current,
          sequence.frameCount,
          neutralFill(stage, false, "--canvas"),
        );
      };

      paintRef.current = paint;
      paint();

      const stage = stageRef.current;
      const observer = new ResizeObserver(() => {
        paint();
      });
      if (stage) observer.observe(stage);

      let pixelRatioQuery = window.matchMedia(
        `(resolution: ${window.devicePixelRatio || 1}dppx)`,
      );
      const onPixelRatio = () => {
        pixelRatioQuery.removeEventListener("change", onPixelRatio);
        paint();
        pixelRatioQuery = window.matchMedia(
          `(resolution: ${window.devicePixelRatio || 1}dppx)`,
        );
        pixelRatioQuery.addEventListener("change", onPixelRatio);
      };
      pixelRatioQuery.addEventListener("change", onPixelRatio);

      return () => {
        paintRef.current = () => {};
        observer.disconnect();
        pixelRatioQuery.removeEventListener("change", onPixelRatio);
      };
    }

    const paint = () => {
      const section = sectionRef.current;
      const stage = stageRef.current;
      const canvas = canvasRef.current;
      if (!section || !stage || !canvas) return;

      pinStage(section, stage, reducedMotion);

      paintCanvasFrame(
        canvas,
        imagesRef.current,
        statusRef.current,
        reducedMotion ? 0 : progressRef.current,
        sequence.frameCount,
        neutralFill(section, Boolean(lead)),
      );

      if (reducedMotion) return;

      const progress = progressRef.current;
      const leadNode = stage.querySelector<HTMLElement>("[data-scroll-lead]");
      if (leadNode) {
        const leadOpacity = overlayOpacity(progress, 0, LEAD_UNTIL);
        leadNode.style.opacity = leadOpacity.toFixed(3);
        leadNode.style.pointerEvents = leadOpacity > 0.05 ? "auto" : "none";
      }

      const nodes = stage.querySelectorAll<HTMLElement>("[data-scroll-overlay]");
      nodes.forEach((node) => {
        const index = Number(node.dataset.scrollOverlay);
        const overlay = overlays[index];
        if (!overlay) return;
        const opacity = overlayOpacity(progress, overlay.from, overlay.to);
        node.style.opacity = opacity.toFixed(3);
        node.style.pointerEvents = opacity > 0.05 ? "auto" : "none";
      });

      const hint = stage.querySelector<HTMLElement>("[data-scroll-hint]");
      if (hint) {
        hint.style.opacity = progress > 0.012 ? "0" : "1";
      }
    };

    paintRef.current = paint;

    let rafId = 0;
    let scheduled = false;

    const updateFromScroll = () => {
      const section = sectionRef.current;
      progressRef.current =
        reducedMotion || !section ? 0 : readProgress(section, holdVh);
      paint();
    };

    const onScroll = () => {
      if (scheduled) return;
      scheduled = true;
      rafId = requestAnimationFrame(() => {
        scheduled = false;
        updateFromScroll();
      });
    };

    onScroll();

    const stage = stageRef.current;
    const observer = new ResizeObserver(() => {
      onScroll();
    });
    if (stage) observer.observe(stage);

    if (!reducedMotion) {
      window.addEventListener("scroll", onScroll, { passive: true });
      document.addEventListener("scroll", onScroll, {
        passive: true,
        capture: true,
      });
    }
    window.addEventListener("resize", onScroll, { passive: true });

    let pixelRatioQuery = window.matchMedia(
      `(resolution: ${window.devicePixelRatio || 1}dppx)`,
    );
    const onPixelRatio = () => {
      pixelRatioQuery.removeEventListener("change", onPixelRatio);
      onScroll();
      pixelRatioQuery = window.matchMedia(
        `(resolution: ${window.devicePixelRatio || 1}dppx)`,
      );
      pixelRatioQuery.addEventListener("change", onPixelRatio);
    };
    pixelRatioQuery.addEventListener("change", onPixelRatio);

    return () => {
      cancelAnimationFrame(rafId);
      paintRef.current = () => {};
      observer.disconnect();
      window.removeEventListener("scroll", onScroll);
      document.removeEventListener("scroll", onScroll, true);
      window.removeEventListener("resize", onScroll);
      pixelRatioQuery.removeEventListener("change", onPixelRatio);
    };
  }, [backdrop, holdVh, lead, overlays, reducedMotion, sequence.frameCount]);

  useEffect(() => {
    paintRef.current();
  });

  useEffect(() => {
    if (!backdrop || !filmRef) return;

    const handle: ScrollSequenceHandle = {
      setProgress(progress: number) {
        progressRef.current = Math.min(1, Math.max(0, progress));
        paintRef.current();
      },
    };
    filmRef.current = handle;

    return () => {
      if (filmRef.current === handle) filmRef.current = null;
    };
  }, [backdrop, filmRef]);

  useEffect(() => {
    if (backdrop && !load) {
      imagesRef.current = [];
      statusRef.current = [];
      return;
    }

    const count = Math.max(
      0,
      Math.floor(reducedMotion ? 1 : sequence.frameCount),
    );
    const images = Array.from({ length: count }, () => null);
    const statuses = Array.from(
      { length: count },
      () => "pending" as FrameStatus,
    );
    imagesRef.current = images;
    statusRef.current = statuses;

    return loadFrameSequence({
      basePath: sequence.basePath,
      frameCount: count,
      fileExtension: sequence.fileExtension,
      images,
      statuses,
      eagerFirst: !backdrop,
      onUpdate: () => paintRef.current(),
    });
  }, [
    backdrop,
    load,
    reducedMotion,
    sequence.basePath,
    sequence.fileExtension,
    sequence.frameCount,
  ]);

  if (backdrop) {
    return (
      <div
        ref={stageRef}
        aria-hidden="true"
        className="pointer-events-none absolute inset-0 z-0 overflow-hidden bg-(--canvas)"
      >
        <canvas
          ref={canvasRef}
          className="absolute inset-0 block h-full w-full"
        />
      </div>
    );
  }

  const stageClass = onVideo
    ? reducedMotion
      ? "relative h-screen overflow-hidden bg-(--inverse)"
      : "pointer-events-none absolute top-0 left-0 z-0 h-screen w-full overflow-hidden bg-(--inverse)"
    : reducedMotion
      ? "relative h-screen overflow-hidden bg-(--hero-bg)"
      : "pointer-events-none absolute top-0 left-0 z-0 h-screen w-full overflow-hidden bg-(--hero-bg)";

  return (
    <section
      id="inicio"
      ref={sectionRef}
      data-scroll-sequence=""
      className={
        onVideo
          ? reducedMotion
            ? "relative bg-(--canvas) text-(--ink)"
            : "relative bg-(--inverse)"
          : "relative bg-(--hero-bg) text-(--ink)"
      }
      style={reducedMotion ? undefined : { height: `${trackHeight}vh` }}
    >
      <div ref={stageRef} className={stageClass}>
        <canvas
          ref={canvasRef}
          aria-hidden="true"
          className="absolute inset-0 block h-full w-full"
        />

        {onVideo ? (
          <div
            aria-hidden="true"
            className={
              skin === "aura"
                ? "pointer-events-none absolute inset-x-0 top-0 z-[1] h-32 bg-gradient-to-b from-[#29231f]/50 to-transparent"
                : "pointer-events-none absolute inset-x-0 top-0 z-[1] h-28 bg-gradient-to-b from-black/60 to-transparent"
            }
          />
        ) : null}

        {reducedMotion || !lead ? null : (
          <div
            data-scroll-lead=""
            className="pointer-events-auto absolute inset-x-0 top-20 bottom-0 z-[2] flex items-end overflow-hidden"
            style={{ opacity: overlayOpacity(0, 0, LEAD_UNTIL) }}
          >
            <div className="w-full bg-gradient-to-t from-black/88 via-black/55 to-transparent px-5 pt-16 pb-8 sm:px-8 sm:pb-12 lg:px-16 lg:pb-14">
              <LeadCopy lead={lead} onDark />
            </div>
          </div>
        )}

        {reducedMotion
          ? null
          : overlays.map((overlay, index) => {
              const visible =
                overlayOpacity(0, overlay.from, overlay.to) > 0.05;
              const end = overlay.align === "end";

              return (
                <div
                  key={`${overlay.from}-${overlay.to}-${overlay.title}`}
                  data-scroll-overlay={index}
                  className={`absolute inset-x-0 top-20 bottom-0 z-[2] flex items-end overflow-hidden pb-16 ${
                    end ? "justify-end" : "justify-start"
                  } ${visible ? "pointer-events-auto" : "pointer-events-none"}`}
                  style={{
                    opacity: overlayOpacity(0, overlay.from, overlay.to),
                  }}
                >
                  <div
                    className={`m-5 max-w-xl backdrop-blur-[12px] sm:m-8 lg:m-12 ${
                      skin === "aura" ? "aura-scroll-card" : "scroll-overlay-card"
                    }`}
                  >
                    <OverlayCopy
                      overlay={overlay}
                      onDark={skin !== "aura"}
                      skin={skin}
                    />
                  </div>
                </div>
              );
            })}

        {reducedMotion || !onVideo ? null : (
          <div
            data-scroll-hint
            className={skin === "aura" ? "scroll-hint aura-scroll-hint" : "scroll-hint"}
            aria-hidden="true"
          >
            <span>Desliza</span>
            <span className="scroll-hint-line" />
          </div>
        )}
      </div>

      {reducedMotion && (lead || overlays.length > 0) ? (
        <div className="mx-auto grid w-full max-w-[1440px] gap-16 px-5 py-16 sm:px-8 lg:px-12 lg:py-24">
          {lead ? <LeadCopy lead={lead} onDark={false} /> : null}
          {overlays.map((overlay) => (
            <div
              key={`${overlay.from}-${overlay.to}-${overlay.title}`}
              className="max-w-[720px]"
            >
              <OverlayCopy overlay={overlay} onDark={false} skin={skin} />
            </div>
          ))}
        </div>
      ) : null}
    </section>
  );
}
