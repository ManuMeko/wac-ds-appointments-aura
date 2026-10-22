"use client";

import { useEffect, useRef, useSyncExternalStore } from "react";

import type { ScrollSequenceConfig } from "@/data/types";

/* =========================================================
   WAC DS · MODEL 01 · APPOINTMENTS
   Secuencia de fotogramas que avanza con el scroll
========================================================= */

const PRELOAD_CONCURRENCY = 4;
const EMPTY_OVERLAYS: NonNullable<ScrollSequenceConfig["overlays"]> = [];

type FrameStatus = "pending" | "ready" | "error";

function subscribeReducedMotion(onStoreChange: () => void) {
  const media = window.matchMedia("(prefers-reduced-motion: reduce)");
  media.addEventListener("change", onStoreChange);
  return () => media.removeEventListener("change", onStoreChange);
}

function getReducedMotionSnapshot() {
  return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

function getReducedMotionServerSnapshot() {
  return false;
}

function usePrefersReducedMotion() {
  return useSyncExternalStore(
    subscribeReducedMotion,
    getReducedMotionSnapshot,
    getReducedMotionServerSnapshot,
  );
}

function frameSrc(basePath: string, frameNumber: number, extension: string) {
  const base = basePath.endsWith("/") ? basePath.slice(0, -1) : basePath;
  return `${base}/frame_${String(frameNumber).padStart(4, "0")}.${extension}`;
}

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

function readProgress(section: HTMLElement) {
  const scrollable = section.offsetHeight - window.innerHeight;
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

function neutralFill(section: HTMLElement) {
  const value = getComputedStyle(section).getPropertyValue("--hero-bg").trim();
  return value || "#d6d3d1";
}

function drawCover(
  context: CanvasRenderingContext2D,
  image: HTMLImageElement,
  width: number,
  height: number,
) {
  const sourceWidth = image.naturalWidth;
  const sourceHeight = image.naturalHeight;
  if (sourceWidth <= 0 || sourceHeight <= 0) return false;

  const scale = Math.max(width / sourceWidth, height / sourceHeight);
  const drawnWidth = sourceWidth * scale;
  const drawnHeight = sourceHeight * scale;
  const offsetX = (width - drawnWidth) / 2;
  const offsetY = (height - drawnHeight) / 2;

  context.drawImage(image, offsetX, offsetY, drawnWidth, drawnHeight);
  return true;
}

function readyImageAt(
  images: Array<HTMLImageElement | null>,
  statuses: FrameStatus[],
  index: number,
) {
  const image = images[index];
  if (statuses[index] === "ready" && image && image.naturalWidth > 0) {
    return image;
  }
  return null;
}

function imageForFrame(
  images: Array<HTMLImageElement | null>,
  statuses: FrameStatus[],
  frameIndex: number,
) {
  const status = statuses[frameIndex];
  const direct = readyImageAt(images, statuses, frameIndex);
  if (direct) return direct;

  for (let index = frameIndex - 1; index >= 0; index -= 1) {
    const image = readyImageAt(images, statuses, index);
    if (image) return image;
  }

  if (status === "pending") return null;

  for (let index = frameIndex + 1; index < images.length; index += 1) {
    const image = readyImageAt(images, statuses, index);
    if (image) return image;
  }

  return null;
}

type ScrollSequenceProps = {
  sequence: ScrollSequenceConfig;
};

export function ScrollSequence({ sequence }: ScrollSequenceProps) {
  const reducedMotion = usePrefersReducedMotion();
  const sectionRef = useRef<HTMLElement>(null);
  const stageRef = useRef<HTMLDivElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const imagesRef = useRef<Array<HTMLImageElement | null>>([]);
  const statusRef = useRef<FrameStatus[]>([]);
  const progressRef = useRef(0);
  const paintRef = useRef<() => void>(() => {});

  const overlays = sequence.overlays ?? EMPTY_OVERLAYS;
  const trackHeight = Number.isFinite(sequence.heightVh)
    ? sequence.heightVh
    : 100;

  useEffect(() => {
    const paint = () => {
      const section = sectionRef.current;
      const stage = stageRef.current;
      const canvas = canvasRef.current;
      if (!section || !stage || !canvas) return;

      const context = canvas.getContext("2d");
      if (!context) return;

      pinStage(section, stage, reducedMotion);

      const bounds = stage.getBoundingClientRect();
      const cssWidth = bounds.width;
      const cssHeight = bounds.height;
      if (cssWidth < 1 || cssHeight < 1) return;

      const pixelRatio = window.devicePixelRatio || 1;
      const bitmapWidth = Math.max(1, Math.round(cssWidth * pixelRatio));
      const bitmapHeight = Math.max(1, Math.round(cssHeight * pixelRatio));

      if (canvas.width !== bitmapWidth || canvas.height !== bitmapHeight) {
        canvas.width = bitmapWidth;
        canvas.height = bitmapHeight;
      }

      context.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0);
      context.imageSmoothingEnabled = true;
      context.imageSmoothingQuality = "high";

      const frameCount = Math.max(1, Math.floor(sequence.frameCount));
      const progress = reducedMotion ? 0 : progressRef.current;
      const frameIndex = Math.min(
        frameCount - 1,
        Math.max(0, Math.round(progress * (frameCount - 1))),
      );
      const image = imageForFrame(
        imagesRef.current,
        statusRef.current,
        frameIndex,
      );

      context.fillStyle = neutralFill(section);
      context.fillRect(0, 0, cssWidth, cssHeight);

      if (image) {
        drawCover(context, image, cssWidth, cssHeight);
      }

      if (reducedMotion) return;

      const nodes = stage.querySelectorAll<HTMLElement>("[data-scroll-overlay]");
      nodes.forEach((node) => {
        const index = Number(node.dataset.scrollOverlay);
        const overlay = overlays[index];
        if (!overlay) return;
        node.style.opacity = overlayOpacity(
          progress,
          overlay.from,
          overlay.to,
        ).toFixed(3);
      });
    };

    paintRef.current = paint;

    let rafId = 0;
    let scheduled = false;

    const updateFromScroll = () => {
      const section = sectionRef.current;
      progressRef.current =
        reducedMotion || !section ? 0 : readProgress(section);
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
  }, [overlays, reducedMotion, sequence.frameCount]);

  useEffect(() => {
    paintRef.current();
  });

  useEffect(() => {
    let cancelled = false;
    const count = Math.max(
      0,
      Math.floor(reducedMotion ? 1 : sequence.frameCount),
    );

    imagesRef.current = Array.from({ length: count }, () => null);
    statusRef.current = Array.from({ length: count }, () => "pending");

    const loadFrame = (index: number) =>
      new Promise<void>((resolve) => {
        if (cancelled) {
          resolve();
          return;
        }

        const image = new Image();
        image.decoding = "async";
        image.fetchPriority = index === 0 ? "high" : "low";

        const settle = (status: FrameStatus) => {
          if (!cancelled) {
            statusRef.current[index] = status;
            if (status === "ready") imagesRef.current[index] = image;
            paintRef.current();
          }
          resolve();
        };

        image.onload = () => {
          if (typeof image.decode === "function") {
            image.decode().then(
              () => settle("ready"),
              () => settle("ready"),
            );
            return;
          }
          settle("ready");
        };
        image.onerror = () => settle("error");
        image.src = frameSrc(
          sequence.basePath,
          index + 1,
          sequence.fileExtension,
        );
      });

    const preload = async () => {
      if (count < 1) {
        paintRef.current();
        return;
      }

      await loadFrame(0);
      if (cancelled || reducedMotion || count < 2) return;

      let nextIndex = 1;
      const workers = Array.from(
        { length: Math.min(PRELOAD_CONCURRENCY, count - 1) },
        async () => {
          while (!cancelled) {
            const index = nextIndex;
            nextIndex += 1;
            if (index >= count) return;
            await loadFrame(index);
          }
        },
      );

      await Promise.all(workers);
    };

    void preload();

    return () => {
      cancelled = true;
    };
  }, [
    reducedMotion,
    sequence.basePath,
    sequence.fileExtension,
    sequence.frameCount,
  ]);

  return (
    <section
      ref={sectionRef}
      className="relative bg-(--hero-bg) text-(--ink)"
      style={reducedMotion ? undefined : { height: `${trackHeight}vh` }}
    >
      <div
        ref={stageRef}
        className={
          reducedMotion
            ? "relative h-screen overflow-hidden bg-(--hero-bg)"
            : "pointer-events-none absolute top-0 left-0 z-0 h-screen w-full overflow-hidden bg-(--hero-bg)"
        }
      >
        <canvas
          ref={canvasRef}
          aria-hidden="true"
          className="absolute inset-0 block h-full w-full"
        />

        {reducedMotion
          ? null
          : overlays.map((overlay, index) => (
              <div
                key={`${overlay.from}-${overlay.to}-${overlay.title}`}
                data-scroll-overlay={index}
                className="pointer-events-none absolute inset-0 flex items-end"
                style={{
                  opacity: overlayOpacity(0, overlay.from, overlay.to),
                }}
              >
                <div className="w-full bg-gradient-to-t from-(--inverse)/80 via-(--inverse)/35 to-transparent px-5 pt-32 pb-16 sm:px-8 sm:pb-20 lg:px-16 lg:pb-24">
                  <div className="mx-auto w-full max-w-[1440px]">
                    <div className="mb-5 h-px w-12 bg-(--accent)" />
                    <h2 className="max-w-[14ch] font-serif text-[clamp(2.75rem,6vw,5.25rem)] leading-[0.92] tracking-[-0.045em] text-(--on-inverse)">
                      {overlay.title}
                    </h2>
                    {overlay.text ? (
                      <p className="mt-5 max-w-[34rem] text-base leading-7 text-(--on-inverse) opacity-85 sm:text-lg sm:leading-8">
                        {overlay.text}
                      </p>
                    ) : null}
                  </div>
                </div>
              </div>
            ))}
      </div>

      {reducedMotion && overlays.length > 0 ? (
        <div className="mx-auto grid w-full max-w-[1440px] gap-12 px-5 py-16 sm:px-8 lg:px-12 lg:py-24">
          {overlays.map((overlay) => (
            <div
              key={`${overlay.from}-${overlay.to}-${overlay.title}`}
              className="max-w-[720px]"
            >
              <div className="mb-5 h-px w-12 bg-(--accent)" />
              <h2 className="font-serif text-4xl leading-[1.02] tracking-[-0.03em] text-(--ink) sm:text-5xl">
                {overlay.title}
              </h2>
              {overlay.text ? (
                <p className="mt-4 max-w-[560px] text-base leading-7 text-(--ink-muted) sm:text-lg">
                  {overlay.text}
                </p>
              ) : null}
            </div>
          ))}
        </div>
      ) : null}
    </section>
  );
}
