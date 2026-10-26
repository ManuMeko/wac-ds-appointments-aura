"use client";

import Link from "next/link";
import {
  type FormEvent,
  type ReactNode,
  type RefObject,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

import { BookingCanvas } from "@/components/booking/BookingCanvas";
import { BookingHero } from "@/components/booking/BookingHero";
import {
  BookingStepCard,
  type BookingChip,
} from "@/components/booking/BookingStepCard";
import { SiteFooter } from "@/components/layout/SiteFooter";
import { SiteHeader } from "@/components/layout/SiteHeader";
import { Reveal } from "@/components/motion/Reveal";
import { TradeIcon } from "@/components/ui/TradeIcon";
import { business } from "@/config/business";
import type { BookingExtraField, TradeIconName } from "@/data/types";
import { getApiBaseUrl } from "@/lib/api";

/* =========================================================
   WAC DS · MODEL 01 · APPOINTMENTS
   Aura Studio · Public Booking
========================================================= */

const BUSINESS_SLUG = business.slug;

const API_BASE_URL = getApiBaseUrl().replace(/\/$/, "");

const copy = business.booking;

const extraFields = business.bookingExtraFields;

function composeBookingNotes(
  notes: string,
  extras: Record<string, string>,
  fields: readonly BookingExtraField[],
): string | null {
  const lines = fields.flatMap((field) => {
    const value = extras[field.id]?.trim();

    return value ? [`${field.notesLabel}: ${value}`] : [];
  });

  const body = notes.trim();

  if (lines.length === 0) {
    return body || null;
  }

  if (!body) {
    return lines.join("\n");
  }

  return `${lines.join("\n")}\n\n${body}`.slice(0, 2000);
}

function extraNotesBudget(
  extras: Record<string, string>,
  fields: readonly BookingExtraField[],
): number {
  const lines = fields.flatMap((field) => {
    const value = extras[field.id]?.trim();

    return value ? [`${field.notesLabel}: ${value}`] : [];
  });

  if (lines.length === 0) {
    return 2000;
  }

  return Math.max(0, 2000 - lines.join("\n").length - 2);
}

/* =========================================================
   TYPES
========================================================= */

type BookingStep =
  | "service"
  | "professional"
  | "datetime"
  | "customer"
  | "success";

type PublicCatalogService = {
  id: number;
  slug: string;
  name: string;
  description: string | null;
  durationMinutes: number;
  price: number;
  currency: string;
};

type PublicCatalogProfessional = {
  id: number;
  displayName: string;
  bio: string | null;
};

type AvailabilitySlot = {
  startTime: string;
  endTime: string;
  startAt: string;
  endAt: string;
};

type DailyAvailabilityResponse = {
  businessId: number;
  professionalId: number;
  professionalName: string;
  serviceId: number;
  serviceName: string;
  serviceDurationMinutes: number;
  date: string;
  timezone: string;
  available: boolean;
  totalSlots: number;
  slots: AvailabilitySlot[];
};

type PublicBookingResponse = {
  appointmentId: number;
  status: string;
  businessId: number;
  businessName: string;
  serviceId: number;
  serviceName: string;
  serviceDurationMinutes: number;
  professionalId: number;
  professionalName: string;
  customerName: string;
  startAt: string;
  endAt: string;
  timezone: string;
  managementToken: string;
  message: string;
};

type CustomerForm = {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  notes: string;
};

type ApiErrorPayload = {
  message?: string;
  error?: string;
  detail?: string;
};

type CalendarDatePickerProps = {
  value: string;
  minDate: string;
  maxDate: string;
  disabled?: boolean;
  onChange: (date: string) => void;
};

type CalendarDay = {
  key: string;
  date: string | null;
  dayNumber: number | null;
};

/* =========================================================
   CONSTANTS
========================================================= */

const INITIAL_CUSTOMER_FORM: CustomerForm = {
  firstName: "",
  lastName: "",
  email: "",
  phone: "",
  notes: "",
};

const CALENDAR_WEEKDAYS = [
  "L",
  "M",
  "X",
  "J",
  "V",
  "S",
  "D",
];

const CALENDAR_MONTHS = [
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

/* =========================================================
   DATE HELPERS
========================================================= */

function dateToLocalIso(date: Date): string {
  const year = date.getFullYear();

  const month = String(
    date.getMonth() + 1
  ).padStart(2, "0");

  const day = String(
    date.getDate()
  ).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

function parseLocalDate(value: string): Date {
  const [year, month, day] = value
    .split("-")
    .map(Number);

  return new Date(
    year,
    month - 1,
    day
  );
}

function getTodayLocalDate(): string {
  return dateToLocalIso(
    new Date()
  );
}

function getMaximumDate(): string {
  const date = new Date();

  date.setDate(
    date.getDate() + 365
  );

  return dateToLocalIso(
    date
  );
}

function getMonthKey(date: Date): number {
  return (
    date.getFullYear() * 12 +
    date.getMonth()
  );
}

function addMonths(
  date: Date,
  amount: number
): Date {
  return new Date(
    date.getFullYear(),
    date.getMonth() + amount,
    1
  );
}

function getCalendarDays(
  visibleMonth: Date
): CalendarDay[] {
  const year =
    visibleMonth.getFullYear();

  const month =
    visibleMonth.getMonth();

  const firstDay =
    new Date(
      year,
      month,
      1
    );

  /*
   * JS:
   * domingo = 0
   * lunes   = 1
   *
   * Calendario Aura:
   * lunes   = 0
   * domingo = 6
   */
  const leadingDays =
    (
      firstDay.getDay() +
      6
    ) %
    7;

  const numberOfDays =
    new Date(
      year,
      month + 1,
      0
    ).getDate();

  const days: CalendarDay[] =
    [];

  for (
    let index = 0;
    index < leadingDays;
    index += 1
  ) {
    days.push({
      key: `empty-start-${index}`,
      date: null,
      dayNumber: null,
    });
  }

  for (
    let day = 1;
    day <= numberOfDays;
    day += 1
  ) {
    const date =
      new Date(
        year,
        month,
        day
      );

    const iso =
      dateToLocalIso(
        date
      );

    days.push({
      key: iso,
      date: iso,
      dayNumber: day,
    });
  }

  while (
    days.length % 7 !== 0
  ) {
    days.push({
      key: `empty-end-${days.length}`,
      date: null,
      dayNumber: null,
    });
  }

  return days;
}

/* =========================================================
   FORMAT HELPERS
========================================================= */

function formatMoney(
  value: number,
  currency: string
): string {
  try {
    return new Intl.NumberFormat(
      "es-ES",
      {
        style: "currency",
        currency,
      }
    ).format(value);
  } catch {
    return `${value.toFixed(2)} ${currency}`;
  }
}

function formatTime(
  value: string
): string {
  return value.slice(0, 5);
}

function formatChipWhen(
  date: string,
  time: string
): string {
  const parsed = parseLocalDate(date);
  const weekday = new Intl.DateTimeFormat(
    "es-ES",
    { weekday: "short" }
  )
    .format(parsed)
    .replace(/\./g, "");
  const label =
    weekday.charAt(0).toLocaleUpperCase("es-ES") +
    weekday.slice(1);

  return `${label} ${parsed.getDate()} · ${formatTime(time)}`;
}

function formatDate(
  value: string
): string {
  if (!value) {
    return "";
  }

  const date =
    parseLocalDate(
      value
    );

  return lowercaseLead(
    new Intl.DateTimeFormat(
      "es-ES",
      {
        weekday: "long",
        day: "numeric",
        month: "long",
        year: "numeric",
      }
    ).format(date)
  );
}

function formatSummaryDay(value: string): string {
  if (!value) {
    return "";
  }

  return lowercaseLead(
    new Intl.DateTimeFormat("es-ES", {
      weekday: "long",
      day: "numeric",
      month: "long",
    }).format(parseLocalDate(value)),
  );
}

function lowercaseLead(
  value: string
): string {
  if (!value) {
    return value;
  }

  return (
    value.charAt(0).toLocaleLowerCase("es-ES") +
    value.slice(1)
  );
}

function formatBookingDateTime(
  value: string
): string {
  return lowercaseLead(
    new Intl.DateTimeFormat(
      "es-ES",
      {
        weekday: "long",
        day: "numeric",
        month: "long",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      }
    ).format(
      new Date(value)
    )
  );
}

function getStatusLabel(
  status: string
): string {
  switch (
    status.toUpperCase()
  ) {
    case "CONFIRMED":
      return copy.statuses.CONFIRMED;

    case "PENDING":
      return copy.statuses.PENDING;

    case "COMPLETED":
      return copy.statuses.COMPLETED;

    case "CANCELLED":
      return copy.statuses.CANCELLED;

    case "NO_SHOW":
      return copy.statuses.NO_SHOW;

    default:
      return status;
  }
}

function icsEscape(value: string): string {
  return value
    .replace(/\\/g, "\\\\")
    .replace(/\r?\n/g, "\\n")
    .replace(/,/g, "\\,")
    .replace(/;/g, "\\;");
}

function icsStamp(iso: string): string {
  return new Date(iso).toISOString().replace(/[-:]/g, "").replace(/\.\d{3}/, "");
}

function downloadAppointmentIcs(booking: PublicBookingResponse) {
  const lines = [
    "BEGIN:VCALENDAR",
    "VERSION:2.0",
    "PRODID:-//WAC DS//Cita//ES",
    "CALSCALE:GREGORIAN",
    "METHOD:PUBLISH",
    "BEGIN:VEVENT",
    `UID:cita-${booking.appointmentId}@${booking.businessId}`,
    `DTSTAMP:${icsStamp(new Date().toISOString())}`,
    `DTSTART:${icsStamp(booking.startAt)}`,
    `DTEND:${icsStamp(booking.endAt)}`,
    `SUMMARY:${icsEscape(booking.serviceName)}`,
    `DESCRIPTION:${icsEscape(`${booking.professionalName} · ${booking.businessName}`)}`,
    "END:VEVENT",
    "END:VCALENDAR",
  ];
  const blob = new Blob([lines.join("\r\n")], {
    type: "text/calendar;charset=utf-8",
  });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = `cita-${booking.appointmentId}.ics`;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}

/* =========================================================
   API ERROR
========================================================= */

async function readApiError(
  response: Response,
  fallbackMessage: string
): Promise<string> {
  try {
    const payload =
      (await response.json()) as ApiErrorPayload;

    return (
      payload.message ||
      payload.detail ||
      payload.error ||
      fallbackMessage
    );
  } catch {
    return fallbackMessage;
  }
}

/* =========================================================
   SMALL UI
========================================================= */

function LoadingSpinner() {
  return (
    <span
      aria-hidden="true"
      className="h-4 w-4 animate-spin rounded-full border-2 border-current border-r-transparent"
    />
  );
}

function serviceIcon(
  service: PublicCatalogService,
): TradeIconName | undefined {
  const name = service.name.toLowerCase();
  const slug = service.slug.toLowerCase();

  return business.services.find((item) => {
    if (!item.icon) return false;
    const label = item.name.toLowerCase();
    const id = item.id.toLowerCase();
    return (
      slug === id ||
      slug.includes(id) ||
      id.includes(slug) ||
      name === label ||
      name.includes(label) ||
      label.includes(name)
    );
  })?.icon;
}

function StepNumber({
  number,
  active,
  completed,
}: {
  number: number;
  active: boolean;
  completed: boolean;
}) {
  return (
    <span
      className={[
        "flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-xs font-semibold transition-all",
        active || completed
          ? "bg-(--p-302924) text-white"
          : "border border-(--p-3d342e)/15 bg-white/40 text-(--p-83766d)",
      ].join(" ")}
    >
      {completed ? "✓" : number}
    </span>
  );
}

/* =========================================================
   AURA CUSTOM CALENDAR
========================================================= */

function CalendarDatePicker({
  value,
  minDate,
  maxDate,
  disabled = false,
  onChange,
}: CalendarDatePickerProps) {
  const tallerCalendar =
    business.theme === "taller";

  const today =
    getTodayLocalDate();

  const initialDate =
    value
      ? parseLocalDate(value)
      : parseLocalDate(minDate);

  const [
    visibleMonth,
    setVisibleMonth,
  ] =
    useState<Date>(
      new Date(
        initialDate.getFullYear(),
        initialDate.getMonth(),
        1
      )
    );

  const [
    trackedValue,
    setTrackedValue,
  ] =
    useState(
      value
    );

  if (
    value !== trackedValue
  ) {
    setTrackedValue(
      value
    );

    if (
      value
    ) {
      const selected =
        parseLocalDate(
          value
        );

      setVisibleMonth(
        new Date(
          selected.getFullYear(),
          selected.getMonth(),
          1
        )
      );
    }
  }

  const calendarDays =
    useMemo(
      () =>
        getCalendarDays(
          visibleMonth
        ),
      [visibleMonth]
    );

  const minimumMonth =
    parseLocalDate(
      minDate
    );

  const maximumMonth =
    parseLocalDate(
      maxDate
    );

  const currentMonthKey =
    getMonthKey(
      visibleMonth
    );

  const minimumMonthKey =
    getMonthKey(
      minimumMonth
    );

  const maximumMonthKey =
    getMonthKey(
      maximumMonth
    );

  const canGoPrevious =
    currentMonthKey >
    minimumMonthKey;

  const canGoNext =
    currentMonthKey <
    maximumMonthKey;

  const monthName =
    CALENDAR_MONTHS[
      visibleMonth.getMonth()
    ];

  const monthLabel =
    `${monthName.charAt(0).toLocaleUpperCase("es-ES")}${monthName.slice(1)} ${visibleMonth.getFullYear()}`;

  function goPreviousMonth() {
    if (
      disabled ||
      !canGoPrevious
    ) {
      return;
    }

    setVisibleMonth(
      current =>
        addMonths(
          current,
          -1
        )
    );
  }

  function goNextMonth() {
    if (
      disabled ||
      !canGoNext
    ) {
      return;
    }

    setVisibleMonth(
      current =>
        addMonths(
          current,
          1
        )
    );
  }

  function selectToday() {
    if (
      disabled ||
      today < minDate ||
      today > maxDate
    ) {
      return;
    }

    const todayDate =
      parseLocalDate(
        today
      );

    setVisibleMonth(
      new Date(
        todayDate.getFullYear(),
        todayDate.getMonth(),
        1
      )
    );

    onChange(
      today
    );
  }

  return (
    <div className="mt-5">
      {/* MONTH */}

      <div className="flex items-center justify-between gap-4">
        <button
          type="button"
          aria-label={copy.previousMonth}
          disabled={
            disabled ||
            !canGoPrevious
          }
          onClick={
            goPreviousMonth
          }
          className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full border border-(--p-3b332e)/10 bg-(--p-f8f3ee) text-xl text-(--p-5a4a40) transition hover:border-(--accent)/30 hover:bg-(--p-eadfd4) disabled:cursor-not-allowed disabled:opacity-25"
        >
          ‹
        </button>

        <div className="text-center">
          <p className="font-serif text-xl text-(--p-342c27)">
            {monthLabel}
          </p>

          <p className="mt-0.5 text-[9px] uppercase tracking-[0.18em] text-(--p-9a887c)">
            {copy.selectDay}
          </p>
        </div>

        <button
          type="button"
          aria-label={copy.nextMonth}
          disabled={
            disabled ||
            !canGoNext
          }
          onClick={
            goNextMonth
          }
          className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full border border-(--p-3b332e)/10 bg-(--p-f8f3ee) text-xl text-(--p-5a4a40) transition hover:border-(--accent)/30 hover:bg-(--p-eadfd4) disabled:cursor-not-allowed disabled:opacity-25"
        >
          ›
        </button>
      </div>

      {/* WEEKDAYS */}

      <div className="mt-6 grid grid-cols-7 gap-1">
        {CALENDAR_WEEKDAYS.map(
          weekday => (
            <div
              key={
                weekday
              }
              className="flex h-8 items-center justify-center text-[9px] font-semibold uppercase tracking-[0.13em] text-(--p-a19085)"
            >
              {weekday}
            </div>
          )
        )}
      </div>

      {/* DAYS */}

      <div className="grid grid-cols-7 gap-1.5">
        {calendarDays.map(
          day => {
            if (
              !day.date ||
              day.dayNumber === null
            ) {
              return (
                <div
                  key={
                    day.key
                  }
                  className="aspect-square"
                />
              );
            }

            const isSelected =
              day.date === value;

            const isToday =
              day.date === today;

            const isPast =
              day.date < minDate;

            const isDisabled =
              disabled ||
              isPast ||
              day.date > maxDate;

            return (
              <button
                key={
                  day.key
                }
                type="button"
                disabled={
                  isDisabled
                }
                aria-label={
                  formatDate(
                    day.date
                  )
                }
                aria-pressed={
                  isSelected
                }
                onClick={() =>
                  onChange(
                    day.date!
                  )
                }
                style={
                  isPast && tallerCalendar
                    ? { cursor: "not-allowed" }
                    : undefined
                }
                className={[
                  "relative flex aspect-square min-h-9 items-center justify-center rounded-full text-xs font-medium transition-all duration-200 sm:min-h-10",
                  isSelected
                    ? tallerCalendar
                      ? "bg-[#1c1917] text-white shadow-[0_10px_22px_rgba(28,25,23,0.28)]"
                      : "bg-(--p-302924) text-white shadow-[0_8px_18px_rgba(48,41,36,0.22)]"
                    : "",
                  !isSelected &&
                  isToday
                    ? "border border-(--p-9d7f6a)/50 bg-(--p-efe5db) text-(--p-54453c)"
                    : "",
                  !isSelected &&
                  !isToday &&
                  !isDisabled
                    ? "text-(--p-51453d) hover:bg-(--p-eadfd4) hover:text-(--p-2d2723)"
                    : "",
                  isDisabled
                    ? isPast && tallerCalendar
                      ? "cursor-not-allowed text-[#d6d3d1] opacity-50"
                      : "cursor-not-allowed text-(--p-d0c6bf)"
                    : "",
                ].join(" ")}
              >
                {
                  day.dayNumber
                }

                {isToday &&
                  !isSelected && (
                    <span
                      aria-hidden="true"
                      className={
                        tallerCalendar
                          ? "absolute bottom-[5px] h-1 w-1 rounded-full bg-[#c2410c]"
                          : "absolute bottom-[5px] h-1 w-1 rounded-full bg-(--p-92725d)"
                      }
                    />
                  )}
              </button>
            );
          }
        )}
      </div>

      {/* CALENDAR FOOTER */}

      <div className="mt-6 flex items-center justify-between gap-4 border-t border-(--p-3b332e)/10 pt-4">
        {value &&
        value !== today ? (
          <p className="text-[10px] font-semibold uppercase tracking-[0.15em] text-(--p-706258)">
            Día elegido
          </p>
        ) : (
          <button
            type="button"
            disabled={
              disabled
            }
            onClick={
              selectToday
            }
            className="text-[10px] font-semibold uppercase tracking-[0.15em] text-(--accent) transition hover:text-(--p-342d28) disabled:opacity-40"
          >
            {copy.today}
          </button>
        )}

        {value ? (
          <p className="text-right text-xs text-(--p-706258)">
            {formatDate(
              value
            )}
          </p>
        ) : (
          <p className="text-right text-xs text-(--p-9a8e86)">
            {copy.noDate}
          </p>
        )}
      </div>
    </div>
  );
}

function ChangeStepButton({
  label,
  onClick,
}: {
  label: string;
  onClick: () => void;
}) {
  const text = label.replace(/^←\s*/, "");

  return (
    <button
      type="button"
      onClick={onClick}
      className="group ml-auto inline-flex items-center gap-2 rounded-full border border-[#d6d3d1] px-[18px] py-[10px] text-[14px] font-medium text-[#1c1917] transition hover:border-[#c2410c] hover:text-[#c2410c] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#c2410c]"
      style={{ fontSize: "14px", fontWeight: 500 }}
    >
      <svg
        aria-hidden="true"
        viewBox="0 0 16 16"
        className="h-3.5 w-3.5 shrink-0 transition-transform duration-200 group-hover:-translate-x-[3px] motion-reduce:transition-none motion-reduce:group-hover:translate-x-0"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        <path d="M10 3.5 5.5 8 10 12.5" />
      </svg>
      {text}
    </button>
  );
}

function StepHeading({
  taller,
  index,
  eyebrow,
  title,
  description,
  descriptionClass,
  changeLabel,
  onChange,
}: {
  taller: boolean;
  index: number;
  eyebrow: string;
  title: string;
  description: ReactNode;
  descriptionClass?: string;
  changeLabel?: string;
  onChange?: () => void;
}) {
  if (!taller) {
    return (
      <>
        {onChange && changeLabel ? (
          <button
            type="button"
            onClick={onChange}
            className="mb-7 text-sm font-medium text-(--p-7a6759) transition hover:text-(--p-342d28)"
          >
            {changeLabel}
          </button>
        ) : null}
        <div className="mb-8">
          <p className="text-[10px] font-semibold uppercase tracking-[0.22em] text-(--p-987e6c)">
            {eyebrow}
          </p>
          <h2 className="mt-2 font-serif text-3xl text-(--heading) sm:text-4xl">
            {title}
          </h2>
          <p className={descriptionClass ?? "mt-3 text-sm leading-6 text-(--p-756960)"}>
            {description}
          </p>
        </div>
      </>
    );
  }

  return (
    <div className="mb-8">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <p className="text-[12px] font-semibold tracking-[0.12em] text-[#c2410c]">
            {eyebrow}
          </p>
          <span className="flex items-center gap-1" aria-hidden="true">
            {Array.from({ length: 4 }, (_, segment) => (
              <span
                key={segment}
                className={[
                  "h-1 w-4 rounded-full",
                  segment <= index ? "bg-[#c2410c]" : "bg-[#e7e5e4]",
                ].join(" ")}
              />
            ))}
          </span>
        </div>
        {onChange && changeLabel ? (
          <ChangeStepButton label={changeLabel} onClick={onChange} />
        ) : null}
      </div>
      <h2 className="pt-3 font-serif text-[32px] tracking-[-0.02em] text-(--heading) sm:text-[40px]">
        {title}
      </h2>
      <div className="mt-3 max-w-[40rem] text-sm leading-6 text-(--p-756960)">
        {description}
      </div>
    </div>
  );
}

function slotIsMorning(time: string): boolean {
  return Number.parseInt(time.slice(0, 2), 10) < 14;
}

function TimeSlotGrid({
  slots,
  selectedStartAt,
  taller,
  onSelect,
}: {
  slots: AvailabilitySlot[];
  selectedStartAt: string | null;
  taller: boolean;
  onSelect: (slot: AvailabilitySlot) => void;
}) {
  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
      {slots.map((slot) => {
        const selected = selectedStartAt === slot.startAt;

        return (
          <button
            key={slot.startAt}
            type="button"
            onClick={() => onSelect(slot)}
            className={[
              "rounded-xl border px-4 py-3.5 text-sm font-semibold transition-all duration-200",
              selected
                ? taller
                  ? "border-[#c2410c] bg-[#c2410c] text-white shadow-[0_10px_22px_rgba(194,65,12,0.28)]"
                  : "border-(--heading) bg-(--heading) text-white shadow-[0_10px_22px_rgba(51,43,38,0.18)]"
                : "border-(--p-3b332e)/12 bg-(--p-fffdfb) text-(--p-4b4038) hover:-translate-y-0.5 hover:border-(--accent)/35 hover:bg-(--p-f4ece5)",
            ].join(" ")}
          >
            {formatTime(slot.startTime)}
          </button>
        );
      })}
    </div>
  );
}

function TimesScroller({ children }: { children: ReactNode }) {
  return (
    <div className="relative mt-4 flex flex-col md:min-h-0 md:flex-1">
      <div
        className="booking-times space-y-4 overflow-visible pr-2 md:min-h-0 md:flex-1 md:overflow-y-auto"
        ref={(node) => {
          if (!node) return;

          const sync = () => {
            const canScroll = window.matchMedia("(min-width: 768px)").matches;
            const overflowing =
              canScroll && node.scrollHeight > node.clientHeight + 4;
            const atEnd =
              node.scrollTop + node.clientHeight >= node.scrollHeight - 8;
            node.dataset.overflow = overflowing && !atEnd ? "true" : "false";
          };

          sync();
          const observer = new ResizeObserver(sync);
          observer.observe(node);
          node.addEventListener("scroll", sync, { passive: true });
          window.addEventListener("resize", sync);

          return () => {
            observer.disconnect();
            node.removeEventListener("scroll", sync);
            window.removeEventListener("resize", sync);
          };
        }}
      >
        {children}
      </div>
      <div
        aria-hidden="true"
        className="booking-times-fade pointer-events-none absolute inset-x-0 bottom-0 hidden h-8 opacity-0 transition-opacity md:block"
      />
    </div>
  );
}

/* =========================================================
   PAGE
========================================================= */

const NOTE_SUGGESTIONS = [
  "Ruido al frenar",
  "Testigo encendido",
  "Vibraciones",
  "Revisión antes de viaje",
] as const;

function FieldGlyph({
  name,
}: {
  name:
    | "person"
    | "mail"
    | "phone"
    | "car"
    | "note"
    | "check"
    | "clock"
    | "calendar"
    | "wrench"
    | "duration";
}) {
  const paths: Record<typeof name, ReactNode> = {
    person: (
      <>
        <circle cx="12" cy="8" r="3.2" />
        <path d="M5.5 19.2c.8-3.2 3.3-4.8 6.5-4.8s5.7 1.6 6.5 4.8" />
      </>
    ),
    mail: (
      <>
        <rect x="3.5" y="5.5" width="17" height="13" rx="2" />
        <path d="m4.5 7 7.5 6L19.5 7" />
      </>
    ),
    phone: (
      <path d="M8.2 4.8h2.2l1.2 3-1.6 1a12 12 0 0 0 5.2 5.2l1-1.6 3 1.2v2.2c0 .7-.5 1.3-1.2 1.4A14.5 14.5 0 0 1 6.8 6c.1-.7.7-1.2 1.4-1.2z" />
    ),
    car: (
      <>
        <path d="M4 15.5 5.6 9.8A2 2 0 0 1 7.5 8.5h9a2 2 0 0 1 1.9 1.3L20 15.5" />
        <path d="M3.5 15.5h17V18a1 1 0 0 1-1 1h-1.2a1 1 0 0 1-1-1v-.5H6.7V18a1 1 0 0 1-1 1H4.5a1 1 0 0 1-1-1z" />
        <circle cx="7.2" cy="15.6" r="1" />
        <circle cx="16.8" cy="15.6" r="1" />
      </>
    ),
    note: (
      <>
        <path d="M7 4.5h7.2L19 9.2V19a1.5 1.5 0 0 1-1.5 1.5h-10A1.5 1.5 0 0 1 6 19V6A1.5 1.5 0 0 1 7.5 4.5H7z" />
        <path d="M14 4.8V9h4.2M9 13h6M9 16h4" />
      </>
    ),
    check: <path d="m5.5 12.5 4 4 9-9" />,
    clock: (
      <>
        <circle cx="12" cy="12" r="7.5" />
        <path d="M12 8.5V12l2.5 2" />
      </>
    ),
    calendar: (
      <>
        <rect x="4" y="5.5" width="16" height="14" rx="2" />
        <path d="M8 4v3M16 4v3M4 10h16" />
      </>
    ),
    wrench: (
      <path d="M14.5 6.2a3.2 3.2 0 0 0-4.3 4.2L5 15.6 8.4 19l5.2-5.2a3.2 3.2 0 0 0 4.2-4.3L15.2 12l-2.2-2.2z" />
    ),
    duration: (
      <>
        <path d="M8 4.5h8M8 19.5h8" />
        <path d="M9 4.5c0 4.2 6 3.2 6 7.5S9 15.3 9 19.5" />
      </>
    ),
  };

  return (
    <svg
      viewBox="0 0 24 24"
      aria-hidden="true"
      className="h-5 w-5"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      {paths[name]}
    </svg>
  );
}

function PremiumField({
  id,
  label,
  icon,
  value,
  valid,
  invalid,
  errorId,
  errorMessage,
  hintId,
  plate,
  multiline,
  type = "text",
  maxLength,
  autoComplete,
  inputMode,
  required,
  placeholder,
  notesRef,
  onChange,
  onInvalid,
}: {
  id: string;
  label: string;
  icon: "person" | "mail" | "phone" | "car" | "note";
  value: string;
  valid: boolean;
  invalid: boolean;
  errorId?: string;
  errorMessage?: string;
  hintId?: string;
  plate?: boolean;
  multiline?: boolean;
  type?: string;
  maxLength?: number;
  autoComplete?: string;
  inputMode?: "tel" | "email" | "text";
  required?: boolean;
  placeholder?: string;
  notesRef?: RefObject<HTMLTextAreaElement | null>;
  onChange: (value: string) => void;
  onInvalid?: (event: FormEvent<HTMLInputElement>) => void;
}) {
  const describedBy =
    [hintId, invalid && errorId ? errorId : undefined].filter(Boolean).join(" ") ||
    undefined;
  const controlClass = "booking-field-control";

  return (
    <label
      className={[
        "booking-field",
        plate ? "booking-field-plate" : "",
        valid ? "is-valid" : "",
        invalid ? "is-invalid" : "",
      ].join(" ")}
      htmlFor={id}
    >
      {plate ? (
        <span className="booking-plate">
          <span className="booking-plate-band" aria-hidden="true">
            E
          </span>
          <input
            id={id}
            type="text"
            required={required}
            maxLength={maxLength}
            autoComplete={autoComplete}
            placeholder={placeholder ?? " "}
            value={value}
            aria-invalid={invalid || undefined}
            aria-describedby={describedBy}
            onChange={(event) => onChange(event.target.value)}
            className={controlClass}
          />
          <span className="booking-field-label">{label}</span>
        </span>
      ) : (
        <>
          <span className="booking-field-icon" aria-hidden="true">
            <FieldGlyph name={icon} />
          </span>
          {multiline ? (
            <textarea
              id={id}
              ref={notesRef}
              rows={3}
              maxLength={maxLength}
              autoComplete={autoComplete}
              placeholder=" "
              value={value}
              aria-invalid={invalid || undefined}
              aria-describedby={describedBy}
              onChange={(event) => onChange(event.target.value)}
              className={controlClass}
            />
          ) : (
            <input
              id={id}
              type={type}
              required={required}
              maxLength={maxLength}
              autoComplete={autoComplete}
              inputMode={inputMode}
              placeholder=" "
              value={value}
              aria-invalid={invalid || undefined}
              aria-describedby={describedBy}
              onChange={(event) => onChange(event.target.value)}
              onInvalid={onInvalid}
              className={controlClass}
            />
          )}
          <span className="booking-field-label">{label}</span>
        </>
      )}
      <span className="booking-field-check" aria-hidden="true">
        <FieldGlyph name="check" />
      </span>
      {invalid && errorMessage ? (
        <p id={errorId} role="alert" className="booking-field-error is-shown">
          {errorMessage}
        </p>
      ) : null}
    </label>
  );
}

function TallerConfirmation({
  booking,
  emailed,
  managementHref,
  copied,
  onCopy,
  onReset,
}: {
  booking: PublicBookingResponse;
  emailed: boolean;
  managementHref: string;
  copied: boolean;
  onCopy: () => void;
  onReset: () => void;
}) {
  const confirmed = booking.status.toUpperCase() === "CONFIRMED";
  const reference = `${copy.appointmentRef}${booking.appointmentId}`;

  return (
    <>
      <div className="booking-done-mark" aria-hidden="true">
        <svg viewBox="0 0 24 24" className="booking-done-check">
          <path
            d="M5 12.8 9.8 17.5 19 7"
            fill="none"
            stroke="currentColor"
            strokeWidth="2.4"
            strokeLinecap="round"
            strokeLinejoin="round"
            pathLength="1"
          />
        </svg>
      </div>

      <p className="booking-done-eyebrow">{copy.successEyebrow}</p>
      <h2 className="booking-done-title">{copy.successTitle}</h2>
      <p className="booking-done-lead">
        {emailed
          ? "Te hemos enviado la confirmación. Si necesitas cambiar algo, puedes gestionarla cuando quieras."
          : booking.message || copy.successFallback}
      </p>

      <div className="booking-done-panel">
        <div className="booking-done-grid">
          <div className="booking-done-row">
            <span className="booking-done-row-icon" aria-hidden="true">
              <FieldGlyph name="wrench" />
            </span>
            <div>
              <p className="booking-done-kicker">{copy.successService}</p>
              <p className="booking-done-value">{booking.serviceName}</p>
            </div>
          </div>
          <div className="booking-done-row">
            <span className="booking-done-row-icon" aria-hidden="true">
              <FieldGlyph name="person" />
            </span>
            <div>
              <p className="booking-done-kicker">{copy.successProfessional}</p>
              <p className="booking-done-value">{booking.professionalName}</p>
            </div>
          </div>
          <div className="booking-done-row">
            <span className="booking-done-row-icon" aria-hidden="true">
              <FieldGlyph name="calendar" />
            </span>
            <div>
              <p className="booking-done-kicker">{copy.successDateTime}</p>
              <p className="booking-done-value">{formatBookingDateTime(booking.startAt)}</p>
            </div>
          </div>
          <div className="booking-done-row">
            <span className="booking-done-row-icon" aria-hidden="true">
              <FieldGlyph name="check" />
            </span>
            <div>
              <p className="booking-done-kicker">{copy.successStatus}</p>
              <p className={confirmed ? "booking-done-badge" : "booking-done-badge is-muted"}>
                {getStatusLabel(booking.status)}
              </p>
            </div>
          </div>
        </div>

        <div className="booking-done-ref">
          <p className="booking-done-kicker">{copy.successReference}</p>
          <div className="booking-done-ref-line">
            <p className="booking-done-value">{reference}</p>
            <button
              type="button"
              className="booking-done-copy"
              onClick={onCopy}
              aria-label={copied ? "Copiado" : "Copiar referencia"}
            >
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <rect x="8" y="8" width="11" height="11" rx="2" />
                <path d="M5 15.5V6.5A1.5 1.5 0 0 1 6.5 5H15" />
              </svg>
            </button>
            {copied ? (
              <span className="booking-done-copied" role="status">
                Copiado
              </span>
            ) : null}
          </div>
        </div>
      </div>

      <button
        type="button"
        className="booking-done-ics"
        onClick={() => downloadAppointmentIcs(booking)}
      >
        <FieldGlyph name="calendar" />
        Añadir al calendario
      </button>

      <div className="booking-done-actions">
        <Link href={managementHref} className="booking-done-primary">
          <span className="booking-done-label">
            <FieldGlyph name="calendar" />
            {copy.manageCta}
          </span>
        </Link>
        <button type="button" className="booking-done-secondary" onClick={onReset}>
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M12 5v14M5 12h14" />
          </svg>
          {copy.bookAnother}
        </button>
        <Link href="/" className="booking-done-ghost">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M14 6 8 12l6 6" />
          </svg>
          <span className="booking-done-ghost-label">
            {copy.backPrefix} {business.brand.shortName}
            <span className="booking-done-underline" aria-hidden="true" />
          </span>
        </Link>
      </div>
    </>
  );
}

export default function PublicBookingPage() {
  const [
    step,
    setStep,
  ] =
    useState<BookingStep>(
      "service"
    );

  const [
    services,
    setServices,
  ] =
    useState<
      PublicCatalogService[]
    >([]);

  const [
    professionals,
    setProfessionals,
  ] =
    useState<
      PublicCatalogProfessional[]
    >([]);

  const [
    availability,
    setAvailability,
  ] =
    useState<
      DailyAvailabilityResponse | null
    >(null);

  const [
    selectedService,
    setSelectedService,
  ] =
    useState<
      PublicCatalogService | null
    >(null);

  const [
    selectedProfessional,
    setSelectedProfessional,
  ] =
    useState<
      PublicCatalogProfessional | null
    >(null);

  const [
    selectedDate,
    setSelectedDate,
  ] =
    useState("");

  const [
    selectedSlot,
    setSelectedSlot,
  ] =
    useState<
      AvailabilitySlot | null
    >(null);

  const [
    customerForm,
    setCustomerForm,
  ] =
    useState<CustomerForm>(
      INITIAL_CUSTOMER_FORM
    );

  const [
    extraValues,
    setExtraValues,
  ] =
    useState<Record<string, string>>({});

  const [summaryOpen, setSummaryOpen] = useState(false);

  const notesRef = useRef<HTMLTextAreaElement>(null);

  const [referenceCopied, setReferenceCopied] = useState(false);

  const referenceTimer = useRef<number | null>(null);

  const [
    bookingResult,
    setBookingResult,
  ] =
    useState<
      PublicBookingResponse | null
    >(null);

  const [
    loadingServices,
    setLoadingServices,
  ] =
    useState(true);

  const [
    loadingProfessionals,
    setLoadingProfessionals,
  ] =
    useState(false);

  const [
    loadingAvailability,
    setLoadingAvailability,
  ] =
    useState(false);

  const [
    creatingBooking,
    setCreatingBooking,
  ] =
    useState(false);

  const [
    error,
    setError,
  ] =
    useState<
      string | null
    >(null);

  const today =
    useMemo(
      () =>
        getTodayLocalDate(),
      []
    );

  const maximumDate =
    useMemo(
      () =>
        getMaximumDate(),
      []
    );

  /* =======================================================
     LOAD SERVICES
  ======================================================== */

  useEffect(
    () => {
      let cancelled =
        false;

      async function loadServices() {
        setLoadingServices(
          true
        );

        setError(
          null
        );

        try {
          const response =
            await fetch(
              `${API_BASE_URL}/api/v1/public/catalog/${encodeURIComponent(
                BUSINESS_SLUG
              )}/services`,
              {
                method: "GET",
                headers: {
                  Accept:
                    "application/json",
                },
                cache:
                  "no-store",
              }
            );

          if (!response.ok) {
            throw new Error(
              await readApiError(
                response,
                copy.errorLoadServices
              )
            );
          }

          const payload =
            (await response.json()) as PublicCatalogService[];

          if (!cancelled) {
            setServices(
              Array.isArray(
                payload
              )
                ? payload
                : []
            );
          }
        } catch (cause) {
          if (!cancelled) {
            setError(
              cause instanceof Error
                ? cause.message
                : copy.errorLoadServices
            );
          }
        } finally {
          if (!cancelled) {
            setLoadingServices(
              false
            );
          }
        }
      }

      void loadServices();

      return () => {
        cancelled =
          true;
      };
    },
    []
  );

  /* =======================================================
     LOAD PROFESSIONALS
  ======================================================== */

  async function selectService(
    service:
      PublicCatalogService
  ) {
    setSelectedService(
      service
    );

    setSelectedProfessional(
      null
    );

    setSelectedDate(
      ""
    );

    setSelectedSlot(
      null
    );

    setAvailability(
      null
    );

    setProfessionals(
      []
    );

    setLoadingProfessionals(
      true
    );

    setError(
      null
    );

    try {
      const response =
        await fetch(
          `${API_BASE_URL}/api/v1/public/catalog/${encodeURIComponent(
            BUSINESS_SLUG
          )}/services/${service.id}/professionals`,
          {
            method: "GET",
            headers: {
              Accept:
                "application/json",
            },
            cache:
              "no-store",
          }
        );

      if (!response.ok) {
        throw new Error(
          await readApiError(
            response,
            copy.errorLoadProfessionals
          )
        );
      }

      const payload =
        (await response.json()) as PublicCatalogProfessional[];

      setProfessionals(
        Array.isArray(
          payload
        )
          ? payload
          : []
      );

      setStep(
        "professional"
      );
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : copy.errorLoadProfessionals
      );
    } finally {
      setLoadingProfessionals(
        false
      );
    }
  }

  /* =======================================================
     SELECT PROFESSIONAL
  ======================================================== */

  function selectProfessional(
    professional:
      PublicCatalogProfessional
  ) {
    setSelectedProfessional(
      professional
    );

    setSelectedDate(
      ""
    );

    setSelectedSlot(
      null
    );

    setAvailability(
      null
    );

    setError(
      null
    );

    setStep(
      "datetime"
    );
  }

  /* =======================================================
     LOAD AVAILABILITY
  ======================================================== */

  async function loadAvailability(
    date: string
  ) {
    setSelectedDate(
      date
    );

    setSelectedSlot(
      null
    );

    setAvailability(
      null
    );

    setError(
      null
    );

    if (
      !date ||
      !selectedService ||
      !selectedProfessional
    ) {
      return;
    }

    setLoadingAvailability(
      true
    );

    try {
      const searchParams =
        new URLSearchParams({
          professionalId:
            String(
              selectedProfessional.id
            ),

          serviceId:
            String(
              selectedService.id
            ),

          date,
        });

      const response =
        await fetch(
          `${API_BASE_URL}/api/v1/public/availability-slots?${searchParams.toString()}`,
          {
            method: "GET",
            headers: {
              Accept:
                "application/json",
            },
            cache:
              "no-store",
          }
        );

      if (!response.ok) {
        throw new Error(
          await readApiError(
            response,
            copy.errorLoadAvailabilityDate
          )
        );
      }

      const payload =
        (await response.json()) as DailyAvailabilityResponse;

      setAvailability(
        payload
      );
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : copy.errorLoadAvailability
      );
    } finally {
      setLoadingAvailability(
        false
      );
    }
  }

  /* =======================================================
     CUSTOMER FORM
  ======================================================== */

  function updateCustomerField(
    field: keyof CustomerForm,
    value: string
  ) {
    setCustomerForm(
      current => ({
        ...current,
        [field]: value,
      })
    );
  }

  const hasContactMethod =
    customerForm.email.trim().length > 0 ||
    customerForm.phone.trim().length > 0;

  const formReady =
    customerForm.firstName.trim().length > 0 &&
    hasContactMethod;

  useEffect(() => {
    const node = notesRef.current;

    if (!node) {
      return;
    }

    node.style.height = "auto";
    node.style.height = `${node.scrollHeight}px`;
  }, [customerForm.notes, step]);

  /* =======================================================
     CREATE BOOKING
  ======================================================== */

  async function createBooking(
    event:
      FormEvent<HTMLFormElement>
  ) {
    event.preventDefault();

    setError(
      null
    );

    if (
      !selectedService ||
      !selectedProfessional ||
      !selectedSlot ||
      !availability
    ) {
      setError(
        copy.errorMissingSelection
      );

      return;
    }

    if (
      !customerForm.firstName.trim()
    ) {
      setError(
        copy.errorFirstName
      );

      return;
    }

    if (
      !hasContactMethod
    ) {
      setError(
        copy.errorContact
      );

      return;
    }

    setCreatingBooking(
      true
    );

    try {
      const response =
        await fetch(
          `${API_BASE_URL}/api/v1/public/bookings`,
          {
            method: "POST",

            headers: {
              Accept:
                "application/json",

              "Content-Type":
                "application/json",
            },

            body:
              JSON.stringify({
                businessId:
                  availability.businessId,

                professionalId:
                  selectedProfessional.id,

                serviceId:
                  selectedService.id,

                startAt:
                  selectedSlot.startAt,

                firstName:
                  customerForm.firstName.trim(),

                lastName:
                  customerForm.lastName.trim() ||
                  null,

                email:
                  customerForm.email.trim() ||
                  null,

                phone:
                  customerForm.phone.trim() ||
                  null,

                notes:
                  composeBookingNotes(
                    customerForm.notes,
                    extraValues,
                    extraFields,
                  ),
              }),
          }
        );

      if (!response.ok) {
        throw new Error(
          await readApiError(
            response,
            copy.errorCreate
          )
        );
      }

      const payload =
        (await response.json()) as PublicBookingResponse;

      setBookingResult(
        payload
      );

      /*
       * Credencial secreta para el futuro
       * flujo público de gestión de la cita.
       *
       * Nunca se muestra ni se registra.
       */
      if (
        payload.managementToken &&
        typeof window !==
          "undefined"
      ) {
        try {
          sessionStorage.setItem(
            `wacds_booking_management_${payload.appointmentId}`,
            payload.managementToken
          );
        } catch {
          /*
           * La reserva ya está creada en backend.
           * Un fallo de sessionStorage no debe convertir
           * una reserva válida en un error visual.
           *
           * El CTA de gestión también transporta el token
           * mediante fragment (#token=...), que la página
           * de gestión captura y elimina de la URL.
           */
        }
      }

      setStep(
        "success"
      );

      window.scrollTo({
        top: 0,
        behavior: "smooth",
      });
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : copy.errorCreate
      );
    } finally {
      setCreatingBooking(
        false
      );
    }
  }

  /* =======================================================
     RESET
  ======================================================== */

  function resetBooking() {
    setStep(
      "service"
    );

    setSelectedService(
      null
    );

    setSelectedProfessional(
      null
    );

    setSelectedDate(
      ""
    );

    setSelectedSlot(
      null
    );

    setProfessionals(
      []
    );

    setAvailability(
      null
    );

    setCustomerForm(
      INITIAL_CUSTOMER_FORM
    );

    setReferenceCopied(false);

    setExtraValues(
      {}
    );

    setBookingResult(
      null
    );

    setError(
      null
    );
  }

  /* =======================================================
     STEP STATE
  ======================================================== */

  const stepOrder:
    BookingStep[] = [
      "service",
      "professional",
      "datetime",
      "customer",
    ];

  const currentStepIndex =
    step === "success"
      ? stepOrder.length
      : stepOrder.indexOf(
          step
        );

  const taller =
    business.theme === "taller";

  const sequence =
    copy.heroSequence;

  const phase =
    step === "success"
      ? (copy.stepFrames?.[
          copy.stepFrames.length - 1
        ] ?? 1)
      : (copy.stepFrames?.[
          currentStepIndex
        ] ?? 0);

  const chosenChips: BookingChip[] = [];

  if (
    currentStepIndex > 0 &&
    selectedService
  ) {
    chosenChips.push({
      kind: "service",
      stepIndex: 0,
      text: selectedService.name,
      price: formatMoney(
        Number(selectedService.price),
        selectedService.currency
      ),
    });
  }

  if (
    currentStepIndex > 1 &&
    selectedProfessional
  ) {
    chosenChips.push({
      kind: "professional",
      stepIndex: 1,
      text: selectedProfessional.displayName,
    });
  }

  if (
    currentStepIndex > 2 &&
    selectedDate &&
    selectedSlot
  ) {
    chosenChips.push({
      kind: "datetime",
      stepIndex: 2,
      text: formatChipWhen(
        selectedDate,
        selectedSlot.startTime
      ),
    });
  }

  const managementHref =
    bookingResult
      ? bookingResult.managementToken
        ? `/reservar/gestionar?appointmentId=${bookingResult.appointmentId}#token=${encodeURIComponent(
            bookingResult.managementToken
          )}`
        : `/reservar/gestionar?appointmentId=${bookingResult.appointmentId}`
      : "/reservar/gestionar";

  async function copyAppointmentReference() {
    if (!bookingResult) {
      return;
    }

    const text = `${copy.appointmentRef}${bookingResult.appointmentId}`;
    let copied = false;

    try {
      await navigator.clipboard.writeText(text);
      copied = true;
    } catch {
      try {
        const area = document.createElement("textarea");
        area.value = text;
        area.setAttribute("readonly", "");
        area.style.position = "fixed";
        area.style.left = "-9999px";
        document.body.appendChild(area);
        area.select();
        copied = document.execCommand("copy");
        area.remove();
      } catch {
        copied = false;
      }
    }

    if (!copied) {
      return;
    }

    setReferenceCopied(true);
      if (referenceTimer.current !== null) {
        window.clearTimeout(referenceTimer.current);
      }
      referenceTimer.current = window.setTimeout(() => {
      setReferenceCopied(false);
    }, 2000);
  }

  return (
    <div className="min-h-screen bg-(--canvas) text-(--p-2d2723)">
      <SiteHeader />

      <main>
        {/* ===================================================
            HERO
        ==================================================== */}

        <BookingHero
          eyebrow={copy.eyebrow}
          titleLead={copy.titleLead}
          titleAccent={copy.titleAccent}
          description={copy.description}
          backPrefix={copy.backPrefix}
          brandName={business.brand.name}
          sequence={sequence}
          stepFrames={copy.stepFrames}
          mode={step === "service" ? "loop" : "hold"}
          target={phase}
        />

        {/* ===================================================
            BOOKING
        ==================================================== */}

        <section className={taller && sequence && step !== "success" ? "relative z-[2] mx-auto w-full max-w-[1180px] px-5 pb-10 sm:px-8 sm:pb-14 lg:px-12 lg:pb-16" : "mx-auto w-full max-w-[1180px] px-5 py-10 sm:px-8 sm:py-14 lg:px-12 lg:py-16"}>
          {step !==
            "success" &&
            taller &&
            sequence && (
              <BookingStepCard
                labels={copy.steps}
                currentStepIndex={currentStepIndex}
                sequence={sequence}
                stepFrames={copy.stepFrames}
                phaseLabels={copy.stepPhaseLabels}
                target={phase}
                chips={chosenChips}
                onStepSelect={(index) => {
                  const next = stepOrder[index];
                  if (!next || index >= currentStepIndex) return;
                  setStep(next);
                  setError(null);
                }}
              />
            )}

          {step !==
            "success" &&
            !(taller && sequence) && (
            <div className="mb-8 overflow-x-auto">
              <div className="flex min-w-[650px] items-center">
                {copy.steps.map(
                  (
                    label,
                    index
                  ) => {
                    const active =
                      currentStepIndex ===
                      index;

                    const completed =
                      currentStepIndex >
                      index;

                    return (
                      <div
                        key={
                          label
                        }
                        className="flex flex-1 items-center"
                      >
                        <div className="flex items-center gap-3">
                          <StepNumber
                            number={
                              index + 1
                            }
                            active={
                              active
                            }
                            completed={
                              completed
                            }
                          />

                          <span
                            className={[
                              "text-xs font-semibold uppercase tracking-[0.13em]",
                              active ||
                              completed
                                ? "text-(--p-39302a)"
                                : "text-(--p-9a8d84)",
                            ].join(" ")}
                          >
                            {
                              label
                            }
                          </span>
                        </div>

                        {index <
                          3 && (
                          <div className="mx-5 h-px flex-1 bg-(--p-3b332e)/10" />
                        )}
                      </div>
                    );
                  }
                )}
              </div>
            </div>
          )}

          {/* ERROR */}

          {error &&
            !(
              taller &&
              step === "customer" &&
              (error === copy.errorFirstName ||
                error === copy.errorContact)
            ) && (
            <div
              role="alert"
              className="mb-6 rounded-[1.25rem] border border-red-900/10 bg-red-50 px-5 py-4 text-sm leading-6 text-red-800"
            >
              {error}
            </div>
          )}

          {/* =================================================
              STEP 1 · SERVICES
          ================================================== */}

          {step ===
            "service" && (
            <div>
              <StepHeading
                taller={taller}
                index={0}
                eyebrow={copy.stepHeadings[0]}
                title={copy.chooseServiceTitle}
                description={copy.chooseServiceDescription}
                descriptionClass="mt-3 max-w-[620px] text-sm leading-6 text-(--p-756960)"
              />

              {loadingServices ? (
                <div className="flex min-h-[240px] items-center justify-center rounded-[1.75rem] border border-(--p-3b332e)/10 bg-white/45">
                  <div className="flex items-center gap-3 text-sm text-(--p-6f6259)">
                    <LoadingSpinner />

                    {copy.loadingServices}
                  </div>
                </div>
              ) : services.length ===
                0 ? (
                <div className="rounded-[1.75rem] border border-(--p-3b332e)/10 bg-white/50 p-8">
                  <h3 className="font-serif text-2xl text-(--heading)">
                    {copy.emptyServicesTitle}
                  </h3>

                  <p className="mt-3 max-w-[560px] text-sm leading-6 text-(--p-756960)">
                    {copy.emptyServicesBody}
                  </p>
                </div>
              ) : (
                <div className="grid gap-4 md:grid-cols-2">
                  {services.map(
                    (service, index) => {
                      const icon = serviceIcon(service);
                      const button = (
                      <button
                        key={
                          taller ? undefined : service.id
                        }
                        type="button"
                        disabled={
                          loadingProfessionals
                        }
                        onClick={() =>
                          void selectService(
                            service
                          )
                        }
                        className={
                          taller
                            ? "group h-full rounded-[1.5rem] border border-(--inverse)/10 bg-white p-6 text-left shadow-[0_12px_35px_rgba(28,25,23,0.05)] transition-all duration-300 hover:-translate-y-1 hover:border-[#f97316] hover:shadow-[0_20px_45px_rgba(28,25,23,0.12)] disabled:pointer-events-none disabled:opacity-60"
                            : "group rounded-[1.5rem] border border-(--p-3b332e)/10 bg-white/55 p-6 text-left shadow-[0_12px_35px_rgba(57,47,41,0.05)] transition-all duration-300 hover:-translate-y-1 hover:border-(--accent)/30 hover:bg-white hover:shadow-[0_20px_45px_rgba(57,47,41,0.10)] disabled:pointer-events-none disabled:opacity-60"
                        }
                      >
                        <div className="flex items-start justify-between gap-5">
                          <div>
                            {icon ? (
                              <span className="mb-4 flex h-11 w-11 items-center justify-center rounded-full border border-(--inverse)/10 text-[#f97316]">
                                <TradeIcon name={icon} className="h-5 w-5" />
                              </span>
                            ) : null}
                            <h3 className="font-serif text-2xl text-(--p-342c27)">
                              {
                                service.name
                              }
                            </h3>

                            {service.description && (
                              <p className="mt-3 line-clamp-3 text-sm leading-6 text-(--p-71665e)">
                                {
                                  service.description
                                }
                              </p>
                            )}
                          </div>

                          <span className={taller ? "flex h-10 w-10 shrink-0 items-center justify-center rounded-full border border-(--inverse)/10 bg-(--canvas) text-lg text-(--ink) transition-transform duration-300 group-hover:translate-x-1 group-hover:border-[#f97316] group-hover:text-[#f97316]" : "flex h-10 w-10 shrink-0 items-center justify-center rounded-full border border-(--p-3b332e)/10 bg-(--p-f3ece5) text-lg text-(--p-5a493f) transition-transform duration-300 group-hover:translate-x-1"}>
                            →
                          </span>
                        </div>

                        {taller ? (
                          <div className="mt-6 flex items-end justify-between gap-4 border-t border-(--inverse)/10 pt-4">
                            <span className="text-[11px] font-semibold uppercase tracking-[0.16em] text-(--ink-muted)">
                              {service.durationMinutes} {copy.minuteShort}
                            </span>
                            <strong className="font-serif text-3xl font-normal text-(--ink)">
                              {formatMoney(
                                Number(service.price),
                                service.currency
                              )}
                            </strong>
                          </div>
                        ) : (
                        <div className="mt-6 flex flex-wrap gap-2">
                          <span className="rounded-full bg-(--surface-muted) px-3 py-1.5 text-xs font-medium text-(--p-705d50)">
                            {
                              service.durationMinutes
                            }{" "}
                            {copy.minuteShort}
                          </span>

                          <span className="rounded-full bg-(--heading) px-3 py-1.5 text-xs font-semibold text-white">
                            {formatMoney(
                              Number(
                                service.price
                              ),
                              service.currency
                            )}
                          </span>
                        </div>
                        )}
                      </button>
                      );

                      if (!taller) return button;

                      return (
                        <Reveal
                          key={service.id}
                          variant="fade-up"
                          delay={index * 80}
                          className="h-full"
                        >
                          {button}
                        </Reveal>
                      );
                    }
                  )}
                </div>
              )}
            </div>
          )}

          {/* =================================================
              STEP 2 · PROFESSIONALS
          ================================================== */}

          {step ===
            "professional" &&
            selectedService && (
              <div>
                {!taller && (
                <button
                  type="button"
                  onClick={() => {
                    setStep(
                      "service"
                    );

                    setError(
                      null
                    );
                  }}
                  className="mb-7 text-sm font-medium text-(--p-7a6759) transition hover:text-(--p-342d28)"
                >
                  {copy.changeService}
                </button>
                )}

                <StepHeading
                  taller={taller}
                  index={1}
                  eyebrow={copy.stepHeadings[1]}
                  title={copy.chooseProfessionalTitle}
                  description={copy.chooseProfessionalDescription}
                  changeLabel={copy.changeService}
                  onChange={
                    taller
                      ? () => {
                          setStep("service");
                          setError(null);
                        }
                      : undefined
                  }
                />

                {loadingProfessionals ? (
                  <div className="flex min-h-[220px] items-center justify-center rounded-[1.75rem] border border-(--p-3b332e)/10 bg-white/45">
                    <div className="flex items-center gap-3 text-sm text-(--p-6f6259)">
                      <LoadingSpinner />

                      {copy.loadingProfessionals}
                    </div>
                  </div>
                ) : professionals.length ===
                  0 ? (
                  <div className="rounded-[1.75rem] border border-(--p-3b332e)/10 bg-white/50 p-8">
                    <h3 className="font-serif text-2xl text-(--heading)">
                      {copy.emptyProfessionalsTitle}
                    </h3>

                    <p className="mt-3 text-sm leading-6 text-(--p-756960)">
                      {copy.emptyProfessionalsBody}
                    </p>
                  </div>
                ) : (
                  <div className="grid gap-4 md:grid-cols-2">
                    {professionals.map(
                      professional => (
                        <button
                          key={
                            professional.id
                          }
                          type="button"
                          onClick={() =>
                            selectProfessional(
                              professional
                            )
                          }
                          className="group rounded-[1.5rem] border border-(--p-3b332e)/10 bg-white/55 p-6 text-left shadow-[0_12px_35px_rgba(57,47,41,0.05)] transition-all duration-300 hover:-translate-y-1 hover:border-(--accent)/30 hover:bg-white"
                        >
                          <div className="flex items-start gap-4">
                            <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-(--p-e8dbcf) font-serif text-lg text-(--p-735e50)">
                              {professional.displayName
                                .charAt(0)
                                .toUpperCase()}
                            </span>

                            <div className="min-w-0 flex-1">
                              <h3 className="font-serif text-2xl text-(--p-342c27)">
                                {
                                  professional.displayName
                                }
                              </h3>

                              {professional.bio && (
                                <p className="mt-2 text-sm leading-6 text-(--p-71665e)">
                                  {
                                    professional.bio
                                  }
                                </p>
                              )}
                            </div>

                            <span className="text-lg text-(--p-7b6759) transition-transform duration-300 group-hover:translate-x-1">
                              →
                            </span>
                          </div>
                        </button>
                      )
                    )}
                  </div>
                )}
              </div>
            )}

          {/* =================================================
              STEP 3 · DATE + TIME
          ================================================== */}

          {step ===
            "datetime" &&
            selectedService &&
            selectedProfessional && (
              <div>
                {!taller && (
                <button
                  type="button"
                  onClick={() => {
                    setStep(
                      "professional"
                    );

                    setError(
                      null
                    );
                  }}
                  className="mb-7 text-sm font-medium text-(--p-7a6759) transition hover:text-(--p-342d28)"
                >
                  {copy.changeProfessional}
                </button>
                )}

                <StepHeading
                  taller={taller}
                  index={2}
                  eyebrow={copy.stepHeadings[2]}
                  title={copy.dateTimeTitle}
                  description={
                    <>
                      <span className="block">
                        {selectedService.name} {copy.withProfessional}{" "}
                        {selectedProfessional.displayName} ·{" "}
                        {selectedService.durationMinutes} {copy.minuteShort}.
                      </span>
                      <span className="mt-1 block">{copy.dateTimePrompt}</span>
                    </>
                  }
                  changeLabel={copy.changeProfessional}
                  onChange={
                    taller
                      ? () => {
                          setStep("professional");
                          setError(null);
                        }
                      : undefined
                  }
                />

                <div className="grid items-stretch gap-6 md:grid-cols-[0.95fr_1.05fr]">
                  {/* CALENDAR */}

                  <div className="rounded-[1.75rem] border border-(--p-3b332e)/10 bg-white/60 p-5 shadow-[0_14px_40px_rgba(57,47,41,0.04)] sm:p-6">
                    <div>
                      <p className="text-[10px] font-semibold uppercase tracking-[0.18em] text-(--p-79685c)">
                        {copy.chooseDateLabel}
                      </p>

                      <p className="mt-2 text-xs leading-5 text-(--p-96877d)">
                        {copy.chooseDateHint}
                      </p>
                    </div>

                    <CalendarDatePicker
                      value={
                        selectedDate
                      }
                      minDate={
                        today
                      }
                      maxDate={
                        maximumDate
                      }
                      disabled={
                        loadingAvailability
                      }
                      onChange={
                        date =>
                          void loadAvailability(
                            date
                          )
                      }
                    />
                  </div>

                  {/* TIMES */}

                  <div className="flex flex-col rounded-[1.75rem] border border-(--p-3b332e)/10 bg-white/60 p-5 shadow-[0_14px_40px_rgba(57,47,41,0.04)] sm:p-6 md:h-0 md:min-h-full md:overflow-hidden">
                    <div className="flex shrink-0 items-start justify-between gap-4">
                      <div>
                        <p className="text-[10px] font-semibold uppercase tracking-[0.18em] text-(--p-79685c)">
                          {copy.timesLabel}
                        </p>

                        {selectedDate && (
                          <p className="mt-2 text-xs leading-5 text-(--p-96877d)">
                            {formatDate(
                              selectedDate
                            )}
                          </p>
                        )}
                      </div>

                      {availability?.available &&
                        availability.totalSlots >
                          0 && (
                          <span className="rounded-full bg-(--surface-muted) px-3 py-1.5 text-xs font-medium text-(--p-755f51)">
                            {
                              availability.totalSlots
                            }{" "}
                            {availability.totalSlots === 1
                              ? copy.slotSingular
                              : copy.slotPlural}
                          </span>
                        )}
                    </div>

                    {!selectedDate ? (
                      <div className="mt-7 rounded-[1.25rem] bg-(--p-f5efe9) px-5 py-6">
                        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-(--p-e8ddd2) text-(--p-7c6758)">
                          ◷
                        </div>

                        <p className="mt-4 text-sm font-medium text-(--p-554940)">
                          {copy.selectDateTitle}
                        </p>

                        <p className="mt-1 max-w-[320px] text-xs leading-5 text-(--p-8a7d74)">
                          {copy.selectDateHint}
                        </p>
                      </div>
                    ) : loadingAvailability ? (
                      <div className="mt-8 flex min-h-[120px] items-center justify-center">
                        <div className="flex items-center gap-3 text-sm text-(--p-6f6259)">
                          <LoadingSpinner />

                          {copy.loadingAvailability}
                        </div>
                      </div>
                    ) : !availability ||
                      !availability.available ||
                      availability.slots.length ===
                        0 ? (
                      <div className="mt-7 rounded-[1.25rem] bg-(--p-f2ebe4) px-5 py-6">
                        <p className="text-sm font-medium text-(--p-584b42)">
                          {copy.emptyTimesTitle}
                        </p>

                        <p className="mt-2 text-xs leading-5 text-(--p-84766d)">
                          {copy.emptyTimesHint}
                        </p>
                      </div>
                    ) : (
                      <div className="mt-6 flex flex-col md:min-h-0 md:flex-1">
                        <p className="shrink-0 text-xs leading-5 text-(--p-85776d)">
                          {copy.selectTimeHint}
                        </p>

                        {selectedSlot && (
                          <div className="mt-4 flex shrink-0 items-center justify-between gap-4 rounded-[1.1rem] bg-(--surface-muted) px-4 py-3.5">
                            <div>
                              <p className="text-[9px] font-semibold uppercase tracking-[0.14em] text-(--p-8c7565)">
                                {copy.selectedTimeLabel}
                              </p>

                              <p className="mt-1 text-sm font-semibold text-(--p-42372f)">
                                {formatTime(
                                  selectedSlot.startTime
                                )}{" "}
                                –{" "}
                                {formatTime(
                                  selectedSlot.endTime
                                )}
                              </p>
                            </div>

                            <span className="flex h-8 w-8 items-center justify-center rounded-full bg-(--heading) text-xs text-white">
                              ✓
                            </span>
                          </div>
                        )}

                        <TimesScroller>
                          {(["Mañana", "Tarde"] as const).map((label) => {
                            const group = availability.slots.filter((slot) =>
                              label === "Mañana"
                                ? slotIsMorning(slot.startTime)
                                : !slotIsMorning(slot.startTime)
                            );

                            if (group.length === 0) return null;

                            return (
                              <div key={label}>
                                <p className="booking-times-heading mb-2 text-[11px] font-medium uppercase tracking-[0.12em] text-(--ink-muted) md:sticky md:top-0 md:z-[1] md:py-2">
                                  {label}
                                </p>
                                <TimeSlotGrid
                                  slots={group}
                                  selectedStartAt={selectedSlot?.startAt ?? null}
                                  taller={taller}
                                  onSelect={(slot) => {
                                    setSelectedSlot(slot);
                                    setError(null);
                                  }}
                                />
                              </div>
                            );
                          })}
                        </TimesScroller>
                      </div>
                    )}
                  </div>
                </div>

                <div className="mt-8 flex justify-end">
                  <button
                    type="button"
                    disabled={
                      !selectedSlot
                    }
                    onClick={() =>
                      setStep(
                        "customer"
                      )
                    }
                    className={[
                      "inline-flex h-13 items-center justify-center rounded-full px-8 text-xs font-semibold uppercase tracking-[0.16em] transition-[background-color,color,box-shadow,transform] duration-300",
                      selectedSlot
                        ? "bg-(--button-bg) text-(--button-text) shadow-[0_10px_28px_rgba(48,41,36,0.16)] hover:-translate-y-0.5 hover:brightness-95"
                        : "bg-(--inverse)/30 text-(--inverse) shadow-none",
                    ].join(" ")}
                    style={
                      selectedSlot
                        ? undefined
                        : { cursor: "not-allowed" }
                    }
                  >
                    {copy.continueLabel}
                  </button>
                </div>
              </div>
            )}

          {/* =================================================
              STEP 4 · CUSTOMER
          ================================================== */}

          {step ===
            "customer" &&
            selectedService &&
            selectedProfessional &&
            selectedSlot &&
            availability && (
              <div>
                {!taller && (
                <button
                  type="button"
                  onClick={() => {
                    setStep(
                      "datetime"
                    );

                    setError(
                      null
                    );
                  }}
                  className="mb-7 text-sm font-medium text-(--p-7a6759) transition hover:text-(--p-342d28)"
                >
                  {copy.changeDateTime}
                </button>
                )}

                <div className="grid min-w-0 grid-cols-1 items-start gap-8 md:grid-cols-[minmax(0,1fr)_360px] md:gap-10">
                  <div className="order-2 min-w-0 md:order-1">
                    <StepHeading
                      taller={taller}
                      index={3}
                      eyebrow={copy.stepHeadings[3]}
                      title={copy.customerTitle}
                      description={copy.customerDescription}
                      changeLabel={copy.changeDateTime}
                      onChange={
                        taller
                          ? () => {
                              setStep("datetime");
                              setError(null);
                            }
                          : undefined
                      }
                    />

                    {taller ? (
                    <form onSubmit={createBooking} className="booking-form-card">
                      <section className="booking-section">
                        <h3 className="booking-section-title">
                          <span>01</span> · Sobre ti
                        </h3>
                        <div className="booking-field-grid cols-2">
                          <PremiumField
                            id="booking-first-name"
                            label={copy.firstNameLabel}
                            icon="person"
                            required
                            maxLength={100}
                            autoComplete="given-name"
                            value={customerForm.firstName}
                            valid={customerForm.firstName.trim().length > 0 && error !== copy.errorFirstName}
                            invalid={error === copy.errorFirstName}
                            errorId="booking-first-name-error"
                            errorMessage={copy.errorFirstName}
                            onChange={(value) => {
                              updateCustomerField("firstName", value);
                              if (error === copy.errorFirstName && value.trim()) {
                                setError(null);
                              }
                            }}
                            onInvalid={(event) => {
                              event.preventDefault();
                              setError(copy.errorFirstName);
                            }}
                          />
                          <PremiumField
                            id="booking-last-name"
                            label={copy.lastNameLabel}
                            icon="person"
                            maxLength={150}
                            autoComplete="family-name"
                            value={customerForm.lastName}
                            valid={customerForm.lastName.trim().length > 0}
                            invalid={false}
                            onChange={(value) => updateCustomerField("lastName", value)}
                          />
                        </div>
                      </section>

                      <section className="booking-section">
                        <h3 className="booking-section-title">
                          <span>02</span> · Cómo te avisamos
                        </h3>
                        <div className="booking-field-grid cols-2">
                          <PremiumField
                            id="booking-email"
                            label={copy.emailLabel}
                            icon="mail"
                            type="email"
                            maxLength={180}
                            autoComplete="email"
                            inputMode="email"
                            value={customerForm.email}
                            valid={customerForm.email.trim().length > 0 && error !== copy.errorContact}
                            invalid={error === copy.errorContact}
                            errorId="booking-contact-error"
                            hintId="booking-contact-hint"
                            onChange={(value) => {
                              updateCustomerField("email", value);
                              if (
                                error === copy.errorContact &&
                                (value.trim() || customerForm.phone.trim())
                              ) {
                                setError(null);
                              }
                            }}
                          />
                          <PremiumField
                            id="booking-phone"
                            label={copy.phoneLabel}
                            icon="phone"
                            type="tel"
                            maxLength={40}
                            autoComplete="tel"
                            inputMode="tel"
                            value={customerForm.phone}
                            valid={customerForm.phone.trim().length > 0 && error !== copy.errorContact}
                            invalid={error === copy.errorContact}
                            errorId="booking-contact-error"
                            hintId="booking-contact-hint"
                            onChange={(value) => {
                              updateCustomerField("phone", value);
                              if (
                                error === copy.errorContact &&
                                (value.trim() || customerForm.email.trim())
                              ) {
                                setError(null);
                              }
                            }}
                          />
                        </div>
                        <p id="booking-contact-hint" className="booking-hint">
                          {copy.contactHint}
                        </p>
                        {error === copy.errorContact ? (
                          <p id="booking-contact-error" role="alert" className="booking-field-error is-shown">
                            {copy.errorContact}
                          </p>
                        ) : null}
                      </section>

                      <section className="booking-section">
                        <h3 className="booking-section-title">
                          <span>03</span> · Tu coche
                        </h3>
                        {extraFields.length > 0 ? (
                          <div className="booking-field-grid cols-2">
                            {extraFields.map((field) => (
                              <PremiumField
                                key={field.id}
                                id={`booking-extra-${field.id}`}
                                label={field.label}
                                icon="car"
                                plate={field.id === "plate"}
                                maxLength={field.id === "plate" ? 12 : 80}
                                autoComplete="off"
                                value={extraValues[field.id] ?? ""}
                                valid={(extraValues[field.id] ?? "").trim().length > 0}
                                invalid={false}
                                onChange={(value) =>
                                  setExtraValues((current) => ({
                                    ...current,
                                    [field.id]: value,
                                  }))
                                }
                              />
                            ))}
                          </div>
                        ) : null}
                        <div className="booking-chips">
                          {NOTE_SUGGESTIONS.map((suggestion) => (
                            <button
                              key={suggestion}
                              type="button"
                              className="booking-chip"
                              onClick={() => {
                                setCustomerForm((current) => {
                                  if (current.notes.includes(suggestion)) {
                                    return current;
                                  }
                                  const budget = extraNotesBudget(extraValues, extraFields);
                                  const next = current.notes.trim()
                                    ? `${current.notes.replace(/\s+$/, "")} · ${suggestion}`
                                    : suggestion;
                                  return { ...current, notes: next.slice(0, budget) };
                                });
                              }}
                            >
                              {suggestion}
                            </button>
                          ))}
                        </div>
                        <PremiumField
                          id="booking-notes"
                          label={copy.notesLabel}
                          icon="note"
                          multiline
                          notesRef={notesRef}
                          maxLength={extraNotesBudget(extraValues, extraFields)}
                          value={customerForm.notes}
                          valid={customerForm.notes.trim().length > 0}
                          invalid={false}
                          onChange={(value) => updateCustomerField("notes", value)}
                        />
                      </section>

                      <button
                        type="submit"
                        disabled={creatingBooking}
                        className={[
                          "booking-submit",
                          creatingBooking ? "is-busy" : formReady ? "is-ready" : "is-pending",
                        ].join(" ")}
                        aria-busy={creatingBooking || undefined}
                      >
                        <span className="booking-submit-label">
                          {creatingBooking ? <LoadingSpinner /> : null}
                          {creatingBooking ? "Reservando…" : copy.submit}
                          {creatingBooking ? null : (
                            <svg
                              viewBox="0 0 24 24"
                              aria-hidden="true"
                              className="h-4 w-4"
                              fill="none"
                              stroke="currentColor"
                              strokeWidth="1.8"
                              strokeLinecap="round"
                              strokeLinejoin="round"
                            >
                              <path d="M5 12h14M13 6l6 6-6 6" />
                            </svg>
                          )}
                        </span>
                      </button>
                      <p className="booking-legal">
                        Al confirmar aceptas la <Link href="/privacidad">política de privacidad</Link>.
                      </p>
                    </form>
                    ) : (
                    <form
                      onSubmit={
                        createBooking
                      }
                      className="space-y-5"
                    >
                      <div className="grid gap-5 sm:grid-cols-2">
                        <label className="block">
                          <span className="text-xs font-semibold text-(--p-665950)">
                            {copy.firstNameLabel}
                          </span>

                          <input
                            type="text"
                            required
                            maxLength={100}
                            autoComplete="given-name"
                            value={
                              customerForm.firstName
                            }
                            onChange={
                              event =>
                                updateCustomerField(
                                  "firstName",
                                  event.target.value
                                )
                            }
                            className="mt-2 h-12 w-full rounded-xl border border-(--p-3b332e)/15 bg-white px-4 text-sm outline-none transition focus:border-(--accent)/50 focus:ring-2 focus:ring-(--accent)/10"
                          />
                        </label>

                        <label className="block">
                          <span className="text-xs font-semibold text-(--p-665950)">
                            {copy.lastNameLabel}
                          </span>

                          <input
                            type="text"
                            maxLength={150}
                            autoComplete="family-name"
                            value={
                              customerForm.lastName
                            }
                            onChange={
                              event =>
                                updateCustomerField(
                                  "lastName",
                                  event.target.value
                                )
                            }
                            className="mt-2 h-12 w-full rounded-xl border border-(--p-3b332e)/15 bg-white px-4 text-sm outline-none transition focus:border-(--accent)/50 focus:ring-2 focus:ring-(--accent)/10"
                          />
                        </label>
                      </div>

                      <div className="grid gap-5 sm:grid-cols-2">
                        <label className="block">
                          <span className="text-xs font-semibold text-(--p-665950)">
                            {copy.emailLabel}
                          </span>

                          <input
                            type="email"
                            maxLength={180}
                            autoComplete="email"
                            value={
                              customerForm.email
                            }
                            onChange={
                              event =>
                                updateCustomerField(
                                  "email",
                                  event.target.value
                                )
                            }
                            className="mt-2 h-12 w-full rounded-xl border border-(--p-3b332e)/15 bg-white px-4 text-sm outline-none transition focus:border-(--accent)/50 focus:ring-2 focus:ring-(--accent)/10"
                          />
                        </label>

                        <label className="block">
                          <span className="text-xs font-semibold text-(--p-665950)">
                            {copy.phoneLabel}
                          </span>

                          <input
                            type="tel"
                            maxLength={40}
                            autoComplete="tel"
                            inputMode="tel"
                            value={
                              customerForm.phone
                            }
                            onChange={
                              event =>
                                updateCustomerField(
                                  "phone",
                                  event.target.value
                                )
                            }
                            className="mt-2 h-12 w-full rounded-xl border border-(--p-3b332e)/15 bg-white px-4 text-sm outline-none transition focus:border-(--accent)/50 focus:ring-2 focus:ring-(--accent)/10"
                          />
                        </label>
                      </div>

                      <p className="-mt-1 text-xs leading-5 text-(--p-8a7d74)">
                        {copy.contactHint}
                      </p>

                      {extraFields.length > 0 && (
                        <div className="grid gap-5 sm:grid-cols-2">
                          {extraFields.map((field) => (
                            <label
                              key={field.id}
                              className="block"
                            >
                              <span className="text-xs font-semibold text-(--p-665950)">
                                {field.label}
                              </span>

                              <input
                                type="text"
                                maxLength={field.id === "plate" ? 12 : 80}
                                autoComplete="off"
                                value={extraValues[field.id] ?? ""}
                                onChange={(event) =>
                                  setExtraValues((current) => ({
                                    ...current,
                                    [field.id]: event.target.value,
                                  }))
                                }
                                placeholder={field.placeholder}
                                className="mt-2 h-12 w-full rounded-xl border border-(--p-3b332e)/15 bg-white px-4 text-sm outline-none transition focus:border-(--accent)/50 focus:ring-2 focus:ring-(--accent)/10"
                              />
                            </label>
                          ))}
                        </div>
                      )}

                      <label className="block">
                        <span className="text-xs font-semibold text-(--p-665950)">
                          {copy.notesLabel}
                        </span>

                        <textarea
                          rows={4}
                          maxLength={extraNotesBudget(extraValues, extraFields)}
                          value={
                            customerForm.notes
                          }
                          onChange={
                            event =>
                              updateCustomerField(
                                "notes",
                                event.target.value
                              )
                          }
                          placeholder={copy.notesPlaceholder}
                          className="mt-2 w-full resize-none rounded-xl border border-(--p-3b332e)/15 bg-white px-4 py-3 text-sm leading-6 outline-none transition focus:border-(--accent)/50 focus:ring-2 focus:ring-(--accent)/10"
                        />
                      </label>

                      <button
                        type="submit"
                        disabled={
                          creatingBooking ||
                          !customerForm.firstName.trim() ||
                          !hasContactMethod
                        }
                        className="inline-flex h-14 w-full items-center justify-center gap-3 rounded-full bg-(--p-302924) px-8 text-xs font-semibold uppercase tracking-[0.17em] text-white shadow-[0_12px_30px_rgba(48,41,36,0.18)] transition hover:bg-(--p-4b3d34) disabled:cursor-not-allowed disabled:opacity-40 sm:w-auto"
                      >
                        {creatingBooking && (
                          <LoadingSpinner />
                        )}

                        {creatingBooking
                          ? copy.submitting
                          : copy.submit}
                      </button>
                    </form>
                    )}
                  </div>

                  {/* SUMMARY */}

                  <aside className="booking-summary-scroll order-1 min-w-0 md:sticky md:top-[120px] md:order-2 md:max-h-[calc(100dvh-140px)] md:self-start md:overflow-y-auto">
                    <button
                      type="button"
                      className="mb-3 flex w-full items-center justify-between rounded-2xl border border-(--p-3b332e)/10 bg-white/80 px-4 py-3 text-(--heading) md:hidden"
                      style={{ fontSize: "14px", fontWeight: 600 }}
                      aria-expanded={summaryOpen}
                      aria-controls="booking-summary-panel"
                      onClick={() => setSummaryOpen((open) => !open)}
                    >
                      Ver resumen de tu cita
                      <svg
                        viewBox="0 0 24 24"
                        aria-hidden="true"
                        className={["h-4 w-4 transition-transform", summaryOpen ? "rotate-180" : ""].join(" ")}
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="1.8"
                        strokeLinecap="round"
                      >
                        <path d="m6 9 6 6 6-6" />
                      </svg>
                    </button>
                    <div id="booking-summary-panel" className={summaryOpen ? "block" : "hidden md:block"}>
                    {taller ? (
                      <div className="booking-summary">
                        {sequence ? (
                          <div className="booking-summary-thumb">
                            <BookingCanvas
                              sequence={sequence}
                              stepFrames={copy.stepFrames}
                              mode="hold"
                              immediate
                              target={phase}
                              className="absolute inset-0 h-full w-full"
                            />
                            <p className="booking-summary-phase">
                              {copy.stepPhaseLabels?.[3] ?? "4/4"}
                            </p>
                          </div>
                        ) : null}
                        <div className="booking-summary-row">
                          <span className="booking-summary-icon"><FieldGlyph name="wrench" /></span>
                          <div className="min-w-0">
                            <p className="booking-summary-kicker">{business.vocabulary.serviceTitle}</p>
                            <p className="booking-summary-value">{selectedService.name}</p>
                          </div>
                        </div>
                        <div className="booking-summary-row">
                          <span className="booking-summary-icon"><FieldGlyph name="person" /></span>
                          <div className="min-w-0">
                            <p className="booking-summary-kicker">{copy.summaryProfessional}</p>
                            <p className="booking-summary-value">{selectedProfessional.displayName}</p>
                          </div>
                        </div>
                        <div className="booking-summary-row">
                          <span className="booking-summary-icon"><FieldGlyph name="calendar" /></span>
                          <div className="min-w-0">
                            <p className="booking-summary-kicker">{copy.summaryDate}</p>
                            <p className="booking-summary-value">{formatSummaryDay(selectedDate)}</p>
                          </div>
                        </div>
                        <div className="booking-summary-row">
                          <span className="booking-summary-icon"><FieldGlyph name="clock" /></span>
                          <div className="min-w-0">
                            <p className="booking-summary-kicker">{copy.summaryTime}</p>
                            <p className="booking-summary-value">
                              {formatTime(selectedSlot.startTime)} – {formatTime(selectedSlot.endTime)}
                            </p>
                          </div>
                        </div>
                        <div className="booking-summary-row">
                          <span className="booking-summary-icon"><FieldGlyph name="duration" /></span>
                          <div className="min-w-0">
                            <p className="booking-summary-kicker">{copy.summaryDuration}</p>
                            <p className="booking-summary-value">
                              {selectedService.durationMinutes} {copy.minutes}
                            </p>
                          </div>
                        </div>
                        <div className="booking-summary-price-block">
                          <p className="booking-summary-price">
                            {formatMoney(Number(selectedService.price), selectedService.currency)}
                          </p>
                          <p className="booking-summary-note">Precio orientativo</p>
                        </div>
                        <ul className="booking-summary-guarantees">
                          {[
                            "Presupuesto antes de reparar",
                            "Sin pago por adelantado",
                            "Cancela gratis",
                          ].map((item) => (
                            <li key={item} className="booking-summary-guarantee">
                              <span className="booking-summary-check" aria-hidden="true">
                                <FieldGlyph name="check" />
                              </span>
                              {item}
                            </li>
                          ))}
                        </ul>
                      </div>
                    ) : (
                    <div className="h-fit rounded-[1.75rem] border border-(--p-3b332e)/10 bg-(--surface-muted) p-6">
                    <p className="text-[10px] font-semibold uppercase tracking-[0.2em] text-(--p-8b7261)">
                      {copy.summaryEyebrow}
                    </p>

                    <h3 className="mt-3 font-serif text-2xl text-(--heading)">
                      {
                        selectedService.name
                      }
                    </h3>

                    <div className="mt-6 space-y-4 border-t border-(--p-3b332e)/10 pt-5 text-sm">
                      <div>
                        <p className="text-xs text-(--p-8b7c72)">
                          {copy.summaryProfessional}
                        </p>

                        <p className="mt-1 font-medium text-(--p-423830)">
                          {
                            selectedProfessional.displayName
                          }
                        </p>
                      </div>

                      <div>
                        <p className="text-xs text-(--p-8b7c72)">
                          {copy.summaryDate}
                        </p>

                        <p className="mt-1 font-medium text-(--p-423830)">
                          {formatDate(
                            selectedDate
                          )}
                        </p>
                      </div>

                      <div>
                        <p className="text-xs text-(--p-8b7c72)">
                          {copy.summaryTime}
                        </p>

                        <p className="mt-1 font-medium text-(--p-423830)">
                          {formatTime(
                            selectedSlot.startTime
                          )}{" "}
                          –{" "}
                          {formatTime(
                            selectedSlot.endTime
                          )}
                        </p>
                      </div>

                      <div>
                        <p className="text-xs text-(--p-8b7c72)">
                          {copy.summaryDuration}
                        </p>

                        <p className="mt-1 font-medium text-(--p-423830)">
                          {
                            selectedService.durationMinutes
                          }{" "}
                          {copy.minutes}
                        </p>
                      </div>

                      <div>
                        <p className="text-xs text-(--p-8b7c72)">
                          {copy.summaryPrice}
                        </p>

                        <p className="mt-1 font-serif text-xl text-(--p-342d28)">
                          {formatMoney(
                            Number(
                              selectedService.price
                            ),
                            selectedService.currency
                          )}
                        </p>
                      </div>
                    </div>
                    </div>
                    )}
                    </div>
                  </aside>
                </div>
              </div>
            )}

          {/* =================================================
              SUCCESS
          ================================================== */}

          {step ===
            "success" &&
            bookingResult && (
              <div className={taller && sequence ? "booking-success-in mx-auto max-w-[880px]" : "mx-auto max-w-[760px]"}>
                {taller && sequence && copy.successVisualTitle ? (
                  <div className="mb-6 overflow-hidden rounded-[1.75rem] bg-[#1c1917]">
                    <div className="relative h-64 sm:h-72">
                      <BookingCanvas
                        sequence={sequence}
                        stepFrames={copy.stepFrames}
                        mode="hold"
                        target={phase}
                        className="h-full w-full"
                      />
                      <div className="pointer-events-none absolute inset-0 bg-gradient-to-r from-[#1c1917] via-[#1c1917]/55 to-transparent" />
                      <p className="absolute bottom-6 left-6 max-w-[12ch] font-serif text-4xl leading-[0.95] text-white sm:text-5xl">
                        {copy.successVisualTitle}
                      </p>
                    </div>
                  </div>
                ) : null}
                <div className={taller && sequence ? "rounded-[2rem] border border-(--inverse)/10 border-t-2 border-t-[#f97316] bg-white p-7 text-left shadow-[0_28px_80px_rgba(28,25,23,0.12)] sm:p-10" : "rounded-[2rem] border border-(--p-3b332e)/10 bg-white/60 p-7 text-center shadow-[0_24px_70px_rgba(56,46,40,0.08)] sm:p-10"}>
                  {taller ? (
                    <TallerConfirmation
                      booking={bookingResult}
                      emailed={customerForm.email.trim().length > 0}
                      managementHref={managementHref}
                      copied={referenceCopied}
                      onCopy={() => {
                        void copyAppointmentReference();
                      }}
                      onReset={resetBooking}
                    />
                  ) : (
                  <>
                  <div className={taller && sequence ? "flex h-16 w-16 items-center justify-center rounded-full bg-[#1c1917] text-2xl text-[#f97316]" : "mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-(--p-ded2c6) text-2xl text-(--p-4f4036)"}>
                    ✓
                  </div>

                  <p className="mt-7 text-[10px] font-semibold uppercase tracking-[0.22em] text-(--p-987e6c)">
                    {copy.successEyebrow}
                  </p>

                  <h2 className="mt-3 font-serif text-4xl text-(--p-302924) sm:text-5xl">
                    {copy.successTitle}
                  </h2>

                  <p className="mx-auto mt-5 max-w-[560px] text-sm leading-7 text-(--p-756960)">
                    {bookingResult.message || copy.successFallback}
                  </p>

                  <div className="mt-8 rounded-[1.5rem] bg-(--surface-muted) p-6 text-left">
                    <div className="grid gap-5 sm:grid-cols-2">
                      <div>
                        <p className="text-[10px] font-semibold uppercase tracking-[0.15em] text-(--p-917b6b)">
                          {copy.successService}
                        </p>

                        <p className="mt-1 font-medium text-(--p-3f352f)">
                          {
                            bookingResult.serviceName
                          }
                        </p>
                      </div>

                      <div>
                        <p className="text-[10px] font-semibold uppercase tracking-[0.15em] text-(--p-917b6b)">
                          {copy.successProfessional}
                        </p>

                        <p className="mt-1 font-medium text-(--p-3f352f)">
                          {
                            bookingResult.professionalName
                          }
                        </p>
                      </div>

                      <div>
                        <p className="text-[10px] font-semibold uppercase tracking-[0.15em] text-(--p-917b6b)">
                          {copy.successDateTime}
                        </p>

                        <p className="mt-1 font-medium text-(--p-3f352f)">
                          {formatBookingDateTime(
                            bookingResult.startAt
                          )}
                        </p>
                      </div>

                      <div>
                        <p className="text-[10px] font-semibold uppercase tracking-[0.15em] text-(--p-917b6b)">
                          {copy.successStatus}
                        </p>

                        <p className="mt-1 font-medium text-(--p-3f352f)">
                          {getStatusLabel(
                            bookingResult.status
                          )}
                        </p>
                      </div>
                    </div>

                    <div className="mt-5 border-t border-(--p-3b332e)/10 pt-5">
                      <p className="text-[10px] font-semibold uppercase tracking-[0.15em] text-(--p-917b6b)">
                        {copy.successReference}
                      </p>

                      <p className="mt-1 font-medium text-(--p-3f352f)">
                        {copy.appointmentRef}{" "}
                        {bookingResult.appointmentId}
                      </p>
                    </div>
                  </div>

                  <div className="mt-8 flex flex-col justify-center gap-3 sm:flex-row sm:flex-wrap">
                    <Link
                      href={managementHref}
                      className="inline-flex min-h-13 items-center justify-center rounded-full bg-(--p-302924) px-7 py-3 text-center text-xs font-semibold uppercase tracking-[0.15em] !text-(--p-fffaf5) no-underline shadow-[0_10px_26px_rgba(48,41,36,0.16)] transition hover:-translate-y-0.5 hover:bg-(--p-4b3d34) hover:!text-white focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-(--accent)"
                    >
                      <span style={{ color: "var(--p-fffaf5)", WebkitTextFillColor: "var(--p-fffaf5)" }}>
                        {copy.manageCta}
                      </span>
                    </Link>

                    <button
                      type="button"
                      onClick={
                        resetBooking
                      }
                      className="inline-flex h-13 items-center justify-center rounded-full border border-(--p-3b332e)/15 bg-white px-7 text-xs font-semibold uppercase tracking-[0.15em] text-(--p-403630) transition hover:bg-(--p-f5eee8)"
                    >
                      {copy.bookAnother}
                    </button>

                    <Link
                      href="/"
                      className="inline-flex h-13 items-center justify-center rounded-full px-7 text-xs font-semibold uppercase tracking-[0.15em] text-(--p-6f6259) transition hover:bg-(--surface-muted)/70 hover:text-(--p-302924)"
                    >
                      {copy.backPrefix} {business.brand.shortName}
                    </Link>
                  </div>
                  </>
                  )}
                </div>
              </div>
            )}
        </section>
      </main>

      <SiteFooter />
    </div>
  );
}