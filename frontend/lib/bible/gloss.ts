import type { InterlinearWordDTO } from "@/types";

export function getDisplayGloss(
  word: InterlinearWordDTO,
  alignLang: string
): string | null | undefined {
  if (alignLang === "pt") {
    return word.portugueseGloss ?? word.kjvAlignment?.alignedText ?? word.englishGloss;
  }
  if (alignLang === "es") {
    return word.spanishGloss ?? word.kjvAlignment?.alignedText ?? word.englishGloss;
  }
  return word.englishGloss;
}
