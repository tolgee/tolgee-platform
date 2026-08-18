import { Box } from '@mui/material';
import { DocsTheme } from './DocsTheme';
import { useThemes } from './themes';

const MEANS: Record<string, string> = {
  xs: 'Phone. The base — styles outside any query apply here.',
  sm: 'Large phone and small tablet. The one the app uses most.',
  md: 'Tablet and small laptop.',
  lg: 'Laptop and desktop.',
  xl: 'Wide desktop.',
};

export const Breakpoints = () => {
  const { active } = useThemes();
  const { values, keys } = active.breakpoints;
  const max = values[keys[keys.length - 1]];

  return (
    <DocsTheme>
      <Box sx={{ my: 2, color: 'text.primary' }}>
        {keys.map((key) => (
          <Box
            key={key}
            sx={{
              display: 'grid',
              gridTemplateColumns: '60px 90px 160px 1fr',
              alignItems: 'center',
              gap: 2,
              py: 1,
              borderBottom: (theme) => `1px solid ${theme.palette.divider}`,
            }}
          >
            <Box sx={{ fontFamily: 'monospace', typography: 'caption' }}>
              {key}
            </Box>
            <Box sx={{ fontFamily: 'monospace', typography: 'overline' }}>
              {values[key]}px
            </Box>
            <Box
              sx={{
                height: 10,
                borderRadius: 0.5,
                width: `${Math.max(6, (values[key] / max) * 100)}%`,
                background: (theme) => theme.palette.tokens.primary.main,
              }}
            />
            <Box sx={{ typography: 'body2', color: 'text.secondary' }}>
              {MEANS[key]}
            </Box>
          </Box>
        ))}
      </Box>
    </DocsTheme>
  );
};
