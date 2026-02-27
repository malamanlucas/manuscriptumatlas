"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
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
} from "lucide-react";
import { cn } from "@/lib/utils";

const navItems = [
  { href: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { href: "/manuscripts", label: "Explorer", icon: Library },
  { href: "/timeline", label: "Timeline", icon: Clock },
  { href: "/compare", label: "Comparativo", icon: GitCompareArrows },
  { href: "/metrics", label: "Métricas", icon: BarChart3 },
  { href: "/history", label: "História", icon: BookMarked },
];

const infoItems = [
  { href: "/manuscript-count", label: "Manuscritos", icon: Hash },
  { href: "/sources", label: "Fontes", icon: Database },
  { href: "/faq", label: "FAQ", icon: HelpCircle },
  { href: "/ingestion-status", label: "Ingestão", icon: Activity },
];

export function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="fixed left-0 top-0 z-40 flex h-screen w-64 flex-col bg-sidebar text-sidebar-foreground">
      <div className="flex h-16 items-center gap-3 border-b border-white/10 px-6">
        <ScrollText className="h-7 w-7 text-blue-400" />
        <div>
          <h1 className="text-sm font-bold leading-tight">NT Manuscripts</h1>
          <p className="text-xs text-white/60">Computational Analysis</p>
        </div>
      </div>

      <nav className="flex-1 space-y-1 overflow-y-auto px-3 py-4">
        {navItems.map((item) => {
          const isActive =
            pathname === item.href || pathname.startsWith(item.href + "/");
          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors",
                isActive
                  ? "bg-white/10 text-white"
                  : "text-white/70 hover:bg-white/5 hover:text-white"
              )}
            >
              <item.icon className="h-5 w-5" />
              {item.label}
            </Link>
          );
        })}

        <div className="pb-2 pt-4">
          <p className="px-3 text-xs font-semibold uppercase tracking-wider text-white/40">
            Informações
          </p>
        </div>
        {infoItems.map((item) => {
          const isActive =
            pathname === item.href || pathname.startsWith(item.href + "/");
          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition-colors",
                isActive
                  ? "bg-white/10 text-white"
                  : "text-white/60 hover:bg-white/5 hover:text-white/90"
              )}
            >
              <item.icon className="h-4 w-4" />
              {item.label}
            </Link>
          );
        })}

        <div className="pb-2 pt-4">
          <p className="px-3 text-xs font-semibold uppercase tracking-wider text-white/40">
            Books
          </p>
        </div>
        <Link
          href="/dashboard"
          className={cn(
            "flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition-colors",
            pathname === "/dashboard"
              ? "text-white/90"
              : "text-white/60 hover:text-white/90"
          )}
        >
          <BookOpen className="h-4 w-4" />
          All 27 Books
        </Link>
      </nav>

      <div className="border-t border-white/10 px-6 py-4">
        <p className="text-xs text-white/40">
          v2.0 &middot; Seculos I&ndash;X
        </p>
      </div>
    </aside>
  );
}
