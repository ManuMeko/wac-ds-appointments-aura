
"use client";

import {
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
} from "react";

type CalendarMode = "popover" | "inline";

type CalendarTheme = "aura" | "slate";

export type WacCalendarProps = {
  value: string;
  onChange: (date: string) => void;

  mode?: CalendarMode;
  theme?: CalendarTheme;

  label?: string;
  placeholder?: string;

  minDate?: string;
  maxDate?: string;

  disabled?: boolean;

  isDateDisabled?: (date: string) => boolean;

  className?: string;
};

const WEEKDAYS = [
  "L",
  "M",
  "X",
  "J",
  "V",
  "S",
  "D",
];

const MONTHS = [
  "enero",
  "febrero",
  "marzo",
  "abril",
  "mayo",
  "junio",
  "julio",
  "agosto",
  "septiembre",
  "octubre",
  "noviembre",
  "diciembre",
];

const THEMES = {
  aura: {
    accent: "#342d28",
    accentHover: "#4b4038",
    accentSoft: "#eee7df",
    border: "#e5dbd2",
    surface: "#ffffff",
    surfaceSoft: "#faf8f6",
    foreground: "#342d28",
    muted: "#938276",
    today: "#a78a73",
  },
  slate: {
    accent: "#25364a",
    accentHover: "#364b64",
    accentSoft: "#e8edf3",
    border: "#dbe2e9",
    surface: "#ffffff",
    surfaceSoft: "#f8fafc",
    foreground: "#25364a",
    muted: "#7a8795",
    today: "#53769b",
  },
} as const;

// =========================================================
// DATE HELPERS
// =========================================================

function isValidDateKey(value: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return false;
  }

  const [year, month, day] = value
    .split("-")
    .map(Number);

  const date = new Date(
    Date.UTC(year, month - 1, day)
  );

  return (
    date.getUTCFullYear() === year &&
    date.getUTCMonth() === month - 1 &&
    date.getUTCDate() === day
  );
}

function toDateKey(
  year: number,
  month: number,
  day: number
): string {
  return [
    String(year).padStart(4, "0"),
    String(month).padStart(2, "0"),
    String(day).padStart(2, "0"),
  ].join("-");
}

function parseDateKey(value: string): Date {
  const [year, month, day] = value
    .split("-")
    .map(Number);

  return new Date(
    Date.UTC(year, month - 1, day, 12)
  );
}

function getTodayKey(
  timezone = "Europe/Madrid"
): string {
  const parts = new Intl.DateTimeFormat(
    "en-GB",
    {
      timeZone: timezone,
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    }
  ).formatToParts(new Date());

  const year = parts.find(
    (part) => part.type === "year"
  )?.value;

  const month = parts.find(
    (part) => part.type === "month"
  )?.value;

  const day = parts.find(
    (part) => part.type === "day"
  )?.value;

  if (!year || !month || !day) {
    return new Date().toISOString().slice(0, 10);
  }

  return `${year}-${month}-${day}`;
}

function formatDisplayDate(value: string): string {
  if (!isValidDateKey(value)) {
    return "";
  }

  return new Intl.DateTimeFormat(
    "es-ES",
    {
      day: "2-digit",
      month: "short",
      year: "numeric",
      timeZone: "UTC",
    }
  ).format(parseDateKey(value));
}

function formatAccessibleDate(value: string): string {
  return new Intl.DateTimeFormat(
    "es-ES",
    {
      weekday: "long",
      day: "numeric",
      month: "long",
      year: "numeric",
      timeZone: "UTC",
    }
  ).format(parseDateKey(value));
}

function moveMonth(
  year: number,
  month: number,
  amount: number
): {
  year: number;
  month: number;
} {
  const result = new Date(
    Date.UTC(year, month + amount, 1)
  );

  return {
    year: result.getUTCFullYear(),
    month: result.getUTCMonth(),
  };
}

// =========================================================
// COMPONENT
// =========================================================

