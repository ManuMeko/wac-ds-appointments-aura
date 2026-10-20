// apps/web/components/home/ReviewsSection.tsx

import { StarIcon } from "@/components/ui/StarIcon";
import { business } from "@/config/business";

/* =========================================================
   WAC DS · MODEL 01 · APPOINTMENTS
   Aura Studio · Reviews Section
========================================================= */

export function ReviewsSection() {
  return (
    <section
      id="opiniones"
      className="relative overflow-hidden bg-(--p-302a26) text-white"
    >
      {/* =====================================================
          AMBIENT BACKGROUND
      ====================================================== */}
      <div className="pointer-events-none absolute -right-24 -top-24 h-72 w-72 rounded-full bg-(--p-8e725f)/10 blur-3xl" />

      <div className="pointer-events-none absolute -bottom-32 -left-24 h-80 w-80 rounded-full bg-black/10 blur-3xl" />

      <div className="relative mx-auto w-full max-w-[1440px] px-5 py-20 sm:px-8 lg:px-12 lg:py-28 xl:px-16">
        {/* =====================================================
            INTRO
        ====================================================== */}
        <div className="flex flex-col justify-between gap-8 lg:flex-row lg:items-end">
          <div className="max-w-[760px]">
            <p className="text-[10px] font-semibold uppercase tracking-[0.27em] text-(--p-c1a189)">
              {business.reviewsIntro.eyebrow}
            </p>

            <h2 className="mt-5 font-serif text-5xl leading-[0.97] tracking-[-0.035em] text-(--p-f5eee8) sm:text-6xl">
              {business.reviewsIntro.title}
            </h2>
          </div>

          <p className="max-w-[390px] text-sm leading-7 text-(--p-a99e96)">
            {business.reviewsIntro.aside}
          </p>
        </div>

        {/* =====================================================
            REVIEWS GRID
        ====================================================== */}
        <div className="mt-14 grid gap-4 lg:grid-cols-3">
          {business.reviews.map((review, index) => (
            <article
              key={review.id}
              className="group relative flex min-h-[320px] flex-col justify-between overflow-hidden rounded-[1.6rem] border border-white/10 bg-(--p-342e2a) p-7 shadow-[0_20px_60px_rgba(0,0,0,0.10)] transition-all duration-300 hover:-translate-y-1 hover:border-white/15 hover:bg-(--p-39322d) hover:shadow-[0_28px_80px_rgba(0,0,0,0.16)] sm:p-9"
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
                  className="flex gap-1 text-(--p-c6a68e)"
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
                <blockquote className="mt-8 max-w-[360px] font-serif text-[1.5rem] leading-[1.35] tracking-[-0.015em] text-(--p-eee5de)">
                  “{review.quote}”
                </blockquote>
              </div>

              {/* Author */}
              <div className="mt-10 flex items-center justify-between gap-4 border-t border-white/10 pt-6">
                <div className="flex items-center gap-3">
                  <span className="h-px w-8 bg-(--p-9d846f)/60" />

                  <p className="text-xs font-semibold uppercase tracking-[0.16em] text-(--p-a99a90)">
                    {review.author}
                  </p>
                </div>

                <span className="text-[10px] font-semibold uppercase tracking-[0.16em] text-(--p-746a64)">
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