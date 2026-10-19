"use client";

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
} from "@/lib/api";

/* =========================================================
   TYPES
========================================================= */

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

type BusinessResponse = {
  id: number;

  name: string;
  slug: string;
  description: string | null;

  email: string;
  phone: string | null;
  websiteUrl: string | null;

  addressLine1: string | null;
  addressLine2: string | null;
  postalCode: string | null;
  city: string | null;
  stateRegion: string | null;
  countryCode: string;

  timezone: string;
  locale: string;
  currency: string;

  logoUrl: string | null;
  coverImageUrl: string | null;

  primaryColor: string | null;
  secondaryColor: string | null;

  publicBookingEnabled: boolean;

  active: boolean;

  createdAt: string;
  updatedAt: string;
};

type BusinessUpdateRequest = {
  name: string;
  slug: string;
  description: string;

  email: string;
  phone: string;
  websiteUrl: string;

  addressLine1: string;
  addressLine2: string;
  postalCode: string;
  city: string;
  stateRegion: string;
  countryCode: string;

  timezone: string;
  locale: string;
  currency: string;

  logoUrl: string;
  coverImageUrl: string;

  primaryColor: string;
  secondaryColor: string;

  publicBookingEnabled: boolean;
};

type BusinessFormState = {
  name: string;
  slug: string;
  description: string;

  email: string;
  phone: string;
  websiteUrl: string;

  addressLine1: string;
  addressLine2: string;
  postalCode: string;
  city: string;
  stateRegion: string;
  countryCode: string;

  timezone: string;
  locale: string;
  currency: string;

  logoUrl: string;
  coverImageUrl: string;

  primaryColor: string;
  secondaryColor: string;

  publicBookingEnabled: boolean;
};

type BusinessSettingsResponse = {
  id: number;
  businessId: number;

  minimumBookingNoticeMinutes: number;
  bookingHorizonDays: number;
  slotIntervalMinutes: number;

  defaultBufferBeforeMinutes: number;
  defaultBufferAfterMinutes: number;

  autoConfirmAppointments: boolean;

  onlineCancellationEnabled: boolean;
  minimumCancellationNoticeMinutes: number;

  onlineReschedulingEnabled: boolean;
  minimumRescheduleNoticeMinutes: number;

  remindersEnabled: boolean;
  firstReminderMinutesBefore: number;
  secondReminderEnabled: boolean;
  secondReminderMinutesBefore: number;

  version: number;

  createdAt: string;
  updatedAt: string;
};

type BusinessSettingsUpdateRequest = {
  minimumBookingNoticeMinutes: number;
  bookingHorizonDays: number;
  slotIntervalMinutes: number;

  defaultBufferBeforeMinutes: number;
  defaultBufferAfterMinutes: number;

  autoConfirmAppointments: boolean;

  onlineCancellationEnabled: boolean;
  minimumCancellationNoticeMinutes: number;

  onlineReschedulingEnabled: boolean;
  minimumRescheduleNoticeMinutes: number;

  remindersEnabled: boolean;
  firstReminderMinutesBefore: number;
  secondReminderEnabled: boolean;
  secondReminderMinutesBefore: number;

  version: number;
};

type OperationalNumericKey =
  | "minimumBookingNoticeMinutes"
  | "bookingHorizonDays"
  | "slotIntervalMinutes"
  | "defaultBufferBeforeMinutes"
  | "defaultBufferAfterMinutes"
  | "minimumCancellationNoticeMinutes"
  | "minimumRescheduleNoticeMinutes"
  | "firstReminderMinutesBefore"
  | "secondReminderMinutesBefore";

type OperationalSettingsForm = {
  minimumBookingNoticeMinutes: string;
  bookingHorizonDays: string;
  slotIntervalMinutes: string;

  defaultBufferBeforeMinutes: string;
  defaultBufferAfterMinutes: string;

  autoConfirmAppointments: boolean;

  onlineCancellationEnabled: boolean;
  minimumCancellationNoticeMinutes: string;

  onlineReschedulingEnabled: boolean;
  minimumRescheduleNoticeMinutes: string;

  remindersEnabled: boolean;
  firstReminderMinutesBefore: string;
  secondReminderEnabled: boolean;
  secondReminderMinutesBefore: string;

  version: number;
};

/* =========================================================
   CONSTANTS
========================================================= */

const ACCESS_TOKEN_STORAGE_KEY =
  "wacds_access_token";

const BUSINESS_SESSION_STORAGE_KEY =
  "wacds_business_session";

const DEFAULT_PRIMARY_COLOR =
  "#342D28";

const DEFAULT_SECONDARY_COLOR =
  "#EEE7DF";

const timezoneOptions = [
  "Europe/Madrid",
  "Europe/Lisbon",
  "Europe/Paris",
  "Europe/London",
  "Europe/Rome",
  "Europe/Berlin",
  "Atlantic/Canary",
  "America/New_York",
  "America/Los_Angeles",
  "UTC",
];

const localeOptions = [
  {
    value:
      "es-ES",

    label:
      "Español · España",
  },
  {
    value:
      "en-GB",

    label:
      "English · United Kingdom",
  },
  {
    value:
      "en-US",

    label:
      "English · United States",
  },
  {
    value:
      "pt-PT",

    label:
      "Português · Portugal",
  },
  {
    value:
      "fr-FR",

    label:
      "Français · France",
  },
];

const currencyOptions = [
  {
    value:
      "EUR",

    label:
      "EUR · Euro",
  },
  {
    value:
      "USD",

    label:
      "USD · Dólar estadounidense",
  },
  {
    value:
      "GBP",

    label:
      "GBP · Libra esterlina",
  },
];

const countryOptions = [
  {
    value:
      "ES",

    label:
      "España",
  },
  {
    value:
      "PT",

    label:
      "Portugal",
  },
  {
    value:
      "FR",

    label:
      "Francia",
  },
  {
    value:
      "IT",

    label:
      "Italia",
  },
  {
    value:
      "DE",

    label:
      "Alemania",
  },
  {
    value:
      "GB",

    label:
      "Reino Unido",
  },
  {
    value:
      "US",

    label:
      "Estados Unidos",
  },
];

const operationalNumberLimits: Record<
  OperationalNumericKey,
  {
    min: number;
    max: number;
    belowMin: string;
    aboveMax: string;
  }
> = {
  minimumBookingNoticeMinutes: {
    min: 0,
    max: 525600,
    belowMin:
      "minimumBookingNoticeMinutes no puede ser negativo.",
    aboveMax:
      "minimumBookingNoticeMinutes no puede superar 525600 minutos.",
  },
  bookingHorizonDays: {
    min: 1,
    max: 730,
    belowMin:
      "bookingHorizonDays debe ser al menos 1.",
    aboveMax:
      "bookingHorizonDays no puede superar 730 días.",
  },
  slotIntervalMinutes: {
    min: 5,
    max: 240,
    belowMin:
      "slotIntervalMinutes debe ser como mínimo 5.",
    aboveMax:
      "slotIntervalMinutes no puede superar 240 minutos.",
  },
  defaultBufferBeforeMinutes: {
    min: 0,
    max: 240,
    belowMin:
      "defaultBufferBeforeMinutes no puede ser negativo.",
    aboveMax:
      "defaultBufferBeforeMinutes no puede superar 240 minutos.",
  },
  defaultBufferAfterMinutes: {
    min: 0,
    max: 240,
    belowMin:
      "defaultBufferAfterMinutes no puede ser negativo.",
    aboveMax:
      "defaultBufferAfterMinutes no puede superar 240 minutos.",
  },
  minimumCancellationNoticeMinutes: {
    min: 0,
    max: 525600,
    belowMin:
      "minimumCancellationNoticeMinutes no puede ser negativo.",
    aboveMax:
      "minimumCancellationNoticeMinutes no puede superar 525600 minutos.",
  },
  minimumRescheduleNoticeMinutes: {
    min: 0,
    max: 525600,
    belowMin:
      "minimumRescheduleNoticeMinutes no puede ser negativo.",
    aboveMax:
      "minimumRescheduleNoticeMinutes no puede superar 525600 minutos.",
  },
  firstReminderMinutesBefore: {
    min: 1,
    max: 525600,
    belowMin:
      "firstReminderMinutesBefore debe ser mayor que 0.",
    aboveMax:
      "firstReminderMinutesBefore no puede superar 525600 minutos.",
  },
  secondReminderMinutesBefore: {
    min: 1,
    max: 525600,
    belowMin:
      "secondReminderMinutesBefore debe ser mayor que 0.",
    aboveMax:
      "secondReminderMinutesBefore no puede superar 525600 minutos.",
  },
};

const operationalFieldLabels: Record<
  string,
  string
