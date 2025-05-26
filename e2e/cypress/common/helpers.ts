export const rgbToHex = (rgb: string): string => {
  const result = rgb.match(/\d+/g);
  if (!result) return '';
  return (
    '#' +
    result
      .slice(0, 3)
      .map((x) => (+x).toString(16).padStart(2, '0').toUpperCase())
      .join('')
  );
};

export const isDarkMode = window.matchMedia(
  '(prefers-color-scheme: dark)'
).matches;
