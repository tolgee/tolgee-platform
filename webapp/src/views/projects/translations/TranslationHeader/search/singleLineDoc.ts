export type SingleLineResolution =
  | { action: 'allow' }
  | { action: 'reject' }
  | { action: 'flatten'; value: string };

export const toSingleLineDoc = (value: string): string =>
  value.replace(/\s*\n\s*/g, ' ');

export function resolveSingleLineDoc(
  previous: string,
  next: string,
  lines: number
): SingleLineResolution {
  if (lines <= 1) {
    return { action: 'allow' };
  }
  if (next.replace(/\n/g, '') === previous) {
    return { action: 'reject' };
  }
  return { action: 'flatten', value: toSingleLineDoc(next) };
}
