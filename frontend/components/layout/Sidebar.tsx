"use client";

import { useTranslations } from "next-intl";
import { Link, usePathname } from "@/i18n/navigation";
import {
  LayoutDashboard,
  Clock,
  GitCompareArrows,
  BookOpen,
  BrainCircuit,
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
  FlaskConical,
  Landmark,
  ShieldAlert,
  ShieldQuestion,
  ChevronDown,
  PieChart,
  Languages,
  PanelLeftClose,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { LanguageSelector } from "./LanguageSelector";
import { ThemeToggle } from "./ThemeToggle";
import { useSidebar } from "./SidebarContext";
import { useAuth } from "@/hooks/useAuth";

type NavItem = { href: string; label: string; icon: React.ElementType };

export function Sidebar() {
  const pathname = usePathname();
  const t = useTranslations("sidebar");
  const { isOpen, close, isCollapsed, toggleCollapse, expandedSections, toggleSection } = useSidebar();
  const { isAdmin } = useAuth();

  const analysisItems: NavItem[] = [
    { href: "/dashboard", label: t("dashboard"), icon: LayoutDashboard },
    { href: "/manuscripts", label: t("explorer"), icon: Library },
    { href: "/verse-lookup", label: t("verseLookup"), icon: Search },
    { href: "/timeline", label: t("timeline"), icon: Clock },
    { href: "/compare", label: t("compare"), icon: GitCompareArrows },
    { href: "/metrics", label: t("metrics"), icon: BarChart3 },
  ];

  const patristicItems: NavItem[] = [
    { href: "/patristic-dashboard", label: t("patristicDashboard"), icon: PieChart },
    { href: "/fathers", label: t("fathers"), icon: Users },
    { href: "/fathers/testimony", label: t("testimony"), icon: Quote },
    { href: "/councils", label: t("councils"), icon: Landmark },
    { href: "/heresies", label: t("heresies"), icon: ShieldAlert },
    { href: "/apologetics", label: t("apologetics"), icon: ShieldQuestion },
  ];

  const bibleItems: NavItem[] = [
    { href: "/bible", label: t("bibleReader"), icon: BookOpen },
    { href: "/bible/compare", label: t("bibleCompare"), icon: GitCompareArrows },
    { href: "/bible/search", label: t("bibleSearch"), icon: Search },
    { href: "/bible/interlinear", label: t("interlinear"), icon: Languages },
  ];

  const referenceItems: NavItem[] = [
    { href: "/manuscript-count", label: t("manuscriptCount"), icon: Hash },
    { href: "/history", label: t("history"), icon: BookMarked },
    { href: "/sources", label: t("sources"), icon: Database },
    { href: "/methodology", label: t("methodology"), icon: FlaskConical },
    { href: "/faq", label: t("faq"), icon: HelpCircle },
    { href: "/wiki-llm", label: t("wikiLlm"), icon: BrainCircuit },
  ];

  const adminItems: NavItem[] = [
    { href: "/observatory", label: t("observatory"), icon: Telescope },
    { href: "/ingestion-status", label: t("ingestion"), icon: Activity },
    { href: "/llm-usage", label: t("llmUsage"), icon: BrainCircuit },
    { href: "/admin/users", label: t("users"), icon: Shield },
  ];

  const renderNavItem = (item: NavItem, size: "normal" | "small" = "normal") => {
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

  const renderCollapsibleSection = (
    sectionKey: string,
    label: string,
    items: NavItem[],
    size: "normal" | "small" = "normal"
  ) => {
    const isExpanded = expandedSections.has(sectionKey);
    const hasActiveChild = items.some(
      (item) => pathname === item.href || pathname.startsWith(item.href + "/")
    );

    return (
      <div key={sectionKey}>
        <button
          onClick={() => toggleSection(sectionKey)}
          className="flex w-full cursor-pointer items-center justify-between px-3 pb-2 pt-4"
        >
          <p className={cn(
            "text-xs font-semibold uppercase tracking-wider",
            hasActiveChild && !isExpanded ? "text-white/70" : "text-white/40"
          )}>
            {label}
          </p>
          <ChevronDown
            className={cn(
              "h-3 w-3 text-white/40 transition-transform duration-200",
              isExpanded ? "rotate-0" : "-rotate-90"
            )}
          />
        </button>
        <div
          className={cn(
            "overflow-hidden transition-all duration-200",
            isExpanded ? "max-h-96 opacity-100" : "max-h-0 opacity-0"
          )}
        >
          {items.map((item) => renderNavItem(item, size))}
        </div>
      </div>
    );
  };

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
          isCollapsed ? "md:-translate-x-full" : "md:translate-x-0",
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
          {renderCollapsibleSection("analysis", t("analysis"), analysisItems)}
          {renderCollapsibleSection("patristic", t("patristic"), patristicItems)}
          {renderCollapsibleSection("bible", t("bible"), bibleItems)}
          {renderCollapsibleSection("reference", t("reference"), referenceItems, "small")}

          {isAdmin && renderCollapsibleSection("admin", t("administration"), adminItems, "small")}
        </nav>

        <div className="border-t border-white/10 px-6 py-4 space-y-3">
          <ThemeToggle />
          <LanguageSelector />
          <div className="flex items-center justify-between">
            <p className="text-xs text-white/40">{t("version")}</p>
            <button
              onClick={toggleCollapse}
              className="hidden rounded-md p-1 text-white/40 hover:bg-white/10 hover:text-white/80 md:inline-flex"
              title={t("hideSidebar")}
            >
              <PanelLeftClose className="h-4 w-4" />
            </button>
          </div>
        </div>
      </aside>
    </>
  );
}
