"use client";

import { useCallback, useEffect, useRef, useSyncExternalStore } from "react";

import {
  type FrameStatus,
  loadFrameSequence,
  paintCanvasFrame,
} from "@/components/sequence/frames";

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

export function usePrefersReducedMotion() {
  return useSyncExternalStore(
    subscribeReducedMotion,
    getReducedMotionSnapshot,
    getReducedMotionServerSnapshot,
  );
}

type FrameBank = {
  images: Array<HTMLImageElement | null>;
  statuses: FrameStatus[];
  users: number;
  listeners: Set<() => void>;
  cancel: () => void;
};

const banks = new Map<string, FrameBank>();

type UseFrameSequenceOptions = {
  basePath: string;
  frameCount: number;
  fileExtension: string;
  fill: string;
  stride?: number;
  priorityIndexes?: readonly number[];
};

export function useFrameSequence({
  basePath,
  frameCount,
  fileExtension,
  fill,
  stride = 1,
  priorityIndexes = [],
}: UseFrameSequenceOptions) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const bankRef = useRef<FrameBank | null>(null);
  const progressRef = useRef(0);

  const paint = useCallback((progress?: number) => {
    if (typeof progress === "number") progressRef.current = progress;
    const canvas = canvasRef.current;
    const bank = bankRef.current;
    if (!canvas || !bank) return;
    paintCanvasFrame(
      canvas,
      bank.images,
      bank.statuses,
      progressRef.current,
      frameCount,
      fill,
    );
  }, [fill, frameCount]);

  const priorityKey = priorityIndexes.join(",");

  useEffect(() => {
    const count = Math.max(0, Math.floor(frameCount));
    const key = `${basePath}|${count}|${fileExtension}|${stride}|${priorityKey}`;
    const indexes = priorityKey
      ? priorityKey.split(",").map((value) => Number(value))
      : [];
    let bank = banks.get(key);

    if (!bank) {
      const images = Array.from({ length: count }, () => null);
      const statuses = Array.from(
        { length: count },
        () => "pending" as FrameStatus,
      );
      const listeners = new Set<() => void>();
      const cancel = loadFrameSequence({
        basePath,
        frameCount: count,
        fileExtension,
        images,
        statuses,
        stride,
        priorityIndexes: indexes,
        onUpdate: () => {
          listeners.forEach((listener) => listener());
        },
      });
      bank = { images, statuses, users: 0, listeners, cancel };
      banks.set(key, bank);
    }

    bank.users += 1;
    bankRef.current = bank;

    const listener = () => paint();
    bank.listeners.add(listener);

    const canvas = canvasRef.current;
    const observer = new ResizeObserver(() => paint());
    if (canvas) observer.observe(canvas);
    paint();

    return () => {
      observer.disconnect();
      bank.listeners.delete(listener);
      bank.users -= 1;
      if (bank.users === 0) {
        bank.cancel();
        banks.delete(key);
      }
      if (bankRef.current === bank) bankRef.current = null;
    };
  }, [
    basePath,
    fileExtension,
    frameCount,
    paint,
    priorityKey,
    stride,
  ]);

  return { canvasRef, paint };
}
