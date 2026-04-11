"use client";

import { useEffect, useRef, useCallback } from "react";
import { usePathname } from "next/navigation";

const API_BASE = "/api";

function generateUUID(): string {
  return crypto.randomUUID();
}

function getOrCreateVisitorId(): string {
  const key = "ma_visitor_id";
  let id = localStorage.getItem(key);
  if (!id) {
    id = generateUUID();
    localStorage.setItem(key, id);
  }
  return id;
}

function getOrCreateSessionId(): string {
  const key = "ma_session_id";
  let id = sessionStorage.getItem(key);
  if (!id) {
    id = generateUUID();
    sessionStorage.setItem(key, id);
  }
  return id;
}

function getCanvasFingerprint(): string {
  try {
    const canvas = document.createElement("canvas");
    canvas.width = 200;
    canvas.height = 50;
    const ctx = canvas.getContext("2d");
    if (!ctx) return "";
    ctx.textBaseline = "top";
    ctx.font = "14px Arial";
    ctx.fillStyle = "#f60";
    ctx.fillRect(0, 0, 200, 50);
    ctx.fillStyle = "#069";
    ctx.fillText("Manuscriptum Atlas", 2, 15);
    ctx.strokeStyle = "rgba(102, 204, 0, 0.7)";
    ctx.beginPath();
    ctx.arc(100, 25, 20, 0, Math.PI * 2);
    ctx.stroke();
    const dataUrl = canvas.toDataURL();
    let hash = 0;
    for (let i = 0; i < dataUrl.length; i++) {
      const char = dataUrl.charCodeAt(i);
      hash = ((hash << 5) - hash + char) | 0;
    }
    return Math.abs(hash).toString(16).padStart(16, "0");
  } catch {
    return "";
  }
}

function getWebGLInfo(): { renderer: string; vendor: string } {
  try {
    const canvas = document.createElement("canvas");
    const gl =
      canvas.getContext("webgl") || canvas.getContext("experimental-webgl");
    if (!gl) return { renderer: "", vendor: "" };
    const glCtx = gl as WebGLRenderingContext;
    const dbg = glCtx.getExtension("WEBGL_debug_renderer_info");
    if (!dbg) return { renderer: "", vendor: "" };
    return {
      renderer: glCtx.getParameter(dbg.UNMASKED_RENDERER_WEBGL) || "",
      vendor: glCtx.getParameter(dbg.UNMASKED_VENDOR_WEBGL) || "",
    };
  } catch {
    return { renderer: "", vendor: "" };
  }
}

function getNetworkInfo(): {
  effectiveType?: string;
  downlink?: number;
  rtt?: number;
} | null {
  const nav = navigator as Navigator & {
    connection?: {
      effectiveType?: string;
      downlink?: number;
      rtt?: number;
    };
  };
  if (!nav.connection) return null;
  return {
    effectiveType: nav.connection.effectiveType,
    downlink: nav.connection.downlink,
    rtt: nav.connection.rtt,
  };
}

function getPageLoadTime(): number | null {
  try {
    const entries = performance.getEntriesByType(
      "navigation"
    ) as PerformanceNavigationTiming[];
    if (entries.length > 0) {
      return Math.round(entries[0].loadEventEnd - entries[0].startTime);
    }
  } catch {
    /* no-op */
  }
  return null;
}

function collectBrowserData() {
  const webgl = getWebGLInfo();
  const network = getNetworkInfo();
  const nav = navigator as Navigator & {
    deviceMemory?: number;
  };

  return {
    userAgent: navigator.userAgent,
    screenWidth: screen.width,
    screenHeight: screen.height,
    viewportWidth: window.innerWidth,
    viewportHeight: window.innerHeight,
    language: navigator.language,
    languages: [...navigator.languages],
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    platform: navigator.platform,
    networkInfo: network,
    deviceMemory: nav.deviceMemory ?? null,
    hardwareConcurrency: navigator.hardwareConcurrency ?? null,
    colorDepth: screen.colorDepth,
    pixelRatio: window.devicePixelRatio,
    touchPoints: navigator.maxTouchPoints,
    cookieEnabled: navigator.cookieEnabled,
    doNotTrack: navigator.doNotTrack === "1",
    webglRenderer: webgl.renderer,
    webglVendor: webgl.vendor,
    canvasFingerprint: getCanvasFingerprint(),
    referrer: document.referrer || null,
    pageLoadTimeMs: getPageLoadTime(),
  };
}

