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
