"use client";

import { useSyncExternalStore } from "react";

import { StarIcon } from "@/components/ui/StarIcon";
import type { Review } from "@/data/types";

function subscribeReducedMotion(onChange: () => void) {
  const media = window.matchMedia("(prefers-reduced-motion: reduce)");
  media.addEventListener("change", onChange);
  return () => media.removeEventListener("change", onChange);
}

function ReviewCard({ review, index }: { review: Review; index: number }) {
  return (
    <article className="relative flex min-h-[280px] w-[min(82vw,22rem)] shrink-0 flex-col justify-between overflow-hidden rounded-[1.6rem] border border-white/10 bg-[#292524] p-7">
      <div>
        <div
          className="flex gap-1 text-[#f97316]"
          aria-label={`${review.rating} de 5 estrellas`}
        >
          {Array.from({ length: review.rating }).map((_, starIndex) => (
            <StarIcon key={`${review.id}-${index}-star-${starIndex}`} className="h-4 w-4" />
          ))}
        </div>
        <blockquote className="mt-6 font-serif text-[1.35rem] leading-[1.35] tracking-[-0.02em] text-white">
          “{review.quote}”
        </blockquote>
      </div>
      <p className="mt-8 border-t border-white/10 pt-5 text-xs font-semibold uppercase tracking-[0.16em] text-white/70">
        {review.author}
      </p>
    </article>
  );
}

export function ReviewMarquee({ reviews }: { reviews: readonly Review[] }) {
  const reduced = useSyncExternalStore(
    subscribeReducedMotion,
    () => window.matchMedia("(prefers-reduced-motion: reduce)").matches,
    () => false,
  );
  const row = (copy: string) =>
    reviews.map((review, index) => (
      <ReviewCard key={`${copy}-${review.id}`} review={review} index={index} />
    ));

  if (reduced) {
    return (
      <div className="mt-14 grid gap-4 lg:grid-cols-3">
        {row("still")}
      </div>
    );
  }

  return (
    <div className="wac-marquee mt-14 overflow-hidden">
      <div className="wac-marquee-track flex w-max">
        <div className="flex gap-4 pr-4">{row("a")}</div>
        <div className="flex gap-4 pr-4" aria-hidden="true">
          {row("b")}
        </div>
      </div>
    </div>
  );
}
