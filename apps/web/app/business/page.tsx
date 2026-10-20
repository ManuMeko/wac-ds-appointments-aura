"use client";

import Link from "next/link";

import {
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";

import { business } from "@/config/business";
import {
  ApiError,
  apiGet,
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

type CustomerResponse = {
  id: number;
  businessId: number;

  firstName: string | null;
  lastName: string | null;
  fullName: string | null;

  email: string | null;
  phone: string | null;
  notes: string | null;

  active: boolean;

  createdAt: string;
  updatedAt: string;
};

type ProfessionalResponse = {
  id: number;
  businessId: number;

  firstName: string | null;
  lastName: string | null;
  displayName: string | null;

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
  businessId: number;

  name: string;
  slug: string;

  description: string | null;

  durationMinutes: number;
  price: number | null;

  active: boolean;
  bookingEnabled: boolean;

  createdAt: string;
  updatedAt: string;
};

type DashboardMetric = {
  label: string;
  value: string;
  helper: string;
};

type QuickAction = {
  label: string;
  href: string;
  description: string;
};

/* =========================================================
   CONSTANTS
========================================================= */

const ACCESS_TOKEN_STORAGE_KEY =
  "wacds_access_token";

const BUSINESS_SESSION_STORAGE_KEY =
  "wacds_business_session";

const DEFAULT_TIMEZONE =
  "Europe/Madrid";

const quickActions: QuickAction[] = [
  {
    label:
      "Ver agenda",

    href:
      "/business/appointments",

    description:
      "Consulta y gestiona todas las citas.",
  },

  {
    label:
      "Añadir cliente",

    href:
      "/business/customers",

    description:
      "Gestiona clientes e historial.",
  },

  {
    label:
      "Gestionar servicios",

    href:
      "/business/services",

    description:
      "Edita precios, duración y disponibilidad.",
  },

  {
    label:
      "Profesionales",

    href:
      "/business/professionals",

    description:
      "Configura equipo, servicios y horarios.",
  },
];

const statusLabels: Record<
  AppointmentStatus,
  string
> = {
  PENDING:
    "Pendiente",

  CONFIRMED:
    "Confirmada",

  CANCELLED:
    "Cancelada",

  COMPLETED:
    "Completada",

  NO_SHOW:
    "No asistió",
};

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

function getStoredBusinessName(): string {

  if (
    typeof window === "undefined"
  ) {
    return business.brand.name;
  }

  const raw =
    window.localStorage.getItem(
      BUSINESS_SESSION_STORAGE_KEY,
    );

  if (
    !raw
  ) {
    return business.brand.name;
  }

  try {

    const parsed =
      JSON.parse(
        raw,
      ) as {
        businessName?: string;
        businessSlug?: string;
      };

    if (
      parsed.businessName
      && parsed.businessName.trim()
    ) {
      return parsed.businessName.trim();
    }

    if (
      parsed.businessSlug === business.slug
    ) {
      return business.brand.name;
    }

  } catch {

    // Mantener fallback.
  }

  return business.brand.name;
}

/* =========================================================
   DATE / TIME
========================================================= */

function getTodayDateKey(): string {

  try {

    const formatter =
      new Intl.DateTimeFormat(
        "en-CA",
        {
          year:
            "numeric",

          month:
            "2-digit",

          day:
            "2-digit",

          timeZone:
            DEFAULT_TIMEZONE,
        },
      );

    const parts =
      formatter.formatToParts(
        new Date(),
      );

    const year =
      parts.find(
        (
          part,
        ) =>
          part.type
          === "year",
      )?.value;

    const month =
      parts.find(
        (
          part,
        ) =>
          part.type
          === "month",
      )?.value;

    const day =
      parts.find(
        (
          part,
        ) =>
          part.type
          === "day",
      )?.value;

    if (
      year
      && month
      && day
    ) {
      return `${year}-${month}-${day}`;
    }

  } catch {

    // Fallback local.
  }

  const now =
    new Date();

  const year =
    now.getFullYear();

  const month =
    String(
      now.getMonth() + 1,
    ).padStart(
      2,
      "0",
    );

  const day =
    String(
      now.getDate(),
    ).padStart(
      2,
      "0",
    );

  return `${year}-${month}-${day}`;
}

function formatAppointmentTime(
  value: string,
  timezone?: string | null,
): string {

  try {

    return new Intl.DateTimeFormat(
      "es-ES",
      {
        hour:
          "2-digit",

        minute:
          "2-digit",

        hour12:
          false,

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

function formatAppointmentDate(
  value: string,
  timezone?: string | null,
): string {

  try {

    return new Intl.DateTimeFormat(
      "es-ES",
      {
        weekday:
          "short",

        day:
          "numeric",

        month:
          "short",

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

    return "";
  }
}

/* =========================================================
   UI HELPERS
========================================================= */

function getStatusClass(
  status: AppointmentStatus,
): string {

  switch (
    status
  ) {

    case "CONFIRMED":
      return "bg-[#edf4ee] text-[#55735c]";

    case "PENDING":
      return "bg-[#f7f0e5] text-[#947348]";

    case "CANCELLED":
      return "bg-[#f7eaea] text-[#9a5f5f]";

    case "COMPLETED":
      return "bg-[#edf1f5] text-[#607283]";

    case "NO_SHOW":
      return "bg-[#f1eeee] text-[#756b6b]";
  }
}

function resolveApiError(
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

    return (
      error.message
      || "No se han podido cargar los datos del dashboard."
    );
  }

  return "No se ha podido conectar con la API Business.";
}

/* =========================================================
   COMPONENT
========================================================= */

export default function BusinessDashboardPage() {

  /* =======================================================
     STATE
  ======================================================= */

  const [
    businessName,
    setBusinessName,
  ] =
    useState<string>(
      business.brand.name,
    );

  const [
    todayAppointments,
    setTodayAppointments,
  ] =
    useState<
      AppointmentResponse[]
    >(
      [],
    );

  const [
    upcomingAppointments,
    setUpcomingAppointments,
  ] =
    useState<
      AppointmentResponse[]
    >(
      [],
    );

  const [
    customers,
    setCustomers,
  ] =
    useState<
      CustomerResponse[]
    >(
      [],
    );

  const [
    professionals,
    setProfessionals,
  ] =
    useState<
      ProfessionalResponse[]
    >(
      [],
    );

  const [
    services,
    setServices,
  ] =
    useState<
      ServiceResponse[]
    >(
      [],
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

  /* =======================================================
     LOAD DASHBOARD
  ======================================================= */

  const loadDashboard =
    useCallback(
      async () => {

        setLoading(
          true,
        );

        setError(
          null,
        );

        setMissingSession(
          false,
        );

        setBusinessName(
          getStoredBusinessName(),
        );

        const accessToken =
          getStoredAccessToken();

        if (
          !accessToken
        ) {

          setTodayAppointments(
            [],
          );

          setUpcomingAppointments(
            [],
          );

          setCustomers(
            [],
          );

          setProfessionals(
            [],
          );

          setServices(
            [],
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

          const today =
            getTodayDateKey();

          const [
            todayResponse,
            upcomingResponse,
            customersResponse,
            professionalsResponse,
            servicesResponse,
          ] =
            await Promise.all(
              [
                apiGet<
                  AppointmentResponse[]
                >(
                  `/api/v1/appointments/day?date=${encodeURIComponent(
                    today,
                  )}`,
                  {
                    accessToken,
                  },
                ),

                apiGet<
                  AppointmentResponse[]
                >(
                  "/api/v1/appointments/upcoming",
                  {
                    accessToken,
                  },
                ),

                apiGet<
                  CustomerResponse[]
                >(
                  "/api/v1/customers/active",
                  {
                    accessToken,
                  },
                ),

                apiGet<
                  ProfessionalResponse[]
                >(
                  "/api/v1/professionals/bookable",
                  {
                    accessToken,
                  },
                ),

                apiGet<
                  ServiceResponse[]
                >(
                  "/api/v1/services",
                  {
                    accessToken,
                  },
                ),
              ],
            );

          setTodayAppointments(
            Array.isArray(
              todayResponse,
            )
              ? todayResponse
              : [],
          );

          setUpcomingAppointments(
            Array.isArray(
              upcomingResponse,
            )
              ? upcomingResponse
              : [],
          );

          setCustomers(
            Array.isArray(
              customersResponse,
            )
              ? customersResponse
              : [],
          );

          setProfessionals(
            Array.isArray(
              professionalsResponse,
            )
              ? professionalsResponse
              : [],
          );

          setServices(
            Array.isArray(
              servicesResponse,
            )
              ? servicesResponse
              : [],
          );

        } catch (
          requestError
        ) {

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
            resolveApiError(
              requestError,
            ),
          );

        } finally {

          setLoading(
            false,
          );
        }
      },
      [],
    );

  useEffect(
    () => {

      void loadDashboard();

    },
    [
      loadDashboard,
    ],
  );

  /* =======================================================
     COMPUTED
  ======================================================= */

  const activeServices =
    useMemo(
      () =>
        services.filter(
          (
            service,
          ) =>
            service.active
            && service.bookingEnabled,
        ),
      [
        services,
      ],
    );

  const pendingToday =
    useMemo(
      () =>
        todayAppointments.filter(
          (
            appointment,
          ) =>
            appointment.status
            === "PENDING",
        ).length,
      [
        todayAppointments,
      ],
    );

  const confirmedToday =
    useMemo(
      () =>
        todayAppointments.filter(
          (
            appointment,
          ) =>
            appointment.status
            === "CONFIRMED",
        ).length,
      [
        todayAppointments,
      ],
    );

  const nextAppointments =
    useMemo(
      () =>
        [...todayAppointments]
          .filter(
            (
              appointment,
            ) =>
              appointment.status
              !== "CANCELLED",
          )
          .sort(
            (
              first,
              second,
            ) =>
              new Date(
                first.startAt,
              ).getTime()
              - new Date(
                second.startAt,
              ).getTime(),
          )
          .slice(
            0,
            5,
          ),
      [
        todayAppointments,
      ],
    );

  const metrics =
    useMemo<
      DashboardMetric[]
    >(
      () => [
        {
          label:
            "Citas hoy",

          value:
            String(
              todayAppointments.length,
            ),

          helper:
            pendingToday === 1
              ? "1 pendiente de confirmar"
              : `${pendingToday} pendientes de confirmar`,
        },

        {
          label:
            "Próximas citas",

          value:
            String(
              upcomingAppointments.length,
            ),

          helper:
            "Próximas reservas activas",
        },

        {
          label:
            "Clientes activos",

          value:
            String(
              customers.length,
            ),

          helper:
            "Disponibles en el negocio",
        },

        {
          label:
            "Servicios",

          value:
            String(
              activeServices.length,
            ),

          helper:
            "Disponibles para reservar",
        },
      ],
      [
        activeServices.length,
        customers.length,
        pendingToday,
        todayAppointments.length,
        upcomingAppointments.length,
      ],
    );

  /* =======================================================
     RENDER
  ======================================================= */

  return (
    <div className="space-y-8 pb-10">

      {/* ===================================================
          HEADER
      =================================================== */}

      <section className="flex flex-col gap-5 xl:flex-row xl:items-end xl:justify-between">

        <div>

          <div className="text-xs font-semibold uppercase tracking-[0.22em] text-[#a18c7f]">
            Dashboard
          </div>

          <h1 className="mt-2 text-3xl font-semibold tracking-[-0.03em] text-[#2f2925] md:text-4xl">
            {businessName}
          </h1>

          <p className="mt-3 max-w-2xl text-sm leading-6 text-[#796b62] md:text-base">
            Resumen en tiempo real de la agenda y de los principales recursos
            del negocio.
          </p>

        </div>

        <div className="flex flex-wrap gap-3">

          <button
            type="button"
            onClick={() =>
              void loadDashboard()
            }
            disabled={
              loading
            }
            className="inline-flex w-fit items-center justify-center rounded-full border border-[#d8cec6] bg-white px-5 py-3 text-sm font-semibold text-[#62564f] transition hover:bg-[#f7f3ef] disabled:cursor-not-allowed disabled:opacity-50"
          >
            {loading
              ? "Actualizando..."
              : "Actualizar"}
          </button>

          <Link
            href="/business/appointments"
            className="inline-flex w-fit items-center justify-center rounded-full bg-[#332c27] px-5 py-3 text-sm font-semibold !text-white transition hover:bg-[#211c18] hover:!text-white"
          >
            Ver agenda
          </Link>

        </div>

      </section>

      {/* ===================================================
          SESSION
      =================================================== */}

      {missingSession && (

        <section className="rounded-[24px] border border-[#e8d6c3] bg-[#fbf5ec] p-5 md:p-6">

          <div className="text-sm font-semibold text-[#7a5a38]">
            Sesión Business necesaria
          </div>

          <p className="mt-2 max-w-3xl text-sm leading-6 text-[#806d5b]">
            Inicia sesión como OWNER, ADMIN o STAFF para cargar la información
            real del negocio.
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
              No se ha podido cargar el dashboard
            </div>

            <p className="mt-1 text-sm text-[#946f6f]">
              {error}
            </p>

          </div>

          <button
            type="button"
            onClick={() =>
              void loadDashboard()
            }
            className="w-fit rounded-full border border-[#ddc5c5] bg-white px-4 py-2 text-xs font-semibold text-[#7e5858] transition hover:bg-[#fffafa]"
          >
            Reintentar
          </button>

        </section>

      )}

      {/* ===================================================
          METRICS
      =================================================== */}

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">

        {metrics.map(
          (
            metric,
          ) => (

            <article
              key={
                metric.label
              }
              className="bp-metric rounded-[24px] border border-[#e5ddd5] bg-white p-6 shadow-[0_10px_35px_rgba(62,49,41,0.04)]"
            >

              <div className="text-sm font-medium text-[#786b62]">
                {metric.label}
              </div>

              {loading
                ? (

                  <div className="mt-5 h-10 w-16 animate-pulse rounded-xl bg-[#f0ebe7]" />

                )
                : (

                  <div className="mt-4 text-4xl font-semibold tracking-[-0.04em] text-[#302a26]">
                    {metric.value}
                  </div>

                )}

              <div className="mt-3 text-xs leading-5 text-[#9a8a7f]">
                {metric.helper}
              </div>

            </article>

          ),
        )}

      </section>

      {/* ===================================================
          AGENDA + QUICK ACTIONS
      =================================================== */}

      <section className="grid gap-6 xl:grid-cols-[1.6fr_0.9fr]">

        {/* TODAY */}

        <article className="overflow-hidden rounded-[26px] border border-[#e3dbd3] bg-white shadow-[0_10px_35px_rgba(62,49,41,0.04)]">

          <div className="flex items-center justify-between border-b border-[#eee7e1] px-6 py-5 md:px-7">

            <div>

              <h2 className="text-lg font-semibold text-[#312b27]">
                Citas de hoy
              </h2>

              <p className="mt-1 text-sm text-[#8a7b71]">
                Agenda real del día.
              </p>

            </div>

            <Link
              href="/business/appointments"
              className="text-sm font-semibold text-[#6e5d52] transition hover:text-[#302a26]"
            >
              Ver todas
            </Link>

          </div>

          {loading && (

            <div className="space-y-3 p-6 md:p-7">

              {Array.from(
                {
                  length:
                    4,
                },
              ).map(
                (
                  _,
                  index,
                ) => (

                  <div
                    key={
                      index
                    }
                    className="h-16 animate-pulse rounded-2xl bg-[#f5f1ed]"
                  />

                ),
              )}

            </div>

          )}

          {!loading
          && !missingSession
          && nextAppointments.length === 0
          && (

            <div className="bp-empty px-6 py-14 text-center md:px-7">

              <div className="text-base font-semibold text-[#3b342f]">
                No hay citas para hoy
              </div>

              <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-[#8a7b71]">
                La agenda está libre para el día de hoy.
              </p>

              <Link
                href="/business/appointments"
                className="mt-5 inline-flex items-center justify-center rounded-full bg-[#342d28] px-4 py-2 text-xs font-semibold !text-white transition hover:bg-[#211c18] hover:!text-white"
              >
                Abrir agenda
              </Link>

            </div>

          )}

          {!loading
          && nextAppointments.length > 0
          && (

            <div className="divide-y divide-[#f0eae4]">

              {nextAppointments.map(
                (
                  appointment,
                ) => (

                  <Link
                    key={
                      appointment.id
                    }
                    href={`/business/appointments/${appointment.id}`}
                    className="grid gap-4 px-6 py-5 transition hover:bg-[#fcfaf8] md:grid-cols-[80px_1.4fr_1.4fr_1.2fr_auto] md:items-center md:px-7"
                  >

                    <div className="text-sm font-semibold text-[#332d29]">
                      {formatAppointmentTime(
                        appointment.startAt,
                        appointment.timezone,
                      )}
                    </div>

                    <div>

                      <div className="text-sm font-semibold text-[#342e2a]">
                        {appointment.customerName
                        || "Cliente sin nombre"}
                      </div>

                      <div className="mt-1 text-xs text-[#94857a] md:hidden">
                        {appointment.serviceName
                        || "Servicio"}
                      </div>

                    </div>

                    <div className="hidden text-sm text-[#6f6259] md:block">
                      {appointment.serviceName
                      || "Servicio"}
                    </div>

                    <div className="hidden text-sm text-[#6f6259] md:block">
                      {appointment.professionalName
                      || "Sin profesional"}
                    </div>

                    <div>

                      <span
                        className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${getStatusClass(
                          appointment.status,
                        )}`}
                      >
                        {
                          statusLabels[
                            appointment.status
                          ]
                        }
                      </span>

                    </div>

                  </Link>

                ),
              )}

            </div>

          )}

        </article>

        {/* QUICK ACTIONS */}

        <aside className="rounded-[26px] border border-[#e3dbd3] bg-[#eee7df] p-6 md:p-7">

          <div>

            <div className="text-xs font-semibold uppercase tracking-[0.2em] text-[#9a8577]">
              Accesos rápidos
            </div>

            <h2 className="mt-2 text-xl font-semibold tracking-[-0.02em] text-[#302a26]">
              Gestiona tu negocio
            </h2>

          </div>

          <div className="mt-6 space-y-3">

            {quickActions.map(
              (
                action,
              ) => (

                <Link
                  key={
                    action.href
                  }
                  href={
                    action.href
                  }
                  className="group block rounded-2xl border border-[#ddd2c8] bg-white/75 p-4 transition hover:-translate-y-0.5 hover:bg-white hover:shadow-sm"
                >

                  <div className="flex items-center justify-between gap-3">

                    <div className="text-sm font-semibold text-[#342e2a]">
                      {action.label}
                    </div>

                    <span className="text-[#a28f82] transition group-hover:translate-x-0.5 group-hover:text-[#5f5046]">
                      →
                    </span>

                  </div>

                  <div className="mt-1 text-xs leading-5 text-[#85766c]">
                    {action.description}
                  </div>

                </Link>

              ),
            )}

          </div>

        </aside>

      </section>

      {/* ===================================================
          BUSINESS OVERVIEW
      =================================================== */}

      <section className="grid gap-5 lg:grid-cols-2">

        {/* RESOURCE STATUS */}

        <article className="rounded-[24px] border border-[#e4dcd4] bg-white p-6 md:p-7">

          <div className="text-xs font-semibold uppercase tracking-[0.2em] text-[#a18d80]">
            Operativa
          </div>

          <h2 className="mt-2 text-lg font-semibold text-[#312b27]">
            Recursos disponibles
          </h2>

          <p className="mt-2 text-sm leading-6 text-[#887970]">
            Estado actual de los recursos que intervienen en las reservas.
          </p>

          <div className="mt-6 space-y-3">

            <Link
              href="/business/customers"
              className="flex items-center justify-between rounded-2xl bg-[#f8f5f1] p-4 transition hover:bg-[#f3eee9]"
            >

              <div>

                <div className="text-sm font-semibold text-[#3b342f]">
                  Clientes activos
                </div>

                <div className="mt-1 text-xs text-[#8a7b71]">
                  Disponibles para nuevas citas
                </div>

              </div>

              <div className="text-xl font-semibold text-[#443b35]">
                {customers.length}
              </div>

            </Link>

            <Link
              href="/business/professionals"
              className="flex items-center justify-between rounded-2xl bg-[#f8f5f1] p-4 transition hover:bg-[#f3eee9]"
            >

              <div>

                <div className="text-sm font-semibold text-[#3b342f]">
                  Profesionales reservables
                </div>

                <div className="mt-1 text-xs text-[#8a7b71]">
                  Activos y habilitados
                </div>

              </div>

              <div className="text-xl font-semibold text-[#443b35]">
                {professionals.length}
              </div>

            </Link>

            <Link
              href="/business/services"
              className="flex items-center justify-between rounded-2xl bg-[#f8f5f1] p-4 transition hover:bg-[#f3eee9]"
            >

              <div>

                <div className="text-sm font-semibold text-[#3b342f]">
                  Servicios reservables
                </div>

                <div className="mt-1 text-xs text-[#8a7b71]">
                  Activos y publicados
                </div>

              </div>

              <div className="text-xl font-semibold text-[#443b35]">
                {activeServices.length}
              </div>

            </Link>

          </div>

        </article>

        {/* SUMMARY */}

        <article className="rounded-[24px] border border-[#e4dcd4] bg-[#342d28] p-6 text-white md:p-7">

          <div className="text-xs font-semibold uppercase tracking-[0.2em] text-[#c7b7ab]">
            Resumen
          </div>

          <h2 className="mt-2 text-xl font-semibold">
            Control del día
          </h2>

          <p className="mt-3 max-w-xl text-sm leading-6 text-[#d7ccc5]">
            Consulta el estado de la agenda y entra directamente en los módulos
            principales del negocio.
          </p>

          <div className="mt-7 grid grid-cols-2 gap-3">

            <div className="rounded-2xl bg-white/8 p-4">

              <div className="text-2xl font-semibold">
                {confirmedToday}
              </div>

              <div className="mt-1 text-xs text-[#cbbeb5]">
                Confirmadas hoy
              </div>

            </div>

            <div className="rounded-2xl bg-white/8 p-4">

              <div className="text-2xl font-semibold">
                {pendingToday}
              </div>

              <div className="mt-1 text-xs text-[#cbbeb5]">
                Pendientes hoy
              </div>

            </div>

          </div>

          <div className="mt-5 rounded-2xl border border-white/10 bg-white/5 p-4">

            <div className="text-xs font-semibold uppercase tracking-[0.14em] text-[#c7b7ab]">
              Próxima actividad
            </div>

            {upcomingAppointments.length > 0
              ? (
                <>

                  <div className="mt-3 text-base font-semibold">
                    {upcomingAppointments[0].customerName
                    || "Próxima cita"}
                  </div>

                  <div className="mt-1 text-sm text-[#d7ccc5]">
                    {formatAppointmentDate(
                      upcomingAppointments[0].startAt,
                      upcomingAppointments[0].timezone,
                    )}

                    {" · "}

                    {formatAppointmentTime(
                      upcomingAppointments[0].startAt,
                      upcomingAppointments[0].timezone,
                    )}

                    {" · "}

                    {upcomingAppointments[0].serviceName
                    || "Servicio"}
                  </div>

                </>
              )
              : (

                <div className="mt-3 text-sm text-[#d7ccc5]">
                  No hay próximas citas activas.
                </div>

              )}

          </div>

          <Link
            href="/business/appointments"
            className="mt-6 inline-flex rounded-full bg-white px-5 py-2.5 text-sm font-semibold !text-[#342d28] transition hover:bg-[#f5f1ed] hover:!text-[#342d28]"
          >
            Abrir agenda
          </Link>

        </article>

      </section>

      {/* ===================================================
          SOURCE
      =================================================== */}

      {!missingSession && (

        <section className="flex flex-col gap-3 rounded-[22px] border border-[#dce5dc] bg-[#eff4ef] p-5 text-sm text-[#5e7160] md:flex-row md:items-center md:justify-between">

          <div>
            Fuente de datos
          </div>

          <div className="font-semibold text-[#405343]">
            API Business · PostgreSQL · datos reales
          </div>

        </section>

      )}

    </div>
  );
}