async function postJSON(url: string, body: unknown): Promise<boolean> {
  try {
    const res = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
      keepalive: true,
    });
    return res.ok;
  } catch {
    return false;
  }
}

export function VisitorTracker() {
  const pathname = usePathname();
  const sessionStarted = useRef(false);
  const lastPath = useRef<string | null>(null);
  const pageEnteredAt = useRef(Date.now());
  const visitorIdRef = useRef("");
  const sessionIdRef = useRef("");

  const sendPageView = useCallback(
    (path: string, prevPath: string | null) => {
      const duration =
        prevPath !== null ? Date.now() - pageEnteredAt.current : undefined;

      if (prevPath !== null) {
        postJSON(`${API_BASE}/visitor/pageview`, {
          sessionId: sessionIdRef.current,
          visitorId: visitorIdRef.current,
          path: prevPath,
          referrerPath: null,
          durationMs: duration,
        });
      }

      postJSON(`${API_BASE}/visitor/pageview`, {
        sessionId: sessionIdRef.current,
        visitorId: visitorIdRef.current,
        path,
        referrerPath: prevPath,
      });

      pageEnteredAt.current = Date.now();
    },
    []
  );

  useEffect(() => {
    if (sessionStarted.current) return;
    sessionStarted.current = true;

    const visitorId = getOrCreateVisitorId();
    const sessionId = getOrCreateSessionId();
    visitorIdRef.current = visitorId;
    sessionIdRef.current = sessionId;

    const data = collectBrowserData();
    const sessionPayload = { visitorId, sessionId, ...data };
    let sessionCreated = false;
    let retryTimer: ReturnType<typeof setTimeout> | null = null;

    async function createSessionWithRetry(attempt = 0) {
      const ok = await postJSON(`${API_BASE}/visitor/session`, sessionPayload);
      if (ok) {
        sessionCreated = true;
      } else if (attempt < 10) {
        const delay = Math.min(2000 * Math.pow(2, attempt), 30000);
        retryTimer = setTimeout(() => createSessionWithRetry(attempt + 1), delay);
      }
    }
    createSessionWithRetry();

    const heartbeatInterval = setInterval(() => {
      if (sessionCreated) {
        postJSON(`${API_BASE}/visitor/heartbeat`, { sessionId });
      }
    }, 30000);

    const handleBeforeUnload = () => {
      const duration = Date.now() - pageEnteredAt.current;
      const body = JSON.stringify({
        sessionId,
        visitorId,
        path: lastPath.current || pathname,
        durationMs: duration,
      });
      navigator.sendBeacon(
        `${API_BASE}/visitor/pageview`,
        new Blob([body], { type: "application/json" })
      );
    };

    window.addEventListener("beforeunload", handleBeforeUnload);

    return () => {
      clearInterval(heartbeatInterval);
      if (retryTimer) clearTimeout(retryTimer);
      window.removeEventListener("beforeunload", handleBeforeUnload);
    };
  }, [pathname]);

  useEffect(() => {
    if (!sessionStarted.current) return;
    if (lastPath.current === pathname) return;

    const prevPath = lastPath.current;
    lastPath.current = pathname;

    if (prevPath !== null) {
      sendPageView(pathname, prevPath);
    } else {
      postJSON(`${API_BASE}/visitor/pageview`, {
        sessionId: sessionIdRef.current,
        visitorId: visitorIdRef.current,
        path: pathname,
        referrerPath: null,
      });
    }
  }, [pathname, sendPageView]);

  return null;
}
