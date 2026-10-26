"use client";

import { useEffect, useRef } from "react";

import { frameIndexForProgress, frameSrc } from "@/components/sequence/frames";
import {
  useFrameSequence,
  usePrefersReducedMotion,
} from "@/components/sequence/useFrameSequence";
import type { BookingHeroSequence } from "@/data/types";

const SEEK_MS = 600;
const SAMPLE_FRAMES = 36;

type BookingCanvasProps = {
  sequence: BookingHeroSequence;
  stepFrames?: readonly number[];
  mode: "loop" | "hold";
  target: number;
  className?: string;
  preload?: boolean;
  /** Pinta el fotograma al instante. El crossfade lo hace quien lo envuelve. */
  immediate?: boolean;
};

export function BookingCanvas({
  sequence,
  stepFrames = [],
  mode,
  target,
  className = "absolute inset-0 h-full w-full",
  preload = false,
  immediate = false,
}: BookingCanvasProps) {
  const reducedMotion = usePrefersReducedMotion();
  const frameCount = Math.max(1, Math.floor(sequence.frameCount));
  const extension = sequence.fileExtension ?? "webp";
  const stride = reducedMotion
    ? frameCount
    : Math.max(1, Math.round(frameCount / SAMPLE_FRAMES));
  const priorityIndexes = reducedMotion
    ? [0]
    : stepFrames.map((fraction) => frameIndexForProgress(fraction, frameCount));
  const { canvasRef, paint } = useFrameSequence({
    basePath: sequence.basePath,
    frameCount: reducedMotion ? 1 : frameCount,
    fileExtension: extension,
    fill: "#1c1917",
    stride: reducedMotion ? 1 : stride,
    priorityIndexes,
  });
  const progressRef = useRef(0);

  useEffect(() => {
    if (reducedMotion || immediate) {
      progressRef.current = reducedMotion ? 0 : target;
      paint(progressRef.current);
      return;
    }

    const from = progressRef.current;
    const started = performance.now();
    let raf = 0;

    const tick = (now: number) => {
      if (mode === "loop") {
        const span = Math.max(1, sequence.durationMs);
        const cycle = ((now - started) % span) / span;
        const ping = cycle < 0.5 ? cycle * 2 : (1 - cycle) * 2;
        progressRef.current = ping;
        paint(ping);
        raf = requestAnimationFrame(tick);
        return;
      }

      const amount = Math.min(1, (now - started) / SEEK_MS);
      const eased = 1 - (1 - amount) ** 3;
      const next = from + (target - from) * eased;
      progressRef.current = next;
      paint(next);
      if (amount < 1) raf = requestAnimationFrame(tick);
    };

    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [immediate, mode, paint, reducedMotion, sequence.durationMs, target]);

  const firstFrame = frameSrc(sequence.basePath, 1, extension);

  return (
    <>
      {preload ? (
        <link
          rel="preload"
          as="image"
          href={firstFrame}
          fetchPriority="high"
        />
      ) : null}
      <canvas ref={canvasRef} aria-hidden="true" className={className} />
    </>
  );
}
