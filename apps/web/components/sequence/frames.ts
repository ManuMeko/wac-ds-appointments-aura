export type FrameStatus = "pending" | "ready" | "error";

export const FRAME_PRELOAD_CONCURRENCY = 4;

export function frameSrc(
  basePath: string,
  frameNumber: number,
  extension: string,
) {
  const base = basePath.endsWith("/") ? basePath.slice(0, -1) : basePath;
  return `${base}/frame_${String(frameNumber).padStart(4, "0")}.${extension}`;
}

export function frameIndexForProgress(progress: number, frameCount: number) {
  const count = Math.max(1, Math.floor(frameCount));
  const clamped = Math.min(1, Math.max(0, progress));
  return Math.min(count - 1, Math.max(0, Math.round(clamped * (count - 1))));
}

export function drawCover(
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

export function imageForFrame(
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

export function paintCanvasFrame(
  canvas: HTMLCanvasElement,
  images: Array<HTMLImageElement | null>,
  statuses: FrameStatus[],
  progress: number,
  frameCount: number,
  fill: string,
) {
  const context = canvas.getContext("2d");
  if (!context) return;

  const bounds = canvas.getBoundingClientRect();
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
  context.fillStyle = fill;
  context.fillRect(0, 0, cssWidth, cssHeight);

  const frameIndex = frameIndexForProgress(progress, frameCount);
  const image = imageForFrame(images, statuses, frameIndex);
  if (image) drawCover(context, image, cssWidth, cssHeight);
}

type LoadOptions = {
  basePath: string;
  frameCount: number;
  fileExtension: string;
  images: Array<HTMLImageElement | null>;
  statuses: FrameStatus[];
  onUpdate: () => void;
  stride?: number;
  priorityIndexes?: readonly number[];
  /** El primer fotograma de la apertura va en alta prioridad. Un fondo diferido no. */
  eagerFirst?: boolean;
};

export function loadFrameSequence({
  basePath,
  frameCount,
  fileExtension,
  images,
  statuses,
  onUpdate,
  stride = 1,
  priorityIndexes = [],
  eagerFirst = true,
}: LoadOptions) {
  let cancelled = false;
  const count = Math.max(0, Math.floor(frameCount));
  const step = Math.max(1, Math.floor(stride));
  const include = new Set(
    priorityIndexes.filter(
      (index) => index >= 0 && index < count,
    ),
  );

  const sampled: number[] = [];
  for (let index = 0; index < count; index += 1) {
    if (include.has(index) || index % step === 0) sampled.push(index);
  }

  if (sampled.length < 1) {
    onUpdate();
    return () => {
      cancelled = true;
    };
  }

  const selected = sampled;

  const loadFrame = (index: number) =>
    new Promise<void>((resolve) => {
      if (cancelled) {
        resolve();
        return;
      }

      const image = new Image();
      image.decoding = "async";
      image.fetchPriority = index === 0 && eagerFirst ? "high" : "low";

      const settle = (status: FrameStatus) => {
        if (!cancelled) {
          statuses[index] = status;
          if (status === "ready") images[index] = image;
          onUpdate();
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
      image.src = frameSrc(basePath, index + 1, fileExtension);
    });

  const preload = async () => {
    if (selected.length < 1) {
      onUpdate();
      return;
    }

    await loadFrame(selected[0] ?? 0);
    if (cancelled || selected.length < 2) return;

    let cursor = 1;
    const workers = Array.from(
      { length: Math.min(FRAME_PRELOAD_CONCURRENCY, selected.length - 1) },
      async () => {
        while (!cancelled) {
          const position = cursor;
          cursor += 1;
          const index = selected[position];
          if (index === undefined) return;
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
}
