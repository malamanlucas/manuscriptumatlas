"use client";

import { Menu, LogIn, LogOut, AlertCircle, Loader2 } from "lucide-react";
import { useSidebar } from "./SidebarContext";
import { useAuth } from "@/hooks/useAuth";
import { GoogleLogin } from "@react-oauth/google";
import { useState, useRef, useEffect } from "react";

interface HeaderProps {
  title: string;
  subtitle?: string;
}

export function Header({ title, subtitle }: HeaderProps) {
  const { toggle } = useSidebar();
  const { user, isAuthenticated, status, loginError, login, logout } = useAuth();
  const [showLoginPopup, setShowLoginPopup] = useState(false);
  const [loggingIn, setLoggingIn] = useState(false);
  const popupRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (isAuthenticated) setShowLoginPopup(false);
  }, [isAuthenticated]);

  useEffect(() => {
    if (status !== "loading") setLoggingIn(false);
  }, [status]);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (popupRef.current && !popupRef.current.contains(e.target as Node)) {
        setShowLoginPopup(false);
      }
    }
    if (showLoginPopup) {
      document.addEventListener("mousedown", handleClickOutside);
      return () => document.removeEventListener("mousedown", handleClickOutside);
    }
  }, [showLoginPopup]);

  return (
    <header className="sticky top-0 z-30 flex h-16 items-center gap-4 border-b border-border bg-background/80 px-4 backdrop-blur-sm md:px-6">
      <button
        onClick={toggle}
        className="rounded-md p-1.5 text-muted-foreground hover:bg-accent hover:text-foreground md:hidden"
        aria-label="Toggle menu"
      >
        <Menu className="h-5 w-5" />
      </button>
      <div className="flex-1">
        <h1 className="text-lg font-semibold">{title}</h1>
        {subtitle && (
          <p className="text-xs text-muted-foreground">{subtitle}</p>
        )}
      </div>

      <div className="relative flex items-center gap-2">
        {isAuthenticated && user ? (
          <div className="flex items-center gap-2 animate-fade-in">
            {user.pictureUrl && (
              <img
                src={user.pictureUrl}
                alt=""
                className="h-7 w-7 rounded-full"
                referrerPolicy="no-referrer"
              />
            )}
            <span className="hidden text-sm text-muted-foreground sm:inline">
              {user.displayName}
            </span>
            <span className="hidden rounded bg-purple-100 px-1.5 py-0.5 text-xs font-medium text-purple-700 dark:bg-purple-900/30 dark:text-purple-300 sm:inline">
              {user.role}
            </span>
            <button
              onClick={logout}
              className="rounded-md p-1.5 text-muted-foreground hover:bg-accent hover:text-foreground"
              title="Sair"
            >
              <LogOut className="h-4 w-4" />
            </button>
          </div>
        ) : loggingIn ? (
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Loader2 className="h-4 w-4 animate-spin" />
            <span className="hidden sm:inline">Autenticando...</span>
          </div>
        ) : (
          <div ref={popupRef}>
            {loginError && (
              <span className="mr-2 hidden items-center gap-1 text-xs text-red-500 sm:inline-flex">
                <AlertCircle className="h-3 w-3" />
                {loginError}
              </span>
            )}
            <button
              onClick={() => setShowLoginPopup(!showLoginPopup)}
              className="inline-flex items-center gap-2 rounded-lg border border-border px-3 py-1.5 text-sm text-muted-foreground hover:bg-accent hover:text-foreground"
            >
              <LogIn className="h-4 w-4" />
              <span className="hidden sm:inline">Entrar</span>
            </button>

            {showLoginPopup && (
              <div className="absolute right-0 top-full mt-2 z-50 rounded-lg border border-border bg-card p-4 shadow-lg">
                <GoogleLogin
                  onSuccess={async (response) => {
                    if (response.credential) {
                      setLoggingIn(true);
                      setShowLoginPopup(false);
                      await login(response.credential);
                    }
                  }}
                  onError={() => {}}
                  theme="outline"
                  size="large"
                />
              </div>
            )}
          </div>
        )}
      </div>
    </header>
  );
}
