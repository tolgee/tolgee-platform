import { Box } from '@mui/material';
import { DocsTheme } from './DocsTheme';
import { IconFlags } from './IconFlags';
import { flagHint } from './iconHints';
import type { IconSource } from './iconMeta';
import { ICON_MEANINGS, anchorOf, iconEntries } from './iconMeta';

/**
 * The glyphs alone, dense enough to be scanned in one look. Clicking one jumps to its entry in the
 * meanings list, because that is the thing the glyph cannot tell you.
 */
export const IconGrid = ({ source }: { source: IconSource }) => (
  <DocsTheme>
    <Box
      sx={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fill, minmax(44px, 1fr))',
        gap: 0.5,
        my: 2,
      }}
    >
      {iconEntries(source).map(([name, Icon]) => (
        <Box
          key={name}
          component="a"
          href={`#${anchorOf(source, name)}`}
          title={[
            `${name} — ${ICON_MEANINGS[source][name]?.means ?? 'no meaning yet'}`,
            flagHint(ICON_MEANINGS[source][name]),
          ]
            .filter(Boolean)
            .join('\n')}
          sx={{
            position: 'relative',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            height: 44,
            borderRadius: 1,
            color: 'text.primary',
            border: (theme) => `1px solid ${theme.palette.divider}`,
            '&:hover': {
              background: (theme) => theme.palette.background.paper,
            },
          }}
        >
          <Icon width={20} height={20} />
          <IconFlags meaning={ICON_MEANINGS[source][name]} placement="tile" />
        </Box>
      ))}
    </Box>
  </DocsTheme>
);
