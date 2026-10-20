"use client";

import type {
  ReactNode,
} from "react";

import {
  useEffect,
  useRef,
  useState,
} from "react";

import Link from "next/link";

import {
  usePathname,
} from "next/navigation";

import { business } from "@/config/business";
import {
  apiPost,
  clearBusinessSession,
  getCurrentRefreshToken,
} from "@/lib/api";

/* =========================================================
   TYPES
========================================================= */

type BusinessLayoutProps = {
  children: ReactNode;
};

type BusinessRole =
  | "OWNER"
  | "ADMIN"
  | "STAFF";

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

type NavigationItem = {
  label: string;
  shortLabel: string;
  href: string;
};

/* =========================================================
   CONSTANTS
========================================================= */

const BUSINESS_SESSION_STORAGE_KEY =
  "wacds_business_session";

const navigationItems: NavigationItem[] = [
  {
    label:
      "Dashboard",

    shortLabel:
      "Inicio",

    href:
      "/business",
  },

  {
    label:
      "Citas",

    shortLabel:
      "Citas",

    href:
      "/business/appointments",
  },

  {
    label:
      "Clientes",

    shortLabel:
      "Clientes",

    href:
      "/business/customers",
  },

  {
    label:
      "Servicios",

    shortLabel:
      "Servicios",

    href:
      "/business/services",
  },

  {
    label:
      "Profesionales",

    shortLabel:
      "Equipo",

    href:
      "/business/professionals",
  },

  {
    label:
      "Configuración",

    shortLabel:
      "Ajustes",

    href:
      "/business/settings",
  },
];

/* =========================================================
   PATH
========================================================= */

function normalizePathname(
  pathname: string,
): string {
  if (
    pathname.length > 1
    && pathname.endsWith("/")
  ) {
    return pathname.slice(
      0,
      -1,
    );
  }

  return pathname;
}

function isNavigationItemActive(
  pathname: string,
  href: string,
): boolean {
  const normalizedPathname =
    normalizePathname(
      pathname,
    );

  const normalizedHref =
    normalizePathname(
      href,
    );

  if (
    normalizedHref
    === "/business"
  ) {
    return (
      normalizedPathname
      === normalizedHref
    );
  }

  return (
    normalizedPathname
    === normalizedHref
    || normalizedPathname.startsWith(
      `${normalizedHref}/`,
    )
  );
}

/* =========================================================
   SESSION
========================================================= */

function readStoredBusinessSession():
  | StoredBusinessSession
  | null {
  if (
    typeof window === "undefined"
  ) {
    return null;
  }

  const rawSession =
    window.localStorage.getItem(
      BUSINESS_SESSION_STORAGE_KEY,
    );

  if (
    !rawSession
    || rawSession.trim() === ""
  ) {
    return null;
  }

  try {
    const parsed =
      JSON.parse(
        rawSession,
      ) as StoredBusinessSession;

    if (
      !parsed
      || !parsed.userId
      || !parsed.businessId
      || !parsed.email
      || !parsed.role
    ) {
      return null;
    }

    return parsed;
  } catch {
    return null;
  }
}

/* =========================================================
   DISPLAY HELPERS
========================================================= */

