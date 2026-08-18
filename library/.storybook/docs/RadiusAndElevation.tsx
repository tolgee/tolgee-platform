import { Box } from '@mui/material';
import { DocsTheme } from './DocsTheme';
import { useThemes } from './themes';

/** The levels MUI components actually reach for; the array has 25 and the rest are unused here. */
const LEVELS = [0, 1, 2, 3, 4, 6, 8, 12, 16, 24];

export const RadiusAndElevation = () => {
  const { active } = useThemes();
  const base = active.shape.borderRadius;

  return (
    <DocsTheme>
      <Box sx={{ my: 2, color: 'text.primary' }}>
        <Box sx={{ typography: 'button', mb: 1 }}>Radius</Box>
        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', mb: 4 }}>
          {[0.5, 1, 2, 3].map((step) => (
            <Box key={step} sx={{ textAlign: 'center' }}>
              <Box
                sx={{
                  width: 72,
                  height: 48,
                  borderRadius: step,
                  border: (theme) => `1px solid ${theme.palette.divider}`,
                  background: (theme) => theme.palette.background.paper,
                }}
              />
              <Box sx={{ fontFamily: 'monospace', typography: 'overline' }}>
                {step} · {base * step}px
              </Box>
            </Box>
          ))}
        </Box>

        <Box sx={{ typography: 'button', mb: 1 }}>Elevation</Box>
        <Box sx={{ display: 'flex', gap: 3, flexWrap: 'wrap' }}>
          {LEVELS.map((level) => (
            <Box key={level} sx={{ textAlign: 'center' }}>
              <Box
                sx={{
                  width: 72,
                  height: 48,
                  borderRadius: 1,
                  background: (theme) => theme.palette.background.paper,
                  boxShadow: active.shadows[level],
                }}
              />
              <Box sx={{ fontFamily: 'monospace', typography: 'overline' }}>
                {level}
              </Box>
            </Box>
          ))}
        </Box>
      </Box>
    </DocsTheme>
  );
};
