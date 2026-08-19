import type { Meta, StoryObj } from '@storybook/react-vite';
import { Box, Button } from '@mui/material';

const meta = {
  title: 'Components/Buttons/Button',
  component: Button,
  parameters: {
    layout: 'centered',
  },
} satisfies Meta<typeof Button>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Commit = {
  args: {
    variant: 'contained',
    color: 'primary',
    children: 'Save',
  },
} satisfies Story;

export const Secondary = {
  args: {
    variant: 'outlined',
    children: 'Delete',
  },
} satisfies Story;

export const Dismiss = {
  args: {
    children: 'Cancel',
  },
} satisfies Story;

export const AllThree = {
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
      <Button>Cancel</Button>
      <Button variant="outlined">Delete</Button>
      <Button variant="contained" color="primary">
        Save
      </Button>
    </Box>
  ),
} satisfies Story;

export const Sizes = {
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
      <Button variant="contained" color="primary" size="small">
        Small
      </Button>
      <Button variant="contained" color="primary">
        Default
      </Button>
    </Box>
  ),
} satisfies Story;

export const Disabled = {
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
      <Button disabled>Cancel</Button>
      <Button variant="outlined" disabled>
        Delete
      </Button>
      <Button variant="contained" color="primary" disabled>
        Save
      </Button>
    </Box>
  ),
} satisfies Story;

const COLORS = [
  'primary',
  'secondary',
  'error',
  'success',
  'info',
  'inherit',
] as const;

export const Colors = {
  args: {},
  render: () => (
    <Box sx={{ display: 'grid', gap: 1.5 }}>
      {COLORS.map((color) => (
        <Box key={color} sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Box
            sx={{
              width: 90,
              fontFamily: 'monospace',
              fontSize: 12,
              color: 'text.secondary',
            }}
          >
            {color}
          </Box>
          <Button variant="contained" color={color}>
            Contained
          </Button>
          <Button variant="outlined" color={color}>
            Outlined
          </Button>
          <Button color={color}>Text</Button>
        </Box>
      ))}
    </Box>
  ),
} satisfies Story;
