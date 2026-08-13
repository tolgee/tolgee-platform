import { Box } from '@mui/material';
import { getTheme } from '../../src/theme/getTheme';
import { useThemeMode } from './useThemeMode';

const LIGHT = getTheme('light');
const DARK = getTheme('dark');

const useThemes = () => {
  const dark = useThemeMode() === 'dark';
  return { active: dark ? DARK : LIGHT, other: dark ? LIGHT : DARK, dark };
};

/** `green.500` groups under `green`; `_components.alert.error.background` under its parent path. */
const splitName = (key: string) => {
  const i = key.lastIndexOf('.');
  return i === -1
    ? { group: '', label: key }
    : { group: key.slice(0, i), label: key.slice(i + 1) };
};

const isColor = (v: unknown): v is string =>
  typeof v === 'string' && /^(#|rgb|hsl)/.test(v.trim());

const flatten = (value: unknown, prefix = ''): Record<string, string> => {
  if (isColor(value)) return { [prefix]: value };
  if (!value || typeof value !== 'object') return {};
  return Object.entries(value).reduce(
    (acc, [k, v]) => ({ ...acc, ...flatten(v, prefix ? `${prefix}.${k}` : k) }),
    {},
  );
};

const at = (theme: unknown, path: string) =>
  path
    .split('.')
    .reduce<unknown>((acc, k) => (acc as Record<string, unknown>)?.[k], theme);

// Numeric-aware, so shade 50 sorts before shade 100.
const byName = (a: string, b: string) =>
  a.localeCompare(b, 'en', { numeric: true, sensitivity: 'base' });

// Base value first, then emphasis levels, then interaction states, then borders, then surfaces.
// Names outside this list — numeric shades in particular — keep their numeric-aware order after it.
const LABEL_ORDER = [
  'main',
  'dark',
  'light',
  'contrast',
  'contrastText',
  'primary',
  'secondary',
  'tertiary',
  'disabled',
  'disabledBackground',
  'hover',
  'selected',
  'focus',
  'focusVisible',
  'active',
  'outlinedBorder',
  'enabledBorder',
  'hoverBorder',
  'background',
  'backgroundDark',
  'backgroundDarkHover',
  'fill',
  'color',
  'onDark',
  'onDarkHover',
].map((n) => n.toLowerCase());

const rankOf = (label: string) => {
  const i = LABEL_ORDER.indexOf(label.toLowerCase());
  return i === -1 ? LABEL_ORDER.length : i;
};

const byLabel = (a: string, b: string) => {
  const la = splitName(a).label;
  const lb = splitName(b).label;
  const diff = rankOf(la) - rankOf(lb);
  return diff !== 0 ? diff : byName(la, lb);
};

export const Swatches = ({ path }: { path: string }) => {
  const { active, other, dark } = useThemes();
  const values = flatten(at(active, path));
  const otherValues = flatten(at(other, path));
  const keys = Object.keys(values).sort(byName);

  if (!keys.length) {
    return (
      <Box sx={{ fontFamily: 'monospace', fontSize: 13, color: 'error.main' }}>
        {path} holds no color values
      </Box>
    );
  }

  const groups = new Map<string, string[]>();
  keys.forEach((k) => {
    const { group } = splitName(k);
    groups.set(group, [...(groups.get(group) ?? []), k]);
  });
  groups.forEach((groupKeys) => groupKeys.sort(byLabel));

  return (
    <Box sx={{ my: 2, color: active.palette.text.primary }}>
      {[...groups.entries()]
        .sort(([a], [b]) => byName(a, b))
        .map(([group, groupKeys]) => (
          <Box key={group} sx={{ mb: 2.5 }}>
            {group && (
              <Box
                sx={{
                  fontFamily: 'monospace',
                  fontSize: 12,
                  fontWeight: 700,
                  pb: 0.5,
                  mb: 1,
                  borderBottom: `1px solid ${active.palette.divider}`,
                }}
              >
                {group}
              </Box>
            )}
            <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(150px, 1fr))',
                gap: 1.5,
              }}
            >
              {groupKeys.map((k) => {
                const value = values[k];
                const differs = otherValues[k] !== value;
                return (
                  <Box key={k}>
                    <Box
                      sx={{
                        height: 40,
                        borderRadius: 0.5,
                        background: value,
                        border: `1px solid ${active.palette.divider}`,
                      }}
                    />
                    <Box sx={{ fontSize: 11.5, fontWeight: 600, mt: 0.5 }}>
                      {splitName(k).label || path.split('.').pop()}
                    </Box>
                    <Box sx={{ fontFamily: 'monospace', fontSize: 10.5 }}>
                      {value}
                    </Box>
                    {differs && (
                      <Box
                        sx={{
                          fontFamily: 'monospace',
                          fontSize: 10,
                          opacity: 0.6,
                        }}
                      >
                        {dark ? 'light' : 'dark'}: {otherValues[k] ?? '—'}
                      </Box>
                    )}
                  </Box>
                );
              })}
            </Box>
          </Box>
        ))}
    </Box>
  );
};

const VARIANTS = [
  'h1',
  'h2',
  'h3',
  'h4',
  'h5',
  'h6',
  'subtitle1',
  'subtitle2',
  'body1',
  'body2',
  'button',
  'caption',
  'overline',
] as const;

export const TypeScale = () => {
  const { active } = useThemes();
  return (
    <Box sx={{ my: 2, color: active.palette.text.primary }}>
      {VARIANTS.map((v) => {
        const style = active.typography[v];
        return (
          <Box
            key={v}
            sx={{
              display: 'flex',
              alignItems: 'baseline',
              gap: 2,
              py: 1,
              borderBottom: `1px solid ${active.palette.divider}`,
            }}
          >
            <Box
              sx={{
                fontFamily: 'monospace',
                fontSize: 11,
                minWidth: 170,
                opacity: 0.7,
              }}
            >
              {v} · {String(style.fontSize)} · {String(style.fontWeight)}
            </Box>
            <Box
              sx={{
                fontSize: style.fontSize,
                fontWeight: style.fontWeight,
                fontFamily: active.typography.fontFamily,
              }}
            >
              Tolgee
            </Box>
          </Box>
        );
      })}
    </Box>
  );
};

export const ComponentOverrides = () => {
  const { active } = useThemes();
  const names = Object.keys(active.components ?? {}).sort();
  return (
    <Box sx={{ my: 2, color: active.palette.text.primary }}>
      <Box sx={{ fontSize: 14, mb: 1 }}>
        {names.length} MUI components are restyled by the theme:
      </Box>
      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
        {names.map((n) => (
          <Box
            key={n}
            sx={{
              px: 1,
              py: 0.5,
              border: `1px solid ${active.palette.divider}`,
              borderRadius: 0.5,
              fontFamily: 'monospace',
              fontSize: 12,
            }}
          >
            {n}
          </Box>
        ))}
      </Box>
    </Box>
  );
};
