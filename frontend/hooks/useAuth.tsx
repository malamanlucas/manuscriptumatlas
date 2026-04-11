"use client";

import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  type ReactNode,
} from "react";
import { jwtDecode } from "jwt-decode";
import type { UserDTO } from "@/types";
import {
  getAuthMe,
  getAuthToken,
  setAuthToken,
  clearAuthToken,
  loginWithGoogle,
  AuthError,
} from "@/lib/api";

type AuthStatus = "loading" | "authenticated" | "unauthenticated";

interface AuthContextValue {
  user: UserDTO | null;
  status: AuthStatus;
  isAuthenticated: boolean;
  isAdmin: boolean;
  loginError: string | null;
  expiresAt: number | null;
  login: (credential: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue>({
  user: null,
  status: "loading",
  isAuthenticated: false,
  isAdmin: false,
  loginError: null,
  expiresAt: null,
  login: async () => {},
  logout: () => {},
});

const SEVEN_DAYS_SECONDS = 7 * 24 * 60 * 60;

export function useAuth() {
  return useContext(AuthContext);
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserDTO | null>(null);
  const [status, setStatus] = useState<AuthStatus>("loading");
  const [loginError, setLoginError] = useState<string | null>(null);
  const [expiresAt, setExpiresAt] = useState<number | null>(null);

  const hydrate = useCallback(async () => {
    const token = getAuthToken();
    if (!token) {
      setStatus("unauthenticated");
      return;
    }

    try {
      const decoded = jwtDecode<{ exp: number }>(token);
      if (decoded.exp * 1000 < Date.now()) {
        clearAuthToken();
        setStatus("unauthenticated");
        return;
      }
      setExpiresAt(decoded.exp * 1000);
    } catch {
      clearAuthToken();
      setStatus("unauthenticated");
      return;
    }

    try {
      const me = await getAuthMe();
      setUser(me);
      setStatus("authenticated");
      setLoginError(null);
    } catch (err) {
      if (err instanceof AuthError && (err.status === 401 || err.status === 403)) {
        clearAuthToken();
      }
      setStatus("unauthenticated");
      setExpiresAt(null);
    }
  }, []);

  useEffect(() => {
    hydrate();
  }, [hydrate]);

  const login = useCallback(async (credential: string) => {
    setStatus("loading");
    setLoginError(null);

    try {
      const { token, user: loggedUser } = await loginWithGoogle(credential);
      setAuthToken(token, SEVEN_DAYS_SECONDS);
      try {
        const decoded = jwtDecode<{ exp: number }>(token);
        setExpiresAt(decoded.exp * 1000);
      } catch { /* ignore */ }
      setUser(loggedUser);
      setStatus("authenticated");
    } catch (err) {
      clearAuthToken();
      setUser(null);
      setStatus("unauthenticated");
      const msg = err instanceof AuthError
        ? `Error ${err.status}: ${err.message}`
        : err instanceof Error ? err.message : "Login failed";
      setLoginError(msg);
    }
  }, []);

  const logout = useCallback(() => {
    clearAuthToken();
    setUser(null);
    setStatus("unauthenticated");
    setLoginError(null);
    setExpiresAt(null);
  }, []);

  return (
    <AuthContext.Provider
      value={{
        user,
        status,
        isAuthenticated: status === "authenticated",
        isAdmin: user?.role === "ADMIN",
        loginError,
        expiresAt,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
