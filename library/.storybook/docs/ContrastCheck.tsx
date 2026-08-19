import { Box } from '@mui/material';
import { DocsTheme } from './DocsTheme';
import { useThemes } from './themes';

const AA_NORMAL = 4.5;

const hexToRgba = (hex: string): [number, number, number, number] => {
  const h = hex.replace('#', '');
  const full =
    h.length <= 4
      ? h
          .split('')
          .map((c) => c + c)
          .join('')
      : h;
  const n = (i: number) => parseInt(full.slice(i, i + 2), 16);
  return [n(0), n(2), n(4), full.length === 8 ? n(6) / 255 : 1];
};

const over = (
  fg: [number, number, number, number],
  bg: [number, number, number, number],
): [number, number, number] => [
  fg[0] * fg[3] + bg[0] * (1 - fg[3]),
  fg[1] * fg[3] + bg[1] * (1 - fg[3]),
  fg[2] * fg[3] + bg[2] * (1 - fg[3]),
];

const luminance = ([r, g, b]: [number, number, number]) => {
  const s = [r, g, b].map((v) => {
    const c = v / 255;
    return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
  });
  return 0.2126 * s[0] + 0.7152 * s[1] + 0.0722 * s[2];
};

const ratio = (fg: string, bg: string) => {
  const bgRgba = hexToRgba(bg);
  const bgRgb: [number, number, number] = [bgRgba[0], bgRgba[1], bgRgba[2]];
  const fgRgb = over(hexToRgba(fg), [255, 255, 255, 1]);
  const composited = hexToRgba(fg)[3] < 1 ? over(hexToRgba(fg), bgRgba) : fgRgb;
  const [l1, l2] = [luminance(composited), luminance(bgRgb)];
  return (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05);
};

const resolve = (palette: unknown, path: string) =>
  path
    .split('.')
    .reduce<unknown>(
      (acc, key) => (acc as Record<string, unknown>)?.[key],
      palette,
    ) as string;

type Props = {
  /** Dotted path into `theme.palette`, e.g. `tokens.primary.contrast`. */
  fg: string;
  /** Dotted path into `theme.palette`, e.g. `tokens.primary.main`. */
  bg: string;
  label: string;
};

/**
 * Measures a token pair against WCAG AA at render time, so the verdict cannot
 * go stale: fix the token and the failure disappears on its own.
 */
export const ContrastCheck = ({ fg, bg, label }: Props) => {
  const { active } = useThemes();
  const fgColor = resolve(active.palette, fg);
  const bgColor = resolve(active.palette, bg);
  const value = ratio(fgColor, bgColor);
  const passes = value >= AA_NORMAL;

  return (
    <DocsTheme>
      <Box
        sx={{
          my: 2,
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          gap: 2,
          p: 2,
          borderRadius: 1,
          border: (theme) =>
            `1px solid ${
              passes ? theme.palette.divider : theme.palette.error.main
            }`,
          color: 'text.primary',
        }}
      >
        <Box
          sx={{
            px: 2,
            py: 1,
            borderRadius: 0.5,
            background: bgColor,
            color: fgColor,
            typography: 'button',
          }}
        >
          Sample
        </Box>
        <Box>
          <Box sx={{ typography: 'body2' }}>{label}</Box>
          <Box
            sx={{ typography: 'caption', fontFamily: 'monospace' }}
          >{`${fg} on ${bg}`}</Box>
        </Box>
        <Box sx={{ ml: 'auto', textAlign: 'right' }}>
          <Box
            sx={{
              typography: 'button',
              color: (theme) =>
                passes ? theme.palette.success.main : theme.palette.error.main,
            }}
          >
            {value.toFixed(2)} : 1
          </Box>
          <Box sx={{ typography: 'caption', color: 'text.secondary' }}>
            {passes ? 'passes AA' : `below AA (needs ${AA_NORMAL})`}
          </Box>
        </Box>
      </Box>
    </DocsTheme>
  );
};
