"use client";

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

type ServiceResponse = {
  id: number;
  businessId: number;

  name: string;
  slug: string;

  description: string | null;

  durationMinutes: number;
  price: number;

  active: boolean;
  bookingEnabled: boolean;

  createdAt: string;
  updatedAt: string;
};

type ServiceRequest = {
  name: string;
  slug: string;
  description: string | null;
  durationMinutes: number;
  price: number;
  active: boolean;
  bookingEnabled: boolean;
};

type ServiceFormState = {
  name: string;
  slug: string;
  description: string;
  durationMinutes: string;
  price: string;
  active: boolean;
  bookingEnabled: boolean;
};

type ServiceFilter =
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

const initialFormState: ServiceFormState = {
  name: "",
  slug: "",
  description: "",
  durationMinutes: "45",
  price: "",
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
   TEXT HELPERS
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

function createSlug(
  value: string,
): string {
  return value
    .normalize(
      "NFD",
    )
    .replace(
      /[\u0300-\u036f]/g,
      "",
    )
    .toLowerCase()
    .trim()
    .replace(
      /[^a-z0-9]+/g,
      "-",
    )
    .replace(
      /^-+|-+$/g,
      "",
    );
}

/* =========================================================
   FORMAT
========================================================= */

function formatPrice(
  price: number,
): string {
  try {
    return new Intl.NumberFormat(
      "es-ES",
      {
        style:
          "currency",

        currency:
          "EUR",

        minimumFractionDigits:
          2,

        maximumFractionDigits:
          2,
      },
    ).format(
      price,
    );
  } catch {
    return `${price.toFixed(
      2,
    )} €`;
  }
}

function formatDate(
  value: string,
): string {
  try {
    return new Intl.DateTimeFormat(
      "es-ES",
      {
        day:
          "2-digit",

        month:
          "short",

        year:
          "numeric",
      },
    ).format(
      new Date(
        value,
      ),
    );
  } catch {
    return "—";
  }
}

function formatDuration(
  minutes: number,
): string {
  if (
    minutes < 60
  ) {
    return `${minutes} min`;
  }

  const hours =
    Math.floor(
      minutes / 60,
    );

  const remainder =
    minutes % 60;

  if (
    remainder === 0
  ) {
    return hours === 1
      ? "1 h"
      : `${hours} h`;
  }

  return `${hours} h ${remainder} min`;
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
      return "No tienes permisos para gestionar servicios. Esta operación requiere un usuario OWNER o ADMIN.";
    }

    if (
      error.status === 409
    ) {
      return (
        error.message
        || "Existe un conflicto con los datos del servicio."
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
   COMPONENT
========================================================= */

export default function ServicesPage() {

  /* =======================================================
     STATE
  ======================================================= */

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
    successMessage,
    setSuccessMessage,
  ] =
    useState<
      string | null
    >(
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
    useState<ServiceFilter>(
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
    selectedService,
    setSelectedService,
  ] =
    useState<
      ServiceResponse
      | null
    >(
      null,
    );

  const [
    form,
    setForm,
  ] =
    useState<ServiceFormState>(
      initialFormState,
    );

  const [
    slugManuallyEdited,
    setSlugManuallyEdited,
  ] =
    useState(
      false,
    );

  const [
    formError,
    setFormError,
  ] =
    useState<
      string | null
    >(
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
    updatingServiceId,
    setUpdatingServiceId,
  ] =
    useState<
      number
      | null
    >(
      null,
    );

  /* =======================================================
     LOAD
  ======================================================= */

  const loadServices =
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
              ServiceResponse[]
            >(
              "/api/v1/services",
              {
                accessToken,
              },
            );

          setServices(
            Array.isArray(
              response,
            )
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
      void loadServices();
    },
    [
      loadServices,
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

          setSelectedService(
            null,
          );

          setForm(
            initialFormState,
          );

          setFormError(
            null,
          );

          setSlugManuallyEdited(
            false,
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

  const activeServices =
    useMemo(
      () =>
        services.filter(
          (
            service,
          ) =>
            service.active,
        ),
      [
        services,
      ],
    );

  const inactiveServices =
    useMemo(
      () =>
        services.filter(
          (
            service,
          ) =>
            !service.active,
        ),
      [
        services,
      ],
    );

  const bookableServices =
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

  const notBookableServices =
    useMemo(
      () =>
        services.filter(
          (
            service,
          ) =>
            !service.bookingEnabled,
        ),
      [
        services,
      ],
    );

  /* =======================================================
     FILTERED
  ======================================================= */

  const filteredServices =
    useMemo(
      () => {
        const normalizedSearch =
          search
            .trim()
            .toLowerCase();

        return services.filter(
          (
            service,
          ) => {

            if (
              filter === "ACTIVE"
              && !service.active
            ) {
              return false;
            }

            if (
              filter === "INACTIVE"
              && service.active
            ) {
              return false;
            }

            if (
              filter === "BOOKABLE"
              && (
                !service.active
                || !service.bookingEnabled
              )
            ) {
              return false;
            }

            if (
              filter === "NOT_BOOKABLE"
              && service.bookingEnabled
            ) {
              return false;
            }

            if (
              normalizedSearch === ""
            ) {
              return true;
            }

            const haystack = [
              service.name,
              service.slug,
              service.description,
              String(
                service.durationMinutes,
              ),
              String(
                service.price,
              ),
            ]
              .filter(
                Boolean,
              )
              .join(
                " ",
              )
              .toLowerCase();

            return haystack.includes(
              normalizedSearch,
            );
          },
        );
      },
      [
        filter,
        search,
        services,
      ],
    );

  /* =======================================================
     MODALS
  ======================================================= */

  function openCreateModal() {
    setSelectedService(
      null,
    );

    setForm(
      initialFormState,
    );

    setSlugManuallyEdited(
      false,
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

    setSlugManuallyEdited(
      false,
    );

    setFormError(
      null,
    );
  }

  function openEditModal(
    service: ServiceResponse,
  ) {
    setSelectedService(
      service,
    );

    setForm(
      {
        name:
          service.name,

        slug:
          service.slug,

        description:
          service.description
          ?? "",

        durationMinutes:
          String(
            service.durationMinutes,
          ),

        price:
          String(
            service.price,
          ),

        active:
          service.active,

        bookingEnabled:
          service.bookingEnabled,
      },
    );

    /*
     * En edición no queremos modificar automáticamente
     * un slug ya persistido al cambiar el nombre.
     */
    setSlugManuallyEdited(
      true,
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

    setSelectedService(
      null,
    );

    setForm(
      initialFormState,
    );

    setSlugManuallyEdited(
      false,
    );

    setFormError(
      null,
    );
  }

  /* =======================================================
     PAYLOAD
  ======================================================= */

  function buildPayload():
    | ServiceRequest
    | null {
    const name =
      form.name.trim();

    const slug =
      form.slug.trim();

    if (
      name === ""
    ) {
      setFormError(
        "El nombre del servicio es obligatorio.",
      );

      return null;
    }

    if (
      slug === ""
    ) {
      setFormError(
        "El slug del servicio es obligatorio.",
      );

      return null;
    }

    const durationMinutes =
      Number(
        form.durationMinutes,
      );

    if (
      !Number.isInteger(
        durationMinutes,
      )
      || durationMinutes <= 0
    ) {
      setFormError(
        "La duración debe ser un número entero mayor que 0.",
      );

      return null;
    }

    const price =
      Number(
        form.price
          .replace(
            ",",
            ".",
          ),
      );

    if (
      !Number.isFinite(
        price,
      )
      || price < 0
    ) {
      setFormError(
        "El precio debe ser un número igual o mayor que 0.",
      );

      return null;
    }

    return {
      name,

      slug:
        createSlug(
          slug,
        ),

      description:
        normalizeNullableText(
          form.description,
        ),

      durationMinutes,

      price:
        Number(
          price.toFixed(
            2,
          ),
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

  async function handleCreateService() {
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
        ServiceResponse
      >(
        "/api/v1/services",
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

      setSlugManuallyEdited(
        false,
      );

      setSuccessMessage(
        "Servicio creado correctamente.",
      );

      await loadServices();
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

  async function handleUpdateService() {
    if (
      saving
      || !selectedService
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
        ServiceResponse
      >(
        `/api/v1/services/${selectedService.id}`,
        payload,
        {
          accessToken,
        },
      );

      setEditOpen(
        false,
      );

      setSelectedService(
        null,
      );

      setForm(
        initialFormState,
      );

      setSlugManuallyEdited(
        false,
      );

      setSuccessMessage(
        "Servicio actualizado correctamente.",
      );

      await loadServices();
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

  async function toggleServiceActive(
    service: ServiceResponse,
  ) {
    if (
      updatingServiceId != null
    ) {
      return;
    }

    setUpdatingServiceId(
      service.id,
    );

    setError(
      null,
    );

    const nextActive =
      !service.active;

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
        ServiceResponse
      >(
        `/api/v1/services/${service.id}/active?active=${String(
          nextActive,
        )}`,
        undefined,
        {
          accessToken,
        },
      );

      setSuccessMessage(
        nextActive
          ? "Servicio activado correctamente."
          : "Servicio desactivado correctamente.",
      );

      await loadServices();
    } catch (
      requestError
    ) {
      setError(
        resolveApiError(
          requestError,
        ),
      );
    } finally {
      setUpdatingServiceId(
        null,
      );
    }
  }

  /* =======================================================
     BOOKING
  ======================================================= */

  async function toggleBookingEnabled(
    service: ServiceResponse,
  ) {
    if (
      updatingServiceId != null
      || !service.active
    ) {
      return;
    }

    setUpdatingServiceId(
      service.id,
    );

    setError(
      null,
    );

    const nextEnabled =
      !service.bookingEnabled;

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
        ServiceResponse
      >(
        `/api/v1/services/${service.id}/booking-enabled?enabled=${String(
          nextEnabled,
        )}`,
        undefined,
        {
          accessToken,
        },
      );

      setSuccessMessage(
        nextEnabled
          ? "Reservas habilitadas para el servicio."
          : "Reservas deshabilitadas para el servicio.",
      );

      await loadServices();
    } catch (
      requestError
    ) {
      setError(
        resolveApiError(
          requestError,
        ),
      );
    } finally {
      setUpdatingServiceId(
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
            Servicios
          </div>

          <h1 className="mt-2 text-3xl font-semibold tracking-[-0.03em] text-[#2f2925] md:text-4xl">
            Catálogo de servicios
          </h1>

          <p className="mt-3 max-w-2xl text-sm leading-6 text-[#796b62] md:text-base">
            Gestiona los servicios, precios, duración y disponibilidad para
            reservas.
          </p>
        </div>

        <div className="flex flex-wrap gap-3">
          <button
            type="button"
            onClick={() =>
              void loadServices()
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
            Nuevo servicio
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
              void loadServices()
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

        <article className="bp-metric rounded-[24px] border border-[#e5ddd5] bg-white p-5 shadow-[0_10px_35px_rgba(62,49,41,0.04)]">
          <div className="text-sm font-medium text-[#786b62]">
            Total servicios
          </div>

          <div className="mt-3 text-3xl font-semibold tracking-[-0.04em] text-[#302a26]">
            {services.length}
          </div>

          <div className="mt-2 text-xs text-[#9a8a7f]">
            Catálogo completo
          </div>
        </article>

        <article className="bp-metric rounded-[24px] border border-[#e5ddd5] bg-white p-5 shadow-[0_10px_35px_rgba(62,49,41,0.04)]">
          <div className="text-sm font-medium text-[#786b62]">
            Activos
          </div>

          <div className="mt-3 text-3xl font-semibold tracking-[-0.04em] text-[#302a26]">
            {activeServices.length}
          </div>

          <div className="mt-2 text-xs text-[#9a8a7f]">
            En catálogo
          </div>
        </article>

        <article className="bp-metric rounded-[24px] border border-[#e5ddd5] bg-white p-5 shadow-[0_10px_35px_rgba(62,49,41,0.04)]">
          <div className="text-sm font-medium text-[#786b62]">
            Reservables
          </div>

          <div className="mt-3 text-3xl font-semibold tracking-[-0.04em] text-[#302a26]">
            {bookableServices.length}
          </div>

          <div className="mt-2 text-xs text-[#9a8a7f]">
            Admiten nuevas citas
          </div>
        </article>

        <article className="bp-metric rounded-[24px] border border-[#e5ddd5] bg-white p-5 shadow-[0_10px_35px_rgba(62,49,41,0.04)]">
          <div className="text-sm font-medium text-[#786b62]">
            No reservables
          </div>

          <div className="mt-3 text-3xl font-semibold tracking-[-0.04em] text-[#302a26]">
            {notBookableServices.length}
          </div>

          <div className="mt-2 text-xs text-[#9a8a7f]">
            Sin nuevas reservas
          </div>
        </article>
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
                (
                  event,
                ) =>
                  setSearch(
                    event.target.value,
                  )
              }
              placeholder="Buscar por nombre, slug o descripción..."
              className="w-full rounded-2xl border border-[#ddd3cb] bg-[#fbf9f7] px-4 py-3 text-sm text-[#3c342f] outline-none transition placeholder:text-[#a89a90] focus:border-[#b9a89c] focus:bg-white focus:ring-2 focus:ring-[#ded3cb]/50"
            />
          </div>

          <div className="flex flex-wrap gap-2">
            {(
              [
                {
                  value:
                    "ALL",

                  label:
                    "Todos",
                },

                {
                  value:
                    "ACTIVE",

                  label:
                    "Activos",
                },

                {
                  value:
                    "INACTIVE",

                  label:
                    "Inactivos",
                },

                {
                  value:
                    "BOOKABLE",

                  label:
                    "Reservables",
                },

                {
                  value:
                    "NOT_BOOKABLE",

                  label:
                    "No reservables",
                },
              ] as const
            ).map(
              (
                option,
              ) => {
                const active =
                  filter
                  === option.value;

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
                    ].join(
                      " ",
                    )}
                  >
                    {option.label}
                  </button>
                );
              },
            )}
          </div>
        </div>

        <div className="mt-4 text-xs text-[#93847a]">
          {filteredServices.length}
          {" "}
          {filteredServices.length === 1
            ? "servicio encontrado"
            : "servicios encontrados"}
        </div>
      </section>

      {/* ===================================================
          SERVICE LIST
      =================================================== */}

      <section className="overflow-hidden rounded-[26px] border border-[#e3dbd3] bg-white shadow-[0_10px_35px_rgba(62,49,41,0.04)]">

        <div className="border-b border-[#eee7e1] px-6 py-5">
          <h2 className="text-lg font-semibold text-[#312b27]">
            Servicios
          </h2>

          <p className="mt-1 text-sm text-[#8a7b71]">
            Configuración de los servicios ofrecidos por el negocio.
          </p>
        </div>

        {loading && (
          <div className="space-y-3 p-6">
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
                  className="h-24 animate-pulse rounded-2xl bg-[#f5f1ed]"
                />
              ),
            )}
          </div>
        )}

        {!loading
        && filteredServices.length === 0
        && (
          <div className="bp-empty px-6 py-16 text-center">
            <div className="text-base font-semibold text-[#3b342f]">
              No hay servicios que mostrar
            </div>

            <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-[#8a7b71]">
              Ajusta los filtros o crea el primer servicio del negocio.
            </p>

            <button
              type="button"
              onClick={
                openCreateModal
              }
              className="mt-5 rounded-full bg-[#342d28] px-5 py-2.5 text-sm font-semibold text-white"
            >
              Nuevo servicio
            </button>
          </div>
        )}

        {!loading
        && filteredServices.length > 0
        && (
          <>
            {/* ===============================================
                DESKTOP
            =============================================== */}

            <div className="hidden overflow-x-auto lg:block">
              <table className="w-full min-w-[1100px]">
                <thead>
                  <tr className="border-b border-[#eee7e1] bg-[#fbf9f7] text-left text-[11px] font-semibold uppercase tracking-[0.14em] text-[#9a8b80]">

                    <th className="px-6 py-4">
                      Servicio
                    </th>

                    <th className="px-6 py-4">
                      Duración
                    </th>

                    <th className="px-6 py-4">
                      Precio
                    </th>

                    <th className="px-6 py-4">
                      Estado
                    </th>

                    <th className="px-6 py-4">
                      Reservas
                    </th>

                    <th className="px-6 py-4 text-right">
                      Acciones
                    </th>
                  </tr>
                </thead>

                <tbody className="divide-y divide-[#f0eae4]">
                  {filteredServices.map(
                    (
                      service,
                    ) => (
                      <tr
                        key={
                          service.id
                        }
                        className="transition hover:bg-[#fcfaf8]"
                      >
                        <td className="px-6 py-5">
                          <div className="max-w-sm">
                            <div className="text-sm font-semibold text-[#342e2a]">
                              {service.name}
                            </div>

                            <div className="mt-1 text-xs text-[#94857a]">
                              /{service.slug}
                            </div>

                            {service.description && (
                              <div className="mt-2 line-clamp-2 text-xs leading-5 text-[#8b7d73]">
                                {service.description}
                              </div>
                            )}
                          </div>
                        </td>

                        <td className="px-6 py-5 text-sm font-medium text-[#675a52]">
                          {formatDuration(
                            service.durationMinutes,
                          )}
                        </td>

                        <td className="px-6 py-5 text-sm font-semibold text-[#443b35]">
                          {formatPrice(
                            service.price,
                          )}
                        </td>

                        <td className="px-6 py-5">
                          <span
                            className={[
                              "inline-flex rounded-full px-3 py-1 text-xs font-semibold",

                              service.active
                                ? "bg-[#edf4ee] text-[#55735c]"
                                : "bg-[#f1eeee] text-[#756b6b]",
                            ].join(
                              " ",
                            )}
                          >
                            {service.active
                              ? "Activo"
                              : "Inactivo"}
                          </span>
                        </td>

                        <td className="px-6 py-5">
                          <span
                            className={[
                              "inline-flex rounded-full px-3 py-1 text-xs font-semibold",

                              service.active
                              && service.bookingEnabled
                                ? "bg-[#edf4ee] text-[#55735c]"
                                : "bg-[#f7f0e5] text-[#947348]",
                            ].join(
                              " ",
                            )}
                          >
                            {service.active
                            && service.bookingEnabled
                              ? "Habilitadas"
                              : "Deshabilitadas"}
                          </span>
                        </td>

                        <td className="px-6 py-5">
                          <div className="flex justify-end gap-2">

                            <button
                              type="button"
                              onClick={() =>
                                openEditModal(
                                  service,
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
                                  service,
                                )
                              }
                              disabled={
                                updatingServiceId
                                === service.id
                                || !service.active
                              }
                              className={[
                                "rounded-full px-3.5 py-2 text-xs font-semibold transition disabled:cursor-not-allowed disabled:opacity-45",

                                service.bookingEnabled
                                  ? "border border-[#e4d6c4] bg-[#fffaf4] text-[#8b6d47] hover:bg-[#f8efe3]"
                                  : "border border-[#cfddd1] bg-[#f4f8f4] text-[#55705a] hover:bg-[#eaf2eb]",
                              ].join(
                                " ",
                              )}
                            >
                              {service.bookingEnabled
                                ? "Pausar reservas"
                                : "Habilitar reservas"}
                            </button>

                            <button
                              type="button"
                              onClick={() =>
                                void toggleServiceActive(
                                  service,
                                )
                              }
                              disabled={
                                updatingServiceId
                                === service.id
                              }
                              className={[
                                "rounded-full px-3.5 py-2 text-xs font-semibold transition disabled:cursor-not-allowed disabled:opacity-50",

                                service.active
                                  ? "border border-[#e4cece] bg-[#fffafa] text-[#925d5d] hover:bg-[#f9eeee]"
                                  : "border border-[#cfded1] bg-[#f4f8f4] text-[#55705a] hover:bg-[#eaf2eb]",
                              ].join(
                                " ",
                              )}
                            >
                              {updatingServiceId
                              === service.id
                                ? "Guardando..."
                                : service.active
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
                MOBILE / TABLET
            =============================================== */}

            <div className="divide-y divide-[#f0eae4] lg:hidden">
              {filteredServices.map(
                (
                  service,
                ) => (
                  <article
                    key={
                      service.id
                    }
                    className="p-5"
                  >
                    <div className="flex items-start justify-between gap-4">

                      <div className="min-w-0">
                        <div className="text-base font-semibold text-[#342e2a]">
                          {service.name}
                        </div>

                        <div className="mt-1 text-xs text-[#96877c]">
                          /{service.slug}
                        </div>
                      </div>

                      <span
                        className={[
                          "shrink-0 rounded-full px-2.5 py-1 text-[11px] font-semibold",

                          service.active
                            ? "bg-[#edf4ee] text-[#55735c]"
                            : "bg-[#f1eeee] text-[#756b6b]",
                        ].join(
                          " ",
                        )}
                      >
                        {service.active
                          ? "Activo"
                          : "Inactivo"}
                      </span>
                    </div>

                    {service.description && (
                      <p className="mt-3 text-sm leading-6 text-[#7c6e65]">
                        {service.description}
                      </p>
                    )}

                    <div className="mt-4 grid grid-cols-2 gap-3">

                      <div className="rounded-2xl bg-[#f8f5f1] p-3">
                        <div className="text-[10px] font-semibold uppercase tracking-[0.12em] text-[#9a897d]">
                          Duración
                        </div>

                        <div className="mt-1 text-sm font-semibold text-[#443b35]">
                          {formatDuration(
                            service.durationMinutes,
                          )}
                        </div>
                      </div>

                      <div className="rounded-2xl bg-[#f8f5f1] p-3">
                        <div className="text-[10px] font-semibold uppercase tracking-[0.12em] text-[#9a897d]">
                          Precio
                        </div>

                        <div className="mt-1 text-sm font-semibold text-[#443b35]">
                          {formatPrice(
                            service.price,
                          )}
                        </div>
                      </div>
                    </div>

                    <div className="mt-3">
                      <span
                        className={[
                          "inline-flex rounded-full px-3 py-1 text-xs font-semibold",

                          service.active
                          && service.bookingEnabled
                            ? "bg-[#edf4ee] text-[#55735c]"
                            : "bg-[#f7f0e5] text-[#947348]",
                        ].join(
                          " ",
                        )}
                      >
                        {service.active
                        && service.bookingEnabled
                          ? "Reservas habilitadas"
                          : "Reservas deshabilitadas"}
                      </span>
                    </div>

                    <div className="mt-5 flex flex-wrap gap-2">

                      <button
                        type="button"
                        onClick={() =>
                          openEditModal(
                            service,
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
                            service,
                          )
                        }
                        disabled={
                          updatingServiceId
                          === service.id
                          || !service.active
                        }
                        className="rounded-full border border-[#dfd3c3] bg-[#fffaf4] px-3.5 py-2 text-xs font-semibold text-[#866c4c] disabled:opacity-45"
                      >
                        {service.bookingEnabled
                          ? "Pausar reservas"
                          : "Habilitar reservas"}
                      </button>

                      <button
                        type="button"
                        onClick={() =>
                          void toggleServiceActive(
                            service,
                          )
                        }
                        disabled={
                          updatingServiceId
                          === service.id
                        }
                        className={[
                          "rounded-full px-3.5 py-2 text-xs font-semibold disabled:opacity-50",

                          service.active
                            ? "border border-[#e4cece] bg-[#fffafa] text-[#925d5d]"
                            : "border border-[#cfded1] bg-[#f4f8f4] text-[#55705a]",
                        ].join(
                          " ",
                        )}
                      >
                        {updatingServiceId
                        === service.id
                          ? "Guardando..."
                          : service.active
                            ? "Desactivar"
                            : "Activar"}
                      </button>
                    </div>

                    <div className="mt-4 text-[11px] text-[#9c8d83]">
                      Creado el{" "}
                      {formatDate(
                        service.createdAt,
                      )}
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
        <ServiceModal
          title="Nuevo servicio"
          subtitle="Añade un nuevo servicio al catálogo del negocio."
          form={
            form
          }
          setForm={
            setForm
          }
          slugManuallyEdited={
            slugManuallyEdited
          }
          setSlugManuallyEdited={
            setSlugManuallyEdited
          }
          error={
            formError
          }
          saving={
            saving
          }
          submitLabel="Crear servicio"
          onClose={
            closeCreateModal
          }
          onSubmit={() =>
            void handleCreateService()
          }
        />
      )}

      {/* ===================================================
          EDIT MODAL
      =================================================== */}

      {editOpen
      && selectedService
      && (
        <ServiceModal
          title="Editar servicio"
          subtitle={
            selectedService.name
          }
          form={
            form
          }
          setForm={
            setForm
          }
          slugManuallyEdited={
            slugManuallyEdited
          }
          setSlugManuallyEdited={
            setSlugManuallyEdited
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
            void handleUpdateService()
          }
        />
      )}
    </div>
  );
}

/* =========================================================
   MODAL
========================================================= */

type ServiceModalProps = {
  title: string;
  subtitle: string;

  form: ServiceFormState;

  setForm: Dispatch<
    SetStateAction<ServiceFormState>
  >;

  slugManuallyEdited: boolean;

  setSlugManuallyEdited: Dispatch<
    SetStateAction<boolean>
  >;

  error: string | null;

  saving: boolean;

  submitLabel: string;

  onClose: () => void;
  onSubmit: () => void;
};

function ServiceModal({
  title,
  subtitle,
  form,
  setForm,
  slugManuallyEdited,
  setSlugManuallyEdited,
  error,
  saving,
  submitLabel,
  onClose,
  onSubmit,
}: ServiceModalProps) {

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
              Servicios
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

          <label className="block">
            <span className="text-xs font-semibold uppercase tracking-[0.12em] text-[#8e7e73]">
              Nombre *
            </span>

            <input
              type="text"
              value={
                form.name
              }
              maxLength={
                150
              }
              onChange={
                (
                  event,
                ) => {
                  const name =
                    event.target.value;

                  setForm(
                    (
                      current,
                    ) => ({
                      ...current,

                      name,

                      slug:
                        slugManuallyEdited
                          ? current.slug
                          : createSlug(
                              name,
                            ),
                    }),
                  );
                }
              }
              placeholder="Ej. Tratamiento facial"
              className="mt-2 w-full rounded-2xl border border-[#ddd3cb] bg-white px-4 py-3 text-sm text-[#352f2b] outline-none transition focus:border-[#b9a89c] focus:ring-2 focus:ring-[#ded3cb]/50"
            />
          </label>

          {/* SLUG */}

          <label className="mt-5 block">
            <span className="text-xs font-semibold uppercase tracking-[0.12em] text-[#8e7e73]">
              Slug *
            </span>

            <input
              type="text"
              value={
                form.slug
              }
              maxLength={
                180
              }
              onChange={
                (
                  event,
                ) => {
                  setSlugManuallyEdited(
                    true,
                  );

                  setForm(
                    (
                      current,
                    ) => ({
                      ...current,

                      slug:
                        createSlug(
                          event.target.value,
                        ),
                    }),
                  );
                }
              }
              placeholder="tratamiento-facial"
              className="mt-2 w-full rounded-2xl border border-[#ddd3cb] bg-white px-4 py-3 text-sm text-[#352f2b] outline-none transition focus:border-[#b9a89c] focus:ring-2 focus:ring-[#ded3cb]/50"
            />

            <p className="mt-2 text-xs leading-5 text-[#998a80]">
              Identificador interno y URL amigable. Se genera automáticamente
              al escribir el nombre.
            </p>
          </label>

          {/* DESCRIPTION */}

          <label className="mt-5 block">
            <span className="text-xs font-semibold uppercase tracking-[0.12em] text-[#8e7e73]">
              Descripción
            </span>

            <textarea
              value={
                form.description
              }
              maxLength={
                5000
              }
              rows={
                4
              }
              onChange={
                (
                  event,
                ) =>
                  setForm(
                    (
                      current,
                    ) => ({
                      ...current,

                      description:
                        event.target.value,
                    }),
                  )
              }
              placeholder="Describe brevemente el servicio..."
              className="mt-2 w-full resize-none rounded-2xl border border-[#ddd3cb] bg-white px-4 py-3 text-sm leading-6 text-[#352f2b] outline-none transition focus:border-[#b9a89c] focus:ring-2 focus:ring-[#ded3cb]/50"
            />
          </label>

          {/* DURATION / PRICE */}

          <div className="mt-5 grid gap-5 sm:grid-cols-2">

            <label className="block">
              <span className="text-xs font-semibold uppercase tracking-[0.12em] text-[#8e7e73]">
                Duración *
              </span>

              <div className="relative mt-2">
                <input
                  type="number"
                  min={
                    1
                  }
                  step={
                    1
                  }
                  value={
                    form.durationMinutes
                  }
                  onChange={
                    (
                      event,
                    ) =>
                      setForm(
                        (
                          current,
                        ) => ({
                          ...current,

                          durationMinutes:
                            event.target.value,
                        }),
                      )
                  }
                  className="w-full rounded-2xl border border-[#ddd3cb] bg-white px-4 py-3 pr-16 text-sm text-[#352f2b] outline-none transition focus:border-[#b9a89c] focus:ring-2 focus:ring-[#ded3cb]/50"
                />

                <span className="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-xs font-medium text-[#9a8a80]">
                  min
                </span>
              </div>
            </label>

            <label className="block">
              <span className="text-xs font-semibold uppercase tracking-[0.12em] text-[#8e7e73]">
                Precio *
              </span>

              <div className="relative mt-2">
                <input
                  type="number"
                  min={
                    0
                  }
                  step={
                    0.01
                  }
                  value={
                    form.price
                  }
                  onChange={
                    (
                      event,
                    ) =>
                      setForm(
                        (
                          current,
                        ) => ({
                          ...current,

                          price:
                            event.target.value,
                        }),
                      )
                  }
                  placeholder="49.00"
                  className="w-full rounded-2xl border border-[#ddd3cb] bg-white px-4 py-3 pr-12 text-sm text-[#352f2b] outline-none transition focus:border-[#b9a89c] focus:ring-2 focus:ring-[#ded3cb]/50"
                />

                <span className="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-sm font-medium text-[#8d7d72]">
                  €
                </span>
              </div>
            </label>
          </div>

          {/* ACTIVE */}

          <div className="mt-6 space-y-3">

            <div className="flex items-center justify-between gap-4 rounded-2xl border border-[#e1d8d1] bg-[#f8f5f1] p-4">
              <div>
                <div className="text-sm font-semibold text-[#3b342f]">
                  Servicio activo
                </div>

                <div className="mt-1 text-xs leading-5 text-[#8a7b71]">
                  Mantiene el servicio disponible dentro del catálogo.
                </div>
              </div>

              <ToggleSwitch
                active={
                  form.active
                }
                onClick={() =>
                  setForm(
                    (
                      current,
                    ) => ({
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

            {/* BOOKING */}

            <div
              className={[
                "flex items-center justify-between gap-4 rounded-2xl border p-4 transition",

                form.active
                  ? "border-[#e1d8d1] bg-[#f8f5f1]"
                  : "border-[#e5dfdb] bg-[#f5f2ef] opacity-60",
              ].join(
                " ",
              )}
            >
              <div>
                <div className="text-sm font-semibold text-[#3b342f]">
                  Permitir reservas
                </div>

                <div className="mt-1 text-xs leading-5 text-[#8a7b71]">
                  El servicio aparecerá disponible para crear nuevas citas.
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
                    (
                      current,
                    ) => ({
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
              || form.name.trim() === ""
              || form.slug.trim() === ""
              || form.durationMinutes.trim() === ""
              || form.price.trim() === ""
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
      ].join(
        " ",
      )}
    >
      <span
        className={[
          "absolute top-1 h-5 w-5 rounded-full bg-white shadow-sm transition",

          active
            ? "left-6"
            : "left-1",
        ].join(
          " ",
        )}
      />
    </button>
  );
}