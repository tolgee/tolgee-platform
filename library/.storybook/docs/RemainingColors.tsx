import { Box } from '@mui/material';
import { Swatches } from './Swatches';
import type { PaletteGroup } from './Swatches';
import { CURATED_GROUPS } from './colorSections';
import { DocsTheme } from './DocsTheme';
import { useThemes } from './themes';
import { byName, flatten } from './utils';

export const RemainingColors = () => {
  const { active } = useThemes();
  const rest = (Object.keys(active.palette) as PaletteGroup[])
    .filter((g) => !CURATED_GROUPS.includes(g))
    .filter((g) => Object.keys(flatten(active.palette[g])).length > 0)
    .sort(byName);

  return (
    <DocsTheme>
      {rest.map((group) => (
        <Box key={String(group)} sx={{ mb: 1 }}>
          <Box sx={{ fontFamily: 'monospace', typography: 'button' }}>
            palette.{String(group)}
          </Box>
          <Swatches group={group} />
        </Box>
      ))}
    </DocsTheme>
  );
};