> = {
  minimumBookingNoticeMinutes:
    "Antelación mínima para reservar",
  bookingHorizonDays:
    "Horizonte de reserva",
  slotIntervalMinutes:
    "Intervalo entre huecos",
  defaultBufferBeforeMinutes:
    "Margen antes de la cita",
  defaultBufferAfterMinutes:
    "Margen después de la cita",
  autoConfirmAppointments:
    "Confirmación automática",
  onlineCancellationEnabled:
    "Cancelación online",
  minimumCancellationNoticeMinutes:
    "Antelación mínima para cancelar",
  onlineReschedulingEnabled:
    "Reprogramación online",
  minimumRescheduleNoticeMinutes:
    "Antelación mínima para reprogramar",
  remindersEnabled:
    "Recordatorios",
  firstReminderMinutesBefore:
    "Primer recordatorio",
  secondReminderEnabled:
    "Segundo recordatorio",
  secondReminderMinutesBefore:
    "Antelación del segundo recordatorio",
  version:
    "Versión",
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
    rawSession == null
    || rawSession.trim() === ""
  ) {
    return null;
  }

  try {
    return JSON.parse(
      rawSession,
    ) as StoredBusinessSession;
  } catch {
    return null;
  }
}

/* =========================================================
   NORMALIZATION
========================================================= */

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

function normalizeHexColor(
  value: string,
  fallback: string,
): string {
  const normalized =
    value
      .trim()
      .toUpperCase();

  if (
    /^#[0-9A-F]{6}$/.test(
      normalized,
    )
  ) {
    return normalized;
  }

  return fallback;
}

/* =========================================================
   MAPPING
========================================================= */

function businessToForm(
  business: BusinessResponse,
): BusinessFormState {
  return {
    name:
      business.name
      ?? "",

    slug:
      business.slug
      ?? "",

    description:
      business.description
      ?? "",

    email:
      business.email
      ?? "",

    phone:
      business.phone
      ?? "",

    websiteUrl:
      business.websiteUrl
      ?? "",

    addressLine1:
      business.addressLine1
      ?? "",

    addressLine2:
      business.addressLine2
      ?? "",

    postalCode:
      business.postalCode
      ?? "",

    city:
      business.city
      ?? "",

    stateRegion:
      business.stateRegion
      ?? "",

    countryCode:
      business.countryCode
      ?? "ES",

    timezone:
      business.timezone
      ?? "Europe/Madrid",

    locale:
      business.locale
      ?? "es-ES",

    currency:
      business.currency
      ?? "EUR",

    logoUrl:
      business.logoUrl
      ?? "",

    coverImageUrl:
      business.coverImageUrl
      ?? "",

    primaryColor:
      business.primaryColor
      ?? DEFAULT_PRIMARY_COLOR,

    secondaryColor:
      business.secondaryColor
      ?? DEFAULT_SECONDARY_COLOR,

    publicBookingEnabled:
      business.publicBookingEnabled,
  };
}

function settingsToForm(
  settings: BusinessSettingsResponse,
): OperationalSettingsForm {
  return {
    minimumBookingNoticeMinutes:
      String(
        settings.minimumBookingNoticeMinutes,
      ),

    bookingHorizonDays:
      String(
        settings.bookingHorizonDays,
      ),

    slotIntervalMinutes:
      String(
        settings.slotIntervalMinutes,
      ),

    defaultBufferBeforeMinutes:
      String(
        settings.defaultBufferBeforeMinutes,
      ),

    defaultBufferAfterMinutes:
      String(
        settings.defaultBufferAfterMinutes,
      ),

    autoConfirmAppointments:
      settings.autoConfirmAppointments,

    onlineCancellationEnabled:
      settings.onlineCancellationEnabled,

    minimumCancellationNoticeMinutes:
      String(
        settings.minimumCancellationNoticeMinutes,
      ),

    onlineReschedulingEnabled:
      settings.onlineReschedulingEnabled,

    minimumRescheduleNoticeMinutes:
      String(
        settings.minimumRescheduleNoticeMinutes,
      ),

    remindersEnabled:
      settings.remindersEnabled,

    firstReminderMinutesBefore:
      String(
        settings.firstReminderMinutesBefore,
      ),

    secondReminderEnabled:
      settings.remindersEnabled
      && settings.secondReminderEnabled,

    secondReminderMinutesBefore:
      String(
        settings.secondReminderMinutesBefore,
      ),

    version:
      settings.version,
  };
}

function parseIntegerField(
  value: string,
): number | null {
  const trimmed =
    value.trim();

  if (
    !/^-?\d+$/.test(
      trimmed,
    )
  ) {
    return null;
  }

  const parsed =
    Number(
      trimmed,
    );

  if (
    !Number.isSafeInteger(
      parsed,
    )
  ) {
    return null;
  }

  return parsed;
}

function validateOperationalForm(
  form: OperationalSettingsForm,
): Record<
  string,
  string
> {
  const fieldErrors: Record<
    string,
    string
  > = {};

  const parsed: Partial<
    Record<
      OperationalNumericKey,
      number
    >
  > = {};

  (
    Object.keys(
      operationalNumberLimits,
    ) as OperationalNumericKey[]
  ).forEach(
    (
      field,
    ) => {
      const limits =
        operationalNumberLimits[
          field
        ];

      const value =
        parseIntegerField(
          form[field],
        );

      if (
        value == null
      ) {
        fieldErrors[field] =
          "Introduce un número entero.";

        return;
      }

      if (
        value < limits.min
      ) {
        fieldErrors[field] =
          limits.belowMin;

        return;
      }

      if (
        value > limits.max
      ) {
        fieldErrors[field] =
          limits.aboveMax;

        return;
      }

      parsed[field] =
        value;
    },
  );

  const firstReminder =
    parsed.firstReminderMinutesBefore;

  const secondReminder =
    parsed.secondReminderMinutesBefore;

  if (
    form.remindersEnabled
    && form.secondReminderEnabled
    && firstReminder != null
    && secondReminder != null
    && secondReminder >= firstReminder
  ) {
    fieldErrors.secondReminderMinutesBefore =
      "El segundo recordatorio debe ejecutarse más cerca de la cita que el primero.";
  }

  if (
    !Number.isSafeInteger(
      form.version,
    )
    || form.version < 0
  ) {
    fieldErrors.version =
      "version no puede ser negativa.";
  }

  return fieldErrors;
}

function readValidationErrors(
  payload: unknown,
): Record<
  string,
  string
