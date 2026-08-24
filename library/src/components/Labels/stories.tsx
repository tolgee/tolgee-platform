import type { ReactNode } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import {
  Box,
  Checkbox,
  FormControlLabel,
  InputAdornment,
  InputLabel,
  TextField,
  Typography,
} from '@mui/material';
import { SearchSm } from '../../icons';

const meta = {
  title: 'Components/Forms/Labels',
  component: InputLabel,
  parameters: {
    layout: 'centered',
  },
} satisfies Meta<typeof InputLabel>;

export default meta;

type Story = StoryObj<typeof meta>;

const Note = ({
  tone = 'muted',
  children,
}: {
  tone?: 'muted' | 'good' | 'bad';
  children: ReactNode;
}) => (
  <Box
    sx={{
      typography: 'caption',
      mt: 0.75,
      color:
        tone === 'good'
          ? 'success.main'
          : tone === 'bad'
            ? 'error.main'
            : 'text.secondary',
    }}
  >
    {children}
  </Box>
);

export const AboveTheField = {
  args: {},
  render: () => (
    <Box sx={{ display: 'grid', width: 300, minHeight: 64 }}>
      <InputLabel htmlFor="labels-above" sx={{ fontSize: 14, mb: 0.5 }}>
        Task name
      </InputLabel>
      <TextField id="labels-above" size="small" placeholder="Task" fullWidth />
    </Box>
  ),
} satisfies Story;

export const BesideTheControl = {
  args: {},
  render: () => (
    <Box sx={{ display: 'grid' }}>
      <FormControlLabel control={<Checkbox />} label="Include screenshots" />
      <FormControlLabel control={<Checkbox />} label="Include tags" />
    </Box>
  ),
} satisfies Story;

export const NoRoomForALabel = {
  args: {},
  render: () => (
    <Box sx={{ width: 260 }}>
      <TextField
        size="small"
        placeholder="Search"
        fullWidth
        inputProps={{ 'aria-label': 'Search keys' }}
        InputProps={{
          startAdornment: (
            <InputAdornment position="start">
              <SearchSm width={20} height={20} />
            </InputAdornment>
          ),
        }}
      />
      <Note>named by aria-label — the placeholder is only a hint</Note>
    </Box>
  ),
} satisfies Story;

/** The same picture twice. One of them has a name. */
export const FindingLooksTheSame = {
  tags: ['!dev'],
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 5, alignItems: 'flex-start' }}>
      <Box sx={{ width: 250 }}>
        <Box sx={{ display: 'grid' }}>
          <InputLabel sx={{ fontSize: 14, mb: 0.5 }}>Task name</InputLabel>
          <TextField size="small" placeholder="Task" fullWidth />
        </Box>
        <Note tone="bad">
          no htmlFor — announced with no name, not clickable
        </Note>
      </Box>
      <Box sx={{ width: 250 }}>
        <Box sx={{ display: 'grid' }}>
          <InputLabel htmlFor="labels-ok" sx={{ fontSize: 14, mb: 0.5 }}>
            Task name
          </InputLabel>
          <TextField id="labels-ok" size="small" placeholder="Task" fullWidth />
        </Box>
        <Note tone="good">
          htmlFor + id — announced, and the label focuses it
        </Note>
      </Box>
    </Box>
  ),
} satisfies Story;

/** A label in the next grid cell is a label to the eye only. */
export const FindingLabelInAnotherCell = {
  tags: ['!dev'],
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 5, alignItems: 'flex-start' }}>
      <Box sx={{ width: 250 }}>
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: '1fr auto',
            alignItems: 'center',
          }}
        >
          <Typography>Quality checks</Typography>
          <Checkbox defaultChecked />
        </Box>
        <Note tone="bad">two siblings — a screen reader reads “checkbox”</Note>
      </Box>
      <Box sx={{ width: 250 }}>
        <FormControlLabel
          control={<Checkbox defaultChecked />}
          label="Quality checks"
          labelPlacement="start"
          sx={{ ml: 0, width: '100%', justifyContent: 'space-between' }}
        />
        <Note tone="good">
          FormControlLabel — same layout, and the words belong to the box
        </Note>
      </Box>
    </Box>
  ),
} satisfies Story;
