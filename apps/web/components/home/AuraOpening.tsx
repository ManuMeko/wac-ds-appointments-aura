"use client";

import { useEffect, useState } from "react";

import { HeroSection } from "@/components/home/HeroSection";
import { ScrollSequence } from "@/components/home/ScrollSequence";
import { usePrefersReducedMotion } from "@/components/sequence/useFrameSequence";
import type { ScrollSequenceConfig } from "@/data/types";

function firstFrameSrc(sequence: ScrollSequenceConfig) {
  const base = sequence.basePath.replace(/\/$/, "");
  return `${base}/frame_0001.${sequence.fileExtension}`;
}

/**
 * Aura: la película solo si hay fotogramas y el movimiento está permitido.
 * Reduced motion y un frame_0001 que no carga vuelven al hero estático.
 */
export function AuraOpening({
  ready,
  sequence,
}: {
  ready: boolean;
  sequence?: ScrollSequenceConfig;
}) {
  const reduced = usePrefersReducedMotion();
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    if (!ready || !sequence || reduced) return;

    let cancelled = false;
    const image = new Image();
    image.onload = () => {
      if (!cancelled) setFailed(false);
    };
    image.onerror = () => {
      if (!cancelled) setFailed(true);
    };
    image.src = firstFrameSrc(sequence);

    return () => {
      cancelled = true;
    };
  }, [ready, reduced, sequence]);

  if (!ready || !sequence || reduced || failed) {
    return <HeroSection />;
  }

  return <ScrollSequence sequence={sequence} skin="aura" />;
}
