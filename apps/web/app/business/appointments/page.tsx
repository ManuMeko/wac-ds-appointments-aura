"use client";

import Link from "next/link";
import WacCalendar from "@/components/ui/WacCalendar";

import {
  useCallback,
  type KeyboardEvent,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
} from "react";

import {
  ApiError,
  apiGet,
  apiPost,
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

type CreatableAppointmentStatus =
  | "PENDING"
  | "CONFIRMED";

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

type AppointmentCreateRequest = {
  customerId: number;
  professionalId: number;
  serviceId: number;
  startAt: string;
  status: CreatableAppointmentStatus;
  notes: string | null;
};

type FilterValue =
  | "all"
  | string;

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
   TYPE GUARDS
========================================================= */

function isCreatableAppointmentStatus(
  value: string,
): value is CreatableAppointmentStatus {
  return (
    value === "PENDING"
    || value === "CONFIRMED"
  );
}

/* =========================================================
   STATUS
========================================================= */

function getStatusClass(
  status: AppointmentStatus,
): string {
  switch (
    status
  ) {
    case "CONFIRMED":
      return "bg-[#edf4ee] text-[#55735c] border-[#dce9de]";

    case "PENDING":
      return "bg-[#f8f0e5] text-[#947348] border-[#eadcc8]";

    case "COMPLETED":
      return "bg-[#edf1f5] text-[#607283] border-[#dde4ea]";

    case "CANCELLED":
      return "bg-[#f7eaea] text-[#9a5f5f] border-[#ecd4d4]";

    case "NO_SHOW":
      return "bg-[#f1eeee] text-[#756b6b] border-[#e3dddd]";
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
   DATE HELPERS
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

function parseDateKey(
  value: string,
): Date {
  const [
    year,
    month,
    day,
  ] =
    value
      .split("-")
      .map(
        Number,
      );

  return new Date(
    year,
    month - 1,
    day,
    12,
    0,
    0,
    0,
  );
}

function formatDateKey(
  date: Date,
): string {
  const year =
    date.getFullYear();

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

function addDays(
  value: string,
  amount: number,
): string {
  const date =
    parseDateKey(
      value,
    );

  date.setDate(
    date.getDate() + amount,
  );

  return formatDateKey(
    date,
  );
}

function formatSelectedDate(
  value: string,
): string {
  const date =
    parseDateKey(
      value,
    );

  const formatted =
    new Intl.DateTimeFormat(
      "es-ES",
      {
        weekday:
          "long",

        day:
          "numeric",

        month:
          "long",

        year:
          "numeric",
      },
    ).format(
      date,
    );

  return (
    formatted.charAt(0).toUpperCase()
    + formatted.slice(1)
  );
}

function formatCalendarWeekday(
  value: string,
): string {
  return new Intl.DateTimeFormat(
    "es-ES",
    {
      weekday:
        "short",
    },
  )
    .format(
      parseDateKey(
        value,
      ),
    )
    .replace(
      ".",
      "",
    )
    .toUpperCase();
}

function formatCalendarDay(
  value: string,
): string {
  return new Intl.DateTimeFormat(
    "es-ES",
    {
      day:
        "2-digit",
    },
  ).format(
    parseDateKey(
      value,
    ),
  );
}

function formatCalendarMonth(
  value: string,
): string {
  return new Intl.DateTimeFormat(
    "es-ES",
    {
      month:
        "short",
    },
  )
    .format(
      parseDateKey(
        value,
      ),
    )
    .replace(
      ".",
      "",
    )
    .toUpperCase();
}

function formatCalendarMonthYear(
  value: string,
): string {
  if (
    !value
  ) {
    return "";
  }

  const result =
    new Intl.DateTimeFormat(
      "es-ES",
      {
        month:
          "long",

        year:
          "numeric",
      },
    ).format(
      parseDateKey(
        value,
      ),
    );

  return (
    result.charAt(0).toUpperCase()
    + result.slice(1)
  );
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
   FORMAT
========================================================= */

function formatPrice(
  value: number | null,
): string | null {
  if (
    value === null
    || value === undefined
  ) {
    return null;
  }

  return new Intl.NumberFormat(
    "es-ES",
    {
      style:
        "currency",

      currency:
        "EUR",
    },
  ).format(
    value,
  );
}

function normalizeSearchValue(
  value:
    | string
    | null
    | undefined,
): string {
  return (
    value
      ?.trim()
      .toLocaleLowerCase(
        "es-ES",
      )
    || ""
  );
}

function resolveApiError(
  error: unknown,
  fallback: string,
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
      || fallback
    );
  }

  return fallback;
}

/* =========================================================
   CSV
========================================================= */

function downloadCsv(
  appointments:
    AppointmentResponse[],
  selectedDate:
    string,
) {
  if (
    typeof window === "undefined"
  ) {
    return;
  }

  const header = [
    "ID",
    "Fecha",
    "Hora inicio",
    "Hora fin",
    "Cliente",
    "Email",
    "Teléfono",
    "Servicio",
    "Precio",
    "Profesional",
    "Estado",
    "Notas",
  ];

  const rows =
    appointments.map(
      (
        appointment,
      ) => [
        appointment.id,
        selectedDate,

        formatAppointmentTime(
          appointment.startAt,
          appointment.timezone,
        ),

        formatAppointmentTime(
          appointment.endAt,
          appointment.timezone,
        ),

        appointment.customerName
        ?? "",

        appointment.customerEmail
        ?? "",

        appointment.customerPhone
        ?? "",

        appointment.serviceName
        ?? "",

        appointment.servicePrice
        ?? "",

        appointment.professionalName
        ?? "",

        statusLabels[
          appointment.status
        ],

        appointment.notes
        ?? "",
      ],
    );

  const csv =
    [
      header,
      ...rows,
    ]
      .map(
        (
          row,
        ) =>
          row
            .map(
              (
                value,
              ) => {
                const stringValue =
                  String(
                    value ?? "",
                  );

                return `"${stringValue.replaceAll(
                  '"',
                  '""',
                )}"`;
              },
            )
            .join(
              ";",
            ),
      )
      .join(
        "\n",
      );

  const blob =
    new Blob(
      [
        "\uFEFF",
        csv,
      ],
      {
        type:
          "text/csv;charset=utf-8;",
      },
    );

  const url =
    URL.createObjectURL(
      blob,
    );

  const anchor =
    document.createElement(
      "a",
    );

  anchor.href =
    url;

  anchor.download =
    `citas-${selectedDate}.csv`;

  document.body.appendChild(
    anchor,
  );

  anchor.click();

  anchor.remove();

  URL.revokeObjectURL(
    url,
  );
}


/* =========================================================
   PREMIUM BUSINESS FORM COMPONENTS
   Presentational controls; booking rules stay in the page.
========================================================= */

type PremiumOption = {
  value: string;
  label: string;
  description?: string;
};

type PremiumSelectProps = {
  value: string;
  onChange: (value: string) => void;
  options: PremiumOption[];
  placeholder: string;
  ariaLabel: string;
  disabled?: boolean;
  searchable?: boolean;
};

function PremiumSelect({
  value,
  onChange,
  options,
  placeholder,
  ariaLabel,
  disabled = false,
  searchable = false,
}: PremiumSelectProps) {
  const id = useId();
  const rootRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const searchRef = useRef<HTMLInputElement>(null);
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [activeIndex, setActiveIndex] = useState(0);
  const [trackedOpen, setTrackedOpen] = useState(open);

  if (open !== trackedOpen) {
    setTrackedOpen(open);

    if (open) {
      setActiveIndex(0);
    }
  }

  const selected = options.find((option) => option.value === value);
  const filtered = useMemo(() => {
    const normalized = query.trim().toLocaleLowerCase("es-ES");
    return normalized
      ? options.filter((option) =>
          `${option.label} ${option.description ?? ""}`
            .toLocaleLowerCase("es-ES")
            .includes(normalized)
        )
      : options;
  }, [options, query]);

  useEffect(() => {
    if (!open) return;

    function onOutside(event: PointerEvent) {
      if (
        event.target instanceof Node &&
        !rootRef.current?.contains(event.target)
      ) {
        setOpen(false);
        setQuery("");
      }
    }

    document.addEventListener("pointerdown", onOutside);
    return () => document.removeEventListener("pointerdown", onOutside);
  }, [open]);

  useEffect(() => {
    if (!open || !searchable) return;
    searchRef.current?.focus();
  }, [open, searchable]);

  function choose(option: PremiumOption) {
    onChange(option.value);
    setOpen(false);
    setQuery("");
    triggerRef.current?.focus();
  }

  function handleKeys(event: KeyboardEvent<HTMLDivElement>) {
    if (disabled) return;

    if (event.key === "Escape" && open) {
      event.preventDefault();
      event.stopPropagation();
      setOpen(false);
      setQuery("");
      triggerRef.current?.focus();
      return;
    }

    if (!open && (event.key === "ArrowDown" || event.key === "ArrowUp")) {
      event.preventDefault();
      setOpen(true);
      return;
    }

    if (!open) return;

    if (event.key === "ArrowDown" || event.key === "ArrowUp") {
      event.preventDefault();
      setActiveIndex((current) => {
        if (!filtered.length) return 0;
        return event.key === "ArrowDown"
          ? (current + 1) % filtered.length
          : (current - 1 + filtered.length) % filtered.length;
      });
    }

    if (event.key === "Enter" && filtered.length) {
      event.preventDefault();
      choose(filtered[Math.min(activeIndex, filtered.length - 1)]);
    }
  }

  return (
    <div ref={rootRef} className="relative mt-3" onKeyDown={handleKeys}>
      <button
        ref={triggerRef}
        type="button"
        aria-label={ariaLabel}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={open ? id : undefined}
        disabled={disabled}
        onClick={() => {
          setOpen((current) => !current);
          setQuery("");
        }}
        className={`group flex min-h-[58px] w-full items-center justify-between gap-3 rounded-[19px] border px-4 py-3 text-left shadow-[0_5px_16px_rgba(56,44,35,0.035)] outline-none transition-all duration-200 focus-visible:ring-4 focus-visible:ring-[#b9a18e]/20 disabled:cursor-not-allowed disabled:opacity-50 ${
          open
            ? "border-[#90796a] bg-white shadow-[0_12px_32px_rgba(56,44,35,0.12)]"
            : "border-[#e4d9cf] bg-[#fbf9f6] hover:border-[#bba79a] hover:bg-white"
        }`}
      >
        <span className="flex min-w-0 items-center gap-3">
          <span className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-[13px] ${selected ? "bg-[#e8ded5] text-[#58473c]" : "bg-[#eee9e4] text-[#a49386]"}`}>
            {selected ? "✓" : "·"}
          </span>
          <span className="min-w-0">
            <span className={`block truncate text-[13px] font-semibold sm:text-sm ${selected ? "text-[#342c26]" : "text-[#8c7c71]"}`}>
              {selected?.label ?? placeholder}
            </span>
            {selected?.description && (
              <span className="mt-0.5 block truncate text-[11px] text-[#958479]">
                {selected.description}
              </span>
            )}
          </span>
        </span>
        <svg
          aria-hidden="true"
          viewBox="0 0 20 20"
          fill="none"
          className={`h-5 w-5 shrink-0 text-[#8e7b6d] transition-transform duration-200 ${open ? "rotate-180" : ""}`}
        >
          <path d="m5 7.5 5 5 5-5" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </button>

      {open && !disabled && (
        <div
          id={id}
          role="listbox"
          aria-label={ariaLabel}
          className="absolute left-0 right-0 top-[calc(100%+8px)] z-[90] overflow-hidden rounded-[20px] border border-[#e2d7cc] bg-white p-2 shadow-[0_22px_55px_rgba(49,37,28,0.20)]"
        >
          {searchable && (
            <div className="p-1 pb-2">
              <input
                ref={searchRef}
                type="search"
                value={query}
                onChange={(event) => {
                  setQuery(event.target.value);
                  setActiveIndex(0);
                }}
                placeholder="Buscar..."
                aria-label={`Buscar ${ariaLabel.toLowerCase()}`}
                className="h-11 w-full rounded-[13px] border border-[#e5dbd1] bg-[#faf8f5] px-3 text-sm text-[#342c26] outline-none transition focus:border-[#a99281]"
              />
            </div>
          )}

          <div className="max-h-60 space-y-1 overflow-y-auto overscroll-contain">
            {filtered.length === 0 ? (
              <div className="px-4 py-6 text-center text-sm text-[#938277]">
                Sin resultados
              </div>
            ) : (
              filtered.map((option, index) => {
                const isSelected = value === option.value;
                return (
                  <button
                    key={option.value}
                    type="button"
                    role="option"
                    aria-selected={isSelected}
                    onMouseEnter={() => setActiveIndex(index)}
                    onClick={() => choose(option)}
                    className={`flex w-full items-center justify-between gap-3 rounded-[13px] px-3 py-3 text-left transition ${
                      isSelected
                        ? "bg-[#eee7df] text-[#342c26]"
                        : index === activeIndex
                          ? "bg-[#f6f2ee] text-[#342c26]"
                          : "text-[#67594f] hover:bg-[#f8f5f2]"
                    }`}
                  >
                    <span className="min-w-0">
                      <span className="block truncate text-sm font-semibold">{option.label}</span>
                      {option.description && (
                        <span className="mt-1 block truncate text-[11px] text-[#98877b]">
                          {option.description}
                        </span>
                      )}
                    </span>
                    {isSelected && <span className="text-[#705a4c]" aria-hidden="true">✓</span>}
                  </button>
                );
              })
            )}
          </div>
        </div>
      )}
    </div>
  );
}

type StepHeadingProps = {
  number: number;
  title: string;
  description?: string;
};

function StepHeading({ number, title, description }: StepHeadingProps) {
  return (
    <div className="flex items-start gap-3.5">
      <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-[14px] bg-[#342d28] text-[14px] font-bold text-white shadow-[0_5px_14px_rgba(52,45,40,0.17)]">
        {String(number).padStart(2, "0")}
      </span>
      <div className="min-w-0 pt-0.5">
        <h3 className="text-[17px] font-semibold leading-6 tracking-[-0.025em] text-[#342d28]">
          {title}
        </h3>
        {description && (
          <p className="mt-1 text-xs leading-5 text-[#8a7a6e]">{description}</p>
        )}
      </div>
    </div>
  );
}

/* =========================================================
   COMPONENT
========================================================= */

export default function BusinessAppointmentsPage() {

  /* =======================================================
     AGENDA
  ======================================================= */

  const [
    selectedDate,
    setSelectedDate,
  ] =
    useState<string>(
      getTodayDateKey,
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
     FILTERS
  ======================================================= */

  const [
    search,
    setSearch,
  ] =
    useState(
      "",
    );

  const [
    professionalFilter,
    setProfessionalFilter,
  ] =
    useState<FilterValue>(
      "all",
    );

  const [
    serviceFilter,
    setServiceFilter,
  ] =
    useState<FilterValue>(
      "all",
    );

  const [
    statusFilter,
    setStatusFilter,
  ] =
    useState<FilterValue>(
      "all",
    );

  /* =======================================================
     CREATE MODAL
  ======================================================= */

  const [
    createOpen,
    setCreateOpen,
  ] =
    useState(
      false,
    );

  const [
    createResourcesLoading,
    setCreateResourcesLoading,
  ] =
    useState(
      false,
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
    professionalsData,
    setProfessionalsData,
  ] =
    useState<
      ProfessionalResponse[]
    >(
      [],
    );

  const [
    servicesData,
    setServicesData,
  ] =
    useState<
      ServiceResponse[]
    >(
      [],
    );

  const [
    createCustomerId,
    setCreateCustomerId,
  ] =
    useState<
      number | null
    >(
      null,
    );

  const [
    createProfessionalId,
    setCreateProfessionalId,
  ] =
    useState<
      number | null
    >(
      null,
    );

  const [
    createServiceId,
    setCreateServiceId,
  ] =
    useState<
      number | null
    >(
      null,
    );

  const [
    createDate,
    setCreateDate,
  ] =
    useState(
      getTodayDateKey,
    );

  const [
    createCalendarStart,
    setCreateCalendarStart,
  ] =
    useState(
      getTodayDateKey,
    );

  const [
    createStatus,
    setCreateStatus,
  ] =
    useState<
      CreatableAppointmentStatus
    >(
      "PENDING",
    );

  const [
    createNotes,
    setCreateNotes,
  ] =
    useState(
      "",
    );

  const [
    createAvailability,
    setCreateAvailability,
  ] =
    useState<
      DailyAvailabilityResponse
      | null
    >(
      null,
    );

  const [
    createAvailabilityLoading,
    setCreateAvailabilityLoading,
  ] =
    useState(
      false,
    );

  const [
    createSelectedSlot,
    setCreateSelectedSlot,
  ] =
    useState<
      AvailabilitySlotResponse
      | null
    >(
      null,
    );

  const [
    createError,
    setCreateError,
  ] =
    useState<
      string | null
    >(
      null,
    );

  const [
    creating,
    setCreating,
  ] =
    useState(
      false,
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
     LOAD AGENDA
  ======================================================= */

  const loadAppointments =
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

        const accessToken =
          getStoredAccessToken();

        if (
          !accessToken
        ) {
          setAppointments(
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
          const response =
            await apiGet<
              AppointmentResponse[]
            >(
              `/api/v1/appointments/day?date=${encodeURIComponent(
                selectedDate,
              )}`,
              {
                accessToken,
              },
            );

          setAppointments(
            Array.isArray(
              response,
            )
              ? response
              : [],
          );
        } catch (
          requestError
        ) {
          setAppointments(
            [],
          );

          if (
            requestError
            instanceof ApiError
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
              "No se ha podido conectar con la API de citas.",
            ),
          );
        } finally {
          setLoading(
            false,
          );
        }
      },
      [
        selectedDate,
      ],
    );

  useEffect(
    () => {
      void Promise.resolve().then(
        () => {
          void loadAppointments();
        },
      );
    },
    [
      loadAppointments,
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
     MODAL UX
  ======================================================= */

  useEffect(
    () => {
      if (
        !createOpen
      ) {
        return;
      }

      const previousOverflow =
        document.body.style.overflow;

      document.body.style.overflow =
        "hidden";

      function handleKeyDown(event: globalThis.KeyboardEvent) {
        if (
          event.key === "Escape"
          && !creating
        ) {
          setCreateOpen(
            false,
          );

          setCreateAvailability(
            null,
          );

          setCreateSelectedSlot(
            null,
          );

          setCreateError(
            null,
          );
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
      createOpen,
      creating,
    ],
  );

  /* =======================================================
     CREATE AVAILABILITY
  ======================================================= */

  useEffect(
    () => {
      let cancelled =
        false;

      void Promise.resolve().then(
        () => {
          if (
            !createOpen
            || createProfessionalId == null
            || createServiceId == null
            || !createDate
          ) {
            setCreateAvailability(
              null,
            );

            setCreateSelectedSlot(
              null,
            );

            return;
          }

          void loadAvailability();
        },
      );

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

        setCreateAvailabilityLoading(
          true,
        );

        setCreateAvailability(
          null,
        );

        setCreateSelectedSlot(
          null,
        );

        setCreateError(
          null,
        );

        try {
          const query =
            new URLSearchParams(
              {
                professionalId:
                  String(
                    createProfessionalId,
                  ),

                serviceId:
                  String(
                    createServiceId,
                  ),

                date:
                  createDate,
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

          setCreateAvailability(
            response,
          );
        } catch (
          requestError
        ) {
          if (
            cancelled
          ) {
            return;
          }

          if (
            requestError
            instanceof ApiError
            && (
              requestError.status === 401
              || requestError.status === 403
            )
          ) {
            setMissingSession(
              true,
            );
          }

          setCreateError(
            resolveApiError(
              requestError,
              "No se ha podido consultar la disponibilidad.",
            ),
          );
        } finally {
          if (
            !cancelled
          ) {
            setCreateAvailabilityLoading(
              false,
            );
          }
        }
      }

      return () => {
        cancelled =
          true;
      };
    },
    [
      createDate,
      createOpen,
      createProfessionalId,
      createServiceId,
    ],
  );

  /* =======================================================
     AGENDA FILTER OPTIONS
  ======================================================= */

  const professionals =
    useMemo(
      () => {
        const map =
          new Map<
            number,
            string
          >();

        appointments.forEach(
          (
            appointment,
          ) => {
            if (
              appointment.professionalId
              != null
              && appointment.professionalName
            ) {
              map.set(
                appointment.professionalId,
                appointment.professionalName,
              );
            }
          },
        );

        return Array.from(
          map.entries(),
        )
          .map(
            (
              [
                id,
                name,
              ],
            ) => ({
              id,
              name,
            }),
          )
          .sort(
            (
              first,
              second,
            ) =>
              first.name.localeCompare(
                second.name,
                "es",
              ),
          );
      },
      [
        appointments,
      ],
    );

  const services =
    useMemo(
      () => {
        const map =
          new Map<
            number,
            string
          >();

        appointments.forEach(
          (
            appointment,
          ) => {
            if (
              appointment.serviceId
              != null
              && appointment.serviceName
            ) {
              map.set(
                appointment.serviceId,
                appointment.serviceName,
              );
            }
          },
        );

        return Array.from(
          map.entries(),
        )
          .map(
            (
              [
                id,
                name,
              ],
            ) => ({
              id,
              name,
            }),
          )
          .sort(
            (
              first,
              second,
            ) =>
              first.name.localeCompare(
                second.name,
                "es",
              ),
          );
      },
      [
        appointments,
      ],
    );

  /* =======================================================
     FILTERED APPOINTMENTS
  ======================================================= */

  const filteredAppointments =
    useMemo(
      () => {
        const normalizedSearch =
          normalizeSearchValue(
            search,
          );

        return appointments.filter(
          (
            appointment,
          ) => {
            if (
              statusFilter
              !== "all"
              && appointment.status
              !== statusFilter
            ) {
              return false;
            }

            if (
              professionalFilter
              !== "all"
              && String(
                appointment.professionalId,
              )
              !== professionalFilter
            ) {
              return false;
            }

            if (
              serviceFilter
              !== "all"
              && String(
                appointment.serviceId,
              )
              !== serviceFilter
            ) {
              return false;
            }

            if (
              normalizedSearch
              === ""
            ) {
              return true;
            }

            const searchableValues = [
              appointment.customerName,
              appointment.customerEmail,
              appointment.customerPhone,
              appointment.serviceName,
              appointment.professionalName,
              appointment.notes,
            ];

            return searchableValues.some(
              (
                value,
              ) =>
                normalizeSearchValue(
                  value,
                ).includes(
                  normalizedSearch,
                ),
            );
          },
        );
      },
      [
        appointments,
        professionalFilter,
        search,
        serviceFilter,
        statusFilter,
      ],
    );

  /* =======================================================
     TOTALS
  ======================================================= */

  const totals =
    useMemo(
      () => ({
        total:
          appointments.length,

        confirmed:
          appointments.filter(
            (
              appointment,
            ) =>
              appointment.status
              === "CONFIRMED",
          ).length,

        pending:
          appointments.filter(
            (
              appointment,
            ) =>
              appointment.status
              === "PENDING",
          ).length,

        cancelled:
          appointments.filter(
            (
              appointment,
            ) =>
              appointment.status
              === "CANCELLED",
          ).length,
      }),
      [
        appointments,
      ],
    );

  /* =======================================================
     CREATE COMPUTED
  ======================================================= */

  const bookableServices =
    useMemo(
      () =>
        servicesData
          .filter(
            (
              service,
            ) =>
              service.active
              && service.bookingEnabled,
          )
          .sort(
            (
              first,
              second,
            ) =>
              first.name.localeCompare(
                second.name,
                "es",
              ),
          ),
      [
        servicesData,
      ],
    );

  const bookableProfessionals =
    useMemo(
      () =>
        professionalsData
          .filter(
            (
              professional,
            ) =>
              professional.active
              && professional.bookingEnabled,
          )
          .sort(
            (
              first,
              second,
            ) =>
              (
                first.displayName
                || `${first.firstName ?? ""} ${first.lastName ?? ""}`.trim()
              ).localeCompare(
                second.displayName
                || `${second.firstName ?? ""} ${second.lastName ?? ""}`.trim(),
                "es",
              ),
          ),
      [
        professionalsData,
      ],
    );

  const activeCustomers =
    useMemo(
      () =>
        customers
          .filter(
            (
              customer,
            ) =>
              customer.active,
          )
          .sort(
            (
              first,
              second,
            ) =>
              (
                first.fullName
                || `${first.firstName ?? ""} ${first.lastName ?? ""}`.trim()
              ).localeCompare(
                second.fullName
                || `${second.firstName ?? ""} ${second.lastName ?? ""}`.trim(),
                "es",
              ),
          ),
      [
        customers,
      ],
    );

  const selectedCreateCustomer =
    useMemo(
      () =>
        activeCustomers.find(
          (
            customer,
          ) =>
            customer.id
            === createCustomerId,
        )
        ?? null,
      [
        activeCustomers,
        createCustomerId,
      ],
    );

  const selectedCreateProfessional =
    useMemo(
      () =>
        bookableProfessionals.find(
          (
            professional,
          ) =>
            professional.id
            === createProfessionalId,
        )
        ?? null,
      [
        bookableProfessionals,
        createProfessionalId,
      ],
    );

  const selectedCreateService =
    useMemo(
      () =>
        bookableServices.find(
          (
            service,
          ) =>
            service.id
            === createServiceId,
        )
        ?? null,
      [
        bookableServices,
        createServiceId,
      ],
    );

  const createCalendarDays =
    useMemo(
      () =>
        Array.from(
          {
            length:
              7,
          },
          (
            _,
            index,
          ) =>
            addDays(
              createCalendarStart,
              index,
            ),
        ),
      [
        createCalendarStart,
      ],
    );

  const canCreateAppointment =
    Boolean(
      createCustomerId != null
      && createProfessionalId != null
      && createServiceId != null
      && createSelectedSlot
      && !creating,
    );

  /* =======================================================
     FILTER ACTIONS
  ======================================================= */

  function clearFilters() {
    setSearch(
      "",
    );

    setProfessionalFilter(
      "all",
    );

    setServiceFilter(
      "all",
    );

    setStatusFilter(
      "all",
    );
  }

  function goToPreviousDay() {
    setSelectedDate(
      (
        current,
      ) =>
        addDays(
          current,
          -1,
        ),
    );
  }

  function goToNextDay() {
    setSelectedDate(
      (
        current,
      ) =>
        addDays(
          current,
          1,
        ),
    );
  }

  function goToToday() {
    setSelectedDate(
      getTodayDateKey(),
    );
  }

  /* =======================================================
     OPEN CREATE
  ======================================================= */

  async function openCreateModal() {
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

    const initialDate =
      selectedDate < getTodayDateKey()
        ? getTodayDateKey()
        : selectedDate;

    setCreateCustomerId(
      null,
    );

    setCreateProfessionalId(
      null,
    );

    setCreateServiceId(
      null,
    );

    setCreateDate(
      initialDate,
    );

    setCreateCalendarStart(
      initialDate,
    );

    setCreateStatus(
      "PENDING",
    );

    setCreateNotes(
      "",
    );

    setCreateAvailability(
      null,
    );

    setCreateSelectedSlot(
      null,
    );

    setCreateError(
      null,
    );

    setCreateOpen(
      true,
    );

    setCreateResourcesLoading(
      true,
    );

    try {
      const [
        customerResponse,
        professionalResponse,
        serviceResponse,
      ] =
        await Promise.all(
          [
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

      setCustomers(
        Array.isArray(
          customerResponse,
        )
          ? customerResponse
          : [],
      );

      setProfessionalsData(
        Array.isArray(
          professionalResponse,
        )
          ? professionalResponse
          : [],
      );

      setServicesData(
        Array.isArray(
          serviceResponse,
        )
          ? serviceResponse
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

      setCreateError(
        resolveApiError(
          requestError,
          "No se han podido cargar los datos necesarios para crear la cita.",
        ),
      );
    } finally {
      setCreateResourcesLoading(
        false,
      );
    }
  }

  /* =======================================================
     CLOSE CREATE
  ======================================================= */

  function closeCreateModal() {
    if (
      creating
    ) {
      return;
    }

    setCreateOpen(
      false,
    );

    setCreateAvailability(
      null,
    );

    setCreateSelectedSlot(
      null,
    );

    setCreateError(
      null,
    );
  }

  /* =======================================================
     CREATE CALENDAR
  ======================================================= */

  function selectCreateDate(
    value: string,
  ) {
    if (
      value < getTodayDateKey()
      || creating
    ) {
      return;
    }

    setCreateDate(
      value,
    );

    setCreateSelectedSlot(
      null,
    );

    setCreateError(
      null,
    );
  }

  function moveCreateCalendar(
    amount: number,
  ) {
    if (
      creating
    ) {
      return;
    }

    setCreateCalendarStart(
      (
        current,
      ) =>
        addDays(
          current,
          amount,
        ),
    );
  }

  function goCreateCalendarToday() {
    if (
      creating
    ) {
      return;
    }

    const today =
      getTodayDateKey();

    setCreateCalendarStart(
      today,
    );

    setCreateDate(
      today,
    );

    setCreateSelectedSlot(
      null,
    );

    setCreateError(
      null,
    );
  }

  /* =======================================================
     CREATE APPOINTMENT
  ======================================================= */

  async function createAppointment() {
    if (
      createCustomerId == null
      || createProfessionalId == null
      || createServiceId == null
      || !createSelectedSlot
      || creating
    ) {
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

    setCreating(
      true,
    );

    setCreateError(
      null,
    );

    try {
      const trimmedNotes =
        createNotes.trim();

      const requestBody: AppointmentCreateRequest = {
        customerId:
          createCustomerId,

        professionalId:
          createProfessionalId,

        serviceId:
          createServiceId,

        startAt:
          createSelectedSlot.startAt,

        status:
          createStatus,

        notes:
          trimmedNotes === ""
            ? null
            : trimmedNotes,
      };

      const createdAppointment =
        await apiPost<
          AppointmentResponse
        >(
          "/api/v1/appointments",
          requestBody,
          {
            accessToken,
          },
        );

      setCreateOpen(
        false,
      );

      setCreateAvailability(
        null,
      );

      setCreateSelectedSlot(
        null,
      );

      setSuccessMessage(
        `Cita creada para ${
          createdAppointment.customerName
          || "el cliente"
        } el ${formatSelectedDate(
          createDate,
        )} a las ${formatAppointmentTime(
          createdAppointment.startAt,
          createdAppointment.timezone,
        )}.`,
      );

      if (
        selectedDate
        === createDate
      ) {
        await loadAppointments();
      } else {
        setSelectedDate(
          createDate,
        );
      }
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

      setCreateError(
        resolveApiError(
          requestError,
          "No se ha podido crear la cita.",
        ),
      );
    } finally {
      setCreating(
        false,
      );
    }
  }

  /* =======================================================
     RENDER
  ======================================================= */

  return (
    <>
      <div className="space-y-7 pb-10">

        {/* ===================================================
            PAGE HEADER
        =================================================== */}

        <section className="flex flex-col gap-5 xl:flex-row xl:items-end xl:justify-between">
          <div>
            <div className="text-xs font-semibold uppercase tracking-[0.22em] text-[#a18c7f]">
              Agenda
            </div>

            <h1 className="mt-2 text-3xl font-semibold tracking-[-0.03em] text-[#2f2925] md:text-4xl">
              Citas
            </h1>

            <p className="mt-3 max-w-2xl text-sm leading-6 text-[#796b62] md:text-base">
              Consulta, filtra y gestiona las reservas reales del negocio desde un
              único lugar.
            </p>
          </div>

          <div className="flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() =>
                downloadCsv(
                  filteredAppointments,
                  selectedDate,
                )
              }
              disabled={
                filteredAppointments.length
                === 0
              }
              className="inline-flex items-center justify-center rounded-full border border-[#d8cdc4] bg-white px-5 py-3 text-sm font-semibold text-[#5e5149] transition hover:bg-[#f8f4f0] disabled:cursor-not-allowed disabled:opacity-40"
            >
              Exportar
            </button>

            <button
              type="button"
              onClick={() =>
                void openCreateModal()
              }
              disabled={
                missingSession
              }
              className="inline-flex items-center justify-center gap-2 rounded-full bg-[#332c27] px-5 py-3 text-sm font-semibold text-white shadow-[0_8px_24px_rgba(51,44,39,0.16)] transition hover:bg-[#241f1c] disabled:cursor-not-allowed disabled:opacity-45"
            >
              <span className="text-base">
                +
              </span>

              <span>
                Nueva cita
              </span>
            </button>
          </div>
        </section>

        {/* ===================================================
            SUCCESS
        =================================================== */}

        {successMessage && (
          <div className="fixed right-5 top-[92px] z-[100] max-w-sm rounded-[20px] border border-[#d6e5d8] bg-white px-5 py-4 shadow-[0_18px_55px_rgba(53,69,56,0.16)]">
            <div className="flex items-start gap-3">
              <div className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-[#eaf4ec] text-sm font-bold text-[#55735c]">
                ✓
              </div>

              <div>
                <div className="text-sm font-semibold text-[#405946]">
                  Cita creada
                </div>

                <p className="mt-1 text-xs leading-5 text-[#6d7d70]">
                  {successMessage}
                </p>
              </div>
            </div>
          </div>
        )}

        {/* ===================================================
            TOTALS
        =================================================== */}

        <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <article className="bp-metric rounded-[22px] border border-[#e5ddd5] bg-white p-5 shadow-[0_10px_28px_rgba(62,49,41,0.04)]">
            <div className="text-sm text-[#7d7067]">
              Citas del día
            </div>

            <div className="mt-3 text-3xl font-semibold tracking-[-0.04em] text-[#302a26]">
              {totals.total}
            </div>
          </article>

          <article className="bp-metric rounded-[22px] border border-[#e5ddd5] bg-white p-5 shadow-[0_10px_28px_rgba(62,49,41,0.04)]">
            <div className="text-sm text-[#7d7067]">
              Confirmadas
            </div>

            <div className="mt-3 text-3xl font-semibold tracking-[-0.04em] text-[#302a26]">
              {totals.confirmed}
            </div>
          </article>

          <article className="bp-metric rounded-[22px] border border-[#e5ddd5] bg-white p-5 shadow-[0_10px_28px_rgba(62,49,41,0.04)]">
            <div className="text-sm text-[#7d7067]">
              Pendientes
            </div>

            <div className="mt-3 text-3xl font-semibold tracking-[-0.04em] text-[#302a26]">
              {totals.pending}
            </div>
          </article>

          <article className="bp-metric rounded-[22px] border border-[#e5ddd5] bg-white p-5 shadow-[0_10px_28px_rgba(62,49,41,0.04)]">
            <div className="text-sm text-[#7d7067]">
              Canceladas
            </div>

            <div className="mt-3 text-3xl font-semibold tracking-[-0.04em] text-[#302a26]">
              {totals.cancelled}
            </div>
          </article>
        </section>

        {/* ===================================================
            FILTERS
        =================================================== */}

        <section className="rounded-[26px] border border-[#e3dbd3] bg-white p-5 shadow-[0_10px_35px_rgba(62,49,41,0.04)] md:p-6">
          <div className="grid gap-4 xl:grid-cols-[1.3fr_1fr_1fr_1fr_auto]">
            <div>
              <label
                htmlFor="search"
                className="mb-2 block text-xs font-semibold uppercase tracking-[0.12em] text-[#96857a]"
              >
                Buscar
              </label>

              <input
                id="search"
                type="text"
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
                placeholder="Cliente, email, teléfono..."
                className="h-12 w-full rounded-2xl border border-[#e2dad3] bg-[#faf8f6] px-4 text-sm text-[#342e2a] outline-none transition placeholder:text-[#afa198] focus:border-[#b9a79a] focus:bg-white"
              />
            </div>

            <div>
              <label
                htmlFor="professional"
                className="mb-2 block text-xs font-semibold uppercase tracking-[0.12em] text-[#96857a]"
              >
                Profesional
              </label>

              <select
                id="professional"
                value={
                  professionalFilter
                }
                onChange={
                  (
                    event,
                  ) =>
                    setProfessionalFilter(
                      event.target.value,
                    )
                }
                className="h-12 w-full rounded-2xl border border-[#e2dad3] bg-[#faf8f6] px-4 text-sm text-[#554941] outline-none transition focus:border-[#b9a79a] focus:bg-white"
              >
                <option value="all">
                  Todos
                </option>

                {professionals.map(
                  (
                    professional,
                  ) => (
                    <option
                      key={
                        professional.id
                      }
                      value={
                        String(
                          professional.id,
                        )
                      }
                    >
                      {
                        professional.name
                      }
                    </option>
                  ),
                )}
              </select>
            </div>

            <div>
              <label
                htmlFor="service"
                className="mb-2 block text-xs font-semibold uppercase tracking-[0.12em] text-[#96857a]"
              >
                Servicio
              </label>

              <select
                id="service"
                value={
                  serviceFilter
                }
                onChange={
                  (
                    event,
                  ) =>
                    setServiceFilter(
                      event.target.value,
                    )
                }
                className="h-12 w-full rounded-2xl border border-[#e2dad3] bg-[#faf8f6] px-4 text-sm text-[#554941] outline-none transition focus:border-[#b9a79a] focus:bg-white"
              >
                <option value="all">
                  Todos
                </option>

                {services.map(
                  (
                    service,
                  ) => (
                    <option
                      key={
                        service.id
                      }
                      value={
                        String(
                          service.id,
                        )
                      }
                    >
                      {
                        service.name
                      }
                    </option>
                  ),
                )}
              </select>
            </div>

            <div>
              <label
                htmlFor="status"
                className="mb-2 block text-xs font-semibold uppercase tracking-[0.12em] text-[#96857a]"
              >
                Estado
              </label>

              <select
                id="status"
                value={
                  statusFilter
                }
                onChange={
                  (
                    event,
                  ) =>
                    setStatusFilter(
                      event.target.value,
                    )
                }
                className="h-12 w-full rounded-2xl border border-[#e2dad3] bg-[#faf8f6] px-4 text-sm text-[#554941] outline-none transition focus:border-[#b9a79a] focus:bg-white"
              >
                <option value="all">
                  Todos
                </option>

                <option value="PENDING">
                  Pendiente
                </option>

                <option value="CONFIRMED">
                  Confirmada
                </option>

                <option value="COMPLETED">
                  Completada
                </option>

                <option value="CANCELLED">
                  Cancelada
                </option>

                <option value="NO_SHOW">
                  No asistió
                </option>
              </select>
            </div>

            <div className="flex items-end">
              <button
                type="button"
                onClick={
                  clearFilters
                }
                className="h-12 w-full rounded-2xl border border-[#d8cec6] px-5 text-sm font-semibold text-[#62564f] transition hover:bg-[#f7f3ef] xl:w-auto"
              >
                Limpiar
              </button>
            </div>
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
              Necesitas iniciar sesión como OWNER, ADMIN o STAFF para consultar
              y gestionar la agenda.
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
                No se han podido cargar las citas
              </div>

              <div className="mt-1 text-sm text-[#946f6f]">
                {error}
              </div>
            </div>

            <button
              type="button"
              onClick={() =>
                void loadAppointments()
              }
              className="w-fit rounded-full border border-[#ddc5c5] bg-white px-4 py-2 text-xs font-semibold text-[#7e5858] transition hover:bg-[#fffafa]"
            >
              Reintentar
            </button>
          </section>
        )}

        {/* ===================================================
            APPOINTMENTS
        =================================================== */}

        <section className="rounded-[26px] border border-[#e3dbd3] bg-white shadow-[0_10px_35px_rgba(62,49,41,0.04)]">
          <div className="flex flex-col gap-4 border-b border-[#eee7e1] px-5 py-5 md:flex-row md:items-center md:justify-between md:px-7">
            <div>
              <h2 className="text-lg font-semibold text-[#312b27]">
                {formatSelectedDate(
                  selectedDate,
                )}
              </h2>

              <p className="mt-1 text-sm text-[#8a7b71]">
                {loading
                  ? "Cargando agenda..."
                  : `${filteredAppointments.length} ${
                    filteredAppointments.length === 1
                      ? "cita visible"
                      : "citas visibles"
                  }`}
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <WacCalendar
  value={selectedDate}
  onChange={setSelectedDate}
  mode="popover"
  theme="aura"
/>

              <button
                type="button"
                onClick={
                  goToPreviousDay
                }
                className="flex h-10 w-10 items-center justify-center rounded-full border border-[#ded5cd] text-lg text-[#675a52] transition hover:bg-[#f8f4f0]"
                aria-label="Día anterior"
              >
                ‹
              </button>

              <button
                type="button"
                onClick={
                  goToToday
                }
                className="rounded-full border border-[#ded5cd] px-4 py-2 text-sm font-semibold text-[#675a52] transition hover:bg-[#f8f4f0]"
              >
                Hoy
              </button>

              <button
                type="button"
                onClick={
                  goToNextDay
                }
                className="flex h-10 w-10 items-center justify-center rounded-full border border-[#ded5cd] text-lg text-[#675a52] transition hover:bg-[#f8f4f0]"
                aria-label="Día siguiente"
              >
                ›
              </button>

              <button
                type="button"
                onClick={() =>
                  void loadAppointments()
                }
                disabled={
                  loading
                }
                className="rounded-full border border-[#ded5cd] px-4 py-2 text-sm font-semibold text-[#675a52] transition hover:bg-[#f8f4f0] disabled:cursor-not-allowed disabled:opacity-50"
              >
                {loading
                  ? "Actualizando..."
                  : "Actualizar"}
              </button>
            </div>
          </div>

          {loading && (
            <div className="px-6 py-16 text-center">
              <div className="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-[#ded4cb] border-t-[#4c4038]" />

              <p className="mt-4 text-sm text-[#8b7c72]">
                Cargando citas reales...
              </p>
            </div>
          )}

          {!loading
          && !missingSession
          && !error
          && filteredAppointments.length === 0
          && (
            <div className="bp-empty px-6 py-16 text-center">
              <div className="text-lg font-semibold text-[#342e2a]">
                No hay citas que mostrar
              </div>

              <p className="mx-auto mt-2 max-w-xl text-sm leading-6 text-[#8b7c72]">
                No existen citas para esta fecha o ninguna coincide con los
                filtros seleccionados.
              </p>

              {appointments.length > 0 ? (
                <button
                  type="button"
                  onClick={
                    clearFilters
                  }
                  className="mt-5 rounded-full border border-[#d8cec6] px-5 py-2.5 text-sm font-semibold text-[#62564f] transition hover:bg-[#f7f3ef]"
                >
                  Limpiar filtros
                </button>
              ) : (
                <button
                  type="button"
                  onClick={() =>
                    void openCreateModal()
                  }
                  className="mt-5 rounded-full bg-[#342d28] px-5 py-2.5 text-sm font-semibold text-white"
                >
                  Nueva cita
                </button>
              )}
            </div>
          )}

          {!loading
          && !missingSession
          && filteredAppointments.length > 0
          && (
            <>
              {/* DESKTOP */}

              <div className="hidden overflow-x-auto lg:block">
                <table className="w-full min-w-[1150px] border-collapse">
                  <thead>
                    <tr className="border-b border-[#eee7e1] bg-[#faf8f6]">
                      <th className="px-6 py-4 text-left text-[11px] font-semibold uppercase tracking-[0.12em] text-[#99887d]">
                        Hora
                      </th>

                      <th className="px-6 py-4 text-left text-[11px] font-semibold uppercase tracking-[0.12em] text-[#99887d]">
                        Cliente
                      </th>

                      <th className="px-6 py-4 text-left text-[11px] font-semibold uppercase tracking-[0.12em] text-[#99887d]">
                        Servicio
                      </th>

                      <th className="px-6 py-4 text-left text-[11px] font-semibold uppercase tracking-[0.12em] text-[#99887d]">
                        Profesional
                      </th>

                      <th className="px-6 py-4 text-left text-[11px] font-semibold uppercase tracking-[0.12em] text-[#99887d]">
                        Estado
                      </th>

                      <th className="px-6 py-4 text-right text-[11px] font-semibold uppercase tracking-[0.12em] text-[#99887d]">
                        Acción
                      </th>
                    </tr>
                  </thead>

                  <tbody>
                    {filteredAppointments.map(
                      (
                        appointment,
                      ) => {
                        const servicePrice =
                          formatPrice(
                            appointment.servicePrice,
                          );

                        return (
                          <tr
                            key={
                              appointment.id
                            }
                            className="border-b border-[#f0eae4] transition last:border-0 hover:bg-[#fcfaf8]"
                          >
                            <td className="px-6 py-5 align-top">
                              <div className="text-sm font-semibold text-[#342e2a]">
                                {formatAppointmentTime(
                                  appointment.startAt,
                                  appointment.timezone,
                                )}
                              </div>

                              <div className="mt-1 text-xs text-[#9b8b80]">
                                hasta{" "}
                                {formatAppointmentTime(
                                  appointment.endAt,
                                  appointment.timezone,
                                )}
                              </div>
                            </td>

                            <td className="px-6 py-5 align-top">
                              <div className="text-sm font-semibold text-[#342e2a]">
                                {appointment.customerName
                                || "Cliente sin nombre"}
                              </div>

                              {appointment.customerEmail && (
                                <div className="mt-1 text-xs text-[#8b7c72]">
                                  {appointment.customerEmail}
                                </div>
                              )}

                              {appointment.customerPhone && (
                                <div className="mt-1 text-xs text-[#a09389]">
                                  {appointment.customerPhone}
                                </div>
                              )}
                            </td>

                            <td className="px-6 py-5 align-top">
                              <div className="text-sm font-medium text-[#62564f]">
                                {appointment.serviceName
                                || "Servicio"}
                              </div>

                              <div className="mt-1 flex flex-wrap gap-x-3 gap-y-1 text-xs text-[#9b8b80]">
                                {appointment.serviceDurationMinutes
                                != null
                                && (
                                  <span>
                                    {
                                      appointment.serviceDurationMinutes
                                    }{" "}
                                    min
                                  </span>
                                )}

                                {servicePrice && (
                                  <span>
                                    {servicePrice}
                                  </span>
                                )}
                              </div>
                            </td>

                            <td className="px-6 py-5 align-top text-sm text-[#62564f]">
                              {appointment.professionalName
                              || "Sin profesional"}
                            </td>

                            <td className="px-6 py-5 align-top">
                              <span
                                className={`inline-flex rounded-full border px-3 py-1 text-xs font-semibold ${getStatusClass(
                                  appointment.status,
                                )}`}
                              >
                                {
                                  statusLabels[
                                    appointment.status
                                  ]
                                }
                              </span>
                            </td>

                            <td className="px-6 py-5 text-right align-top">
                              <Link
                                href={`/business/appointments/${appointment.id}`}
                                className="inline-flex rounded-full border border-[#ddd3cb] px-4 py-2 text-xs font-semibold text-[#61544c] transition hover:bg-[#f7f3ef]"
                              >
                                Ver detalle
                              </Link>
                            </td>
                          </tr>
                        );
                      },
                    )}
                  </tbody>
                </table>
              </div>

              {/* MOBILE */}

              <div className="divide-y divide-[#eee7e1] lg:hidden">
                {filteredAppointments.map(
                  (
                    appointment,
                  ) => {
                    const servicePrice =
                      formatPrice(
                        appointment.servicePrice,
                      );

                    return (
                      <article
                        key={
                          appointment.id
                        }
                        className="p-5"
                      >
                        <div className="flex items-start justify-between gap-4">
                          <div>
                            <div className="text-lg font-semibold text-[#302a26]">
                              {formatAppointmentTime(
                                appointment.startAt,
                                appointment.timezone,
                              )}
                            </div>

                            <div className="mt-1 text-xs text-[#9a8b81]">
                              Hasta las{" "}
                              {formatAppointmentTime(
                                appointment.endAt,
                                appointment.timezone,
                              )}
                            </div>
                          </div>

                          <span
                            className={`inline-flex rounded-full border px-3 py-1 text-xs font-semibold ${getStatusClass(
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

                        <div className="mt-5">
                          <div className="text-base font-semibold text-[#342e2a]">
                            {appointment.customerName
                            || "Cliente sin nombre"}
                          </div>

                          <div className="mt-2 text-sm text-[#75685f]">
                            {appointment.serviceName
                            || "Servicio"}
                          </div>

                          <div className="mt-1 text-sm text-[#8c7e74]">
                            {appointment.professionalName
                            || "Sin profesional"}
                          </div>

                          <div className="mt-2 flex flex-wrap gap-3 text-xs text-[#9a8b81]">
                            {appointment.serviceDurationMinutes
                            != null
                            && (
                              <span>
                                {
                                  appointment.serviceDurationMinutes
                                }{" "}
                                min
                              </span>
                            )}

                            {servicePrice && (
                              <span>
                                {servicePrice}
                              </span>
                            )}
                          </div>
                        </div>

                        {(appointment.customerEmail
                          || appointment.customerPhone)
                        && (
                          <div className="mt-4 border-t border-[#f0ebe6] pt-4">
                            {appointment.customerEmail && (
                              <div className="text-xs text-[#8d7f75]">
                                {
                                  appointment.customerEmail
                                }
                              </div>
                            )}

                            {appointment.customerPhone && (
                              <div className="mt-1 text-xs text-[#9c8f85]">
                                {
                                  appointment.customerPhone
                                }
                              </div>
                            )}
                          </div>
                        )}

                        <Link
                          href={`/business/appointments/${appointment.id}`}
                          className="mt-5 inline-flex rounded-full bg-[#332c27] px-4 py-2 text-xs font-semibold text-white"
                        >
                          Ver detalle
                        </Link>
                      </article>
                    );
                  },
                )}
              </div>
            </>
          )}
        </section>

        {/* ===================================================
            DATA SOURCE
        =================================================== */}

        <section className="flex flex-col gap-3 rounded-[22px] border border-[#dce5dc] bg-[#eff4ef] p-5 text-sm text-[#5e7160] md:flex-row md:items-center md:justify-between">
          <div>
            Fuente de datos
          </div>

          <div className="font-semibold text-[#405343]">
            API Business · PostgreSQL · datos reales
          </div>
        </section>
      </div>

      {/* =====================================================
          CREATE APPOINTMENT MODAL
      ===================================================== */}

      {createOpen && (
        <div
          className="fixed inset-0 z-[80] flex items-center justify-center bg-[#2d2722]/40 p-3 backdrop-blur-[4px] sm:p-4"
          onMouseDown={
            (
              event,
            ) => {
              if (
                event.target === event.currentTarget
                && !creating
              ) {
                closeCreateModal();
              }
            }
          }
        >
          <div className="flex max-h-[94vh] w-full max-w-[1180px] flex-col overflow-hidden rounded-[32px] border border-white/75 bg-[#f8f5f1] shadow-[0_40px_110px_rgba(38,30,25,0.30)]">

            {/* HEADER */}

            <div className="flex items-start justify-between gap-5 border-b border-[#e8dfd7] bg-white px-5 py-5 sm:px-7 md:px-8 md:py-6">
              <div>
                <div className="text-[10px] font-semibold uppercase tracking-[0.22em] text-[#a08a7c]">
                  Gestión de agenda
                </div>

                <h2 className="mt-2 text-2xl font-semibold tracking-[-0.035em] text-[#302a26]">
                  Nueva cita
                </h2>

                <p className="mt-2 max-w-2xl text-sm leading-6 text-[#81736a]">
                  Selecciona cliente, profesional, servicio, fecha y uno de los
                  horarios disponibles.
                </p>
              </div>

              <button
                type="button"
                onClick={
                  closeCreateModal
                }
                disabled={
                  creating
                }
                aria-label="Cerrar"
                className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full border border-[#e1d8d0] bg-[#faf8f6] text-lg text-[#72645b] transition hover:bg-[#f2ece7] disabled:cursor-not-allowed disabled:opacity-50"
              >
                ×
              </button>
            </div>

            {/* BODY */}

            <div className="overflow-y-auto px-5 py-5 sm:px-7 md:px-8 md:py-6">

              {createResourcesLoading
                ? (
                  <div className="py-20 text-center">
                    <div className="mx-auto h-9 w-9 animate-spin rounded-full border-2 border-[#ddd3cb] border-t-[#51463f]" />

                    <p className="mt-4 text-sm text-[#8b7c72]">
                      Preparando nueva cita...
                    </p>
                  </div>
                )
                : (
                  <div className="grid gap-6 lg:grid-cols-[minmax(315px,0.92fr)_minmax(0,1.28fr)]">

                    {/* LEFT */}

                    <div className="space-y-5">

                      {/* CLIENT */}

                      <section className="relative z-0 rounded-[26px] border border-[#e8ded5] bg-white p-5 shadow-[0_8px_28px_rgba(60,45,35,0.045)] transition-shadow focus-within:z-40 focus-within:shadow-[0_14px_38px_rgba(60,45,35,0.09)]">
                        <StepHeading number={1} title="Cliente" description="¿Para quién es la cita?" />

                        <PremiumSelect
                          value={createCustomerId == null ? "" : String(createCustomerId)}
                          onChange={(nextValue) => {
                            const next = Number(nextValue);
                            setCreateCustomerId(Number.isInteger(next) && next > 0 ? next : null);
                            setCreateError(null);
                          }}
                          options={activeCustomers.map((customer) => ({
                            value: String(customer.id),
                            label: customer.fullName || `${customer.firstName ?? ""} ${customer.lastName ?? ""}`.trim() || `Cliente #${customer.id}`,
                            description: customer.email || customer.phone || undefined,
                          }))}
                          placeholder="Selecciona un cliente"
                          ariaLabel="Cliente de la cita"
                          searchable
                          disabled={creating}
                        />

                        {selectedCreateCustomer && (
                          <div className="mt-4 rounded-[17px] bg-[#faf8f6] p-4">
                            <div className="text-sm font-semibold text-[#3f3732]">
                              {selectedCreateCustomer.fullName
                              || `${selectedCreateCustomer.firstName ?? ""} ${selectedCreateCustomer.lastName ?? ""}`.trim()}
                            </div>

                            {selectedCreateCustomer.email && (
                              <div className="mt-1 text-xs text-[#887970]">
                                {selectedCreateCustomer.email}
                              </div>
                            )}

                            {selectedCreateCustomer.phone && (
                              <div className="mt-1 text-xs text-[#9c8d83]">
                                {selectedCreateCustomer.phone}
                              </div>
                            )}
                          </div>
                        )}
                      </section>

                      {/* PROFESSIONAL */}

                      <section className="relative z-0 rounded-[26px] border border-[#e8ded5] bg-white p-5 shadow-[0_8px_28px_rgba(60,45,35,0.045)] transition-shadow focus-within:z-40 focus-within:shadow-[0_14px_38px_rgba(60,45,35,0.09)]">
                        <StepHeading number={2} title="Profesional" description="Elige quién realizará el servicio" />

                        <PremiumSelect
                          value={createProfessionalId == null ? "" : String(createProfessionalId)}
                          onChange={(nextValue) => {
                            const next = Number(nextValue);
                            setCreateProfessionalId(Number.isInteger(next) && next > 0 ? next : null);
                            setCreateSelectedSlot(null);
                            setCreateError(null);
                          }}
                          options={bookableProfessionals.map((professional) => ({
                            value: String(professional.id),
                            label: professional.displayName || `${professional.firstName ?? ""} ${professional.lastName ?? ""}`.trim() || `Profesional #${professional.id}`,
                            description: professional.bio || undefined,
                          }))}
                          placeholder="Selecciona un profesional"
                          ariaLabel="Profesional de la cita"
                          searchable
                          disabled={creating}
                        />

                        {selectedCreateProfessional && (
                          <div className="mt-4 flex items-center justify-between gap-4 rounded-[17px] bg-[#faf8f6] p-4">
                            <div>
                              <div className="text-sm font-semibold text-[#3f3732]">
                                {selectedCreateProfessional.displayName
                                || `${selectedCreateProfessional.firstName ?? ""} ${selectedCreateProfessional.lastName ?? ""}`.trim()}
                              </div>

                              <div className="mt-1 text-xs text-[#8f8076]">
                                Disponible para reservas
                              </div>
                            </div>

                            <span className="h-2 w-2 rounded-full bg-[#719079]" />
                          </div>
                        )}
                      </section>

                      {/* SERVICE */}

                      <section className="relative z-0 rounded-[26px] border border-[#e8ded5] bg-white p-5 shadow-[0_8px_28px_rgba(60,45,35,0.045)] transition-shadow focus-within:z-40 focus-within:shadow-[0_14px_38px_rgba(60,45,35,0.09)]">
                        <StepHeading number={3} title="Servicio" description="Selecciona el tratamiento" />

                        <PremiumSelect
                          value={createServiceId == null ? "" : String(createServiceId)}
                          onChange={(nextValue) => {
                            const next = Number(nextValue);
                            setCreateServiceId(Number.isInteger(next) && next > 0 ? next : null);
                            setCreateSelectedSlot(null);
                            setCreateError(null);
                          }}
                          options={bookableServices.map((service) => ({
                            value: String(service.id),
                            label: service.name,
                            description: `${service.durationMinutes} min · ${formatPrice(service.price) ?? "Sin precio"}`,
                          }))}
                          placeholder="Selecciona un servicio"
                          ariaLabel="Servicio de la cita"
                          searchable
                          disabled={creating}
                        />

                        {selectedCreateService && (
                          <div className="mt-4 flex items-center justify-between gap-4 rounded-[17px] bg-[#faf8f6] p-4">
                            <div>
                              <div className="text-sm font-semibold text-[#3f3732]">
                                {selectedCreateService.name}
                              </div>

                              <div className="mt-1 text-xs text-[#8f8076]">
                                {selectedCreateService.durationMinutes} min
                              </div>
                            </div>

                            <div className="text-sm font-semibold text-[#4a403a]">
                              {formatPrice(
                                selectedCreateService.price,
                              ) || "—"}
                            </div>
                          </div>
                        )}
                      </section>
                    </div>

                    {/* RIGHT */}

                    <div className="space-y-5">

                      {/* DATE */}

                      <section className="rounded-[24px] border border-[#dfd4ca] bg-[#eee7df] p-5 md:p-6">
                        <div className="flex items-start justify-between gap-4">
                          <div>
                            <StepHeading number={4} title="Fecha" description="Escoge el día de la reserva" />
                          </div>

                          <button
                            type="button"
                            onClick={
                              goCreateCalendarToday
                            }
                            disabled={
                              creating
                            }
                            className="rounded-full border border-[#d5c8bd] bg-white/80 px-3 py-1.5 text-[11px] font-semibold text-[#75665d] transition hover:bg-white disabled:opacity-50"
                          >
                            Hoy
                          </button>
                        </div>

                        <div className="mt-5 flex items-center justify-between gap-3">
                          <button
                            type="button"
                            onClick={() =>
                              moveCreateCalendar(
                                -7,
                              )
                            }
                            disabled={
                              creating
                            }
                            aria-label="Semana anterior"
                            className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-[#d6c9be] bg-white text-sm font-semibold text-[#6c5f57] transition hover:bg-[#faf8f6] disabled:opacity-50"
                          >
                            ←
                          </button>

                          <div className="text-center text-sm font-semibold text-[#3c342f]">
                            {formatCalendarMonthYear(
                              createCalendarStart,
                            )}
                          </div>

                          <button
                            type="button"
                            onClick={() =>
                              moveCreateCalendar(
                                7,
                              )
                            }
                            disabled={
                              creating
                            }
                            aria-label="Semana siguiente"
                            className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-[#d6c9be] bg-white text-sm font-semibold text-[#6c5f57] transition hover:bg-[#faf8f6] disabled:opacity-50"
                          >
                            →
                          </button>
                        </div>

                        <div className="mt-4 grid grid-cols-7 gap-1.5">
                          {createCalendarDays.map(
                            (
                              dateValue,
                            ) => {
                              const selected =
                                dateValue
                                === createDate;

                              const today =
                                dateValue
                                === getTodayDateKey();

                              const past =
                                dateValue
                                < getTodayDateKey();

                              return (
                                <button
                                  key={
                                    dateValue
                                  }
                                  type="button"
                                  onClick={() =>
                                    selectCreateDate(
                                      dateValue,
                                    )
                                  }
                                  disabled={
                                    past
                                    || creating
                                  }
                                  className={[
                                    "relative min-w-0 rounded-[15px] border px-1 py-2.5 text-center transition",

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
                                    className={
                                      selected
                                        ? "truncate text-[8px] font-semibold tracking-[0.06em] text-white/70"
                                        : "truncate text-[8px] font-semibold tracking-[0.06em] text-[#9a887b]"
                                    }
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
                                    className={
                                      selected
                                        ? "mt-1 truncate text-[8px] font-semibold text-white/60"
                                        : "mt-1 truncate text-[8px] font-semibold text-[#a49387]"
                                    }
                                  >
                                    {formatCalendarMonth(
                                      dateValue,
                                    )}
                                  </div>

                                  {today && (
                                    <span
                                      className={
                                        selected
                                          ? "absolute bottom-1 left-1/2 h-1 w-1 -translate-x-1/2 rounded-full bg-white"
                                          : "absolute bottom-1 left-1/2 h-1 w-1 -translate-x-1/2 rounded-full bg-[#755f51]"
                                      }
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

                          <div className="mt-1 text-sm font-semibold text-[#443a34]">
                            {formatSelectedDate(
                              createDate,
                            )}
                          </div>
                        </div>
                      </section>

                      {/* AVAILABILITY */}

                      <section className="rounded-[24px] border border-[#e1d8d0] bg-white p-5 md:p-6">
                        <div className="flex flex-col gap-3 border-b border-[#eee7e1] pb-5 sm:flex-row sm:items-center sm:justify-between">
                          <div>
                            <StepHeading number={5} title="Disponibilidad" description="Horarios disponibles en tiempo real" />
                          </div>

                          {createAvailability && (
                            <div className="rounded-full bg-[#f5f1ed] px-3 py-1.5 text-xs font-semibold text-[#786a61]">
                              {createAvailability.totalSlots}{" "}
                              {createAvailability.totalSlots === 1
                                ? "horario"
                                : "horarios"}
                            </div>
                          )}
                        </div>

                        {(createProfessionalId == null
                          || createServiceId == null)
                        && !createAvailabilityLoading
                        && (
                          <div className="py-14 text-center">
                            <div className="text-sm font-semibold text-[#61554e]">
                              Completa profesional y servicio
                            </div>

                            <p className="mx-auto mt-2 max-w-sm text-xs leading-5 text-[#94857b]">
                              En cuanto selecciones ambos, consultaremos los
                              horarios reales disponibles para este día.
                            </p>
                          </div>
                        )}

                        {createAvailabilityLoading && (
                          <div className="py-14 text-center">
                            <div className="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-[#ddd3cb] border-t-[#51463f]" />

                            <p className="mt-4 text-sm text-[#8b7c72]">
                              Consultando disponibilidad...
                            </p>
                          </div>
                        )}

                        {!createAvailabilityLoading
                        && createProfessionalId != null
                        && createServiceId != null
                        && createAvailability
                        && createAvailability.slots.length === 0
                        && (
                          <div className="py-14 text-center">
                            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-[#f4efea] text-xl text-[#958478]">
                              —
                            </div>

                            <div className="mt-4 text-sm font-semibold text-[#5f534b]">
                              No hay horarios disponibles
                            </div>

                            <p className="mx-auto mt-2 max-w-sm text-xs leading-5 text-[#94857b]">
                              Selecciona otra fecha o comprueba la combinación
                              de profesional y servicio.
                            </p>
                          </div>
                        )}

                        {!createAvailabilityLoading
                        && createAvailability
                        && createAvailability.slots.length > 0
                        && (
                          <div className="mt-5 max-h-[280px] overflow-y-auto pr-1">
                            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
                              {createAvailability.slots.map(
                                (
                                  slot,
                                ) => {
                                  const selected =
                                    createSelectedSlot?.startAt
                                    === slot.startAt;

                                  return (
                                    <button
                                      key={
                                        slot.startAt
                                      }
                                      type="button"
                                      onClick={() => {
                                        setCreateSelectedSlot(
                                          slot,
                                        );

                                        setCreateError(
                                          null,
                                        );
                                      }}
                                      disabled={
                                        creating
                                      }
                                      className={[
                                        "relative rounded-[17px] border px-4 py-3 text-left transition",

                                        selected
                                          ? "border-[#4f6256] bg-[#eef4ef] shadow-[0_6px_20px_rgba(72,91,78,0.10)]"
                                          : "border-[#e3dad2] bg-[#faf8f6] hover:border-[#c9bbb0] hover:bg-white",
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
                                    </button>
                                  );
                                },
                              )}
                            </div>
                          </div>
                        )}

                        {createSelectedSlot && (
                          <div className="mt-5 rounded-[20px] border border-[#d7e3d9] bg-[#f0f6f1] p-4">
                            <div className="text-[10px] font-semibold uppercase tracking-[0.14em] text-[#66806c]">
                              Horario seleccionado
                            </div>

                            <div className="mt-3 flex items-center justify-between gap-5">
                              <div>
                                <div className="text-sm font-semibold text-[#3e5544]">
                                  {formatSelectedDate(
                                    createDate,
                                  )}
                                </div>

                                <div className="mt-1 text-xs text-[#6c806f]">
                                  {selectedCreateProfessional?.displayName
                                  || selectedCreateProfessional?.firstName
                                  || "Profesional"}
                                </div>
                              </div>

                              <div className="text-right">
                                <div className="text-xl font-semibold text-[#3e5544]">
                                  {formatSlotTime(
                                    createSelectedSlot.startTime,
                                  )}
                                </div>

                                <div className="text-xs text-[#6c806f]">
                                  hasta{" "}
                                  {formatSlotTime(
                                    createSelectedSlot.endTime,
                                  )}
                                </div>
                              </div>
                            </div>
                          </div>
                        )}
                      </section>

                      {/* OPTIONS */}

                      <section className="relative z-0 grid gap-5 rounded-[26px] border border-[#e6dbd0] bg-white p-5 shadow-[0_8px_28px_rgba(60,45,35,0.045)] md:grid-cols-2 md:p-6">
                        <div>
                          <StepHeading number={6} title="Estado inicial" description="Define cómo se registrará" />

                          <PremiumSelect
                            value={createStatus}
                            onChange={(nextStatus) => {
                              if (isCreatableAppointmentStatus(nextStatus)) {
                                setCreateStatus(nextStatus);
                              }
                            }}
                            options={[
                              { value: "PENDING", label: "Pendiente", description: "Requiere confirmación posterior" },
                              { value: "CONFIRMED", label: "Confirmada", description: "Cita confirmada desde el negocio" },
                            ]}
                            placeholder="Estado inicial"
                            ariaLabel="Estado inicial de la cita"
                            disabled={creating}
                          />

                          <p className="mt-2 text-xs leading-5 text-[#928278]">
                            Normalmente una cita manual puede dejarse pendiente
                            o confirmarse directamente.
                          </p>
                        </div>

                        <div>
                          <StepHeading number={7} title="Notas internas" description="Información útil para el equipo" />

                          <textarea
                            value={
                              createNotes
                            }
                            onChange={
                              (
                                event,
                              ) =>
                                setCreateNotes(
                                  event.target.value,
                                )
                            }
                            maxLength={
                              5000
                            }
                            disabled={
                              creating
                            }
                            placeholder="Observaciones opcionales..."
                            className="mt-3 min-h-[160px] w-full resize-none rounded-[24px] border border-[#e4d9cf] bg-[#fbf9f6] px-4 py-4 text-sm leading-6 text-[#342d28] shadow-[0_5px_16px_rgba(56,44,35,0.035)] outline-none transition-all duration-200 placeholder:text-[#a8998e] hover:border-[#bba79a] focus:border-[#90796a] focus:bg-white focus:shadow-[0_12px_32px_rgba(56,44,35,0.09)] focus:ring-4 focus:ring-[#b9a18e]/20 disabled:opacity-55"
                          />

                          <div className="mt-2 text-right text-xs font-medium text-[#958477]">
                            {createNotes.length}/5000
                          </div>
                        </div>
                      </section>

                      {/* ERROR */}

                      {createError && (
                        <section className="rounded-[20px] border border-[#ead3d3] bg-[#fff5f5] p-4">
                          <div className="text-sm font-semibold text-[#8d5959]">
                            No se ha podido completar la operación
                          </div>

                          <p className="mt-1 text-xs leading-5 text-[#956f6f]">
                            {createError}
                          </p>
                        </section>
                      )}
                    </div>
                  </div>
                )}
            </div>

            {/* FOOTER */}

            <div className="flex flex-col-reverse gap-3 border-t border-[#e8dfd7] bg-white px-5 py-5 sm:flex-row sm:items-center sm:justify-between sm:px-7 md:px-8">
              <p className="text-xs leading-5 text-[#93857b]">
                La disponibilidad se valida de nuevo en el backend al crear la cita.
              </p>

              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={
                    closeCreateModal
                  }
                  disabled={
                    creating
                  }
                  className="rounded-full border border-[#d9cfc7] bg-white px-5 py-2.5 text-sm font-semibold text-[#655950] transition hover:bg-[#faf8f6] disabled:cursor-not-allowed disabled:opacity-50"
                >
                  Cancelar
                </button>

                <button
                  type="button"
                  onClick={() =>
                    void createAppointment()
                  }
                  disabled={
                    !canCreateAppointment
                  }
                  className="rounded-full bg-[#342d28] px-6 py-2.5 text-sm font-semibold text-white shadow-[0_8px_24px_rgba(52,45,40,0.18)] transition hover:bg-[#241f1c] disabled:cursor-not-allowed disabled:opacity-40"
                >
                  {creating
                    ? "Creando cita..."
                    : "Crear cita"}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
}