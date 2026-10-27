"use client";

import { useEffect, useRef, type ReactNode } from "react";

/* Keeps the services intro in view while the list scrolls.
   The home root uses overflow-x: hidden, which breaks position: sticky. */

export function PinAside({ children }: { children: ReactNode }) {
  const slotRef = useRef<HTMLDivElement>(null);
  const panelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const slot = slotRef.current;
    const panel = panelRef.current;
    const list = slot?.nextElementSibling as HTMLElement | null;
    if (!slot || !panel || !list) return;

    const desktop = window.matchMedia("(min-width: 1024px)");
    let frame = 0;
    let scheduled = false;

    const reset = () => {
      panel.style.position = "";
      panel.style.top = "";
      panel.style.bottom = "";
      panel.style.left = "";
      panel.style.width = "";
    };

    const place = () => {
      reset();
      if (!desktop.matches) return;

      const topOffset = 96;
      const slotRect = slot.getBoundingClientRect();
      const listRect = list.getBoundingClientRect();
      const panelHeight = panel.offsetHeight;
      const width = slotRect.width;
      if (width < 1) return;

      if (listRect.top >= topOffset) return;

      if (listRect.bottom - panelHeight <= topOffset) {
        panel.style.position = "absolute";
        panel.style.top = "auto";
        panel.style.bottom = "0px";
        panel.style.left = "0px";
        panel.style.width = `${width}px`;
        return;
      }

      panel.style.position = "fixed";
      panel.style.top = `${topOffset}px`;
      panel.style.left = `${slotRect.left}px`;
      panel.style.width = `${width}px`;
    };

    const onScroll = () => {
      if (scheduled) return;
      scheduled = true;
      frame = requestAnimationFrame(() => {
        scheduled = false;
        place();
      });
    };

    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    document.addEventListener("scroll", onScroll, {
      passive: true,
      capture: true,
    });
    window.addEventListener("resize", onScroll);
    desktop.addEventListener("change", onScroll);

    return () => {
      cancelAnimationFrame(frame);
      window.removeEventListener("scroll", onScroll);
      document.removeEventListener("scroll", onScroll, true);
      window.removeEventListener("resize", onScroll);
      desktop.removeEventListener("change", onScroll);
      reset();
    };
  }, []);

  return (
    <div ref={slotRef} className="relative min-w-0">
      <div ref={panelRef}>{children}</div>
    </div>
  );
}
