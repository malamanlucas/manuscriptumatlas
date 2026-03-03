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
  AuthError,
} from "@/lib/api";

type AuthStatus = "loading" | "authenticated" | "unauthenticated";

interface AuthContextValue {
  user: UserDTO | null;
  status: AuthStatus;
  isAuthenticated: boolean;
  isAdmin: boolean;
  loginError: string | null;
  login: (credential: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue>({
  user: null,
  status: "loading",
  isAuthenticated: false,
  isAdmin: false,
  loginError: null,
  login: async () => {},
  logout: () => {},
});

export function useAuth() {
  return useContext(AuthContext);
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserDTO | null>(null);
  const [status, setStatus] = useState<AuthStatus>("loading");
  const [loginError, setLoginError] = useState<string | null>(null);

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
    }
  }, []);

  useEffect(() => {
    hydrate();
  }, [hydrate]);

  const login = useCallback(async (credential: string) => {
    setStatus("loading");
    setLoginError(null);

    try {
      const decoded = jwtDecode<{ exp: number }>(credential);
      const maxAge = Math.max(decoded.exp - Math.floor(Date.now() / 1000), 3600);
      setAuthToken(credential, maxAge);
    } catch {
      setAuthToken(credential, 3600);
    }

    try {
      const me = await getAuthMe();
      setUser(me);
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
  }, []);

  return (
    <AuthContext.Provider
      value={{
        user,
        status,
        isAuthenticated: status === "authenticated",
        isAdmin: user?.role === "ADMIN",
        loginError,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