> {
  if (
    typeof payload !== "object"
    || payload === null
    || !(
      "validationErrors" in payload
    )
  ) {
    return {};
  }

  const validationErrors =
    payload.validationErrors;

  if (
    typeof validationErrors !== "object"
    || validationErrors === null
  ) {
    return {};
  }

  const fieldErrors: Record<
    string,
    string
  > = {};

  Object.entries(
    validationErrors,
  ).forEach(
    (
      [
        field,
        message,
      ],
    ) => {
      if (
        typeof message === "string"
        && message.trim() !== ""
      ) {
        fieldErrors[field] =
          message.trim();
      }
    },
  );

  return fieldErrors;
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
      return "No tienes permisos para modificar la configuración del negocio.";
    }

    if (
      error.status === 409
    ) {
      return (
        error.message
        || "Existe un conflicto con los datos del negocio."
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

function resolveOperationalError(
  error: unknown,
): {
  message: string;
  fieldErrors: Record<
    string,
    string
  >;
} {
  if (
    error instanceof ApiError
  ) {
    const fieldErrors =
      readValidationErrors(
        error.payload,
      );

    if (
      error.status === 401
    ) {
      return {
        message:
          "La sesión Business ha caducado. Vuelve a iniciar sesión.",
        fieldErrors,
      };
    }

    if (
      error.status === 403
    ) {
      return {
        message:
          "No tienes permisos para consultar o modificar los recordatorios y las reservas.",
        fieldErrors,
      };
    }

    if (
      error.status === 409
    ) {
      return {
        message:
          error.message
          || "La configuración ha cambiado desde que fue cargada.",
        fieldErrors,
      };
    }

    return {
      message:
        error.message
        || "No se ha podido completar la operación.",
      fieldErrors,
    };
  }

  return {
    message:
      resolveApiError(
        error,
      ),
    fieldErrors: {},
  };
}

function requireSessionBusinessId(): number {
  const storedSession =
    readStoredBusinessSession();

  const businessId =
    storedSession?.businessId;

  if (
    typeof businessId !== "number"
    || !Number.isInteger(
      businessId,
    )
    || businessId <= 0
  ) {
    throw new Error(
      "La sesión actual no contiene un negocio válido.",
    );
  }

  return businessId;
}

/* =========================================================
   DATE
========================================================= */

function formatDateTime(
  value: string,
): string {
  try {
    return new Intl.DateTimeFormat(
      "es-ES",
      {
        day:
          "2-digit",

        month:
          "2-digit",

        year:
          "numeric",

        hour:
          "2-digit",

        minute:
          "2-digit",
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
   PAGE
========================================================= */

export default function BusinessSettingsPage() {

  const [
    business,
    setBusiness,
  ] =
    useState<
      BusinessResponse
      | null
    >(
      null,
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
    form,
    setForm,
  ] =
    useState<
      BusinessFormState
      | null
    >(
      null,
    );

  const [
    loading,
    setLoading,
  ] =
    useState(
      true,
    );

  const [
    saving,
    setSaving,
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
    successMessage,
    setSuccessMessage,
  ] =
    useState<
      string | null
    >(
      null,
    );

  const [
    slugManuallyEdited,
    setSlugManuallyEdited,
  ] =
    useState(
      true,
    );

  const [
    operationalSettings,
    setOperationalSettings,
  ] =
    useState<
      BusinessSettingsResponse
      | null
    >(
      null,
    );

  const [
    operationalForm,
    setOperationalForm,
  ] =
    useState<
      OperationalSettingsForm
      | null
    >(
      null,
    );

  const [
    operationalLoading,
    setOperationalLoading,
  ] =
    useState(
      true,
    );

  const [
    operationalSaving,
    setOperationalSaving,
  ] =
    useState(
      false,
    );

  const [
    operationalError,
    setOperationalError,
  ] =
    useState<
      string | null
    >(
      null,
    );

  const [
    operationalFieldErrors,
    setOperationalFieldErrors,
  ] =
    useState<
      Record<
        string,
        string
      >
    >(
      {},
    );

  const [
    operationalSuccess,
    setOperationalSuccess,
  ] =
    useState<
      string | null
    >(
      null,
    );

  /* =======================================================
     PERMISSIONS
  ======================================================= */

  const canEdit =
    session?.role === "OWNER"
    || session?.role === "ADMIN";

  /* =======================================================
     LOAD
  ======================================================= */

  const loadBusiness =
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
              BusinessResponse
            >(
              "/api/v1/businesses/current",
              {
                accessToken,
              },
            );

          setBusiness(
            response,
          );

          setForm(
            businessToForm(
              response,
            ),
          );

          setSlugManuallyEdited(
            true,
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

  const loadOperationalSettings =
    useCallback(
      async () => {
        setOperationalLoading(
          true,
        );

        setOperationalError(
          null,
        );

        setOperationalFieldErrors(
          {},
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

          const businessId =
            requireSessionBusinessId();

          const response =
            await apiGet<
              BusinessSettingsResponse
            >(
              `/api/v1/businesses/${businessId}/settings`,
              {
                accessToken,
              },
            );

          setOperationalSettings(
            response,
          );

          setOperationalForm(
            settingsToForm(
              response,
            ),
          );
        } catch (
          requestError
        ) {
          const resolved =
            resolveOperationalError(
              requestError,
            );

          setOperationalSettings(
            null,
          );

          setOperationalForm(
            null,
          );

          setOperationalError(
            resolved.message,
          );

          setOperationalFieldErrors(
            resolved.fieldErrors,
          );
        } finally {
          setOperationalLoading(
            false,
          );
        }
      },
      [],
    );

  useEffect(
    () => {
      setSession(
        readStoredBusinessSession(),
      );

      void loadBusiness();

      void loadOperationalSettings();
    },
    [
      loadBusiness,
      loadOperationalSettings,
    ],
  );

  /* =======================================================
     SUCCESS
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

  useEffect(
    () => {
      if (
        !operationalSuccess
      ) {
        return;
      }

      const timeout =
        window.setTimeout(
          () => {
            setOperationalSuccess(
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
      operationalSuccess,
    ],
  );

  /* =======================================================
     DIRTY STATE
  ======================================================= */

  const dirty =
    useMemo(
      () => {
        if (
          !business
          || !form
        ) {
          return false;
        }

        return (
          JSON.stringify(
            businessToForm(
              business,
            ),
          )
          !== JSON.stringify(
            form,
          )
        );
      },
      [
        business,
        form,
      ],
    );

  const operationalDirty =
    useMemo(
      () => {
        if (
          !operationalSettings
          || !operationalForm
        ) {
          return false;
        }

        return (
          JSON.stringify(
            settingsToForm(
              operationalSettings,
            ),
          )
          !== JSON.stringify(
            operationalForm,
          )
        );
      },
      [
        operationalSettings,
        operationalForm,
      ],
    );

  /* =======================================================
     RESET
  ======================================================= */

  function resetForm() {
    if (
      !business
    ) {
      return;
    }

    setForm(
      businessToForm(
        business,
      ),
    );

    setError(
      null,
    );

    setSuccessMessage(
      null,
    );
  }

  function resetOperationalForm() {
    if (
      !operationalSettings
    ) {
      return;
    }

    setOperationalForm(
      settingsToForm(
        operationalSettings,
      ),
    );

    setOperationalError(
      null,
    );

    setOperationalFieldErrors(
      {},
    );

    setOperationalSuccess(
      null,
    );
  }

  function updateOperationalForm(
    patch: Partial<
      OperationalSettingsForm
    >,
  ) {
    setOperationalForm(
      (
        current,
      ) => current
        ? {
            ...current,
            ...patch,
          }
        : current,
    );

    setOperationalError(
      null,
    );

    setOperationalFieldErrors(
      {},
    );
  }

  /* =======================================================
     VALIDATE
  ======================================================= */

  function validateForm():
    | string
    | null {
    if (
      !form
    ) {
      return "No existen datos para guardar.";
    }

    if (
      form.name.trim() === ""
    ) {
      return "El nombre del negocio es obligatorio.";
    }

    if (
      form.slug.trim() === ""
    ) {
      return "El slug del negocio es obligatorio.";
    }

    if (
      form.email.trim() === ""
    ) {
      return "El email del negocio es obligatorio.";
    }

    if (
      form.countryCode.trim().length !== 2
    ) {
      return "El código de país debe contener 2 letras.";
    }

    if (
      form.timezone.trim() === ""
    ) {
      return "La zona horaria es obligatoria.";
    }

    if (
      form.locale.trim() === ""
    ) {
      return "El locale es obligatorio.";
    }

    if (
      form.currency.trim().length !== 3
    ) {
      return "La moneda debe contener 3 letras.";
    }

    if (
      form.primaryColor.trim() !== ""
      && !/^#[0-9A-Fa-f]{6}$/.test(
        form.primaryColor.trim(),
      )
    ) {
      return "El color principal debe tener formato #RRGGBB.";
    }

    if (
      form.secondaryColor.trim() !== ""
      && !/^#[0-9A-Fa-f]{6}$/.test(
        form.secondaryColor.trim(),
      )
    ) {
      return "El color secundario debe tener formato #RRGGBB.";
    }

    return null;
  }

  /* =======================================================
     SAVE
  ======================================================= */

  async function saveSettings() {
    if (
      !form
      || saving
      || !canEdit
    ) {
      return;
    }

    const validationError =
      validateForm();

    if (
      validationError
    ) {
      setError(
        validationError,
      );

      return;
    }

    setSaving(
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

      const payload: BusinessUpdateRequest = {
        name:
          form.name.trim(),

        slug:
          createSlug(
            form.slug,
          ),

        description:
          form.description,

        email:
          form.email.trim(),

        phone:
          form.phone,

        websiteUrl:
          form.websiteUrl,

        addressLine1:
          form.addressLine1,

        addressLine2:
          form.addressLine2,

        postalCode:
          form.postalCode,

        city:
          form.city,

        stateRegion:
          form.stateRegion,

        countryCode:
          form.countryCode
            .trim()
            .toUpperCase(),

        timezone:
          form.timezone.trim(),

        locale:
          form.locale.trim(),

        currency:
          form.currency
            .trim()
            .toUpperCase(),

        logoUrl:
          form.logoUrl,

        coverImageUrl:
          form.coverImageUrl,

        primaryColor:
          form.primaryColor.trim(),

        secondaryColor:
          form.secondaryColor.trim(),

        publicBookingEnabled:
          form.publicBookingEnabled,
      };

      const response =
        await apiPatch<
          BusinessResponse
        >(
          "/api/v1/businesses/current",
          payload,
          {
            accessToken,
          },
        );

      setBusiness(
        response,
      );

      setForm(
        businessToForm(
          response,
        ),
      );

      setSuccessMessage(
        "Configuración guardada correctamente.",
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
      setSaving(
        false,
      );
    }
  }

  /* =======================================================
     SAVE OPERATIONAL SETTINGS
  ======================================================= */

  async function saveOperationalSettings() {
    if (
      !operationalForm
      || operationalSaving
      || !canEdit
    ) {
      return;
    }

    const fieldErrors =
      validateOperationalForm(
        operationalForm,
      );

    if (
      Object.keys(
        fieldErrors,
      ).length > 0
    ) {
      setOperationalFieldErrors(
        fieldErrors,
      );

      setOperationalError(
        "Revisa los campos antes de guardar.",
      );

      setOperationalSuccess(
        null,
      );

      return;
    }

    setOperationalSaving(
      true,
    );

    setOperationalError(
      null,
    );

    setOperationalFieldErrors(
      {},
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

      const businessId =
        requireSessionBusinessId();

      const minimumBookingNoticeMinutes =
        parseIntegerField(
          operationalForm.minimumBookingNoticeMinutes,
        );

      const bookingHorizonDays =
        parseIntegerField(
          operationalForm.bookingHorizonDays,
        );

      const slotIntervalMinutes =
        parseIntegerField(
          operationalForm.slotIntervalMinutes,
        );

      const defaultBufferBeforeMinutes =
        parseIntegerField(
          operationalForm.defaultBufferBeforeMinutes,
        );

      const defaultBufferAfterMinutes =
        parseIntegerField(
          operationalForm.defaultBufferAfterMinutes,
        );

      const minimumCancellationNoticeMinutes =
        parseIntegerField(
          operationalForm.minimumCancellationNoticeMinutes,
        );

      const minimumRescheduleNoticeMinutes =
        parseIntegerField(
          operationalForm.minimumRescheduleNoticeMinutes,
        );

      const firstReminderMinutesBefore =
        parseIntegerField(
          operationalForm.firstReminderMinutesBefore,
        );

      const secondReminderMinutesBefore =
        parseIntegerField(
          operationalForm.secondReminderMinutesBefore,
        );

      if (
        minimumBookingNoticeMinutes == null
        || bookingHorizonDays == null
        || slotIntervalMinutes == null
        || defaultBufferBeforeMinutes == null
        || defaultBufferAfterMinutes == null
        || minimumCancellationNoticeMinutes == null
        || minimumRescheduleNoticeMinutes == null
        || firstReminderMinutesBefore == null
        || secondReminderMinutesBefore == null
      ) {
        throw new Error(
          "Revisa los campos numéricos antes de guardar.",
        );
      }

      const payload: BusinessSettingsUpdateRequest = {
        minimumBookingNoticeMinutes,
        bookingHorizonDays,
        slotIntervalMinutes,
        defaultBufferBeforeMinutes,
        defaultBufferAfterMinutes,
        autoConfirmAppointments:
          operationalForm.autoConfirmAppointments,
        onlineCancellationEnabled:
          operationalForm.onlineCancellationEnabled,
        minimumCancellationNoticeMinutes,
        onlineReschedulingEnabled:
          operationalForm.onlineReschedulingEnabled,
        minimumRescheduleNoticeMinutes,
        remindersEnabled:
          operationalForm.remindersEnabled,
        firstReminderMinutesBefore,
        secondReminderEnabled:
          operationalForm.remindersEnabled
          && operationalForm.secondReminderEnabled,
        secondReminderMinutesBefore,
        version:
          operationalForm.version,
      };

      const response =
        await apiPatch<
          BusinessSettingsResponse
        >(
          `/api/v1/businesses/${businessId}/settings`,
          payload,
          {
            accessToken,
          },
        );

      setOperationalSettings(
        response,
      );

      setOperationalForm(
        settingsToForm(
          response,
        ),
      );

      setOperationalSuccess(
        "Recordatorios y reservas guardados correctamente.",
      );
    } catch (
      requestError
    ) {
      const resolved =
        resolveOperationalError(
          requestError,
        );

      setOperationalError(
        resolved.message,
      );

      setOperationalFieldErrors(
        resolved.fieldErrors,
      );
    } finally {
      setOperationalSaving(
        false,
      );
    }
  }

  /* =======================================================
     LOADING
  ======================================================= */

  if (
    loading
  ) {
    return (
      <div className="space-y-6 pb-10">
        <div className="h-28 animate-pulse rounded-[26px] bg-white" />

        <div className="grid gap-5 xl:grid-cols-2">
          <div className="h-[380px] animate-pulse rounded-[26px] bg-white" />
          <div className="h-[380px] animate-pulse rounded-[26px] bg-white" />
        </div>

        <div className="h-[420px] animate-pulse rounded-[26px] bg-white" />
      </div>
    );
  }

  /* =======================================================
     NO DATA
  ======================================================= */

  if (
    !business
    || !form
  ) {
    return (
      <section className="rounded-[26px] border border-[#ead3d3] bg-[#fbf1f1] p-6">
        <h1 className="text-xl font-semibold text-[#744d4d]">
          No se ha podido cargar la configuración
        </h1>

        <p className="mt-2 text-sm text-[#946f6f]">
          {error
          || "No existen datos disponibles del negocio."}
        </p>

        <button
          type="button"
          onClick={() => {
            void loadBusiness();
            void loadOperationalSettings();
          }}
          className="mt-5 rounded-full bg-[#342d28] px-5 py-2.5 text-sm font-semibold text-white"
        >
          Reintentar
        </button>
      </section>
    );
  }

  /* =======================================================
     RENDER
  ======================================================= */

  return (
    <div className="space-y-7 pb-12">

      {/* ===================================================
          HEADER
      =================================================== */}

      <section className="flex flex-col gap-5 xl:flex-row xl:items-end xl:justify-between">

        <div>
          <div className="text-xs font-semibold uppercase tracking-[0.22em] text-[#a18c7f]">
            Configuración
          </div>

          <h1 className="mt-2 text-3xl font-semibold tracking-[-0.03em] text-[#2f2925] md:text-4xl">
            Configuración del negocio
          </h1>

          <p className="mt-3 max-w-2xl text-sm leading-6 text-[#796b62] md:text-base">
            Gestiona la identidad, contacto, ubicación, configuración regional
            y reservas públicas de tu negocio.
          </p>
        </div>

        <div className="flex flex-wrap gap-3">

          <button
            type="button"
            onClick={
              resetForm
            }
            disabled={
              !dirty
              || saving
            }
            className="rounded-full border border-[#d8cec6] bg-white px-5 py-3 text-sm font-semibold text-[#62564f] transition hover:bg-[#f7f3ef] disabled:cursor-not-allowed disabled:opacity-40"
          >
            Descartar cambios
          </button>

          <button
            type="button"
            onClick={() =>
              void saveSettings()
            }
            disabled={
              !dirty
              || saving
              || !canEdit
            }
            className="rounded-full bg-[#342d28] px-5 py-3 text-sm font-semibold text-white transition hover:bg-[#211c18] disabled:cursor-not-allowed disabled:opacity-40"
          >
            {saving
              ? "Guardando..."
              : "Guardar cambios"}
          </button>
        </div>
      </section>

      {/* ===================================================
          READ ONLY NOTICE
      =================================================== */}

      {!canEdit && (
        <section className="rounded-[22px] border border-[#eadfc7] bg-[#fff9ee] px-5 py-4">
          <div className="text-sm font-semibold text-[#806840]">
            Modo solo lectura
          </div>

          <p className="mt-1 text-sm leading-6 text-[#907c58]">
            Tu rol STAFF puede consultar la configuración, pero solo OWNER o
            ADMIN pueden modificarla.
          </p>
        </section>
      )}

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
        <section className="rounded-[20px] border border-[#ead3d3] bg-[#fbf1f1] px-5 py-4 text-sm text-[#8f5f5f]">
          {error}
        </section>
      )}

      {/* ===================================================
          STATUS
      =================================================== */}

      <section className="grid gap-4 md:grid-cols-3">

        <StatusCard
          label="Estado del negocio"
          value={
            business.active
              ? "Activo"
              : "Inactivo"
          }
          description={
            business.active
              ? "Tenant operativo"
              : "Tenant deshabilitado"
          }
        />

        <StatusCard
          label="Reservas públicas"
          value={
            business.publicBookingEnabled
              ? "Habilitadas"
              : "Deshabilitadas"
          }
          description="Disponibilidad pública actual"
        />

        <StatusCard
          label="Última actualización"
          value={formatDateTime(
            business.updatedAt,
          )}
          description={`Business #${business.id}`}
        />
      </section>

      {/* ===================================================
          IDENTITY
      =================================================== */}

      <SettingsSection
        title="Identidad"
        description="Información principal con la que se identifica el negocio."
      >
        <div className="grid gap-5 md:grid-cols-2">

          <Field
            label="Nombre"
            required
          >
            <input
              type="text"
              value={
                form.name
              }
              maxLength={
                150
              }
              disabled={
                !canEdit
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
                    ) => current
                      ? {
                          ...current,

                          name,

                          slug:
                            slugManuallyEdited
                              ? current.slug
                              : createSlug(
                                  name,
                                ),
                        }
                      : current,
                  );
                }
              }
              className={inputClassName}
            />
          </Field>

          <Field
            label="Slug"
            required
            description="Identificador público y URL amigable."
          >
            <input
              type="text"
              value={
                form.slug
              }
              maxLength={
                180
              }
              disabled={
                !canEdit
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
                    ) => current
                      ? {
                          ...current,

                          slug:
                            createSlug(
                              event.target.value,
                            ),
                        }
                      : current,
                  );
                }
              }
              className={inputClassName}
            />
          </Field>
        </div>

        <Field
          label="Descripción"
          className="mt-5"
        >
          <textarea
            value={
              form.description
            }
            rows={
              5
            }
            maxLength={
              5000
            }
            disabled={
              !canEdit
            }
            onChange={
              (
                event,
              ) =>
                setForm(
                  (
                    current,
                  ) => current
                    ? {
                        ...current,

                        description:
                          event.target.value,
                      }
                    : current,
                )
            }
            className={`${inputClassName} resize-none leading-6`}
            placeholder="Describe brevemente tu negocio..."
          />
        </Field>
      </SettingsSection>

      {/* ===================================================
          CONTACT + ADDRESS
      =================================================== */}

      <div className="grid gap-6 xl:grid-cols-2">

        <SettingsSection
          title="Contacto"
          description="Información utilizada para comunicaciones y presencia digital."
        >
          <div className="space-y-5">

            <Field
              label="Email"
              required
            >
              <input
                type="email"
                value={
                  form.email
                }
                maxLength={
                  180
                }
                disabled={
                  !canEdit
                }
                onChange={
                  (
                    event,
                  ) =>
                    setForm(
                      (
                        current,
                      ) => current
                        ? {
                            ...current,

                            email:
                              event.target.value,
                          }
                        : current,
                    )
                }
                className={inputClassName}
              />
            </Field>

            <Field label="Teléfono">
              <input
                type="tel"
                value={
                  form.phone
                }
                maxLength={
                  40
                }
                disabled={
                  !canEdit
                }
                onChange={
                  (
                    event,
                  ) =>
                    setForm(
                      (
                        current,
                      ) => current
                        ? {
                            ...current,

                            phone:
                              event.target.value,
                          }
                        : current,
                    )
                }
                className={inputClassName}
              />
            </Field>

            <Field label="Página web">
              <input
                type="url"
                value={
                  form.websiteUrl
                }
                maxLength={
                  500
                }
                disabled={
                  !canEdit
                }
                onChange={
                  (
                    event,
                  ) =>
                    setForm(
                      (
                        current,
                      ) => current
                        ? {
                            ...current,

                            websiteUrl:
                              event.target.value,
                          }
                        : current,
                    )
                }
                placeholder="https://..."
                className={inputClassName}
              />
            </Field>
          </div>
        </SettingsSection>

        <SettingsSection
          title="Ubicación"
          description="Dirección física y datos regionales del establecimiento."
        >
          <div className="space-y-5">

            <Field label="Dirección">
              <input
                type="text"
                value={
                  form.addressLine1
                }
                maxLength={
                  180
                }
                disabled={
                  !canEdit
                }
                onChange={
                  (
                    event,
                  ) =>
                    setForm(
                      (
                        current,
                      ) => current
                        ? {
                            ...current,

                            addressLine1:
                              event.target.value,
                          }
                        : current,
                    )
                }
                className={inputClassName}
              />
            </Field>

            <Field label="Dirección adicional">
              <input
                type="text"
                value={
                  form.addressLine2
                }
                maxLength={
                  180
                }
                disabled={
                  !canEdit
                }
                onChange={
                  (
                    event,
                  ) =>
                    setForm(
                      (
                        current,
                      ) => current
                        ? {
                            ...current,

                            addressLine2:
                              event.target.value,
                          }
                        : current,
                    )
                }
                className={inputClassName}
              />
            </Field>

            <div className="grid gap-5 sm:grid-cols-2">

              <Field label="Código postal">
                <input
                  type="text"
                  value={
                    form.postalCode
                  }
                  maxLength={
                    30
                  }
                  disabled={
                    !canEdit
                  }
                  onChange={
                    (
                      event,
                    ) =>
                      setForm(
                        (
                          current,
                        ) => current
                          ? {
                              ...current,

                              postalCode:
                                event.target.value,
                            }
                          : current,
                      )
                  }
                  className={inputClassName}
                />
              </Field>

              <Field label="Ciudad">
                <input
                  type="text"
                  value={
                    form.city
                  }
                  maxLength={
                    120
                  }
                  disabled={
                    !canEdit
                  }
                  onChange={
                    (
                      event,
                    ) =>
                      setForm(
                        (
                          current,
                        ) => current
                          ? {
                              ...current,

                              city:
                                event.target.value,
                            }
                          : current,
                      )
                  }
                  className={inputClassName}
                />
              </Field>
            </div>

            <div className="grid gap-5 sm:grid-cols-2">

              <Field label="Provincia / región">
                <input
                  type="text"
                  value={
                    form.stateRegion
                  }
                  maxLength={
                    120
                  }
                  disabled={
                    !canEdit
                  }
                  onChange={
                    (
                      event,
                    ) =>
                      setForm(
                        (
                          current,
                        ) => current
                          ? {
                              ...current,

                              stateRegion:
                                event.target.value,
                            }
                          : current,
                      )
                  }
                  className={inputClassName}
                />
              </Field>

              <Field
                label="País"
                required
              >
                <select
                  value={
                    form.countryCode
                  }
                  disabled={
                    !canEdit
                  }
                  onChange={
                    (
                      event,
                    ) =>
                      setForm(
                        (
                          current,
                        ) => current
                          ? {
                              ...current,

                              countryCode:
                                event.target.value,
                            }
                          : current,
                      )
                  }
                  className={inputClassName}
                >
                  {countryOptions.map(
                    (
                      country,
                    ) => (
                      <option
                        key={
                          country.value
                        }
                        value={
                          country.value
                        }
                      >
                        {country.label}
                      </option>
                    ),
                  )}
                </select>
              </Field>
            </div>
          </div>
        </SettingsSection>
      </div>

      {/* ===================================================
          REGIONAL
      =================================================== */}

      <SettingsSection
        title="Configuración regional"
        description="Zona horaria, idioma y moneda utilizados por citas y precios."
      >
        <div className="grid gap-5 md:grid-cols-3">

          <Field
            label="Zona horaria"
            required
          >
            <select
              value={
                form.timezone
              }
              disabled={
                !canEdit
              }
              onChange={
                (
                  event,
                ) =>
                  setForm(
                    (
                      current,
                    ) => current
                      ? {
                          ...current,

                          timezone:
                            event.target.value,
                        }
                      : current,
                  )
              }
              className={inputClassName}
            >
              {!timezoneOptions.includes(
                form.timezone,
              ) && (
                <option
                  value={
                    form.timezone
                  }
                >
                  {form.timezone}
                </option>
              )}

              {timezoneOptions.map(
                (
                  timezone,
                ) => (
                  <option
                    key={
                      timezone
                    }
                    value={
                      timezone
                    }
                  >
                    {timezone}
                  </option>
                ),
              )}
            </select>
          </Field>

          <Field
            label="Idioma / locale"
            required
          >
            <select
              value={
                form.locale
              }
              disabled={
                !canEdit
              }
              onChange={
                (
                  event,
                ) =>
                  setForm(
                    (
                      current,
                    ) => current
                      ? {
                          ...current,

                          locale:
                            event.target.value,
                        }
                      : current,
                  )
              }
              className={inputClassName}
            >
              {!localeOptions.some(
                (
                  option,
                ) =>
                  option.value
                  === form.locale,
              ) && (
                <option
                  value={
                    form.locale
                  }
                >
                  {form.locale}
                </option>
              )}

              {localeOptions.map(
                (
                  option,
                ) => (
                  <option
                    key={
                      option.value
                    }
                    value={
                      option.value
                    }
                  >
                    {option.label}
                  </option>
                ),
              )}
            </select>
          </Field>

          <Field
            label="Moneda"
            required
          >
            <select
              value={
                form.currency
              }
              disabled={
                !canEdit
              }
              onChange={
                (
                  event,
                ) =>
                  setForm(
                    (
                      current,
                    ) => current
                      ? {
                          ...current,

                          currency:
                            event.target.value,
                        }
                      : current,
                  )
              }
              className={inputClassName}
            >
              {!currencyOptions.some(
                (
                  option,
                ) =>
                  option.value
                  === form.currency,
              ) && (
                <option
                  value={
                    form.currency
                  }
                >
                  {form.currency}
                </option>
              )}

              {currencyOptions.map(
                (
                  option,
                ) => (
                  <option
                    key={
                      option.value
                    }
                    value={
                      option.value
                    }
                  >
                    {option.label}
                  </option>
                ),
              )}
            </select>
          </Field>
        </div>
      </SettingsSection>

      {/* ===================================================
          BRANDING
      =================================================== */}

      <SettingsSection
        title="Branding"
        description="Recursos visuales utilizados por la experiencia pública del negocio."
      >
        <div className="grid gap-5 xl:grid-cols-2">

          <Field label="URL del logo">
            <input
              type="url"
              value={
                form.logoUrl
              }
              maxLength={
                500
              }
              disabled={
                !canEdit
              }
              onChange={
                (
                  event,
                ) =>
                  setForm(
                    (
                      current,
                    ) => current
                      ? {
                          ...current,

                          logoUrl:
                            event.target.value,
                        }
                      : current,
                  )
              }
              placeholder="https://..."
              className={inputClassName}
            />
          </Field>

          <Field label="URL de portada">
            <input
              type="url"
              value={
                form.coverImageUrl
              }
              maxLength={
                500
              }
              disabled={
                !canEdit
              }
              onChange={
                (
                  event,
                ) =>
                  setForm(
                    (
                      current,
                    ) => current
                      ? {
                          ...current,

                          coverImageUrl:
                            event.target.value,
                        }
                      : current,
                  )
              }
              placeholder="https://..."
              className={inputClassName}
            />
          </Field>
        </div>

        <div className="mt-5 grid gap-5 md:grid-cols-2">

          <ColorField
            label="Color principal"
            value={
              form.primaryColor
            }
            disabled={
              !canEdit
            }
            fallback={
              DEFAULT_PRIMARY_COLOR
            }
            onChange={
              (
                value,
              ) =>
                setForm(
                  (
                    current,
                  ) => current
                    ? {
                        ...current,

                        primaryColor:
                          value,
                      }
                    : current,
                )
            }
          />

          <ColorField
            label="Color secundario"
            value={
              form.secondaryColor
            }
            disabled={
              !canEdit
            }
            fallback={
              DEFAULT_SECONDARY_COLOR
            }
            onChange={
              (
                value,
              ) =>
                setForm(
                  (
                    current,
                  ) => current
                    ? {
                        ...current,

                        secondaryColor:
                          value,
                      }
                    : current,
                )
            }
          />
        </div>
      </SettingsSection>

      {/* ===================================================
          PUBLIC BOOKING
      =================================================== */}

      <SettingsSection
        title="Reservas públicas"
        description="Controla si los clientes pueden reservar citas públicamente."
      >
        <div className="flex flex-col gap-5 rounded-[22px] border border-[#e5ddd5] bg-[#f8f5f1] p-5 sm:flex-row sm:items-center sm:justify-between">

          <div>
            <div className="text-sm font-semibold text-[#342e2a]">
              Permitir reservas públicas
            </div>

            <p className="mt-1 max-w-2xl text-sm leading-6 text-[#82746b]">
              Al desactivarlo, los clientes dejarán de poder generar nuevas
              reservas públicas. Las citas existentes no se eliminan.
            </p>
          </div>

          <ToggleSwitch
            active={
              form.publicBookingEnabled
            }
            disabled={
              !canEdit
            }
            onClick={() =>
              setForm(
                (
                  current,
                ) => current
                  ? {
                      ...current,

                      publicBookingEnabled:
                        !current.publicBookingEnabled,
                    }
                  : current,
              )
            }
          />
        </div>
      </SettingsSection>

      {/* ===================================================
          REMINDERS AND BOOKING POLICIES
      =================================================== */}

      <SettingsSection
        title="Recordatorios y reservas"
        description="Antelación, horizonte, huecos, márgenes, cancelación, reprogramación y recordatorios."
      >
        {operationalLoading && (
          <div className="space-y-4">
            <div className="h-24 animate-pulse rounded-[22px] bg-[#f6f2ee]" />
            <div className="h-24 animate-pulse rounded-[22px] bg-[#f6f2ee]" />
            <div className="h-40 animate-pulse rounded-[22px] bg-[#f6f2ee]" />
          </div>
        )}

        {!operationalLoading
        && (
          !operationalSettings
          || !operationalForm
        ) && (
          <div className="rounded-[22px] border border-[#ead3d3] bg-[#fbf1f1] p-5">
            <div className="text-sm font-semibold text-[#744d4d]">
              No se ha podido cargar la configuración de recordatorios y reservas
            </div>

            <p className="mt-2 text-sm leading-6 text-[#946f6f]">
              {operationalError
              || "No existen datos disponibles."}
            </p>

            <button
              type="button"
              onClick={() =>
                void loadOperationalSettings()
              }
              className="mt-5 rounded-full bg-[#342d28] px-5 py-2.5 text-sm font-semibold text-white"
            >
              Reintentar
            </button>
          </div>
        )}

        {!operationalLoading
        && operationalSettings
        && operationalForm && (
          <div className="space-y-7">

            {operationalSuccess && (
              <section className="rounded-[20px] border border-[#dce8dd] bg-[#f1f7f1] px-5 py-4 text-sm font-medium text-[#55705a]">
                {operationalSuccess}
              </section>
            )}

            {operationalError && (
              <section className="rounded-[20px] border border-[#ead3d3] bg-[#fbf1f1] px-5 py-4 text-sm text-[#8f5f5f]">
                <p>
                  {operationalError}
                </p>

                {Object.keys(
                  operationalFieldErrors,
                ).length > 0 && (
                  <ul className="mt-3 list-disc space-y-1 pl-5">
                    {Object.entries(
                      operationalFieldErrors,
                    ).map(
                      (
                        [
                          field,
                          message,
                        ],
                      ) => (
                        <li
                          key={
                            field
                          }
                        >
                          {operationalFieldLabels[field]
                          ?? field}
                          {": "}
                          {message}
                        </li>
                      ),
                    )}
                  </ul>
                )}
              </section>
            )}

            <div>
              <div className="text-xs font-semibold uppercase tracking-[0.14em] text-[#9a897d]">
                Ventana de reserva
              </div>

              <div className="mt-4 grid gap-5 md:grid-cols-2">
                <OperationalNumberField
                  label="Antelación mínima para reservar"
                  description="Minutos exigidos para crear una reserva. 0 permite reservar de inmediato. Máximo 525600."
                  value={
                    operationalForm.minimumBookingNoticeMinutes
                  }
                  min={
                    operationalNumberLimits.minimumBookingNoticeMinutes.min
                  }
                  max={
                    operationalNumberLimits.minimumBookingNoticeMinutes.max
                  }
                  disabled={
                    !canEdit
                    || operationalSaving
                  }
                  error={
                    operationalFieldErrors.minimumBookingNoticeMinutes
                  }
                  onChange={
                    (
                      value,
                    ) =>
                      updateOperationalForm(
                        {
                          minimumBookingNoticeMinutes:
                            value,
                        },
                      )
                  }
                />

                <OperationalNumberField
                  label="Horizonte de reserva"
                  description="Días hacia el futuro en los que se puede reservar. Entre 1 y 730."
                  value={
                    operationalForm.bookingHorizonDays
                  }
                  min={
                    operationalNumberLimits.bookingHorizonDays.min
                  }
                  max={
                    operationalNumberLimits.bookingHorizonDays.max
                  }
                  disabled={
                    !canEdit
                    || operationalSaving
                  }
                  error={
                    operationalFieldErrors.bookingHorizonDays
                  }
                  onChange={
                    (
                      value,
                    ) =>
                      updateOperationalForm(
                        {
                          bookingHorizonDays:
                            value,
                        },
                      )
                  }
                />
              </div>
            </div>

            <div>
              <div className="text-xs font-semibold uppercase tracking-[0.14em] text-[#9a897d]">
                Huecos y márgenes
              </div>

              <div className="mt-4 grid gap-5 md:grid-cols-3">
                <OperationalNumberField
                  label="Intervalo entre huecos"
                  description="Minutos entre horas de inicio posibles. Entre 5 y 240."
                  value={
                    operationalForm.slotIntervalMinutes
                  }
                  min={
                    operationalNumberLimits.slotIntervalMinutes.min
                  }
                  max={
                    operationalNumberLimits.slotIntervalMinutes.max
                  }
                  disabled={
                    !canEdit
                    || operationalSaving
                  }
                  error={
                    operationalFieldErrors.slotIntervalMinutes
                  }
                  onChange={
                    (
                      value,
                    ) =>
                      updateOperationalForm(
                        {
                          slotIntervalMinutes:
                            value,
                        },
                      )
                  }
                />

                <OperationalNumberField
                  label="Margen antes de la cita"
                  description="Minutos de ocupación previa. Entre 0 y 240."
                  value={
                    operationalForm.defaultBufferBeforeMinutes
                  }
                  min={
                    operationalNumberLimits.defaultBufferBeforeMinutes.min
                  }
                  max={
                    operationalNumberLimits.defaultBufferBeforeMinutes.max
                  }
                  disabled={
                    !canEdit
                    || operationalSaving
                  }
                  error={
                    operationalFieldErrors.defaultBufferBeforeMinutes
                  }
                  onChange={
                    (
                      value,
                    ) =>
                      updateOperationalForm(
                        {
                          defaultBufferBeforeMinutes:
                            value,
                        },
                      )
                  }
                />

                <OperationalNumberField
                  label="Margen después de la cita"
                  description="Minutos de ocupación posterior. Entre 0 y 240."
                  value={
                    operationalForm.defaultBufferAfterMinutes
                  }
                  min={
                    operationalNumberLimits.defaultBufferAfterMinutes.min
                  }
                  max={
                    operationalNumberLimits.defaultBufferAfterMinutes.max
                  }
                  disabled={
                    !canEdit
                    || operationalSaving
                  }
                  error={
                    operationalFieldErrors.defaultBufferAfterMinutes
                  }
                  onChange={
                    (
                      value,
                    ) =>
                      updateOperationalForm(
                        {
                          defaultBufferAfterMinutes:
                            value,
                        },
                      )
                  }
                />
              </div>
            </div>

            <div className="space-y-4">
              <div className="text-xs font-semibold uppercase tracking-[0.14em] text-[#9a897d]">
                Confirmación, cancelación y reprogramación
              </div>

              <PolicyToggle
                title="Confirmar citas automáticamente"
                description="Si está activo, las nuevas reservas quedan confirmadas. Si no, pueden permanecer pendientes."
                active={
                  operationalForm.autoConfirmAppointments
                }
                disabled={
                  !canEdit
                  || operationalSaving
                }
                onClick={() =>
                  updateOperationalForm(
                    {
                      autoConfirmAppointments:
                        !operationalForm.autoConfirmAppointments,
                    },
                  )
                }
              />

              <PolicyToggle
                title="Permitir cancelación online"
                description="El cliente puede cancelar una cita desde el flujo público."
                active={
                  operationalForm.onlineCancellationEnabled
                }
                disabled={
                  !canEdit
                  || operationalSaving
                }
                onClick={() =>
                  updateOperationalForm(
                    {
                      onlineCancellationEnabled:
                        !operationalForm.onlineCancellationEnabled,
                    },
                  )
                }
              />

              <OperationalNumberField
                label="Antelación mínima para cancelar"
                description="Minutos de antelación exigidos para cancelar online. Entre 0 y 525600."
                value={
                  operationalForm.minimumCancellationNoticeMinutes
                }
                min={
                  operationalNumberLimits.minimumCancellationNoticeMinutes.min
                }
                max={
                  operationalNumberLimits.minimumCancellationNoticeMinutes.max
                }
                disabled={
                  !canEdit
                  || operationalSaving
                }
                error={
                  operationalFieldErrors.minimumCancellationNoticeMinutes
                }
                onChange={
                  (
                    value,
                  ) =>
                    updateOperationalForm(
                      {
                        minimumCancellationNoticeMinutes:
                          value,
                      },
                    )
                }
              />

              <PolicyToggle
                title="Permitir reprogramación online"
                description="El cliente puede cambiar la fecha de una cita desde el flujo público."
                active={
                  operationalForm.onlineReschedulingEnabled
                }
                disabled={
                  !canEdit
                  || operationalSaving
                }
                onClick={() =>
                  updateOperationalForm(
                    {
                      onlineReschedulingEnabled:
                        !operationalForm.onlineReschedulingEnabled,
                    },
                  )
                }
              />

              <OperationalNumberField
                label="Antelación mínima para reprogramar"
                description="Minutos de antelación exigidos para reprogramar online. Entre 0 y 525600."
                value={
                  operationalForm.minimumRescheduleNoticeMinutes
                }
                min={
                  operationalNumberLimits.minimumRescheduleNoticeMinutes.min
                }
                max={
                  operationalNumberLimits.minimumRescheduleNoticeMinutes.max
                }
                disabled={
                  !canEdit
                  || operationalSaving
                }
                error={
                  operationalFieldErrors.minimumRescheduleNoticeMinutes
                }
                onChange={
                  (
                    value,
                  ) =>
                    updateOperationalForm(
                      {
                        minimumRescheduleNoticeMinutes:
                          value,
                      },
                    )
                }
              />
            </div>

            <div className="space-y-4">
              <div className="text-xs font-semibold uppercase tracking-[0.14em] text-[#9a897d]">
                Recordatorios
              </div>

              <PolicyToggle
                title="Activar recordatorios"
                description="Controla el sistema general de recordatorios de citas."
                active={
                  operationalForm.remindersEnabled
                }
                disabled={
                  !canEdit
                  || operationalSaving
                }
                onClick={() =>
                  updateOperationalForm(
                    {
                      remindersEnabled:
                        !operationalForm.remindersEnabled,

                      secondReminderEnabled:
                        operationalForm.remindersEnabled
                          ? false
                          : operationalForm.secondReminderEnabled,
                    },
                  )
                }
              />

              <div className="grid gap-5 md:grid-cols-2">
                <OperationalNumberField
                  label="Primer recordatorio"
                  description="Minutos antes de la cita. Entre 1 y 525600."
                  value={
                    operationalForm.firstReminderMinutesBefore
                  }
                  min={
                    operationalNumberLimits.firstReminderMinutesBefore.min
                  }
                  max={
                    operationalNumberLimits.firstReminderMinutesBefore.max
                  }
                  disabled={
                    !canEdit
                    || operationalSaving
                  }
                  error={
                    operationalFieldErrors.firstReminderMinutesBefore
                  }
                  onChange={
                    (
                      value,
                    ) =>
                      updateOperationalForm(
                        {
                          firstReminderMinutesBefore:
                            value,
                        },
                      )
                  }
                />

                <OperationalNumberField
                  label="Segundo recordatorio"
                  description="Minutos antes de la cita. Debe quedar más cerca que el primero. Entre 1 y 525600."
                  value={
                    operationalForm.secondReminderMinutesBefore
                  }
                  min={
                    operationalNumberLimits.secondReminderMinutesBefore.min
                  }
                  max={
                    operationalNumberLimits.secondReminderMinutesBefore.max
                  }
                  disabled={
                    !canEdit
                    || operationalSaving
                  }
                  error={
                    operationalFieldErrors.secondReminderMinutesBefore
                  }
                  onChange={
                    (
                      value,
                    ) =>
                      updateOperationalForm(
                        {
                          secondReminderMinutesBefore:
                            value,
                        },
                      )
                  }
                />
              </div>

              <PolicyToggle
                title="Activar segundo recordatorio"
                description="Solo tiene efecto cuando los recordatorios generales están activos. Se envía más cerca de la cita que el primero."
                active={
                  operationalForm.remindersEnabled
                  && operationalForm.secondReminderEnabled
                }
                disabled={
                  !canEdit
                  || operationalSaving
                  || !operationalForm.remindersEnabled
                }
                onClick={() =>
                  updateOperationalForm(
                    {
                      secondReminderEnabled:
                        !operationalForm.secondReminderEnabled,
                    },
                  )
                }
              />
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <InfoCard
                label="Creada"
                value={formatDateTime(
                  operationalSettings.createdAt,
                )}
              />

              <InfoCard
                label="Actualizada"
                value={formatDateTime(
                  operationalSettings.updatedAt,
                )}
              />
            </div>

            <div className="flex flex-wrap gap-3">
              <button
                type="button"
                onClick={
                  resetOperationalForm
                }
                disabled={
                  !operationalDirty
                  || operationalSaving
                }
                className="rounded-full border border-[#d8cec6] bg-white px-5 py-3 text-sm font-semibold text-[#62564f] transition hover:bg-[#f7f3ef] disabled:cursor-not-allowed disabled:opacity-40"
              >
                Descartar cambios
              </button>

              <button
                type="button"
                onClick={() =>
                  void saveOperationalSettings()
                }
                disabled={
                  !operationalDirty
                  || operationalSaving
                  || !canEdit
                }
                className="rounded-full bg-[#342d28] px-5 py-3 text-sm font-semibold text-white transition hover:bg-[#211c18] disabled:cursor-not-allowed disabled:opacity-40"
              >
                {operationalSaving
                  ? "Guardando..."
                  : "Guardar recordatorios y reservas"}
              </button>
            </div>
          </div>
        )}
      </SettingsSection>

      {/* ===================================================
          SYSTEM INFO
      =================================================== */}

      <SettingsSection
        title="Información del sistema"
        description="Datos internos de referencia del Business."
      >
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">

          <InfoCard
            label="Business ID"
            value={String(
              business.id,
            )}
          />

          <InfoCard
            label="Rol actual"
            value={
              session?.role
              ?? "—"
            }
          />

          <InfoCard
            label="Creado"
            value={formatDateTime(
              business.createdAt,
            )}
          />

          <InfoCard
            label="Actualizado"
            value={formatDateTime(
              business.updatedAt,
            )}
          />
        </div>
      </SettingsSection>

      {/* ===================================================
          STICKY SAVE BAR
      =================================================== */}

      {dirty
      && canEdit
      && (
        <div className="sticky bottom-4 z-20">

          <div className="mx-auto flex max-w-3xl flex-col gap-3 rounded-[22px] border border-[#d9cfc7] bg-[#fffdfa]/95 p-4 shadow-[0_20px_60px_rgba(55,45,38,0.16)] backdrop-blur sm:flex-row sm:items-center sm:justify-between">

            <div>
              <div className="text-sm font-semibold text-[#382f2a]">
                Tienes cambios sin guardar
              </div>

              <div className="mt-1 text-xs text-[#8c7c71]">
                Guarda los cambios antes de abandonar esta sección.
              </div>
            </div>

            <div className="flex gap-2">

              <button
                type="button"
                onClick={
                  resetForm
                }
                disabled={
                  saving
                }
                className="rounded-full border border-[#d8cec6] bg-white px-4 py-2 text-xs font-semibold text-[#665950]"
              >
                Descartar
              </button>

              <button
                type="button"
                onClick={() =>
                  void saveSettings()
                }
                disabled={
                  saving
                }
                className="rounded-full bg-[#342d28] px-4 py-2 text-xs font-semibold text-white disabled:opacity-50"
              >
                {saving
                  ? "Guardando..."
                  : "Guardar"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

/* =========================================================
   STYLES
========================================================= */

const inputClassName =
  "w-full rounded-2xl border border-[#ddd3cb] bg-white px-4 py-3 text-sm text-[#352f2b] outline-none transition placeholder:text-[#aaa097] focus:border-[#b9a89c] focus:ring-2 focus:ring-[#ded3cb]/50 disabled:cursor-not-allowed disabled:bg-[#f2eeea] disabled:text-[#877970]";

/* =========================================================
   SECTION
========================================================= */

type SettingsSectionProps = {
  title: string;
  description: string;
  children: React.ReactNode;
};

function SettingsSection({
  title,
  description,
  children,
}: SettingsSectionProps) {

  return (
    <section className="rounded-[26px] border border-[#e3dbd3] bg-white shadow-[0_10px_35px_rgba(62,49,41,0.04)]">

      <div className="border-b border-[#eee7e1] px-6 py-5 md:px-7">
        <h2 className="text-lg font-semibold text-[#312b27]">
          {title}
        </h2>

        <p className="mt-1 text-sm leading-6 text-[#8a7b71]">
          {description}
        </p>
      </div>

      <div className="p-6 md:p-7">
        {children}
      </div>
    </section>
  );
}

/* =========================================================
   FIELD
========================================================= */

type FieldProps = {
  label: string;
  required?: boolean;
  description?: string;
  error?: string;
  className?: string;
  children: React.ReactNode;
};

function Field({
  label,
  required = false,
  description,
  error,
  className = "",
  children,
}: FieldProps) {

  return (
    <label className={`block ${className}`}>

      <span className="text-xs font-semibold uppercase tracking-[0.12em] text-[#8e7e73]">
        {label}

        {required && (
          <span className="ml-1 text-[#9c6969]">
            *
          </span>
        )}
      </span>

      <div className="mt-2">
        {children}
      </div>

      {description && (
        <p className="mt-2 text-xs leading-5 text-[#998a80]">
          {description}
        </p>
      )}

      {error && (
        <p className="mt-2 text-xs leading-5 text-[#9c6969]">
          {error}
        </p>
      )}
    </label>
  );
}

/* =========================================================
   STATUS CARD
========================================================= */

type StatusCardProps = {
  label: string;
  value: string;
  description: string;
};

function StatusCard({
  label,
  value,
  description,
}: StatusCardProps) {

  return (
    <article className="rounded-[22px] border border-[#e5ddd5] bg-white p-5 shadow-[0_8px_28px_rgba(62,49,41,0.035)]">

      <div className="text-xs font-semibold uppercase tracking-[0.14em] text-[#9a897d]">
        {label}
      </div>

      <div className="mt-3 text-lg font-semibold text-[#342e2a]">
        {value}
      </div>

      <div className="mt-1 text-xs text-[#97887e]">
        {description}
      </div>
    </article>
  );
}

/* =========================================================
   INFO CARD
========================================================= */

type InfoCardProps = {
  label: string;
  value: string;
};

function InfoCard({
  label,
  value,
}: InfoCardProps) {

  return (
    <div className="rounded-2xl border border-[#e5ddd5] bg-[#faf8f5] p-4">

      <div className="text-[10px] font-semibold uppercase tracking-[0.14em] text-[#9a897d]">
        {label}
      </div>

      <div className="mt-2 break-words text-sm font-semibold text-[#4a4039]">
        {value}
      </div>
    </div>
  );
}

/* =========================================================
   COLOR FIELD
========================================================= */

type ColorFieldProps = {
  label: string;
  value: string;
  fallback: string;
  disabled: boolean;
  onChange: (
    value: string,
  ) => void;
};

function ColorField({
  label,
  value,
  fallback,
  disabled,
  onChange,
}: ColorFieldProps) {

  const resolvedColor =
    normalizeHexColor(
      value,
      fallback,
    );

  return (
    <Field
      label={
        label
      }
      description="Formato hexadecimal #RRGGBB."
    >
      <div className="flex gap-3">

        <input
          type="color"
          value={
            resolvedColor
          }
          disabled={
            disabled
          }
          onChange={
            (
              event,
            ) =>
              onChange(
                event.target.value.toUpperCase(),
              )
          }
          className="h-[46px] w-14 shrink-0 cursor-pointer rounded-xl border border-[#ddd3cb] bg-white p-1 disabled:cursor-not-allowed disabled:opacity-60"
        />

        <input
          type="text"
          value={
            value
          }
          maxLength={
            7
          }
          disabled={
            disabled
          }
          onChange={
            (
              event,
            ) =>
              onChange(
                event.target.value,
              )
          }
          placeholder="#342D28"
          className={inputClassName}
        />
      </div>
    </Field>
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

type OperationalNumberFieldProps = {
  label: string;
  description: string;
  value: string;
  min: number;
  max: number;
  disabled: boolean;
  error?: string;
  onChange: (
    value: string,
  ) => void;
};

function OperationalNumberField({
  label,
  description,
  value,
  min,
  max,
  disabled,
  error,
  onChange,
}: OperationalNumberFieldProps) {

  return (
    <Field
      label={
        label
      }
      description={
        description
      }
      error={
        error
      }
      required
    >
      <input
        type="number"
        inputMode="numeric"
        min={
          min
        }
        max={
          max
        }
        step={
          1
        }
        value={
          value
        }
        disabled={
          disabled
        }
        aria-invalid={
          Boolean(
            error,
          )
        }
        onChange={
          (
            event,
          ) =>
            onChange(
              event.target.value,
            )
        }
        className={inputClassName}
      />
    </Field>
  );
}

type PolicyToggleProps = {
  title: string;
  description: string;
  active: boolean;
  disabled: boolean;
  onClick: () => void;
};

function PolicyToggle({
  title,
  description,
  active,
  disabled,
  onClick,
}: PolicyToggleProps) {

  return (
    <div className="flex flex-col gap-5 rounded-[22px] border border-[#e5ddd5] bg-[#f8f5f1] p-5 sm:flex-row sm:items-center sm:justify-between">

      <div>
        <div className="text-sm font-semibold text-[#342e2a]">
          {title}
        </div>

        <p className="mt-1 max-w-2xl text-sm leading-6 text-[#82746b]">
          {description}
        </p>
      </div>

      <ToggleSwitch
        active={
          active
        }
        disabled={
          disabled
        }
        onClick={
          onClick
        }
      />
    </div>
  );
}

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
        "relative h-7 w-12 shrink-0 rounded-full transition disabled:cursor-not-allowed disabled:opacity-55",

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