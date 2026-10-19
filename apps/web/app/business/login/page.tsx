"use client";

import {
  FormEvent,
  useEffect,
  useState,
} from "react";

import {
  useRouter,
} from "next/navigation";

import {
  ApiError,
  apiPost,
} from "@/lib/api";

type BusinessRole =
  | "OWNER"
  | "ADMIN"
  | "STAFF";

type LoginResponse = {
  accessToken: string;
  refreshToken: string;
  tokenType: string;

  accessTokenExpiresAt: string;
  refreshTokenExpiresAt: string;

  userId: number;

  email: string;

  firstName: string | null;
  lastName: string | null;
  fullName: string | null;

  businessId: number;
  businessName: string;

  role: BusinessRole;
};

type StoredBusinessSession = {
  userId: number;

  email: string;

  firstName: string | null;
  lastName: string | null;
  fullName: string | null;

  businessId: number;
  businessName: string;

  role: BusinessRole;

  accessTokenExpiresAt: string;
  refreshTokenExpiresAt: string;
};

const ACCESS_TOKEN_STORAGE_KEY =
  "wacds_access_token";

const REFRESH_TOKEN_STORAGE_KEY =
  "wacds_refresh_token";

const BUSINESS_SESSION_STORAGE_KEY =
  "wacds_business_session";

function normalizeEmail(
  value: string,
): string {
  return value
    .trim()
    .toLowerCase();
}

function persistSession(
  response: LoginResponse,
) {
  window.localStorage.setItem(
    ACCESS_TOKEN_STORAGE_KEY,
    response.accessToken,
  );

  window.localStorage.setItem(
    REFRESH_TOKEN_STORAGE_KEY,
    response.refreshToken,
  );

  const session: StoredBusinessSession = {
    userId:
      response.userId,

    email:
      response.email,

    firstName:
      response.firstName,

    lastName:
      response.lastName,

    fullName:
      response.fullName,

    businessId:
      response.businessId,

    businessName:
      response.businessName,

    role:
      response.role,

    accessTokenExpiresAt:
      response.accessTokenExpiresAt,

    refreshTokenExpiresAt:
      response.refreshTokenExpiresAt,
  };

  window.localStorage.setItem(
    BUSINESS_SESSION_STORAGE_KEY,
    JSON.stringify(
      session,
    ),
  );
}

function removeExistingSession() {
  window.localStorage.removeItem(
    ACCESS_TOKEN_STORAGE_KEY,
  );

  window.localStorage.removeItem(
    REFRESH_TOKEN_STORAGE_KEY,
  );

  window.localStorage.removeItem(
    BUSINESS_SESSION_STORAGE_KEY,
  );
}

function resolveLoginError(
  error: unknown,
): string {
  if (
    error instanceof ApiError
  ) {
    if (
      error.status === 401
    ) {
      return "El email o la contraseña no son correctos.";
    }

    if (
      error.status === 403
    ) {
      return "Tu usuario no tiene acceso al panel de este negocio.";
    }

    if (
      error.status === 400
    ) {
      return error.message
        || "Los datos introducidos no son válidos.";
    }

    if (
      error.status >= 500
    ) {
      return "El servidor ha encontrado un problema. Inténtalo de nuevo.";
    }

    return error.message
      || "No se ha podido iniciar sesión.";
  }

  if (
    error instanceof TypeError
  ) {
    return (
      "No se ha podido conectar con la API. "
      + "Comprueba que el backend esté iniciado."
    );
  }

  return "No se ha podido iniciar sesión.";
}

