"use client";

import { useEffect, useState } from "react";

export function BackToTop() {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const footer = document.querySelector("footer");
    if (!footer) return;

    const observer = new IntersectionObserver(
      (entries) => {
        setVisible(entries.some((entry) => entry.isIntersecting));
      },
      { threshold: 0.12 },
    );

    observer.observe(footer);
    return () => observer.disconnect();
  }, []);

  return (
    <button
      type="button"
      className={visible ? "home-top is-visible" : "home-top"}
      aria-label="Volver arriba"
      onClick={() => {
        const reduce = window.matchMedia(
          "(prefers-reduced-motion: reduce)",
        ).matches;
        const behavior = reduce ? "auto" : "smooth";
        window.scrollTo({ top: 0, behavior });
        document.documentElement.scrollTo({ top: 0, behavior });
      }}
    >
      <svg
        aria-hidden="true"
        viewBox="0 0 24 24"
        className="h-4 w-4"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.7"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        <path d="M12 19V6M6 11l6-6 6 6" />
      </svg>
      Volver arriba
    </button>
  );
}
