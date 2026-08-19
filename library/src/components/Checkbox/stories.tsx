import type { Meta, StoryObj } from '@storybook/react-vite';
import { Box, Checkbox, FormControlLabel } from '@mui/material';

const meta = {
  title: 'Components/Forms/Checkbox',
  component: Checkbox,
  parameters: {
    layout: 'centered',
  },
} satisfies Meta<typeof Checkbox>;

export default meta;

type Story = StoryObj<typeof meta>;

export const WithLabel = {
  args: {},
  render: () => (
    <FormControlLabel control={<Checkbox size="small" />} label="Reviewed" />
  ),
} satisfies Story;

export const States = {
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
      <Checkbox size="small" aria-label="Unchecked" />
      <Checkbox size="small" defaultChecked aria-label="Checked" />
      <Checkbox size="small" indeterminate aria-label="Some selected" />
      <Checkbox size="small" disabled aria-label="Unavailable" />
      <Checkbox size="small" disabled defaultChecked aria-label="Locked on" />
    </Box>
  ),
} satisfies Story;

export const Sizes = {
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
      <Checkbox size="small" defaultChecked aria-label="Small" />
      <Checkbox defaultChecked aria-label="Default" />
    </Box>
  ),
} satisfies Story;

export const InARow = {
  args: {},
  render: () => (
    <Box sx={{ display: 'grid', gap: 0.5 }}>
      {['English', 'Czech', 'German'].map((language) => (
        <FormControlLabel
          key={language}
          control={
            <Checkbox size="small" defaultChecked={language !== 'German'} />
          }
          label={language}
        />
      ))}
    </Box>
  ),
} satisfies Story;

/** Both are the same control; only one of them can be announced. */
export const FindingNoName = {
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 5, alignItems: 'flex-start' }}>
      <Box>
        <Checkbox size="small" defaultChecked />
        <Box sx={{ typography: 'caption', color: 'error.main' }}>
          bare · 38 places
        </Box>
      </Box>
      <Box>
        <FormControlLabel
          control={<Checkbox size="small" defaultChecked />}
          label="Reviewed"
        />
        <Box sx={{ typography: 'caption', color: 'text.secondary' }}>
          labelled · 11 places
        </Box>
      </Box>
    </Box>
  ),
} satisfies Story;

/** The same override written twice, in two different cells. */
export const FindingHandSized = {
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 4, alignItems: 'flex-end' }}>
      <Box sx={{ textAlign: 'center' }}>
        <Checkbox size="small" defaultChecked sx={{ height: 20, width: 20 }} />
        <Box sx={{ typography: 'caption', color: 'error.main', mt: 0.5 }}>
          hand-sized · key cell, trash cell
        </Box>
      </Box>
      <Box sx={{ textAlign: 'center' }}>
        <Checkbox size="small" defaultChecked />
        <Box sx={{ typography: 'caption', color: 'text.secondary', mt: 0.5 }}>
          size=&quot;small&quot;
        </Box>
      </Box>
    </Box>
  ),
} satisfies Story;
