"use client";

import { useEffect } from "react";

export function AuraAtmosphere() {
  useEffect(() => {
    const root = document.querySelector<HTMLElement>(".aura-home");
    if (!root) return;

    const fine = window.matchMedia("(hover: hover) and (pointer: fine)");
    const reduced = window.matchMedia("(prefers-reduced-motion: reduce)");
    if (!fine.matches || reduced.matches) return;

    const cursor = document.createElement("div");
    cursor.className = "aura-cursor";
    cursor.setAttribute("aria-hidden", "true");
    document.body.appendChild(cursor);

    let x = 0;
    let y = 0;
    let rafId = 0;
    let scheduled = false;

    const buttons = () => root.querySelectorAll<HTMLElement>("[data-magnetic]");

    const paint = () => {
      scheduled = false;
      cursor.style.transform = `translate3d(${x}px, ${y}px, 0)`;
      let hot = false;
      buttons().forEach((button) => {
        const rect = button.getBoundingClientRect();
        const cx = rect.left + rect.width / 2;
        const cy = rect.top + rect.height / 2;
        const dx = x - cx;
        const dy = y - cy;
        const dist = Math.hypot(dx, dy);
        if (dist < 140 && dist > 0) {
          hot = true;
          const pull = (1 - dist / 140) * 12;
          button.style.transform = `translate3d(${(dx / dist) * pull}px, ${(dy / dist) * pull}px, 0)`;
        } else {
          button.style.transform = "";
        }
      });
      cursor.classList.toggle("is-hot", hot);
    };

    const onMove = (event: MouseEvent) => {
      x = event.clientX;
      y = event.clientY;
      cursor.style.opacity = "1";
      if (scheduled) return;
      scheduled = true;
      rafId = requestAnimationFrame(paint);
    };

    const onLeave = () => {
      cursor.style.opacity = "0";
      buttons().forEach((button) => {
        button.style.transform = "";
      });
    };

    window.addEventListener("mousemove", onMove, { passive: true });
    document.documentElement.addEventListener("mouseleave", onLeave);

    return () => {
      cancelAnimationFrame(rafId);
      window.removeEventListener("mousemove", onMove);
      document.documentElement.removeEventListener("mouseleave", onLeave);
      buttons().forEach((button) => {
        button.style.transform = "";
      });
      cursor.remove();
    };
  }, []);

  return (
    <svg aria-hidden="true" className="aura-grain">
      <filter id="aura-grain" x="0" y="0" width="100%" height="100%">
        <feTurbulence
          type="fractalNoise"
          baseFrequency="0.8"
          numOctaves="2"
          stitchTiles="stitch"
        />
      </filter>
      <rect width="100%" height="100%" filter="url(#aura-grain)" />
    </svg>
  );
}
