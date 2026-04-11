"use client";

import { Sidebar } from "@/components/layout/Sidebar";
import { SidebarProvider, useSidebar } from "@/components/layout/SidebarContext";

function AtlasContent({ children }: { children: React.ReactNode }) {
  const { isCollapsed } = useSidebar();
  return (
    <div className="flex min-h-screen">
      <Sidebar />
      <main className={`flex-1 pl-0 transition-[padding] duration-300 ${isCollapsed ? "md:pl-0" : "md:pl-64"}`}>
        {children}
      </main>
    </div>
  );
}

export default function AtlasLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <SidebarProvider>
      <AtlasContent>{children}</AtlasContent>
    </SidebarProvider>
  );
}
