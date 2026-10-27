"use client";

import { HeroSection } from "@/components/home/HeroSection";
import { ScrollSequence } from "@/components/home/ScrollSequence";
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
  const showHero = !ready || !sequence;

  if (showHero) return <HeroSection />;

  return <ScrollSequence sequence={sequence} skin={skin} />;
}
