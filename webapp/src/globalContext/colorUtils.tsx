export interface RGB {
  r: number;
  g: number;
  b: number;
}

/**
 * Converts a hex color to an RGB object.
 */
export function hexToRgb(hex: string): RGB {
  const cleanHex = hex.replace('#', '');
  const normalizedHex =
    cleanHex.length === 3
      ? cleanHex
          .split('')
          .map((c) => c + c)
          .join('')
      : cleanHex;

  const r = parseInt(normalizedHex.slice(0, 2), 16);
  const g = parseInt(normalizedHex.slice(2, 4), 16);
  const b = parseInt(normalizedHex.slice(4, 6), 16);
  return { r, g, b };
}

/**
 * Converts RGB values to hex color string.
 */
export function rgbToHex({ r, g, b }: RGB): string {
  const toHex = (n: number) =>
    Math.max(0, Math.min(255, n)).toString(16).padStart(2, '0');
  return `#${toHex(r)}${toHex(g)}${toHex(b)}`;
}

/**
 * Adjusts the brightness of a hex color by the specified amount.
 */
export function adjustColorBrightness(hex: string, amount: number): string {
  const { r, g, b } = hexToRgb(hex);
  return rgbToHex({
    r: r + amount,
    g: g + amount,
    b: b + amount,
  });
}
