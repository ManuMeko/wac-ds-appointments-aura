"use client";

import Link from "next/link";
import { useId } from "react";

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

type CustomerRequest = {
  firstName: string;
  lastName: string | null;
  email: string | null;
  phone: string | null;
  notes: string | null;
  active: boolean;
};

type CustomerFilter =
  | "ALL"
  | "ACTIVE"
  | "INACTIVE";

type CustomerFormState = {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  notes: string;
  active: boolean;
};

/* =========================================================
   CONSTANTS
========================================================= */

const ACCESS_TOKEN_STORAGE_KEY =
  "wacds_access_token";

const initialFormState: CustomerFormState = {
  firstName: "",
  lastName: "",
  email: "",
  phone: "",
  notes: "",
  active: true,
};

/* =========================================================
   HELPERS
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

function normalizeNullableText(
  value: string,
): string | null {

  const normalized =
    value.trim();

  return normalized === ""
    ? null
    : normalized;
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

    if (
      error.status === 409
    ) {
      return (
        error.message
        || "Existe un conflicto con los datos del cliente."
      );
    }

    return (
      error.message
      || "No se ha podido completar la operación."
    );
  }

  return "No se ha podido conectar con la API Business.";
}

function getCustomerDisplayName(
  customer: CustomerResponse,
): string {

  if (
    customer.fullName
    && customer.fullName.trim()
  ) {
    return customer.fullName.trim();
  }

  const parts = [
    customer.firstName?.trim(),
    customer.lastName?.trim(),
  ].filter(
    Boolean,
  );

  if (
    parts.length > 0
  ) {
    return parts.join(
      " ",
    );
  }

  return "Cliente sin nombre";
}

function getCustomerInitials(
  customer: CustomerResponse,
): string {

  const name =
    getCustomerDisplayName(
      customer,
    );

  const parts =
    name
      .split(
        /\s+/,
      )
      .filter(
        Boolean,
      );

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

  return "CL";
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

/* =========================================================
   COMPONENT
========================================================= */

