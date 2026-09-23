// apps/web/components/home/ReviewsSection.tsx

import { StarIcon } from "@/components/ui/StarIcon";
import {
  auraReviews,
  auraReviewsIntro,
} from "@/data/aura";

/* =========================================================
   WAC DS · MODEL 01 · APPOINTMENTS
   Aura Studio · Reviews Section
========================================================= */

export function ReviewsSection() {
  return (
    <section
      id="opiniones"
      className="relative overflow-hidden bg-[#302a26] text-white"
    >
      {/* =====================================================
          AMBIENT BACKGROUND
      ====================================================== */}
      <div className="pointer-events-none absolute -right-24 -top-24 h-72 w-72 rounded-full bg-[#8e725f]/10 blur-3xl" />

      <div className="pointer-events-none absolute -bottom-32 -left-24 h-80 w-80 rounded-full bg-black/10 blur-3xl" />

      <div className="relative mx-auto w-full max-w-[1440px] px-5 py-20 sm:px-8 lg:px-12 lg:py-28 xl:px-16">
        {/* =====================================================
            INTRO
        ====================================================== */}
        <div className="flex flex-col justify-between gap-8 lg:flex-row lg:items-end">
          <div className="max-w-[760px]">
            <p className="text-[10px] font-semibold uppercase tracking-[0.27em] text-[#c1a189]">
              {auraReviewsIntro.eyebrow}
            </p>

            <h2 className="mt-5 font-serif text-5xl leading-[0.97] tracking-[-0.035em] text-[#f5eee8] sm:text-6xl">
              {auraReviewsIntro.title}
            </h2>
          </div>

          <p className="max-w-[390px] text-sm leading-7 text-[#a99e96]">
            La experiencia se mide también en cómo te sientes al salir y en
            las ganas de volver.
          </p>
        </div>

        {/* =====================================================
            REVIEWS GRID
        ====================================================== */}
        <div className="mt-14 grid gap-4 lg:grid-cols-3">
          {auraReviews.map((review, index) => (
            <article
              key={review.id}
              className="group relative flex min-h-[320px] flex-col justify-between overflow-hidden rounded-[1.6rem] border border-white/10 bg-[#342e2a] p-7 shadow-[0_20px_60px_rgba(0,0,0,0.10)] transition-all duration-300 hover:-translate-y-1 hover:border-white/15 hover:bg-[#39322d] hover:shadow-[0_28px_80px_rgba(0,0,0,0.16)] sm:p-9"
            >
              {/* Decorative quote mark */}
              <span
                aria-hidden="true"
                className="pointer-events-none absolute right-6 top-2 font-serif text-[7rem] leading-none text-white/[0.035]"
              >
                “
              </span>

              <div>
                {/* Rating */}
                <div
                  className="flex gap-1 text-[#c6a68e]"
                  aria-label={`${review.rating} de 5 estrellas`}
                >
                  {Array.from({ length: review.rating }).map((_, starIndex) => (
                    <StarIcon
                      key={`${review.id}-star-${starIndex}`}
                      className="h-4 w-4"
                    />
                  ))}
                </div>

                {/* Quote */}
                <blockquote className="mt-8 max-w-[360px] font-serif text-[1.5rem] leading-[1.35] tracking-[-0.015em] text-[#eee5de]">
                  “{review.quote}”
                </blockquote>
              </div>

              {/* Author */}
              <div className="mt-10 flex items-center justify-between gap-4 border-t border-white/10 pt-6">
                <div className="flex items-center gap-3">
                  <span className="h-px w-8 bg-[#9d846f]/60" />

                  <p className="text-xs font-semibold uppercase tracking-[0.16em] text-[#a99a90]">
                    {review.author}
                  </p>
                </div>

                <span className="text-[10px] font-semibold uppercase tracking-[0.16em] text-[#746a64]">
                  {String(index + 1).padStart(2, "0")}
                </span>
              </div>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}