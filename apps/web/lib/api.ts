const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL?.trim()
  || "http://localhost:8080";

/* =========================================================
   STORAGE KEYS
========================================================= */

const ACCESS_TOKEN_STORAGE_KEY =
  "wacds_access_token";

const REFRESH_TOKEN_STORAGE_KEY =
  "wacds_refresh_token";

const BUSINESS_SESSION_STORAGE_KEY =
  "wacds_business_session";

/**
 * Renovamos preventivamente el access token cuando
 * quedan 30 segundos o menos para su expiración.
 *
 * Esto evita que una acción de usuario tenga que:
 *
 * request -> 401 -> refresh -> retry
 *
 * si ya sabemos de antemano que el JWT está expirado
 * o a punto de expirar.
 */
const ACCESS_TOKEN_REFRESH_MARGIN_MS =
  30_000;

/* =========================================================
   TYPES
========================================================= */

export type ApiRequestOptions =
  Omit<RequestInit, "body"> & {
    body?: unknown;

    /**
     * Token explícito opcional.
     *
     * En navegador, el token vigente de localStorage
     * tendrá prioridad para evitar reutilizar JWT antiguos
     * capturados anteriormente por un componente.
     */
    accessToken?: string | null;

    /**
     * Desactiva el refresh automático.
     *
     * Se utiliza para endpoints como:
     *
     * - /auth/login
     * - /auth/refresh
     */
    skipAuthRefresh?: boolean;
  };

type TokenResponse = {
  accessToken: string;
  refreshToken: string;

  tokenType: string;

  accessTokenExpiresAt: string;
  refreshTokenExpiresAt: string;
};

type StoredBusinessSession = {
  userId?: number;

  email?: string;

  firstName?: string | null;
  lastName?: string | null;
  fullName?: string | null;

  businessId?: number;
  businessName?: string;

  role?: string;

  accessTokenExpiresAt?: string;
  refreshTokenExpiresAt?: string;

  [key: string]: unknown;
};

type PerformedRequest = {
  response: Response;

  /**
   * Access token realmente utilizado en esta request.
   *
   * Nos permite detectar si otra request concurrente
   * ya renovó la sesión mientras esta recibía un 401.
   */
  accessTokenUsed: string | null;
};

/* =========================================================
   API ERROR
========================================================= */

export class ApiError extends Error {

  readonly status: number;

  readonly statusText: string;

  readonly payload: unknown;

  constructor(
    message: string,
    status: number,
    statusText: string,
    payload: unknown,
  ) {

    super(
      message,
    );

    this.name =
      "ApiError";

    this.status =
      status;

    this.statusText =
      statusText;

    this.payload =
      payload;
  }
}

/* =========================================================
   BROWSER
========================================================= */

function isBrowser(): boolean {

  return (
    typeof window !== "undefined"
  );
}

/* =========================================================
   STORAGE
========================================================= */

