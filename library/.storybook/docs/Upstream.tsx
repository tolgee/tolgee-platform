import { Box } from '@mui/material';
import { DocsTheme } from './DocsTheme';

type Props = {
  /** Component name as it appears in the Material UI docs URL, e.g. `react-button`. */
  docs: string;
};

export const Upstream = ({ docs }: Props) => (
  <DocsTheme>
    <Box
      sx={{
        my: 2,
        px: 2,
        py: 1.5,
        display: 'flex',
        flexWrap: 'wrap',
        alignItems: 'center',
        gap: 1,
        borderRadius: 1,
        border: (theme) => `1px solid ${theme.palette.divider}`,
        color: 'text.primary',
        typography: 'body2',
      }}
    >
      <Box component="span">
        Comes from <strong>Material UI</strong>
      </Box>
      <Box
        component="span"
        sx={{
          px: 0.75,
          py: 0.25,
          borderRadius: 0.5,
          fontFamily: 'monospace',
          typography: 'caption',
          border: (theme) => `1px solid ${theme.palette.divider}`,
        }}
      >
        v{import.meta.env.VITE_MUI_VERSION}
      </Box>
      <Box component="span" sx={{ color: 'text.secondary' }}>
        — Tolgee does not own this code, only its styling.
      </Box>
      <Box
        component="a"
        href={`https://mui.com/material-ui/${docs}/`}
        target="_blank"
        rel="noreferrer"
      >
        API and props ↗
      </Box>
    </Box>
  </DocsTheme>
);
