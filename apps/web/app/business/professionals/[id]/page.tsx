"use client";

import Link from "next/link";

import type {
  ReactNode,
} from "react";

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
  apiDelete,
  apiGet,
  apiPost,
} from "@/lib/api";

/* =========================================================
   TYPES
========================================================= */

type ProfessionalResponse = {
  id: number;
  businessId: number;

  firstName: string;
  lastName: string | null;
  displayName: string;

  email: string | null;
  phone: string | null;
  bio: string | null;

  active: boolean;
  bookingEnabled: boolean;

  createdAt: string;
  updatedAt: string;
};

type ServiceResponse = {
  id: number;
  businessId?: number;

  name: string;
  slug?: string | null;
  description?: string | null;

  durationMinutes: number;
  price: number | string;

  active: boolean;
  bookingEnabled: boolean;

  createdAt?: string;
  updatedAt?: string;
};

type ProfessionalServiceAssignmentResponse = {
  professionalId: number;

  serviceId: number;
  serviceName: string;
  serviceSlug: string | null;

  durationMinutes: number;
  price: number | string;

  serviceActive: boolean;
  serviceBookingEnabled: boolean;

  assignedAt: string;
};

type DayOfWeek =
  | "MONDAY"
  | "TUESDAY"
  | "WEDNESDAY"
  | "THURSDAY"
  | "FRIDAY"
  | "SATURDAY"
  | "SUNDAY";

type ProfessionalAvailabilityResponse = {
  id: number;
  professionalId?: number;

  dayOfWeek: DayOfWeek;

  startTime: string;
  endTime: string;

  active: boolean;

  createdAt?: string;
  updatedAt?: string;
};

type ProfessionalAvailabilityRequest = {
  dayOfWeek: DayOfWeek;

  startTime: string;
  endTime: string;

  active: boolean;
};

type ScheduleExceptionType =
  | "UNAVAILABLE"
  | "CUSTOM_HOURS";

type ScheduleExceptionResponse = {
  id: number;
  professionalId?: number;

  type: ScheduleExceptionType;

  exceptionDate: string;

  startTime: string | null;
  endTime: string | null;

  active: boolean;

  createdAt?: string;
  updatedAt?: string;
};

type ScheduleExceptionRequest = {
  type: ScheduleExceptionType;

  exceptionDate: string;

  startTime: string | null;
  endTime: string | null;

  active: boolean;
};

/* =========================================================
   CONSTANTS
========================================================= */

const ACCESS_TOKEN_STORAGE_KEY =
  "wacds_access_token";

const DAYS: Array<{
  value: DayOfWeek;
  label: string;
}> = [
  {
    value: "MONDAY",
    label: "Lunes",
  },
  {
    value: "TUESDAY",
    label: "Martes",
  },
  {
    value: "WEDNESDAY",
    label: "Miércoles",
  },
  {
    value: "THURSDAY",
    label: "Jueves",
  },
  {
    value: "FRIDAY",
    label: "Viernes",
  },
  {
    value: "SATURDAY",
    label: "Sábado",
  },
  {
    value: "SUNDAY",
    label: "Domingo",
  },
];

const MINIMUM_ACTION_FEEDBACK_MS =
  450;

function captureTimestamp(): number {
  return Date.now();
}

