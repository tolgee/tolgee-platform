const REGIONAL_INDICATOR_A = 0x1f1e6;
const LETTER_A = 'A'.charCodeAt(0);

/**
 * FlagImage resolves its SVG from a flag emoji, while sessions store an ISO 3166-1 alpha-2 code.
 * The two map onto each other by offsetting each letter into the regional indicator block.
 */
export function countryCodeToFlagEmoji(
  countryCode: string
): string | undefined {
  const code = countryCode.trim().toUpperCase();
  if (!/^[A-Z]{2}$/.test(code)) {
    return undefined;
  }

  return String.fromCodePoint(
    ...[...code].map(
      (letter) => REGIONAL_INDICATOR_A + letter.charCodeAt(0) - LETTER_A
    )
  );
}
