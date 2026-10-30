import { existsSync } from "node:fs";
import path from "node:path";

import { business } from "@/config/business";

function shotExists(src: string): boolean {
  const relative = src.replace(/^\//, "");
  return existsSync(path.join(process.cwd(), "public", relative));
}

export function OwnerControlSection() {
  const panel = business.ownerPanel;
  const shots = panel.shots.filter((shot) => shotExists(shot.src));

  if (shots.length === 0) {
    return null;
  }

  const taller = business.theme === "taller";

  return (
    <section id="panel" className="bg-(--canvas)">
      <div className="mx-auto w-full max-w-[1440px] px-5 py-20 sm:px-8 lg:px-12 lg:py-28 xl:px-16">
        <p className="text-[10px] font-semibold uppercase tracking-[0.27em] text-(--p-8b725f)">
          {panel.eyebrow}
        </p>
        <h2 className="aura-display mt-5 max-w-[14ch] font-serif text-5xl leading-[0.96] tracking-[-0.035em] text-(--ink) sm:text-6xl">
          {panel.title}
        </h2>
        <p className="mt-6 max-w-[38rem] text-sm leading-7 text-(--ink-muted) sm:text-base">
          {panel.description}
        </p>

        <div className="mt-12 grid gap-6 lg:grid-cols-2">
          {shots.map((shot) => (
            <figure
              key={shot.src}
              className={[
                "owner-panel-card overflow-hidden rounded-[1.6rem] border bg-white/70",
                taller ? "border-[#e7e5e4]" : "border-(--inverse)/10",
              ].join(" ")}
            >
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={shot.src}
                alt={shot.alt}
                width={1440}
                height={900}
                className="aspect-[16/10] w-full object-cover object-top"
              />
              <figcaption className="px-6 py-5">
                <p className="text-[10px] font-semibold uppercase tracking-[0.18em] text-(--p-8b725f)">
                  {shot.label}
                </p>
                <p className="mt-2 font-serif text-2xl text-(--heading)">{shot.caption}</p>
              </figcaption>
            </figure>
          ))}
        </div>
      </div>
    </section>
  );
}
