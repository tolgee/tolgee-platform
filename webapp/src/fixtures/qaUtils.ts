export type QaReplacementParams = {
  positionStart?: number;
  positionEnd?: number;
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
  if (issue.positionStart == null || issue.positionEnd == null) {
    return text;
  }
  return (
    text.slice(0, issue.positionStart) +
    (issue.replacement ?? '') +
    text.slice(issue.positionEnd)
  );
}

export type QaVariantIssue = {
  positionStart?: number;
  positionEnd?: number;
  pluralVariant?: string;
};

export function adjustQaIssuesForVariant<T extends QaVariantIssue>(
  issues: T[],
  variant: string | undefined,
  variantOffset: number
): T[] {
  if (!variant || variantOffset === 0) {
    return issues;
  }
  return issues
    .filter((i) => i.pluralVariant === variant || !i.pluralVariant)
    .map((i) => ({
      ...i,
      positionStart:
        i.positionStart != null
          ? i.positionStart - variantOffset
          : i.positionStart,
      positionEnd:
        i.positionEnd != null ? i.positionEnd - variantOffset : i.positionEnd,
    }));
}
