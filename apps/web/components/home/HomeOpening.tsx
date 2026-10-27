"use client";

import { HeroSection } from "@/components/home/HeroSection";
import { ScrollSequence } from "@/components/home/ScrollSequence";
import { usePrefersReducedMotion } from "@/components/sequence/useFrameSequence";
import type { DemoId, ScrollSequenceConfig } from "@/data/types";

export function HomeOpening({
  ready,
  sequence,
  skin,
}: {
  ready: boolean;
  sequence?: ScrollSequenceConfig;
  skin: DemoId;
}) {
  const reducedMotion = usePrefersReducedMotion();
  const showHero =
    !ready || !sequence || (skin === "aura" && reducedMotion);

  if (showHero) return <HeroSection />;

  return <ScrollSequence sequence={sequence} skin={skin} />;
}
