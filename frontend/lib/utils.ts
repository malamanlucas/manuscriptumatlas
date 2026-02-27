import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

const ROMAN = ["", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"];

export function toRoman(n: number): string {
  return ROMAN[n] ?? String(n);
}

export function coverageColor(percent: number): string {
  if (percent >= 90) return "bg-emerald-600 text-white";
  if (percent >= 60) return "bg-emerald-400 text-white";
  if (percent >= 30) return "bg-amber-400 text-black";
  if (percent > 0) return "bg-red-400 text-white";
  return "bg-gray-300 text-gray-600 dark:bg-gray-700 dark:text-gray-400";
}

export function coverageHeatColor(percent: number): string {
  if (percent >= 90) return "#059669";
  if (percent >= 60) return "#34d399";
  if (percent >= 30) return "#fbbf24";
  if (percent > 0) return "#f87171";
  return "#d1d5db";
}

export const NT_BOOKS = [
  "Matthew", "Mark", "Luke", "John",
  "Acts",
  "Romans", "1 Corinthians", "2 Corinthians", "Galatians", "Ephesians",
  "Philippians", "Colossians", "1 Thessalonians", "2 Thessalonians",
  "1 Timothy", "2 Timothy", "Titus", "Philemon",
  "Hebrews", "James", "1 Peter", "2 Peter",
  "1 John", "2 John", "3 John", "Jude", "Revelation",
];

export const GOSPELS = ["Matthew", "Mark", "Luke", "John"];

export const BOOK_CATEGORIES: Record<string, string[]> = {
  Gospels: ["Matthew", "Mark", "Luke", "John"],
  History: ["Acts"],
  "Pauline Epistles": [
    "Romans", "1 Corinthians", "2 Corinthians", "Galatians", "Ephesians",
    "Philippians", "Colossians", "1 Thessalonians", "2 Thessalonians",
    "1 Timothy", "2 Timothy", "Titus", "Philemon",
  ],
  "General Epistles": ["Hebrews", "James", "1 Peter", "2 Peter", "1 John", "2 John", "3 John", "Jude"],
  Apocalypse: ["Revelation"],
};
