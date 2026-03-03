"use client";

import { useAuth } from "@/hooks/useAuth";
import { useTranslations } from "next-intl";
import { GoogleLogin } from "@react-oauth/google";
import { ShieldAlert, LogOut, Loader2, AlertCircle, CheckCircle2 } from "lucide-react";
import { useEffect, useRef, useState } from "react";

interface AuthGateProps {
  children: React.ReactNode;
  requiredRole?: "ADMIN" | "MEMBER";
}

export function AuthGate({ children, requiredRole = "ADMIN" }: AuthGateProps) {
  const { user, status, isAuthenticated, loginError, login, logout } = useAuth();
  const t = useTranslations("auth");
  const prevStatus = useRef(status);
  const [showSuccess, setShowSuccess] = useState(false);

  useEffect(() => {
    if (prevStatus.current === "loading" && status === "authenticated") {
      setShowSuccess(true);
      const timer = setTimeout(() => setShowSuccess(false), 1200);
      return () => clearTimeout(timer);
    }
    prevStatus.current = status;
  }, [status]);

  if (status === "loading") {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <div className="flex flex-col items-center gap-3">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
          <p className="text-sm text-muted-foreground animate-pulse">{t("authenticating")}</p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <div className="mx-auto max-w-sm space-y-6 text-center">
          <ShieldAlert className="mx-auto h-16 w-16 text-muted-foreground" />
          <h2 className="text-2xl font-bold">{t("loginTitle")}</h2>
          <p className="text-muted-foreground">{t("loginDescription")}</p>

          {loginError && (
            <div className="flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700 dark:border-red-900 dark:bg-red-950 dark:text-red-300">
              <AlertCircle className="h-4 w-4 shrink-0" />
              <span>{loginError}</span>
            </div>
          )}

          <div className="flex justify-center">
            <GoogleLogin
              onSuccess={(response) => {
                if (response.credential) {
                  login(response.credential);
                }
              }}
              onError={() => {}}
              theme="outline"
              size="large"
              shape="pill"
            />
          </div>
        </div>
      </div>
    );
  }

  if (requiredRole === "ADMIN" && user?.role !== "ADMIN") {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <div className="mx-auto max-w-sm space-y-4 text-center">
          <ShieldAlert className="mx-auto h-16 w-16 text-red-500" />
          <h2 className="text-2xl font-bold">{t("accessDenied")}</h2>
          <p className="text-muted-foreground">
            {t("signedInAs")} <strong>{user?.email}</strong>
          </p>
          <button
            onClick={logout}
            className="inline-flex items-center gap-2 rounded-lg bg-zinc-800 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-700 dark:bg-zinc-200 dark:text-zinc-900 dark:hover:bg-zinc-300"
          >
            <LogOut className="h-4 w-4" />
            {t("signOut")}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className={showSuccess ? "animate-fade-in" : ""}>
      {showSuccess && (
        <div className="mb-4 flex items-center justify-center gap-2 rounded-lg border border-green-200 bg-green-50 p-3 text-sm text-green-700 dark:border-green-900 dark:bg-green-950 dark:text-green-300">
          <CheckCircle2 className="h-4 w-4 shrink-0" />
          <span>{t("loginSuccess")}</span>
        </div>
      )}
      {children}
    </div>
  );
}
