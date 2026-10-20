"use client";

import Link from "next/link";
import {
  FormEvent,
  useEffect,
  useMemo,
  useState,
} from "react";

import { SiteFooter } from "@/components/layout/SiteFooter";
import { SiteHeader } from "@/components/layout/SiteHeader";
import { business } from "@/config/business";
import type { BookingExtraField } from "@/data/types";
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

  return new Intl.DateTimeFormat(
    "es-ES",
    {
      weekday: "long",
      day: "numeric",
      month: "long",
      year: "numeric",
    }
  ).format(date);
}

function formatBookingDateTime(
  value: string
): string {
  return new Intl.DateTimeFormat(
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

  const monthLabel =
    `${CALENDAR_MONTHS[
      visibleMonth.getMonth()
    ]} ${visibleMonth.getFullYear()}`;

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
          <p className="font-serif text-xl capitalize text-(--p-342c27)">
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

            const isDisabled =
              disabled ||
              day.date < minDate ||
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
                className={[
                  "relative flex aspect-square min-h-9 items-center justify-center rounded-full text-xs font-medium transition-all duration-200 sm:min-h-10",
                  isSelected
                    ? "bg-(--p-302924) text-white shadow-[0_8px_18px_rgba(48,41,36,0.22)]"
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
                    ? "cursor-not-allowed text-(--p-d0c6bf)"
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
                      className="absolute bottom-[5px] h-1 w-1 rounded-full bg-(--p-92725d)"
                    />
                  )}
              </button>
            );
          }
        )}
      </div>

      {/* CALENDAR FOOTER */}

      <div className="mt-6 flex items-center justify-between gap-4 border-t border-(--p-3b332e)/10 pt-4">
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

        {value ? (
          <p className="text-right text-xs capitalize text-(--p-706258)">
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

/* =========================================================
   PAGE
========================================================= */

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

  const managementHref =
    bookingResult
      ? bookingResult.managementToken
        ? `/reservar/gestionar?appointmentId=${bookingResult.appointmentId}#token=${encodeURIComponent(
            bookingResult.managementToken
          )}`
        : `/reservar/gestionar?appointmentId=${bookingResult.appointmentId}`
      : "/reservar/gestionar";

  return (
    <div className="min-h-screen bg-(--canvas) text-(--p-2d2723)">
      <SiteHeader />

      <main>
        {/* ===================================================
            HERO
        ==================================================== */}

        <section className="border-b border-(--p-3a312c)/10 bg-(--hero-bg)">
          <div className="mx-auto w-full max-w-[1180px] px-5 py-14 sm:px-8 sm:py-16 lg:px-12">
            <Link
              href="/"
              className="inline-flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.18em] text-(--p-856f5f) transition hover:text-(--p-392f29)"
            >
              <span aria-hidden="true">
                ←
              </span>

              {copy.backPrefix} {business.brand.name}
            </Link>

            <div className="mt-8 max-w-[760px]">
              <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-(--accent)">
                {copy.eyebrow}
              </p>

              <h1 className="mt-4 font-serif text-[clamp(2.8rem,6vw,5.6rem)] leading-[0.95] tracking-[-0.04em] text-(--p-2b2521)">
                {copy.titleLead}

                <span className="block italic text-(--accent)">
                  {copy.titleAccent}
                </span>
              </h1>

              <p className="mt-6 max-w-[620px] text-base leading-7 text-(--p-6b5f56) sm:text-lg">
                {copy.description}
              </p>
            </div>
          </div>
        </section>

        {/* ===================================================
            BOOKING
        ==================================================== */}

        <section className="mx-auto w-full max-w-[1180px] px-5 py-10 sm:px-8 sm:py-14 lg:px-12 lg:py-16">
          {step !==
            "success" && (
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

          {error && (
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
              <div className="mb-8">
                <p className="text-[10px] font-semibold uppercase tracking-[0.22em] text-(--p-987e6c)">
                  {copy.stepHeadings[0]}
                </p>

                <h2 className="mt-2 font-serif text-3xl text-(--heading) sm:text-4xl">
                  {copy.chooseServiceTitle}
                </h2>

                <p className="mt-3 max-w-[620px] text-sm leading-6 text-(--p-756960)">
                  {copy.chooseServiceDescription}
                </p>
              </div>

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
                    service => (
                      <button
                        key={
                          service.id
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
                        className="group rounded-[1.5rem] border border-(--p-3b332e)/10 bg-white/55 p-6 text-left shadow-[0_12px_35px_rgba(57,47,41,0.05)] transition-all duration-300 hover:-translate-y-1 hover:border-(--accent)/30 hover:bg-white hover:shadow-[0_20px_45px_rgba(57,47,41,0.10)] disabled:pointer-events-none disabled:opacity-60"
                      >
                        <div className="flex items-start justify-between gap-5">
                          <div>
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

                          <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full border border-(--p-3b332e)/10 bg-(--p-f3ece5) text-lg text-(--p-5a493f) transition-transform duration-300 group-hover:translate-x-1">
                            →
                          </span>
                        </div>

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
                      </button>
                    )
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

                <div className="mb-8">
                  <p className="text-[10px] font-semibold uppercase tracking-[0.22em] text-(--p-987e6c)">
                    {copy.stepHeadings[1]}
                  </p>

                  <h2 className="mt-2 font-serif text-3xl text-(--heading) sm:text-4xl">
                    {copy.chooseProfessionalTitle}
                  </h2>

                  <p className="mt-3 text-sm leading-6 text-(--p-756960)">
                    {
                      selectedService.name
                    }{" "}
                    ·{" "}
                    {
                      selectedService.durationMinutes
                    }{" "}
                    {copy.minuteShort} ·{" "}
                    {formatMoney(
                      Number(
                        selectedService.price
                      ),
                      selectedService.currency
                    )}
                  </p>
                </div>

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

                <div className="mb-8">
                  <p className="text-[10px] font-semibold uppercase tracking-[0.22em] text-(--p-987e6c)">
                    {copy.stepHeadings[2]}
                  </p>

                  <h2 className="mt-2 font-serif text-3xl text-(--heading) sm:text-4xl">
                    {copy.dateTimeTitle}
                  </h2>

                  <p className="mt-3 text-sm leading-6 text-(--p-756960)">
                    {
                      selectedService.name
                    }{" "}
                    {copy.withProfessional}{" "}
                    {
                      selectedProfessional.displayName
                    }
                  </p>
                </div>

                <div className="grid gap-6 lg:grid-cols-[0.95fr_1.05fr]">
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

                  <div className="rounded-[1.75rem] border border-(--p-3b332e)/10 bg-white/60 p-5 shadow-[0_14px_40px_rgba(57,47,41,0.04)] sm:p-6">
                    <div className="flex items-start justify-between gap-4">
                      <div>
                        <p className="text-[10px] font-semibold uppercase tracking-[0.18em] text-(--p-79685c)">
                          {copy.timesLabel}
                        </p>

                        {selectedDate && (
                          <p className="mt-2 text-xs capitalize leading-5 text-(--p-96877d)">
                            {formatDate(
                              selectedDate
                            )}
                          </p>
                        )}
                      </div>

                      {availability?.available &&
                        availability.totalSlots >
                          0 && (
                          <span className="rounded-full bg-(--surface-muted) px-3 py-1.5 text-[10px] font-semibold uppercase tracking-[0.1em] text-(--p-755f51)">
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
                      <div className="mt-6">
                        <p className="text-xs leading-5 text-(--p-85776d)">
                          {copy.selectTimeHint}
                        </p>

                        <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-3">
                          {availability.slots.map(
                            slot => {
                              const selected =
                                selectedSlot?.startAt ===
                                slot.startAt;

                              return (
                                <button
                                  key={
                                    slot.startAt
                                  }
                                  type="button"
                                  onClick={() => {
                                    setSelectedSlot(
                                      slot
                                    );

                                    setError(
                                      null
                                    );
                                  }}
                                  className={[
                                    "rounded-xl border px-4 py-3.5 text-sm font-semibold transition-all duration-200",
                                    selected
                                      ? "border-(--heading) bg-(--heading) text-white shadow-[0_10px_22px_rgba(51,43,38,0.18)]"
                                      : "border-(--p-3b332e)/12 bg-(--p-fffdfb) text-(--p-4b4038) hover:-translate-y-0.5 hover:border-(--accent)/35 hover:bg-(--p-f4ece5)",
                                  ].join(" ")}
                                >
                                  {formatTime(
                                    slot.startTime
                                  )}
                                </button>
                              );
                            }
                          )}
                        </div>

                        {selectedSlot && (
                          <div className="mt-6 flex items-center justify-between gap-4 rounded-[1.1rem] bg-(--surface-muted) px-4 py-3.5">
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
                    className="inline-flex h-13 items-center justify-center rounded-full bg-(--p-302924) px-8 text-xs font-semibold uppercase tracking-[0.16em] text-white shadow-[0_10px_28px_rgba(48,41,36,0.16)] transition hover:-translate-y-0.5 hover:bg-(--p-4b3d34) disabled:cursor-not-allowed disabled:opacity-35 disabled:hover:translate-y-0"
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

                <div className="grid gap-8 lg:grid-cols-[1fr_0.72fr]">
                  <div>
                    <div className="mb-8">
                      <p className="text-[10px] font-semibold uppercase tracking-[0.22em] text-(--p-987e6c)">
                        {copy.stepHeadings[3]}
                      </p>

                      <h2 className="mt-2 font-serif text-3xl text-(--heading) sm:text-4xl">
                        {copy.customerTitle}
                      </h2>

                      <p className="mt-3 text-sm leading-6 text-(--p-756960)">
                        {copy.customerDescription}
                      </p>
                    </div>

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
                  </div>

                  {/* SUMMARY */}

                  <aside className="h-fit rounded-[1.75rem] border border-(--p-3b332e)/10 bg-(--surface-muted) p-6 lg:sticky lg:top-[100px]">
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

                        <p className="mt-1 capitalize font-medium text-(--p-423830)">
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
              <div className="mx-auto max-w-[760px]">
                <div className="rounded-[2rem] border border-(--p-3b332e)/10 bg-white/60 p-7 text-center shadow-[0_24px_70px_rgba(56,46,40,0.08)] sm:p-10">
                  <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-(--p-ded2c6) text-2xl text-(--p-4f4036)">
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

                        <p className="mt-1 capitalize font-medium text-(--p-3f352f)">
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
                </div>
              </div>
            )}
        </section>
      </main>

      <SiteFooter />
    </div>
  );
}