export default function WacCalendar({
  value,
  onChange,
  mode = "popover",
  theme = "aura",
  label,
  placeholder = "Seleccionar fecha",
  minDate,
  maxDate,
  disabled = false,
  isDateDisabled,
  className = "",
}: WacCalendarProps) {
  const calendarId = useId();

  const rootRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);

  const today = getTodayKey();

  const selectedDate = isValidDateKey(value)
    ? value
    : null;

  const initialDate = selectedDate ?? today;

  const [open, setOpen] = useState(false);

  const [visibleMonth, setVisibleMonth] = useState(
    () => {
      const date = parseDateKey(initialDate);

      return {
        year: date.getUTCFullYear(),
        month: date.getUTCMonth(),
      };
    }
  );

  const palette = THEMES[theme];

  // =======================================================
  // SYNCHRONIZE SELECTED MONTH
  // =======================================================

  useEffect(() => {
    if (!selectedDate) {
      return;
    }

    const parsed = parseDateKey(selectedDate);

    setVisibleMonth({
      year: parsed.getUTCFullYear(),
      month: parsed.getUTCMonth(),
    });
  }, [selectedDate]);

  // =======================================================
  // CLOSE ON OUTSIDE CLICK / ESCAPE
  // =======================================================

  useEffect(() => {
    if (mode !== "popover" || !open) {
      return;
    }

    function handlePointerDown(event: PointerEvent) {
      const target = event.target;

      if (
        target instanceof Node &&
        rootRef.current &&
        !rootRef.current.contains(target)
      ) {
        setOpen(false);
      }
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setOpen(false);
        triggerRef.current?.focus();
      }
    }

    document.addEventListener(
      "pointerdown",
      handlePointerDown
    );

    document.addEventListener(
      "keydown",
      handleKeyDown
    );

    return () => {
      document.removeEventListener(
        "pointerdown",
        handlePointerDown
      );

      document.removeEventListener(
        "keydown",
        handleKeyDown
      );
    };
  }, [mode, open]);

  // =======================================================
  // CALENDAR GRID
  // =======================================================

  const calendarDays = useMemo(() => {
    const firstDay = new Date(
      Date.UTC(
        visibleMonth.year,
        visibleMonth.month,
        1
      )
    );

    // Monday = 0, Sunday = 6.
    const leadingDays =
      (firstDay.getUTCDay() + 6) % 7;

    const daysInMonth = new Date(
      Date.UTC(
        visibleMonth.year,
        visibleMonth.month + 1,
        0
      )
    ).getUTCDate();

    const totalCells = Math.ceil(
      (leadingDays + daysInMonth) / 7
    ) * 7;

    return Array.from(
      { length: totalCells },
      (_, index) => {
        const date = new Date(
          Date.UTC(
            visibleMonth.year,
            visibleMonth.month,
            index - leadingDays + 1
          )
        );

        return {
          key: toDateKey(
            date.getUTCFullYear(),
            date.getUTCMonth() + 1,
            date.getUTCDate()
          ),
          day: date.getUTCDate(),
          currentMonth:
            date.getUTCMonth() === visibleMonth.month &&
            date.getUTCFullYear() === visibleMonth.year,
        };
      }
    );
  }, [visibleMonth]);

  // =======================================================
  // VALIDATION
  // =======================================================

  function dateIsDisabled(date: string): boolean {
    if (disabled) {
      return true;
    }

    if (minDate && date < minDate) {
      return true;
    }

    if (maxDate && date > maxDate) {
      return true;
    }

    return isDateDisabled?.(date) ?? false;
  }

  const previousMonth = moveMonth(
    visibleMonth.year,
    visibleMonth.month,
    -1
  );

  const nextMonth = moveMonth(
    visibleMonth.year,
    visibleMonth.month,
    1
  );

  const previousMonthDisabled = Boolean(
    minDate &&
    toDateKey(
      previousMonth.year,
      previousMonth.month + 1,
      1
    ) < minDate.slice(0, 7) + "-01"
  );

  const nextMonthDisabled = Boolean(
    maxDate &&
    toDateKey(
      nextMonth.year,
      nextMonth.month + 1,
      1
    ) > maxDate.slice(0, 7) + "-01"
  );

  // =======================================================
  // ACTIONS
  // =======================================================

  function selectDate(date: string) {
    if (dateIsDisabled(date)) {
      return;
    }

    onChange(date);

    if (mode === "popover") {
      setOpen(false);
    }
  }

  function navigateMonth(amount: number) {
    const result = moveMonth(
      visibleMonth.year,
      visibleMonth.month,
      amount
    );

    setVisibleMonth(result);
  }

  function goToToday() {
    const date = parseDateKey(today);

    setVisibleMonth({
      year: date.getUTCFullYear(),
      month: date.getUTCMonth(),
    });

    if (!dateIsDisabled(today)) {
      selectDate(today);
    }
  }

  // =======================================================
  // CALENDAR CONTENT
  // =======================================================

  const calendarContent = (
    <div
      id={calendarId}
      className="w-full rounded-[22px] border p-4 shadow-[0_18px_55px_rgba(54,42,33,0.10)] sm:p-5"
      style={{
        backgroundColor: palette.surface,
        borderColor: palette.border,
        color: palette.foreground,
      }}
    >
      {/* MONTH HEADER */}

      <div className="flex items-center justify-between gap-3">
        <button
          type="button"
          onClick={() => navigateMonth(-1)}
          disabled={disabled || previousMonthDisabled}
          aria-label="Mes anterior"
          className="flex h-9 w-9 items-center justify-center rounded-full border text-lg transition hover:opacity-70 disabled:cursor-not-allowed disabled:opacity-30"
          style={{
            borderColor: palette.border,
          }}
        >
          ‹
        </button>

        <div className="text-center">
          <div className="text-[10px] font-semibold uppercase tracking-[0.18em]"
            style={{ color: palette.muted }}
          >
            Selecciona un día
          </div>

          <div className="mt-1 text-base font-semibold capitalize tracking-[-0.02em]">
            {MONTHS[visibleMonth.month]}{" "}
            {visibleMonth.year}
          </div>
        </div>

        <button
          type="button"
          onClick={() => navigateMonth(1)}
          disabled={disabled || nextMonthDisabled}
          aria-label="Mes siguiente"
          className="flex h-9 w-9 items-center justify-center rounded-full border text-lg transition hover:opacity-70 disabled:cursor-not-allowed disabled:opacity-30"
          style={{
            borderColor: palette.border,
          }}
        >
          ›
        </button>
      </div>

      {/* WEEKDAYS */}

      <div className="mt-5 grid grid-cols-7 gap-1">
        {WEEKDAYS.map((weekday, index) => (
          <div
            key={index}
            className="flex h-8 items-center justify-center text-[11px] font-semibold"
            style={{ color: palette.muted }}
          >
            {weekday}
          </div>
        ))}
      </div>

      {/* DAYS */}

      <div className="mt-1 grid grid-cols-7 gap-1">
        {calendarDays.map((day) => {
          const selected = selectedDate === day.key;
          const isToday = today === day.key;
          const unavailable = dateIsDisabled(day.key);

          return (
            <button
              key={day.key}
              type="button"
              disabled={unavailable}
              onClick={() => selectDate(day.key)}
              aria-label={formatAccessibleDate(day.key)}
              aria-pressed={selected}
              aria-current={isToday ? "date" : undefined}
              className={[
                "relative flex aspect-square min-h-9 w-full items-center justify-center rounded-full text-xs font-medium transition-colors sm:text-sm",
                unavailable
                  ? "cursor-not-allowed opacity-25"
                  : "hover:bg-[#f0ebe6]",
                !day.currentMonth
                  ? "opacity-40"
                  : "",
              ].join(" ")}
              style={{
                backgroundColor: selected
                  ? palette.accent
                  : "transparent",
                color: selected
                  ? "#ffffff"
                  : palette.foreground,
                fontWeight: selected ? 700 : 500,
              }}
            >
              {day.day}

              {isToday && (
                <span
                  className="absolute bottom-1 left-1/2 h-1 w-1 -translate-x-1/2 rounded-full"
                  style={{
                    backgroundColor: selected
                      ? "#ffffff"
                      : palette.today,
                  }}
                />
              )}
            </button>
          );
        })}
      </div>

      {/* FOOTER */}

      <div
        className="mt-4 flex items-center justify-between gap-3 border-t pt-4"
        style={{ borderColor: palette.border }}
      >
        <div className="min-w-0">
          <div
            className="text-[10px] font-semibold uppercase tracking-[0.12em]"
            style={{ color: palette.muted }}
          >
            {selectedDate
              ? "Fecha seleccionada"
              : "Sin fecha seleccionada"}
          </div>

          <div className="mt-1 truncate text-xs font-semibold">
            {selectedDate
              ? formatDisplayDate(selectedDate)
              : "Elige una fecha"}
          </div>
        </div>

        <button
          type="button"
          onClick={goToToday}
          disabled={disabled}
          className="shrink-0 rounded-full border px-4 py-2 text-xs font-semibold transition hover:opacity-70 disabled:opacity-40"
          style={{
            borderColor: palette.border,
            backgroundColor: palette.surfaceSoft,
          }}
        >
          Hoy
        </button>
      </div>
    </div>
  );

  // =======================================================
  // INLINE MODE
  // =======================================================

  if (mode === "inline") {
    return (
      <div className={className}>
        {label && (
          <div
            className="mb-2 text-xs font-semibold uppercase tracking-[0.12em]"
            style={{ color: palette.muted }}
          >
            {label}
          </div>
        )}

        {calendarContent}
      </div>
    );
  }

  // =======================================================
  // POPOVER MODE
  // =======================================================

  return (
    <div
      ref={rootRef}
      className={`relative inline-block ${className}`}
    >
      {label && (
        <div
          className="mb-2 text-xs font-semibold uppercase tracking-[0.12em]"
          style={{ color: palette.muted }}
        >
          {label}
        </div>
      )}

      <button
        ref={triggerRef}
        type="button"
        disabled={disabled}
        aria-label="Seleccionar fecha"
        aria-expanded={open}
        aria-controls={open ? calendarId : undefined}
        aria-haspopup="dialog"
        onClick={() => setOpen((current) => !current)}
        className="flex h-11 min-w-[165px] items-center justify-between gap-3 rounded-full border bg-white px-4 text-sm font-medium shadow-[0_3px_12px_rgba(50,40,32,0.03)] transition hover:shadow-md disabled:cursor-not-allowed disabled:opacity-40"
        style={{
          borderColor: palette.border,
          color: palette.foreground,
        }}
      >
        <span>
          {selectedDate
            ? formatDisplayDate(selectedDate)
            : placeholder}
        </span>

        <svg
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.7"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden="true"
        >
          <rect
            x="3"
            y="5"
            width="18"
            height="16"
            rx="2"
          />
          <path d="M16 3v4M8 3v4M3 10h18" />
        </svg>
      </button>

      {open && (
        <div
          role="dialog"
          aria-label="Calendario de selección de fecha"
          className="absolute right-0 top-full z-[100] mt-2 w-[min(340px,calc(100vw-32px))]"
        >
          {calendarContent}
        </div>
      )}
    </div>
  );
}
