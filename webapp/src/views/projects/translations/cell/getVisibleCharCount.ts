import { getPlaceholders } from '@tginternal/editor';

export function getVisibleCharCount(
  text: string | undefined | null,
  nested?: boolean
): number {
  if (!text) return 0;
  const placeholders = getPlaceholders(text, nested);
  if (!placeholders) return text.length;
  const placeholderChars = placeholders.reduce(
    (sum, p) => sum + (p.position.end - p.position.start),
    0
  );
  return text.length - placeholderChars;
}
