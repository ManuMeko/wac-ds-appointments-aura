import fs from "node:fs";
import path from "node:path";

import type { ScrollSequenceConfig } from "@/data/types";

/**
 * La secuencia solo se monta si frameCount es mayor que 0
 * y existe el primer fotograma en public/.
 */
export function scrollSequenceReady(sequence: ScrollSequenceConfig | undefined) {
  if (!sequence || sequence.frameCount < 1) return false;

  const folder = sequence.basePath.replace(/^\/+/, "");
  const first = path.join(
    process.cwd(),
    "public",
    folder,
    `frame_0001.${sequence.fileExtension}`,
  );

  return fs.existsSync(first);
}
