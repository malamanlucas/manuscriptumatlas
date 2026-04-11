"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";

interface SidebarContextValue {
  isOpen: boolean;
  toggle: () => void;
  close: () => void;
  isCollapsed: boolean;
  toggleCollapse: () => void;
  expandedSections: Set<string>;
  toggleSection: (name: string) => void;
}

const SidebarContext = createContext<SidebarContextValue>({
  isOpen: false,
  toggle: () => {},
  close: () => {},
  isCollapsed: false,
  toggleCollapse: () => {},
  expandedSections: new Set(),
  toggleSection: () => {},
});

export function SidebarProvider({ children }: { children: ReactNode }) {
  const [isOpen, setIsOpen] = useState(false);
  const [isCollapsed, setIsCollapsed] = useState(() => {
    if (typeof window === "undefined") return false;
    return localStorage.getItem("sidebar-collapsed") === "true";
  });
  const [expandedSections, setExpandedSections] = useState<Set<string>>(
    () => new Set(["analysis", "patristic", "reference"])
  );

  const toggle = useCallback(() => setIsOpen((v) => !v), []);
  const close = useCallback(() => setIsOpen(false), []);
  const toggleCollapse = useCallback(() => {
    setIsCollapsed((v) => {
      const next = !v;
      localStorage.setItem("sidebar-collapsed", String(next));
      return next;
    });
  }, []);
  const toggleSection = useCallback((name: string) => {
    setExpandedSections((prev) => {
      const next = new Set(prev);
      if (next.has(name)) next.delete(name);
      else next.add(name);
      return next;
    });
  }, []);

  useEffect(() => {
    document.body.style.overflow = isOpen ? "hidden" : "";
    return () => {
      document.body.style.overflow = "";
    };
  }, [isOpen]);

  useEffect(() => {
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") close();
    };
    window.addEventListener("keydown", handleKey);
    return () => window.removeEventListener("keydown", handleKey);
  }, [close]);

  return (
    <SidebarContext.Provider value={{ isOpen, toggle, close, isCollapsed, toggleCollapse, expandedSections, toggleSection }}>
      {children}
    </SidebarContext.Provider>
  );
}

export function useSidebar() {
  return useContext(SidebarContext);
}
