"use client";

import { useEffect, useState } from "react";

import { business } from "@/config/business";
import { getApiBaseUrl } from "@/lib/api";

const TIMEOUT_MS = 4_000;

export type PublicCatalogService = {
  id: number;
  slug: string;
  name: string;
  description: string | null;
  durationMinutes: number;
  price: number;
  currency: string;
};

export type HomeSlot = {
  startTime: string;
  professionalName: string;
};

export type FeaturedAvailability = {
  serviceName: string;
  serviceSlug: string;
  todayTotal: number;
  dayLabel: "hoy" | "mañana";
  slots: HomeSlot[];
};

export type HomeAvailability =
  | {
      status: "ready";
      catalog: PublicCatalogService[];
      featured: FeaturedAvailability | null;
    }
  | { status: "hidden" };

type ProfessionalRow = {
  id: number;
  displayName: string;
};

function apiBase(): string {
  return getApiBaseUrl().replace(/\/$/, "");
}

function todayIso(): string {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${now.getFullYear()}-${month}-${day}`;
}

function addDays(iso: string, days: number): string {
  const [year, month, day] = iso.split("-").map(Number);
  const date = new Date(year, (month ?? 1) - 1, day ?? 1);
  date.setDate(date.getDate() + days);
  const nextMonth = String(date.getMonth() + 1).padStart(2, "0");
  const nextDay = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${nextMonth}-${nextDay}`;
}

async function getJson(url: string, signal: AbortSignal): Promise<unknown> {
  const response = await fetch(url, {
    method: "GET",
    headers: { Accept: "application/json" },
    cache: "no-store",
    signal,
  });

  if (!response.ok) {
    throw new Error("request failed");
  }

  return response.json() as Promise<unknown>;
}

function readServices(value: unknown): PublicCatalogService[] {
  if (!Array.isArray(value)) {
    throw new Error("catalog");
  }

  return value.flatMap((item) => {
    if (!item || typeof item !== "object") return [];

    const row = item as Record<string, unknown>;
    const id = Number(row.id);
    const price = Number(row.price);
    const durationMinutes = Number(row.durationMinutes);

    if (
      !Number.isFinite(id) ||
      typeof row.slug !== "string" ||
      typeof row.name !== "string" ||
      !Number.isFinite(price) ||
      !Number.isFinite(durationMinutes)
    ) {
      return [];
    }

    return [
      {
        id,
        slug: row.slug,
        name: row.name,
        description: typeof row.description === "string" ? row.description : null,
        durationMinutes,
        price,
        currency: typeof row.currency === "string" ? row.currency : "EUR",
      },
    ];
  });
}

function readProfessionals(value: unknown): ProfessionalRow[] {
  if (!Array.isArray(value)) {
    throw new Error("professionals");
  }

  return value.flatMap((item) => {
    if (!item || typeof item !== "object") return [];

    const row = item as Record<string, unknown>;
    const id = Number(row.id);

    if (!Number.isFinite(id) || typeof row.displayName !== "string") {
      return [];
    }

    return [{ id, displayName: row.displayName }];
  });
}

async function slotsForDay(
  serviceId: number,
  professionals: readonly ProfessionalRow[],
  date: string,
  signal: AbortSignal,
): Promise<{ total: number; slots: HomeSlot[] }> {
  const days = await Promise.all(
    professionals.map(async (professional) => {
      const params = new URLSearchParams({
        professionalId: String(professional.id),
        serviceId: String(serviceId),
        date,
      });
      const payload = await getJson(
        `${apiBase()}/api/v1/public/availability-slots?${params.toString()}`,
        signal,
      );

      if (!payload || typeof payload !== "object") {
        throw new Error("availability");
      }

      const row = payload as Record<string, unknown>;
      const total = Number(row.totalSlots);
      const rawSlots = Array.isArray(row.slots) ? row.slots : [];
      const professionalName =
        typeof row.professionalName === "string"
          ? row.professionalName
          : professional.displayName;

      return {
        total: Number.isFinite(total) ? total : 0,
        slots: rawSlots.flatMap((slot) => {
          if (!slot || typeof slot !== "object") return [];
          const startTime = (slot as Record<string, unknown>).startTime;
          if (typeof startTime !== "string") return [];
          return [{ startTime, professionalName }];
        }),
      };
    }),
  );

  return {
    total: days.reduce((sum, day) => sum + day.total, 0),
    slots: days
      .flatMap((day) => day.slots)
      .sort((left, right) => left.startTime.localeCompare(right.startTime))
      .slice(0, 3),
  };
}