function getUserDisplayName(
  session:
    StoredBusinessSession
    | null,
): string {
  if (
    session?.fullName
      ?.trim()
  ) {
    return session.fullName.trim();
  }

  const parts = [
    session?.firstName?.trim(),
    session?.lastName?.trim(),
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

  if (
    session?.email
  ) {
    return session.email;
  }

  return "Administrador";
}

function getBusinessDisplayName(
  session:
    StoredBusinessSession
    | null,
): string {
  return (
    session?.businessName?.trim()
    || business.brand.name
  );
}

function getRoleDisplayName(
  session:
    StoredBusinessSession
    | null,
): string {
  switch (
    session?.role
  ) {
    case "OWNER":
      return "Propietario";

    case "ADMIN":
      return "Administrador";

    case "STAFF":
      return "Personal";

    default:
      return "Business";
  }
}

function getInitials(
  session:
    StoredBusinessSession
    | null,
): string {
  const fullName =
    getUserDisplayName(
      session,
    );

  const normalized =
    fullName
      .trim()
      .split(
        /\s+/,
      )
      .filter(
        Boolean,
      );

  if (
    normalized.length >= 2
  ) {
    return (
      normalized[0][0]
      + normalized[1][0]
    ).toUpperCase();
  }

  if (
    normalized.length === 1
    && normalized[0].length >= 2
  ) {
    return normalized[0]
      .slice(
        0,
        2,
      )
      .toUpperCase();
  }

  return "AS";
}

/* =========================================================
   COMPONENT
========================================================= */

export default function BusinessLayout({
  children,
}: BusinessLayoutProps) {

  const pathname =
    usePathname();

  const normalizedPathname =
    normalizePathname(
      pathname,
    );

  const [
    session,
    setSession,
  ] =
    useState<
      StoredBusinessSession
      | null
    >(
      null,
    );

  const [
    loggingOut,
    setLoggingOut,
  ] =
    useState(
      false,
    );

  const loggingOutRef =
    useRef(
      false,
    );

  async function handleLogout() {
    if (
      loggingOutRef.current
    ) {
      return;
    }

    loggingOutRef.current =
      true;

    setLoggingOut(
      true,
    );

    try {
      const refreshToken =
        getCurrentRefreshToken();

      if (
        refreshToken
      ) {
        await apiPost<null>(
          "/api/v1/auth/logout",
          {
            refreshToken,
          },
          {
            skipAuthRefresh:
              true,
          },
        );
      }
    } catch {
      /*
       * El cierre local no depende de la respuesta.
       * Un 401, un token ya revocado o el API caído
       * deben dejar igualmente la sesión cerrada.
       */
    } finally {
      clearBusinessSession();

      window.location.replace(
        "/business/login",
      );
    }
  }

  /* =======================================================
     SESSION SYNC
  ======================================================= */

  useEffect(
    () => {
      setSession(
        readStoredBusinessSession(),
      );
    },
    [
      normalizedPathname,
    ],
  );

  /* =======================================================
     LOGIN
  ======================================================= */

  const isLoginPage =
    normalizedPathname
    === "/business/login";

  if (
    isLoginPage
  ) {
    return (
      <div className="min-h-screen bg-[#f5f1ec] text-[#2f2925]">
        {children}
      </div>
    );
  }

  /* =======================================================
     SESSION DISPLAY DATA
  ======================================================= */

  const businessName =
    getBusinessDisplayName(
      session,
    );

  const userName =
    getUserDisplayName(
      session,
    );

  const userRole =
    getRoleDisplayName(
      session,
    );

  const userInitials =
    getInitials(
      session,
    );

  /* =======================================================
     BUSINESS SHELL
  ======================================================= */

  return (
    <div className="business-panel min-h-screen bg-[#f6f2ed] text-[#241f1b]">

      <div className="flex min-h-screen">

        {/* ===================================================
            DESKTOP SIDEBAR
        =================================================== */}

        <aside className="bp-sidebar hidden w-[272px] shrink-0 border-r lg:flex lg:flex-col">

          {/* BRAND */}

          <div className="border-b border-[#e0d6cd] px-7 py-8">
            <Link
              href="/business"
              className="inline-flex flex-col gap-1"
            >
              <span className="text-[11px] font-semibold uppercase tracking-[0.28em] text-[#9a8374]">
                WAC D.S.
              </span>

              <span className="text-xl font-semibold tracking-[-0.02em] text-[#2f2925]">
                {businessName}
              </span>

              <span className="text-sm text-[#7d6f66]">
                Panel de negocio
              </span>
            </Link>
          </div>

          {/* NAVIGATION */}

          <nav className="flex-1 px-4 py-6">
            <div className="mb-3 px-3 text-[13px] font-semibold uppercase tracking-[0.14em] text-[#5e534b]">
              Gestión
            </div>

            <div className="space-y-1">
              {navigationItems.map(
                (
                  item,
                ) => {
                  const active =
                    isNavigationItemActive(
                      normalizedPathname,
                      item.href,
                    );

                  return (
                    <Link
                      key={
                        item.href
                      }
                      href={
                        item.href
                      }
                      className={[
                        "bp-nav-link",

                        active
                          ? "bp-nav-link-active"
                          : "",
                      ].join(
                        " ",
                      )}
                    >
                      <span>
                        {item.label}
                      </span>

                      {active && (
                        <span className="h-1.5 w-1.5 rounded-full bg-white" />
                      )}
                    </Link>
                  );
                },
              )}
            </div>
          </nav>

          {/* SIDEBAR BUSINESS CARD */}

          <div className="border-t border-[#e0d6cd] p-5">
            <div className="rounded-2xl border border-[#ddd2c8] bg-white/70 p-4">
              <div className="text-sm font-semibold text-[#342e2a]">
                {businessName}
              </div>

              <div className="mt-1 text-xs leading-5 text-[#7d7067]">
                Gestiona citas, clientes, servicios y profesionales desde un
                único espacio.
              </div>

              {session && (
                <div className="mt-4 border-t border-[#e8dfd8] pt-3">
                  <div className="truncate text-xs font-semibold text-[#62564f]">
                    {userName}
                  </div>

                  <div className="mt-1 text-[11px] text-[#96867b]">
                    {userRole}
                  </div>
                </div>
              )}
            </div>
          </div>
        </aside>

        {/* ===================================================
            MAIN COLUMN
        =================================================== */}

        <div className="flex min-w-0 flex-1 flex-col">

          {/* =================================================
              HEADER
          ================================================= */}

          <header className="bp-header sticky top-0 z-30 border-b backdrop-blur">

            <div className="flex min-h-16 items-center justify-between gap-4 px-4 md:px-8 xl:px-10">

              {/* BUSINESS */}

              <Link
                href="/business"
                className="min-w-0"
              >
                <div className="text-[13px] font-semibold uppercase tracking-[0.12em] text-[#5e534b]">
                  Espacio Business
                </div>

                <div className="mt-0.5 truncate text-base font-semibold text-[#241f1b]">
                  {businessName}
                </div>
              </Link>

              {/* USER */}

              <div className="flex items-center gap-2 sm:gap-3">
                <button
                  type="button"
                  onClick={() => {
                    void handleLogout();
                  }}
                  disabled={
                    loggingOut
                  }
                  aria-busy={
                    loggingOut
                  }
                  className="inline-flex shrink-0 items-center justify-center rounded-full border border-[#ddd2c8] bg-white px-3.5 py-2 text-xs font-semibold text-[#62564f] transition hover:border-[#cfc3b8] hover:bg-[#f3ece6] hover:text-[#342d28] disabled:cursor-not-allowed disabled:opacity-60 sm:px-4"
                >
                  {loggingOut
                    ? "Cerrando..."
                    : "Cerrar sesión"}
                </button>

                <div className="hidden text-right sm:block">
                  <div className="max-w-[240px] truncate text-sm font-semibold text-[#332d29]">
                    {userName}
                  </div>

                  <div className="text-xs text-[#8a7a70]">
                    {userRole}
                  </div>
                </div>

                <div
                  className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-[#342d28] text-sm font-semibold text-white shadow-[0_5px_16px_rgba(52,45,40,0.16)]"
                  title={
                    userName
                  }
                >
                  {userInitials}
                </div>
              </div>
            </div>

            {/* ===============================================
                MOBILE / TABLET NAVIGATION
            =============================================== */}

            <nav className="border-t border-[#ebe3dc] bg-[#f8f5f1] lg:hidden">
              <div className="overflow-x-auto px-4 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
                <div className="flex min-w-max gap-1 py-2">

                  {navigationItems.map(
                    (
                      item,
                    ) => {
                      const active =
                        isNavigationItemActive(
                          normalizedPathname,
                          item.href,
                        );

                      return (
                        <Link
                          key={
                            item.href
                          }
                          href={
                            item.href
                          }
                          className={[
                            "relative whitespace-nowrap rounded-full px-4 py-2 text-sm font-semibold transition",

                            active
                              ? "bg-[#342d28] !text-white shadow-[0_5px_15px_rgba(52,45,40,0.15)]"
                              : "text-[#5c5148] hover:bg-[#eee7df] hover:text-[#241f1b]",
                          ].join(
                            " ",
                          )}
                        >
                          {item.shortLabel}
                        </Link>
                      );
                    },
                  )}
                </div>
              </div>
            </nav>
          </header>

          {/* =================================================
              PAGE CONTENT
          ================================================= */}

          <main className="flex-1">
            <div className="mx-auto w-full max-w-[1600px] px-5 py-6 md:px-8 md:py-8 xl:px-10">
              {children}
            </div>
          </main>

          {/* =================================================
              MOBILE FOOTER
          ================================================= */}

          <footer className="border-t border-[#e5ddd5] bg-[#eee7df]/60 px-5 py-5 lg:hidden">
            <div className="flex flex-col gap-1 text-xs text-[#85776e] sm:flex-row sm:items-center sm:justify-between">
              <span>
                WAC D.S. · Panel Business
              </span>

              <span className="font-medium text-[#6e6057]">
                {businessName}
              </span>
            </div>
          </footer>
        </div>
      </div>
    </div>
  );
}