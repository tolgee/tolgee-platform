import { getPluralVariants } from '@tginternal/editor';

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
  if (!variant && variantOffset === 0) {
    return issues;
  }
  return issues
    .filter((i) => !variant || i.pluralVariant === variant || !i.pluralVariant)
    .map((i) => offsetQaIssue(i, variantOffset));
}

export function offsetQaIssue<T extends QaVariantIssue>(
  issue: T,
  offset: number
) {
  if (offset === 0) return issue;
  return {
    ...issue,
    positionStart:
      issue.positionStart != null
        ? issue.positionStart - offset
        : issue.positionStart,
    positionEnd:
      issue.positionEnd != null
        ? issue.positionEnd - offset
        : issue.positionEnd,
  };
}

export function getFirstPluralVariantWithQaIssues(
  qaIssues: Array<{ pluralVariant?: string }> | undefined,
  languageTag: string
): string | undefined {
  if (!qaIssues?.length) return undefined;
  const variants = getPluralVariants(languageTag);
  return variants.find((v) =>
    qaIssues.some((issue) => issue.pluralVariant === v)
  );
}
