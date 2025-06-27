/**
 * Generated with AI
 */
export function getEffectiveBackgroundColor(element: HTMLElement): string {
  // Type for RGBA color as an array of numbers [r, g, b, a]
  type RGBA = [number, number, number, number];

  // Helper to parse rgba or rgb color to [r, g, b, a]
  function parseColor(color: string): RGBA {
    if (!color || color === 'transparent') return [0, 0, 0, 0];
    const rgbaMatch = color.match(
      /rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*([\d.]+))?\)/
    );
    if (rgbaMatch) {
      return [
        parseInt(rgbaMatch[1]), // r
        parseInt(rgbaMatch[2]), // g
        parseInt(rgbaMatch[3]), // b
        rgbaMatch[4] ? parseFloat(rgbaMatch[4]) : 1, // a (default to 1 if not specified)
      ];
    }
    // Handle hex or named colors using canvas for better performance
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');
    if (ctx) {
      ctx.fillStyle = color;
      const computedColor = ctx.fillStyle;
      // Parse hex format
      if (computedColor.startsWith('#')) {
        const hex = computedColor.slice(1);
        const r = parseInt(hex.substring(0, 2), 16);
        const g = parseInt(hex.substring(2, 4), 16);
        const b = parseInt(hex.substring(4, 6), 16);
        return [r, g, b, 1];
      }
    }
    return [0, 0, 0, 0]; // Fallback for unrecognized colors
  }

  // Blend two colors (top over bottom) using alpha compositing
  function blendColors(bottom: RGBA, top: RGBA): RGBA {
    const [r1, g1, b1, a1] = bottom;
    const [r2, g2, b2, a2] = top;
    const aOut = a2 + a1 * (1 - a2); // Resulting alpha
    if (aOut === 0) return [0, 0, 0, 0]; // Fully transparent
    const rOut = Math.round((r2 * a2 + r1 * a1 * (1 - a2)) / aOut);
    const gOut = Math.round((g2 * a2 + g1 * a1 * (1 - a2)) / aOut);
    const bOut = Math.round((b2 * a2 + b1 * a1 * (1 - a2)) / aOut);
    return [rOut, gOut, bOut, aOut];
  }

  // Collect all parents up to document.body
  const elements: HTMLElement[] = [];
  let current: HTMLElement | null = element;
  while (current && current !== document.documentElement) {
    elements.push(current);
    current = current.parentElement;
  }

  // Start with a default background (white, fully opaque)
  let resultColor: RGBA = [255, 255, 255, 1];

  // Process from topmost ancestor (body) to the element
  for (const el of elements.reverse()) {
    const style = getComputedStyle(el);
    const bgColor = parseColor(style.backgroundColor);
    const opacity = parseFloat(style.opacity) || 1;

    // Adjust the color's alpha by the element's opacity
    bgColor[3] *= opacity;

    // Blend the current element's background with the accumulated result
    resultColor = blendColors(resultColor, bgColor);
  }

  // Format the result as an rgba string
  const [r, g, b, a] = resultColor;
  return `rgba(${r}, ${g}, ${b}, ${a})`;
}
