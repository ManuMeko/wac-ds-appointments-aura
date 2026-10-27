import { auraConfig } from "@/data/aura";
import { tallerConfig } from "@/data/taller";
import type { DemoConfig, DemoId } from "@/data/types";

/**
 * Demo activa del sitio.
 *
 * NEXT_PUBLIC_DEMO elige el contenido y el tema en el build
 * (aura | taller). Si falta o no es "taller", se usa Aura.
 * Un export estático publica una sola demo.
 */

const demos: Record<DemoId, DemoConfig> = {
  aura: auraConfig,
  taller: tallerConfig,
};

function selectDemo(): DemoConfig {
  const requested = process.env.NEXT_PUBLIC_DEMO?.trim();

  if (requested === "taller") {
    return demos.taller;
  }

  return demos.aura;
}

export const business = selectDemo();
