"use client";

import { useTranslations } from "next-intl";
import { Link, usePathname } from "@/i18n/navigation";
import {
  LayoutDashboard,
  Clock,
  GitCompareArrows,
  BookOpen,
  ScrollText,
  Library,
  BarChart3,
  BookMarked,
  Hash,
  Database,
  HelpCircle,
  Activity,
  Search,
  Users,
  Quote,
  Telescope,
  Shield,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { LanguageSelector } from "./LanguageSelector";
import { ThemeToggle } from "./ThemeToggle";
import { useSidebar } from "./SidebarContext";
import { useAuth } from "@/hooks/useAuth";

export function Sidebar() {
  const pathname = usePathname();
  const t = useTranslations("sidebar");
  const { isOpen, close } = useSidebar();
  const { isAdmin } = useAuth();

  const analysisItems = [
    { href: "/dashboard", label: t("dashboard"), icon: LayoutDashboard },
    { href: "/manuscripts", label: t("explorer"), icon: Library },
    { href: "/verse-lookup", label: t("verseLookup"), icon: Search },
    { href: "/timeline", label: t("timeline"), icon: Clock },
    { href: "/compare", label: t("compare"), icon: GitCompareArrows },
    { href: "/metrics", label: t("metrics"), icon: BarChart3 },
  ];

  const patristicItems = [
    { href: "/fathers", label: t("fathers"), icon: Users },
    { href: "/fathers/testimony", label: t("testimony"), icon: Quote },
  ];

  const referenceItems = [
    { href: "/manuscript-count", label: t("manuscriptCount"), icon: Hash },
    { href: "/history", label: t("history"), icon: BookMarked },
    { href: "/sources", label: t("sources"), icon: Database },
    { href: "/faq", label: t("faq"), icon: HelpCircle },
  ];

  const adminItems = [
    { href: "/observatory", label: t("observatory"), icon: Telescope },
    { href: "/ingestion-status", label: t("ingestion"), icon: Activity },
    { href: "/admin/users", label: t("users"), icon: Shield },
  ];

  const renderNavItem = (
    item: { href: string; label: string; icon: React.ElementType },
    size: "normal" | "small" = "normal"
  ) => {
    const isActive =
      pathname === item.href || pathname.startsWith(item.href + "/");
    return (
      <Link
        key={item.href}
        href={item.href}
        onClick={close}
        className={cn(
          "flex items-center gap-3 rounded-lg transition-colors",
          size === "normal"
            ? "px-3 py-2.5 text-sm font-medium"
            : "px-3 py-2 text-sm",
          isActive
            ? "bg-white/10 text-white"
            : size === "normal"
              ? "text-white/70 hover:bg-white/5 hover:text-white"
              : "text-white/60 hover:bg-white/5 hover:text-white/90"
        )}
      >
        <item.icon className={size === "normal" ? "h-5 w-5" : "h-4 w-4"} />
        {item.label}
      </Link>
    );
  };

  const renderSectionLabel = (label: string) => (
    <div className="pb-2 pt-4">
      <p className="px-3 text-xs font-semibold uppercase tracking-wider text-white/40">
        {label}
      </p>
    </div>
  );

  return (
    <>
      {isOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/60 md:hidden"
          onClick={close}
          aria-hidden="true"
        />
      )}

      <aside
        className={cn(
          "fixed left-0 top-0 z-50 flex h-screen w-64 flex-col bg-sidebar text-sidebar-foreground",
          "transition-transform duration-300 ease-in-out",
          "md:translate-x-0",
          isOpen ? "translate-x-0" : "-translate-x-full"
        )}
      >
        <Link href="/" onClick={close} className="flex h-16 items-center gap-3 border-b border-white/10 px-6">
          <ScrollText className="h-7 w-7 text-blue-400" />
          <div>
            <h1 className="text-sm font-bold leading-tight">{t("title")}</h1>
            <p className="text-xs text-white/60">{t("subtitle")}</p>
          </div>
        </Link>

        <nav className="flex-1 space-y-1 overflow-y-auto px-3 py-4">
          {renderSectionLabel(t("analysis"))}
          {analysisItems.map((item) => renderNavItem(item))}

          {renderSectionLabel(t("patristic"))}
          {patristicItems.map((item) => renderNavItem(item))}

          {renderSectionLabel(t("reference"))}
          {referenceItems.map((item) => renderNavItem(item, "small"))}

          {renderSectionLabel(t("books"))}
          <Link
            href="/dashboard"
            onClick={close}
            className={cn(
              "flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition-colors",
              pathname === "/dashboard"
                ? "text-white/90"
                : "text-white/60 hover:text-white/90"
            )}
          >
            <BookOpen className="h-4 w-4" />
            {t("allBooks")}
          </Link>

          {isAdmin && (
            <>
              {renderSectionLabel(t("administration"))}
              {adminItems.map((item) => renderNavItem(item, "small"))}
            </>
          )}
        </nav>

        <div className="border-t border-white/10 px-6 py-4 space-y-3">
          <ThemeToggle />
          <LanguageSelector />
          <p className="text-xs text-white/40">{t("version")}</p>
        </div>
      </aside>
    </>
  );
}
