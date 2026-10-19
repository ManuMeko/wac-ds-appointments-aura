"use client";

import Link from "next/link";
import {
  useParams,
} from "next/navigation";

import {
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";

import {
  ApiError,
  apiGet,
} from "@/lib/api";

// ============================================================
// TYPES
// ============================================================

type AppointmentStatus =
  | "PENDING"
  | "CONFIRMED"
  | "CANCELLED"
  | "COMPLETED"
  | "NO_SHOW";

type CustomerResponse = {
  id: number;

  businessId: number;

  firstName: string;

  lastName: string | null;

  email: string | null;

  phone: string | null;

  notes: string | null;

  active: boolean;

  createdAt: string | null;

  updatedAt: string | null;
};

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

type HistoryFilter =
  | "ALL"
  | AppointmentStatus;

// ============================================================
// CONSTANTS
// ============================================================

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

const filterLabels: Record<
  HistoryFilter,
  string
> = {
  ALL: "Todas",

  PENDING: "Pendientes",

  CONFIRMED: "Confirmadas",

  CANCELLED: "Canceladas",

  COMPLETED: "Completadas",

  NO_SHOW: "No asistió",
};

// ============================================================
// HELPERS
// ============================================================

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

function getStatusClass(
  status: AppointmentStatus,
): string {

  switch (status) {

    case "CONFIRMED":
      return [
        "border-[#dce9de]",
        "bg-[#edf4ee]",
        "text-[#55735c]",
      ].join(
        " ",
      );

    case "PENDING":
      return [
        "border-[#eadcc8]",
        "bg-[#f8f0e5]",
        "text-[#947348]",
      ].join(
        " ",
      );

    case "COMPLETED":
      return [
        "border-[#dde4ea]",
        "bg-[#edf1f5]",
        "text-[#607283]",
      ].join(
        " ",
      );

    case "CANCELLED":
      return [
        "border-[#ecd4d4]",
        "bg-[#f7eaea]",
        "text-[#9a5f5f]",
      ].join(
        " ",
      );

    case "NO_SHOW":
      return [
        "border-[#e3dddd]",
        "bg-[#f1eeee]",
        "text-[#756b6b]",
      ].join(
        " ",
      );
  }
}

function getInitials(
  firstName: string,
  lastName: string | null,
): string {

  const first =
    firstName
      ?.trim()
      .charAt(
        0,
      )
      .toUpperCase()
      ?? "";

  const last =
    lastName
      ?.trim()
      .charAt(
        0,
      )
      .toUpperCase()
      ?? "";

  return (
    `${first}${last}`
    || "CL"
  );
}

function getFullName(
  customer: CustomerResponse,
): string {

  return [
    customer.firstName,
    customer.lastName,
  ]
    .filter(
      Boolean,
    )
    .join(
      " ",
    );
}

function formatDateTime(
  value:
    | string
    | null
    | undefined,
  timezone = DEFAULT_TIMEZONE,
): string {

  if (!value) {
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
    return "—";
  }

  try {

    return new Intl.DateTimeFormat(
      "es-ES",
      {
        day: "2-digit",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
        timeZone:
          timezone,
      },
    ).format(
      date,
    );

  } catch {

    return new Intl.DateTimeFormat(
      "es-ES",
      {
        day: "2-digit",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      },
    ).format(
      date,
    );
  }
}

function formatDate(
  value:
    | string
    | null
    | undefined,
): string {

  if (!value) {
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
    return "—";
  }

  return new Intl.DateTimeFormat(
    "es-ES",
    {
      day: "2-digit",
      month: "long",
      year: "numeric",
    },
  ).format(
    date,
  );
}

function formatCurrency(
  value:
    | number
    | null
    | undefined,
): string {

  if (
    value === null
      || value === undefined
      || Number.isNaN(
        value,
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
    value,
  );
}

function getErrorMessage(
  error: unknown,
  fallback: string,
): string {

  if (
    error instanceof ApiError
  ) {
    return (
      error.message
      || fallback
    );
  }

  if (
    error instanceof Error
  ) {
    return (
      error.message
      || fallback
    );
  }

  return fallback;
}

// ============================================================
// ICONS
// ============================================================

function MailIcon() {

  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      className="h-4 w-4"
    >
      <path d="M3.5 6.5h17v11h-17z" />
      <path d="m4 7 8 6 8-6" />
    </svg>
  );
}

function PhoneIcon() {

  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      className="h-4 w-4"
    >
      <path d="M8.2 3.5 5.7 5.2c-.7.5-.9 1.4-.6 2.2 2.2 5.8 5.7 9.3 11.5 11.5.8.3 1.7.1 2.2-.6l1.7-2.5-4.4-2.1-1.6 2c-2.7-1.2-5-3.5-6.2-6.2l2-1.6z" />
    </svg>
  );
}

