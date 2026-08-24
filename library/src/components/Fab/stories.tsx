import type { Meta, StoryObj } from '@storybook/react-vite';
import { Box, Fab } from '@mui/material';
import { HelpCircle, Plus } from '../../icons';

const meta = {
  title: 'Components/Buttons/Fab',
  component: Fab,
  parameters: {
    layout: 'centered',
  },
} satisfies Meta<typeof Fab>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Help = {
  args: {},
  render: () => (
    <Fab color="primary" size="small" aria-label="Help">
      <HelpCircle width={20} height={20} />
    </Fab>
  ),
} satisfies Story;

export const Sizes = {
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
      <Fab color="primary" size="small" aria-label="Add, small">
        <Plus width={20} height={20} />
      </Fab>
      <Fab color="primary" size="medium" aria-label="Add, medium">
        <Plus width={24} height={24} />
      </Fab>
    </Box>
  ),
} satisfies Story;

/** The defect is invisible: both look and behave the same under a mouse. */
export const FindingNameOnWrapper = {
  tags: ['!dev'],
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 5, alignItems: 'flex-start' }}>
      <Box sx={{ textAlign: 'center' }}>
        <Box onClick={() => {}}>
          <Fab color="primary" size="small">
            <HelpCircle width={20} height={20} />
          </Fab>
        </Box>
        <Box sx={{ typography: 'caption', color: 'error.main', mt: 0.5 }}>
          handler and name on the div
        </Box>
      </Box>
      <Box sx={{ textAlign: 'center' }}>
        <Fab color="primary" size="small" aria-label="Help" onClick={() => {}}>
          <HelpCircle width={20} height={20} />
        </Fab>
        <Box sx={{ typography: 'caption', color: 'text.secondary', mt: 0.5 }}>
          both on the Fab
        </Box>
      </Box>
    </Box>
  ),
} satisfies Story;
