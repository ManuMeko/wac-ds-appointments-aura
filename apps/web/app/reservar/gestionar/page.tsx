
"use client";

import Link from "next/link";
import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

import { SiteFooter } from "@/components/layout/SiteFooter";
import { SiteHeader } from "@/components/layout/SiteHeader";
import { business } from "@/config/business";
import { getApiBaseUrl } from "@/lib/api";

/* =========================================================
   WAC D.S. · MODEL 01 · APPOINTMENTS
   Aura Studio · Public Booking Management
   Secure appointment-scoped token handling
========================================================= */

const API_BASE_URL = getApiBaseUrl().replace(/\/$/, "");

const copy = business.manageBooking;
const bookingCopy = business.booking;

const MANAGEMENT_HEADER = "X-Booking-Management-Token";
const STORAGE_PREFIX = "wacds_booking_management_";
const TOKEN_HASH_KEY = "token";

type BookingStatus =
  | "PENDING"
  | "CONFIRMED"
  | "COMPLETED"
  | "CANCELLED"
  | "NO_SHOW"
  | string;

type PublicBookingManagementResponse = {
  appointmentId: number;
  status: BookingStatus;
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
  onlineCancellationEnabled: boolean;
  minimumCancellationNoticeMinutes: number;
  onlineReschedulingEnabled: boolean;
  minimumRescheduleNoticeMinutes: number;
  cancellationAllowed: boolean;
  reschedulingAllowed: boolean;
  cancellationDeadline: string | null;
  reschedulingDeadline: string | null;
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

type AccessState =
  | "loading"
  | "ready"
  | "missing"
  | "invalid"
  | "cancelled"
  | "error";

type BookingCredentials = {
  appointmentId: number;
  token: string;
};

const WEEKDAYS = ["L", "M", "X", "J", "V", "S", "D"];

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

/* =========================================================
   DATE HELPERS
========================================================= */

function dateToLocalIso(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

function parseLocalDate(value: string): Date {
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, month - 1, day);
}

function getTodayLocalDate(): string {
  return dateToLocalIso(new Date());
}

function getMaximumDate(): string {
  const date = new Date();
  date.setDate(date.getDate() + 365);
  return dateToLocalIso(date);
}

function getMonthKey(date: Date): number {
  return date.getFullYear() * 12 + date.getMonth();
}

function addMonths(date: Date, amount: number): Date {
  return new Date(
    date.getFullYear(),
    date.getMonth() + amount,
    1
  );
}

function getCalendarDays(visibleMonth: Date): CalendarDay[] {
  const year = visibleMonth.getFullYear();
  const month = visibleMonth.getMonth();
  const firstDay = new Date(year, month, 1);
  const leadingDays = (firstDay.getDay() + 6) % 7;
  const numberOfDays = new Date(year, month + 1, 0).getDate();

  const days: CalendarDay[] = [];

  for (let index = 0; index < leadingDays; index += 1) {
    days.push({
      key: `empty-start-${index}`,
      date: null,
      dayNumber: null,
    });
  }

  for (let day = 1; day <= numberOfDays; day += 1) {
    const iso = dateToLocalIso(new Date(year, month, day));

    days.push({
      key: iso,
      date: iso,
      dayNumber: day,
    });
  }

  while (days.length % 7 !== 0) {
    days.push({
      key: `empty-end-${days.length}`,
      date: null,
      dayNumber: null,
    });
  }

  return days;
}

/* =========================================================
   FORMATTERS
========================================================= */

function formatDateTime(value: string): string {
  try {
    return new Intl.DateTimeFormat("es-ES", {
      weekday: "long",
      day: "numeric",
      month: "long",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    }).format(new Date(value));
  } catch {
    return copy.invalidDate;
  }
}

function formatDate(value: string): string {
  if (!value) return "";

  return new Intl.DateTimeFormat("es-ES", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(parseLocalDate(value));
}

function formatTime(value: string): string {
  return value.slice(0, 5);
}

function getStatusLabel(status: string): string {
  switch (status.toUpperCase()) {
    case "PENDING":
      return bookingCopy.statuses.PENDING;
    case "CONFIRMED":
      return bookingCopy.statuses.CONFIRMED;
    case "COMPLETED":
      return bookingCopy.statuses.COMPLETED;
    case "CANCELLED":
      return bookingCopy.statuses.CANCELLED;
    case "NO_SHOW":
      return bookingCopy.statuses.NO_SHOW;
    default:
      return status;
  }
}

function isCancelled(booking: PublicBookingManagementResponse) {
  return booking.status.toUpperCase() === "CANCELLED";
}

/* =========================================================
   API HELPERS
========================================================= */

async function readApiError(
  response: Response,
  fallback: string
): Promise<string> {
  try {
    const payload = (await response.json()) as ApiErrorPayload;

    return (
      payload.message ||
      payload.detail ||
      payload.error ||
      fallback
    );
  } catch {
    return fallback;
  }
}

function isValidBookingResponse(
  value: unknown
): value is PublicBookingManagementResponse {
  if (!value || typeof value !== "object") return false;

  const data = value as Partial<PublicBookingManagementResponse>;

  return (
    typeof data.appointmentId === "number" &&
    Number.isSafeInteger(data.appointmentId) &&
    data.appointmentId > 0 &&
    typeof data.status === "string" &&
    typeof data.serviceName === "string" &&
    typeof data.startAt === "string"
  );
}

/* =========================================================
   SECURE TOKEN HELPERS
========================================================= */

function normalizeToken(
  value: string | null | undefined
): string | null {
  if (typeof value !== "string") return null;
  return value.trim() || null;
}

function getAppointmentIdFromUrl(): number | null {
  if (typeof window === "undefined") return null;

  const params = new URLSearchParams(window.location.search);
  const raw = params.get("appointmentId");

  if (!raw || !/^[1-9]\d*$/.test(raw)) return null;

  const id = Number(raw);
  return Number.isSafeInteger(id) ? id : null;
}

function getTokenFromHash(): string | null {
  if (typeof window === "undefined") return null;

  const rawHash = window.location.hash.replace(/^#/, "");
  if (!rawHash) return null;

  return normalizeToken(
    new URLSearchParams(rawHash).get(TOKEN_HASH_KEY)
  );
}

function removeTokenFromUrl(): void {
  if (typeof window === "undefined") return;
  if (!window.location.hash) return;

  const safeUrl =
    window.location.pathname + window.location.search;

  window.history.replaceState(
    window.history.state,
    "",
    safeUrl
  );
}

function getStorageKey(appointmentId: number): string {
  return `${STORAGE_PREFIX}${appointmentId}`;
}

function readStoredToken(appointmentId: number): string | null {
  if (typeof window === "undefined") return null;

  try {
    return normalizeToken(
      window.sessionStorage.getItem(
        getStorageKey(appointmentId)
      )
    );
  } catch {
    return null;
  }
}

function storeManagementToken(
  appointmentId: number,
  token: string
): void {
  if (typeof window === "undefined") return;

  if (
    !Number.isSafeInteger(appointmentId) ||
    appointmentId <= 0
  ) {
    return;
  }

  const normalized = normalizeToken(token);
  if (!normalized) return;

  try {
    window.sessionStorage.setItem(
      getStorageKey(appointmentId),
      normalized
    );
  } catch {
    // Private storage may be disabled.
  }
}

function removeStoredManagementToken(
  appointmentId: number
): void {
  if (typeof window === "undefined") return;

  try {
    window.sessionStorage.removeItem(
      getStorageKey(appointmentId)
    );
  } catch {
    // Storage can be unavailable.
  }
}

/*
 * SECURITY:
 * Never fall back to another appointment's stored token.
 *
 * If appointmentId=34 is requested, only the credential
 * associated with appointment #34 may be used.
 *
 * A token arriving through #token is provisional until
 * the backend confirms its appointmentId.
 */
function resolveCredentials(): BookingCredentials | null {
  const appointmentId = getAppointmentIdFromUrl();
  const hashToken = getTokenFromHash();

  // Remove the fragment before any API request.
  if (hashToken) {
    removeTokenFromUrl();
  }

  if (appointmentId === null) {
    return null;
  }

  const token =
    hashToken || readStoredToken(appointmentId);

  if (!token) return null;

  return {
    appointmentId,
    token,
  };
}

/* =========================================================
   UI HELPERS
========================================================= */

function LoadingSpinner() {
  return (
    <span
      aria-hidden="true"
      className="h-4 w-4 animate-spin rounded-full border-2 border-current border-r-transparent"
    />
  );
}

function CalendarDatePicker({
  value,
  minDate,
  maxDate,
  disabled = false,
  onChange,
}: CalendarDatePickerProps) {
  const today = getTodayLocalDate();

  const initialDate = value
    ? parseLocalDate(value)
    : parseLocalDate(minDate);

  const [visibleMonth, setVisibleMonth] = useState<Date>(
    new Date(
      initialDate.getFullYear(),
      initialDate.getMonth(),
      1
    )
  );

  const [trackedValue, setTrackedValue] = useState(value);

  if (value !== trackedValue) {
    setTrackedValue(value);

    if (value) {
      const selected = parseLocalDate(value);

      setVisibleMonth(
        new Date(
          selected.getFullYear(),
          selected.getMonth(),
          1
        )
      );
    }
  }

  const calendarDays = useMemo(
    () => getCalendarDays(visibleMonth),
    [visibleMonth]
  );

  const currentMonthKey = getMonthKey(visibleMonth);
  const minMonthKey = getMonthKey(parseLocalDate(minDate));
  const maxMonthKey = getMonthKey(parseLocalDate(maxDate));

  const canGoPrevious = currentMonthKey > minMonthKey;
  const canGoNext = currentMonthKey < maxMonthKey;

  const monthLabel =
    `${MONTHS[visibleMonth.getMonth()]} ` +
    `${visibleMonth.getFullYear()}`;

  return (
    <div className="mt-5">
      <div className="flex items-center justify-between gap-4">
        <button
          type="button"
          aria-label={bookingCopy.previousMonth}
          disabled={disabled || !canGoPrevious}
          onClick={() =>
            setVisibleMonth(current => addMonths(current, -1))
          }
          className="flex h-10 w-10 items-center justify-center rounded-full border border-(--p-3b332e)/10 bg-(--p-f8f3ee) text-xl text-(--p-5a4a40) transition hover:bg-(--p-eadfd4) disabled:cursor-not-allowed disabled:opacity-25"
        >
          ‹
        </button>

        <div className="text-center">
          <p className="font-serif text-xl capitalize text-(--p-342c27)">
            {monthLabel}
          </p>
          <p className="mt-0.5 text-[9px] uppercase tracking-[0.18em] text-(--p-9a887c)">
            {bookingCopy.selectDay}
          </p>
        </div>

        <button
          type="button"
          aria-label={bookingCopy.nextMonth}
          disabled={disabled || !canGoNext}
          onClick={() =>
            setVisibleMonth(current => addMonths(current, 1))
          }
          className="flex h-10 w-10 items-center justify-center rounded-full border border-(--p-3b332e)/10 bg-(--p-f8f3ee) text-xl text-(--p-5a4a40) transition hover:bg-(--p-eadfd4) disabled:cursor-not-allowed disabled:opacity-25"
        >
          ›
        </button>
      </div>

      <div className="mt-6 grid grid-cols-7 gap-1">
        {WEEKDAYS.map((weekday, index) => (
          <div
            key={`${weekday}-${index}`}
            className="flex h-8 items-center justify-center text-[9px] font-semibold uppercase tracking-[0.13em] text-(--p-a19085)"
          >
            {weekday}
          </div>
        ))}
      </div>

      <div className="grid grid-cols-7 gap-1.5">
        {calendarDays.map(day => {
          if (!day.date || day.dayNumber === null) {
            return (
              <div
                key={day.key}
                className="aspect-square"
              />
            );
          }

          const date = day.date;
          const isSelected = date === value;
          const isToday = date === today;

          const isDisabled =
            disabled ||
            date < minDate ||
            date > maxDate;

          return (
            <button
              key={day.key}
              type="button"
              disabled={isDisabled}
              aria-label={formatDate(date)}
              aria-pressed={isSelected}
              onClick={() => onChange(date)}
              className={[
                "relative flex aspect-square min-h-9 items-center justify-center rounded-full text-xs font-medium transition-all duration-200 sm:min-h-10",
                isSelected
                  ? "bg-(--p-302924) text-white shadow-[0_8px_18px_rgba(48,41,36,0.22)]"
                  : "",
                !isSelected && isToday
                  ? "border border-(--p-9d7f6a)/50 bg-(--p-efe5db) text-(--p-54453c)"
                  : "",
                !isSelected && !isToday && !isDisabled
                  ? "text-(--p-51453d) hover:bg-(--p-eadfd4)"
                  : "",
                isDisabled
                  ? "cursor-not-allowed text-(--p-d0c6bf)"
                  : "",
              ].join(" ")}
            >
              {day.dayNumber}
            </button>
          );
        })}
      </div>
    </div>
  );
}

/* =========================================================
   PAGE
========================================================= */

export default function ManageBookingPage() {
  const [credentials, setCredentials] =
    useState<BookingCredentials | null>(null);

  const [booking, setBooking] =
    useState<PublicBookingManagementResponse | null>(null);

  const [accessState, setAccessState] =
    useState<AccessState>("loading");

  const [error, setError] = useState<string | null>(null);

  const [cancelling, setCancelling] = useState(false);
  const [showCancelConfirm, setShowCancelConfirm] =
    useState(false);

  const [rescheduling, setRescheduling] = useState(false);
  const [rescheduleDate, setRescheduleDate] = useState("");

  const [availability, setAvailability] =
    useState<DailyAvailabilityResponse | null>(null);

  const [loadingAvailability, setLoadingAvailability] =
    useState(false);

  const [selectedSlot, setSelectedSlot] =
    useState<AvailabilitySlot | null>(null);

  const rescheduleSectionRef = useRef<HTMLDivElement | null>(
    null
  );

  const requestSequence = useRef(0);
  const availabilitySequence = useRef(0);
  const operationLock = useRef(false);
  const currentCredentialsRef =
    useRef<BookingCredentials | null>(null);

  const today = useMemo(() => getTodayLocalDate(), []);
  const maximumDate = useMemo(() => getMaximumDate(), []);

  const resetManagementUI = useCallback(() => {
    availabilitySequence.current += 1;
    setBooking(null);
    setError(null);
    setCancelling(false);
    setShowCancelConfirm(false);
    setRescheduling(false);
    setRescheduleDate("");
    setAvailability(null);
    setLoadingAvailability(false);
    setSelectedSlot(null);
  }, []);

  /* =======================================================
     SECURE INITIALIZATION
  ======================================================== */

  useEffect(() => {
    let active = true;
    const controller = new AbortController();

    async function initialize() {
      const sequence = ++requestSequence.current;
      const resolved = resolveCredentials();

      currentCredentialsRef.current = resolved;
      resetManagementUI();
      setCredentials(resolved);
      setAccessState("loading");

      if (!resolved) {
        setAccessState("missing");
        return;
      }

      try {
        const response = await fetch(
          `${API_BASE_URL}/api/v1/public/bookings/manage`,
          {
            method: "GET",
            headers: {
              Accept: "application/json",
              [MANAGEMENT_HEADER]: resolved.token,
            },
            cache: "no-store",
            signal: controller.signal,
          }
        );

        if (!active || sequence !== requestSequence.current) {
          return;
        }

        if (!response.ok) {
          if ([401, 403, 404].includes(response.status)) {
            removeStoredManagementToken(resolved.appointmentId);
            currentCredentialsRef.current = null;
            setCredentials(null);
            setBooking(null);
            setAccessState("invalid");
            return;
          }

          throw new Error(
            await readApiError(
              response,
              copy.errorLoad
            )
          );
        }

        const payload: unknown = await response.json();

        if (!active || sequence !== requestSequence.current) {
          return;
        }

        if (
          !isValidBookingResponse(payload) ||
          payload.appointmentId !== resolved.appointmentId
        ) {
          /*
           * Critical security invariant:
           * requested ID must equal backend response ID.
           *
           * Never render another appointment's information.
           */
          currentCredentialsRef.current = null;
          setCredentials(null);
          setBooking(null);
          setError(
            copy.errorCredentialMismatch
          );
          setAccessState("invalid");
          return;
        }

        storeManagementToken(
          resolved.appointmentId,
          resolved.token
        );

        setBooking(payload);
        setAccessState("ready");
      } catch (cause) {
        if (!active || controller.signal.aborted) return;

        setBooking(null);
        setError(
          cause instanceof Error
            ? cause.message
            : copy.errorLoad
        );
        setAccessState("error");
      }
    }

    void initialize();

    return () => {
      active = false;
      controller.abort();
      requestSequence.current += 1;
      availabilitySequence.current += 1;
    };
  }, [resetManagementUI]);

  /* =======================================================
     SCROLL TO RESCHEDULE
  ======================================================== */

  useEffect(() => {
    if (!rescheduling) return;

    const frame = window.requestAnimationFrame(() => {
      rescheduleSectionRef.current?.scrollIntoView({
        behavior: "smooth",
        block: "start",
      });

      rescheduleSectionRef.current?.focus({
        preventScroll: true,
      });
    });

    return () => window.cancelAnimationFrame(frame);
  }, [rescheduling]);

  /* =======================================================
     LOAD AVAILABILITY
  ======================================================== */

  async function loadAvailability(date: string) {
    const sequence = ++availabilitySequence.current;

    setRescheduleDate(date);
    setAvailability(null);
    setSelectedSlot(null);
    setError(null);

    if (
      !booking ||
      !date ||
      !booking.reschedulingAllowed ||
      accessState !== "ready"
    ) {
      return;
    }

    const expectedAppointmentId = booking.appointmentId;

    setLoadingAvailability(true);

    try {
      const params = new URLSearchParams({
        professionalId: String(booking.professionalId),
        serviceId: String(booking.serviceId),
        date,
      });

      const response = await fetch(
        `${API_BASE_URL}/api/v1/public/availability-slots?${params}`,
        {
          method: "GET",
          headers: {
            Accept: "application/json",
          },
          cache: "no-store",
        }
      );

      if (!response.ok) {
        throw new Error(
          await readApiError(
            response,
            copy.errorAvailability
          )
        );
      }

      const payload =
        (await response.json()) as DailyAvailabilityResponse;

      if (sequence !== availabilitySequence.current) return;

      if (
        payload.professionalId !== booking.professionalId ||
        payload.serviceId !== booking.serviceId ||
        payload.date !== date
      ) {
        throw new Error(
          copy.errorAvailabilityMismatch
        );
      }

      if (
        currentCredentialsRef.current?.appointmentId !==
        expectedAppointmentId
      ) {
        return;
      }

      setAvailability(payload);
    } catch (cause) {
      if (sequence !== availabilitySequence.current) return;

      setError(
        cause instanceof Error
          ? cause.message
          : copy.errorAvailability
      );
    } finally {
      if (sequence === availabilitySequence.current) {
        setLoadingAvailability(false);
      }
    }
  }

  /* =======================================================
     CANCEL BOOKING
  ======================================================== */

  async function cancelBooking() {
    if (
      !credentials ||
      !booking ||
      !booking.cancellationAllowed ||
      accessState !== "ready" ||
      operationLock.current
    ) {
      return;
    }

    if (
      credentials.appointmentId !== booking.appointmentId ||
      currentCredentialsRef.current?.token !==
        credentials.token
    ) {
      setError(copy.errorSession);
      return;
    }

    operationLock.current = true;
    setCancelling(true);
    setError(null);

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/v1/public/bookings/manage/cancel`,
        {
          method: "POST",
          headers: {
            Accept: "application/json",
            [MANAGEMENT_HEADER]: credentials.token,
          },
        }
      );

      if (!response.ok) {
        throw new Error(
          await readApiError(
            response,
            copy.errorCancel
          )
        );
      }

      /*
       * Backend revokes the token after cancellation.
       * Never fall back to a different appointment.
       */
      removeStoredManagementToken(booking.appointmentId);
      currentCredentialsRef.current = null;

      availabilitySequence.current += 1;

      setCredentials(null);
      setBooking(current =>
        current?.appointmentId === booking.appointmentId
          ? {
              ...current,
              status: "CANCELLED",
              cancellationAllowed: false,
              reschedulingAllowed: false,
            }
          : current
      );

      setShowCancelConfirm(false);
      setRescheduling(false);
      setRescheduleDate("");
      setAvailability(null);
      setSelectedSlot(null);
      setLoadingAvailability(false);
      setAccessState("cancelled");
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : copy.errorCancel
      );
    } finally {
      operationLock.current = false;
      setCancelling(false);
    }
  }

  /* =======================================================
     CONFIRM RESCHEDULE
  ======================================================== */

  async function confirmReschedule() {
    if (
      !credentials ||
      !booking ||
      !selectedSlot ||
      !booking.reschedulingAllowed ||
      accessState !== "ready" ||
      operationLock.current
    ) {
      return;
    }

    if (
      credentials.appointmentId !== booking.appointmentId ||
      currentCredentialsRef.current?.token !==
        credentials.token
    ) {
      setError(copy.errorSession);
      return;
    }

    const expectedAppointmentId = booking.appointmentId;
    const expectedToken = credentials.token;

    operationLock.current = true;
    setLoadingAvailability(true);
    setError(null);

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/v1/public/bookings/manage/reschedule`,
        {
          method: "POST",
          headers: {
            Accept: "application/json",
            "Content-Type": "application/json",
            [MANAGEMENT_HEADER]: expectedToken,
          },
          body: JSON.stringify({
            startAt: selectedSlot.startAt,
          }),
        }
      );

      if (!response.ok) {
        throw new Error(
          await readApiError(
            response,
            copy.errorReschedule
          )
        );
      }

      const refreshResponse = await fetch(
        `${API_BASE_URL}/api/v1/public/bookings/manage`,
        {
          method: "GET",
          headers: {
            Accept: "application/json",
            [MANAGEMENT_HEADER]: expectedToken,
          },
          cache: "no-store",
        }
      );

      if (!refreshResponse.ok) {
        throw new Error(
          copy.errorRescheduleVerify
        );
      }

      const refreshed: unknown = await refreshResponse.json();

      if (
        !isValidBookingResponse(refreshed) ||
        refreshed.appointmentId !== expectedAppointmentId
      ) {
        throw new Error(
          copy.errorRescheduleMismatch
        );
      }

      if (
        currentCredentialsRef.current?.appointmentId !==
          expectedAppointmentId ||
        currentCredentialsRef.current?.token !== expectedToken
      ) {
        return;
      }

      storeManagementToken(
        expectedAppointmentId,
        expectedToken
      );

      availabilitySequence.current += 1;

      setBooking(refreshed);
      setRescheduling(false);
      setRescheduleDate("");
      setAvailability(null);
      setSelectedSlot(null);

      window.scrollTo({
        top: 0,
        behavior: "smooth",
      });
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : copy.errorReschedule
      );
    } finally {
      operationLock.current = false;
      setLoadingAvailability(false);
    }
  }

  /* =======================================================
     RENDER
  ======================================================== */

  const showLoading = accessState === "loading";
  const showBooking =
    booking !== null &&
    (accessState === "ready" ||
      accessState === "cancelled");

  const bookingIsCancelled =
    booking !== null && isCancelled(booking);

  const canCancel =
    accessState === "ready" &&
    booking?.onlineCancellationEnabled === true &&
    booking?.cancellationAllowed === true;

  const canReschedule =
    accessState === "ready" &&
    booking?.onlineReschedulingEnabled === true &&
    booking?.reschedulingAllowed === true;

  return (
    <div className="min-h-screen bg-(--p-f8f5f0) text-(--p-2d2723)">
      <SiteHeader />

      <main>
        {/* HERO */}

        <section className="border-b border-(--p-3a312c)/10 bg-(--p-e9dfd4)">
          <div className="mx-auto w-full max-w-[1180px] px-5 py-14 sm:px-8 sm:py-16 lg:px-12">
            <Link
              href="/reservar"
              className="inline-flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.18em] text-(--p-856f5f) transition hover:text-(--p-392f29)"
            >
              {copy.backToBooking}
            </Link>

            <div className="mt-8 max-w-[760px]">
              <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-(--p-8b6d59)">
                {copy.eyebrow}
              </p>

              <h1 className="mt-4 font-serif text-[clamp(2.8rem,6vw,5.3rem)] leading-[0.95] tracking-[-0.04em] text-(--p-2b2521)">
                {copy.titleLead}
                <span className="block italic text-(--p-8b6d59)">
                  {copy.titleAccent}
                </span>
              </h1>

              <p className="mt-6 max-w-[620px] text-base leading-7 text-(--p-6b5f56) sm:text-lg">
                {copy.description}
              </p>
            </div>
          </div>
        </section>

        <section className="mx-auto w-full max-w-[1000px] px-5 py-10 sm:px-8 sm:py-14 lg:px-12 lg:py-16">
          {/* ERROR */}

          {error && (
            <div
              role="alert"
              className="mb-6 rounded-[1.25rem] border border-red-900/10 bg-red-50 px-5 py-4 text-sm leading-6 text-red-800"
            >
              {error}
            </div>
          )}

          {/* LOADING */}

          {showLoading && (
            <div className="flex min-h-[320px] items-center justify-center rounded-[2rem] border border-(--p-3b332e)/10 bg-white/55">
              <div className="flex items-center gap-3 text-sm text-(--p-756960)">
                <LoadingSpinner />
                {copy.loading}
              </div>
            </div>
          )}

          {/* MISSING OR INVALID CREDENTIAL */}

          {!showLoading &&
            !showBooking &&
            ["missing", "invalid", "error"].includes(accessState) && (
              <div className="mx-auto max-w-[680px] rounded-[2rem] border border-(--p-3b332e)/10 bg-white/60 p-8 text-center shadow-[0_22px_60px_rgba(57,47,41,0.06)] sm:p-10">
                <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-(--p-eee4da) text-xl text-(--p-5a493f)">
                  ◷
                </div>

                <h2 className="mt-6 font-serif text-3xl text-(--p-332b26)">
                  {accessState === "missing"
                    ? copy.missingTitle
                    : accessState === "invalid"
                      ? copy.invalidTitle
                      : copy.errorTitle}
                </h2>

                <p className="mx-auto mt-4 max-w-[520px] text-sm leading-7 text-(--p-756960)">
                  {accessState === "invalid"
                    ? copy.invalidBody
                    : copy.missingBody}
                </p>

                <p className="mx-auto mt-3 max-w-[500px] text-xs leading-6 text-(--p-91847b)">
                  {copy.credentialNote}
                </p>

                <Link
                  href="/reservar"
                  className="mt-7 inline-flex min-h-13 items-center justify-center rounded-full bg-(--p-302924) px-7 py-3 text-xs font-semibold uppercase tracking-[0.15em] transition hover:bg-(--p-4b3d34)"
                  style={{ color: "var(--p-ffffff)" }}
                >
                  {copy.bookCta}
                </Link>
              </div>
            )}

          {/* BOOKING */}

          {showBooking && booking && (
            <div className="space-y-6">
              {/* SUMMARY */}

              <div className="rounded-[2rem] border border-(--p-3b332e)/10 bg-white/60 p-7 shadow-[0_22px_60px_rgba(57,47,41,0.06)] sm:p-9">
                <div className="flex flex-col gap-5 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <p className="text-[10px] font-semibold uppercase tracking-[0.2em] text-(--p-967b68)">
                      {copy.summaryEyebrow}
                    </p>

                    <h2 className="mt-3 font-serif text-3xl text-(--p-332b26) sm:text-4xl">
                      {booking.serviceName}
                    </h2>

                    <p className="mt-2 text-sm text-(--p-756960)">
                      {copy.withProfessional} {booking.professionalName}
                    </p>
                  </div>

                  <span className="w-fit rounded-full bg-(--p-eee4da) px-4 py-2 text-[10px] font-semibold uppercase tracking-[0.12em] text-(--p-6f5b4d)">
                    {getStatusLabel(booking.status)}
                  </span>
                </div>

                <div className="mt-8 grid gap-5 border-t border-(--p-3b332e)/10 pt-6 sm:grid-cols-2">
                  <div>
                    <p className="text-[10px] font-semibold uppercase tracking-[0.15em] text-(--p-9a887b)">
                      {copy.dateTimeLabel}
                    </p>
                    <p className="mt-2 capitalize text-sm font-medium leading-6 text-(--p-433831)">
                      {formatDateTime(booking.startAt)}
                    </p>
                  </div>

                  <div>
                    <p className="text-[10px] font-semibold uppercase tracking-[0.15em] text-(--p-9a887b)">
                      {copy.durationLabel}
                    </p>
                    <p className="mt-2 text-sm font-medium text-(--p-433831)">
                      {booking.serviceDurationMinutes} {copy.minutes}
                    </p>
                  </div>

                  <div>
                    <p className="text-[10px] font-semibold uppercase tracking-[0.15em] text-(--p-9a887b)">
                      {copy.customerLabel}
                    </p>
                    <p className="mt-2 text-sm font-medium text-(--p-433831)">
                      {booking.customerName}
                    </p>
                  </div>

                  <div>
                    <p className="text-[10px] font-semibold uppercase tracking-[0.15em] text-(--p-9a887b)">
                      {copy.referenceLabel}
                    </p>
                    <p className="mt-2 text-sm font-medium text-(--p-433831)">
                      {copy.appointmentRef}{booking.appointmentId}
                    </p>
                  </div>
                </div>
              </div>

              {/* ACTIONS */}

              {!bookingIsCancelled && (
                <div className="grid gap-4 md:grid-cols-2">
                  {/* RESCHEDULE CARD */}

                  <div className="rounded-[1.75rem] border border-(--p-3b332e)/10 bg-white/55 p-6">
                    <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-(--p-927663)">
                      {copy.rescheduleEyebrow}
                    </p>

                    <h3 className="mt-2 font-serif text-2xl text-(--p-342c27)">
                      {copy.rescheduleTitle}
                    </h3>

                    <p className="mt-3 text-sm leading-6 text-(--p-786b62)">
                      {copy.rescheduleBody}
                    </p>

                    {!booking.onlineReschedulingEnabled ? (
                      <p className="mt-5 rounded-xl bg-(--p-f1ebe5) p-4 text-xs leading-5 text-(--p-7c6e64)">
                        {copy.rescheduleDisabled}
                      </p>
                    ) : !canReschedule ? (
                      <p className="mt-5 rounded-xl bg-(--p-f1ebe5) p-4 text-xs leading-5 text-(--p-7c6e64)">
                        {copy.rescheduleClosed}
                      </p>
                    ) : (
                      <button
                        type="button"
                        disabled={cancelling || loadingAvailability}
                        onClick={() => {
                          setRescheduling(true);
                          setShowCancelConfirm(false);
                          setError(null);
                        }}
                        className="mt-6 inline-flex h-12 items-center justify-center rounded-full bg-(--p-302924) px-6 text-[11px] font-semibold uppercase tracking-[0.14em] text-white transition hover:bg-(--p-4b3d34) disabled:opacity-50"
                      >
                        {copy.rescheduleCta}
                      </button>
                    )}
                  </div>

                  {/* CANCELLATION CARD */}

                  <div className="rounded-[1.75rem] border border-(--p-3b332e)/10 bg-white/55 p-6">
                    <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-(--p-927663)">
                      {copy.cancelEyebrow}
                    </p>

                    <h3 className="mt-2 font-serif text-2xl text-(--p-342c27)">
                      {copy.cancelTitle}
                    </h3>

                    <p className="mt-3 text-sm leading-6 text-(--p-786b62)">
                      {copy.cancelBody}
                    </p>

                    {!booking.onlineCancellationEnabled ? (
                      <p className="mt-5 rounded-xl bg-(--p-f1ebe5) p-4 text-xs leading-5 text-(--p-7c6e64)">
                        {copy.cancelDisabled}
                      </p>
                    ) : !canCancel ? (
                      <p className="mt-5 rounded-xl bg-(--p-f1ebe5) p-4 text-xs leading-5 text-(--p-7c6e64)">
                        {copy.cancelClosed}
                      </p>
                    ) : (
                      <button
                        type="button"
                        disabled={cancelling || loadingAvailability}
                        onClick={() => {
                          setShowCancelConfirm(true);
                          setRescheduling(false);
                          availabilitySequence.current += 1;
                          setLoadingAvailability(false);
                          setError(null);
                        }}
                        className="mt-6 inline-flex h-12 items-center justify-center rounded-full border border-(--p-7d4e45)/25 bg-(--p-fff9f7) px-6 text-[11px] font-semibold uppercase tracking-[0.14em] text-(--p-7a463e) transition hover:bg-(--p-f7eae6) disabled:opacity-50"
                      >
                        {copy.cancelCta}
                      </button>
                    )}
                  </div>
                </div>
              )}

              {/* CANCELLATION CONFIRMATION */}

              {showCancelConfirm && canCancel && (
                <div className="rounded-[1.75rem] border border-(--p-7d4e45)/15 bg-(--p-fff8f5) p-6">
                  <h3 className="font-serif text-2xl text-(--p-4f332d)">
                    {copy.confirmCancelTitle}
                  </h3>

                  <p className="mt-3 max-w-[600px] text-sm leading-6 text-(--p-80665f)">
                    {copy.confirmCancelBody}
                  </p>

                  <div className="mt-6 flex flex-col gap-3 sm:flex-row">
                    <button
                      type="button"
                      disabled={cancelling}
                      onClick={() => void cancelBooking()}
                      className="inline-flex h-12 items-center justify-center gap-2 rounded-full bg-(--p-70453d) px-6 text-[11px] font-semibold uppercase tracking-[0.14em] text-white transition hover:bg-(--p-5d3933) disabled:opacity-50"
                    >
                      {cancelling && <LoadingSpinner />}
                      {cancelling ? copy.cancelling : copy.confirmCancel}
                    </button>

                    <button
                      type="button"
                      disabled={cancelling}
                      onClick={() => setShowCancelConfirm(false)}
                      className="inline-flex h-12 items-center justify-center rounded-full border border-(--p-3b332e)/12 bg-white px-6 text-[11px] font-semibold uppercase tracking-[0.14em] text-(--p-544940) disabled:opacity-50"
                    >
                      {copy.back}
                    </button>
                  </div>
                </div>
              )}

              {/* RESCHEDULE FLOW */}

              {rescheduling && canReschedule && (
                <div
                  ref={rescheduleSectionRef}
                  tabIndex={-1}
                  aria-label={copy.rescheduleRegionLabel}
                  className="scroll-mt-24 rounded-[2rem] border border-(--p-3b332e)/10 bg-white/60 p-6 outline-none sm:p-8"
                >
                  <div className="mb-7">
                    <p className="text-[10px] font-semibold uppercase tracking-[0.18em] text-(--p-927663)">
                      {copy.newDateEyebrow}
                    </p>

                    <h3 className="mt-2 font-serif text-3xl text-(--p-342c27)">
                      {copy.rescheduleFlowTitle}
                    </h3>

                    <p className="mt-2 text-sm text-(--p-7d7067)">
                      {booking.serviceName} {copy.withProfessional}{" "}
                      {booking.professionalName}
                    </p>
                  </div>

                  <div className="grid gap-6 lg:grid-cols-[0.95fr_1.05fr]">
                    {/* DATE */}

                    <div className="rounded-[1.5rem] border border-(--p-3b332e)/10 bg-(--p-fffdfb) p-5">
                      <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-(--p-79685c)">
                        {copy.dateLabel}
                      </p>

                      <CalendarDatePicker
                        value={rescheduleDate}
                        minDate={today}
                        maxDate={maximumDate}
                        disabled={loadingAvailability}
                        onChange={date =>
                          void loadAvailability(date)
                        }
                      />
                    </div>

                    {/* AVAILABLE SLOTS */}

                    <div className="rounded-[1.5rem] border border-(--p-3b332e)/10 bg-(--p-fffdfb) p-5">
                      <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-(--p-79685c)">
                        {copy.timesLabel}
                      </p>

                      {!rescheduleDate ? (
                        <p className="mt-5 text-sm leading-6 text-(--p-85786f)">
                          {copy.pickDateHint}
                        </p>
                      ) : loadingAvailability ? (
                        <div className="mt-6 flex items-center gap-3 text-sm text-(--p-756960)">
                          <LoadingSpinner />
                          {copy.loadingAvailability}
                        </div>
                      ) : !availability ||
                        !availability.available ||
                        availability.slots.length === 0 ? (
                        <div className="mt-5 rounded-xl bg-(--p-f2ebe4) p-4">
                          <p className="text-sm text-(--p-74675e)">
                            {copy.emptyTimes}
                          </p>
                        </div>
                      ) : (
                        <>
                          <p className="mt-3 text-xs capitalize text-(--p-8c7e74)">
                            {formatDate(rescheduleDate)}
                          </p>

                          <div className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-3">
                            {availability.slots.map(slot => {
                              const selected =
                                selectedSlot?.startAt ===
                                slot.startAt;

                              return (
                                <button
                                  key={slot.startAt}
                                  type="button"
                                  aria-pressed={selected}
                                  onClick={() =>
                                    setSelectedSlot(slot)
                                  }
                                  className={[
                                    "rounded-xl border px-4 py-3 text-sm font-semibold transition",
                                    selected
                                      ? "border-(--p-302924) bg-(--p-302924) text-white"
                                      : "border-(--p-3b332e)/12 bg-white text-(--p-4b4038) hover:bg-(--p-f2e9e1)",
                                  ].join(" ")}
                                >
                                  {formatTime(slot.startTime)}
                                </button>
                              );
                            })}
                          </div>
                        </>
                      )}
                    </div>
                  </div>

                  <div className="mt-7 flex flex-col justify-end gap-3 sm:flex-row">
                    <button
                      type="button"
                      disabled={loadingAvailability}
                      onClick={() => {
                        availabilitySequence.current += 1;
                        setRescheduling(false);
                        setRescheduleDate("");
                        setAvailability(null);
                        setSelectedSlot(null);
                        setLoadingAvailability(false);
                      }}
                      className="inline-flex h-12 items-center justify-center rounded-full border border-(--p-3b332e)/12 bg-white px-6 text-[11px] font-semibold uppercase tracking-[0.14em] text-(--p-544940) disabled:opacity-50"
                    >
                      {copy.cancelChange}
                    </button>

                    <button
                      type="button"
                      disabled={
                        !selectedSlot ||
                        loadingAvailability ||
                        !canReschedule
                      }
                      onClick={() => void confirmReschedule()}
                      className="inline-flex h-12 items-center justify-center gap-2 rounded-full bg-(--p-302924) px-7 text-[11px] font-semibold uppercase tracking-[0.14em] text-white transition hover:bg-(--p-4b3d34) disabled:cursor-not-allowed disabled:opacity-35"
                    >
                      {loadingAvailability && <LoadingSpinner />}
                      {copy.confirmNewTime}
                    </button>
                  </div>
                </div>
              )}

              {/* CANCELLED STATE */}

              {bookingIsCancelled && (
                <div className="rounded-[1.75rem] border border-(--p-3b332e)/10 bg-(--p-eee4da) p-7 text-center shadow-[0_16px_40px_rgba(57,47,41,0.05)] sm:p-8">
                  <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-white/75 text-lg font-semibold text-(--p-6f5b4d) shadow-sm">
                    ✓
                  </div>

                  <p className="mt-5 text-[10px] font-semibold uppercase tracking-[0.16em] text-(--p-8d7463)">
                    {copy.cancelledEyebrow}
                  </p>

                  <h3 className="mt-3 font-serif text-3xl text-(--p-342c27)">
                    {copy.cancelledTitle}
                  </h3>

                  <p className="mx-auto mt-3 max-w-[520px] text-sm leading-6 text-(--p-756960)">
                    {copy.cancelledBody}
                  </p>

                  <div className="mt-7 flex flex-col items-center justify-center gap-3 sm:flex-row">
                    <Link
                      href="/"
                      className="inline-flex h-12 min-w-[168px] items-center justify-center rounded-full border border-(--p-3b332e)/15 bg-white px-6 text-[11px] font-semibold uppercase tracking-[0.14em] text-(--p-4f433b) transition hover:bg-(--p-f8f3ee)"
                    >
                      {copy.backHomePrefix} {business.brand.shortName}
                    </Link>

                    <Link
                      href="/reservar"
                      className="inline-flex h-12 min-w-[190px] items-center justify-center rounded-full bg-(--p-302924) px-6 text-[11px] font-semibold uppercase tracking-[0.14em] shadow-[0_10px_24px_rgba(48,41,36,0.16)] transition hover:bg-(--p-4b3d34)"
                      style={{ color: "var(--p-ffffff)" }}
                    >
                      {copy.bookAnother}
                    </Link>
                  </div>
                </div>
              )}
            </div>
          )}
        </section>
      </main>

      <SiteFooter />
    </div>
  );
}
