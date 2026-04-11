"use client";

import { Menu, LogIn, LogOut, AlertCircle, Loader2, Timer, PanelLeftOpen } from "lucide-react";
import { useSidebar } from "./SidebarContext";
import { useAuth } from "@/hooks/useAuth";
import { GoogleLogin } from "@react-oauth/google";
import { useState, useRef, useEffect } from "react";
import { useTranslations } from "next-intl";

function SessionCountdown({ expiresAt }: { expiresAt: number }) {
  const ta = useTranslations("auth");
  const [remaining, setRemaining] = useState(() => Math.max(0, expiresAt - Date.now()));

  useEffect(() => {
    const timer = setInterval(() => {
      const diff = Math.max(0, expiresAt - Date.now());
      setRemaining(diff);
      if (diff <= 0) clearInterval(timer);
    }, 60_000);
    return () => clearInterval(timer);
  }, [expiresAt]);

  if (remaining <= 0) return null;

  const totalMinutes = Math.floor(remaining / 60_000);
  const days = Math.floor(totalMinutes / 1440);
  const hours = Math.floor((totalMinutes % 1440) / 60);
  const minutes = totalMinutes % 60;

  let label: string;
  if (days > 0) {
    label = `${days}d ${hours}h`;
  } else if (hours > 0) {
    label = `${hours}h ${minutes}m`;
  } else {
    label = `${minutes}m`;
  }

  const isLow = totalMinutes < 60;

  return (
    <span
      className={`hidden items-center gap-1 rounded-md px-1.5 py-0.5 text-[10px] font-mono sm:inline-flex ${
        isLow
          ? "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400"
          : "bg-muted text-muted-foreground"
      }`}
      title={ta("sessionExpiresIn", { time: `${days}d ${hours}h ${minutes}m` })}
    >
      <Timer className="h-3 w-3" />
      {label}
    </span>
  );
}

interface HeaderProps {
  title: string;
  subtitle?: string;
}

export function Header({ title, subtitle }: HeaderProps) {
  const ta = useTranslations("auth");
  const { toggle, isCollapsed, toggleCollapse } = useSidebar();
  const { user, isAuthenticated, status, loginError, expiresAt, login, logout } = useAuth();
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
      {isCollapsed && (
        <button
          onClick={toggleCollapse}
          className="hidden rounded-md p-1.5 text-muted-foreground hover:bg-accent hover:text-foreground md:inline-flex"
          aria-label="Show sidebar"
        >
          <PanelLeftOpen className="h-5 w-5" />
        </button>
      )}
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
            {expiresAt && <SessionCountdown expiresAt={expiresAt} />}
            <button
              onClick={logout}
              className="rounded-md p-1.5 text-muted-foreground hover:bg-accent hover:text-foreground"
              title={ta("signOut")}
            >
              <LogOut className="h-4 w-4" />
            </button>
          </div>
        ) : loggingIn ? (
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Loader2 className="h-4 w-4 animate-spin" />
            <span className="hidden sm:inline">{ta("authenticating")}</span>
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
              <span className="hidden sm:inline">{ta("signIn")}</span>
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