export default function BusinessLoginPage() {
  const router =
    useRouter();

  const [
    email,
    setEmail,
  ] =
    useState(
      "",
    );

  const [
    password,
    setPassword,
  ] =
    useState(
      "",
    );

  const [
    showPassword,
    setShowPassword,
  ] =
    useState(
      false,
    );

  const [
    submitting,
    setSubmitting,
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

  useEffect(
    () => {
      const existingToken =
        window.localStorage.getItem(
          ACCESS_TOKEN_STORAGE_KEY,
        );

      if (
        existingToken
          ?.trim()
      ) {
        router.replace(
          "/business/appointments",
        );
      }
    },
    [
      router,
    ],
  );

  async function handleSubmit(
    event: FormEvent<HTMLFormElement>,
  ) {
    event.preventDefault();

    if (
      submitting
    ) {
      return;
    }

    const normalizedEmail =
      normalizeEmail(
        email,
      );

    if (
      !normalizedEmail
    ) {
      setError(
        "Introduce tu email.",
      );

      return;
    }

    if (
      !password
    ) {
      setError(
        "Introduce tu contraseña.",
      );

      return;
    }

    setSubmitting(
      true,
    );

    setError(
      null,
    );

    try {
      removeExistingSession();

      const response =
        await apiPost<LoginResponse>(
          "/api/v1/auth/login",
          {
            email:
              normalizedEmail,

            password,
          },
        );

      if (
        !response.accessToken
          || !response.refreshToken
      ) {
        throw new Error(
          "La respuesta de autenticación no contiene los tokens esperados.",
        );
      }

      persistSession(
        response,
      );

      router.replace(
        "/business/appointments",
      );

      router.refresh();
    } catch (
      loginError
    ) {
      removeExistingSession();

      setError(
        resolveLoginError(
          loginError,
        ),
      );
    } finally {
      setSubmitting(
        false,
      );
    }
  }

  return (
    <div className="flex min-h-[calc(100vh-140px)] items-center justify-center py-8 md:py-12">
      <div className="w-full max-w-[470px]">
        <div className="rounded-[30px] border border-[#e2d9d1] bg-white p-6 shadow-[0_22px_70px_rgba(58,44,36,0.08)] sm:p-8 md:p-10">
          <div className="text-center">
            <div className="text-[11px] font-semibold uppercase tracking-[0.3em] text-[#a18b7d]">
              WAC D.S.
            </div>

            <h1 className="mt-3 text-3xl font-semibold tracking-[-0.04em] text-[#302a26]">
              Acceso Business
            </h1>

            <p className="mx-auto mt-3 max-w-sm text-sm leading-6 text-[#817269]">
              Accede al panel privado para gestionar citas, clientes,
              profesionales y servicios.
            </p>
          </div>

          <form
            onSubmit={
              handleSubmit
            }
            className="mt-8 space-y-5"
          >
            <div>
              <label
                htmlFor="business-email"
                className="mb-2 block text-xs font-semibold uppercase tracking-[0.12em] text-[#8e7c70]"
              >
                Email
              </label>

              <input
                id="business-email"
                name="email"
                type="email"
                autoComplete="email"
                required
                value={
                  email
                }
                onChange={
                  (
                    event,
                  ) => {
                    setEmail(
                      event.target.value,
                    );

                    if (
                      error
                    ) {
                      setError(
                        null,
                      );
                    }
                  }
                }
                placeholder="tu@email.com"
                disabled={
                  submitting
                }
                className="h-13 w-full rounded-2xl border border-[#e1d8d0] bg-[#faf8f6] px-4 text-sm text-[#342e2a] outline-none transition placeholder:text-[#b3a69d] focus:border-[#ae998b] focus:bg-white disabled:cursor-not-allowed disabled:opacity-60"
              />
            </div>

            <div>
              <div className="mb-2 flex items-center justify-between gap-4">
                <label
                  htmlFor="business-password"
                  className="block text-xs font-semibold uppercase tracking-[0.12em] text-[#8e7c70]"
                >
                  Contraseña
                </label>
              </div>

              <div className="relative">
                <input
                  id="business-password"
                  name="password"
                  type={
                    showPassword
                      ? "text"
                      : "password"
                  }
                  autoComplete="current-password"
                  required
                  value={
                    password
                  }
                  onChange={
                    (
                      event,
                    ) => {
                      setPassword(
                        event.target.value,
                      );

                      if (
                        error
                      ) {
                        setError(
                          null,
                        );
                      }
                    }
                  }
                  placeholder="Introduce tu contraseña"
                  disabled={
                    submitting
                  }
                  className="h-13 w-full rounded-2xl border border-[#e1d8d0] bg-[#faf8f6] px-4 pr-24 text-sm text-[#342e2a] outline-none transition placeholder:text-[#b3a69d] focus:border-[#ae998b] focus:bg-white disabled:cursor-not-allowed disabled:opacity-60"
                />

                <button
                  type="button"
                  onClick={() =>
                    setShowPassword(
                      (
                        current,
                      ) =>
                        !current,
                    )
                  }
                  disabled={
                    submitting
                  }
                  className="absolute right-4 top-1/2 -translate-y-1/2 text-xs font-semibold text-[#837269] transition hover:text-[#40362f] disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {showPassword
                    ? "Ocultar"
                    : "Mostrar"}
                </button>
              </div>
            </div>

            {error && (
              <div
                role="alert"
                className="rounded-2xl border border-[#ead0d0] bg-[#fbf1f1] px-4 py-3"
              >
                <div className="text-sm font-semibold text-[#8c5353]">
                  No se ha podido iniciar sesión
                </div>

                <div className="mt-1 text-sm leading-5 text-[#936c6c]">
                  {error}
                </div>
              </div>
            )}

            <button
              type="submit"
              disabled={
                submitting
              }
              className="flex h-13 w-full items-center justify-center rounded-2xl bg-[#342d28] px-5 text-sm font-semibold text-white transition hover:bg-[#221d19] disabled:cursor-not-allowed disabled:opacity-60"
            >
              {submitting
                ? "Accediendo..."
                : "Entrar al panel"}
            </button>
          </form>

          <div className="mt-7 border-t border-[#eee7e1] pt-6">
            <div className="rounded-2xl bg-[#f7f3ef] p-4">
              <div className="text-xs font-semibold uppercase tracking-[0.12em] text-[#9a877a]">
                Acceso privado
              </div>

              <p className="mt-2 text-xs leading-5 text-[#786b62]">
                Esta área está reservada a usuarios autorizados del negocio.
                OWNER, ADMIN y STAFF acceden únicamente a los datos de su
                negocio.
              </p>
            </div>
          </div>
        </div>

        <p className="mt-5 text-center text-xs leading-5 text-[#94867c]">
          WAC D.S. · Gestión de reservas y citas
        </p>
      </div>
    </div>
  );
}