"use client";

import { useTranslations } from "next-intl";
import {
  useAnalyticsSessionDetail,
  useAnalyticsSessionPageviews,
} from "@/hooks/useVisitorAnalytics";
import { X, Monitor, Cpu, Wifi, Shield, Map } from "lucide-react";

export function SessionDrawer({
  sessionId,
  onClose,
}: {
  sessionId: string | null;
  onClose: () => void;
}) {
  const t = useTranslations("observatory");
  const { data: session } = useAnalyticsSessionDetail(sessionId);
  const { data: pageviews } = useAnalyticsSessionPageviews(sessionId);

  if (!sessionId) return null;

  const networkInfo = session?.networkInfo
    ? (() => {
        try {
          return JSON.parse(session.networkInfo);
        } catch {
          return null;
        }
      })()
    : null;

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 z-40 bg-black/50"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Drawer */}
      <div className="fixed right-0 top-0 z-50 h-screen w-full max-w-lg overflow-y-auto border-l border-border bg-card shadow-xl">
        <div className="sticky top-0 z-10 flex items-center justify-between border-b border-border bg-card px-4 py-3">
          <h3 className="text-sm font-semibold">{t("drawer.title")}</h3>
          <button
            onClick={onClose}
            className="rounded-md p-1 hover:bg-muted"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {!session ? (
          <div className="flex items-center justify-center p-12 text-muted-foreground">
            {t("loading")}
          </div>
        ) : (
          <div className="divide-y divide-border">
            {/* Identity */}
            <Section title={t("drawer.identity")}>
              <Field label="Visitor ID" value={session.visitorId} mono />
              <Field label="Session ID" value={session.sessionId} mono />
              <Field label="IP" value={session.ipAddress} mono />
              <Field
                label="Fingerprint"
                value={session.canvasFingerprint ?? "—"}
                mono
              />
              <Field
                label={t("drawer.firstSeen")}
                value={new Date(session.createdAt).toLocaleString()}
              />
              <Field
                label={t("drawer.lastActivity")}
                value={new Date(session.lastActivityAt).toLocaleString()}
              />
            </Section>

            {/* Browser & OS */}
            <Section title={t("drawer.browserOs")} icon={Monitor}>
              <Field
                label={t("table.browser")}
                value={`${session.browserName ?? "—"} ${session.browserVersion ?? ""}`}
              />
              <Field
                label="OS"
                value={`${session.osName ?? "—"} ${session.osVersion ?? ""}`}
              />
              <Field label="Platform" value={session.platform ?? "—"} />
              <Field label="User-Agent" value={session.userAgent} small />
              <Field
                label={t("table.language")}
                value={`${session.language ?? "—"} (${session.languages ?? ""})`}
              />
              <Field
                label={t("table.timezone")}
                value={session.timezone ?? "—"}
              />
            </Section>

            {/* Display */}
            <Section title={t("drawer.display")} icon={Monitor}>
              <Field
                label="Screen"
                value={
                  session.screenWidth
                    ? `${session.screenWidth}x${session.screenHeight}`
                    : "—"
                }
              />
              <Field
                label="Viewport"
                value={
                  session.viewportWidth
                    ? `${session.viewportWidth}x${session.viewportHeight}`
                    : "—"
                }
              />
              <Field
                label="Pixel Ratio"
                value={session.pixelRatio?.toString() ?? "—"}
              />
              <Field
                label="Color Depth"
                value={session.colorDepth?.toString() ?? "—"}
              />
            </Section>

            {/* Hardware */}
            <Section title={t("drawer.hardware")} icon={Cpu}>
              <Field
                label="CPU Cores"
                value={session.hardwareConcurrency?.toString() ?? "—"}
              />
              <Field
                label="RAM"
                value={session.deviceMemory ? `${session.deviceMemory} GB` : "—"}
              />
              <Field label="GPU" value={session.webglRenderer ?? "—"} />
              <Field label="GPU Vendor" value={session.webglVendor ?? "—"} />
              <Field
                label="Touch Points"
                value={session.touchPoints?.toString() ?? "—"}
              />
            </Section>

            {/* Network */}
            <Section title={t("drawer.network")} icon={Wifi}>
              <Field
                label="Type"
                value={networkInfo?.effectiveType ?? "—"}
              />
              <Field
                label="Downlink"
                value={
                  networkInfo?.downlink
                    ? `${networkInfo.downlink} Mbps`
                    : "—"
                }
              />
              <Field
                label="RTT"
                value={networkInfo?.rtt ? `${networkInfo.rtt}ms` : "—"}
              />
              <Field
                label={t("table.loadTime")}
                value={
                  session.pageLoadTimeMs
                    ? `${session.pageLoadTimeMs}ms`
                    : "—"
                }
              />
            </Section>

            {/* Privacy */}
            <Section title={t("drawer.privacy")} icon={Shield}>
              <Field
                label="Cookies"
                value={
                  session.cookieEnabled === true
                    ? "Enabled"
                    : session.cookieEnabled === false
                      ? "Disabled"
                      : "—"
                }
              />
              <Field
                label="Do Not Track"
                value={
                  session.doNotTrack === true
                    ? "On"
                    : session.doNotTrack === false
                      ? "Off"
                      : "—"
                }
              />
            </Section>

            {/* Journey */}
            <Section title={t("drawer.journey")} icon={Map}>
              {pageviews && pageviews.length > 0 ? (
                <div className="space-y-1">
                  {pageviews.map((pv, i) => (
                    <div
                      key={pv.id}
                      className="flex items-center justify-between text-xs"
                    >
                      <div className="flex items-center gap-2">
                        <span className="text-muted-foreground">
                          {new Date(pv.createdAt).toLocaleTimeString([], {
                            hour: "2-digit",
                            minute: "2-digit",
                            second: "2-digit",
                          })}
                        </span>
                        <span className="font-mono">{pv.path}</span>
                      </div>
                      {pv.durationMs && (
                        <span className="text-muted-foreground">
                          {formatDuration(pv.durationMs)}
                        </span>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-xs text-muted-foreground">{t("noData")}</p>
              )}
            </Section>
          </div>
        )}
      </div>
    </>
  );
}

function Section({
  title,
  icon: Icon,
  children,
}: {
  title: string;
  icon?: React.ElementType;
  children: React.ReactNode;
}) {
  return (
    <div className="px-4 py-3">
      <div className="mb-2 flex items-center gap-2">
        {Icon && <Icon className="h-4 w-4 text-muted-foreground" />}
        <h4 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
          {title}
        </h4>
      </div>
      <div className="space-y-1">{children}</div>
    </div>
  );
}

function Field({
  label,
  value,
  mono,
  small,
}: {
  label: string;
  value: string;
  mono?: boolean;
  small?: boolean;
}) {
  return (
    <div className="flex items-start justify-between gap-4 text-xs">
      <span className="text-muted-foreground shrink-0">{label}</span>
      <span
        className={`text-right break-all ${mono ? "font-mono" : ""} ${small ? "text-[10px] max-w-[280px]" : ""}`}
      >
        {value}
      </span>
    </div>
  );
}

function formatDuration(ms: number): string {
  const seconds = Math.floor(ms / 1000);
  if (seconds < 60) return `${seconds}s`;
  const minutes = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${minutes}m ${secs}s`;
}
