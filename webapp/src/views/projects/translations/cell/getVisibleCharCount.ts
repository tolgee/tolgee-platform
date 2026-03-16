import { getPlaceholders } from '@tginternal/editor';

type Params = {
  text: string | undefined | null;
  nested?: boolean;
};

export function getVisibleCharCount({ text, nested }: Params): number {
  if (!text) return 0;
  const placeholders = getPlaceholders(text, nested);
  if (!placeholders) return text.length;
  const placeholderChars = placeholders.reduce(
    (sum, p) => sum + (p.position.end - p.position.start),
    0
  );
  return text.length - placeholderChars;
}
