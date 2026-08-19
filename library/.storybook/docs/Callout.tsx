import type { ReactNode } from 'react';
import { Box } from '@mui/material';
import { DocsTheme } from './DocsTheme';

type Props = {
  /** Heading shown above the body; keep it to a few words. */
  title: string;
  kind?: 'info' | 'warning';
  children: ReactNode;
};

/**
 * The accent is the only tinted part: the surface stays `background.paper` and the
 * text stays `text.primary`, so the block cannot fall below the contrast the rest of
 * the page already meets.
 */
export const Callout = ({ title, kind = 'info', children }: Props) => (
  <DocsTheme>
    <Box
      sx={{
        my: 3,
        px: 2.5,
        py: 2,
        borderRadius: 1,
        color: 'text.primary',
        background: (theme) => theme.palette.background.paper,
        border: (theme) =>
          `1px solid ${
            kind === 'warning'
              ? theme.palette.warning.main
              : theme.palette.info.main
          }`,
        borderLeftWidth: 4,
      }}
    >
      <Box sx={{ typography: 'subtitle2', mb: 1 }}>{title}</Box>
      <Box
        sx={{
          typography: 'body2',
          '& p': { margin: 0, marginBottom: '8px' },
          '& p:last-child': { marginBottom: 0 },
          '& ul': { margin: 0, paddingLeft: '20px' },
          '& li': { marginBottom: '4px' },
        }}
      >
        {children}
      </Box>
    </Box>
  </DocsTheme>
);
