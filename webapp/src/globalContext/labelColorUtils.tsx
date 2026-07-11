import { Theme } from '@mui/material';
import {
  adjustColorBrightness,
  hexToRgb,
  RGB,
  rgbToHex,
} from 'tg.globalContext/colorUtils';

const DARK_MODE_OPACITY = 0.85;
const CONTRAST = 150;
const MIN_LIGHT_VALUE = 0xe1; // 225
const BRIGHTNESS_THRESHOLD = 128;
const LOW_BRIGHTNESS_THRESHOLD = 100;
const COLOR_DIFFERENCE_THRESHOLD = 50;
const HIGH_COLOR_VALUE = 200;

/**
 * Ensures that a light text color meets minimum brightness levels for readability.
 */
export function fixLightTextColor(textColor: string): string {
  const { r, g, b } = hexToRgb(textColor);
  return rgbToHex({
    r: Math.max(r, MIN_LIGHT_VALUE),
    g: Math.max(g, MIN_LIGHT_VALUE),
    b: Math.max(b, MIN_LIGHT_VALUE),
  }).toUpperCase();
}

/**
 * Gets the background color based on the theme and provided hex.
 */
export function getLabelBackgroundColor(theme: Theme, hex?: string): string {
  if (!hex) return 'transparent';

  if (theme.palette.mode === 'dark') {
    const { r, g, b } = hexToRgb(hex);
    return `rgba(${r}, ${g}, ${b}, ${DARK_MODE_OPACITY})`;
  }

  return hex;
}

/**
 * Determines if a white text should be used based on color composition
 */
function shouldUseWhiteText(rgb: RGB, textRgb: RGB): boolean {
  const { r, g, b } = rgb;
  const { r: textR, g: textG, b: textB } = textRgb;

  if (
    g > r &&
    g > b &&
    textB > textG + COLOR_DIFFERENCE_THRESHOLD &&
    textB > HIGH_COLOR_VALUE
  ) {
    return true;
  }

  if (
    b > r &&
    b > g &&
    textR > textB + COLOR_DIFFERENCE_THRESHOLD &&
    textR > HIGH_COLOR_VALUE
  ) {
    return true;
  }

  return false;
}

/**
 * Calculates brightness of a color using RGB values.
 */
export function calculateBrightness({ r, g, b }: RGB): number {
  return (r * 299 + g * 587 + b * 114) / 1000;
}

/**
 * Gets the translation label text color appropriate for a given background color
 */
export function getLabelTextColor(theme: Theme, color?: string): string {
  if (!color) return theme.palette.tooltip.text;

  const rgb = hexToRgb(color);
  const brightness = calculateBrightness(rgb);

  let adjustedColor = adjustColorBrightness(
    color,
    brightness > BRIGHTNESS_THRESHOLD ? -CONTRAST : CONTRAST
  );

  if (brightness < BRIGHTNESS_THRESHOLD) {
    adjustedColor = fixLightTextColor(adjustedColor);
    const textRgb = hexToRgb(adjustedColor);

    if (
      shouldUseWhiteText(rgb, textRgb) ||
      brightness < LOW_BRIGHTNESS_THRESHOLD
    ) {
      return '#ffffff';
    }
  }

  return adjustedColor;
}
