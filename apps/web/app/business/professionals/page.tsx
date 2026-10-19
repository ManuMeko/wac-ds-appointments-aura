"use client";

import Link from "next/link";

import type {
  Dispatch,
  SetStateAction,
} from "react";

import {
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";

import {
  ApiError,
  apiGet,
  apiPatch,
  apiPost,
  apiPut,
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

type ProfessionalRequest = {
  firstName: string;
  lastName: string | null;
  displayName: string | null;
  email: string | null;
  phone: string | null;
  bio: string | null;
  active: boolean;
  bookingEnabled: boolean;
};

type ProfessionalFormState = {
  firstName: string;
  lastName: string;
  displayName: string;
  email: string;
  phone: string;
  bio: string;
  active: boolean;
  bookingEnabled: boolean;
};

type ProfessionalFilter =
  | "ALL"
  | "ACTIVE"
  | "INACTIVE"
  | "BOOKABLE"
  | "NOT_BOOKABLE";

/* =========================================================
   CONSTANTS
========================================================= */

const ACCESS_TOKEN_STORAGE_KEY =
  "wacds_access_token";

const initialFormState: ProfessionalFormState = {
  firstName: "",
  lastName: "",
  displayName: "",
  email: "",
  phone: "",
  bio: "",
  active: true,
  bookingEnabled: true,
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

/* =========================================================
   NORMALIZATION
========================================================= */

function normalizeNullableText(
  value: string,
): string | null {

  const normalized =
    value.trim();

  return normalized === ""
    ? null
    : normalized;
}

/* =========================================================
   DISPLAY HELPERS
========================================================= */

function getProfessionalDisplayName(
  professional: ProfessionalResponse,
): string {

  if (
    professional.displayName
    && professional.displayName.trim() !== ""
  ) {
    return professional.displayName.trim();
  }

  const parts = [
    professional.firstName?.trim(),
    professional.lastName?.trim(),
  ].filter(Boolean);

  if (
    parts.length > 0
  ) {
    return parts.join(" ");
  }

  return "Profesional";
}

function getProfessionalInitials(
  professional: ProfessionalResponse,
): string {

  const displayName =
    getProfessionalDisplayName(
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
      .slice(0, 2)
      .toUpperCase();
  }

  return "PR";
}

function formatDate(
  value: string,
): string {

  try {
    return new Intl.DateTimeFormat(
      "es-ES",
      {
        day: "2-digit",
        month: "short",
        year: "numeric",
      },
    ).format(
      new Date(value),
    );
  } catch {
    return "—";
  }
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
      return "No tienes permisos para gestionar profesionales. Esta operación requiere un usuario OWNER o ADMIN.";
    }

    if (
      error.status === 404
    ) {
      return (
        error.message
        || "No se ha encontrado el profesional solicitado."
      );
    }

    if (
      error.status === 409
    ) {
      return (
        error.message
        || "Existe un conflicto con los datos del profesional."
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

export default function ProfessionalsPage() {

  /* =======================================================
     STATE
  ======================================================= */

  const [
    professionals,
    setProfessionals,
  ] =
    useState<ProfessionalResponse[]>(
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
    useState<string | null>(
      null,
    );

  const [
    successMessage,
    setSuccessMessage,
  ] =
    useState<string | null>(
      null,
    );

  const [
    search,
    setSearch,
  ] =
    useState(
      "",
    );

  const [
    filter,
    setFilter,
  ] =
    useState<ProfessionalFilter>(
      "ALL",
    );

  const [
    createOpen,
    setCreateOpen,
  ] =
    useState(
      false,
    );

  const [
    editOpen,
    setEditOpen,
  ] =
    useState(
      false,
    );

  const [
    selectedProfessional,
    setSelectedProfessional,
  ] =
    useState<
      ProfessionalResponse | null
    >(
      null,
    );

  const [
    form,
    setForm,
  ] =
    useState<ProfessionalFormState>(
      initialFormState,
    );

  const [
    formError,
    setFormError,
  ] =
    useState<string | null>(
      null,
    );

  const [
    saving,
    setSaving,
  ] =
    useState(
      false,
    );

  const [
    updatingProfessionalId,
    setUpdatingProfessionalId,
  ] =
    useState<number | null>(
      null,
    );

  /* =======================================================
     LOAD
  ======================================================= */

  const loadProfessionals =
    useCallback(
      async () => {

        setLoading(
          true,
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

          const response =
            await apiGet<
              ProfessionalResponse[]
            >(
              "/api/v1/professionals",
              {
                accessToken,
              },
            );

          setProfessionals(
            Array.isArray(response)
              ? response
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
        }
      },
      [],
    );

  useEffect(
    () => {

      void loadProfessionals();

    },
    [
      loadProfessionals,
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
     ESCAPE MODALS
  ======================================================= */

  useEffect(
    () => {

      if (
        !createOpen
        && !editOpen
      ) {
        return;
      }

      const handleKeyDown =
        (
          event: KeyboardEvent,
        ) => {

          if (
            event.key !== "Escape"
            || saving
          ) {
            return;
          }

          setCreateOpen(
            false,
          );

          setEditOpen(
            false,
          );

          setSelectedProfessional(
            null,
          );

          setForm(
            initialFormState,
          );

          setFormError(
            null,
          );
        };

      window.addEventListener(
        "keydown",
        handleKeyDown,
      );

      return () => {

        window.removeEventListener(
          "keydown",
          handleKeyDown,
        );
      };

    },
    [
      createOpen,
      editOpen,
      saving,
    ],
  );

  /* =======================================================
     METRICS
  ======================================================= */

  const activeProfessionals =
    useMemo(
      () =>
        professionals.filter(
          professional =>
            professional.active,
        ),
      [
        professionals,
      ],
    );

  const inactiveProfessionals =
    useMemo(
      () =>
        professionals.filter(
          professional =>
            !professional.active,
        ),
      [
        professionals,
      ],
    );

  const bookableProfessionals =
    useMemo(
      () =>
        professionals.filter(
          professional =>
            professional.active
            && professional.bookingEnabled,
        ),
      [
        professionals,
      ],
    );

  const notBookableProfessionals =
    useMemo(
      () =>
        professionals.filter(
          professional =>
            !professional.bookingEnabled,
        ),
      [
        professionals,
      ],
    );

  /* =======================================================
     FILTERS
  ======================================================= */

  const filteredProfessionals =
    useMemo(
      () => {

        const normalizedSearch =
          search
            .trim()
            .toLowerCase();

        return professionals.filter(
          professional => {

            if (
              filter === "ACTIVE"
              && !professional.active
            ) {
              return false;
            }

            if (
              filter === "INACTIVE"
              && professional.active
            ) {
              return false;
            }

            if (
              filter === "BOOKABLE"
              && (
                !professional.active
                || !professional.bookingEnabled
              )
            ) {
              return false;
            }

            if (
              filter === "NOT_BOOKABLE"
              && professional.bookingEnabled
            ) {
              return false;
            }

            if (
              normalizedSearch === ""
            ) {
              return true;
            }

            const haystack = [
              professional.firstName,
              professional.lastName,
              professional.displayName,
              professional.email,
              professional.phone,
              professional.bio,
            ]
              .filter(Boolean)
              .join(" ")
              .toLowerCase();

            return haystack.includes(
              normalizedSearch,
            );
          },
        );
      },
      [
        filter,
        professionals,
        search,
      ],
    );

  /* =======================================================
     MODALS
  ======================================================= */

  function openCreateModal() {

    setSelectedProfessional(
      null,
    );

    setForm(
      initialFormState,
    );

    setFormError(
      null,
    );

    setCreateOpen(
      true,
    );
  }

  function closeCreateModal() {

    if (
      saving
    ) {
      return;
    }

    setCreateOpen(
      false,
    );

    setForm(
      initialFormState,
    );

    setFormError(
      null,
    );
  }

  function openEditModal(
    professional: ProfessionalResponse,
  ) {

    setSelectedProfessional(
      professional,
    );

    setForm(
      {
        firstName:
          professional.firstName
          ?? "",

        lastName:
          professional.lastName
          ?? "",

        displayName:
          professional.displayName
          ?? "",

        email:
          professional.email
          ?? "",

        phone:
          professional.phone
          ?? "",

        bio:
          professional.bio
          ?? "",

        active:
          professional.active,

        bookingEnabled:
          professional.bookingEnabled,
      },
    );

    setFormError(
      null,
    );

    setEditOpen(
      true,
    );
  }

  function closeEditModal() {

    if (
      saving
    ) {
      return;
    }

    setEditOpen(
      false,
    );

    setSelectedProfessional(
      null,
    );

    setForm(
      initialFormState,
    );

    setFormError(
      null,
    );
  }

  /* =======================================================
     PAYLOAD
  ======================================================= */

  function buildPayload():
    | ProfessionalRequest
    | null {

    const firstName =
      form.firstName.trim();

    if (
      firstName === ""
    ) {

      setFormError(
        "El nombre del profesional es obligatorio.",
      );

      return null;
    }

    return {
      firstName,

      lastName:
        normalizeNullableText(
          form.lastName,
        ),

      displayName:
        normalizeNullableText(
          form.displayName,
        ),

      email:
        normalizeNullableText(
          form.email,
        ),

      phone:
        normalizeNullableText(
          form.phone,
        ),

      bio:
        normalizeNullableText(
          form.bio,
        ),

      active:
        form.active,

      bookingEnabled:
        form.active
          ? form.bookingEnabled
          : false,
    };
  }

  /* =======================================================
     CREATE
  ======================================================= */

  async function handleCreateProfessional() {

    if (
      saving
    ) {
      return;
    }

    const payload =
      buildPayload();

    if (
      !payload
    ) {
      return;
    }

    setSaving(
      true,
    );

    setFormError(
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
        ProfessionalResponse
      >(
        "/api/v1/professionals",
        payload,
        {
          accessToken,
        },
      );

      setCreateOpen(
        false,
      );

      setForm(
        initialFormState,
      );

      setSuccessMessage(
        "Profesional creado correctamente.",
      );

      await loadProfessionals();

    } catch (
      requestError
    ) {

      setFormError(
        resolveApiError(
          requestError,
        ),
      );

    } finally {

      setSaving(
        false,
      );
    }
  }

  /* =======================================================
     UPDATE
  ======================================================= */

  async function handleUpdateProfessional() {

    if (
      saving
      || !selectedProfessional
    ) {
      return;
    }

    const payload =
      buildPayload();

    if (
      !payload
    ) {
      return;
    }

    setSaving(
      true,
    );

    setFormError(
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

      await apiPut<
        ProfessionalResponse
      >(
        `/api/v1/professionals/${selectedProfessional.id}`,
        payload,
        {
          accessToken,
        },
      );

      setEditOpen(
        false,
      );

      setSelectedProfessional(
        null,
      );

      setForm(
        initialFormState,
      );

      setSuccessMessage(
        "Profesional actualizado correctamente.",
      );

      await loadProfessionals();

    } catch (
      requestError
    ) {

      setFormError(
        resolveApiError(
          requestError,
        ),
      );

    } finally {

      setSaving(
        false,
      );
    }
  }

  /* =======================================================
     ACTIVE
  ======================================================= */

  async function toggleProfessionalActive(
    professional: ProfessionalResponse,
  ) {

    if (
      updatingProfessionalId != null
    ) {
      return;
    }

    const nextActive =
      !professional.active;

    setUpdatingProfessionalId(
      professional.id,
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

      await apiPatch<
        ProfessionalResponse
      >(
        `/api/v1/professionals/${professional.id}/active?active=${String(
          nextActive,
        )}`,
        undefined,
        {
          accessToken,
        },
      );

      setSuccessMessage(
        nextActive
          ? "Profesional activado correctamente."
          : "Profesional desactivado correctamente.",
      );

      await loadProfessionals();

    } catch (
      requestError
    ) {

      setError(
        resolveApiError(
          requestError,
        ),
      );

    } finally {

      setUpdatingProfessionalId(
        null,
      );
    }
  }

  /* =======================================================
     BOOKING
  ======================================================= */

  async function toggleBookingEnabled(
    professional: ProfessionalResponse,
  ) {

    if (
      updatingProfessionalId != null
      || !professional.active
    ) {
      return;
    }

    const nextEnabled =
      !professional.bookingEnabled;

    setUpdatingProfessionalId(
      professional.id,
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

      await apiPatch<
        ProfessionalResponse
      >(
        `/api/v1/professionals/${professional.id}/booking-enabled?enabled=${String(
          nextEnabled,
        )}`,
        undefined,
        {
          accessToken,
        },
      );

      setSuccessMessage(
        nextEnabled
          ? "Reservas habilitadas para el profesional."
          : "Reservas pausadas para el profesional.",
      );

      await loadProfessionals();

    } catch (
      requestError
    ) {

      setError(
        resolveApiError(
          requestError,
        ),
      );

    } finally {

      setUpdatingProfessionalId(
        null,
      );
    }
  }

  /* =======================================================
     RENDER
  ======================================================= */

  return (
    <div className="space-y-7 pb-10">

      {/* ===================================================
          HEADER
      =================================================== */}

      <section className="flex flex-col gap-5 xl:flex-row xl:items-end xl:justify-between">

        <div>

          <div className="text-xs font-semibold uppercase tracking-[0.22em] text-[#a18c7f]">
            Profesionales
          </div>

          <h1 className="mt-2 text-3xl font-semibold tracking-[-0.03em] text-[#2f2925] md:text-4xl">
            Equipo profesional
          </h1>

          <p className="mt-3 max-w-2xl text-sm leading-6 text-[#796b62] md:text-base">
            Gestiona el equipo, sus servicios, horarios,
            disponibilidad para reservas y la información de cada profesional.
          </p>

        </div>

        <div className="flex flex-wrap gap-3">

          <button
            type="button"
            onClick={() =>
              void loadProfessionals()
            }
            disabled={
              loading
            }
            className="inline-flex items-center justify-center rounded-full border border-[#d8cec6] bg-white px-5 py-3 text-sm font-semibold text-[#62564f] transition hover:bg-[#f7f3ef] disabled:cursor-not-allowed disabled:opacity-50"
          >
            {loading
              ? "Actualizando..."
              : "Actualizar"}
          </button>

          <button
            type="button"
            onClick={
              openCreateModal
            }
            className="inline-flex items-center justify-center rounded-full bg-[#342d28] px-5 py-3 text-sm font-semibold text-white transition hover:bg-[#211c18]"
          >
            Nuevo profesional
          </button>

        </div>

      </section>

      {/* ===================================================
          SUCCESS
      =================================================== */}

      {successMessage && (

        <section className="rounded-[20px] border border-[#dce8dd] bg-[#f1f7f1] px-5 py-4 text-sm font-medium text-[#55705a]">
          {successMessage}
        </section>

      )}

      {/* ===================================================
          ERROR
      =================================================== */}

      {error && (

        <section className="flex flex-col gap-4 rounded-[22px] border border-[#ead3d3] bg-[#fbf1f1] p-5 md:flex-row md:items-center md:justify-between">

          <div>

            <div className="text-sm font-semibold text-[#8f5555]">
              No se ha podido completar la operación
            </div>

            <p className="mt-1 text-sm leading-6 text-[#946f6f]">
              {error}
            </p>

          </div>

          <button
            type="button"
            onClick={() =>
              void loadProfessionals()
            }
            className="w-fit rounded-full border border-[#ddc5c5] bg-white px-4 py-2 text-xs font-semibold text-[#7e5858]"
          >
            Reintentar
          </button>

        </section>

      )}

      {/* ===================================================
          METRICS
      =================================================== */}

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">

        <MetricCard
          label="Total profesionales"
          value={professionals.length}
          description="Equipo completo"
        />

        <MetricCard
          label="Activos"
          value={activeProfessionals.length}
          description="En plantilla"
        />

        <MetricCard
          label="Reservables"
          value={bookableProfessionals.length}
          description="Admiten nuevas citas"
        />

        <MetricCard
          label="No reservables"
          value={notBookableProfessionals.length}
          description="Pausados o inactivos"
        />

      </section>

      {/* ===================================================
          FILTERS
      =================================================== */}

      <section className="rounded-[24px] border border-[#e4dcd4] bg-white p-5 shadow-[0_10px_35px_rgba(62,49,41,0.035)]">

        <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">

          <div className="w-full xl:max-w-md">

            <input
              type="search"
              value={
                search
              }
              onChange={
                event =>
                  setSearch(
                    event.target.value,
                  )
              }
              placeholder="Buscar por nombre, email, teléfono..."
              className="w-full rounded-2xl border border-[#ddd3cb] bg-[#fbf9f7] px-4 py-3 text-sm text-[#3c342f] outline-none transition placeholder:text-[#a89a90] focus:border-[#b9a89c] focus:bg-white focus:ring-2 focus:ring-[#ded3cb]/50"
            />

          </div>

          <div className="flex flex-wrap gap-2">

            {(
              [
                {
                  value: "ALL",
                  label: "Todos",
                },
                {
                  value: "ACTIVE",
                  label: "Activos",
                },
                {
                  value: "INACTIVE",
                  label: "Inactivos",
                },
                {
                  value: "BOOKABLE",
                  label: "Reservables",
                },
                {
                  value: "NOT_BOOKABLE",
                  label: "No reservables",
                },
              ] as const
            ).map(
              option => {

                const active =
                  filter === option.value;

                return (
                  <button
                    key={
                      option.value
                    }
                    type="button"
                    onClick={() =>
                      setFilter(
                        option.value,
                      )
                    }
                    className={[
                      "rounded-full px-4 py-2 text-xs font-semibold transition",

                      active
                        ? "bg-[#342d28] text-white"
                        : "border border-[#ded5ce] bg-white text-[#74665d] hover:bg-[#f7f3ef]",
                    ].join(" ")}
                  >
                    {option.label}
                  </button>
                );
              },
            )}

          </div>

        </div>

        <div className="mt-4 text-xs text-[#93847a]">

          {filteredProfessionals.length}

          {" "}

          {filteredProfessionals.length === 1
            ? "profesional encontrado"
            : "profesionales encontrados"}

        </div>

      </section>

      {/* ===================================================
          LIST
      =================================================== */}

      <section className="overflow-hidden rounded-[26px] border border-[#e3dbd3] bg-white shadow-[0_10px_35px_rgba(62,49,41,0.04)]">

        <div className="border-b border-[#eee7e1] px-6 py-5">

          <h2 className="text-lg font-semibold text-[#312b27]">
            Profesionales
          </h2>

          <p className="mt-1 text-sm text-[#8a7b71]">
            Equipo asociado al negocio y configuración de reservas.
          </p>

        </div>

        {/* LOADING */}

        {loading && (

          <div className="space-y-3 p-6">

            {Array.from(
              {
                length: 4,
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
                  className="h-24 animate-pulse rounded-2xl bg-[#f5f1ed]"
                />

              ),
            )}

          </div>

        )}

        {/* EMPTY */}

        {!loading
        && filteredProfessionals.length === 0
        && (

          <div className="px-6 py-16 text-center">

            <div className="text-base font-semibold text-[#3b342f]">
              No hay profesionales que mostrar
            </div>

            <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-[#8a7b71]">
              Ajusta los filtros o añade el primer profesional del negocio.
            </p>

            <button
              type="button"
              onClick={
                openCreateModal
              }
              className="mt-5 rounded-full bg-[#342d28] px-5 py-2.5 text-sm font-semibold text-white"
            >
              Nuevo profesional
            </button>

          </div>

        )}

        {!loading
        && filteredProfessionals.length > 0
        && (
          <>

            {/* ===============================================
                DESKTOP
            =============================================== */}

            <div className="hidden overflow-x-auto lg:block">

              <table className="w-full min-w-[1220px]">

                <thead>

                  <tr className="border-b border-[#eee7e1] bg-[#fbf9f7] text-left text-[11px] font-semibold uppercase tracking-[0.14em] text-[#9a8b80]">

                    <th className="px-6 py-4">
                      Profesional
                    </th>

                    <th className="px-6 py-4">
                      Contacto
                    </th>

                    <th className="px-6 py-4">
                      Estado
                    </th>

                    <th className="px-6 py-4">
                      Reservas
                    </th>

                    <th className="px-6 py-4">
                      Alta
                    </th>

                    <th className="px-6 py-4 text-right">
                      Acciones
                    </th>

                  </tr>

                </thead>

                <tbody className="divide-y divide-[#f0eae4]">

                  {filteredProfessionals.map(
                    professional => (

                      <tr
                        key={
                          professional.id
                        }
                        className="transition hover:bg-[#fcfaf8]"
                      >

                        {/* PROFESSIONAL */}

                        <td className="px-6 py-5">

                          <div className="flex items-start gap-3">

                            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-[#eee7df] text-xs font-semibold text-[#68594f]">

                              {getProfessionalInitials(
                                professional,
                              )}

                            </div>

                            <div className="min-w-0">

                              <Link
                                href={`/business/professionals/${professional.id}`}
                                className="text-sm font-semibold text-[#342e2a] transition hover:text-[#8a6e5e]"
                              >
                                {getProfessionalDisplayName(
                                  professional,
                                )}
                              </Link>

                              <div className="mt-1 text-xs text-[#94857a]">

                                {professional.firstName}

                                {professional.lastName
                                  ? ` ${professional.lastName}`
                                  : ""}

                              </div>

                              {professional.bio && (

                                <div className="mt-2 max-w-xs line-clamp-2 text-xs leading-5 text-[#8b7d73]">
                                  {professional.bio}
                                </div>

                              )}

                            </div>

                          </div>

                        </td>

                        {/* CONTACT */}

                        <td className="px-6 py-5">

                          <div className="text-sm text-[#665a52]">
                            {professional.email
                            || "Sin email"}
                          </div>

                          <div className="mt-1 text-xs text-[#97887e]">
                            {professional.phone
                            || "Sin teléfono"}
                          </div>

                        </td>

                        {/* STATUS */}

                        <td className="px-6 py-5">

                          <span
                            className={[
                              "inline-flex rounded-full px-3 py-1 text-xs font-semibold",

                              professional.active
                                ? "bg-[#edf4ee] text-[#55735c]"
                                : "bg-[#f1eeee] text-[#756b6b]",
                            ].join(" ")}
                          >
                            {professional.active
                              ? "Activo"
                              : "Inactivo"}
                          </span>

                        </td>

                        {/* BOOKINGS */}

                        <td className="px-6 py-5">

                          <span
                            className={[
                              "inline-flex rounded-full px-3 py-1 text-xs font-semibold",

                              professional.active
                              && professional.bookingEnabled
                                ? "bg-[#edf4ee] text-[#55735c]"
                                : "bg-[#f7f0e5] text-[#947348]",
                            ].join(" ")}
                          >
                            {professional.active
                            && professional.bookingEnabled
                              ? "Habilitadas"
                              : "Deshabilitadas"}
                          </span>

                        </td>

                        {/* CREATED */}

                        <td className="px-6 py-5 text-sm text-[#776a61]">

                          {formatDate(
                            professional.createdAt,
                          )}

                        </td>

                        {/* ACTIONS */}

                        <td className="px-6 py-5">

                          <div className="flex flex-wrap justify-end gap-2">

                            <Link
                              href={`/business/professionals/${professional.id}`}
                              className="inline-flex min-w-[86px] items-center justify-center whitespace-nowrap rounded-full border border-[#d8cec6] bg-[#f8f4f0] px-3.5 py-2 text-xs font-semibold leading-none text-[#5f534b] transition hover:border-[#c5b7ac] hover:bg-[#eee7e1]"
                            >
                              Ver ficha
                            </Link>

                            <button
                              type="button"
                              onClick={() =>
                                openEditModal(
                                  professional,
                                )
                              }
                              className="rounded-full border border-[#ddd3cb] bg-white px-3.5 py-2 text-xs font-semibold text-[#65584f] transition hover:bg-[#f7f3ef]"
                            >
                              Editar
                            </button>

                            <button
                              type="button"
                              onClick={() =>
                                void toggleBookingEnabled(
                                  professional,
                                )
                              }
                              disabled={
                                updatingProfessionalId
                                === professional.id
                                || !professional.active
                              }
                              className={[
                                "rounded-full px-3.5 py-2 text-xs font-semibold transition disabled:cursor-not-allowed disabled:opacity-45",

                                professional.bookingEnabled
                                  ? "border border-[#e4d6c4] bg-[#fffaf4] text-[#8b6d47] hover:bg-[#f8efe3]"
                                  : "border border-[#cfddd1] bg-[#f4f8f4] text-[#55705a] hover:bg-[#eaf2eb]",
                              ].join(" ")}
                            >
                              {professional.bookingEnabled
                                ? "Pausar reservas"
                                : "Habilitar reservas"}
                            </button>

                            <button
                              type="button"
                              onClick={() =>
                                void toggleProfessionalActive(
                                  professional,
                                )
                              }
                              disabled={
                                updatingProfessionalId
                                === professional.id
                              }
                              className={[
                                "rounded-full px-3.5 py-2 text-xs font-semibold transition disabled:cursor-not-allowed disabled:opacity-50",

                                professional.active
                                  ? "border border-[#e4cece] bg-[#fffafa] text-[#925d5d] hover:bg-[#f9eeee]"
                                  : "border border-[#cfded1] bg-[#f4f8f4] text-[#55705a] hover:bg-[#eaf2eb]",
                              ].join(" ")}
                            >

                              {updatingProfessionalId
                              === professional.id
                                ? "Guardando..."
                                : professional.active
                                  ? "Desactivar"
                                  : "Activar"}

                            </button>

                          </div>

                        </td>

                      </tr>

                    ),
                  )}

                </tbody>

              </table>

            </div>

            {/* ===============================================
                MOBILE
            =============================================== */}

            <div className="divide-y divide-[#f0eae4] lg:hidden">

              {filteredProfessionals.map(
                professional => (

                  <article
                    key={
                      professional.id
                    }
                    className="p-5"
                  >

                    <div className="flex items-start gap-3">

                      <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-[#eee7df] text-xs font-semibold text-[#68594f]">

                        {getProfessionalInitials(
                          professional,
                        )}

                      </div>

                      <div className="min-w-0 flex-1">

                        <div className="flex items-start justify-between gap-3">

                          <div>

                            <Link
                              href={`/business/professionals/${professional.id}`}
                              className="text-base font-semibold text-[#342e2a]"
                            >
                              {getProfessionalDisplayName(
                                professional,
                              )}
                            </Link>

                            <div className="mt-1 text-xs text-[#97887e]">
                              {professional.email
                              || "Sin email"}
                            </div>

                          </div>

                          <span
                            className={[
                              "shrink-0 rounded-full px-2.5 py-1 text-[11px] font-semibold",

                              professional.active
                                ? "bg-[#edf4ee] text-[#55735c]"
                                : "bg-[#f1eeee] text-[#756b6b]",
                            ].join(" ")}
                          >
                            {professional.active
                              ? "Activo"
                              : "Inactivo"}
                          </span>

                        </div>

                        <div className="mt-2 text-xs text-[#97887e]">
                          {professional.phone
                          || "Sin teléfono"}
                        </div>

                        {professional.bio && (

                          <p className="mt-3 text-sm leading-6 text-[#7c6e65]">
                            {professional.bio}
                          </p>

                        )}

                        <div className="mt-3">

                          <span
                            className={[
                              "inline-flex rounded-full px-3 py-1 text-xs font-semibold",

                              professional.active
                              && professional.bookingEnabled
                                ? "bg-[#edf4ee] text-[#55735c]"
                                : "bg-[#f7f0e5] text-[#947348]",
                            ].join(" ")}
                          >
                            {professional.active
                            && professional.bookingEnabled
                              ? "Reservas habilitadas"
                              : "Reservas deshabilitadas"}
                          </span>

                        </div>

                        <div className="mt-5 flex flex-wrap gap-2">

                          <Link
                            href={`/business/professionals/${professional.id}`}
                            className="inline-flex min-w-[86px] items-center justify-center whitespace-nowrap rounded-full border border-[#d8cec6] bg-[#f8f4f0] px-3.5 py-2 text-xs font-semibold leading-none text-[#5f534b] transition hover:border-[#c5b7ac] hover:bg-[#eee7e1]"
                          >
                            Ver ficha
                          </Link>

                          <button
                            type="button"
                            onClick={() =>
                              openEditModal(
                                professional,
                              )
                            }
                            className="rounded-full border border-[#ddd3cb] bg-white px-3.5 py-2 text-xs font-semibold text-[#65584f]"
                          >
                            Editar
                          </button>

                          <button
                            type="button"
                            onClick={() =>
                              void toggleBookingEnabled(
                                professional,
                              )
                            }
                            disabled={
                              updatingProfessionalId
                              === professional.id
                              || !professional.active
                            }
                            className="rounded-full border border-[#dfd3c3] bg-[#fffaf4] px-3.5 py-2 text-xs font-semibold text-[#866c4c] disabled:opacity-45"
                          >
                            {professional.bookingEnabled
                              ? "Pausar reservas"
                              : "Habilitar reservas"}
                          </button>

                          <button
                            type="button"
                            onClick={() =>
                              void toggleProfessionalActive(
                                professional,
                              )
                            }
                            disabled={
                              updatingProfessionalId
                              === professional.id
                            }
                            className={[
                              "rounded-full px-3.5 py-2 text-xs font-semibold disabled:opacity-50",

                              professional.active
                                ? "border border-[#e4cece] bg-[#fffafa] text-[#925d5d]"
                                : "border border-[#cfded1] bg-[#f4f8f4] text-[#55705a]",
                            ].join(" ")}
                          >

                            {updatingProfessionalId
                            === professional.id
                              ? "Guardando..."
                              : professional.active
                                ? "Desactivar"
                                : "Activar"}

                          </button>

                        </div>

                      </div>

                    </div>

                  </article>

                ),
              )}

            </div>

          </>
        )}

      </section>

      {/* ===================================================
          CREATE MODAL
      =================================================== */}

      {createOpen && (

        <ProfessionalModal
          title="Nuevo profesional"
          subtitle="Añade una persona al equipo del negocio."
          form={
            form
          }
          setForm={
            setForm
          }
          error={
            formError
          }
          saving={
            saving
          }
          submitLabel="Crear profesional"
          onClose={
            closeCreateModal
          }
          onSubmit={() =>
            void handleCreateProfessional()
          }
        />

      )}

      {/* ===================================================
          EDIT MODAL
      =================================================== */}

      {editOpen
      && selectedProfessional
      && (

        <ProfessionalModal
          title="Editar profesional"
          subtitle={getProfessionalDisplayName(
            selectedProfessional,
          )}
          form={
            form
          }
          setForm={
            setForm
          }
          error={
            formError
          }
          saving={
            saving
          }
          submitLabel="Guardar cambios"
          onClose={
            closeEditModal
          }
          onSubmit={() =>
            void handleUpdateProfessional()
          }
        />

      )}

    </div>
  );
}

/* =========================================================
   METRIC CARD
========================================================= */

type MetricCardProps = {
  label: string;
  value: number;
  description: string;
};

function MetricCard({
  label,
  value,
  description,
}: MetricCardProps) {

  return (
    <article className="rounded-[24px] border border-[#e5ddd5] bg-white p-5 shadow-[0_10px_35px_rgba(62,49,41,0.04)]">

      <div className="text-sm font-medium text-[#786b62]">
        {label}
      </div>

      <div className="mt-3 text-3xl font-semibold tracking-[-0.04em] text-[#302a26]">
        {value}
      </div>

      <div className="mt-2 text-xs text-[#9a8a7f]">
        {description}
      </div>

    </article>
  );
}

/* =========================================================
   MODAL
========================================================= */

type ProfessionalModalProps = {
  title: string;
  subtitle: string;

  form: ProfessionalFormState;

  setForm: Dispatch<
    SetStateAction<ProfessionalFormState>
  >;

  error: string | null;
  saving: boolean;

  submitLabel: string;

  onClose: () => void;
  onSubmit: () => void;
};

function ProfessionalModal({
  title,
  subtitle,
  form,
  setForm,
  error,
  saving,
  submitLabel,
  onClose,
  onSubmit,
}: ProfessionalModalProps) {

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-[#241f1b]/45 p-4 backdrop-blur-sm">

      {/* BACKDROP */}

      <button
        type="button"
        aria-label="Cerrar"
        onClick={
          onClose
        }
        className="absolute inset-0 cursor-default"
      />

      {/* PANEL */}

      <div className="relative z-10 flex max-h-[calc(100vh-32px)] w-full max-w-3xl flex-col overflow-hidden rounded-[28px] border border-[#e0d6ce] bg-[#fdfbf9] shadow-[0_30px_90px_rgba(36,31,27,0.22)]">

        {/* HEADER */}

        <div className="flex items-start justify-between gap-4 border-b border-[#ebe3dd] px-6 py-5 md:px-7">

          <div>

            <div className="text-xs font-semibold uppercase tracking-[0.18em] text-[#a18c7f]">
              Profesionales
            </div>

            <h2 className="mt-2 text-2xl font-semibold tracking-[-0.03em] text-[#312b27]">
              {title}
            </h2>

            <p className="mt-1 text-sm text-[#8a7b71]">
              {subtitle}
            </p>

          </div>

          <button
            type="button"
            onClick={
              onClose
            }
            disabled={
              saving
            }
            className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-[#ddd3cb] bg-white text-lg text-[#76685f] transition hover:bg-[#f5f1ed] disabled:opacity-50"
          >
            ×
          </button>

        </div>

        {/* BODY */}

        <div className="overflow-y-auto px-6 py-6 md:px-7">

          {error && (

            <div className="mb-5 rounded-2xl border border-[#ead3d3] bg-[#fbf1f1] px-4 py-3 text-sm leading-6 text-[#8f5f5f]">
              {error}
            </div>

          )}

          {/* NAME */}

          <div className="grid gap-5 md:grid-cols-2">

            <label className="block">

              <span className="text-xs font-semibold uppercase tracking-[0.12em] text-[#8e7e73]">
                Nombre *
              </span>

              <input
                type="text"
                value={
                  form.firstName
                }
                maxLength={
                  100
                }
                onChange={
                  event =>
                    setForm(
                      current => ({
                        ...current,

                        firstName:
                          event.target.value,
                      }),
                    )
                }
                placeholder="Laura"
                className="mt-2 w-full rounded-2xl border border-[#ddd3cb] bg-white px-4 py-3 text-sm text-[#352f2b] outline-none transition focus:border-[#b9a89c] focus:ring-2 focus:ring-[#ded3cb]/50"
              />

            </label>

            <label className="block">

              <span className="text-xs font-semibold uppercase tracking-[0.12em] text-[#8e7e73]">
                Apellidos
              </span>

              <input
                type="text"
                value={
                  form.lastName
                }
                maxLength={
                  150
                }
                onChange={
                  event =>
                    setForm(
                      current => ({
                        ...current,

                        lastName:
                          event.target.value,
                      }),
                    )
                }
                placeholder="Martín"
                className="mt-2 w-full rounded-2xl border border-[#ddd3cb] bg-white px-4 py-3 text-sm text-[#352f2b] outline-none transition focus:border-[#b9a89c] focus:ring-2 focus:ring-[#ded3cb]/50"
              />

            </label>

          </div>

          {/* DISPLAY NAME */}

          <label className="mt-5 block">

            <span className="text-xs font-semibold uppercase tracking-[0.12em] text-[#8e7e73]">
              Nombre visible
            </span>

            <input
              type="text"
              value={
                form.displayName
              }
              maxLength={
                180
              }
              onChange={
                event =>
                  setForm(
                    current => ({
                      ...current,

                      displayName:
                        event.target.value,
                    }),
                  )
              }
              placeholder="Ej. Laura · Especialista facial"
              className="mt-2 w-full rounded-2xl border border-[#ddd3cb] bg-white px-4 py-3 text-sm text-[#352f2b] outline-none transition focus:border-[#b9a89c] focus:ring-2 focus:ring-[#ded3cb]/50"
            />

            <p className="mt-2 text-xs leading-5 text-[#998a80]">
              Si lo dejas vacío, se utilizará automáticamente nombre y apellidos.
            </p>

          </label>

          {/* CONTACT */}

          <div className="mt-5 grid gap-5 md:grid-cols-2">

            <label className="block">

              <span className="text-xs font-semibold uppercase tracking-[0.12em] text-[#8e7e73]">
                Email
              </span>

              <input
                type="email"
                value={
                  form.email
                }
                maxLength={
                  180
                }
                onChange={
                  event =>
                    setForm(
                      current => ({
                        ...current,

                        email:
                          event.target.value,
                      }),
                    )
                }
                placeholder="profesional@aurastudio.es"
                className="mt-2 w-full rounded-2xl border border-[#ddd3cb] bg-white px-4 py-3 text-sm text-[#352f2b] outline-none transition focus:border-[#b9a89c] focus:ring-2 focus:ring-[#ded3cb]/50"
              />

            </label>

            <label className="block">

              <span className="text-xs font-semibold uppercase tracking-[0.12em] text-[#8e7e73]">
                Teléfono
              </span>

              <input
                type="tel"
                value={
                  form.phone
                }
                maxLength={
                  40
                }
                onChange={
                  event =>
                    setForm(
                      current => ({
                        ...current,

                        phone:
                          event.target.value,
                      }),
                    )
                }
                placeholder="600 000 000"
                className="mt-2 w-full rounded-2xl border border-[#ddd3cb] bg-white px-4 py-3 text-sm text-[#352f2b] outline-none transition focus:border-[#b9a89c] focus:ring-2 focus:ring-[#ded3cb]/50"
              />

            </label>

          </div>

          {/* BIO */}

          <label className="mt-5 block">

            <span className="text-xs font-semibold uppercase tracking-[0.12em] text-[#8e7e73]">
              Biografía
            </span>

            <textarea
              value={
                form.bio
              }
              maxLength={
                5000
              }
              rows={
                5
              }
              onChange={
                event =>
                  setForm(
                    current => ({
                      ...current,

                      bio:
                        event.target.value,
                    }),
                  )
              }
              placeholder="Especialidad, experiencia o información relevante..."
              className="mt-2 w-full resize-none rounded-2xl border border-[#ddd3cb] bg-white px-4 py-3 text-sm leading-6 text-[#352f2b] outline-none transition focus:border-[#b9a89c] focus:ring-2 focus:ring-[#ded3cb]/50"
            />

          </label>

          {/* STATUS */}

          <div className="mt-6 space-y-3">

            <div className="flex items-center justify-between gap-4 rounded-2xl border border-[#e1d8d1] bg-[#f8f5f1] p-4">

              <div>

                <div className="text-sm font-semibold text-[#3b342f]">
                  Profesional activo
                </div>

                <div className="mt-1 text-xs leading-5 text-[#8a7b71]">
                  Mantiene al profesional dentro del equipo operativo.
                </div>

              </div>

              <ToggleSwitch
                active={
                  form.active
                }
                onClick={() =>
                  setForm(
                    current => ({
                      ...current,

                      active:
                        !current.active,

                      bookingEnabled:
                        current.active
                          ? false
                          : current.bookingEnabled,
                    }),
                  )
                }
              />

            </div>

            <div
              className={[
                "flex items-center justify-between gap-4 rounded-2xl border p-4 transition",

                form.active
                  ? "border-[#e1d8d1] bg-[#f8f5f1]"
                  : "border-[#e5dfdb] bg-[#f5f2ef] opacity-60",
              ].join(" ")}
            >

              <div>

                <div className="text-sm font-semibold text-[#3b342f]">
                  Permitir reservas
                </div>

                <div className="mt-1 text-xs leading-5 text-[#8a7b71]">
                  Podrá seleccionarse para recibir nuevas citas.
                </div>

              </div>

              <ToggleSwitch
                active={
                  form.active
                  && form.bookingEnabled
                }
                disabled={
                  !form.active
                }
                onClick={() => {

                  if (
                    !form.active
                  ) {
                    return;
                  }

                  setForm(
                    current => ({
                      ...current,

                      bookingEnabled:
                        !current.bookingEnabled,
                    }),
                  );
                }}
              />

            </div>

          </div>

        </div>

        {/* FOOTER */}

        <div className="flex flex-col-reverse gap-3 border-t border-[#ebe3dd] bg-[#faf7f4] px-6 py-5 sm:flex-row sm:justify-end md:px-7">

          <button
            type="button"
            onClick={
              onClose
            }
            disabled={
              saving
            }
            className="rounded-full border border-[#d9cfc7] bg-white px-5 py-2.5 text-sm font-semibold text-[#6e6057] transition hover:bg-[#f5f1ed] disabled:opacity-50"
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
              || form.firstName.trim() === ""
            }
            className="rounded-full bg-[#342d28] px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-[#211c18] disabled:cursor-not-allowed disabled:opacity-50"
          >
            {saving
              ? "Guardando..."
              : submitLabel}
          </button>

        </div>

      </div>

    </div>
  );
}

/* =========================================================
   TOGGLE
========================================================= */

type ToggleSwitchProps = {
  active: boolean;
  disabled?: boolean;
  onClick: () => void;
};

function ToggleSwitch({
  active,
  disabled = false,
  onClick,
}: ToggleSwitchProps) {

  return (
    <button
      type="button"
      aria-pressed={
        active
      }
      disabled={
        disabled
      }
      onClick={
        onClick
      }
      className={[
        "relative h-7 w-12 shrink-0 rounded-full transition disabled:cursor-not-allowed",

        active
          ? "bg-[#657b68]"
          : "bg-[#c9c0ba]",
      ].join(" ")}
    >

      <span
        className={[
          "absolute top-1 h-5 w-5 rounded-full bg-white shadow-sm transition",

          active
            ? "left-6"
            : "left-1",
        ].join(" ")}
      />

    </button>
  );
}