function getStoredAccessToken():
  | string
  | null {

  if (
    !isBrowser()
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

function getStoredRefreshToken():
  | string
  | null {

  if (
    !isBrowser()
  ) {
    return null;
  }

  const token =
    window.localStorage.getItem(
      REFRESH_TOKEN_STORAGE_KEY,
    );

  if (
    token == null
    || token.trim() === ""
  ) {
    return null;
  }

  return token.trim();
}

function getStoredBusinessSession():
  | StoredBusinessSession
  | null {

  if (
    !isBrowser()
  ) {
    return null;
  }

  const rawSession =
    window.localStorage.getItem(
      BUSINESS_SESSION_STORAGE_KEY,
    );

  if (
    !rawSession
  ) {
    return null;
  }

  try {

    const parsed =
      JSON.parse(
        rawSession,
      );

    if (
      typeof parsed !== "object"
      || parsed === null
    ) {
      return null;
    }

    return parsed as StoredBusinessSession;

  } catch {

    return null;
  }
}

function updateStoredBusinessSession(
  tokenResponse: TokenResponse,
): void {

  if (
    !isBrowser()
  ) {
    return;
  }

  const session =
    getStoredBusinessSession();

  if (
    !session
  ) {
    return;
  }

  const updatedSession: StoredBusinessSession = {
    ...session,

    accessTokenExpiresAt:
      tokenResponse.accessTokenExpiresAt,

    refreshTokenExpiresAt:
      tokenResponse.refreshTokenExpiresAt,
  };

  window.localStorage.setItem(
    BUSINESS_SESSION_STORAGE_KEY,
    JSON.stringify(
      updatedSession,
    ),
  );
}

function persistTokens(
  tokenResponse: TokenResponse,
): void {

  if (
    !isBrowser()
  ) {
    return;
  }

  window.localStorage.setItem(
    ACCESS_TOKEN_STORAGE_KEY,
    tokenResponse.accessToken,
  );

  window.localStorage.setItem(
    REFRESH_TOKEN_STORAGE_KEY,
    tokenResponse.refreshToken,
  );

  updateStoredBusinessSession(
    tokenResponse,
  );
}

export function clearBusinessSession(): void {

  if (
    !isBrowser()
  ) {
    return;
  }

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

/* =========================================================
   REDIRECT
========================================================= */

function redirectToBusinessLogin(): void {

  if (
    !isBrowser()
  ) {
    return;
  }

  const currentPath =
    window.location.pathname;

  if (
    currentPath === "/business/login"
    || currentPath === "/business/login/"
  ) {
    return;
  }

  window.location.assign(
    new URL(
      "/business/login",
      window.location.origin,
    ).href,
  );
}

/* =========================================================
   RESPONSE PARSING
========================================================= */

async function parseResponseBody(
  response: Response,
): Promise<unknown> {

  const contentType =
    response.headers.get(
      "content-type",
    );

  if (
    response.status === 204
  ) {
    return null;
  }

  if (
    contentType?.includes(
      "application/json",
    )
  ) {

    try {

      return await response.json();

    } catch {

      return null;
    }
  }

  try {

    const text =
      await response.text();

    return (
      text
      || null
    );

  } catch {

    return null;
  }
}

function resolveErrorMessage(
  payload: unknown,
  response: Response,
): string {

  if (
    typeof payload === "object"
    && payload !== null
  ) {

    const candidate =
      payload as Record<
        string,
        unknown
      >;

    const possibleMessages = [
      candidate.message,
      candidate.error,
      candidate.detail,
      candidate.title,
    ];

    for (
      const possibleMessage
      of possibleMessages
    ) {

      if (
        typeof possibleMessage === "string"
        && possibleMessage.trim() !== ""
      ) {
        return possibleMessage.trim();
      }
    }
  }

  if (
    typeof payload === "string"
    && payload.trim() !== ""
  ) {
    return payload.trim();
  }

  return (
    `Error HTTP ${response.status}`
    + (
      response.statusText
        ? ` ${response.statusText}`
        : ""
    )
  );
}

async function createApiError(
  response: Response,
): Promise<ApiError> {

  const payload =
    await parseResponseBody(
      response,
    );

  return new ApiError(
    resolveErrorMessage(
      payload,
      response,
    ),
    response.status,
    response.statusText,
    payload,
  );
}

/* =========================================================
   TOKEN EXPIRATION
========================================================= */

function getStoredAccessTokenExpiresAt():
  | string
  | null {

  const session =
    getStoredBusinessSession();

  const expiresAt =
    session?.accessTokenExpiresAt;

  if (
    typeof expiresAt !== "string"
    || expiresAt.trim() === ""
  ) {
    return null;
  }

  return expiresAt.trim();
}

function isAccessTokenNearExpiration(): boolean {

  if (
    !isBrowser()
  ) {
    return false;
  }

  const accessToken =
    getStoredAccessToken();

  const refreshToken =
    getStoredRefreshToken();

  const expiresAt =
    getStoredAccessTokenExpiresAt();

  if (
    !accessToken
    || !refreshToken
    || !expiresAt
  ) {
    return false;
  }

  const expirationTimestamp =
    Date.parse(
      expiresAt,
    );

  if (
    Number.isNaN(
      expirationTimestamp,
    )
  ) {
    return false;
  }

  return (
    Date.now()
    + ACCESS_TOKEN_REFRESH_MARGIN_MS
    >= expirationTimestamp
  );
}

/* =========================================================
   TOKEN RESOLUTION
========================================================= */

/**
 * Orden de prioridad:
 *
 * 1. override interno usado durante retry;
 * 2. token vigente de localStorage;
 * 3. token explícito del caller.
 *
 * La diferencia importante respecto a la versión anterior
 * es que, en navegador, un componente no puede seguir
 * imponiendo accidentalmente un JWT antiguo después de que
 * otra petición haya renovado la sesión.
 */
function resolveRequestAccessToken(
  options: ApiRequestOptions,
  accessTokenOverride?:
    | string
    | null,
):
  | string
  | null {

  if (
    accessTokenOverride !== undefined
  ) {
    return accessTokenOverride;
  }

  const storedAccessToken =
    getStoredAccessToken();

  if (
    storedAccessToken
  ) {
    return storedAccessToken;
  }

  const explicitAccessToken =
    options.accessToken;

  if (
    explicitAccessToken
    && explicitAccessToken.trim() !== ""
  ) {
    return explicitAccessToken.trim();
  }

  return null;
}

/* =========================================================
   HEADERS
========================================================= */

function buildHeaders(
  options: ApiRequestOptions,
  accessToken:
    | string
    | null,
): Headers {

  const headers =
    new Headers(
      options.headers,
    );

  if (
    options.body !== undefined
    && !headers.has(
      "Content-Type",
    )
  ) {
    headers.set(
      "Content-Type",
      "application/json",
    );
  }

  /**
   * Importante:
   *
   * Si existe un token resuelto, sustituimos SIEMPRE
   * Authorization.
   *
   * Esto permite que el retry posterior al refresh
   * reemplace cualquier bearer antiguo que hubiese llegado
   * mediante options.headers.
   */
  if (
    accessToken
  ) {
    headers.set(
      "Authorization",
      `Bearer ${accessToken}`,
    );
  }

  if (
    !headers.has(
      "Accept",
    )
  ) {
    headers.set(
      "Accept",
      "application/json",
    );
  }

  return headers;
}

/* =========================================================
   REQUEST CREATION
========================================================= */

async function performRequest(
  url: string,
  options: ApiRequestOptions,
  accessTokenOverride?:
    | string
    | null,
): Promise<PerformedRequest> {

  const {
    body,
    ...requestInit
  } =
    options;

  delete requestInit.accessToken;
  delete requestInit.skipAuthRefresh;

  const accessToken =
    resolveRequestAccessToken(
      options,
      accessTokenOverride,
    );

  const response =
    await fetch(
      url,
      {
        ...requestInit,

        headers:
          buildHeaders(
            options,
            accessToken,
          ),

        body:
          body === undefined
            ? undefined
            : JSON.stringify(
                body,
              ),

        cache:
          options.cache
          ?? "no-store",
      },
    );

  return {
    response,
    accessTokenUsed:
      accessToken,
  };
}

/* =========================================================
   TOKEN RESPONSE VALIDATION
========================================================= */

function isValidTokenResponse(
  value: unknown,
): value is TokenResponse {

  if (
    typeof value !== "object"
    || value === null
  ) {
    return false;
  }

  const candidate =
    value as Record<
      string,
      unknown
    >;

  return (
    typeof candidate.accessToken === "string"
    && candidate.accessToken.trim() !== ""

    && typeof candidate.refreshToken === "string"
    && candidate.refreshToken.trim() !== ""

    && typeof candidate.tokenType === "string"

    && typeof candidate.accessTokenExpiresAt === "string"
    && candidate.accessTokenExpiresAt.trim() !== ""

    && typeof candidate.refreshTokenExpiresAt === "string"
    && candidate.refreshTokenExpiresAt.trim() !== ""
  );
}

/* =========================================================
   REFRESH TOKEN
========================================================= */

/**
 * Evita refresh paralelos.
 *
 * Si cinco requests reciben 401 al mismo tiempo,
 * todas esperan a esta misma Promise.
 */
let refreshPromise:
  | Promise<string>
  | null =
    null;

async function executeTokenRefresh():
  Promise<string> {

  const refreshToken =
    getStoredRefreshToken();

  if (
    !refreshToken
  ) {
    throw new ApiError(
      "No existe un refresh token válido.",
      401,
      "Unauthorized",
      null,
    );
  }

  const url =
    `${API_BASE_URL}/api/v1/auth/refresh`;

  const response =
    await fetch(
      url,
      {
        method:
          "POST",

        headers: {
          Accept:
            "application/json",

          "Content-Type":
            "application/json",
        },

        body:
          JSON.stringify(
            {
              refreshToken,
            },
          ),

        cache:
          "no-store",
      },
    );

  if (
    !response.ok
  ) {
    throw await createApiError(
      response,
    );
  }

  const payload =
    await parseResponseBody(
      response,
    );

  if (
    !isValidTokenResponse(
      payload,
    )
  ) {
    throw new ApiError(
      "La respuesta de renovación de sesión no es válida.",
      500,
      "Invalid refresh response",
      payload,
    );
  }

  persistTokens(
    payload,
  );

  return payload.accessToken;
}

async function refreshAccessToken():
  Promise<string> {

  if (
    !isBrowser()
  ) {
    throw new ApiError(
      "La renovación automática requiere una sesión de navegador.",
      401,
      "Unauthorized",
      null,
    );
  }

  if (
    refreshPromise
  ) {
    return refreshPromise;
  }

  refreshPromise =
    executeTokenRefresh()
      .finally(
        () => {

          refreshPromise =
            null;
        },
      );

  return refreshPromise;
}

/* =========================================================
   AUTH ENDPOINTS
========================================================= */

function isAuthenticationEndpoint(
  normalizedPath: string,
): boolean {

  return (
    normalizedPath
    === "/api/v1/auth/login"

    || normalizedPath
    === "/api/v1/auth/refresh"
  );
}

/* =========================================================
   REFRESH RULES
========================================================= */

function canAttemptAutomaticRefresh(
  normalizedPath: string,
  options: ApiRequestOptions,
): boolean {

  if (
    options.skipAuthRefresh
  ) {
    return false;
  }

  if (
    isAuthenticationEndpoint(
      normalizedPath,
    )
  ) {
    return false;
  }

  if (
    !isBrowser()
  ) {
    return false;
  }

  return true;
}

function shouldAttemptRefreshAfterResponse(
  normalizedPath: string,
  options: ApiRequestOptions,
  response: Response,
): boolean {

  return (
    response.status === 401
    && canAttemptAutomaticRefresh(
      normalizedPath,
      options,
    )
  );
}

function shouldDestroySessionAfterRefreshError(
  error: unknown,
): boolean {

  if (
    !(error instanceof ApiError)
  ) {

    /**
     * Ejemplos:
     *
     * - backend apagado;
     * - corte de red;
     * - error de DNS.
     *
     * No eliminamos una sesión válida por un problema
     * temporal de conectividad.
     */
    return false;
  }

  return (
    error.status === 400
    || error.status === 401
    || error.status === 403
  );
}

function destroySessionAndRedirect(): void {

  clearBusinessSession();

  redirectToBusinessLogin();
}

/* =========================================================
   PREEMPTIVE REFRESH
========================================================= */

/**
 * Intenta renovar antes de lanzar la request cuando sabemos
 * que el access token ya está expirado o a punto de hacerlo.
 *
 * Si la renovación falla por un problema de red o un 5xx,
 * dejamos que la request original continúe.
 *
 * Si el refresh token es inválido (400/401/403),
 * destruimos la sesión.
 */
async function attemptPreemptiveRefresh(
  normalizedPath: string,
  options: ApiRequestOptions,
): Promise<void> {

  if (
    !canAttemptAutomaticRefresh(
      normalizedPath,
      options,
    )
  ) {
    return;
  }

  if (
    !isAccessTokenNearExpiration()
  ) {
    return;
  }

  try {

    await refreshAccessToken();

  } catch (
    refreshError
  ) {

    if (
      shouldDestroySessionAfterRefreshError(
        refreshError,
      )
    ) {

      destroySessionAndRedirect();

      throw refreshError;
    }

    /**
     * Si ha sido un fallo temporal de red/servidor,
     * permitimos que continúe la request normal.
     */
  }
}

/* =========================================================
   401 RECOVERY
========================================================= */

/**
 * Si una request recibe 401, antes de lanzar otro refresh
 * comprobamos si otra request concurrente ya cambió el JWT.
 *
 * Esto es especialmente importante porque el backend rota
 * también el refresh token.
 */
async function recoverUnauthorizedRequest(
  url: string,
  normalizedPath: string,
  options: ApiRequestOptions,
  failedRequest: PerformedRequest,
): Promise<PerformedRequest> {

  if (
    !shouldAttemptRefreshAfterResponse(
      normalizedPath,
      options,
      failedRequest.response,
    )
  ) {
    return failedRequest;
  }

  const latestStoredAccessToken =
    getStoredAccessToken();

  /**
   * Otra request pudo renovar la sesión mientras esta estaba
   * esperando la respuesta 401.
   *
   * En ese caso primero probamos el JWT nuevo ya almacenado,
   * evitando ejecutar un segundo refresh innecesario.
   */
  if (
    latestStoredAccessToken
    && latestStoredAccessToken
      !== failedRequest.accessTokenUsed
  ) {

    const retryWithLatestToken =
      await performRequest(
        url,
        options,
        latestStoredAccessToken,
      );

    if (
      retryWithLatestToken.response.status
      !== 401
    ) {
      return retryWithLatestToken;
    }
  }

  try {

    const newAccessToken =
      await refreshAccessToken();

    const retriedRequest =
      await performRequest(
        url,
        options,
        newAccessToken,
      );

    if (
      retriedRequest.response.status === 401
    ) {

      destroySessionAndRedirect();
    }

    return retriedRequest;

  } catch (
    refreshError
  ) {

    if (
      shouldDestroySessionAfterRefreshError(
        refreshError,
      )
    ) {

      destroySessionAndRedirect();
    }

    throw refreshError;
  }
}

/* =========================================================
   API FETCH
========================================================= */

export async function apiFetch<T>(
  path: string,
  options: ApiRequestOptions = {},
): Promise<T> {

  const normalizedPath =
    path.startsWith("/")
      ? path
      : `/${path}`;

  const url =
    `${API_BASE_URL}${normalizedPath}`;

  /* -------------------------------------------------------
     PREEMPTIVE REFRESH
  ------------------------------------------------------- */

  await attemptPreemptiveRefresh(
    normalizedPath,
    options,
  );

  /* -------------------------------------------------------
     FIRST REQUEST
  ------------------------------------------------------- */

  let performedRequest =
    await performRequest(
      url,
      options,
    );

  /* -------------------------------------------------------
     401 RECOVERY
  ------------------------------------------------------- */

  if (
    performedRequest.response.status
    === 401
  ) {

    performedRequest =
      await recoverUnauthorizedRequest(
        url,
        normalizedPath,
        options,
        performedRequest,
      );
  }

  /* -------------------------------------------------------
     FINAL RESPONSE
  ------------------------------------------------------- */

  const response =
    performedRequest.response;

  const payload =
    await parseResponseBody(
      response,
    );

  if (
    !response.ok
  ) {
    throw new ApiError(
      resolveErrorMessage(
        payload,
        response,
      ),
      response.status,
      response.statusText,
      payload,
    );
  }

  return payload as T;
}

/* =========================================================
   GET
========================================================= */

export function apiGet<T>(
  path: string,
  options: Omit<
    ApiRequestOptions,
    "method" | "body"
  > = {},
): Promise<T> {

  return apiFetch<T>(
    path,
    {
      ...options,

      method:
        "GET",
    },
  );
}

/* =========================================================
   POST
========================================================= */

export function apiPost<TResponse>(
  path: string,
  body?: unknown,
  options: Omit<
    ApiRequestOptions,
    "method" | "body"
  > = {},
): Promise<TResponse> {

  return apiFetch<TResponse>(
    path,
    {
      ...options,

      method:
        "POST",

      body,
    },
  );
}

/* =========================================================
   PUT
========================================================= */

export function apiPut<TResponse>(
  path: string,
  body?: unknown,
  options: Omit<
    ApiRequestOptions,
    "method" | "body"
  > = {},
): Promise<TResponse> {

  return apiFetch<TResponse>(
    path,
    {
      ...options,

      method:
        "PUT",

      body,
    },
  );
}

/* =========================================================
   PATCH
========================================================= */

export function apiPatch<TResponse>(
  path: string,
  body?: unknown,
  options: Omit<
    ApiRequestOptions,
    "method" | "body"
  > = {},
): Promise<TResponse> {

  return apiFetch<TResponse>(
    path,
    {
      ...options,

      method:
        "PATCH",

      body,
    },
  );
}

/* =========================================================
   DELETE
========================================================= */

export function apiDelete<TResponse>(
  path: string,
  options: Omit<
    ApiRequestOptions,
    "method" | "body"
  > = {},
): Promise<TResponse> {

  return apiFetch<TResponse>(
    path,
    {
      ...options,

      method:
        "DELETE",
    },
  );
}

/* =========================================================
   API CONFIG
========================================================= */

export function getApiBaseUrl():
  string {

  return API_BASE_URL;
}

/* =========================================================
   SESSION HELPERS
========================================================= */

export function getCurrentAccessToken():
  | string
  | null {

  return getStoredAccessToken();
}

export function getCurrentRefreshToken():
  | string
  | null {

  return getStoredRefreshToken();
}