import type { Meta, StoryObj } from '@storybook/react-vite';
import { Box, IconButton, Tooltip } from '@mui/material';
import { DotsVertical, InfoCircle } from '../../icons';

const meta = {
  title: 'Components/Tooltip',
  component: Tooltip,
  parameters: {
    layout: 'centered',
  },
} satisfies Meta<typeof Tooltip>;

export default meta;

type Story = StoryObj<typeof meta>;

export const NamingAnIconButton = {
  args: { title: '', children: <span /> },
  render: () => (
    <Tooltip title="Delete key">
      <IconButton size="small">
        <DotsVertical width={20} height={20} />
      </IconButton>
    </Tooltip>
  ),
} satisfies Story;

export const Placement = {
  args: { title: '', children: <span /> },
  render: () => (
    <Box sx={{ display: 'flex', gap: 4 }}>
      {(['bottom', 'bottom-start', 'right'] as const).map((placement) => (
        <Tooltip key={placement} title={placement} placement={placement} open>
          <IconButton size="small" aria-label={placement}>
            <InfoCircle width={20} height={20} />
          </IconButton>
        </Tooltip>
      ))}
    </Box>
  ),
} satisfies Story;

export const Interactive = {
  args: { title: '', children: <span /> },
  render: () => (
    <Box sx={{ display: 'flex', gap: 4 }}>
      <Tooltip title="You can move onto me" open placement="bottom">
        <IconButton size="small" aria-label="Interactive">
          <InfoCircle width={20} height={20} />
        </IconButton>
      </Tooltip>
      <Tooltip
        title="I vanish on the way"
        disableInteractive
        open
        placement="bottom"
      >
        <IconButton size="small" aria-label="Not interactive">
          <InfoCircle width={20} height={20} />
        </IconButton>
      </Tooltip>
    </Box>
  ),
} satisfies Story;

/** A tooltip with an empty title renders nothing — and gives its child no name either. */
export const FindingNoTitle = {
  args: { title: '', children: <span /> },
  render: () => (
    <Box sx={{ display: 'flex', gap: 5, alignItems: 'flex-start' }}>
      <Box sx={{ textAlign: 'center' }}>
        <Tooltip title="">
          <IconButton size="small">
            <DotsVertical width={20} height={20} />
          </IconButton>
        </Tooltip>
        <Box sx={{ typography: 'caption', color: 'error.main', mt: 0.5 }}>
          empty title
        </Box>
      </Box>
      <Box sx={{ textAlign: 'center' }}>
        <Tooltip title="More actions">
          <IconButton size="small">
            <DotsVertical width={20} height={20} />
          </IconButton>
        </Tooltip>
        <Box sx={{ typography: 'caption', color: 'text.secondary', mt: 0.5 }}>
          named
        </Box>
      </Box>
    </Box>
  ),
} satisfies Story;
