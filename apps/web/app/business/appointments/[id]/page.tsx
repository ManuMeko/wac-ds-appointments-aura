"use client";

import Link from "next/link";
import {
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";
import {
  useParams,
} from "next/navigation";

import {
  ApiError,
  apiGet,
  apiPatch,
} from "@/lib/api";

/* =========================================================
   TYPES
========================================================= */

type AppointmentStatus =
  | "PENDING"
  | "CONFIRMED"
  | "CANCELLED"
  | "COMPLETED"
  | "NO_SHOW";

type AppointmentResponse = {
  id: number;

  businessId: number;

  customerId: number;
  customerName: string | null;
  customerEmail: string | null;
  customerPhone: string | null;

  professionalId: number;
  professionalName: string | null;

  serviceId: number;
  serviceName: string | null;
  serviceDurationMinutes: number | null;
  servicePrice: number | null;

  startAt: string;
  endAt: string;

  timezone: string | null;

  status: AppointmentStatus;

  notes: string | null;

  createdAt: string;
  updatedAt: string;
};

type AppointmentStatusUpdateRequest = {
  status: AppointmentStatus;
};

type AppointmentRescheduleRequest = {
  startAt: string;
};

type AvailabilitySlotResponse = {
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

  slots: AvailabilitySlotResponse[];
};

/* =========================================================
   CONSTANTS
========================================================= */

const ACCESS_TOKEN_STORAGE_KEY =
  "wacds_access_token";

const DEFAULT_TIMEZONE =
  "Europe/Madrid";

const statusLabels: Record<
  AppointmentStatus,
  string
> = {
  PENDING: "Pendiente",
  CONFIRMED: "Confirmada",
  CANCELLED: "Cancelada",
  COMPLETED: "Completada",
  NO_SHOW: "No asistió",
};

const statusDescriptions: Record<
  AppointmentStatus,
  string
> = {
  PENDING:
    "La reserva está pendiente de confirmación.",

  CONFIRMED:
    "La reserva está confirmada y preparada para realizarse.",

  CANCELLED:
    "La reserva ha sido cancelada.",

  COMPLETED:
    "El servicio ha sido completado.",

  NO_SHOW:
    "El cliente no se presentó a la cita.",
};

/* =========================================================
   STATUS STYLES
========================================================= */

function getStatusClass(
  status: AppointmentStatus,
): string {
  switch (status) {
    case "CONFIRMED":
      return "border-[#d7e6da] bg-[#edf5ef] text-[#4f7057]";

    case "PENDING":
      return "border-[#eadbc6] bg-[#faf2e7] text-[#906c3f]";

    case "COMPLETED":
      return "border-[#d8e1e8] bg-[#eef2f5] text-[#5d7282]";

    case "CANCELLED":
      return "border-[#ecd3d3] bg-[#faeeee] text-[#965b5b]";

    case "NO_SHOW":
      return "border-[#e1dcdc] bg-[#f2efef] text-[#746a6a]";
  }
}

function getStatusDotClass(
  status: AppointmentStatus,
): string {
  switch (status) {
    case "CONFIRMED":
      return "bg-[#719079]";

    case "PENDING":
      return "bg-[#b28a56]";

    case "COMPLETED":
      return "bg-[#718596]";

    case "CANCELLED":
      return "bg-[#b76f6f]";

    case "NO_SHOW":
      return "bg-[#817575]";
  }
}

/* =========================================================
   STORAGE
========================================================= */

function getStoredAccessToken():
  | string
  | null {
  if (
    typeof window === "undefined"
  ) {
    return null;
  }

  const token =
    window.localStorage.getItem(
      ACCESS_TOKEN_STORAGE_KEY,
    );

  if (
    token == null
      || token.trim() === ""
  ) {
    return null;
  }

  return token.trim();
}

/* =========================================================
   DATE / TIME HELPERS
========================================================= */

function formatDate(
  value: string,
  timezone?: string | null,
): string {
  try {
    return new Intl.DateTimeFormat(
      "es-ES",
      {
        weekday: "long",
        day: "numeric",
        month: "long",
        year: "numeric",
        timeZone:
          timezone?.trim()
          || DEFAULT_TIMEZONE,
      },
    ).format(
      new Date(
        value,
      ),
    );
  } catch {
    return "Fecha no disponible";
  }
}

function formatTime(
  value: string,
  timezone?: string | null,
): string {
  try {
    return new Intl.DateTimeFormat(
      "es-ES",
      {
        hour: "2-digit",
        minute: "2-digit",
        hour12: false,
        timeZone:
          timezone?.trim()
          || DEFAULT_TIMEZONE,
      },
    ).format(
      new Date(
        value,
      ),
    );
  } catch {
    return "--:--";
  }
}

function formatDateTime(
  value: string,
  timezone?: string | null,
): string {
  try {
    return new Intl.DateTimeFormat(
      "es-ES",
      {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
        hour12: false,
        timeZone:
          timezone?.trim()
          || DEFAULT_TIMEZONE,
      },
    ).format(
      new Date(
        value,
      ),
    );
  } catch {
    return "No disponible";
  }
}

function formatPrice(
  value: number | null,
): string {
  if (
    value === null
      || value === undefined
  ) {
    return "No disponible";
  }

  return new Intl.NumberFormat(
    "es-ES",
    {
      style: "currency",
      currency: "EUR",
    },
  ).format(
    value,
  );
}

function formatSlotTime(
  value: string,
): string {
  if (
    !value
  ) {
    return "--:--";
  }

  return value.slice(
    0,
    5,
  );
}

/* =========================================================
   DATE-ONLY HELPERS
========================================================= */

function parseDateValue(
  value: string,
): Date {
  return new Date(
    `${value}T12:00:00`,
  );
}

function toDateValue(
  date: Date,
): string {
  const year =
    String(
      date.getFullYear(),
    );

  const month =
    String(
      date.getMonth() + 1,
    ).padStart(
      2,
      "0",
    );

  const day =
    String(
      date.getDate(),
    ).padStart(
      2,
      "0",
    );

  return `${year}-${month}-${day}`;
}

function addDaysToDateValue(
  value: string,
  days: number,
): string {
  const date =
    parseDateValue(
      value,
    );

  date.setDate(
    date.getDate() + days,
  );

  return toDateValue(
    date,
  );
}

function formatInputDateLabel(
  value: string,
): string {
  if (
    !value
  ) {
    return "";
  }

  try {
    return new Intl.DateTimeFormat(
      "es-ES",
      {
        weekday: "long",
        day: "numeric",
        month: "long",
        year: "numeric",
      },
    ).format(
      parseDateValue(
        value,
      ),
    );
  } catch {
    return value;
  }
}

function formatCalendarWeekday(
  value: string,
): string {
  try {
    return new Intl.DateTimeFormat(
      "es-ES",
      {
        weekday: "short",
      },
    )
      .format(
        parseDateValue(
          value,
        ),
      )
      .replace(
        ".",
        "",
      )
      .toUpperCase();
  } catch {
    return "";
  }
}

function formatCalendarDay(
  value: string,
): string {
  try {
    return new Intl.DateTimeFormat(
      "es-ES",
      {
        day: "2-digit",
      },
    ).format(
      parseDateValue(
        value,
      ),
    );
  } catch {
    return "";
  }
}

function formatCalendarMonthShort(
  value: string,
): string {
  try {
    return new Intl.DateTimeFormat(
      "es-ES",
      {
        month: "short",
      },
    )
      .format(
        parseDateValue(
          value,
        ),
      )
      .replace(
        ".",
        "",
      )
      .toUpperCase();
  } catch {
    return "";
  }
}

function formatCalendarMonthYear(
  value: string,
): string {
  if (
    !value
  ) {
    return "";
  }

  try {
    const formatted =
      new Intl.DateTimeFormat(
        "es-ES",
        {
          month: "long",
          year: "numeric",
        },
      ).format(
        parseDateValue(
          value,
        ),
      );

    return (
      formatted.charAt(0).toUpperCase()
      + formatted.slice(1)
    );
  } catch {
    return "";
  }
}

function getTodayDateValue(
  timezone?: string | null,
): string {
  try {
    const formatter =
      new Intl.DateTimeFormat(
        "en-CA",
        {
          year: "numeric",
          month: "2-digit",
          day: "2-digit",
          timeZone:
            timezone?.trim()
            || DEFAULT_TIMEZONE,
        },
      );

    const parts =
      formatter.formatToParts(
        new Date(),
      );

    const year =
      parts.find(
        (part) =>
          part.type === "year",
      )?.value;

    const month =
      parts.find(
        (part) =>
          part.type === "month",
      )?.value;

    const day =
      parts.find(
        (part) =>
          part.type === "day",
      )?.value;

    if (
      !year
        || !month
        || !day
    ) {
      return "";
    }

    return `${year}-${month}-${day}`;
  } catch {
    return "";
  }
}

/**
 * Convierte un OffsetDateTime/ISO a YYYY-MM-DD
 * respetando la zona horaria del negocio.
 */
function toDateInputValue(
  value: string,
  timezone?: string | null,
): string {
  try {
    const formatter =
      new Intl.DateTimeFormat(
        "en-CA",
        {
          year: "numeric",
          month: "2-digit",
          day: "2-digit",
          timeZone:
            timezone?.trim()
            || DEFAULT_TIMEZONE,
        },
      );

    const parts =
      formatter.formatToParts(
        new Date(
          value,
        ),
      );

    const year =
      parts.find(
        (part) =>
          part.type === "year",
      )?.value;

    const month =
      parts.find(
        (part) =>
          part.type === "month",
      )?.value;

    const day =
      parts.find(
        (part) =>
          part.type === "day",
      )?.value;

    if (
      !year
        || !month
        || !day
    ) {
      return "";
    }

    return `${year}-${month}-${day}`;
  } catch {
    return "";
  }
}

/* =========================================================
   PARAMS
========================================================= */

function normalizeParamId(
  value: string | string[] | undefined,
): number | null {
  if (
    value == null
  ) {
    return null;
  }

  const rawValue =
    Array.isArray(
      value,
    )
      ? value[0]
      : value;

  const parsed =
    Number(
      rawValue,
    );

  if (
    !Number.isInteger(
      parsed,
    )
      || parsed <= 0
  ) {
    return null;
  }

  return parsed;
}

/* =========================================================
   ERRORS
========================================================= */

function resolveErrorMessage(
  error: unknown,
): string {
  if (
    error instanceof ApiError
  ) {
    if (
      error.status === 401
        || error.status === 403
    ) {
      return "La sesión Business no es válida o ha caducado.";
    }

    if (
      error.status === 404
    ) {
      return "La cita no existe o no pertenece a este negocio.";
    }

    if (
      error.status === 409
    ) {
      return (
        error.message
        || "La operación entra en conflicto con el estado actual de la cita."
      );
    }

    if (
      error.status === 400
    ) {
      return (
        error.message
        || "Los datos enviados no son válidos."
      );
    }

    return (
      error.message
      || "No se ha podido completar la operación."
    );
  }

  return "No se ha podido conectar con la API.";
}

/* =========================================================
   STATUS RULES
========================================================= */

function canConfirm(
  status: AppointmentStatus,
): boolean {
  return (
    status === "PENDING"
  );
}

function canComplete(
  status: AppointmentStatus,
): boolean {
  return (
    status === "CONFIRMED"
  );
}

function canMarkNoShow(
  status: AppointmentStatus,
): boolean {
  return (
    status === "CONFIRMED"
  );
}

function canCancel(
  status: AppointmentStatus,
): boolean {
  return (
    status === "PENDING"
      || status === "CONFIRMED"
  );
}

function canReschedule(
  status: AppointmentStatus,
): boolean {
  return (
    status === "PENDING"
      || status === "CONFIRMED"
  );
}

function isTerminalStatus(
  status: AppointmentStatus,
): boolean {
  return (
    status === "CANCELLED"
      || status === "COMPLETED"
      || status === "NO_SHOW"
  );
}

/* =========================================================
   COMPONENT
========================================================= */

export default function BusinessAppointmentDetailPage() {
  const params =
    useParams<{
      id: string;
    }>();

  const appointmentId =
    useMemo(
      () =>
        normalizeParamId(
          params?.id,
        ),
      [
        params?.id,
      ],
    );

  /* =======================================================
     APPOINTMENT STATE
  ======================================================= */

  const [
    appointment,
    setAppointment,
  ] =
    useState<
      AppointmentResponse | null
    >(
      null,
    );

  const [
    loading,
    setLoading,
  ] =
    useState(
      true,
    );

  const [
    error,
    setError,
  ] =
    useState<
      string | null
    >(
      null,
    );

  const [
    missingSession,
    setMissingSession,
  ] =
    useState(
      false,
    );

  const [
    statusUpdating,
    setStatusUpdating,
  ] =
    useState<
      AppointmentStatus | null
    >(
      null,
    );

  const [
    successMessage,
    setSuccessMessage,
  ] =
    useState<
      string | null
    >(
      null,
    );

  /* =======================================================
     RESCHEDULE STATE
  ======================================================= */

  const [
    rescheduleOpen,
    setRescheduleOpen,
  ] =
    useState(
      false,
    );

  const [
    rescheduleDate,
    setRescheduleDate,
  ] =
    useState(
      "",
    );

  const [
    calendarStartDate,
    setCalendarStartDate,
  ] =
    useState(
      "",
    );

  const [
    availability,
    setAvailability,
  ] =
    useState<
      DailyAvailabilityResponse | null
    >(
      null,
    );

  const [
    availabilityLoading,
    setAvailabilityLoading,
  ] =
    useState(
      false,
    );

  const [
    availabilityError,
    setAvailabilityError,
  ] =
    useState<
      string | null
    >(
      null,
    );

  const [
    selectedSlot,
    setSelectedSlot,
  ] =
    useState<
      AvailabilitySlotResponse | null
    >(
      null,
    );

  const [
    rescheduling,
    setRescheduling,
  ] =
    useState(
      false,
    );

  /* =======================================================
     CALENDAR COMPUTED
  ======================================================= */

  const calendarDays =
    useMemo(
      () => {
        if (
          !calendarStartDate
        ) {
          return [];
        }

        return Array.from(
          {
            length: 7,
          },
          (
            _,
            index,
          ) =>
            addDaysToDateValue(
              calendarStartDate,
              index,
            ),
        );
      },
      [
        calendarStartDate,
      ],
    );

  const todayDateValue =
    appointment
      ? getTodayDateValue(
          appointment.timezone,
        )
      : "";

  /* =======================================================
     LOAD APPOINTMENT
  ======================================================= */

  const loadAppointment =
    useCallback(
      async () => {
        if (
          appointmentId == null
        ) {
          setAppointment(
            null,
          );

          setError(
            "El identificador de la cita no es válido.",
          );

          setLoading(
            false,
          );

          return;
        }

        setLoading(
          true,
        );

        setError(
          null,
        );

        setMissingSession(
          false,
        );

        const accessToken =
          getStoredAccessToken();

        if (
          !accessToken
        ) {
          setAppointment(
            null,
          );

          setMissingSession(
            true,
          );

          setLoading(
            false,
          );

          return;
        }

        try {
          const response =
            await apiGet<
              AppointmentResponse
            >(
              `/api/v1/appointments/${appointmentId}`,
              {
                accessToken,
              },
            );

          setAppointment(
            response,
          );
        } catch (
          requestError
        ) {
          setAppointment(
            null,
          );

          if (
            requestError instanceof ApiError
              && (
                requestError.status === 401
                  || requestError.status === 403
              )
          ) {
            setMissingSession(
              true,
            );
          }

          setError(
            resolveErrorMessage(
              requestError,
            ),
          );
        } finally {
          setLoading(
            false,
          );
        }
      },
      [
        appointmentId,
      ],
    );

  useEffect(
    () => {
      void loadAppointment();
    },
    [
      loadAppointment,
    ],
  );

  /* =======================================================
     SUCCESS TOAST
  ======================================================= */

  useEffect(
    () => {
      if (
        !successMessage
      ) {
        return;
      }

      const timeout =
        window.setTimeout(
          () => {
            setSuccessMessage(
              null,
            );
          },
          4500,
        );

      return () => {
        window.clearTimeout(
          timeout,
        );
      };
    },
    [
      successMessage,
    ],
  );

  /* =======================================================
     RESCHEDULE MODAL UX
  ======================================================= */

  useEffect(
    () => {
      if (
        !rescheduleOpen
      ) {
        return;
      }

      const previousOverflow =
        document.body.style.overflow;

      document.body.style.overflow =
        "hidden";

      function handleKeyDown(
        event: KeyboardEvent,
      ) {
        if (
          event.key === "Escape"
            && !rescheduling
        ) {
          closeReschedulePanel();
        }
      }

      window.addEventListener(
        "keydown",
        handleKeyDown,
      );

      return () => {
        document.body.style.overflow =
          previousOverflow;

        window.removeEventListener(
          "keydown",
          handleKeyDown,
        );
      };
    },
    [
      rescheduleOpen,
      rescheduling,
    ],
  );

  /* =======================================================
     LOAD AVAILABILITY
  ======================================================= */

  useEffect(
    () => {
      if (
        !rescheduleOpen
          || !appointment
          || !rescheduleDate
      ) {
        return;
      }

      const currentAppointment =
        appointment;

      let cancelled =
        false;

      async function loadAvailability() {
        const accessToken =
          getStoredAccessToken();

        if (
          !accessToken
        ) {
          setMissingSession(
            true,
          );

          return;
        }

        setAvailabilityLoading(
          true,
        );

        setAvailabilityError(
          null,
        );

        setSelectedSlot(
          null,
        );

        setAvailability(
          null,
        );

        try {
          const query =
            new URLSearchParams(
              {
                professionalId:
                  String(
                    currentAppointment.professionalId,
                  ),

                serviceId:
                  String(
                    currentAppointment.serviceId,
                  ),

                date:
                  rescheduleDate,
              },
            );

          const response =
            await apiGet<
              DailyAvailabilityResponse
            >(
              `/api/v1/availability-slots?${query.toString()}`,
              {
                accessToken,
              },
            );

          if (
            cancelled
          ) {
            return;
          }

          setAvailability(
            response,
          );
        } catch (
          availabilityRequestError
        ) {
          if (
            cancelled
          ) {
            return;
          }

          if (
            availabilityRequestError instanceof ApiError
              && (
                availabilityRequestError.status === 401
                  || availabilityRequestError.status === 403
              )
          ) {
            setMissingSession(
              true,
            );
          }

          setAvailabilityError(
            resolveErrorMessage(
              availabilityRequestError,
            ),
          );
        } finally {
          if (
            !cancelled
          ) {
            setAvailabilityLoading(
              false,
            );
          }
        }
      }

      void loadAvailability();

      return () => {
        cancelled =
          true;
      };
    },
    [
      appointment,
      rescheduleDate,
      rescheduleOpen,
    ],
  );

  /* =======================================================
     STATUS UPDATE
  ======================================================= */

  async function updateAppointmentStatus(
    nextStatus: AppointmentStatus,
  ) {
    if (
      appointmentId == null
        || !appointment
    ) {
      return;
    }

    if (
      statusUpdating != null
        || rescheduling
    ) {
      return;
    }

    if (
      nextStatus === appointment.status
    ) {
      return;
    }

    if (
      nextStatus === "CANCELLED"
    ) {
      const confirmed =
        window.confirm(
          "¿Seguro que quieres cancelar esta cita?",
        );

      if (
        !confirmed
      ) {
        return;
      }
    }

    if (
      nextStatus === "NO_SHOW"
    ) {
      const confirmed =
        window.confirm(
          "¿Confirmas que el cliente no ha asistido a la cita?",
        );

      if (
        !confirmed
      ) {
        return;
      }
    }

    setStatusUpdating(
      nextStatus,
    );

    setError(
      null,
    );

    setSuccessMessage(
      null,
    );

    try {
      const accessToken =
        getStoredAccessToken();

      if (
        !accessToken
      ) {
        setMissingSession(
          true,
        );

        return;
      }

      const requestBody: AppointmentStatusUpdateRequest = {
        status:
          nextStatus,
      };

      const updatedAppointment =
        await apiPatch<
          AppointmentResponse
        >(
          `/api/v1/appointments/${appointmentId}/status`,
          requestBody,
          {
            accessToken,
          },
        );

      setAppointment(
        updatedAppointment,
      );

      setSuccessMessage(
        `Estado actualizado a ${statusLabels[nextStatus]}.`,
      );
    } catch (
      updateError
    ) {
      if (
        updateError instanceof ApiError
          && (
            updateError.status === 401
              || updateError.status === 403
          )
      ) {
        setMissingSession(
          true,
        );
      }

      setError(
        resolveErrorMessage(
          updateError,
        ),
      );
    } finally {
      setStatusUpdating(
        null,
      );
    }
  }

  /* =======================================================
     CALENDAR ACTIONS
  ======================================================= */

  function selectRescheduleDate(
    dateValue: string,
  ) {
    if (
      rescheduling
    ) {
      return;
    }

    if (
      todayDateValue
        && dateValue < todayDateValue
    ) {
      return;
    }

    setRescheduleDate(
      dateValue,
    );

    setSelectedSlot(
      null,
    );

    setAvailabilityError(
      null,
    );
  }

  function moveCalendar(
    days: number,
  ) {
    if (
      !calendarStartDate
        || rescheduling
    ) {
      return;
    }

    setCalendarStartDate(
      addDaysToDateValue(
        calendarStartDate,
        days,
      ),
    );
  }

  function goToToday() {
    if (
      !appointment
        || !todayDateValue
        || rescheduling
    ) {
      return;
    }

    setCalendarStartDate(
      todayDateValue,
    );

    setRescheduleDate(
      todayDateValue,
    );

    setSelectedSlot(
      null,
    );

    setAvailabilityError(
      null,
    );
  }

  /* =======================================================
     OPEN RESCHEDULE
  ======================================================= */

  function openReschedulePanel() {
    if (
      !appointment
        || !canReschedule(
          appointment.status,
        )
        || statusUpdating != null
        || rescheduling
    ) {
      return;
    }

    const initialDate =
      toDateInputValue(
        appointment.startAt,
        appointment.timezone,
      );

    setRescheduleDate(
      initialDate,
    );

    setCalendarStartDate(
      initialDate,
    );

    setAvailability(
      null,
    );

    setAvailabilityError(
      null,
    );

    setSelectedSlot(
      null,
    );

    setRescheduleOpen(
      true,
    );
  }

  /* =======================================================
     CLOSE RESCHEDULE
  ======================================================= */

  function closeReschedulePanel() {
    if (
      rescheduling
    ) {
      return;
    }

    setRescheduleOpen(
      false,
    );

    setAvailability(
      null,
    );

    setAvailabilityError(
      null,
    );

    setSelectedSlot(
      null,
    );
  }

  /* =======================================================
     CONFIRM RESCHEDULE
  ======================================================= */

  async function confirmReschedule() {
    if (
      appointmentId == null
        || !appointment
        || !selectedSlot
        || rescheduling
    ) {
      return;
    }

    if (
      selectedSlot.startAt
        === appointment.startAt
    ) {
      setAvailabilityError(
        "Selecciona un horario diferente al actual.",
      );

      return;
    }

    const accessToken =
      getStoredAccessToken();

    if (
      !accessToken
    ) {
      setMissingSession(
        true,
      );

      return;
    }

    setRescheduling(
      true,
    );

    setAvailabilityError(
      null,
    );

    setError(
      null,
    );

    setSuccessMessage(
      null,
    );

    try {
      const requestBody: AppointmentRescheduleRequest = {
        startAt:
          selectedSlot.startAt,
      };

      const updatedAppointment =
        await apiPatch<
          AppointmentResponse
        >(
          `/api/v1/appointments/${appointmentId}/reschedule`,
          requestBody,
          {
            accessToken,
          },
        );

      setAppointment(
        updatedAppointment,
      );

      setRescheduleOpen(
        false,
      );

      setAvailability(
        null,
      );

      setSelectedSlot(
        null,
      );

      setSuccessMessage(
        `Cita reprogramada para ${formatDate(
          updatedAppointment.startAt,
          updatedAppointment.timezone,
        )} a las ${formatTime(
          updatedAppointment.startAt,
          updatedAppointment.timezone,
        )}.`,
      );
    } catch (
      rescheduleError
    ) {
      if (
        rescheduleError instanceof ApiError
          && (
            rescheduleError.status === 401
              || rescheduleError.status === 403
          )
      ) {
        setMissingSession(
          true,
        );
      }

      setAvailabilityError(
        resolveErrorMessage(
          rescheduleError,
        ),
      );
    } finally {
      setRescheduling(
        false,
      );
    }
  }

  /* =======================================================
     RENDER
  ======================================================= */

  return (
    <>
      <div className="space-y-6 pb-10">
        {/* ===================================================
            HEADER
        =================================================== */}

        <section className="flex flex-col gap-5 xl:flex-row xl:items-end xl:justify-between">
          <div>
            <Link
              href="/business/appointments"
              className="inline-flex items-center gap-2 text-sm font-semibold text-[#786a61] transition hover:text-[#2e2824]"
            >
              <span aria-hidden="true">
                ←
              </span>

              <span>
                Volver a citas
              </span>
            </Link>

            <div className="mt-5 text-[11px] font-semibold uppercase tracking-[0.24em] text-[#a18b7d]">
              Agenda
            </div>

            <h1 className="mt-1 text-3xl font-semibold tracking-[-0.04em] text-[#2e2824] md:text-4xl">
              Detalle de cita
            </h1>

            <p className="mt-2 max-w-2xl text-sm leading-6 text-[#786b62]">
              Consulta la reserva y realiza las acciones principales sin salir
              de esta pantalla.
            </p>
          </div>

          <button
            type="button"
            onClick={() =>
              void loadAppointment()
            }
            disabled={
              loading
                || statusUpdating != null
                || rescheduling
            }
            className="inline-flex w-fit items-center justify-center rounded-full border border-[#d9cfc7] bg-white px-5 py-2.5 text-sm font-semibold text-[#5f534b] transition hover:bg-[#faf7f4] disabled:cursor-not-allowed disabled:opacity-50"
          >
            {loading
              ? "Actualizando..."
              : "Actualizar"}
          </button>
        </section>

        {/* ===================================================
            TOAST
        =================================================== */}

        {successMessage && (
          <div className="fixed right-5 top-[92px] z-50 max-w-sm rounded-[20px] border border-[#d6e5d8] bg-white px-5 py-4 shadow-[0_18px_55px_rgba(53,69,56,0.16)]">
            <div className="flex items-start gap-3">
              <div className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-[#eaf4ec] text-sm font-bold text-[#55735c]">
                ✓
              </div>

              <div>
                <div className="text-sm font-semibold text-[#405946]">
                  Cita actualizada
                </div>

                <p className="mt-1 text-xs leading-5 text-[#6d7d70]">
                  {successMessage}
                </p>
              </div>
            </div>
          </div>
        )}

        {/* ===================================================
            SESSION
        =================================================== */}

        {missingSession && (
          <section className="rounded-[24px] border border-[#e8d6c3] bg-[#fbf5ec] p-5">
            <div className="text-sm font-semibold text-[#7a5a38]">
              Sesión Business necesaria
            </div>

            <p className="mt-2 text-sm leading-6 text-[#806d5b]">
              Necesitas iniciar sesión para consultar o modificar esta cita.
            </p>

            <Link
              href="/business/login"
              className="mt-4 inline-flex rounded-full bg-[#342d28] px-4 py-2 text-xs font-semibold text-white"
            >
              Ir al login
            </Link>
          </section>
        )}

        {/* ===================================================
            ERROR
        =================================================== */}

        {error
          && !missingSession
          && (
            <section className="flex flex-col gap-4 rounded-[24px] border border-[#ead3d3] bg-[#fbf1f1] p-5 md:flex-row md:items-center md:justify-between">
              <div>
                <div className="text-sm font-semibold text-[#8f5555]">
                  No se ha podido completar la operación
                </div>

                <div className="mt-1 text-sm text-[#946f6f]">
                  {error}
                </div>
              </div>

              <button
                type="button"
                onClick={() =>
                  void loadAppointment()
                }
                className="w-fit rounded-full border border-[#ddc5c5] bg-white px-4 py-2 text-xs font-semibold text-[#7e5858] transition hover:bg-[#fffafa]"
              >
                Reintentar
              </button>
            </section>
          )}

        {/* ===================================================
            LOADING
        =================================================== */}

        {loading && (
          <section className="rounded-[28px] border border-[#e3dbd3] bg-white px-6 py-20 shadow-[0_12px_40px_rgba(62,49,41,0.04)]">
            <div className="mx-auto h-9 w-9 animate-spin rounded-full border-2 border-[#ded4cb] border-t-[#4c4038]" />

            <p className="mt-4 text-center text-sm text-[#8b7c72]">
              Cargando cita...
            </p>
          </section>
        )}

        {/* ===================================================
            CONTENT
        =================================================== */}

        {!loading
          && appointment
          && (
            <>
              {/* ===============================================
                  SUMMARY
              =============================================== */}

              <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
                <article className="rounded-[24px] border border-[#e4dcd4] bg-white p-5 shadow-[0_8px_28px_rgba(54,43,36,0.025)]">
                  <div className="text-xs font-medium text-[#88796f]">
                    Estado actual
                  </div>

                  <div className="mt-4">
                    <span
                      className={`inline-flex items-center gap-2 rounded-full border px-3 py-1.5 text-xs font-semibold ${getStatusClass(
                        appointment.status,
                      )}`}
                    >
                      <span
                        className={`h-1.5 w-1.5 rounded-full ${getStatusDotClass(
                          appointment.status,
                        )}`}
                      />

                      {
                        statusLabels[
                          appointment.status
                        ]
                      }
                    </span>
                  </div>
                </article>

                <article className="rounded-[24px] border border-[#e4dcd4] bg-white p-5 shadow-[0_8px_28px_rgba(54,43,36,0.025)]">
                  <div className="text-xs font-medium text-[#88796f]">
                    Horario
                  </div>

                  <div className="mt-3 text-2xl font-semibold tracking-[-0.04em] text-[#302a26]">
                    {formatTime(
                      appointment.startAt,
                      appointment.timezone,
                    )}
                  </div>

                  <div className="mt-1 text-xs text-[#9b8c82]">
                    hasta{" "}
                    {formatTime(
                      appointment.endAt,
                      appointment.timezone,
                    )}
                  </div>
                </article>

                <article className="rounded-[24px] border border-[#e4dcd4] bg-white p-5 shadow-[0_8px_28px_rgba(54,43,36,0.025)]">
                  <div className="text-xs font-medium text-[#88796f]">
                    Duración
                  </div>

                  <div className="mt-3 text-2xl font-semibold tracking-[-0.04em] text-[#302a26]">
                    {appointment.serviceDurationMinutes
                      != null
                      ? `${appointment.serviceDurationMinutes} min`
                      : "—"}
                  </div>
                </article>

                <article className="rounded-[24px] border border-[#e4dcd4] bg-white p-5 shadow-[0_8px_28px_rgba(54,43,36,0.025)]">
                  <div className="text-xs font-medium text-[#88796f]">
                    Importe
                  </div>

                  <div className="mt-3 text-2xl font-semibold tracking-[-0.04em] text-[#302a26]">
                    {formatPrice(
                      appointment.servicePrice,
                    )}
                  </div>
                </article>
              </section>

              {/* ===============================================
                  PREMIUM GRID
              =============================================== */}

              <section className="grid items-start gap-6 xl:grid-cols-[minmax(0,1fr)_360px]">
                <div className="min-w-0 space-y-6">
                  {/* ===========================================
                      RESERVATION
                  =========================================== */}

                  <article className="rounded-[28px] border border-[#e3dbd3] bg-white p-6 shadow-[0_12px_38px_rgba(62,49,41,0.04)] md:p-7">
                    <div className="flex flex-col gap-5 border-b border-[#eee7e1] pb-6 sm:flex-row sm:items-start sm:justify-between">
                      <div>
                        <div className="text-[11px] font-semibold uppercase tracking-[0.18em] text-[#a08b7d]">
                          Reserva
                        </div>

                        <h2 className="mt-2 text-2xl font-semibold tracking-[-0.025em] text-[#302a26]">
                          {appointment.serviceName
                            || "Servicio"}
                        </h2>

                        <p className="mt-2 text-sm capitalize text-[#84766c]">
                          {formatDate(
                            appointment.startAt,
                            appointment.timezone,
                          )}
                        </p>
                      </div>

                      <div className="rounded-[20px] bg-[#f8f5f1] px-5 py-4 sm:text-right">
                        <div className="text-2xl font-semibold tracking-[-0.04em] text-[#302a26]">
                          {formatTime(
                            appointment.startAt,
                            appointment.timezone,
                          )}
                        </div>

                        <div className="mt-1 text-xs text-[#998b81]">
                          hasta{" "}
                          {formatTime(
                            appointment.endAt,
                            appointment.timezone,
                          )}
                        </div>
                      </div>
                    </div>

                    <div className="grid gap-x-8 gap-y-6 pt-6 sm:grid-cols-2">
                      <div>
                        <div className="text-[11px] font-semibold uppercase tracking-[0.14em] text-[#a08e81]">
                          Profesional
                        </div>

                        <div className="mt-2 text-sm font-semibold text-[#342e2a]">
                          {appointment.professionalName
                            || "Sin profesional"}
                        </div>
                      </div>

                      <div>
                        <div className="text-[11px] font-semibold uppercase tracking-[0.14em] text-[#a08e81]">
                          Zona horaria
                        </div>

                        <div className="mt-2 text-sm font-semibold text-[#342e2a]">
                          {appointment.timezone
                            || DEFAULT_TIMEZONE}
                        </div>
                      </div>

                      <div>
                        <div className="text-[11px] font-semibold uppercase tracking-[0.14em] text-[#a08e81]">
                          Duración
                        </div>

                        <div className="mt-2 text-sm font-semibold text-[#342e2a]">
                          {appointment.serviceDurationMinutes
                            != null
                            ? `${appointment.serviceDurationMinutes} minutos`
                            : "No disponible"}
                        </div>
                      </div>

                      <div>
                        <div className="text-[11px] font-semibold uppercase tracking-[0.14em] text-[#a08e81]">
                          Precio
                        </div>

                        <div className="mt-2 text-sm font-semibold text-[#342e2a]">
                          {formatPrice(
                            appointment.servicePrice,
                          )}
                        </div>
                      </div>
                    </div>
                  </article>

                  {/* ===========================================
                      NOTES
                  =========================================== */}

                  <article className="rounded-[28px] border border-[#e3dbd3] bg-white p-6 shadow-[0_12px_38px_rgba(62,49,41,0.04)] md:p-7">
                    <div className="text-[11px] font-semibold uppercase tracking-[0.18em] text-[#a08b7d]">
                      Notas
                    </div>

                    <h2 className="mt-2 text-lg font-semibold text-[#342e2a]">
                      Información de la cita
                    </h2>

                    <div className="mt-5 rounded-[20px] border border-[#eee8e2] bg-[#faf8f6] p-5">
                      {appointment.notes
                        ? (
                          <p className="whitespace-pre-wrap text-sm leading-6 text-[#665a52]">
                            {
                              appointment.notes
                            }
                          </p>
                        )
                        : (
                          <p className="text-sm text-[#998b81]">
                            Esta cita no tiene notas.
                          </p>
                        )}
                    </div>
                  </article>

                  {/* ===========================================
                      TECHNICAL
                  =========================================== */}

                  <article className="rounded-[28px] border border-[#e3dbd3] bg-white p-6 shadow-[0_12px_38px_rgba(62,49,41,0.035)] md:p-7">
                    <div className="text-[11px] font-semibold uppercase tracking-[0.18em] text-[#a08b7d]">
                      Información interna
                    </div>

                    <h2 className="mt-2 text-lg font-semibold text-[#342e2a]">
                      Datos técnicos
                    </h2>

                    <div className="mt-6 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
                      <div className="rounded-[18px] bg-[#faf8f6] p-4">
                        <div className="text-xs text-[#9a8c82]">
                          ID cita
                        </div>

                        <div className="mt-1 text-sm font-semibold text-[#3d3530]">
                          #{appointment.id}
                        </div>
                      </div>

                      <div className="rounded-[18px] bg-[#faf8f6] p-4">
                        <div className="text-xs text-[#9a8c82]">
                          ID cliente
                        </div>

                        <div className="mt-1 text-sm font-semibold text-[#3d3530]">
                          #{appointment.customerId}
                        </div>
                      </div>

                      <div className="rounded-[18px] bg-[#faf8f6] p-4">
                        <div className="text-xs text-[#9a8c82]">
                          ID servicio
                        </div>

                        <div className="mt-1 text-sm font-semibold text-[#3d3530]">
                          #{appointment.serviceId}
                        </div>
                      </div>

                      <div className="rounded-[18px] bg-[#faf8f6] p-4">
                        <div className="text-xs text-[#9a8c82]">
                          ID profesional
                        </div>

                        <div className="mt-1 text-sm font-semibold text-[#3d3530]">
                          #{appointment.professionalId}
                        </div>
                      </div>
                    </div>

                    <div className="mt-5 grid gap-5 border-t border-[#eee7e1] pt-5 sm:grid-cols-2">
                      <div>
                        <div className="text-xs text-[#9a8c82]">
                          Creada
                        </div>

                        <div className="mt-1 text-sm font-medium text-[#5f544d]">
                          {formatDateTime(
                            appointment.createdAt,
                            appointment.timezone,
                          )}
                        </div>
                      </div>

                      <div>
                        <div className="text-xs text-[#9a8c82]">
                          Última actualización
                        </div>

                        <div className="mt-1 text-sm font-medium text-[#5f544d]">
                          {formatDateTime(
                            appointment.updatedAt,
                            appointment.timezone,
                          )}
                        </div>
                      </div>
                    </div>
                  </article>
                </div>

                {/* =============================================
                    SIDEBAR
                ============================================= */}

                <aside className="space-y-5 xl:sticky xl:top-[100px]">
                  {/* ===========================================
                      MANAGEMENT
                  =========================================== */}

                  <article className="overflow-hidden rounded-[28px] border border-[#ded4cc] bg-white shadow-[0_18px_50px_rgba(58,45,37,0.09)]">
                    <div className="border-b border-[#eee7e1] bg-[#fbf9f7] px-6 py-5">
                      <div className="text-[11px] font-semibold uppercase tracking-[0.2em] text-[#9d8779]">
                        Gestión
                      </div>

                      <div className="mt-3 flex items-start justify-between gap-4">
                        <div>
                          <h2 className="text-xl font-semibold tracking-[-0.025em] text-[#302a26]">
                            Estado de la cita
                          </h2>

                          <p className="mt-1 text-xs leading-5 text-[#897b71]">
                            {
                              statusDescriptions[
                                appointment.status
                              ]
                            }
                          </p>
                        </div>

                        <span
                          className={`mt-0.5 inline-flex shrink-0 items-center gap-2 rounded-full border px-3 py-1.5 text-xs font-semibold ${getStatusClass(
                            appointment.status,
                          )}`}
                        >
                          <span
                            className={`h-1.5 w-1.5 rounded-full ${getStatusDotClass(
                              appointment.status,
                            )}`}
                          />

                          {
                            statusLabels[
                              appointment.status
                            ]
                          }
                        </span>
                      </div>
                    </div>

                    <div className="p-6">
                      {!isTerminalStatus(
                        appointment.status,
                      ) ? (
                        <div className="space-y-3">
                          {canConfirm(
                            appointment.status,
                          ) && (
                            <button
                              type="button"
                              onClick={() =>
                                void updateAppointmentStatus(
                                  "CONFIRMED",
                                )
                              }
                              disabled={
                                statusUpdating != null
                                  || rescheduling
                              }
                              className="flex w-full items-center justify-between rounded-[18px] bg-[#314137] px-5 py-4 text-left text-sm font-semibold text-white shadow-[0_8px_24px_rgba(49,65,55,0.16)] transition hover:bg-[#26362d] disabled:cursor-not-allowed disabled:opacity-55"
                            >
                              <span>
                                {statusUpdating === "CONFIRMED"
                                  ? "Confirmando cita..."
                                  : "Confirmar cita"}
                              </span>

                              <span className="text-base">
                                →
                              </span>
                            </button>
                          )}

                          {canComplete(
                            appointment.status,
                          ) && (
                            <button
                              type="button"
                              onClick={() =>
                                void updateAppointmentStatus(
                                  "COMPLETED",
                                )
                              }
                              disabled={
                                statusUpdating != null
                                  || rescheduling
                              }
                              className="flex w-full items-center justify-between rounded-[18px] bg-[#40566a] px-5 py-4 text-left text-sm font-semibold text-white shadow-[0_8px_24px_rgba(64,86,106,0.16)] transition hover:bg-[#34495b] disabled:cursor-not-allowed disabled:opacity-55"
                            >
                              <span>
                                {statusUpdating === "COMPLETED"
                                  ? "Completando..."
                                  : "Marcar como completada"}
                              </span>

                              <span className="text-base">
                                ✓
                              </span>
                            </button>
                          )}

                          <button
                            type="button"
                            onClick={
                              openReschedulePanel
                            }
                            disabled={
                              !canReschedule(
                                appointment.status,
                              )
                                || statusUpdating != null
                                || rescheduling
                            }
                            className="flex w-full items-center justify-between rounded-[18px] border border-[#d9cfc7] bg-[#faf8f6] px-5 py-4 text-left text-sm font-semibold text-[#655950] transition hover:border-[#cbbdb2] hover:bg-white disabled:cursor-not-allowed disabled:opacity-45"
                          >
                            <span>
                              Reprogramar cita
                            </span>

                            <span>
                              ↗
                            </span>
                          </button>

                          {canMarkNoShow(
                            appointment.status,
                          ) && (
                            <button
                              type="button"
                              onClick={() =>
                                void updateAppointmentStatus(
                                  "NO_SHOW",
                                )
                              }
                              disabled={
                                statusUpdating != null
                                  || rescheduling
                              }
                              className="flex w-full items-center justify-between rounded-[18px] border border-[#ddd7d4] bg-white px-5 py-4 text-left text-sm font-semibold text-[#756b6b] transition hover:bg-[#f4f1f1] disabled:cursor-not-allowed disabled:opacity-50"
                            >
                              <span>
                                {statusUpdating === "NO_SHOW"
                                  ? "Actualizando..."
                                  : "Cliente no asistió"}
                              </span>

                              <span>
                                —
                              </span>
                            </button>
                          )}

                          {canCancel(
                            appointment.status,
                          ) && (
                            <div className="border-t border-[#eee7e1] pt-3">
                              <button
                                type="button"
                                onClick={() =>
                                  void updateAppointmentStatus(
                                    "CANCELLED",
                                  )
                                }
                                disabled={
                                  statusUpdating != null
                                    || rescheduling
                                }
                                className="flex w-full items-center justify-between rounded-[18px] border border-[#ead3d3] bg-[#fffafa] px-5 py-4 text-left text-sm font-semibold text-[#985e5e] transition hover:bg-[#fbefef] disabled:cursor-not-allowed disabled:opacity-50"
                              >
                                <span>
                                  {statusUpdating === "CANCELLED"
                                    ? "Cancelando..."
                                    : "Cancelar cita"}
                                </span>

                                <span>
                                  ×
                                </span>
                              </button>
                            </div>
                          )}
                        </div>
                      ) : (
                        <div className="rounded-[20px] border border-[#e5ddd6] bg-[#faf8f6] p-5">
                          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-white text-lg shadow-sm">
                            ✓
                          </div>

                          <div className="mt-4 text-sm font-semibold text-[#423933]">
                            Cita finalizada
                          </div>

                          <p className="mt-1 text-xs leading-5 text-[#85776e]">
                            Esta reserva está en un estado final y ya no admite
                            las acciones operativas habituales.
                          </p>
                        </div>
                      )}
                    </div>
                  </article>

                  {/* ===========================================
                      CUSTOMER
                  =========================================== */}

                  <article className="rounded-[28px] border border-[#e3dbd3] bg-white p-6 shadow-[0_12px_38px_rgba(62,49,41,0.045)]">
                    <div className="text-[11px] font-semibold uppercase tracking-[0.18em] text-[#a08b7d]">
                      Cliente
                    </div>

                    <h2 className="mt-2 text-xl font-semibold tracking-[-0.02em] text-[#312b27]">
                      {appointment.customerName
                        || "Cliente sin nombre"}
                    </h2>

                    <div className="mt-5 space-y-4">
                      <div className="rounded-[18px] bg-[#faf8f6] p-4">
                        <div className="text-[10px] font-semibold uppercase tracking-[0.13em] text-[#a08e81]">
                          Email
                        </div>

                        <div className="mt-2 break-all text-sm font-medium text-[#5f544d]">
                          {appointment.customerEmail
                            || "No disponible"}
                        </div>
                      </div>

                      <div className="rounded-[18px] bg-[#faf8f6] p-4">
                        <div className="text-[10px] font-semibold uppercase tracking-[0.13em] text-[#a08e81]">
                          Teléfono
                        </div>

                        <div className="mt-2 text-sm font-medium text-[#5f544d]">
                          {appointment.customerPhone
                            || "No disponible"}
                        </div>
                      </div>
                    </div>
                  </article>

                  {/* ===========================================
                      QUICK SUMMARY
                  =========================================== */}

                  <article className="rounded-[24px] border border-[#dfd6ce] bg-[#eee7df] p-5">
                    <div className="text-[10px] font-semibold uppercase tracking-[0.17em] text-[#907b6e]">
                      Resumen
                    </div>

                    <div className="mt-4 flex items-center justify-between gap-4">
                      <div>
                        <div className="text-xs text-[#7b6d64]">
                          {appointment.serviceName
                            || "Servicio"}
                        </div>

                        <div className="mt-1 text-sm font-semibold text-[#3b332e]">
                          {formatTime(
                            appointment.startAt,
                            appointment.timezone,
                          )}
                          {" · "}
                          {appointment.serviceDurationMinutes
                            != null
                            ? `${appointment.serviceDurationMinutes} min`
                            : "Duración no disponible"}
                        </div>
                      </div>

                      <div className="text-right text-sm font-semibold text-[#3b332e]">
                        {formatPrice(
                          appointment.servicePrice,
                        )}
                      </div>
                    </div>
                  </article>
                </aside>
              </section>
            </>
          )}
      </div>

      {/* =====================================================
          RESCHEDULE MODAL
      ===================================================== */}

      {rescheduleOpen
        && appointment
        && (
          <div
            className="fixed inset-0 z-[80] flex items-center justify-center bg-[#2d2722]/40 p-3 backdrop-blur-[4px] sm:p-4"
            onMouseDown={
              (
                event,
              ) => {
                if (
                  event.target
                    === event.currentTarget
                    && !rescheduling
                ) {
                  closeReschedulePanel();
                }
              }
            }
          >
            <div className="flex max-h-[94vh] w-full max-w-[1040px] flex-col overflow-hidden rounded-[30px] border border-white/70 bg-[#f8f5f1] shadow-[0_35px_100px_rgba(38,30,25,0.28)]">
              {/* ===============================================
                  MODAL HEADER
              =============================================== */}

              <div className="flex items-start justify-between gap-5 border-b border-[#e8dfd7] bg-white px-5 py-5 sm:px-7 md:px-8 md:py-6">
                <div>
                  <div className="text-[10px] font-semibold uppercase tracking-[0.22em] text-[#a08a7c]">
                    Gestión de agenda
                  </div>

                  <h2 className="mt-2 text-2xl font-semibold tracking-[-0.035em] text-[#302a26]">
                    Reprogramar cita
                  </h2>

                  <p className="mt-2 max-w-xl text-sm leading-6 text-[#81736a]">
                    Selecciona una nueva fecha y uno de los horarios disponibles
                    para {appointment.professionalName || "el profesional"}.
                  </p>
                </div>

                <button
                  type="button"
                  onClick={
                    closeReschedulePanel
                  }
                  disabled={
                    rescheduling
                  }
                  aria-label="Cerrar"
                  className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full border border-[#e1d8d0] bg-[#faf8f6] text-lg text-[#72645b] transition hover:bg-[#f2ece7] disabled:cursor-not-allowed disabled:opacity-50"
                >
                  ×
                </button>
              </div>

              {/* ===============================================
                  MODAL BODY
              =============================================== */}

              <div className="overflow-y-auto px-5 py-5 sm:px-7 md:px-8 md:py-6">
                <div className="grid gap-6 lg:grid-cols-[360px_minmax(0,1fr)]">
                  {/* ===========================================
                      LEFT COLUMN
                  =========================================== */}

                  <div className="space-y-5">
                    {/* =========================================
                        CURRENT APPOINTMENT
                    ========================================= */}

                    <section className="rounded-[24px] border border-[#e1d8d0] bg-white p-5">
                      <div className="text-[10px] font-semibold uppercase tracking-[0.18em] text-[#9b8678]">
                        Cita actual
                      </div>

                      <div className="mt-4 text-lg font-semibold text-[#342e2a]">
                        {appointment.serviceName
                          || "Servicio"}
                      </div>

                      <div className="mt-1 text-sm capitalize text-[#81736a]">
                        {formatDate(
                          appointment.startAt,
                          appointment.timezone,
                        )}
                      </div>

                      <div className="mt-4 flex items-center justify-between rounded-[17px] bg-[#f8f5f1] px-4 py-3">
                        <div>
                          <div className="text-xs text-[#95867c]">
                            Horario
                          </div>

                          <div className="mt-1 text-lg font-semibold text-[#342e2a]">
                            {formatTime(
                              appointment.startAt,
                              appointment.timezone,
                            )}
                          </div>
                        </div>

                        <div className="text-right">
                          <div className="text-xs text-[#95867c]">
                            Duración
                          </div>

                          <div className="mt-1 text-sm font-semibold text-[#342e2a]">
                            {appointment.serviceDurationMinutes
                              != null
                              ? `${appointment.serviceDurationMinutes} min`
                              : "—"}
                          </div>
                        </div>
                      </div>

                      <div className="mt-4 border-t border-[#eee7e1] pt-4">
                        <div className="text-xs text-[#95867c]">
                          Profesional
                        </div>

                        <div className="mt-1 text-sm font-semibold text-[#51463f]">
                          {appointment.professionalName
                            || "No disponible"}
                        </div>
                      </div>
                    </section>

                    {/* =========================================
                        PREMIUM DATE PICKER
                    ========================================= */}

                    <section className="rounded-[24px] border border-[#dfd4ca] bg-[#eee7df] p-5">
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <div className="text-[10px] font-semibold uppercase tracking-[0.18em] text-[#8f796c]">
                            Nueva fecha
                          </div>

                          <h3 className="mt-2 text-base font-semibold text-[#3b332e]">
                            Selecciona un día
                          </h3>
                        </div>

                        <button
                          type="button"
                          onClick={
                            goToToday
                          }
                          disabled={
                            rescheduling
                          }
                          className="rounded-full border border-[#d5c8bd] bg-white/80 px-3 py-1.5 text-[11px] font-semibold text-[#75665d] transition hover:bg-white disabled:cursor-not-allowed disabled:opacity-50"
                        >
                          Hoy
                        </button>
                      </div>

                      <div className="mt-5 flex items-center justify-between gap-3">
                        <button
                          type="button"
                          onClick={() =>
                            moveCalendar(
                              -7,
                            )
                          }
                          disabled={
                            rescheduling
                          }
                          aria-label="Semana anterior"
                          className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-[#d6c9be] bg-white text-sm font-semibold text-[#6c5f57] transition hover:border-[#bdaea2] hover:bg-[#faf8f6] disabled:cursor-not-allowed disabled:opacity-50"
                        >
                          ←
                        </button>

                        <div className="min-w-0 flex-1 text-center">
                          <div className="truncate text-sm font-semibold capitalize text-[#3c342f]">
                            {formatCalendarMonthYear(
                              calendarStartDate,
                            )}
                          </div>
                        </div>

                        <button
                          type="button"
                          onClick={() =>
                            moveCalendar(
                              7,
                            )
                          }
                          disabled={
                            rescheduling
                          }
                          aria-label="Semana siguiente"
                          className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-[#d6c9be] bg-white text-sm font-semibold text-[#6c5f57] transition hover:border-[#bdaea2] hover:bg-[#faf8f6] disabled:cursor-not-allowed disabled:opacity-50"
                        >
                          →
                        </button>
                      </div>

                      <div className="mt-4 grid grid-cols-7 gap-1.5">
                        {calendarDays.map(
                          (
                            dateValue,
                          ) => {
                            const selected =
                              dateValue
                                === rescheduleDate;

                            const today =
                              dateValue
                                === todayDateValue;

                            const past =
                              Boolean(
                                todayDateValue
                                  && dateValue
                                    < todayDateValue,
                              );

                            return (
                              <button
                                key={
                                  dateValue
                                }
                                type="button"
                                onClick={() =>
                                  selectRescheduleDate(
                                    dateValue,
                                  )
                                }
                                disabled={
                                  past
                                    || rescheduling
                                }
                                title={
                                  past
                                    ? "No puedes reprogramar una cita en una fecha pasada."
                                    : formatInputDateLabel(
                                        dateValue,
                                      )
                                }
                                className={[
                                  "group relative min-w-0 rounded-[15px] border px-1 py-2.5 text-center transition",
                                  selected
                                    ? "border-[#342d28] bg-[#342d28] text-white shadow-[0_8px_22px_rgba(52,45,40,0.18)]"
                                    : "border-[#ddd1c7] bg-white/80 text-[#64574f] hover:border-[#bfaea1] hover:bg-white",
                                  past
                                    ? "cursor-not-allowed opacity-35"
                                    : "",
                                ].join(
                                  " ",
                                )}
                              >
                                <div
                                  className={[
                                    "truncate text-[8px] font-semibold tracking-[0.06em]",
                                    selected
                                      ? "text-white/70"
                                      : "text-[#9a887b]",
                                  ].join(
                                    " ",
                                  )}
                                >
                                  {formatCalendarWeekday(
                                    dateValue,
                                  )}
                                </div>

                                <div className="mt-1 text-base font-semibold leading-none">
                                  {formatCalendarDay(
                                    dateValue,
                                  )}
                                </div>

                                <div
                                  className={[
                                    "mt-1 truncate text-[8px] font-semibold",
                                    selected
                                      ? "text-white/60"
                                      : "text-[#a49387]",
                                  ].join(
                                    " ",
                                  )}
                                >
                                  {formatCalendarMonthShort(
                                    dateValue,
                                  )}
                                </div>

                                {today && (
                                  <span
                                    className={[
                                      "absolute bottom-1 left-1/2 h-1 w-1 -translate-x-1/2 rounded-full",
                                      selected
                                        ? "bg-white"
                                        : "bg-[#755f51]",
                                    ].join(
                                      " ",
                                    )}
                                  />
                                )}
                              </button>
                            );
                          },
                        )}
                      </div>

                      <div className="mt-4 rounded-[17px] border border-[#d9ccc1] bg-white/70 px-4 py-3">
                        <div className="text-[9px] font-semibold uppercase tracking-[0.14em] text-[#9a877a]">
                          Día seleccionado
                        </div>

                        <div className="mt-1 text-sm font-semibold capitalize text-[#443a34]">
                          {formatInputDateLabel(
                            rescheduleDate,
                          )}
                        </div>

                        <div className="mt-1 text-[11px] text-[#8b796e]">
                          {availabilityLoading
                            ? "Consultando disponibilidad..."
                            : availability
                              ? availability.totalSlots === 0
                                ? "Sin horarios disponibles"
                                : `${availability.totalSlots} ${
                                    availability.totalSlots === 1
                                      ? "horario disponible"
                                      : "horarios disponibles"
                                  }`
                              : "Selecciona un día para consultar disponibilidad"}
                        </div>
                      </div>
                    </section>
                  </div>

                  {/* ===========================================
                      RIGHT / AVAILABILITY
                  =========================================== */}

                  <section className="rounded-[24px] border border-[#e1d8d0] bg-white p-5 md:p-6">
                    <div className="flex flex-col gap-3 border-b border-[#eee7e1] pb-5 sm:flex-row sm:items-center sm:justify-between">
                      <div>
                        <div className="text-[10px] font-semibold uppercase tracking-[0.18em] text-[#9b8678]">
                          Disponibilidad
                        </div>

                        <h3 className="mt-2 text-lg font-semibold text-[#342e2a]">
                          Selecciona una hora
                        </h3>

                        {rescheduleDate && (
                          <div className="mt-1 text-xs capitalize text-[#928278]">
                            {formatInputDateLabel(
                              rescheduleDate,
                            )}
                          </div>
                        )}
                      </div>

                      {availability && (
                        <div className="rounded-full bg-[#f5f1ed] px-3 py-1.5 text-xs font-semibold text-[#786a61]">
                          {availability.totalSlots}{" "}
                          {availability.totalSlots === 1
                            ? "horario"
                            : "horarios"}
                        </div>
                      )}
                    </div>

                    {/* =========================================
                        LOADING
                    ========================================= */}

                    {availabilityLoading && (
                      <div className="py-16 text-center">
                        <div className="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-[#ddd3cb] border-t-[#51463f]" />

                        <p className="mt-4 text-sm text-[#8b7c72]">
                          Consultando disponibilidad...
                        </p>
                      </div>
                    )}

                    {/* =========================================
                        ERROR
                    ========================================= */}

                    {!availabilityLoading
                      && availabilityError
                      && (
                        <div className="mt-5 rounded-[18px] border border-[#ead3d3] bg-[#fff5f5] p-4">
                          <div className="text-sm font-semibold text-[#8d5959]">
                            No se ha podido completar la operación
                          </div>

                          <p className="mt-1 text-xs leading-5 text-[#956f6f]">
                            {availabilityError}
                          </p>
                        </div>
                      )}

                    {/* =========================================
                        NO DATE
                    ========================================= */}

                    {!availabilityLoading
                      && !rescheduleDate
                      && (
                        <div className="py-16 text-center">
                          <div className="text-sm font-semibold text-[#61554e]">
                            Selecciona una fecha
                          </div>

                          <p className="mt-2 text-xs text-[#94857b]">
                            Los horarios disponibles aparecerán aquí.
                          </p>
                        </div>
                      )}

                    {/* =========================================
                        NO SLOTS
                    ========================================= */}

                    {!availabilityLoading
                      && !availabilityError
                      && availability
                      && (
                        !availability.available
                          || availability.totalSlots === 0
                          || availability.slots.length === 0
                      )
                      && (
                        <div className="py-14 text-center">
                          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-[#f4efea] text-xl text-[#958478]">
                            —
                          </div>

                          <div className="mt-4 text-sm font-semibold text-[#5f534b]">
                            No hay horarios disponibles
                          </div>

                          <p className="mx-auto mt-2 max-w-xs text-xs leading-5 text-[#94857b]">
                            Usa el selector de días de la izquierda para buscar
                            otra fecha disponible.
                          </p>
                        </div>
                      )}

                    {/* =========================================
                        SLOTS
                    ========================================= */}

                    {!availabilityLoading
                      && !availabilityError
                      && availability
                      && availability.slots.length > 0
                      && (
                        <div className="mt-5">
                          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
                            {availability.slots.map(
                              (
                                slot,
                              ) => {
                                const selected =
                                  selectedSlot?.startAt
                                    === slot.startAt;

                                const currentSlot =
                                  slot.startAt
                                    === appointment.startAt;

                                return (
                                  <button
                                    key={
                                      slot.startAt
                                    }
                                    type="button"
                                    onClick={() => {
                                      if (
                                        currentSlot
                                      ) {
                                        return;
                                      }

                                      setSelectedSlot(
                                        slot,
                                      );

                                      setAvailabilityError(
                                        null,
                                      );
                                    }}
                                    disabled={
                                      currentSlot
                                        || rescheduling
                                    }
                                    className={[
                                      "relative rounded-[17px] border px-4 py-3 text-left transition",
                                      selected
                                        ? "border-[#4f6256] bg-[#eef4ef] shadow-[0_6px_20px_rgba(72,91,78,0.10)]"
                                        : "border-[#e3dad2] bg-[#faf8f6] hover:border-[#c9bbb0] hover:bg-white",
                                      currentSlot
                                        ? "cursor-not-allowed opacity-45"
                                        : "",
                                    ].join(
                                      " ",
                                    )}
                                  >
                                    <div className="text-base font-semibold text-[#342e2a]">
                                      {formatSlotTime(
                                        slot.startTime,
                                      )}
                                    </div>

                                    <div className="mt-1 text-[11px] text-[#93857b]">
                                      hasta{" "}
                                      {formatSlotTime(
                                        slot.endTime,
                                      )}
                                    </div>

                                    {selected && (
                                      <div className="absolute right-3 top-3 flex h-5 w-5 items-center justify-center rounded-full bg-[#53675a] text-[10px] font-bold text-white">
                                        ✓
                                      </div>
                                    )}

                                    {currentSlot && (
                                      <div className="mt-1 text-[10px] font-semibold uppercase tracking-[0.08em] text-[#93857b]">
                                        Actual
                                      </div>
                                    )}
                                  </button>
                                );
                              },
                            )}
                          </div>
                        </div>
                      )}

                    {/* =========================================
                        SELECTED SLOT
                    ========================================= */}

                    {selectedSlot && (
                      <div className="mt-6 rounded-[20px] border border-[#d7e3d9] bg-[#f0f6f1] p-4">
                        <div className="text-[10px] font-semibold uppercase tracking-[0.14em] text-[#66806c]">
                          Nuevo horario seleccionado
                        </div>

                        <div className="mt-3 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                          <div>
                            <div className="text-sm font-semibold capitalize text-[#3e5544]">
                              {formatInputDateLabel(
                                rescheduleDate,
                              )}
                            </div>

                            <div className="mt-1 text-xs text-[#6c806f]">
                              {appointment.professionalName}
                            </div>
                          </div>

                          <div className="text-left sm:text-right">
                            <div className="text-xl font-semibold text-[#3e5544]">
                              {formatSlotTime(
                                selectedSlot.startTime,
                              )}
                            </div>

                            <div className="text-xs text-[#6c806f]">
                              hasta{" "}
                              {formatSlotTime(
                                selectedSlot.endTime,
                              )}
                            </div>
                          </div>
                        </div>
                      </div>
                    )}
                  </section>
                </div>
              </div>

              {/* ===============================================
                  MODAL FOOTER
              =============================================== */}

              <div className="flex flex-col-reverse gap-3 border-t border-[#e8dfd7] bg-white px-5 py-5 sm:flex-row sm:items-center sm:justify-between sm:px-7 md:px-8">
                <p className="text-xs leading-5 text-[#93857b]">
                  La duración, servicio, cliente y profesional se mantienen.
                </p>

                <div className="flex gap-3">
                  <button
                    type="button"
                    onClick={
                      closeReschedulePanel
                    }
                    disabled={
                      rescheduling
                    }
                    className="rounded-full border border-[#d9cfc7] bg-white px-5 py-2.5 text-sm font-semibold text-[#655950] transition hover:bg-[#faf8f6] disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    Cancelar
                  </button>

                  <button
                    type="button"
                    onClick={() =>
                      void confirmReschedule()
                    }
                    disabled={
                      !selectedSlot
                        || rescheduling
                    }
                    className="rounded-full bg-[#342d28] px-6 py-2.5 text-sm font-semibold text-white shadow-[0_8px_24px_rgba(52,45,40,0.18)] transition hover:bg-[#241f1c] disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    {rescheduling
                      ? "Reprogramando..."
                      : "Confirmar nueva hora"}
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}
    </>
  );
}