import { Box, Theme } from '@mui/material';
import { DocsTheme } from './DocsTheme';
import { useThemes } from './themes';
import { byName, flatten, groupByParent, splitName } from './utils';

export type PaletteGroup = keyof Theme['palette'];

type SwatchProps = {
  label: string;
  value: string;
  otherLabel?: string;
  otherValue?: string;
};

const Swatch = ({ label, value, otherLabel, otherValue }: SwatchProps) => (
  <Box>
    <Box
      sx={{
        height: (theme) => theme.spacing(5),
        borderRadius: 0.5,
        background: value,
        border: (theme) => `1px solid ${theme.palette.divider}`,
      }}
    />
    <Box sx={{ typography: 'caption', fontWeight: 'bold', mt: 0.5 }}>
      {label}
    </Box>
    <Box sx={{ fontFamily: 'monospace', typography: 'overline' }}>{value}</Box>
    {otherLabel && (
      <Box
        sx={{
          fontFamily: 'monospace',
          typography: 'overline',
          color: 'text.secondary',
        }}
      >
        {otherLabel}: {otherValue ?? '—'}
      </Box>
    )}
  </Box>
);

export const Swatches = ({ group }: { group: PaletteGroup }) => {
  const { active, other, dark } = useThemes();
  const values = flatten(active.palette[group]);
  const otherValues = flatten(other.palette[group]);
  const keys = Object.keys(values).sort(byName);

  if (!keys.length) {
    return (
      <DocsTheme>
        <Box
          sx={{
            fontFamily: 'monospace',
            typography: 'body2',
            color: 'error.main',
          }}
        >
          palette.{group} holds no color values
        </Box>
      </DocsTheme>
    );
  }

  return (
    <DocsTheme>
      <Box sx={{ my: 2, color: 'text.primary' }}>
        {[...groupByParent(keys).entries()]
          .sort(([a], [b]) => byName(a, b))
          .map(([subGroup, groupKeys]) => (
            <Box key={subGroup} sx={{ mb: 2.5 }}>
              {subGroup && (
                <Box
                  sx={{
                    fontFamily: 'monospace',
                    typography: 'caption',
                    fontWeight: 'bold',
                    pb: 0.5,
                    mb: 1,
                    borderBottom: (theme) =>
                      `1px solid ${theme.palette.divider}`,
                  }}
                >
                  {subGroup}
                </Box>
              )}
              <Box
                sx={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fill, minmax(150px, 1fr))',
                  gap: 1.5,
                }}
              >
                {groupKeys.map((k) => (
                  <Swatch
                    key={k}
                    label={splitName(k).label || String(group)}
                    value={values[k]}
                    otherLabel={
                      otherValues[k] !== values[k]
                        ? dark
                          ? 'light'
                          : 'dark'
                        : undefined
                    }
                    otherValue={otherValues[k]}
                  />
                ))}
              </Box>
            </Box>
          ))}
      </Box>
    </DocsTheme>
  );
};
