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
  if (percent >= 90) return "bg-[#4a7a6a] text-white";
  if (percent >= 60) return "bg-[#5a8a7a] text-white";
  if (percent >= 30) return "bg-[#b8976a] text-white";
  if (percent > 0) return "bg-[#a65d57] text-white";
  return "bg-gray-300 text-gray-600 dark:bg-gray-700 dark:text-gray-400";
}

export function coverageHeatColor(percent: number): string {
  if (percent >= 90) return "#4a7a6a";
  if (percent >= 60) return "#5a8a7a";
  if (percent >= 30) return "#b8976a";
  if (percent > 0) return "#a65d57";
  return "#c8c8c8";
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
