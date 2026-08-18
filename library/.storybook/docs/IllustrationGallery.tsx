import { Box } from '@mui/material';
import * as illustrations from '../../src/illustrations';
import { DocsTheme } from './DocsTheme';

type Illustration = (props: { width?: number }) => JSX.Element;

/** Where each one belongs. An illustration is documented by the screen it serves. */
const BELONGS_TO: Record<string, string> = {
  GlossaryEmpty: 'The glossary, before the first term has been added.',
  SelfHostedPlaceholder: 'The self-hosted plans list, in place of a plan card.',
};

export const IllustrationGallery = () => (
  <DocsTheme>
    <Box sx={{ my: 2, color: 'text.primary' }}>
      {Object.entries(illustrations as Record<string, Illustration>).map(
        ([name, Illustration]) => (
          <Box
            key={name}
            sx={{
              display: 'grid',
              gridTemplateColumns: '160px 1fr',
              alignItems: 'center',
              gap: 3,
              py: 2,
              borderBottom: (theme) => `1px solid ${theme.palette.divider}`,
            }}
          >
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                p: 1,
                borderRadius: 1,
                background: (theme) => theme.palette.background.paper,
              }}
            >
              <Illustration width={120} />
            </Box>
            <Box>
              <Box sx={{ fontFamily: 'monospace', typography: 'caption' }}>
                {name}
              </Box>
              <Box sx={{ typography: 'body2', mt: 0.5 }}>
                {BELONGS_TO[name] ?? (
                  <Box component="span" sx={{ color: 'error.main' }}>
                    Not documented yet — say where it belongs in
                    .storybook/docs/IllustrationGallery.tsx
                  </Box>
                )}
              </Box>
            </Box>
          </Box>
        ),
      )}
    </Box>
  </DocsTheme>
);