function CalendarIcon() {

  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      className="h-5 w-5"
    >
      <path d="M5 4.5h14a2 2 0 0 1 2 2v13H3v-13a2 2 0 0 1 2-2Z" />
      <path d="M7 2v5M17 2v5M3 9h18" />
    </svg>
  );
}

function ArrowLeftIcon() {

  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      className="h-4 w-4"
    >
      <path d="M19 12H5" />
      <path d="m10 7-5 5 5 5" />
    </svg>
  );
}

function ArrowRightIcon() {

  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      className="h-4 w-4"
    >
      <path d="M5 12h14" />
      <path d="m14 7 5 5-5 5" />
    </svg>
  );
}

function RefreshIcon() {

  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      className="h-4 w-4"
    >
      <path d="M20 11a8 8 0 1 0-2.3 5.7" />
      <path d="M20 4v7h-7" />
    </svg>
  );
}

// ============================================================
// PAGE
// ============================================================

export default function CustomerDetailPage() {

  const params =
    useParams<{
      id: string;
    }>();

  const customerId =
    useMemo(
      () => {

        const value =
          params?.id;

        if (!value) {
          return null;
        }

        const parsed =
          Number(
            value,
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
      },
      [
        params?.id,
      ],
    );

  const [
    customer,
    setCustomer,
  ] =
    useState<
      CustomerResponse | null
    >(
      null,
    );

  const [
    appointments,
    setAppointments,
  ] =
    useState<
      AppointmentResponse[]
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
    historyFilter,
    setHistoryFilter,
  ] =
    useState<HistoryFilter>(
      "ALL",
    );

  // ==========================================================
  // LOAD
  // ==========================================================

  const loadCustomer =
    useCallback(
      async (
        background = false,
      ) => {

        if (
          customerId == null
        ) {

          setCustomer(
            null,
          );

          setAppointments(
            [],
          );

          setError(
            "El identificador del cliente no es válido.",
          );

          setLoading(
            false,
          );

          return;
        }

        const accessToken =
          getStoredAccessToken();

        if (!accessToken) {

          setCustomer(
            null,
          );

          setAppointments(
            [],
          );

          setMissingSession(
            true,
          );

          setError(
            "No existe una sesión Business válida.",
          );

          setLoading(
            false,
          );

          return;
        }

        if (background) {

          setRefreshing(
            true,
          );

        } else {

          setLoading(
            true,
          );
        }

        setMissingSession(
          false,
        );

        setError(
          null,
        );

        try {

          const [
            customerResponse,
            appointmentResponse,
          ] =
            await Promise.all(
              [
                apiGet<
                  CustomerResponse
                >(
                  `/api/v1/customers/${customerId}`,
                  {
                    accessToken,
                  },
                ),

                apiGet<
                  AppointmentResponse[]
                >(
                  `/api/v1/customers/${customerId}/appointments`,
                  {
                    accessToken,
                  },
                ),
              ],
            );

          setCustomer(
            customerResponse,
          );

          setAppointments(
            Array.isArray(
              appointmentResponse,
            )
              ? appointmentResponse
              : [],
          );

        } catch (
          loadError
        ) {

          if (
            loadError instanceof ApiError
              && (
                loadError.status === 401
                  || loadError.status === 403
              )
          ) {
            setMissingSession(
              true,
            );
          }

          setError(
            getErrorMessage(
              loadError,
              "No se ha podido cargar la ficha del cliente.",
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
        customerId,
      ],
    );

  useEffect(
    () => {
      void loadCustomer();
    },
    [
      loadCustomer,
    ],
  );

  // ==========================================================
  // DERIVED DATA
  // ==========================================================

  const sortedAppointments =
    useMemo(
      () =>
        [
          ...appointments,
        ].sort(
          (
            first,
            second,
          ) =>
            new Date(
              second.startAt,
            ).getTime()
              - new Date(
                first.startAt,
              ).getTime(),
        ),
      [
        appointments,
      ],
    );

  const filteredAppointments =
    useMemo(
      () => {

        if (
          historyFilter
            === "ALL"
        ) {
          return sortedAppointments;
        }

        return sortedAppointments.filter(
          appointment =>
            appointment.status
              === historyFilter,
        );
      },
      [
        historyFilter,
        sortedAppointments,
      ],
    );

  const appointmentStats =
    useMemo(
      () => {

        const now =
          Date.now();

        const total =
          appointments.length;

        const completed =
          appointments.filter(
            appointment =>
              appointment.status
                === "COMPLETED",
          ).length;

        const cancelled =
          appointments.filter(
            appointment =>
              appointment.status
                === "CANCELLED",
          ).length;

        const noShow =
          appointments.filter(
            appointment =>
              appointment.status
                === "NO_SHOW",
          ).length;

        const future =
          appointments
            .filter(
              appointment =>
                (
                  appointment.status
                    === "PENDING"
                    || appointment.status
                    === "CONFIRMED"
                )
                  && new Date(
                    appointment.startAt,
                  ).getTime()
                    >= now,
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
            );

        return {
          total,
          completed,
          cancelled,
          noShow,
          upcoming:
            future.length,
          next:
            future[0]
            ?? null,
        };
      },
      [
        appointments,
      ],
    );

  const totalSpent =
    useMemo(
      () =>
        appointments
          .filter(
            appointment =>
              appointment.status
                === "COMPLETED",
          )
          .reduce(
            (
              total,
              appointment,
            ) =>
              total
                + (
                  appointment.servicePrice
                  ?? 0
                ),
            0,
          ),
      [
        appointments,
      ],
    );

  // ==========================================================
  // LOADING
  // ==========================================================

  if (loading) {

    return (
      <div className="min-h-[60vh]">

        <div className="mx-auto max-w-[1500px]">

          <div className="animate-pulse space-y-6">

            <div className="h-5 w-36 rounded-full bg-[#e8e1d8]" />

            <div className="rounded-[28px] border border-[#e9e2d9] bg-white p-7 shadow-[0_18px_60px_rgba(72,59,49,0.05)]">

              <div className="flex gap-5">

                <div className="h-20 w-20 rounded-full bg-[#eee8e0]" />

                <div className="flex-1 space-y-3">

                  <div className="h-8 w-64 rounded bg-[#eee8e0]" />

                  <div className="h-4 w-44 rounded bg-[#f2ede7]" />

                  <div className="h-4 w-56 rounded bg-[#f2ede7]" />

                </div>

              </div>

            </div>

            <div className="grid gap-4 md:grid-cols-4">

              {[
                1,
                2,
                3,
                4,
              ].map(
                item => (
                  <div
                    key={item}
                    className="h-28 rounded-[24px] border border-[#e9e2d9] bg-white"
                  />
                ),
              )}

            </div>

            <div className="h-80 rounded-[28px] border border-[#e9e2d9] bg-white" />

          </div>

        </div>

      </div>
    );
  }

  // ==========================================================
  // ERROR
  // ==========================================================

  if (
    error
      || !customer
  ) {

    return (
      <div className="mx-auto max-w-[1500px]">

        <Link
          href="/business/customers"
          className="mb-6 inline-flex items-center gap-2 text-sm font-medium text-[#75685d] transition hover:text-[#413a34]"
        >
          <ArrowLeftIcon />
          Volver a clientes
        </Link>

        <div className="rounded-[28px] border border-[#eadfd5] bg-white px-6 py-14 text-center shadow-[0_18px_60px_rgba(72,59,49,0.05)]">

          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-[#f7f1eb] text-xl text-[#8d7968]">
            !
          </div>

          <h1 className="mt-5 text-2xl font-semibold tracking-[-0.03em] text-[#3f3832]">
            No se ha podido cargar el cliente
          </h1>

          <p className="mx-auto mt-2 max-w-xl text-sm leading-6 text-[#85786e]">
            {error
              ?? "El cliente solicitado no está disponible."}
          </p>

          <div className="mt-6 flex flex-wrap justify-center gap-3">

            {missingSession && (
              <Link
                href="/business/login"
                className="inline-flex items-center rounded-full border border-[#ddd4ca] bg-white px-5 py-2.5 text-sm font-medium text-[#665a50] transition hover:bg-[#faf8f5]"
              >
                Iniciar sesión
              </Link>
            )}

            <button
              type="button"
              onClick={() =>
                void loadCustomer()
              }
              className="inline-flex items-center gap-2 rounded-full bg-[#5f5147] px-5 py-2.5 text-sm font-medium text-white transition hover:bg-[#4e433b]"
            >
              <RefreshIcon />
              Reintentar
            </button>

          </div>

        </div>

      </div>
    );
  }

  const fullName =
    getFullName(
      customer,
    );

  const initials =
    getInitials(
      customer.firstName,
      customer.lastName,
    );

  // ==========================================================
  // RENDER
  // ==========================================================

  return (
    <div className="mx-auto max-w-[1500px] pb-12">

      {/* =================================================== */}
      {/* BACK */}
      {/* =================================================== */}

      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">

        <Link
          href="/business/customers"
          className="inline-flex items-center gap-2 text-sm font-medium text-[#75685d] transition hover:text-[#413a34]"
        >
          <ArrowLeftIcon />
          Volver a clientes
        </Link>

        <button
          type="button"
          disabled={
            refreshing
          }
          onClick={() =>
            void loadCustomer(
              true,
            )
          }
          className="inline-flex items-center gap-2 rounded-full border border-[#ddd4ca] bg-white px-4 py-2 text-sm font-medium text-[#665a50] transition hover:border-[#cfc3b8] hover:bg-[#faf8f5] disabled:cursor-not-allowed disabled:opacity-50"
        >
          <RefreshIcon />

          {refreshing
            ? "Actualizando..."
            : "Actualizar"}
        </button>

      </div>

      {/* =================================================== */}
      {/* CUSTOMER HERO */}
      {/* =================================================== */}

      <section className="overflow-hidden rounded-[30px] border border-[#e8e0d7] bg-white shadow-[0_24px_70px_rgba(74,61,51,0.055)]">

        <div className="h-2 bg-gradient-to-r from-[#c9b5a3] via-[#e8ded4] to-[#cfc1b4]" />

        <div className="p-6 sm:p-8 lg:p-9">

          <div className="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">

            <div className="flex min-w-0 flex-col gap-5 sm:flex-row sm:items-center">

              <div className="flex h-20 w-20 shrink-0 items-center justify-center rounded-full border border-[#ded3c8] bg-[#f3ede7] text-xl font-semibold tracking-[0.08em] text-[#79695b] shadow-inner">
                {initials}
              </div>

              <div className="min-w-0">

                <div className="flex flex-wrap items-center gap-3">

                  <h1 className="break-words text-3xl font-semibold tracking-[-0.04em] text-[#3e3731] sm:text-[34px]">
                    {fullName}
                  </h1>

                  <span
                    className={[
                      "inline-flex rounded-full border px-3 py-1 text-xs font-semibold",

                      customer.active
                        ? "border-[#d9e7dc] bg-[#edf4ee] text-[#55735c]"
                        : "border-[#ead9d9] bg-[#f7ecec] text-[#956363]",
                    ].join(
                      " ",
                    )}
                  >
                    {customer.active
                      ? "Activo"
                      : "Inactivo"}
                  </span>

                </div>

                <p className="mt-1.5 text-sm text-[#93867b]">
                  Cliente #{customer.id}
                </p>

                <div className="mt-4 flex flex-wrap gap-x-5 gap-y-2 text-sm text-[#6f6359]">

                  {customer.email && (
                    <a
                      href={`mailto:${customer.email}`}
                      className="inline-flex items-center gap-2 transition hover:text-[#413a34]"
                    >
                      <MailIcon />
                      {customer.email}
                    </a>
                  )}

                  {customer.phone && (
                    <a
                      href={`tel:${customer.phone}`}
                      className="inline-flex items-center gap-2 transition hover:text-[#413a34]"
                    >
                      <PhoneIcon />
                      {customer.phone}
                    </a>
                  )}

                </div>

              </div>

            </div>

            <div className="flex flex-wrap gap-3">

              <Link
                href="/business/appointments"
                className="inline-flex items-center justify-center gap-2 rounded-full border border-[#ddd4ca] bg-white px-5 py-2.5 text-sm font-medium text-[#62574e] transition hover:border-[#cfc2b7] hover:bg-[#faf8f5]"
              >
                <CalendarIcon />
                Ver agenda
              </Link>

            </div>

          </div>

        </div>

      </section>

      {/* =================================================== */}
      {/* METRICS */}
      {/* =================================================== */}

      <section className="mt-5 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">

        <MetricCard
          label="Citas totales"
          value={String(
            appointmentStats.total,
          )}
          helper="Histórico completo"
        />

        <MetricCard
          label="Completadas"
          value={String(
            appointmentStats.completed,
          )}
          helper="Servicios finalizados"
        />

        <MetricCard
          label="Próximas"
          value={String(
            appointmentStats.upcoming,
          )}
          helper="Pendientes o confirmadas"
        />

        <MetricCard
          label="Facturación histórica"
          value={formatCurrency(
            totalSpent,
          )}
          helper="Citas completadas"
        />

      </section>

      {/* =================================================== */}
      {/* CONTENT GRID */}
      {/* =================================================== */}

      <div className="mt-5 grid gap-5 xl:grid-cols-[360px_minmax(0,1fr)]">

        {/* ================================================= */}
        {/* LEFT */}
        {/* ================================================= */}

        <div className="space-y-5">

          <section className="rounded-[26px] border border-[#e8e1d8] bg-white p-6 shadow-[0_16px_50px_rgba(72,59,49,0.04)]">

            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[#a49384]">
              Contacto
            </p>

            <div className="mt-5 space-y-5">

              <DetailItem
                label="Nombre"
                value={
                  customer.firstName
                  || "—"
                }
              />

              <DetailItem
                label="Apellidos"
                value={
                  customer.lastName
                  || "—"
                }
              />

              <DetailItem
                label="Email"
                value={
                  customer.email
                  || "—"
                }
              />

              <DetailItem
                label="Teléfono"
                value={
                  customer.phone
                  || "—"
                }
              />

            </div>

          </section>

          <section className="rounded-[26px] border border-[#e8e1d8] bg-white p-6 shadow-[0_16px_50px_rgba(72,59,49,0.04)]">

            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[#a49384]">
              Notas
            </p>

            <p className="mt-4 whitespace-pre-wrap text-sm leading-6 text-[#6c6158]">
              {customer.notes?.trim()
                || "No hay notas internas para este cliente."}
            </p>

          </section>

          <section className="rounded-[26px] border border-[#e8e1d8] bg-white p-6 shadow-[0_16px_50px_rgba(72,59,49,0.04)]">

            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[#a49384]">
              Información
            </p>

            <div className="mt-5 space-y-5">

              <DetailItem
                label="Cliente desde"
                value={formatDate(
                  customer.createdAt,
                )}
              />

              <DetailItem
                label="Última actualización"
                value={formatDateTime(
                  customer.updatedAt,
                )}
              />

              <DetailItem
                label="Business ID"
                value={`#${customer.businessId}`}
              />

            </div>

          </section>

        </div>

        {/* ================================================= */}
        {/* RIGHT */}
        {/* ================================================= */}

        <div className="space-y-5">

          {/* NEXT APPOINTMENT */}

          <section className="rounded-[26px] border border-[#e8e1d8] bg-white p-6 shadow-[0_16px_50px_rgba(72,59,49,0.04)] sm:p-7">

            <div className="flex flex-wrap items-center justify-between gap-4">

              <div>

                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[#a49384]">
                  Próxima cita
                </p>

                <h2 className="mt-2 text-xl font-semibold tracking-[-0.025em] text-[#433b35]">
                  {appointmentStats.next
                    ? "Próxima visita programada"
                    : "Sin próximas citas"}
                </h2>

              </div>

              {appointmentStats.next && (
                <Link
                  href={`/business/appointments/${appointmentStats.next.id}`}
                  className="inline-flex items-center gap-2 rounded-full border border-[#ddd3c9] px-4 py-2 text-sm font-medium text-[#665a50] transition hover:bg-[#faf8f5]"
                >
                  Ver cita
                  <ArrowRightIcon />
                </Link>
              )}

            </div>

            {appointmentStats.next ? (

              <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">

                <DetailBlock
                  label="Fecha y hora"
                  value={formatDateTime(
                    appointmentStats.next.startAt,
                    appointmentStats.next.timezone
                      || DEFAULT_TIMEZONE,
                  )}
                />

                <DetailBlock
                  label="Servicio"
                  value={
                    appointmentStats.next.serviceName
                    || `Servicio #${appointmentStats.next.serviceId}`
                  }
                />

                <DetailBlock
                  label="Profesional"
                  value={
                    appointmentStats.next.professionalName
                    || `Profesional #${appointmentStats.next.professionalId}`
                  }
                />

                <div>

                  <p className="text-xs font-medium text-[#9a8d82]">
                    Estado
                  </p>

                  <span
                    className={[
                      "mt-2 inline-flex rounded-full border px-2.5 py-1 text-xs font-semibold",

                      getStatusClass(
                        appointmentStats.next.status,
                      ),
                    ].join(
                      " ",
                    )}
                  >
                    {statusLabels[
                      appointmentStats.next.status
                    ]}
                  </span>

                </div>

              </div>

            ) : (

              <div className="mt-5 rounded-[20px] border border-dashed border-[#dfd5cb] bg-[#fbf9f7] px-5 py-7 text-sm text-[#887b70]">
                Este cliente no tiene ninguna cita futura pendiente o confirmada.
              </div>

            )}

          </section>

          {/* HISTORY */}

          <section className="overflow-hidden rounded-[26px] border border-[#e8e1d8] bg-white shadow-[0_16px_50px_rgba(72,59,49,0.04)]">

            <div className="border-b border-[#eee8e2] px-6 py-6 sm:px-7">

              <div className="flex flex-col gap-5 xl:flex-row xl:items-center xl:justify-between">

                <div>

                  <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[#a49384]">
                    Historial
                  </p>

                  <h2 className="mt-2 text-xl font-semibold tracking-[-0.025em] text-[#433b35]">
                    Historial de citas
                  </h2>

                  <p className="mt-1 text-sm text-[#93867b]">
                    {appointments.length}
                    {" "}
                    {appointments.length === 1
                      ? "cita registrada"
                      : "citas registradas"}
                  </p>

                </div>

                <div className="flex max-w-full gap-2 overflow-x-auto pb-1">

                  {(
                    Object.keys(
                      filterLabels,
                    ) as HistoryFilter[]
                  ).map(
                    filter => {

                      const selected =
                        historyFilter
                          === filter;

                      return (
                        <button
                          key={filter}
                          type="button"
                          onClick={() =>
                            setHistoryFilter(
                              filter,
                            )
                          }
                          className={[
                            "whitespace-nowrap rounded-full border px-3.5 py-2 text-xs font-semibold transition",

                            selected
                              ? "border-[#756559] bg-[#756559] text-white"
                              : "border-[#e0d7ce] bg-white text-[#766a60] hover:bg-[#faf8f5]",
                          ].join(
                            " ",
                          )}
                        >
                          {filterLabels[
                            filter
                          ]}
                        </button>
                      );
                    },
                  )}

                </div>

              </div>

            </div>

            {filteredAppointments.length === 0 ? (

              <div className="px-6 py-14 text-center sm:px-7">

                <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-[#f4eee8] text-[#897769]">
                  <CalendarIcon />
                </div>

                <p className="mt-4 text-sm font-semibold text-[#665a50]">
                  No hay citas en este filtro
                </p>

                <p className="mt-1 text-sm text-[#988a7f]">
                  El historial aparecerá aquí cuando existan registros.
                </p>

              </div>

            ) : (

              <div className="divide-y divide-[#eee8e2]">

                {filteredAppointments.map(
                  appointment => (
                    <AppointmentHistoryRow
                      key={
                        appointment.id
                      }
                      appointment={
                        appointment
                      }
                    />
                  ),
                )}

              </div>

            )}

          </section>

          {/* SECONDARY STATS */}

          <section className="grid gap-4 sm:grid-cols-3">

            <SmallStat
              label="Canceladas"
              value={
                appointmentStats.cancelled
              }
            />

            <SmallStat
              label="No asistió"
              value={
                appointmentStats.noShow
              }
            />

            <SmallStat
              label="Tasa completadas"
              value={
                appointmentStats.total > 0
                  ? `${Math.round(
                      (
                        appointmentStats.completed
                          / appointmentStats.total
                      )
                        * 100,
                    )}%`
                  : "0%"
              }
            />

          </section>

        </div>

      </div>

    </div>
  );
}

// ============================================================
// COMPONENTS
// ============================================================

function MetricCard({
  label,
  value,
  helper,
}: {
  label: string;
  value: string;
  helper: string;
}) {

  return (
    <article className="rounded-[24px] border border-[#e8e1d8] bg-white px-5 py-5 shadow-[0_14px_40px_rgba(72,59,49,0.035)]">

      <p className="text-xs font-medium uppercase tracking-[0.13em] text-[#a09287]">
        {label}
      </p>

      <p className="mt-3 text-2xl font-semibold tracking-[-0.035em] text-[#433b35]">
        {value}
      </p>

      <p className="mt-1 text-xs text-[#9c8f85]">
        {helper}
      </p>

    </article>
  );
}

function DetailItem({
  label,
  value,
}: {
  label: string;
  value: string;
}) {

  return (
    <div>

      <p className="text-xs font-medium text-[#a09287]">
        {label}
      </p>

      <p className="mt-1 break-words text-sm font-medium leading-6 text-[#574e47]">
        {value}
      </p>

    </div>
  );
}

function DetailBlock({
  label,
  value,
}: {
  label: string;
  value: string;
}) {

  return (
    <div>

      <p className="text-xs font-medium text-[#9a8d82]">
        {label}
      </p>

      <p className="mt-2 text-sm font-semibold leading-5 text-[#5c524a]">
        {value}
      </p>

    </div>
  );
}

function SmallStat({
  label,
  value,
}: {
  label: string;
  value:
    | string
    | number;
}) {

  return (
    <article className="rounded-[22px] border border-[#e8e1d8] bg-white p-5 shadow-[0_14px_40px_rgba(72,59,49,0.03)]">

      <p className="text-xs font-medium text-[#9c8f85]">
        {label}
      </p>

      <p className="mt-2 text-xl font-semibold tracking-[-0.03em] text-[#514840]">
        {value}
      </p>

    </article>
  );
}

function AppointmentHistoryRow({
  appointment,
}: {
  appointment:
    AppointmentResponse;
}) {

  const timezone =
    appointment.timezone
    || DEFAULT_TIMEZONE;

  return (
    <Link
      href={`/business/appointments/${appointment.id}`}
      className="group block px-6 py-5 transition hover:bg-[#fcfaf8] sm:px-7"
    >

      <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">

        <div className="min-w-0">

          <div className="flex flex-wrap items-center gap-2.5">

            <p className="font-semibold text-[#4d443d]">
              {appointment.serviceName
                || `Servicio #${appointment.serviceId}`}
            </p>

            <span
              className={[
                "inline-flex rounded-full border px-2.5 py-1 text-[11px] font-semibold",

                getStatusClass(
                  appointment.status,
                ),
              ].join(
                " ",
              )}
            >
              {statusLabels[
                appointment.status
              ]}
            </span>

          </div>

          <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1.5 text-sm text-[#897c72]">

            <span>
              {formatDateTime(
                appointment.startAt,
                timezone,
              )}
            </span>

            <span>
              {appointment.professionalName
                || `Profesional #${appointment.professionalId}`}
            </span>

            {appointment.serviceDurationMinutes !== null && (
              <span>
                {appointment.serviceDurationMinutes} min
              </span>
            )}

          </div>

          {appointment.notes?.trim() && (
            <p className="mt-2 line-clamp-2 text-sm leading-6 text-[#94877c]">
              {appointment.notes}
            </p>
          )}

        </div>

        <div className="flex shrink-0 items-center justify-between gap-5 lg:justify-end">

          <div className="text-left lg:text-right">

            <p className="text-xs text-[#a09287]">
              Importe
            </p>

            <p className="mt-1 text-sm font-semibold text-[#5b5149]">
              {formatCurrency(
                appointment.servicePrice,
              )}
            </p>

          </div>

          <span className="flex h-9 w-9 items-center justify-center rounded-full border border-[#e2d9d0] text-[#8b7d72] transition group-hover:border-[#cabdb2] group-hover:bg-white group-hover:text-[#5b5149]">
            <ArrowRightIcon />
          </span>

        </div>

      </div>

    </Link>
  );
}