async function load(signal: AbortSignal): Promise<HomeAvailability> {
  const catalog = readServices(
    await getJson(
      `${apiBase()}/api/v1/public/catalog/${encodeURIComponent(business.slug)}/services`,
      signal,
    ),
  );

  const featuredHome = business.services.find(
    (service) => service.featured && service.bookingSlug,
  );
  const featured = featuredHome?.bookingSlug
    ? catalog.find((service) => service.slug === featuredHome.bookingSlug)
    : undefined;

  if (!featured) {
    return { status: "ready", catalog, featured: null };
  }

  const professionals = readProfessionals(
    await getJson(
      `${apiBase()}/api/v1/public/catalog/${encodeURIComponent(business.slug)}/services/${featured.id}/professionals`,
      signal,
    ),
  );

  if (professionals.length === 0) {
    return {
      status: "ready",
      catalog,
      featured: {
        serviceName: featured.name,
        serviceSlug: featured.slug,
        todayTotal: 0,
        dayLabel: "hoy",
        slots: [],
      },
    };
  }

  const today = await slotsForDay(featured.id, professionals, todayIso(), signal);

  if (today.total > 0) {
    return {
      status: "ready",
      catalog,
      featured: {
        serviceName: featured.name,
        serviceSlug: featured.slug,
        todayTotal: today.total,
        dayLabel: "hoy",
        slots: today.slots,
      },
    };
  }

  const tomorrow = await slotsForDay(
    featured.id,
    professionals,
    addDays(todayIso(), 1),
    signal,
  );

  return {
    status: "ready",
    catalog,
    featured: {
      serviceName: featured.name,
      serviceSlug: featured.slug,
      todayTotal: 0,
      dayLabel: tomorrow.slots.length > 0 ? "mañana" : "hoy",
      slots: tomorrow.slots,
    },
  };
}

let pending: Promise<HomeAvailability> | null = null;

export function loadHomeAvailability(): Promise<HomeAvailability> {
  if (!pending) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);
    pending = load(controller.signal)
      .catch(() => ({ status: "hidden" as const }))
      .finally(() => clearTimeout(timer));
  }

  return pending;
}

export function useHomeAvailability(): HomeAvailability | null {
  const [value, setValue] = useState<HomeAvailability | null>(null);

  useEffect(() => {
    let cancelled = false;

    void loadHomeAvailability().then((result) => {
      if (!cancelled) setValue(result);
    });

    return () => {
      cancelled = true;
    };
  }, []);

  return value;
}

export function formatPublicPrice(price: number, currency: string): string | null {
  if (!Number.isFinite(price)) return null;

  try {
    return new Intl.NumberFormat("es-ES", {
      style: "currency",
      currency: currency || "EUR",
      maximumFractionDigits: Number.isInteger(price) ? 0 : 2,
    }).format(price);
  } catch {
    return null;
  }
}

export function formatSlotTime(value: string): string {
  return value.slice(0, 5);
}

export function slotCountLabel(serviceName: string, total: number): string {
  if (total === 0) return `Hoy no quedan huecos para ${serviceName}`;
  if (total === 1) return `Hoy queda 1 hueco para ${serviceName}`;
  return `Hoy quedan ${total} huecos para ${serviceName}`;
}
