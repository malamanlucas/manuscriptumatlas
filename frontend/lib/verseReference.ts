/**
 * Resolves book sigla (Portuguese and English) to canonical English book name for the API.
 * Based on NT book list and sigla column (e.g. Mt, Mc, 1 Co).
 */

const SIGLA_TO_CANONICAL: Record<string, string> = {
  // Portuguese sigla (from tabela livro)
  mt: "Matthew",
  mc: "Mark",
  lc: "Luke",
  jo: "John",
  at: "Acts",
  rm: "Romans",
  "1co": "1 Corinthians",
  "2co": "2 Corinthians",
  gl: "Galatians",
  ef: "Ephesians",
  fp: "Philippians",
  cl: "Colossians",
  "1ts": "1 Thessalonians",
  "2ts": "2 Thessalonians",
  "1tm": "1 Timothy",
  "2tm": "2 Timothy",
  tt: "Titus",
  fl: "Philemon",
  hb: "Hebrews",
  tg: "James",
  "1pe": "1 Peter",
  "2pe": "2 Peter",
  "1jo": "1 John",
  "2jo": "2 John",
  "3jo": "3 John",
  jd: "Jude",
  ap: "Revelation",
  // English abbreviations
  matt: "Matthew",
  mark: "Mark",
  luke: "Luke",
  john: "John",
  acts: "Acts",
  rom: "Romans",
  "1cor": "1 Corinthians",
  "2cor": "2 Corinthians",
  gal: "Galatians",
  eph: "Ephesians",
  phil: "Philippians",
  col: "Colossians",
  "1thess": "1 Thessalonians",
  "2thess": "2 Thessalonians",
  "1tim": "1 Timothy",
  "2tim": "2 Timothy",
  tit: "Titus",
  phlm: "Philemon",
  heb: "Hebrews",
  jas: "James",
  "1pet": "1 Peter",
  "2pet": "2 Peter",
  "1john": "1 John",
  "2john": "2 John",
  "3john": "3 John",
  jude: "Jude",
  rev: "Revelation",
};

const CANONICAL_BOOK_NAMES = [
  "Matthew",
  "Mark",
  "Luke",
  "John",
  "Acts",
  "Romans",
  "1 Corinthians",
  "2 Corinthians",
  "Galatians",
  "Ephesians",
  "Philippians",
  "Colossians",
  "1 Thessalonians",
  "2 Thessalonians",
  "1 Timothy",
  "2 Timothy",
  "Titus",
  "Philemon",
  "Hebrews",
  "James",
  "1 Peter",
  "2 Peter",
  "1 John",
  "2 John",
  "3 John",
  "Jude",
  "Revelation",
] as const;

function normalizeBookToken(token: string): string {
  return token.toLowerCase().replace(/\s+/g, "").trim();
}

/**
 * Resolves a book token (sigla or full name) to canonical English book name.
 * Returns null if not found.
 */
export function resolveBookToCanonical(token: string): string | null {
  const trimmed = token.trim();
  if (!trimmed) return null;
  const normalized = normalizeBookToken(trimmed);
  if (SIGLA_TO_CANONICAL[normalized]) return SIGLA_TO_CANONICAL[normalized];
  const match = CANONICAL_BOOK_NAMES.find(
    (name) => name.toLowerCase() === trimmed.toLowerCase()
  );
  return match ?? null;
}

export interface ParsedVerseReference {
  book: string;
  chapter: number;
  verse: number;
}

/**
 * Parses a verse reference string (e.g. "Mt 1:1", "1 Co 13:1", "John 18:31", "Matthew 1:1").
 * Returns { book (canonical English), chapter, verse } or null if parse fails.
 */
export function parseVerseReference(
  input: string
): ParsedVerseReference | null {
  const s = input.trim();
  if (!s) return null;

  // Match "number number", "number:number", or "number.number" at the end for chapter:verse
  const cvMatch = s.match(/\s+(\d+)\s*[:\s.]\s*(\d+)\s*$/);
  if (!cvMatch) return null;
  const chapter = parseInt(cvMatch[1], 10);
  const verse = parseInt(cvMatch[2], 10);
  if (isNaN(chapter) || isNaN(verse) || chapter < 1 || verse < 1) return null;

  const bookPart = s.slice(0, cvMatch.index).trim();
  if (!bookPart) return null;

  const book = resolveBookToCanonical(bookPart);
  if (!book) return null;

  return { book, chapter, verse };
}

export { CANONICAL_BOOK_NAMES };
