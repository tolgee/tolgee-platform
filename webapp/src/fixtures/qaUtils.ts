export type QaReplacementParams = {
  positionStart: number;
  positionEnd: number;
  replacement?: string;
};

/**
 * Applies a QA issue replacement to the translation text.
 * Replaces the span defined by positionStart/positionEnd with the replacement text.
 */
export function applyQaReplacement(
  text: string,
  issue: QaReplacementParams
): string {
  return (
    text.slice(0, issue.positionStart) +
    (issue.replacement ?? '') +
    text.slice(issue.positionEnd)
  );
}

/**
 * Finds the character offset of a variant's content within an ICU plural string.
 * Searches for the pattern "variantKey {" and returns the position after "{".
 */
export function findVariantOffset(icuText: string, variantKey: string): number {
  const escaped = variantKey.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const pattern = new RegExp(`(?:^|[\\s,])${escaped}\\s*\\{`);
  const match = pattern.exec(icuText);
  if (!match) return 0;
  return match.index + match[0].length;
}

type QaIssueWithVariant = {
  positionStart: number;
  positionEnd: number;
  pluralVariant?: string | null;
};

/**
 * Adjusts QA issue positions from full-ICU-text-relative to variant-relative.
 * Only adjusts issues whose pluralVariant matches the given variant.
 */
export function adjustIssuePositionsForVariant<T extends QaIssueWithVariant>(
  issues: T[],
  icuText: string,
  variant: string
): T[] {
  const offset = findVariantOffset(icuText, variant);
  return issues
    .filter((i) => i.pluralVariant === variant)
    .map((i) => ({
      ...i,
      positionStart: i.positionStart - offset,
      positionEnd: i.positionEnd - offset,
    }));
}
