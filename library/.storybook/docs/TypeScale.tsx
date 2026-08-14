import { Box } from '@mui/material';
import { DocsTheme } from './DocsTheme';
import { useThemes } from './themes';

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
    <DocsTheme>
      <Box sx={{ my: 2, color: 'text.primary' }}>
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
                borderBottom: (theme) => `1px solid ${theme.palette.divider}`,
              }}
            >
              <Box
                sx={{
                  fontFamily: 'monospace',
                  typography: 'overline',
                  minWidth: 170,
                  color: 'text.secondary',
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
    </DocsTheme>
  );
};
