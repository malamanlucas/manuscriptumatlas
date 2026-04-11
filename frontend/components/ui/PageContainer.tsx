"use client";

import { Header } from "@/components/layout/Header";

interface PageContainerProps {
  title: string;
  subtitle?: string;
  children: React.ReactNode;
}

export function PageContainer({ title, subtitle, children }: PageContainerProps) {
  return (
    <div className="min-h-screen bg-background">
      <Header title={title} subtitle={subtitle} />
      <div className="mx-auto w-full max-w-7xl p-4 md:p-6 space-y-6">
        {children}
      </div>
    </div>
  );
}
