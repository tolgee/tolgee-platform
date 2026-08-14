import { Box } from '@mui/material';
import { DocsTheme } from './DocsTheme';
import { useThemes } from './themes';

export const ComponentOverrides = () => {
  const { active } = useThemes();
  const names = Object.keys(active.components ?? {}).sort();
  return (
    <DocsTheme>
      <Box sx={{ my: 2, color: 'text.primary' }}>
        <Box sx={{ typography: 'button', mb: 1 }}>
          {names.length} MUI components are restyled by the theme:
        </Box>
        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
          {names.map((n) => (
            <Box
              key={n}
              sx={{
                px: 1,
                py: 0.5,
                border: (theme) => `1px solid ${theme.palette.divider}`,
                borderRadius: 0.5,
                fontFamily: 'monospace',
                typography: 'caption',
              }}
            >
              {n}
            </Box>
          ))}
        </Box>
      </Box>
    </DocsTheme>
  );
};
