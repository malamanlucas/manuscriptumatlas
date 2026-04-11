import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "NT Manuscript Coverage",
  description:
    "Computational Manuscriptological Analysis of the New Testament",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
