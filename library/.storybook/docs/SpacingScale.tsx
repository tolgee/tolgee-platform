import { Box } from '@mui/material';
import { DocsTheme } from './DocsTheme';
import { useThemes } from './themes';

const STEPS = [0.5, 1, 1.5, 2, 2.5, 3, 4, 5, 6, 8];

export const SpacingScale = () => {
  const { active } = useThemes();
  return (
    <DocsTheme>
      <Box sx={{ my: 2, color: 'text.primary' }}>
        {STEPS.map((step) => (
          <Box
            key={step}
            sx={{
              display: 'grid',
              gridTemplateColumns: '120px 80px 1fr',
              alignItems: 'center',
              gap: 2,
              py: 0.75,
              borderBottom: (theme) => `1px solid ${theme.palette.divider}`,
            }}
          >
            <Box sx={{ fontFamily: 'monospace', typography: 'caption' }}>
              spacing({step})
            </Box>
            <Box sx={{ fontFamily: 'monospace', typography: 'overline' }}>
              {active.spacing(step)}
            </Box>
            <Box
              sx={{
                height: 12,
                width: active.spacing(step),
                borderRadius: 0.5,
                background: (theme) => theme.palette.tokens.primary.main,
              }}
            />
          </Box>
        ))}
      </Box>
    </DocsTheme>
  );
};
