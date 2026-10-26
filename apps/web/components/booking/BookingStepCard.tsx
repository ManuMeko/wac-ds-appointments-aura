"use client";

import { useState } from "react";

import { BookingCanvas } from "@/components/booking/BookingCanvas";
import { usePrefersReducedMotion } from "@/components/sequence/useFrameSequence";
import type { BookingHeroSequence } from "@/data/types";

const PHASE_FALLBACK = [
  "Fase 1/4 · Recepción",
  "2/4 · Ruedas y frenos",
  "3/4 · Carrocería",
  "4/4 · Mecánica",
];

export type BookingChip = {
  kind: "service" | "professional" | "datetime";
  text: string;
  price?: string;
  stepIndex: number;
};

type BookingStepCardProps = {
  labels: readonly string[];
  currentStepIndex: number;
  sequence: BookingHeroSequence;
  stepFrames?: readonly number[];
  phaseLabels?: readonly string[];
  target: number;
  chips?: readonly BookingChip[];
  onStepSelect?: (index: number) => void;
};

function StepGlyph({
  index,
  className = "h-5 w-5",
}: {
  index: number;
  className?: string;
}) {
  const common = {
    viewBox: "0 0 24 24",
    "aria-hidden": true as const,
    className,
    fill: "none",
    stroke: "currentColor",
    strokeWidth: 1.7,
    strokeLinecap: "round" as const,
    strokeLinejoin: "round" as const,
  };

  if (index === 1) {
    return (
      <svg {...common}>
        <circle cx="12" cy="8" r="3" />
        <path d="M5 19c1.2-3 3.4-4.5 7-4.5S17.8 16 19 19" />
      </svg>
    );
  }

  if (index === 2) {
    return (
      <svg {...common}>
        <rect x="4" y="5" width="16" height="15" rx="2" />
        <path d="M8 3.5v3M16 3.5v3M4 10h16" />
      </svg>
    );
  }

  if (index === 3) {
    return (
      <svg {...common}>
        <path d="M7 3.5h7l4 4V20a1.5 1.5 0 0 1-1.5 1.5h-9.5A1.5 1.5 0 0 1 5.5 20V5A1.5 1.5 0 0 1 7 3.5Z" />
        <path d="M14 3.5V8h4.5M8 13h8M8 17h5" />
      </svg>
    );
  }

  return (
    <svg {...common}>
      <path d="M14.7 6.3a4 4 0 0 0-5.4 5.4L4 17l3 3 5.3-5.3a4 4 0 0 0 5.4-5.4L15 12l-3-3 2.7-2.7Z" />
    </svg>
  );
}

function CheckGlyph() {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      className="h-5 w-5"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="m6 12 4 4 8-8" />
    </svg>
  );
}

function PhaseThumb({
  sequence,
  stepFrames,
  target,
  label,
}: {
  sequence: BookingHeroSequence;
  stepFrames?: readonly number[];
  target: number;
  label: string;
}) {
  const reducedMotion = usePrefersReducedMotion();
  const [layer, setLayer] = useState({
    current: target,
    previous: null as number | null,
  });

  if (target !== layer.current) {
    setLayer({
      current: target,
      previous: reducedMotion ? null : layer.current,
    });
  }

  const changing = layer.previous !== null;

  return (
    <div className="w-full shrink-0 md:w-[180px]">
      <div
        className={[
          "booking-phase-ring relative h-[140px] overflow-hidden rounded-2xl bg-[#1c1917] md:h-[110px] md:w-[180px]",
          changing ? "is-changing" : "",
        ].join(" ")}
      >
        {layer.previous !== null ? (
          <div
            key={layer.previous}
            className="booking-phase-out absolute inset-0"
            onAnimationEnd={() => {
              setLayer((current) =>
                current.previous === null
                  ? current
                  : { ...current, previous: null },
              );
            }}
          >
            <BookingCanvas
              sequence={sequence}
              stepFrames={stepFrames}
              mode="hold"
              immediate
              target={layer.previous}
              className="absolute inset-0 h-full w-full"
            />
          </div>
        ) : null}
        <div
          key={layer.current}
          className={changing ? "booking-phase-in absolute inset-0" : "absolute inset-0"}
        >
          <BookingCanvas
            sequence={sequence}
            stepFrames={stepFrames}
            mode="hold"
            immediate
            target={layer.current}
            className="absolute inset-0 h-full w-full"
          />
        </div>
      </div>
      <p className="mt-2 text-center text-[11px] font-semibold tracking-[0.04em] text-[#44403c] md:text-left">
        {label}
      </p>
    </div>
  );
}

