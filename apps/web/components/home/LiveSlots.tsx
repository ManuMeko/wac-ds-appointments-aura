"use client";

import { business } from "@/config/business";
import {
  formatSlotTime,
  slotCountLabel,
  useHomeAvailability,
} from "@/lib/homeAvailability";

export function LiveSlots() {
  const availability = useHomeAvailability();
  const taller = business.theme === "taller";

  if (availability === null) {
    return (
      <section aria-hidden="true" className="bg-(--canvas)">
        <div className="mx-auto w-full max-w-[1440px] px-5 py-14 sm:px-8 lg:px-12 xl:px-16">
          <div
            className={[
              "h-3 w-28 animate-pulse rounded-full motion-reduce:animate-none",
              taller ? "bg-[#e7e5e4]" : "bg-(--p-eee5db)",
            ].join(" ")}
          />
          <div
            className={[
              "mt-5 h-10 w-64 max-w-full animate-pulse rounded-2xl motion-reduce:animate-none",
              taller ? "bg-[#e7e5e4]" : "bg-(--p-eadfd4)",
            ].join(" ")}
          />
          <div className="mt-8 grid gap-3 sm:grid-cols-3">
            {[0, 1, 2].map((item) => (
              <div
                key={item}
                className={[
                  "h-20 animate-pulse rounded-[1.25rem] motion-reduce:animate-none",
                  taller ? "bg-[#f5f5f4]" : "bg-white/70",
                ].join(" ")}
              />
            ))}
          </div>
        </div>
      </section>
    );
  }

  if (availability.status !== "ready" || !availability.featured) {
    return null;
  }

  const featured = availability.featured;
  const href = `/reservar?servicio=${encodeURIComponent(featured.serviceSlug)}`;

  return (
    <section className="bg-(--canvas)">
      <div className="mx-auto w-full max-w-[1440px] px-5 py-14 sm:px-8 lg:px-12 lg:py-20 xl:px-16">
        <p className="text-[10px] font-semibold uppercase tracking-[0.27em] text-(--p-8b725f)">
          Disponibilidad
        </p>
        <h2 className="aura-display mt-4 max-w-[12ch] font-serif text-4xl leading-[0.98] tracking-[-0.03em] text-(--ink) sm:text-5xl">
          Próximos huecos
        </h2>
        <p className="mt-4 max-w-[36rem] text-sm leading-7 text-(--ink-muted)">
          {slotCountLabel(featured.serviceName, featured.todayTotal)}
          {featured.dayLabel === "mañana" && featured.slots.length > 0
            ? " Estos son los primeros de mañana."
            : " La cita queda registrada al elegir hora."}
        </p>

        {featured.slots.length > 0 ? (
          <div className="mt-8 grid gap-3 sm:grid-cols-3">
            {featured.slots.map((slot) => (
              <a
                key={`${slot.professionalName}-${slot.startTime}`}
                href={href}
                className={[
                  "group rounded-[1.25rem] border px-5 py-4 transition duration-300 hover:-translate-y-0.5 motion-reduce:transition-none motion-reduce:hover:translate-y-0",
                  taller
                    ? "border-[#e7e5e4] bg-white hover:border-[#fdba74]"
                    : "border-(--inverse)/10 bg-white/70 hover:border-(--accent)/40",
                ].join(" ")}
              >
                <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-(--p-8b725f)">
                  {featured.dayLabel === "mañana" ? "Mañana" : "Hoy"}
                </p>
                <p className="mt-2 font-serif text-3xl text-(--heading)">
                  {formatSlotTime(slot.startTime)}
                </p>
                <p className="mt-1 text-sm text-(--ink-muted)">{slot.professionalName}</p>
              </a>
            ))}
          </div>
        ) : null}
      </div>
    </section>
  );
}
