"use client";

import { useRef, type MouseEvent, type ReactNode } from "react";

import { business } from "@/config/business";

export function TiltCard({ children }: { children: ReactNode }) {
  const ref = useRef<HTMLDivElement>(null);

  if (business.theme !== "taller") return children;

  function move(event: MouseEvent<HTMLDivElement>) {
    const node = ref.current;
    if (!node) return;
    if (window.innerWidth < 1024) return;
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;

    const rect = node.getBoundingClientRect();
    const x = (event.clientX - rect.left) / rect.width - 0.5;
    const y = (event.clientY - rect.top) / rect.height - 0.5;
    node.style.transform = `rotateX(${(-y * 8).toFixed(2)}deg) rotateY(${(x * 8).toFixed(2)}deg)`;
  }

  function leave() {
    const node = ref.current;
    if (!node) return;
    node.style.transform = "rotateX(0deg) rotateY(0deg)";
  }

  return (
    <div className="[perspective:1400px]">
      <div
        ref={ref}
        onMouseMove={move}
        onMouseLeave={leave}
        className="transition-transform duration-200 will-change-transform"
        style={{ transform: "rotateX(0deg) rotateY(0deg)" }}
      >
        {children}
      </div>
    </div>
  );
}
