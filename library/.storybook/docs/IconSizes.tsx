import { Box } from '@mui/material';
import { Plus, SearchSm, Trash01 } from '../../src/icons';
import { DocsTheme } from './DocsTheme';

const SIZES = [
  {
    px: 16,
    name: 'Small',
    use: 'Inline in running text and inside dense chips, where 20 would push the line height.',
  },
  {
    px: 20,
    name: 'Compact',
    use: 'Buttons, list rows, toolbars — anywhere the icon sits next to a label.',
  },
  {
    px: 24,
    name: 'Default',
    use: 'The size the package renders at when nothing is passed. Standalone icons and empty states.',
  },
];

const SAMPLES = [Plus, SearchSm, Trash01];

export const IconSizes = () => (
  <DocsTheme>
    <Box sx={{ my: 2, color: 'text.primary' }}>
      {SIZES.map(({ px, name, use }) => (
        <Box
          key={px}
          sx={{
            display: 'grid',
            gridTemplateColumns: '110px 120px 1fr',
            alignItems: 'center',
            gap: 2,
            py: 1.5,
            borderBottom: (theme) => `1px solid ${theme.palette.divider}`,
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            {SAMPLES.map((Icon, i) => (
              <Icon key={i} width={px} height={px} />
            ))}
          </Box>
          <Box>
            <Box sx={{ typography: 'body2' }}>{name}</Box>
            <Box sx={{ fontFamily: 'monospace', typography: 'overline' }}>
              {px}px
            </Box>
          </Box>
          <Box sx={{ typography: 'body2', color: 'text.secondary' }}>{use}</Box>
        </Box>
      ))}
    </Box>
  </DocsTheme>
);