async function ensureMinimumActionFeedback(
  startedAt: number,
): Promise<void> {

  const elapsed =
    Date.now()
    - startedAt;

  const remaining =
    Math.max(
      0,
      MINIMUM_ACTION_FEEDBACK_MS
      - elapsed,
    );

  if (
    remaining === 0
  ) {
    return;
  }

  await new Promise<void>(
    resolve => {

      window.setTimeout(
        resolve,
        remaining,
      );
    },
  );
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
   TYPE PARSERS
========================================================= */

function parseDayOfWeek(
  value: string,
): DayOfWeek {

  switch (
    value
  ) {
    case "MONDAY":
      return "MONDAY";

    case "TUESDAY":
      return "TUESDAY";

    case "WEDNESDAY":
      return "WEDNESDAY";

    case "THURSDAY":
      return "THURSDAY";

    case "FRIDAY":
      return "FRIDAY";

    case "SATURDAY":
      return "SATURDAY";

    case "SUNDAY":
      return "SUNDAY";

    default:
      return "MONDAY";
  }
}

function parseScheduleExceptionType(
  value: string,
): ScheduleExceptionType {

  if (
    value === "CUSTOM_HOURS"
  ) {
    return "CUSTOM_HOURS";
  }

  return "UNAVAILABLE";
}

/* =========================================================
   DISPLAY HELPERS
========================================================= */

function getDisplayName(
  professional: ProfessionalResponse,
): string {

  if (
    professional.displayName
    && professional.displayName.trim() !== ""
  ) {
    return professional.displayName.trim();
  }

  const parts = [
    professional.firstName,
    professional.lastName,
  ]
    .filter(Boolean)
    .map(
      value =>
        String(value).trim(),
    )
    .filter(Boolean);

  return parts.length > 0
    ? parts.join(" ")
    : "Profesional";
}

function getInitials(
  professional: ProfessionalResponse,
): string {

  const displayName =
    getDisplayName(
      professional,
    );

  const parts =
    displayName
      .split(/\s+/)
      .filter(Boolean);

  if (
    parts.length >= 2
  ) {
    return (
      parts[0][0]
      + parts[1][0]
    ).toUpperCase();
  }

  if (
    parts.length === 1
  ) {
    return parts[0]
      .slice(
        0,
        2,
      )
      .toUpperCase();
  }

  return "PR";
}

function formatMoney(
  value:
    | number
    | string
    | null
    | undefined,
): string {

  const numericValue =
    Number(value);

  if (
    Number.isNaN(
      numericValue,
    )
  ) {
    return "—";
  }

  return new Intl.NumberFormat(
    "es-ES",
    {
      style: "currency",
      currency: "EUR",
    },
  ).format(
    numericValue,
  );
}

function formatDate(
  value:
    | string
    | null
    | undefined,
): string {

  if (
    !value
  ) {
    return "—";
  }

  const date =
    new Date(
      value,
    );

  if (
    Number.isNaN(
      date.getTime(),
    )
  ) {
    return value;
  }

  return new Intl.DateTimeFormat(
    "es-ES",
    {
      day: "2-digit",
      month: "short",
      year: "numeric",
    },
  ).format(
    date,
  );
}

function formatDateLong(
  value: string,
): string {

  const parts =
    value.split("-");

  if (
    parts.length !== 3
  ) {
    return value;
  }

  const year =
    Number(
      parts[0],
    );

  const month =
    Number(
      parts[1],
    );

  const day =
    Number(
      parts[2],
    );

  const date =
    new Date(
      year,
      month - 1,
      day,
    );

  if (
    Number.isNaN(
      date.getTime(),
    )
  ) {
    return value;
  }

  return new Intl.DateTimeFormat(
    "es-ES",
    {
      weekday: "long",
      day: "numeric",
      month: "long",
      year: "numeric",
    },
  ).format(
    date,
  );
}

function formatTime(
  value:
    | string
    | null
    | undefined,
): string {

  if (
    !value
  ) {
    return "—";
  }

  return value.slice(
    0,
    5,
  );
}

/* =========================================================
   ERROR
========================================================= */

function resolveApiError(
  error: unknown,
): string {

  if (
    error instanceof ApiError
  ) {

    if (
      error.status === 401
    ) {
      return "La sesión Business ha caducado. Vuelve a iniciar sesión.";
    }

    if (
      error.status === 403
    ) {
      return "No tienes permisos suficientes para gestionar este profesional.";
    }

    if (
      error.status === 404
    ) {
      return (
        error.message
        || "No se ha encontrado el recurso solicitado."
      );
    }

    if (
      error.status === 409
    ) {
      return (
        error.message
        || "Existe un conflicto con la configuración solicitada."
      );
    }

    return (
      error.message
      || "No se ha podido completar la operación."
    );
  }

  if (
    error instanceof Error
    && error.message.trim() !== ""
  ) {
    return error.message;
  }

  return "No se ha podido conectar con la API Business.";
}

/* =========================================================
   PAGE
========================================================= */

export default function ProfessionalDetailPage() {

  const params =
    useParams<{
      id: string;
    }>();

  const professionalId =
    Number(
      params.id,
    );

  /* =======================================================
     DATA
  ======================================================= */

  const [
    professional,
    setProfessional,
  ] =
    useState<
      ProfessionalResponse
      | null
    >(
      null,
    );

  const [
    services,
    setServices,
  ] =
    useState<ServiceResponse[]>(
      [],
    );

  const [
    assignedServices,
    setAssignedServices,
  ] =
    useState<
      ProfessionalServiceAssignmentResponse[]
    >(
      [],
    );

  const [
    availabilities,
    setAvailabilities,
  ] =
    useState<
      ProfessionalAvailabilityResponse[]
    >(
      [],
    );

  const [
    scheduleExceptions,
    setScheduleExceptions,
  ] =
    useState<
      ScheduleExceptionResponse[]
    >(
      [],
    );

  /* =======================================================
     UI
  ======================================================= */

  const [
    loading,
    setLoading,
  ] =
    useState(
      true,
    );

  const [
    refreshing,
    setRefreshing,
  ] =
    useState(
      false,
    );

  const [
    error,
    setError,
  ] =
    useState<
      string
      | null
    >(
      null,
    );

  const [
    successMessage,
    setSuccessMessage,
  ] =
    useState<
      string
      | null
    >(
      null,
    );

  const [
    serviceModalOpen,
    setServiceModalOpen,
  ] =
    useState(
      false,
    );

  const [
    availabilityModalOpen,
    setAvailabilityModalOpen,
  ] =
    useState(
      false,
    );

  const [
    exceptionModalOpen,
    setExceptionModalOpen,
  ] =
    useState(
      false,
    );

  const [
    workingKey,
    setWorkingKey,
  ] =
    useState<
      string
      | null
    >(
      null,
    );

  /* =======================================================
     AVAILABILITY FORM
  ======================================================= */

  const [
    availabilityDay,
    setAvailabilityDay,
  ] =
    useState<DayOfWeek>(
      "MONDAY",
    );

  const [
    availabilityStart,
    setAvailabilityStart,
  ] =
    useState(
      "09:00",
    );

  const [
    availabilityEnd,
    setAvailabilityEnd,
  ] =
    useState(
      "17:00",
    );

  /* =======================================================
     EXCEPTION FORM
  ======================================================= */

  const [
    exceptionType,
    setExceptionType,
  ] =
    useState<ScheduleExceptionType>(
      "UNAVAILABLE",
    );

  const [
    exceptionDate,
    setExceptionDate,
  ] =
    useState(
      "",
    );

  const [
    exceptionStart,
    setExceptionStart,
  ] =
    useState(
      "09:00",
    );

  const [
    exceptionEnd,
    setExceptionEnd,
  ] =
    useState(
      "14:00",
    );

  /* =======================================================
     LOAD
  ======================================================= */

  const loadProfessional =
    useCallback(
      async (
        silent = false,
      ) => {

        if (
          !Number.isInteger(
            professionalId,
          )
          || professionalId <= 0
        ) {

          setError(
            "El identificador del profesional no es válido.",
          );

          setLoading(
            false,
          );

          return;
        }

        if (
          silent
        ) {
          setRefreshing(
            true,
          );
        } else {
          setLoading(
            true,
          );
        }

        setError(
          null,
        );

        try {

          const accessToken =
            getStoredAccessToken();

          if (
            !accessToken
          ) {
            throw new Error(
              "No existe una sesión Business válida.",
            );
          }

          const [
            professionalResponse,
            servicesResponse,
            assignedServicesResponse,
            availabilityResponse,
            exceptionsResponse,
          ] =
            await Promise.all([
              apiGet<ProfessionalResponse>(
                `/api/v1/professionals/${professionalId}`,
                {
                  accessToken,
                },
              ),

              apiGet<ServiceResponse[]>(
                "/api/v1/services",
                {
                  accessToken,
                },
              ),

              apiGet<
                ProfessionalServiceAssignmentResponse[]
              >(
                `/api/v1/professionals/${professionalId}/services`,
                {
                  accessToken,
                },
              ),

              apiGet<
                ProfessionalAvailabilityResponse[]
              >(
                `/api/v1/professionals/${professionalId}/availability`,
                {
                  accessToken,
                },
              ),

              apiGet<
                ScheduleExceptionResponse[]
              >(
                `/api/v1/professionals/${professionalId}/schedule-exceptions`,
                {
                  accessToken,
                },
              ),
            ]);

          setProfessional(
            professionalResponse,
          );

          setServices(
            Array.isArray(
              servicesResponse,
            )
              ? servicesResponse
              : [],
          );

          setAssignedServices(
            Array.isArray(
              assignedServicesResponse,
            )
              ? assignedServicesResponse
              : [],
          );

          setAvailabilities(
            Array.isArray(
              availabilityResponse,
            )
              ? availabilityResponse
              : [],
          );

          setScheduleExceptions(
            Array.isArray(
              exceptionsResponse,
            )
              ? exceptionsResponse
              : [],
          );

        } catch (
          requestError
        ) {

          setError(
            resolveApiError(
              requestError,
            ),
          );

        } finally {

          setLoading(
            false,
          );

          setRefreshing(
            false,
          );
        }
      },
      [
        professionalId,
      ],
    );

  useEffect(
    () => {

      void Promise.resolve().then(
        () => {
          void loadProfessional();
        },
      );

    },
    [
      loadProfessional,
    ],
  );

  /* =======================================================
     SUCCESS MESSAGE
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
          3500,
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
     COMPUTED SERVICES
  ======================================================= */

  const assignedServiceIds =
    useMemo(
      () =>
        new Set(
          assignedServices.map(
            assignment =>
              assignment.serviceId,
          ),
        ),
      [
        assignedServices,
      ],
    );

  const availableServicesToAssign =
    useMemo(
      () =>
        services.filter(
          service =>
            !assignedServiceIds.has(
              service.id,
            ),
        ),
      [
        assignedServiceIds,
        services,
      ],
    );

  /* =======================================================
     COMPUTED AVAILABILITY
  ======================================================= */

  const availabilityByDay =
    useMemo(
      () => {

        const result =
          new Map<
            DayOfWeek,
            ProfessionalAvailabilityResponse[]
          >();

        for (
          const day of DAYS
        ) {

          result.set(
            day.value,
            [],
          );
        }

        for (
          const availability of availabilities
        ) {

          const current =
            result.get(
              availability.dayOfWeek,
            )
            ?? [];

          current.push(
            availability,
          );

          result.set(
            availability.dayOfWeek,
            current,
          );
        }

        for (
          const day of DAYS
        ) {

          result
            .get(
              day.value,
            )
            ?.sort(
              (
                first,
                second,
              ) =>
                first.startTime.localeCompare(
                  second.startTime,
                ),
            );
        }

        return result;
      },
      [
        availabilities,
      ],
    );

  const activeAvailabilityCount =
    useMemo(
      () =>
        availabilities.filter(
          availability =>
            availability.active,
        ).length,
      [
        availabilities,
      ],
    );

  const activeExceptionCount =
    useMemo(
      () =>
        scheduleExceptions.filter(
          exception =>
            exception.active,
        ).length,
      [
        scheduleExceptions,
      ],
    );

  /* =======================================================
     SERVICE ASSIGN
  ======================================================= */

  async function handleAssignService(
    serviceId: number,
  ) {

    if (
      workingKey != null
    ) {
      return;
    }

    const key =
      `assign-service-${serviceId}`;

    const startedAt =
      captureTimestamp();

    setWorkingKey(
      key,
    );

    setError(
      null,
    );

    try {

      const accessToken =
        getStoredAccessToken();

      if (
        !accessToken
      ) {
        throw new Error(
          "No existe una sesión Business válida.",
        );
      }

      await apiPost<
        ProfessionalServiceAssignmentResponse
      >(
        `/api/v1/professionals/${professionalId}/services/${serviceId}`,
        undefined,
        {
          accessToken,
        },
      );

      await Promise.all([
        loadProfessional(
          true,
        ),
        ensureMinimumActionFeedback(
          startedAt,
        ),
      ]);

      setSuccessMessage(
        "Servicio asignado al profesional.",
      );

    } catch (
      requestError
    ) {

      setError(
        resolveApiError(
          requestError,
        ),
      );

    } finally {

      setWorkingKey(
        null,
      );
    }
  }

  /* =======================================================
     SERVICE REMOVE
  ======================================================= */

  async function handleRemoveService(
    serviceId: number,
  ) {

    if (
      workingKey != null
    ) {
      return;
    }

    const key =
      `remove-service-${serviceId}`;

    const startedAt =
      captureTimestamp();

    setWorkingKey(
      key,
    );

    setError(
      null,
    );

    try {

      const accessToken =
        getStoredAccessToken();

      if (
        !accessToken
      ) {
        throw new Error(
          "No existe una sesión Business válida.",
        );
      }

      await apiDelete<void>(
        `/api/v1/professionals/${professionalId}/services/${serviceId}`,
        {
          accessToken,
        },
      );

      await Promise.all([
        loadProfessional(
          true,
        ),
        ensureMinimumActionFeedback(
          startedAt,
        ),
      ]);

      setSuccessMessage(
        "Servicio retirado del profesional.",
      );

    } catch (
      requestError
    ) {

      setError(
        resolveApiError(
          requestError,
        ),
      );

    } finally {

      setWorkingKey(
        null,
      );
    }
  }

  /* =======================================================
     AVAILABILITY CREATE
  ======================================================= */

  async function handleCreateAvailability() {

    if (
      workingKey != null
    ) {
      return;
    }

    if (
      !availabilityStart
      || !availabilityEnd
    ) {

      setError(
        "Debes indicar la hora de inicio y de finalización.",
      );

      return;
    }

    if (
      availabilityStart
      >= availabilityEnd
    ) {

      setError(
        "La hora de inicio debe ser anterior a la hora de finalización.",
      );

      return;
    }

    const startedAt =
      captureTimestamp();

    setWorkingKey(
      "create-availability",
    );

    setError(
      null,
    );

    try {

      const accessToken =
        getStoredAccessToken();

      if (
        !accessToken
      ) {
        throw new Error(
          "No existe una sesión Business válida.",
        );
      }

      const payload:
        ProfessionalAvailabilityRequest = {
          dayOfWeek:
            availabilityDay,

          startTime:
            availabilityStart,

          endTime:
            availabilityEnd,

          active:
            true,
        };

      await apiPost<
        ProfessionalAvailabilityResponse
      >(
        `/api/v1/professionals/${professionalId}/availability`,
        payload,
        {
          accessToken,
        },
      );

      await Promise.all([
        loadProfessional(
          true,
        ),
        ensureMinimumActionFeedback(
          startedAt,
        ),
      ]);

      setAvailabilityModalOpen(
        false,
      );

      setAvailabilityDay(
        "MONDAY",
      );

      setAvailabilityStart(
        "09:00",
      );

      setAvailabilityEnd(
        "17:00",
      );

      setSuccessMessage(
        "Franja horaria creada correctamente.",
      );

    } catch (
      requestError
    ) {

      setError(
        resolveApiError(
          requestError,
        ),
      );

    } finally {

      setWorkingKey(
        null,
      );
    }
  }

  /* =======================================================
     AVAILABILITY DELETE
  ======================================================= */

  async function handleDeleteAvailability(
    availabilityId: number,
  ) {

    if (
      workingKey != null
    ) {
      return;
    }

    const key =
      `availability-${availabilityId}`;

    const startedAt =
      captureTimestamp();

    setWorkingKey(
      key,
    );

    setError(
      null,
    );

    try {

      const accessToken =
        getStoredAccessToken();

      if (
        !accessToken
      ) {
        throw new Error(
          "No existe una sesión Business válida.",
        );
      }

      await apiDelete<void>(
        `/api/v1/professionals/${professionalId}/availability/${availabilityId}`,
        {
          accessToken,
        },
      );

      await Promise.all([
        loadProfessional(
          true,
        ),
        ensureMinimumActionFeedback(
          startedAt,
        ),
      ]);

      setSuccessMessage(
        "Franja horaria eliminada.",
      );

    } catch (
      requestError
    ) {

      setError(
        resolveApiError(
          requestError,
        ),
      );

    } finally {

      setWorkingKey(
        null,
      );
    }
  }

  /* =======================================================
     EXCEPTION CREATE
  ======================================================= */

  async function handleCreateException() {

    if (
      workingKey != null
    ) {
      return;
    }

    if (
      exceptionDate.trim() === ""
    ) {

      setError(
        "Selecciona la fecha de la excepción.",
      );

      return;
    }

    if (
      exceptionType === "CUSTOM_HOURS"
      && (
        !exceptionStart
        || !exceptionEnd
      )
    ) {

      setError(
        "El horario especial necesita hora de inicio y de finalización.",
      );

      return;
    }

    if (
      exceptionType === "CUSTOM_HOURS"
      && exceptionStart >= exceptionEnd
    ) {

      setError(
        "La hora de inicio debe ser anterior a la hora de finalización.",
      );

      return;
    }

    const startedAt =
      captureTimestamp();

    setWorkingKey(
      "create-exception",
    );

    setError(
      null,
    );

    try {

      const accessToken =
        getStoredAccessToken();

      if (
        !accessToken
      ) {
        throw new Error(
          "No existe una sesión Business válida.",
        );
      }

      const payload:
        ScheduleExceptionRequest = {
          type:
            exceptionType,

          exceptionDate,

          startTime:
            exceptionType
            === "CUSTOM_HOURS"
              ? exceptionStart
              : null,

          endTime:
            exceptionType
            === "CUSTOM_HOURS"
              ? exceptionEnd
              : null,

          active:
            true,
        };

      await apiPost<
        ScheduleExceptionResponse
      >(
        `/api/v1/professionals/${professionalId}/schedule-exceptions`,
        payload,
        {
          accessToken,
        },
      );

      await Promise.all([
        loadProfessional(
          true,
        ),
        ensureMinimumActionFeedback(
          startedAt,
        ),
      ]);

      setExceptionModalOpen(
        false,
      );

      setExceptionType(
        "UNAVAILABLE",
      );

      setExceptionDate(
        "",
      );

      setExceptionStart(
        "09:00",
      );

      setExceptionEnd(
        "14:00",
      );

      setSuccessMessage(
        "Excepción de horario creada correctamente.",
      );

    } catch (
      requestError
    ) {

      setError(
        resolveApiError(
          requestError,
        ),
      );

    } finally {

      setWorkingKey(
        null,
      );
    }
  }

  /* =======================================================
     EXCEPTION DELETE
  ======================================================= */

  async function handleDeleteException(
    scheduleExceptionId: number,
  ) {

    if (
      workingKey != null
    ) {
      return;
    }

    const key =
      `exception-${scheduleExceptionId}`;

    const startedAt =
      captureTimestamp();

    setWorkingKey(
      key,
    );

    setError(
      null,
    );

    try {

      const accessToken =
        getStoredAccessToken();

      if (
        !accessToken
      ) {
        throw new Error(
          "No existe una sesión Business válida.",
        );
      }

      await apiDelete<void>(
        `/api/v1/professionals/${professionalId}/schedule-exceptions/${scheduleExceptionId}`,
        {
          accessToken,
        },
      );

      await Promise.all([
        loadProfessional(
          true,
        ),
        ensureMinimumActionFeedback(
          startedAt,
        ),
      ]);

      setSuccessMessage(
        "Excepción eliminada.",
      );

    } catch (
      requestError
    ) {

      setError(
        resolveApiError(
          requestError,
        ),
      );

    } finally {

      setWorkingKey(
        null,
      );
    }
  }

  /* =======================================================
     INVALID ID
  ======================================================= */

  if (
    !Number.isInteger(
      professionalId,
    )
    || professionalId <= 0
  ) {

    return (
      <div className="pb-10">

        <section className="rounded-[26px] border border-[#ead3d3] bg-[#fbf1f1] p-8">

          <h1 className="text-xl font-semibold text-[#7f5555]">
            Profesional no válido
          </h1>

          <p className="mt-2 text-sm text-[#946f6f]">
            El identificador recibido no es correcto.
          </p>

          <Link
            href="/business/professionals"
            className="mt-5 inline-flex rounded-full border border-[#ddcaca] bg-white px-4 py-2.5 text-sm font-semibold text-[#775959]"
          >
            Volver a profesionales
          </Link>

        </section>

      </div>
    );
  }

  /* =======================================================
     LOADING
  ======================================================= */

  if (
    loading
  ) {

    return (
      <div className="space-y-6 pb-10">

        <div className="h-8 w-40 animate-pulse rounded-xl bg-[#eee8e2]" />

        <div className="h-52 animate-pulse rounded-[28px] bg-[#f1ece7]" />

        <div className="grid gap-5 xl:grid-cols-2">

          <div className="h-80 animate-pulse rounded-[26px] bg-[#f1ece7]" />

          <div className="h-80 animate-pulse rounded-[26px] bg-[#f1ece7]" />

        </div>

      </div>
    );
  }

  /* =======================================================
     LOAD ERROR
  ======================================================= */

  if (
    !professional
  ) {

    return (
      <div className="pb-10">

        <section className="rounded-[26px] border border-[#ead3d3] bg-[#fbf1f1] p-8">

          <h1 className="text-xl font-semibold text-[#7f5555]">
            No se ha podido cargar el profesional
          </h1>

          <p className="mt-2 text-sm leading-6 text-[#946f6f]">
            {error
            ?? "No se ha encontrado información para este profesional."}
          </p>

          <div className="mt-5 flex flex-wrap gap-3">

            <button
              type="button"
              onClick={() =>
                void loadProfessional()
              }
              className="rounded-full bg-[#342d28] px-5 py-2.5 text-sm font-semibold text-white"
            >
              Reintentar
            </button>

            <Link
              href="/business/professionals"
              className="rounded-full border border-[#ddcfc6] bg-white px-5 py-2.5 text-sm font-semibold text-[#66594f]"
            >
              Volver
            </Link>

          </div>

        </section>

      </div>
    );
  }

  /* =======================================================
     RENDER
  ======================================================= */

  return (
    <div className="space-y-7 pb-12">

      {/* ===================================================
          BREADCRUMB
      =================================================== */}

      <div className="flex flex-wrap items-center gap-2 text-xs font-medium text-[#97887e]">

        <Link
          href="/business/professionals"
          className="transition hover:text-[#5f534b]"
        >
          Profesionales
        </Link>

        <span>
          /
        </span>

        <span className="text-[#66594f]">
          {getDisplayName(
            professional,
          )}
        </span>

      </div>

      {/* ===================================================
          HEADER
      =================================================== */}

      <section className="overflow-hidden rounded-[28px] border border-[#e3dad2] bg-white shadow-[0_14px_45px_rgba(62,49,41,0.05)]">

        <div className="border-b border-[#eee7e1] bg-[#faf7f4] px-6 py-6 md:px-8">

          <div className="flex flex-col gap-6 xl:flex-row xl:items-start xl:justify-between">

            <div className="flex min-w-0 items-start gap-4">

              <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-full bg-[#e9dfd6] text-lg font-semibold text-[#69594e]">
                {getInitials(
                  professional,
                )}
              </div>

              <div className="min-w-0">

                <div className="flex flex-wrap items-center gap-2">

                  <div className="text-xs font-semibold uppercase tracking-[0.2em] text-[#a18c7f]">
                    Ficha profesional
                  </div>

                  <StatusBadge
                    active={
                      professional.active
                    }
                    activeLabel="Activo"
                    inactiveLabel="Inactivo"
                  />

                  <StatusBadge
                    active={
                      professional.active
                      && professional.bookingEnabled
                    }
                    activeLabel="Reservas habilitadas"
                    inactiveLabel="Reservas pausadas"
                    warning
                  />

                </div>

                <h1 className="mt-3 text-3xl font-semibold tracking-[-0.04em] text-[#302a26] md:text-4xl">
                  {getDisplayName(
                    professional,
                  )}
                </h1>

                <p className="mt-2 text-sm text-[#82746a]">

                  {professional.firstName}

                  {professional.lastName
                    ? ` ${professional.lastName}`
                    : ""}

                </p>

              </div>

            </div>

            <div className="flex flex-wrap gap-3">

              <button
                type="button"
                onClick={() =>
                  void loadProfessional(
                    true,
                  )
                }
                disabled={
                  refreshing
                }
                className="rounded-full border border-[#d9cfc7] bg-white px-5 py-2.5 text-sm font-semibold text-[#665a52] transition hover:bg-[#f6f2ee] disabled:opacity-50"
              >
                {refreshing
                  ? "Actualizando..."
                  : "Actualizar"}
              </button>

              <Link
                href="/business/professionals"
                className="inline-flex items-center justify-center rounded-full bg-[#342d28] px-5 py-2.5 text-sm font-semibold !text-white transition hover:bg-[#211c18] hover:!text-white"
              >
                Volver al equipo
              </Link>

            </div>

          </div>

        </div>

        <div className="grid gap-px bg-[#eee7e1] sm:grid-cols-2 xl:grid-cols-4">

          <HeaderMetric
            label="Servicios"
            value={String(
              assignedServices.length,
            )}
            description="Asignados"
          />

          <HeaderMetric
            label="Franjas semanales"
            value={String(
              activeAvailabilityCount,
            )}
            description="Activas"
          />

          <HeaderMetric
            label="Excepciones"
            value={String(
              activeExceptionCount,
            )}
            description="Activas"
          />

          <HeaderMetric
            label="Alta"
            value={formatDate(
              professional.createdAt,
            )}
            description={`ID ${professional.id}`}
          />

        </div>

      </section>

      {/* ===================================================
          MESSAGES
      =================================================== */}

      {successMessage && (

        <div
          role="status"
          aria-live="polite"
          className="fixed right-5 top-20 z-[160] flex max-w-[calc(100vw-40px)] items-center gap-3 rounded-[18px] border border-[#d6e3d8] bg-white px-4 py-3 shadow-[0_18px_55px_rgba(58,75,61,0.16)] sm:right-6 sm:max-w-md"
        >

          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[#edf5ee] text-sm font-bold text-[#58705c]">
            ✓
          </div>

          <div>

            <div className="text-xs font-semibold uppercase tracking-[0.12em] text-[#7e9481]">
              Cambios guardados
            </div>

            <div className="mt-0.5 text-sm font-medium text-[#4f6253]">
              {successMessage}
            </div>

          </div>

        </div>

      )}

      {error && (

        <section className="flex flex-col gap-3 rounded-[20px] border border-[#ead3d3] bg-[#fbf1f1] px-5 py-4 md:flex-row md:items-center md:justify-between">

          <p className="text-sm leading-6 text-[#8f5f5f]">
            {error}
          </p>

          <button
            type="button"
            onClick={() =>
              setError(
                null,
              )
            }
            className="w-fit rounded-full border border-[#dfcaca] bg-white px-3.5 py-1.5 text-xs font-semibold text-[#805a5a]"
          >
            Cerrar
          </button>

        </section>

      )}

      {/* ===================================================
          PROFILE
      =================================================== */}

      <section className="grid gap-5 xl:grid-cols-[1.2fr_0.8fr]">

        <article className="rounded-[26px] border border-[#e3dbd3] bg-white p-6 shadow-[0_10px_35px_rgba(62,49,41,0.04)] md:p-7">

          <SectionHeader
            eyebrow="Información"
            title="Datos del profesional"
            description="Información principal asociada a esta persona."
          />

          <div className="mt-6 grid gap-4 sm:grid-cols-2">

            <InfoItem
              label="Email"
              value={
                professional.email
                || "Sin email"
              }
            />

            <InfoItem
              label="Teléfono"
              value={
                professional.phone
                || "Sin teléfono"
              }
            />

            <InfoItem
              label="Nombre"
              value={
                professional.firstName
              }
            />

            <InfoItem
              label="Apellidos"
              value={
                professional.lastName
                || "—"
              }
            />

            <InfoItem
              label="Business ID"
              value={String(
                professional.businessId,
              )}
            />

            <InfoItem
              label="Última actualización"
              value={formatDate(
                professional.updatedAt,
              )}
            />

          </div>

          <div className="mt-5 rounded-[20px] border border-[#e9e1da] bg-[#faf8f5] p-5">

            <div className="text-xs font-semibold uppercase tracking-[0.14em] text-[#9a887c]">
              Biografía
            </div>

            <p className="mt-3 whitespace-pre-line text-sm leading-7 text-[#71645b]">
              {professional.bio
              || "No hay biografía añadida para este profesional."}
            </p>

          </div>

        </article>

        <article className="rounded-[26px] border border-[#e3dbd3] bg-white p-6 shadow-[0_10px_35px_rgba(62,49,41,0.04)] md:p-7">

          <SectionHeader
            eyebrow="Estado"
            title="Configuración operativa"
            description="Resumen rápido de la configuración del profesional."
          />

          <div className="mt-6 space-y-4">

            <StatusRow
              title="Profesional activo"
              description="Forma parte actualmente del equipo."
              active={
                professional.active
              }
            />

            <StatusRow
              title="Reservas online"
              description="Puede ser seleccionado al reservar una cita."
              active={
                professional.active
                && professional.bookingEnabled
              }
            />

            <StatusRow
              title="Servicios configurados"
              description="Tiene al menos un servicio asociado."
              active={
                assignedServices.length > 0
              }
            />

            <StatusRow
              title="Horario configurado"
              description="Tiene disponibilidad semanal activa."
              active={
                activeAvailabilityCount > 0
              }
            />

          </div>

        </article>

      </section>

      {/* ===================================================
          SERVICES
      =================================================== */}

      <section className="rounded-[26px] border border-[#e3dbd3] bg-white shadow-[0_10px_35px_rgba(62,49,41,0.04)]">

        <div className="flex flex-col gap-4 border-b border-[#eee7e1] px-6 py-5 md:flex-row md:items-center md:justify-between md:px-7">

          <SectionHeader
            eyebrow="Servicios"
            title="Servicios asignados"
            description="Define qué tratamientos o servicios puede realizar este profesional."
          />

          <button
            type="button"
            onClick={() =>
              setServiceModalOpen(
                true,
              )
            }
            className="w-fit rounded-full bg-[#342d28] px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-[#211c18]"
          >
            Gestionar servicios
          </button>

        </div>

        {assignedServices.length === 0
          ? (

            <EmptyState
              title="Sin servicios asignados"
              description="Asigna al menos un servicio para que el profesional pueda utilizarse correctamente en reservas."
              buttonLabel="Asignar servicio"
              onClick={() =>
                setServiceModalOpen(
                  true,
                )
              }
            />

          )
          : (

            <div className="grid gap-4 p-6 md:grid-cols-2 xl:grid-cols-3 md:p-7">

              {assignedServices.map(
                assignment => (

                  <article
                    key={
                      assignment.serviceId
                    }
                    className="rounded-[22px] border border-[#e5ddd6] bg-[#fcfaf8] p-5"
                  >

                    <div className="flex items-start justify-between gap-3">

                      <div className="min-w-0">

                        <div className="text-base font-semibold text-[#352f2b]">
                          {assignment.serviceName}
                        </div>

                        <div className="mt-1 break-all text-xs text-[#998a80]">
                          {assignment.serviceSlug
                          || `Servicio #${assignment.serviceId}`}
                        </div>

                      </div>

                      <StatusBadge
                        active={
                          assignment.serviceActive
                          && assignment.serviceBookingEnabled
                        }
                        activeLabel="Reservable"
                        inactiveLabel="No reservable"
                        warning
                      />

                    </div>

                    <div className="mt-5 grid grid-cols-2 gap-3">

                      <div className="rounded-2xl bg-white p-3">

                        <div className="text-[11px] font-semibold uppercase tracking-[0.12em] text-[#9a8b80]">
                          Duración
                        </div>

                        <div className="mt-1 text-sm font-semibold text-[#443a34]">
                          {assignment.durationMinutes} min
                        </div>

                      </div>

                      <div className="rounded-2xl bg-white p-3">

                        <div className="text-[11px] font-semibold uppercase tracking-[0.12em] text-[#9a8b80]">
                          Precio
                        </div>

                        <div className="mt-1 text-sm font-semibold text-[#443a34]">
                          {formatMoney(
                            assignment.price,
                          )}
                        </div>

                      </div>

                    </div>

                    <button
                      type="button"
                      onClick={() =>
                        void handleRemoveService(
                          assignment.serviceId,
                        )
                      }
                      disabled={
                        workingKey != null
                      }
                      className="mt-4 rounded-full border border-[#e3cccc] bg-white px-4 py-2 text-xs font-semibold text-[#8a5c5c] transition hover:bg-[#fbf1f1] disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      {workingKey
                      === `remove-service-${assignment.serviceId}`
                        ? "Eliminando..."
                        : "Quitar servicio"}
                    </button>

                  </article>

                ),
              )}

            </div>

          )}

      </section>

      {/* ===================================================
          WEEKLY AVAILABILITY
      =================================================== */}

      <section className="rounded-[26px] border border-[#e3dbd3] bg-white shadow-[0_10px_35px_rgba(62,49,41,0.04)]">

        <div className="flex flex-col gap-4 border-b border-[#eee7e1] px-6 py-5 md:flex-row md:items-center md:justify-between md:px-7">

          <SectionHeader
            eyebrow="Agenda"
            title="Horario semanal habitual"
            description="Franjas recurrentes en las que el profesional trabaja normalmente."
          />

          <button
            type="button"
            onClick={() =>
              setAvailabilityModalOpen(
                true,
              )
            }
            className="w-fit rounded-full bg-[#342d28] px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-[#211c18]"
          >
            Añadir horario
          </button>

        </div>

        <div className="divide-y divide-[#eee8e3]">

          {DAYS.map(
            day => {

              const daySlots =
                availabilityByDay.get(
                  day.value,
                )
                ?? [];

              return (
                <div
                  key={
                    day.value
                  }
                  className="grid gap-4 px-6 py-5 md:grid-cols-[150px_1fr] md:px-7"
                >

                  <div>

                    <div className="text-sm font-semibold text-[#443a34]">
                      {day.label}
                    </div>

                    <div className="mt-1 text-xs text-[#9a8b80]">

                      {daySlots.length === 0
                        ? "Sin horario"
                        : `${daySlots.length} ${
                            daySlots.length === 1
                              ? "franja"
                              : "franjas"
                          }`}

                    </div>

                  </div>

                  <div className="flex flex-wrap gap-2">

                    {daySlots.length === 0
                      ? (

                        <span className="rounded-full border border-[#e7dfd8] bg-[#faf8f6] px-3.5 py-2 text-xs font-medium text-[#a19389]">
                          Cerrado / no disponible
                        </span>

                      )
                      : daySlots.map(
                          slot => (

                            <div
                              key={
                                slot.id
                              }
                              className={[
                                "inline-flex items-center gap-2 rounded-full border px-3.5 py-2 text-xs font-semibold",

                                slot.active
                                  ? "border-[#d7e2d8] bg-[#f2f7f2] text-[#58705c]"
                                  : "border-[#e4dcd6] bg-[#f7f4f2] text-[#8e827a]",
                              ].join(" ")}
                            >

                              <span>

                                {formatTime(
                                  slot.startTime,
                                )}

                                {" – "}

                                {formatTime(
                                  slot.endTime,
                                )}

                              </span>

                              {!slot.active && (

                                <span className="text-[10px] uppercase tracking-[0.08em]">
                                  Inactivo
                                </span>

                              )}

                              <button
                                type="button"
                                aria-label="Eliminar horario"
                                onClick={() =>
                                  void handleDeleteAvailability(
                                    slot.id,
                                  )
                                }
                                disabled={
                                  workingKey != null
                                }
                                className="flex h-5 w-5 items-center justify-center rounded-full bg-white/80 text-sm leading-none text-[#9a6767] transition hover:bg-white disabled:cursor-not-allowed disabled:opacity-50"
                              >
                                ×
                              </button>

                            </div>

                          ),
                        )}

                  </div>

                </div>
              );
            },
          )}

        </div>

      </section>

      {/* ===================================================
          EXCEPTIONS
      =================================================== */}

      <section className="rounded-[26px] border border-[#e3dbd3] bg-white shadow-[0_10px_35px_rgba(62,49,41,0.04)]">

        <div className="flex flex-col gap-4 border-b border-[#eee7e1] px-6 py-5 md:flex-row md:items-center md:justify-between md:px-7">

          <SectionHeader
            eyebrow="Excepciones"
            title="Cambios puntuales de horario"
            description="Días libres, cierres completos u horarios especiales para fechas concretas."
          />

          <button
            type="button"
            onClick={() =>
              setExceptionModalOpen(
                true,
              )
            }
            className="w-fit rounded-full bg-[#342d28] px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-[#211c18]"
          >
            Nueva excepción
          </button>

        </div>

        {scheduleExceptions.length === 0
          ? (

            <EmptyState
              title="Sin excepciones"
              description="El profesional seguirá exclusivamente su horario semanal habitual."
              buttonLabel="Crear excepción"
              onClick={() =>
                setExceptionModalOpen(
                  true,
                )
              }
            />

          )
          : (

            <div className="divide-y divide-[#eee8e3]">

              {scheduleExceptions.map(
                scheduleException => (

                  <article
                    key={
                      scheduleException.id
                    }
                    className="flex flex-col gap-4 px-6 py-5 md:flex-row md:items-center md:justify-between md:px-7"
                  >

                    <div className="flex items-start gap-4">

                      <div
                        className={[
                          "flex h-11 w-11 shrink-0 items-center justify-center rounded-full text-sm font-semibold",

                          scheduleException.type
                          === "UNAVAILABLE"
                            ? "bg-[#f6eaea] text-[#926161]"
                            : "bg-[#eee8df] text-[#766352]",
                        ].join(" ")}
                      >

                        {scheduleException.type
                        === "UNAVAILABLE"
                          ? "×"
                          : "H"}

                      </div>

                      <div>

                        <div className="flex flex-wrap items-center gap-2">

                          <div className="text-sm font-semibold capitalize text-[#3f3732]">

                            {formatDateLong(
                              scheduleException.exceptionDate,
                            )}

                          </div>

                          {!scheduleException.active && (

                            <span className="rounded-full bg-[#f1eeee] px-2.5 py-1 text-[10px] font-semibold uppercase tracking-[0.08em] text-[#796d6d]">
                              Inactiva
                            </span>

                          )}

                        </div>

                        <div className="mt-1 text-sm text-[#82746a]">

                          {scheduleException.type
                          === "UNAVAILABLE"
                            ? "No disponible durante todo el día"
                            : (
                              <>
                                Horario especial ·{" "}
                                {formatTime(
                                  scheduleException.startTime,
                                )}
                                {" – "}
                                {formatTime(
                                  scheduleException.endTime,
                                )}
                              </>
                            )}

                        </div>

                      </div>

                    </div>

                    <button
                      type="button"
                      onClick={() =>
                        void handleDeleteException(
                          scheduleException.id,
                        )
                      }
                      disabled={
                        workingKey != null
                      }
                      className="w-fit rounded-full border border-[#e2cccc] bg-white px-4 py-2 text-xs font-semibold text-[#8b5e5e] transition hover:bg-[#fbf1f1] disabled:cursor-not-allowed disabled:opacity-50"
                    >

                      {workingKey
                      === `exception-${scheduleException.id}`
                        ? "Eliminando..."
                        : "Eliminar"}

                    </button>

                  </article>

                ),
              )}

            </div>

          )}

      </section>

      {/* ===================================================
          SERVICE MODAL
      =================================================== */}

      {serviceModalOpen && (

        <Modal
          eyebrow="Servicios"
          title="Gestionar servicios"
          subtitle="Selecciona los servicios que puede realizar este profesional."
          onClose={() => {

            if (
              workingKey == null
            ) {
              setServiceModalOpen(
                false,
              );
            }
          }}
        >

          <div className="space-y-3">

            {services.length === 0 && (

              <div className="bp-empty rounded-2xl border border-[#e6ddd5] bg-[#faf8f5] p-5 text-center text-sm text-[#5e534b]">
                No hay servicios creados en el negocio.
                <Link
                  href="/business/services"
                  className="mt-4 rounded-full bg-[#342d28] px-5 py-2.5 text-sm font-semibold !text-white"
                >
                  Crear servicio
                </Link>
              </div>

            )}

            {services.map(
              service => {

                const assigned =
                  assignedServiceIds.has(
                    service.id,
                  );

                const assignment =
                  assignedServices.find(
                    current =>
                      current.serviceId
                      === service.id,
                  );

                const operationKey =
                  assigned
                    ? `remove-service-${service.id}`
                    : `assign-service-${service.id}`;

                return (
                  <div
                    key={
                      service.id
                    }
                    className="flex flex-col gap-4 rounded-[20px] border border-[#e5ddd6] bg-white p-4 sm:flex-row sm:items-center sm:justify-between"
                  >

                    <div className="min-w-0">

                      <div className="flex flex-wrap items-center gap-2">

                        <div className="text-sm font-semibold text-[#3d3530]">

                          {assignment?.serviceName
                          ?? service.name}

                        </div>

                        {assigned && (

                          <span className="rounded-full bg-[#edf4ee] px-2.5 py-1 text-[10px] font-semibold uppercase tracking-[0.08em] text-[#59715e]">
                            Asignado
                          </span>

                        )}

                        {!service.active && (

                          <span className="rounded-full bg-[#f1eeee] px-2.5 py-1 text-[10px] font-semibold uppercase tracking-[0.08em] text-[#796d6d]">
                            Inactivo
                          </span>

                        )}

                      </div>

                      <div className="mt-1 text-xs text-[#94857a]">

                        {service.durationMinutes} min

                        {" · "}

                        {formatMoney(
                          service.price,
                        )}

                      </div>

                    </div>

                    <button
                      type="button"
                      disabled={
                        workingKey != null
                      }
                      onClick={() => {

                        if (
                          assigned
                        ) {

                          void handleRemoveService(
                            service.id,
                          );

                          return;
                        }

                        void handleAssignService(
                          service.id,
                        );
                      }}
                      className={[
                        "inline-flex w-fit items-center justify-center gap-2 rounded-full px-4 py-2 text-xs font-semibold transition disabled:cursor-not-allowed disabled:opacity-70",

                        assigned
                          ? "border border-[#e4cece] bg-[#fffafa] text-[#8d5e5e] hover:bg-[#f9eeee]"
                          : "bg-[#342d28] text-white hover:bg-[#211c18]",
                      ].join(" ")}
                    >

                      {workingKey === operationKey && (

                        <span
                          aria-hidden="true"
                          className={[
                            "h-3.5 w-3.5 animate-spin rounded-full border-2",

                            assigned
                              ? "border-[#d6bcbc] border-t-[#8d5e5e]"
                              : "border-white/35 border-t-white",
                          ].join(" ")}
                        />

                      )}

                      <span>

                        {workingKey === operationKey
                          ? "Guardando..."
                          : assigned
                            ? "Quitar"
                            : "Asignar"}

                      </span>

                    </button>

                  </div>
                );
              },
            )}

            {services.length > 0
            && availableServicesToAssign.length === 0
            && assignedServices.length > 0 && (

              <div className="rounded-2xl border border-[#dce8dd] bg-[#f1f7f1] px-4 py-3 text-sm text-[#59705c]">
                Todos los servicios disponibles están asignados.
              </div>

            )}

          </div>

        </Modal>

      )}

      {/* ===================================================
          AVAILABILITY MODAL
      =================================================== */}

      {availabilityModalOpen && (

        <Modal
          eyebrow="Horario habitual"
          title="Añadir franja semanal"
          subtitle="Configura un tramo recurrente de trabajo para este profesional."
          onClose={() => {

            if (
              workingKey == null
            ) {
              setAvailabilityModalOpen(
                false,
              );
            }
          }}
        >

          <div className="space-y-5">

            <label className="block">

              <FieldLabel>
                Día de la semana
              </FieldLabel>

              <select
                value={
                  availabilityDay
                }
                onChange={
                  event =>
                    setAvailabilityDay(
                      parseDayOfWeek(
                        event.target.value,
                      ),
                    )
                }
                disabled={
                  workingKey != null
                }
                className="mt-2 w-full rounded-2xl border border-[#ddd3cb] bg-white px-4 py-3 text-sm text-[#3c342f] outline-none transition focus:border-[#b9a89c] focus:ring-2 focus:ring-[#ded3cb]/50 disabled:cursor-not-allowed disabled:opacity-60"
              >

                {DAYS.map(
                  day => (

                    <option
                      key={
                        day.value
                      }
                      value={
                        day.value
                      }
                    >
                      {day.label}
                    </option>

                  ),
                )}

              </select>

            </label>

            <div className="grid gap-4 sm:grid-cols-2">

              <label className="block">

                <FieldLabel>
                  Hora de inicio
                </FieldLabel>

                <input
                  type="time"
                  value={
                    availabilityStart
                  }
                  onChange={
                    event =>
                      setAvailabilityStart(
                        event.target.value,
                      )
                  }
                  disabled={
                    workingKey != null
                  }
                  className="mt-2 w-full rounded-2xl border border-[#ddd3cb] bg-white px-4 py-3 text-sm text-[#3c342f] outline-none transition focus:border-[#b9a89c] focus:ring-2 focus:ring-[#ded3cb]/50 disabled:cursor-not-allowed disabled:opacity-60"
                />

              </label>

              <label className="block">

                <FieldLabel>
                  Hora de fin
                </FieldLabel>

                <input
                  type="time"
                  value={
                    availabilityEnd
                  }
                  onChange={
                    event =>
                      setAvailabilityEnd(
                        event.target.value,
                      )
                  }
                  disabled={
                    workingKey != null
                  }
                  className="mt-2 w-full rounded-2xl border border-[#ddd3cb] bg-white px-4 py-3 text-sm text-[#3c342f] outline-none transition focus:border-[#b9a89c] focus:ring-2 focus:ring-[#ded3cb]/50 disabled:cursor-not-allowed disabled:opacity-60"
                />

              </label>

            </div>

            <div className="rounded-2xl border border-[#e6ddd5] bg-[#faf8f5] p-4 text-xs leading-6 text-[#85766c]">
              Puedes crear varias franjas para el mismo día, por ejemplo
              09:00–14:00 y 16:00–20:00. El backend impedirá que se solapen.
            </div>

            <ModalActions
              saving={
                workingKey
                === "create-availability"
              }
              submitLabel="Añadir horario"
              onCancel={() => {

                if (
                  workingKey == null
                ) {
                  setAvailabilityModalOpen(
                    false,
                  );
                }
              }}
              onSubmit={() =>
                void handleCreateAvailability()
              }
            />

          </div>

        </Modal>

      )}

      {/* ===================================================
          EXCEPTION MODAL
      =================================================== */}

      {exceptionModalOpen && (

        <Modal
          eyebrow="Excepción"
          title="Nueva excepción de horario"
          subtitle="Modifica la disponibilidad para una fecha concreta."
          onClose={() => {

            if (
              workingKey == null
            ) {
              setExceptionModalOpen(
                false,
              );
            }
          }}
        >

          <div className="space-y-5">

            <label className="block">

              <FieldLabel>
                Tipo
              </FieldLabel>

              <select
                value={
                  exceptionType
                }
                onChange={
                  event =>
                    setExceptionType(
                      parseScheduleExceptionType(
                        event.target.value,
                      ),
                    )
                }
                disabled={
                  workingKey != null
                }
                className="mt-2 w-full rounded-2xl border border-[#ddd3cb] bg-white px-4 py-3 text-sm text-[#3c342f] outline-none transition focus:border-[#b9a89c] focus:ring-2 focus:ring-[#ded3cb]/50 disabled:cursor-not-allowed disabled:opacity-60"
              >

                <option value="UNAVAILABLE">
                  Día no disponible
                </option>

                <option value="CUSTOM_HOURS">
                  Horario especial
                </option>

              </select>

            </label>

            <label className="block">

              <FieldLabel>
                Fecha
              </FieldLabel>

              <input
                type="date"
                value={
                  exceptionDate
                }
                onChange={
                  event =>
                    setExceptionDate(
                      event.target.value,
                    )
                }
                disabled={
                  workingKey != null
                }
                className="mt-2 w-full rounded-2xl border border-[#ddd3cb] bg-white px-4 py-3 text-sm text-[#3c342f] outline-none transition focus:border-[#b9a89c] focus:ring-2 focus:ring-[#ded3cb]/50 disabled:cursor-not-allowed disabled:opacity-60"
              />

            </label>

            {exceptionType
            === "CUSTOM_HOURS" && (

              <div className="grid gap-4 sm:grid-cols-2">

                <label className="block">

                  <FieldLabel>
                    Hora de inicio
                  </FieldLabel>

                  <input
                    type="time"
                    value={
                      exceptionStart
                    }
                    onChange={
                      event =>
                        setExceptionStart(
                          event.target.value,
                        )
                    }
                    disabled={
                      workingKey != null
                    }
                    className="mt-2 w-full rounded-2xl border border-[#ddd3cb] bg-white px-4 py-3 text-sm text-[#3c342f] outline-none transition focus:border-[#b9a89c] focus:ring-2 focus:ring-[#ded3cb]/50 disabled:cursor-not-allowed disabled:opacity-60"
                  />

                </label>

                <label className="block">

                  <FieldLabel>
                    Hora de fin
                  </FieldLabel>

                  <input
                    type="time"
                    value={
                      exceptionEnd
                    }
                    onChange={
                      event =>
                        setExceptionEnd(
                          event.target.value,
                        )
                    }
                    disabled={
                      workingKey != null
                    }
                    className="mt-2 w-full rounded-2xl border border-[#ddd3cb] bg-white px-4 py-3 text-sm text-[#3c342f] outline-none transition focus:border-[#b9a89c] focus:ring-2 focus:ring-[#ded3cb]/50 disabled:cursor-not-allowed disabled:opacity-60"
                  />

                </label>

              </div>

            )}

            <div className="rounded-2xl border border-[#e6ddd5] bg-[#faf8f5] p-4 text-xs leading-6 text-[#85766c]">

              {exceptionType === "UNAVAILABLE"
                ? (
                  "El profesional quedará completamente no disponible durante esa fecha."
                )
                : (
                  "Ese día se utilizará el horario especial indicado en lugar del horario semanal habitual."
                )}

            </div>

            <ModalActions
              saving={
                workingKey
                === "create-exception"
              }
              submitLabel="Crear excepción"
              onCancel={() => {

                if (
                  workingKey == null
                ) {
                  setExceptionModalOpen(
                    false,
                  );
                }
              }}
              onSubmit={() =>
                void handleCreateException()
              }
            />

          </div>

        </Modal>

      )}

    </div>
  );
}

/* =========================================================
   SECTION HEADER
========================================================= */

type SectionHeaderProps = {
  eyebrow: string;
  title: string;
  description: string;
};

function SectionHeader({
  eyebrow,
  title,
  description,
}: SectionHeaderProps) {

  return (
    <div>

      <div className="text-[11px] font-semibold uppercase tracking-[0.18em] text-[#a18c7f]">
        {eyebrow}
      </div>

      <h2 className="mt-1.5 text-xl font-semibold tracking-[-0.025em] text-[#332d29]">
        {title}
      </h2>

      <p className="mt-1.5 max-w-2xl text-sm leading-6 text-[#86776d]">
        {description}
      </p>

    </div>
  );
}

/* =========================================================
   HEADER METRIC
========================================================= */

type HeaderMetricProps = {
  label: string;
  value: string;
  description: string;
};

function HeaderMetric({
  label,
  value,
  description,
}: HeaderMetricProps) {

  return (
    <div className="bg-white px-6 py-5 md:px-7">

      <div className="text-[11px] font-semibold uppercase tracking-[0.14em] text-[#9b8b80]">
        {label}
      </div>

      <div className="mt-2 text-xl font-semibold text-[#3d3530]">
        {value}
      </div>

      <div className="mt-1 text-xs text-[#9b8d83]">
        {description}
      </div>

    </div>
  );
}

/* =========================================================
   INFO ITEM
========================================================= */

type InfoItemProps = {
  label: string;
  value: string;
};

function InfoItem({
  label,
  value,
}: InfoItemProps) {

  return (
    <div className="rounded-[18px] border border-[#e9e1da] bg-[#fcfaf8] p-4">

      <div className="text-[11px] font-semibold uppercase tracking-[0.12em] text-[#9b8b80]">
        {label}
      </div>

      <div className="mt-2 break-words text-sm font-medium text-[#4e443e]">
        {value}
      </div>

    </div>
  );
}

/* =========================================================
   STATUS ROW
========================================================= */

type StatusRowProps = {
  title: string;
  description: string;
  active: boolean;
};

function StatusRow({
  title,
  description,
  active,
}: StatusRowProps) {

  return (
    <div className="flex items-center justify-between gap-4 rounded-[20px] border border-[#e9e1da] bg-[#faf8f5] p-4">

      <div>

        <div className="text-sm font-semibold text-[#443a34]">
          {title}
        </div>

        <div className="mt-1 text-xs leading-5 text-[#8b7c72]">
          {description}
        </div>

      </div>

      <div
        className={[
          "h-3 w-3 shrink-0 rounded-full",

          active
            ? "bg-[#69816d]"
            : "bg-[#c3b8b0]",
        ].join(" ")}
      />

    </div>
  );
}

/* =========================================================
   STATUS BADGE
========================================================= */

type StatusBadgeProps = {
  active: boolean;
  activeLabel: string;
  inactiveLabel: string;
  warning?: boolean;
};

function StatusBadge({
  active,
  activeLabel,
  inactiveLabel,
  warning = false,
}: StatusBadgeProps) {

  return (
    <span
      className={[
        "inline-flex rounded-full px-3 py-1 text-[11px] font-semibold",

        active
          ? "bg-[#edf4ee] text-[#55705b]"
          : warning
            ? "bg-[#f7f0e5] text-[#907147]"
            : "bg-[#f1eeee] text-[#796d6d]",
      ].join(" ")}
    >

      {active
        ? activeLabel
        : inactiveLabel}

    </span>
  );
}

/* =========================================================
   EMPTY STATE
========================================================= */

type EmptyStateProps = {
  title: string;
  description: string;
  buttonLabel: string;
  onClick: () => void;
};

function EmptyState({
  title,
  description,
  buttonLabel,
  onClick,
}: EmptyStateProps) {

  return (
    <div className="px-6 py-12 text-center md:px-7">

      <div className="text-base font-semibold text-[#443a34]">
        {title}
      </div>

      <p className="mx-auto mt-2 max-w-lg text-sm leading-6 text-[#8a7b71]">
        {description}
      </p>

      <button
        type="button"
        onClick={
          onClick
        }
        className="mt-5 rounded-full border border-[#d8cec6] bg-[#f8f4f0] px-5 py-2.5 text-sm font-semibold text-[#5f534b] transition hover:bg-[#eee7e1]"
      >
        {buttonLabel}
      </button>

    </div>
  );
}

/* =========================================================
   MODAL
========================================================= */

type ModalProps = {
  eyebrow: string;
  title: string;
  subtitle: string;

  children: ReactNode;

  onClose: () => void;
};

function Modal({
  eyebrow,
  title,
  subtitle,
  children,
  onClose,
}: ModalProps) {

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-[#241f1b]/45 p-4 backdrop-blur-sm">

      <button
        type="button"
        aria-label="Cerrar"
        onClick={
          onClose
        }
        className="absolute inset-0 cursor-default"
      />

      <div className="relative z-10 max-h-[calc(100vh-32px)] w-full max-w-2xl overflow-y-auto rounded-[28px] border border-[#e0d6ce] bg-[#fdfbf9] shadow-[0_30px_90px_rgba(36,31,27,0.22)]">

        <div className="flex items-start justify-between gap-4 border-b border-[#ebe3dd] px-6 py-5">

          <div>

            <div className="text-[11px] font-semibold uppercase tracking-[0.18em] text-[#a18c7f]">
              {eyebrow}
            </div>

            <h2 className="mt-2 text-2xl font-semibold tracking-[-0.03em] text-[#312b27]">
              {title}
            </h2>

            <p className="mt-1 text-sm leading-6 text-[#8a7b71]">
              {subtitle}
            </p>

          </div>

          <button
            type="button"
            aria-label="Cerrar"
            onClick={
              onClose
            }
            className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-[#ddd3cb] bg-white text-lg text-[#76685f] transition hover:bg-[#f5f1ed]"
          >
            ×
          </button>

        </div>

        <div className="p-6">
          {children}
        </div>

      </div>

    </div>
  );
}

/* =========================================================
   FIELD LABEL
========================================================= */

function FieldLabel({
  children,
}: {
  children: ReactNode;
}) {

  return (
    <span className="text-xs font-semibold uppercase tracking-[0.12em] text-[#8e7e73]">
      {children}
    </span>
  );
}

/* =========================================================
   MODAL ACTIONS
========================================================= */

type ModalActionsProps = {
  saving: boolean;

  submitLabel: string;

  onCancel: () => void;
  onSubmit: () => void;
};

function ModalActions({
  saving,
  submitLabel,
  onCancel,
  onSubmit,
}: ModalActionsProps) {

  return (
    <div className="flex flex-col-reverse gap-3 border-t border-[#ebe3dd] pt-5 sm:flex-row sm:justify-end">

      <button
        type="button"
        onClick={
          onCancel
        }
        disabled={
          saving
        }
        className="rounded-full border border-[#d9cfc7] bg-white px-5 py-2.5 text-sm font-semibold text-[#6e6057] transition hover:bg-[#f5f1ed] disabled:cursor-not-allowed disabled:opacity-50"
      >
        Cancelar
      </button>

      <button
        type="button"
        onClick={
          onSubmit
        }
        disabled={
          saving
        }
        aria-busy={
          saving
        }
        className="inline-flex min-w-[142px] items-center justify-center gap-2 rounded-full bg-[#342d28] px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-[#211c18] disabled:cursor-not-allowed disabled:opacity-80"
      >

        {saving && (

          <span
            aria-hidden="true"
            className="h-4 w-4 animate-spin rounded-full border-2 border-white/35 border-t-white"
          />

        )}

        <span>
          {saving
            ? "Guardando..."
            : submitLabel}
        </span>

      </button>

    </div>
  );
}