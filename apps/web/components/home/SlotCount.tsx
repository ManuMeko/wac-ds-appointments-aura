"use client";

import { business } from "@/config/business";
import { slotCountLabel, useHomeAvailability } from "@/lib/homeAvailability";

export function SlotCount({ onVideo = false }: { onVideo?: boolean }) {
  const availability = useHomeAvailability();
  const taller = business.theme === "taller";

  if (availability === null) {
    return (
      <span
        aria-hidden="true"
        className={[
          "inline-block h-4 w-28 animate-pulse rounded-full motion-reduce:animate-none",
          onVideo
            ? "bg-white/25"
            : taller
              ? "bg-[#e7e5e4]"
              : "bg-(--p-eee5db)",
        ].join(" ")}
      />
    );
  }

  if (availability.status !== "ready" || !availability.featured) {
    return null;
  }

  return (
    <p
      className={[
        "max-w-[220px] text-[11px] leading-snug",
        onVideo ? "text-white/85" : "text-(--p-6f6259)",
      ].join(" ")}
    >
      {slotCountLabel(
        availability.featured.serviceName,
        availability.featured.todayTotal,
      )}
    </p>
  );
}
