import { useState } from 'react';
import { Box } from '@mui/material';
import { DocsTheme } from './DocsTheme';
import { IconFlags } from './IconFlags';
import { UNUSED_HINT } from './iconHints';
import type { IconSource } from './iconMeta';
import { ICON_MEANINGS, anchorOf, iconEntries } from './iconMeta';

const SOURCES: { source: IconSource; label: string }[] = [
  { source: 'untitled', label: 'Untitled UI' },
  { source: 'custom', label: 'Tolgee custom icons' },
];

export const IconMeanings = () => {
  const [query, setQuery] = useState('');
  const q = query.trim().toLowerCase();

  const rows = SOURCES.flatMap(({ source, label }) =>
    iconEntries(source)
      .map(([name, Icon]) => ({
        source,
        label,
        name,
        Icon,
        meaning: ICON_MEANINGS[source][name],
      }))
      .filter(
        ({ name, meaning }) =>
          !q ||
          name.toLowerCase().includes(q) ||
          (meaning?.means.toLowerCase().includes(q) ?? false),
      ),
  );

  return (
    <DocsTheme>
      <Box sx={{ my: 2, color: 'text.primary' }}>
        <Box
          component="input"
          value={query}
          placeholder="Filter by name or meaning…"
          onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
            setQuery(e.target.value)
          }
          sx={{
            width: '100%',
            p: 1,
            mb: 2,
            typography: 'body2',
            color: 'text.primary',
            background: (theme) => theme.palette.background.paper,
            border: (theme) => `1px solid ${theme.palette.divider}`,
            borderRadius: 1,
          }}
        />

        {rows.map(({ source, label, name, Icon, meaning }) => (
          <Box
            key={`${source}-${name}`}
            id={anchorOf(source, name)}
            sx={{
              display: 'grid',
              gridTemplateColumns: '28px 210px 1fr',
              alignItems: 'start',
              gap: 2,
              py: 1,
              borderBottom: (theme) => `1px solid ${theme.palette.divider}`,
              scrollMarginTop: '80px',
            }}
          >
            <Box sx={{ pt: 0.25 }}>
              <Icon width={20} height={20} />
            </Box>
            <Box>
              <Box
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 1,
                  fontFamily: 'monospace',
                  typography: 'caption',
                }}
              >
                <IconFlags meaning={meaning} placement="inline" />
                {name}
              </Box>
              <Box sx={{ typography: 'overline', color: 'text.secondary' }}>
                {label}
              </Box>
            </Box>
            <Box>
              <Box sx={{ typography: 'body2' }}>
                {meaning?.means ?? (
                  <Box component="span" sx={{ color: 'error.main' }}>
                    No meaning documented yet — add it to
                    .storybook/docs/iconMeta.ts
                  </Box>
                )}
              </Box>
              {meaning?.prefer && (
                <Box sx={{ typography: 'caption', color: 'text.secondary' }}>
                  Overlaps with <strong>{meaning.prefer}</strong> — prefer that
                  one in new code.
                </Box>
              )}
              {meaning?.unused && (
                <Box sx={{ typography: 'caption', color: 'text.secondary' }}>
                  {UNUSED_HINT}.
                </Box>
              )}
            </Box>
          </Box>
        ))}

        {!rows.length && (
          <Box sx={{ typography: 'body2', color: 'text.secondary' }}>
            Nothing matches “{query}”.
          </Box>
        )}
      </Box>
    </DocsTheme>
  );
};