export function BookingStepCard({
  labels,
  currentStepIndex,
  sequence,
  stepFrames,
  phaseLabels,
  target,
  chips = [],
  onStepSelect,
}: BookingStepCardProps) {
  const progress =
    labels.length <= 1 ? 1 : currentStepIndex / (labels.length - 1);
  const phaseLabel =
    phaseLabels?.[currentStepIndex] ??
    PHASE_FALLBACK[currentStepIndex] ??
    "";

  return (
    <div className="relative z-[2] -mt-12 mb-8">
      <div className="rounded-[24px] border border-[#1c1917]/10 bg-white p-4 shadow-[0_28px_70px_rgba(28,25,23,0.14)] sm:p-5">
        <div className="flex flex-col gap-4 md:flex-row md:items-center md:gap-6">
          <PhaseThumb
            sequence={sequence}
            stepFrames={stepFrames}
            target={target}
            label={phaseLabel}
          />

          <div className="min-w-0 flex-1">
            <ol aria-label="Pasos de la reserva" className="flex items-start">
              {labels.map((label, index) => {
                const active = currentStepIndex === index;
                const completed = currentStepIndex > index;
                const circle = [
                  "relative z-[1] flex h-10 w-10 items-center justify-center rounded-full",
                  active
                    ? "booking-step-pulse bg-[#c2410c] text-white"
                    : completed
                      ? "bg-[#1c1917] text-white"
                      : "border border-[#d6d3d1] bg-white text-[#57534e]",
                ].join(" ");
                const caption = [
                  "mt-2 text-sm font-semibold",
                  active
                    ? "text-[#1c1917]"
                    : completed
                      ? "text-[#44403c]"
                      : "text-[#57534e]",
                  active ? "block" : "hidden md:block",
                ].join(" ");

                const body = (
                  <>
                    <span className={circle}>
                      {completed ? <CheckGlyph /> : <StepGlyph index={index} />}
                    </span>
                    <span className={caption}>{label}</span>
                  </>
                );

                return (
                  <li
                    key={label}
                    aria-current={active ? "step" : undefined}
                    className="relative flex min-w-0 flex-1 flex-col items-center"
                  >
                    <p
                      className={[
                        "mb-2 hidden text-[10px] font-semibold tracking-[0.16em] uppercase md:block",
                        active || completed ? "text-[#44403c]" : "text-[#57534e]",
                      ].join(" ")}
                    >
                      Paso {index + 1}
                    </p>
                    <div className="relative flex w-full justify-center">
                      {index < labels.length - 1 ? (
                        <span
                          aria-hidden="true"
                          className="absolute top-5 left-1/2 hidden h-0.5 w-full -translate-y-1/2 bg-[#e7e5e4] md:block"
                        >
                          <span
                            className="booking-step-line absolute inset-0 origin-left bg-[#c2410c]"
                            style={{ transform: `scaleX(${completed ? 1 : 0})` }}
                          />
                        </span>
                      ) : null}
                      {completed && onStepSelect ? (
                        <button
                          type="button"
                          onClick={() => onStepSelect(index)}
                          aria-label={`Volver a ${label}`}
                          className="flex flex-col items-center rounded-2xl focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[#c2410c]"
                        >
                          {body}
                        </button>
                      ) : (
                        <div className="flex flex-col items-center">{body}</div>
                      )}
                    </div>
                  </li>
                );
              })}
            </ol>
            <div
              aria-hidden="true"
              className="mt-3 h-0.5 overflow-hidden rounded-full bg-[#e7e5e4] md:hidden"
            >
              <div
                className="booking-step-line h-full w-full origin-left bg-[#c2410c]"
                style={{ transform: `scaleX(${progress})` }}
              />
            </div>
          </div>
        </div>

        {chips.length > 0 ? (
          <div className="mt-4 flex flex-wrap items-center gap-2 border-t border-[#1c1917]/10 pt-4">
            <span className="text-[11px] font-medium text-[#57534e]">Tu cita:</span>
            {chips.map((chip) => {
              const content = (
                <>
                  <StepGlyph
                    index={chip.kind === "service" ? 0 : chip.kind === "professional" ? 1 : 2}
                    className="h-3.5 w-3.5"
                  />
                  <span>
                    {chip.text}
                    {chip.price ? (
                      <>
                        {" · "}
                        <strong className="font-semibold">{chip.price}</strong>
                      </>
                    ) : null}
                  </span>
                </>
              );
              const chipClass =
                "inline-flex items-center gap-1.5 rounded-full bg-[#f4f4f5] px-3 py-1.5 text-[13px] text-[#1c1917]";
              const chipStyle = { fontSize: "13px" };

              return onStepSelect ? (
                <button
                  key={`${chip.kind}-${chip.text}`}
                  type="button"
                  onClick={() => onStepSelect(chip.stepIndex)}
                  aria-label={`Volver a ${labels[chip.stepIndex] ?? "este paso"}`}
                  className={`${chipClass} transition hover:bg-[#e7e5e4] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#c2410c]`}
                  style={chipStyle}
                >
                  {content}
                </button>
              ) : (
                <span key={`${chip.kind}-${chip.text}`} className={chipClass} style={chipStyle}>
                  {content}
                </span>
              );
            })}
          </div>
        ) : null}
      </div>
    </div>
  );
}