export default function CustomersPage() {

  /* =======================================================
     STATE
  ======================================================= */

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
    useState<CustomerFilter>(
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
    selectedCustomer,
    setSelectedCustomer,
  ] =
    useState<
      CustomerResponse
      | null
    >(
      null,
    );

  const [
    form,
    setForm,
  ] =
    useState<CustomerFormState>(
      initialFormState,
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
    togglingCustomerId,
    setTogglingCustomerId,
  ] =
    useState<
      number
      | null
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
     LOAD
  ======================================================= */

  const loadCustomers =
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
              CustomerResponse[]
            >(
              "/api/v1/customers",
              {
                accessToken,
              },
            );

          setCustomers(
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

      void Promise.resolve().then(
        () => {
          void loadCustomers();
        },
      );

    },
    [
      loadCustomers,
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
            event.key
            !== "Escape"
          ) {
            return;
          }

          if (
            saving
          ) {
            return;
          }

          setCreateOpen(
            false,
          );

          setEditOpen(
            false,
          );

          setSelectedCustomer(
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
     COMPUTED
  ======================================================= */

  const activeCustomers =
    useMemo(
      () =>
        customers.filter(
          (
            customer,
          ) =>
            customer.active,
        ),
      [
        customers,
      ],
    );

  const inactiveCustomers =
    useMemo(
      () =>
        customers.filter(
          (
            customer,
          ) =>
            !customer.active,
        ),
      [
        customers,
      ],
    );

  const filteredCustomers =
    useMemo(
      () => {

        const normalizedSearch =
          search
            .trim()
            .toLowerCase();

        return customers.filter(
          (
            customer,
          ) => {

            if (
              filter === "ACTIVE"
              && !customer.active
            ) {
              return false;
            }

            if (
              filter === "INACTIVE"
              && customer.active
            ) {
              return false;
            }

            if (
              normalizedSearch === ""
            ) {
              return true;
            }

            const haystack = [
              customer.firstName,
              customer.lastName,
              customer.fullName,
              customer.email,
              customer.phone,
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
        customers,
        filter,
        search,
      ],
    );

  /* =======================================================
     MODAL ACTIONS
  ======================================================= */

  function openCreateModal() {

    setSelectedCustomer(
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
    customer: CustomerResponse,
  ) {

    setSelectedCustomer(
      customer,
    );

    setForm(
      {
        firstName:
          customer.firstName
          ?? "",

        lastName:
          customer.lastName
          ?? "",

        email:
          customer.email
          ?? "",

        phone:
          customer.phone
          ?? "",

        notes:
          customer.notes
          ?? "",

        active:
          customer.active,
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

    setSelectedCustomer(
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
     CREATE
  ======================================================= */

  async function handleCreateCustomer() {

    if (
      saving
    ) {
      return;
    }

    const firstName =
      form.firstName.trim();

    if (
      firstName === ""
    ) {

      setFormError(
        "El nombre del cliente es obligatorio.",
      );

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

      const payload: CustomerRequest = {
        firstName,

        lastName:
          normalizeNullableText(
            form.lastName,
          ),

        email:
          normalizeNullableText(
            form.email,
          ),

        phone:
          normalizeNullableText(
            form.phone,
          ),

        notes:
          normalizeNullableText(
            form.notes,
          ),

        active:
          form.active,
      };

      await apiPost<
        CustomerResponse
      >(
        "/api/v1/customers",
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
        "Cliente creado correctamente.",
      );

      await loadCustomers();

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

  async function handleUpdateCustomer() {

    if (
      saving
      || !selectedCustomer
    ) {
      return;
    }

    const firstName =
      form.firstName.trim();

    if (
      firstName === ""
    ) {

      setFormError(
        "El nombre del cliente es obligatorio.",
      );

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

      const payload: CustomerRequest = {
        firstName,

        lastName:
          normalizeNullableText(
            form.lastName,
          ),

        email:
          normalizeNullableText(
            form.email,
          ),

        phone:
          normalizeNullableText(
            form.phone,
          ),

        notes:
          normalizeNullableText(
            form.notes,
          ),

        active:
          form.active,
      };

      await apiPut<
        CustomerResponse
      >(
        `/api/v1/customers/${selectedCustomer.id}`,
        payload,
        {
          accessToken,
        },
      );

      setEditOpen(
        false,
      );

      setSelectedCustomer(
        null,
      );

      setForm(
        initialFormState,
      );

      setSuccessMessage(
        "Cliente actualizado correctamente.",
      );

      await loadCustomers();

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
     ACTIVE / INACTIVE
  ======================================================= */

  async function toggleCustomerActive(
    customer: CustomerResponse,
  ) {

    if (
      togglingCustomerId != null
    ) {
      return;
    }

    const nextActive =
      !customer.active;

    setTogglingCustomerId(
      customer.id,
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
        CustomerResponse
      >(
        `/api/v1/customers/${customer.id}/active?active=${String(
          nextActive,
        )}`,
        undefined,
        {
          accessToken,
        },
      );

      setSuccessMessage(
        nextActive
          ? "Cliente activado correctamente."
          : "Cliente desactivado correctamente.",
      );

      await loadCustomers();

    } catch (
      requestError
    ) {

      setError(
        resolveApiError(
          requestError,
        ),
      );

    } finally {

      setTogglingCustomerId(
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
            Clientes
          </div>

          <h1 className="mt-2 text-3xl font-semibold tracking-[-0.03em] text-[#2f2925] md:text-4xl">
            Gestión de clientes
          </h1>

          <p className="mt-3 max-w-2xl text-sm leading-6 text-[#796b62] md:text-base">
            Consulta, crea y administra los clientes asociados a tu negocio.
          </p>

        </div>

        <div className="flex flex-wrap gap-3">

          <button
            type="button"
            onClick={() =>
              void loadCustomers()
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
            Nuevo cliente
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

            <p className="mt-1 text-sm text-[#946f6f]">
              {error}
            </p>

          </div>

          <button
            type="button"
            onClick={() =>
              void loadCustomers()
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

      <section className="grid gap-4 sm:grid-cols-3">

        <article className="bp-metric rounded-[26px] border border-[#e8ded5] bg-white p-6 shadow-[0_14px_40px_rgba(62,49,41,0.055)]">

          <div className="text-sm font-medium text-[#786b62]">
            Total clientes
          </div>

          <div className="mt-3 text-3xl font-semibold tracking-[-0.04em] text-[#302a26]">
            {customers.length}
          </div>

        </article>

        <article className="bp-metric rounded-[26px] border border-[#e8ded5] bg-white p-6 shadow-[0_14px_40px_rgba(62,49,41,0.055)]">

          <div className="text-sm font-medium text-[#786b62]">
            Activos
          </div>

          <div className="mt-3 text-3xl font-semibold tracking-[-0.04em] text-[#302a26]">
            {activeCustomers.length}
          </div>

        </article>

        <article className="bp-metric rounded-[26px] border border-[#e8ded5] bg-white p-6 shadow-[0_14px_40px_rgba(62,49,41,0.055)]">

          <div className="text-sm font-medium text-[#786b62]">
            Inactivos
          </div>

          <div className="mt-3 text-3xl font-semibold tracking-[-0.04em] text-[#302a26]">
            {inactiveCustomers.length}
          </div>

        </article>

      </section>

      {/* ===================================================
          FILTERS
      =================================================== */}

      <section className="rounded-[26px] border border-[#e4dcd4] bg-white p-6 shadow-[0_14px_36px_rgba(62,49,41,0.045)]">

        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">

          <div className="relative w-full lg:max-w-lg">
            <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" className="pointer-events-none absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-[#9a8879]"><circle cx="10.8" cy="10.8" r="6.7"/><path d="m16 16 4.4 4.4"/></svg>

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
              placeholder="Buscar por nombre, email o teléfono..."
              aria-label="Buscar clientes"
              className="w-full rounded-[20px] border border-[#e5dbd1] bg-[#faf7f3] py-3.5 pl-12 pr-5 text-sm font-medium text-[#342d28] outline-none transition-all duration-200 placeholder:font-normal placeholder:text-[#a89a90] hover:border-[#c9b8aa] focus:border-[#9d8170] focus:bg-white focus:ring-4 focus:ring-[#bca393]/15"
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
                      "rounded-full border px-5 py-2.5 text-xs font-semibold transition-all duration-200",

                      active
                        ? "border-[#342d28] bg-[#342d28] text-white shadow-[0_7px_18px_rgba(52,45,40,0.16)]"
                        : "border-[#ded5ce] bg-white text-[#74665d] hover:border-[#bda99a] hover:bg-[#f7f3ef]",
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
          {filteredCustomers.length}
          {" "}
          {filteredCustomers.length === 1
            ? "cliente encontrado"
            : "clientes encontrados"}
        </div>

      </section>

      {/* ===================================================
          CUSTOMER LIST
      =================================================== */}

      <section className="overflow-hidden rounded-[28px] border border-[#e5dcd3] bg-white shadow-[0_16px_48px_rgba(62,49,41,0.055)]">

        <div className="border-b border-[#eee7e1] px-6 py-5">

          <h2 className="text-lg font-semibold text-[#312b27]">
            Clientes
          </h2>

          <p className="mt-1 text-sm text-[#8a7b71]">
            Base de clientes del negocio.
          </p>

        </div>

        {loading && (

          <div className="space-y-3 p-6">

            {Array.from(
              {
                length:
                  5,
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
                  className="h-20 animate-pulse rounded-2xl bg-[#f5f1ed]"
                />

              ),
            )}

          </div>

        )}

        {!loading
        && filteredCustomers.length === 0
        && (

          <div className="bp-empty px-6 py-16 text-center">

            <div className="text-base font-semibold text-[#3b342f]">
              No hay clientes que mostrar
            </div>

            <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-[#8a7b71]">
              Ajusta los filtros o crea un nuevo cliente.
            </p>

            <button
              type="button"
              onClick={
                openCreateModal
              }
              className="mt-5 rounded-full bg-[#342d28] px-5 py-2.5 text-sm font-semibold text-white"
            >
              Nuevo cliente
            </button>

          </div>

        )}

        {!loading
        && filteredCustomers.length > 0
        && (
          <>

            {/* ===============================================
                DESKTOP TABLE
            =============================================== */}

            <div className="hidden overflow-x-auto md:block">

              <table className="w-full min-w-[900px]">

                <thead>

                  <tr className="border-b border-[#eee7e1] bg-[#fbf9f7] text-left text-[11px] font-semibold uppercase tracking-[0.14em] text-[#9a8b80]">

                    <th className="px-6 py-4">
                      Cliente
                    </th>

                    <th className="px-6 py-4">
                      Contacto
                    </th>

                    <th className="px-6 py-4">
                      Estado
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

                  {filteredCustomers.map(
                    (
                      customer,
                    ) => (

                      <tr
                        key={
                          customer.id
                        }
                        className="transition-colors duration-200 hover:bg-[#faf7f3]"
                      >

                        <td className="px-6 py-5">

                          <div className="flex items-center gap-3">

                            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-[#eee7df] text-xs font-semibold text-[#68594f]">

                              {getCustomerInitials(
                                customer,
                              )}

                            </div>

                            <div className="min-w-0">

                              <div className="truncate text-sm font-semibold text-[#342e2a]">

                                {getCustomerDisplayName(
                                  customer,
                                )}

                              </div>

                              <div className="mt-1 text-xs text-[#9b8c81]">
                                ID #{customer.id}
                              </div>

                            </div>

                          </div>

                        </td>

                        <td className="px-6 py-5">

                          <div className="text-sm text-[#665a52]">
                            {customer.email
                            || "Sin email"}
                          </div>

                          <div className="mt-1 text-xs text-[#97887e]">
                            {customer.phone
                            || "Sin teléfono"}
                          </div>

                        </td>

                        <td className="px-6 py-5">

                          <span
                            className={[
                              "inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-semibold",

                              customer.active
                                ? "border-[#dce9de] bg-[#edf4ee] text-[#55735c]"
                                : "border-[#e3dddd] bg-[#f1eeee] text-[#756b6b]",
                            ].join(
                              " ",
                            )}
                          >
                            {customer.active
                              ? "Activo"
                              : "Inactivo"}
                          </span>

                        </td>

                        <td className="px-6 py-5 text-sm text-[#776a61]">

                          {formatDate(
                            customer.createdAt,
                          )}

                        </td>

                        <td className="px-6 py-5">

                          <div className="flex justify-end gap-2">

                            <Link
                              href={`/business/customers/${customer.id}`}
                              className="inline-flex min-w-[86px] items-center justify-center whitespace-nowrap rounded-full border border-[#d8cec6] bg-[#f8f4f0] px-4 py-2 text-xs font-semibold leading-none text-[#5f534b] transition hover:border-[#c5b7ac] hover:bg-[#eee7e1]"
                            >
                              Ver ficha
                            </Link>

                            <button
                              type="button"
                              onClick={() =>
                                openEditModal(
                                  customer,
                                )
                              }
                              className="rounded-full border border-[#ddd3cb] bg-white px-3.5 py-2 text-xs font-semibold text-[#65584f] transition hover:bg-[#f7f3ef]"
                            >
                              Editar
                            </button>

                            <button
                              type="button"
                              onClick={() =>
                                void toggleCustomerActive(
                                  customer,
                                )
                              }
                              disabled={
                                togglingCustomerId
                                === customer.id
                              }
                              className={[
                                "rounded-full px-3.5 py-2 text-xs font-semibold transition disabled:cursor-not-allowed disabled:opacity-50",

                                customer.active
                                  ? "border border-[#e4cece] bg-[#fffafa] text-[#925d5d] hover:bg-[#f9eeee]"
                                  : "border border-[#cfded1] bg-[#f4f8f4] text-[#55705a] hover:bg-[#eaf2eb]",
                              ].join(
                                " ",
                              )}
                            >
                              {togglingCustomerId
                              === customer.id
                                ? "Guardando..."
                                : customer.active
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
                MOBILE CARDS
            =============================================== */}

            <div className="divide-y divide-[#f0eae4] md:hidden">

              {filteredCustomers.map(
                (
                  customer,
                ) => (

                  <article
                    key={
                      customer.id
                    }
                    className="p-5"
                  >

                    <div className="flex items-start gap-3">

                      <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-[#eee7df] text-xs font-semibold text-[#68594f]">

                        {getCustomerInitials(
                          customer,
                        )}

                      </div>

                      <div className="min-w-0 flex-1">

                        <div className="flex items-start justify-between gap-3">

                          <div>

                            <div className="text-sm font-semibold text-[#342e2a]">

                              {getCustomerDisplayName(
                                customer,
                              )}

                            </div>

                            <div className="mt-1 text-xs text-[#97887e]">
                              {customer.email
                              || "Sin email"}
                            </div>

                          </div>

                          <span
                            className={[
                              "shrink-0 rounded-full px-2.5 py-1 text-[11px] font-semibold",

                              customer.active
                                ? "border-[#dce9de] bg-[#edf4ee] text-[#55735c]"
                                : "border-[#e3dddd] bg-[#f1eeee] text-[#756b6b]",
                            ].join(
                              " ",
                            )}
                          >
                            {customer.active
                              ? "Activo"
                              : "Inactivo"}
                          </span>

                        </div>

                        <div className="mt-2 text-xs text-[#97887e]">
                          {customer.phone
                          || "Sin teléfono"}
                        </div>

                        <div className="mt-4 flex flex-wrap gap-2">

                          <Link
                            href={`/business/customers/${customer.id}`}
                            className="inline-flex min-w-[86px] items-center justify-center whitespace-nowrap rounded-full border border-[#d8cec6] bg-[#f8f4f0] px-4 py-2 text-xs font-semibold leading-none text-[#5f534b] transition hover:border-[#c5b7ac] hover:bg-[#eee7e1]"
                          >
                            Ver ficha
                          </Link>

                          <button
                            type="button"
                            onClick={() =>
                              openEditModal(
                                customer,
                              )
                            }
                            className="rounded-full border border-[#ddd3cb] bg-white px-3.5 py-2 text-xs font-semibold text-[#65584f]"
                          >
                            Editar
                          </button>

                          <button
                            type="button"
                            onClick={() =>
                              void toggleCustomerActive(
                                customer,
                              )
                            }
                            disabled={
                              togglingCustomerId
                              === customer.id
                            }
                            className={[
                              "rounded-full px-3.5 py-2 text-xs font-semibold disabled:opacity-50",

                              customer.active
                                ? "border border-[#e4cece] bg-[#fffafa] text-[#925d5d]"
                                : "border border-[#cfded1] bg-[#f4f8f4] text-[#55705a]",
                            ].join(
                              " ",
                            )}
                          >
                            {togglingCustomerId
                            === customer.id
                              ? "Guardando..."
                              : customer.active
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

        <CustomerModal
          title="Nuevo cliente"
          subtitle="Añade un cliente a la base de datos del negocio."
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
          submitLabel="Crear cliente"
          onClose={
            closeCreateModal
          }
          onSubmit={() =>
            void handleCreateCustomer()
          }
        />

      )}

      {/* ===================================================
          EDIT MODAL
      =================================================== */}

      {editOpen
      && selectedCustomer
      && (

        <CustomerModal
          title="Editar cliente"
          subtitle={getCustomerDisplayName(
            selectedCustomer,
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
            void handleUpdateCustomer()
          }
        />

      )}

    </div>
  );
}

/* =========================================================
   CUSTOMER MODAL
========================================================= */

type CustomerModalProps = {
  title: string;
  subtitle: string;

  form: CustomerFormState;

  setForm: React.Dispatch<
    React.SetStateAction<CustomerFormState>
  >;

  error: string | null;

  saving: boolean;

  submitLabel: string;

  onClose: () => void;
  onSubmit: () => void;
};

function CustomerModal({
  title,
  subtitle,
  form,
  setForm,
  error,
  saving,
  submitLabel,
  onClose,
  onSubmit,
}: CustomerModalProps) {
  const titleId = useId();
  const descriptionId = useId();
  const isEditing = title.toLowerCase().includes("editar");

  const inputClass =
    "mt-2.5 h-[54px] w-full rounded-[19px] border border-[#e3d8ce] bg-[#fbf9f6] px-5 text-[14px] font-medium text-[#342d28] shadow-[0_3px_12px_rgba(56,44,35,0.025)] outline-none transition-all duration-200 placeholder:font-normal placeholder:text-[#ac9d92] hover:border-[#c7b5a7] focus:border-[#9b8070] focus:bg-white focus:ring-4 focus:ring-[#b9a18e]/20 disabled:cursor-not-allowed disabled:opacity-55";

  const labelClass =
    "block text-[13px] font-semibold text-[#403630]";

  function updateField<K extends keyof CustomerFormState>(
    key: K,
    value: CustomerFormState[K],
  ) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  return (
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center bg-[#241f1b]/50 px-3 py-5 backdrop-blur-[5px] sm:px-5"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !saving) {
          onClose();
        }
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={descriptionId}
        className="relative flex max-h-[min(94vh,920px)] w-full max-w-[860px] flex-col overflow-hidden rounded-[30px] border border-white/75 bg-[#f8f5f1] shadow-[0_35px_100px_rgba(38,30,25,0.30)]"
      >
        {/* HEADER */}
        <div className="flex shrink-0 items-start justify-between gap-4 border-b border-[#e9dfd6] bg-white px-6 py-5 sm:px-8 sm:py-6">
          <div>
            <div className="flex items-center gap-2 text-[10px] font-bold uppercase tracking-[0.20em] text-[#a18a7a]">
              <span className="h-1.5 w-1.5 rounded-full bg-[#a18a7a]" />
              Gestión de clientes
            </div>
            <h2 id={titleId} className="mt-2 text-[26px] font-semibold tracking-[-0.045em] text-[#302923] sm:text-[30px]">
              {title}
            </h2>
            <p id={descriptionId} className="mt-1.5 text-[13px] leading-6 text-[#8a796d]">
              {subtitle}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={saving}
            aria-label="Cerrar formulario"
            className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full border border-[#e2d7ce] bg-[#faf8f5] text-xl text-[#77665b] transition-all hover:rotate-90 hover:bg-[#f0e9e2] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#b9a18e]/25 disabled:opacity-50"
          >
            ×
          </button>
        </div>

        {/* SCROLLABLE BODY */}
        <div className="min-h-0 flex-1 space-y-5 overflow-y-auto px-4 py-5 sm:px-8 sm:py-7">
          {error && (
            <div role="alert" className="rounded-[18px] border border-[#ebd0d0] bg-[#fff3f3] px-5 py-4 text-sm leading-6 text-[#925858]">
              <div className="font-semibold">No se ha podido guardar el cliente</div>
              <p className="mt-1">{error}</p>
            </div>
          )}

          {/* SECTION 1: IDENTITY */}
          <section className="rounded-[25px] border border-[#e7dcd2] bg-white p-5 shadow-[0_10px_30px_rgba(56,44,35,0.045)] sm:p-6">
            <div className="mb-5 flex items-center gap-3">
              <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-[14px] bg-[#342d28] text-xs font-bold text-white shadow-[0_6px_16px_rgba(52,45,40,0.18)]">01</span>
              <div>
                <h3 className="text-[17px] font-semibold tracking-[-0.025em] text-[#342d28]">Datos personales</h3>
                <p className="mt-0.5 text-xs text-[#96877b]">Identificación del cliente</p>
              </div>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <label className={labelClass}>
                Nombre <span className="text-[#aa7563]">*</span>
                <input
                  type="text"
                  value={form.firstName}
                  onChange={(event) => updateField("firstName", event.target.value)}
                  maxLength={100}
                  autoComplete="given-name"
                  required
                  disabled={saving}
                  placeholder="Introduce el nombre"
                  className={inputClass}
                />
              </label>
              <label className={labelClass}>
                Apellidos
                <input
                  type="text"
                  value={form.lastName}
                  onChange={(event) => updateField("lastName", event.target.value)}
                  maxLength={150}
                  autoComplete="family-name"
                  disabled={saving}
                  placeholder="Introduce los apellidos"
                  className={inputClass}
                />
              </label>
            </div>
          </section>

          {/* SECTION 2: CONTACT */}
          <section className="rounded-[25px] border border-[#e7dcd2] bg-white p-5 shadow-[0_10px_30px_rgba(56,44,35,0.045)] sm:p-6">
            <div className="mb-5 flex items-center gap-3">
              <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-[14px] bg-[#342d28] text-xs font-bold text-white shadow-[0_6px_16px_rgba(52,45,40,0.18)]">02</span>
              <div>
                <h3 className="text-[17px] font-semibold tracking-[-0.025em] text-[#342d28]">Contacto</h3>
                <p className="mt-0.5 text-xs text-[#96877b]">Información para localizar al cliente</p>
              </div>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <label className={labelClass}>
                Correo electrónico
                <input
                  type="email"
                  value={form.email}
                  onChange={(event) => updateField("email", event.target.value)}
                  maxLength={180}
                  autoComplete="email"
                  disabled={saving}
                  placeholder="cliente@email.com"
                  className={inputClass}
                />
              </label>
              <label className={labelClass}>
                Teléfono
                <input
                  type="tel"
                  value={form.phone}
                  onChange={(event) => updateField("phone", event.target.value)}
                  maxLength={40}
                  autoComplete="tel"
                  disabled={saving}
                  placeholder="600 000 000"
                  className={inputClass}
                />
              </label>
            </div>
          </section>

          {/* SECTION 3: NOTES */}
          <section className="rounded-[25px] border border-[#e7dcd2] bg-white p-5 shadow-[0_10px_30px_rgba(56,44,35,0.045)] sm:p-6">
            <div className="mb-4 flex items-center gap-3">
              <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-[14px] bg-[#342d28] text-xs font-bold text-white shadow-[0_6px_16px_rgba(52,45,40,0.18)]">03</span>
              <div>
                <h3 className="text-[17px] font-semibold tracking-[-0.025em] text-[#342d28]">Observaciones internas</h3>
                <p className="mt-0.5 text-xs text-[#96877b]">Información útil para el equipo, opcional</p>
              </div>
            </div>
            <label className="block">
              <span className="sr-only">Notas internas del cliente</span>
              <textarea
                value={form.notes}
                onChange={(event) => updateField("notes", event.target.value)}
                maxLength={5000}
                disabled={saving}
                rows={4}
                placeholder="Preferencias, observaciones o información relevante..."
                className="min-h-[140px] w-full resize-none rounded-[24px] border border-[#e3d8ce] bg-[#fbf9f6] px-5 py-4 text-sm leading-6 text-[#342d28] shadow-[0_5px_16px_rgba(56,44,35,0.035)] outline-none transition-all duration-200 placeholder:text-[#ac9d92] hover:border-[#c7b5a7] focus:border-[#9b8070] focus:bg-white focus:shadow-[0_12px_32px_rgba(56,44,35,0.07)] focus:ring-4 focus:ring-[#b9a18e]/20 disabled:opacity-55"
              />
            </label>
            <div className="mt-2 text-right text-xs font-medium text-[#998679]">{form.notes.length}/5000</div>
          </section>

          {/* SECTION 4: STATUS */}
          <section className="flex flex-col gap-4 rounded-[25px] border border-[#e1d6cc] bg-[#eee7df] p-5 sm:flex-row sm:items-center sm:justify-between sm:p-6">
            <div className="flex items-start gap-3">
              <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-[14px] bg-[#342d28] text-xs font-bold text-white shadow-[0_6px_16px_rgba(52,45,40,0.18)]">04</span>
              <div>
                <div className="text-[17px] font-semibold text-[#342d28]">Cliente {form.active ? "activo" : "inactivo"}</div>
                <p className="mt-1 max-w-sm text-xs leading-5 text-[#857469]">
                  {form.active
                    ? "Podrá seleccionarse para nuevas citas."
                    : "Se conservará su historial, pero no estará disponible para nuevas reservas."}
                </p>
              </div>
            </div>
            <button
              type="button"
              role="switch"
              aria-checked={form.active}
              aria-label="Cliente activo"
              onClick={() => updateField("active", !form.active)}
              disabled={saving}
              className={`relative h-9 w-[62px] shrink-0 rounded-full border-2 border-white transition-all duration-200 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[#b9a18e]/25 disabled:opacity-50 ${form.active ? "bg-[#647d6b]" : "bg-[#b4aaa2]"}`}
            >
              <span className={`absolute top-[3px] h-[26px] w-[26px] rounded-full bg-white shadow-[0_3px_8px_rgba(0,0,0,0.14)] transition-all duration-200 ${form.active ? "left-[31px]" : "left-[3px]"}`} />
            </button>
          </section>
        </div>

        {/* FOOTER */}
        <div className="flex shrink-0 flex-col-reverse items-stretch justify-between gap-3 border-t border-[#e9dfd6] bg-white px-5 py-4 sm:flex-row sm:items-center sm:px-8 sm:py-5">
          <div className="text-xs text-[#98887c]">
            {isEditing ? "Los cambios se guardarán en la ficha del cliente." : "El cliente se añadirá a la base de datos del negocio."}
          </div>
          <div className="flex items-center justify-end gap-2.5">
            <button
              type="button"
              onClick={onClose}
              disabled={saving}
              className="rounded-full border border-[#ded3c9] bg-white px-5 py-3 text-xs font-semibold text-[#67594e] transition hover:bg-[#f5f0ec] disabled:opacity-50"
            >
              Cancelar
            </button>
            <button
              type="button"
              onClick={onSubmit}
              disabled={saving || form.firstName.trim() === ""}
              className="rounded-full bg-[#342d28] px-6 py-3 text-xs font-semibold text-white shadow-[0_9px_24px_rgba(52,45,40,0.20)] transition hover:bg-[#221c19] hover:shadow-[0_12px_28px_rgba(52,45,40,0.25)] disabled:cursor-not-allowed disabled:opacity-40"
            >
              {saving ? "Guardando..." : submitLabel}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
