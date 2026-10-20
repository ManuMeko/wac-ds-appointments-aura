
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
    return "Fecha no disponible";
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
      return "Pendiente de confirmación";
    case "CONFIRMED":
      return "Confirmada";
    case "COMPLETED":
      return "Completada";
    case "CANCELLED":
      return "Cancelada";
    case "NO_SHOW":
      return "No asistió";
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
          aria-label="Mes anterior"
          disabled={disabled || !canGoPrevious}
          onClick={() =>
            setVisibleMonth(current => addMonths(current, -1))
          }
          className="flex h-10 w-10 items-center justify-center rounded-full border border-[#3b332e]/10 bg-[#f8f3ee] text-xl text-[#5a4a40] transition hover:bg-[#eadfd4] disabled:cursor-not-allowed disabled:opacity-25"
        >
          ‹
        </button>

        <div className="text-center">
          <p className="font-serif text-xl capitalize text-[#342c27]">
            {monthLabel}
          </p>
          <p className="mt-0.5 text-[9px] uppercase tracking-[0.18em] text-[#9a887c]">
            Selecciona un día
          </p>
        </div>

        <button
          type="button"
          aria-label="Mes siguiente"
          disabled={disabled || !canGoNext}
          onClick={() =>
            setVisibleMonth(current => addMonths(current, 1))
          }
          className="flex h-10 w-10 items-center justify-center rounded-full border border-[#3b332e]/10 bg-[#f8f3ee] text-xl text-[#5a4a40] transition hover:bg-[#eadfd4] disabled:cursor-not-allowed disabled:opacity-25"
        >
          ›
        </button>
      </div>

      <div className="mt-6 grid grid-cols-7 gap-1">
        {WEEKDAYS.map((weekday, index) => (
          <div
            key={`${weekday}-${index}`}
            className="flex h-8 items-center justify-center text-[9px] font-semibold uppercase tracking-[0.13em] text-[#a19085]"
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
                  ? "bg-[#302924] text-white shadow-[0_8px_18px_rgba(48,41,36,0.22)]"
                  : "",
                !isSelected && isToday
                  ? "border border-[#9d7f6a]/50 bg-[#efe5db] text-[#54453c]"
                  : "",
                !isSelected && !isToday && !isDisabled
                  ? "text-[#51453d] hover:bg-[#eadfd4]"
                  : "",
                isDisabled
                  ? "cursor-not-allowed text-[#d0c6bf]"
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
              "No se pudo recuperar la cita."
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
            "La credencial no corresponde a la reserva solicitada."
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
            : "No se pudo recuperar la cita."
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
            "No se pudo consultar la disponibilidad."
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
          "La disponibilidad recibida no corresponde a la selección."
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
          : "No se pudo consultar la disponibilidad."
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
      setError("La sesión de gestión no corresponde a esta cita.");
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
            "No se pudo cancelar la cita."
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
          : "No se pudo cancelar la cita."
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
      setError("La sesión de gestión no corresponde a esta cita.");
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
            "No se pudo reprogramar la cita."
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
          "El cambio se ha enviado, pero no se pudo verificar la información actualizada. Recarga la página antes de intentarlo de nuevo."
        );
      }

      const refreshed: unknown = await refreshResponse.json();

      if (
        !isValidBookingResponse(refreshed) ||
        refreshed.appointmentId !== expectedAppointmentId
      ) {
        throw new Error(
          "La respuesta no corresponde a la cita solicitada."
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
          : "No se pudo reprogramar la cita."
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
    <div className="min-h-screen bg-[#f8f5f0] text-[#2d2723]">
      <SiteHeader />

      <main>
        {/* HERO */}

        <section className="border-b border-[#3a312c]/10 bg-[#e9dfd4]">
          <div className="mx-auto w-full max-w-[1180px] px-5 py-14 sm:px-8 sm:py-16 lg:px-12">
            <Link
              href="/reservar"
              className="inline-flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.18em] text-[#856f5f] transition hover:text-[#392f29]"
            >
              ← Volver a reservas
            </Link>

            <div className="mt-8 max-w-[760px]">
              <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-[#8b6d59]">
                Reserva online
              </p>

              <h1 className="mt-4 font-serif text-[clamp(2.8rem,6vw,5.3rem)] leading-[0.95] tracking-[-0.04em] text-[#2b2521]">
                Gestiona tu
                <span className="block italic text-[#8b6d59]">
                  cita.
                </span>
              </h1>

              <p className="mt-6 max-w-[620px] text-base leading-7 text-[#6b5f56] sm:text-lg">
                Consulta los detalles de tu reserva y,
                cuando esté permitido, cambia el horario
                o cancélala.
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
            <div className="flex min-h-[320px] items-center justify-center rounded-[2rem] border border-[#3b332e]/10 bg-white/55">
              <div className="flex items-center gap-3 text-sm text-[#756960]">
                <LoadingSpinner />
                Recuperando tu cita...
              </div>
            </div>
          )}

          {/* MISSING OR INVALID CREDENTIAL */}

          {!showLoading &&
            !showBooking &&
            ["missing", "invalid", "error"].includes(accessState) && (
              <div className="mx-auto max-w-[680px] rounded-[2rem] border border-[#3b332e]/10 bg-white/60 p-8 text-center shadow-[0_22px_60px_rgba(57,47,41,0.06)] sm:p-10">
                <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-[#eee4da] text-xl text-[#5a493f]">
                  ◷
                </div>

                <h2 className="mt-6 font-serif text-3xl text-[#332b26]">
                  {accessState === "missing"
                    ? "No encontramos una cita accesible"
                    : accessState === "invalid"
                      ? "El acceso a esta cita no está disponible"
                      : "No pudimos cargar tu cita"}
                </h2>

                <p className="mx-auto mt-4 max-w-[520px] text-sm leading-7 text-[#756960]">
                  {accessState === "invalid"
                    ? "El enlace de gestión puede haber caducado, haber sido revocado o no corresponder a esta reserva. Por seguridad, no podemos mostrar otra cita."
                    : "Para gestionar una reserva necesitas acceder desde su enlace privado o desde el navegador donde realizaste la reserva, siempre que la credencial siga vigente."}
                </p>

                <p className="mx-auto mt-3 max-w-[500px] text-xs leading-6 text-[#91847b]">
                  Cada credencial permite acceder únicamente
                  a la reserva para la que fue emitida.
                </p>

                <Link
                  href="/reservar"
                  className="mt-7 inline-flex min-h-13 items-center justify-center rounded-full bg-[#302924] px-7 py-3 text-xs font-semibold uppercase tracking-[0.15em] transition hover:bg-[#4b3d34]"
                  style={{ color: "#ffffff" }}
                >
                  Reservar una cita
                </Link>
              </div>
            )}

          {/* BOOKING */}

          {showBooking && booking && (
            <div className="space-y-6">
              {/* SUMMARY */}

              <div className="rounded-[2rem] border border-[#3b332e]/10 bg-white/60 p-7 shadow-[0_22px_60px_rgba(57,47,41,0.06)] sm:p-9">
                <div className="flex flex-col gap-5 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <p className="text-[10px] font-semibold uppercase tracking-[0.2em] text-[#967b68]">
                      Tu reserva
                    </p>

                    <h2 className="mt-3 font-serif text-3xl text-[#332b26] sm:text-4xl">
                      {booking.serviceName}
                    </h2>

                    <p className="mt-2 text-sm text-[#756960]">
                      con {booking.professionalName}
                    </p>
                  </div>

                  <span className="w-fit rounded-full bg-[#eee4da] px-4 py-2 text-[10px] font-semibold uppercase tracking-[0.12em] text-[#6f5b4d]">
                    {getStatusLabel(booking.status)}
                  </span>
                </div>

                <div className="mt-8 grid gap-5 border-t border-[#3b332e]/10 pt-6 sm:grid-cols-2">
                  <div>
                    <p className="text-[10px] font-semibold uppercase tracking-[0.15em] text-[#9a887b]">
                      Fecha y hora
                    </p>
                    <p className="mt-2 capitalize text-sm font-medium leading-6 text-[#433831]">
                      {formatDateTime(booking.startAt)}
                    </p>
                  </div>

                  <div>
                    <p className="text-[10px] font-semibold uppercase tracking-[0.15em] text-[#9a887b]">
                      Duración
                    </p>
                    <p className="mt-2 text-sm font-medium text-[#433831]">
                      {booking.serviceDurationMinutes} minutos
                    </p>
                  </div>

                  <div>
                    <p className="text-[10px] font-semibold uppercase tracking-[0.15em] text-[#9a887b]">
                      Cliente
                    </p>
                    <p className="mt-2 text-sm font-medium text-[#433831]">
                      {booking.customerName}
                    </p>
                  </div>

                  <div>
                    <p className="text-[10px] font-semibold uppercase tracking-[0.15em] text-[#9a887b]">
                      Referencia
                    </p>
                    <p className="mt-2 text-sm font-medium text-[#433831]">
                      Cita #{booking.appointmentId}
                    </p>
                  </div>
                </div>
              </div>

              {/* ACTIONS */}

              {!bookingIsCancelled && (
                <div className="grid gap-4 md:grid-cols-2">
                  {/* RESCHEDULE CARD */}

                  <div className="rounded-[1.75rem] border border-[#3b332e]/10 bg-white/55 p-6">
                    <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-[#927663]">
                      Reprogramar
                    </p>

                    <h3 className="mt-2 font-serif text-2xl text-[#342c27]">
                      Cambiar fecha y hora
                    </h3>

                    <p className="mt-3 text-sm leading-6 text-[#786b62]">
                      Puedes elegir otro hueco disponible
                      manteniendo el mismo tratamiento
                      y profesional.
                    </p>

                    {!booking.onlineReschedulingEnabled ? (
                      <p className="mt-5 rounded-xl bg-[#f1ebe5] p-4 text-xs leading-5 text-[#7c6e64]">
                        La reprogramación online no está
                        habilitada para este negocio.
                      </p>
                    ) : !canReschedule ? (
                      <p className="mt-5 rounded-xl bg-[#f1ebe5] p-4 text-xs leading-5 text-[#7c6e64]">
                        Esta cita ya no puede reprogramarse online.
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
                        className="mt-6 inline-flex h-12 items-center justify-center rounded-full bg-[#302924] px-6 text-[11px] font-semibold uppercase tracking-[0.14em] text-white transition hover:bg-[#4b3d34] disabled:opacity-50"
                      >
                        Cambiar horario
                      </button>
                    )}
                  </div>

                  {/* CANCELLATION CARD */}

                  <div className="rounded-[1.75rem] border border-[#3b332e]/10 bg-white/55 p-6">
                    <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-[#927663]">
                      Cancelación
                    </p>

                    <h3 className="mt-2 font-serif text-2xl text-[#342c27]">
                      Cancelar la cita
                    </h3>

                    <p className="mt-3 text-sm leading-6 text-[#786b62]">
                      La cancelación es definitiva. El horario
                      volverá a quedar libre.
                    </p>

                    {!booking.onlineCancellationEnabled ? (
                      <p className="mt-5 rounded-xl bg-[#f1ebe5] p-4 text-xs leading-5 text-[#7c6e64]">
                        La cancelación online no está habilitada.
                      </p>
                    ) : !canCancel ? (
                      <p className="mt-5 rounded-xl bg-[#f1ebe5] p-4 text-xs leading-5 text-[#7c6e64]">
                        Esta cita ya no puede cancelarse online.
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
                        className="mt-6 inline-flex h-12 items-center justify-center rounded-full border border-[#7d4e45]/25 bg-[#fff9f7] px-6 text-[11px] font-semibold uppercase tracking-[0.14em] text-[#7a463e] transition hover:bg-[#f7eae6] disabled:opacity-50"
                      >
                        Cancelar cita
                      </button>
                    )}
                  </div>
                </div>
              )}

              {/* CANCELLATION CONFIRMATION */}

              {showCancelConfirm && canCancel && (
                <div className="rounded-[1.75rem] border border-[#7d4e45]/15 bg-[#fff8f5] p-6">
                  <h3 className="font-serif text-2xl text-[#4f332d]">
                    ¿Cancelar esta cita?
                  </h3>

                  <p className="mt-3 max-w-[600px] text-sm leading-6 text-[#80665f]">
                    Esta acción no se puede deshacer desde esta página.
                  </p>

                  <div className="mt-6 flex flex-col gap-3 sm:flex-row">
                    <button
                      type="button"
                      disabled={cancelling}
                      onClick={() => void cancelBooking()}
                      className="inline-flex h-12 items-center justify-center gap-2 rounded-full bg-[#70453d] px-6 text-[11px] font-semibold uppercase tracking-[0.14em] text-white transition hover:bg-[#5d3933] disabled:opacity-50"
                    >
                      {cancelling && <LoadingSpinner />}
                      {cancelling ? "Cancelando..." : "Sí, cancelar"}
                    </button>

                    <button
                      type="button"
                      disabled={cancelling}
                      onClick={() => setShowCancelConfirm(false)}
                      className="inline-flex h-12 items-center justify-center rounded-full border border-[#3b332e]/12 bg-white px-6 text-[11px] font-semibold uppercase tracking-[0.14em] text-[#544940] disabled:opacity-50"
                    >
                      Volver
                    </button>
                  </div>
                </div>
              )}

              {/* RESCHEDULE FLOW */}

              {rescheduling && canReschedule && (
                <div
                  ref={rescheduleSectionRef}
                  tabIndex={-1}
                  aria-label="Reprogramar cita"
                  className="scroll-mt-24 rounded-[2rem] border border-[#3b332e]/10 bg-white/60 p-6 outline-none sm:p-8"
                >
                  <div className="mb-7">
                    <p className="text-[10px] font-semibold uppercase tracking-[0.18em] text-[#927663]">
                      Nueva fecha
                    </p>

                    <h3 className="mt-2 font-serif text-3xl text-[#342c27]">
                      Reprogramar tu cita
                    </h3>

                    <p className="mt-2 text-sm text-[#7d7067]">
                      {booking.serviceName} con{" "}
                      {booking.professionalName}
                    </p>
                  </div>

                  <div className="grid gap-6 lg:grid-cols-[0.95fr_1.05fr]">
                    {/* DATE */}

                    <div className="rounded-[1.5rem] border border-[#3b332e]/10 bg-[#fffdfb] p-5">
                      <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-[#79685c]">
                        Fecha
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

                    <div className="rounded-[1.5rem] border border-[#3b332e]/10 bg-[#fffdfb] p-5">
                      <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-[#79685c]">
                        Horarios
                      </p>

                      {!rescheduleDate ? (
                        <p className="mt-5 text-sm leading-6 text-[#85786f]">
                          Selecciona una fecha para consultar
                          la disponibilidad.
                        </p>
                      ) : loadingAvailability ? (
                        <div className="mt-6 flex items-center gap-3 text-sm text-[#756960]">
                          <LoadingSpinner />
                          Consultando...
                        </div>
                      ) : !availability ||
                        !availability.available ||
                        availability.slots.length === 0 ? (
                        <div className="mt-5 rounded-xl bg-[#f2ebe4] p-4">
                          <p className="text-sm text-[#74675e]">
                            No hay horarios disponibles
                            para esta fecha.
                          </p>
                        </div>
                      ) : (
                        <>
                          <p className="mt-3 text-xs capitalize text-[#8c7e74]">
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
                                      ? "border-[#302924] bg-[#302924] text-white"
                                      : "border-[#3b332e]/12 bg-white text-[#4b4038] hover:bg-[#f2e9e1]",
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
                      className="inline-flex h-12 items-center justify-center rounded-full border border-[#3b332e]/12 bg-white px-6 text-[11px] font-semibold uppercase tracking-[0.14em] text-[#544940] disabled:opacity-50"
                    >
                      Cancelar cambio
                    </button>

                    <button
                      type="button"
                      disabled={
                        !selectedSlot ||
                        loadingAvailability ||
                        !canReschedule
                      }
                      onClick={() => void confirmReschedule()}
                      className="inline-flex h-12 items-center justify-center gap-2 rounded-full bg-[#302924] px-7 text-[11px] font-semibold uppercase tracking-[0.14em] text-white transition hover:bg-[#4b3d34] disabled:cursor-not-allowed disabled:opacity-35"
                    >
                      {loadingAvailability && <LoadingSpinner />}
                      Confirmar nuevo horario
                    </button>
                  </div>
                </div>
              )}

              {/* CANCELLED STATE */}

              {bookingIsCancelled && (
                <div className="rounded-[1.75rem] border border-[#3b332e]/10 bg-[#eee4da] p-7 text-center shadow-[0_16px_40px_rgba(57,47,41,0.05)] sm:p-8">
                  <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-white/75 text-lg font-semibold text-[#6f5b4d] shadow-sm">
                    ✓
                  </div>

                  <p className="mt-5 text-[10px] font-semibold uppercase tracking-[0.16em] text-[#8d7463]">
                    Reserva cancelada
                  </p>

                  <h3 className="mt-3 font-serif text-3xl text-[#342c27]">
                    Esta cita ya no está activa
                  </h3>

                  <p className="mx-auto mt-3 max-w-[520px] text-sm leading-6 text-[#756960]">
                    El horario ha quedado liberado.
                    Si quieres volver a visitarnos,
                    puedes crear una nueva reserva.
                  </p>

                  <div className="mt-7 flex flex-col items-center justify-center gap-3 sm:flex-row">
                    <Link
                      href="/"
                      className="inline-flex h-12 min-w-[168px] items-center justify-center rounded-full border border-[#3b332e]/15 bg-white px-6 text-[11px] font-semibold uppercase tracking-[0.14em] text-[#4f433b] transition hover:bg-[#f8f3ee]"
                    >
                      Volver a {business.brand.shortName}
                    </Link>

                    <Link
                      href="/reservar"
                      className="inline-flex h-12 min-w-[190px] items-center justify-center rounded-full bg-[#302924] px-6 text-[11px] font-semibold uppercase tracking-[0.14em] shadow-[0_10px_24px_rgba(48,41,36,0.16)] transition hover:bg-[#4b3d34]"
                      style={{ color: "#ffffff" }}
                    >
                      Reservar otra cita
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
