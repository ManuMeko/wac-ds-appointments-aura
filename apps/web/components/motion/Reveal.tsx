"use client";

import { useEffect, useRef, type CSSProperties, type ReactNode } from "react";

import { business } from "@/config/business";

type RevealVariant = "fade-up" | "fade-left" | "fade-right" | "scale-in" | "draw" | "zoom";

export function Reveal({
  children = null,
  variant = "fade-up",
  delay = 0,
  className = "",
}: {
  children?: ReactNode;
  variant?: RevealVariant;
  delay?: number;
  className?: string;
}) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const node = ref.current;
    if (!node) return;
    if (business.theme !== "taller" && business.theme !== "aura") return;

    const reveal = () => node.classList.add("is-visible");

    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
      reveal();
      return;
    }

    const observer = new IntersectionObserver(
      (entries) => {
        if (!entries.some((entry) => entry.isIntersecting)) return;
        reveal();
        observer.disconnect();
      },
      { threshold: 0.18, rootMargin: "0px 0px -8% 0px" },
    );

    observer.observe(node);
    return () => observer.disconnect();
  }, []);

  if (business.theme === "aura") {
    return (
      <div
        ref={ref}
        data-reveal={variant}
        className={`aura-reveal aura-reveal-${variant} ${className}`.trim()}
        style={{ "--reveal-delay": `${delay}ms` } as CSSProperties}
      >
        {children}
      </div>
    );
  }

  if (business.theme !== "taller") return children;

  return (
    <div
      ref={ref}
      data-reveal={variant}
      className={`reveal ${className}`.trim()}
      style={{ "--reveal-delay": `${delay}ms` } as CSSProperties}
    >
      {children}
    </div>
  );
}
