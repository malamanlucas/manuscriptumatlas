const BASE = "/api";
export { BASE as BASE_URL };

export async function fetchJson<T>(url: string): Promise<T> {
  const res = await fetch(url);
  if (!res.ok) {
    const body = await res.text();
    throw new Error(`API error ${res.status}: ${body}`);
  }
  return res.json();
}

// ── Token storage (localStorage primary + cookie fallback) ──

const TOKEN_KEY = "observatory_token";

function getCookie(name: string): string | null {
  if (typeof document === "undefined") return null;
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`));
  return match ? decodeURIComponent(match[1]) : null;
}

function setCookie(name: string, value: string, maxAge: number) {
  if (typeof document === "undefined") return;
  const secure = window.location.protocol === "https:" ? "; Secure" : "";
  document.cookie = `${name}=${encodeURIComponent(value)}; max-age=${maxAge}; path=/; SameSite=Strict${secure}`;
}

function deleteCookie(name: string) {
  if (typeof document === "undefined") return;
  document.cookie = `${name}=; max-age=0; path=/; SameSite=Strict`;
}

export function getAuthToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY) ?? getCookie(TOKEN_KEY);
}

export function setAuthToken(token: string, maxAge: number) {
  if (typeof window === "undefined") return;
  localStorage.setItem(TOKEN_KEY, token);
  setCookie(TOKEN_KEY, token, maxAge);
}

export function clearAuthToken() {
  if (typeof window === "undefined") return;
  localStorage.removeItem(TOKEN_KEY);
  deleteCookie(TOKEN_KEY);
}

// ── Auth error class ──

export class AuthError extends Error {
  constructor(public status: number, message: string) {
    super(message);
    this.name = "AuthError";
  }
}

// ── Authenticated fetch ──

export async function fetchJsonAuth<T>(url: string, init?: RequestInit): Promise<T> {
  const token = getAuthToken();
  const headers: Record<string, string> = { ...(init?.headers as Record<string, string>) };
  if (token) headers["Authorization"] = `Bearer ${token}`;

  const res = await fetch(url, { ...init, headers });

  if (res.status === 401) {
    clearAuthToken();
    throw new AuthError(401, "Authentication required");
  }
  if (res.status === 403) {
    let msg = "Access denied";
    try {
      const body = await res.json();
      if (body?.message) msg = body.message;
    } catch { /* use default */ }
    throw new AuthError(403, msg);
  }
  if (!res.ok) {
    const body = await res.text();
    throw new Error(`API error ${res.status}: ${body}`);
  }
  return res.json();
}

// ── Shared param builder ──

export function buildParams(params: Record<string, string | undefined>): string {
  const sp = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== "") sp.set(k, v);
  }
  const str = sp.toString();
  return str ? `?${str}` : "";
}
