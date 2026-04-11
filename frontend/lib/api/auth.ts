import type { UserDTO, LoginResponse } from "@/types";
import { BASE_URL, fetchJsonAuth } from "./client";

const BASE = BASE_URL;

export async function loginWithGoogle(credential: string): Promise<LoginResponse> {
  const res = await fetch(`${BASE}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ credential }),
  });
  if (!res.ok) {
    const body = await res.text();
    throw new Error(`API error ${res.status}: ${body}`);
  }
  return res.json();
}

export function getAuthMe(): Promise<UserDTO> {
  return fetchJsonAuth(`${BASE}/auth/me`);
}

export function getUsers(): Promise<UserDTO[]> {
  return fetchJsonAuth(`${BASE}/auth/users`);
}

export function createUser(email: string, displayName: string, role: string): Promise<UserDTO> {
  return fetchJsonAuth(`${BASE}/auth/users`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, displayName, role }),
  });
}

export function updateUserRole(id: number, role: string): Promise<{ ok: boolean }> {
  return fetchJsonAuth(`${BASE}/auth/users/${id}/role`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ role }),
  });
}

export function deleteUser(id: number): Promise<{ ok: boolean }> {
  return fetchJsonAuth(`${BASE}/auth/users/${id}`, { method: "DELETE" });
}
