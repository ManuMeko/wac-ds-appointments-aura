"use client";

import { useEffect, useRef } from "react";

import { business } from "@/config/business";

function splitLines(text: string) {
  const words = text.trim().split(/\s+/);
  if (words.length <= 5) return [text.trim()];

  const lines = Math.min(3, Math.ceil(words.length / 4));
  const size = Math.ceil(words.length / lines);
  const out: string[] = [];
  for (let index = 0; index < words.length; index += size) {
    out.push(words.slice(index, index + size).join(" "));
  }
  return out;
}

export function MaskLines({
  text,
  italic = false,
  className = "",
}: {
  text: string;
  italic?: boolean;
  className?: string;
}) {
  const ref = useRef<HTMLSpanElement>(null);
  const lines = splitLines(text);

  useEffect(() => {
    const node = ref.current;
    if (!node || business.theme !== "aura") return;

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
      { threshold: 0.2, rootMargin: "0px 0px -6% 0px" },
    );
    observer.observe(node);
    return () => observer.disconnect();
  }, []);

  if (business.theme !== "aura") {
    return <span className={className}>{text}</span>;
  }

  return (
    <span ref={ref} className={`aura-mask ${className}`.trim()}>
      {lines.map((line, index) => (
        <span
          key={`${line}-${index}`}
          className="aura-mask-line"
          style={{ ["--mask-index" as string]: index }}
        >
          <span className={italic ? "italic" : undefined}>{line}</span>
        </span>
      ))}
    </span>
  );
}
