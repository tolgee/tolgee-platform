import type { ReactNode } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import {
  Box,
  FormControl,
  FormHelperText,
  InputLabel,
  MenuItem,
  Select,
} from '@mui/material';

const meta = {
  title: 'Components/Forms/Select',
  component: Select,
  parameters: {
    layout: 'centered',
  },
} satisfies Meta<typeof Select>;

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

/** Rebuilt from common/Select: the label, then a grid that reserves 64px for the
 *  control and whatever message appears under it. */
const Field = ({
  label,
  error,
  children,
}: {
  label: ReactNode;
  error?: string;
  children: ReactNode;
}) => (
  <Box sx={{ display: 'grid' }}>
    <InputLabel sx={{ fontSize: 14, fontWeight: 400, mb: 0.5 }}>
      {label}
    </InputLabel>
    <Box sx={{ display: 'grid', alignItems: 'start', minHeight: 64 }}>
      {children}
      {error && (
        <FormHelperText sx={{ color: 'error.main' }}>{error}</FormHelperText>
      )}
    </Box>
  </Box>
);

const Options = [
  <MenuItem key="translate" value="translate">
    Translate
  </MenuItem>,
  <MenuItem key="review" value="review">
    Review
  </MenuItem>,
];

export const TheStandard = {
  args: {},
  render: () => (
    <Box sx={{ width: 300 }}>
      <Field label="Type">
        <Select size="small" value="translate" fullWidth>
          {Options}
        </Select>
      </Field>
    </Box>
  ),
} satisfies Story;

export const WithAMessage = {
  args: {},
  render: () => (
    <Box sx={{ width: 300 }}>
      <Field label="Type" error="Pick a type">
        <Select size="small" value="translate" fullWidth error>
          {Options}
        </Select>
      </Field>
    </Box>
  ),
} satisfies Story;

export const InAToolbar = {
  args: {},
  render: () => (
    <Box sx={{ width: 220 }}>
      <Select size="small" value="translate" fullWidth aria-label="Task type">
        {Options}
      </Select>
      <Note>no label above — named with aria-label instead</Note>
    </Box>
  ),
} satisfies Story;

/** Three ways a select is named, and the one that names nothing. */
export const FindingHowItIsNamed = {
  tags: ['!dev'],
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 4, alignItems: 'flex-start' }}>
      <Box sx={{ width: 210 }}>
        <Field label="Type">
          <Select size="small" value="translate" fullWidth>
            {Options}
          </Select>
        </Field>
        <Note tone="good">label above, through the wrapper</Note>
      </Box>
      <Box sx={{ width: 210 }}>
        <FormControl fullWidth>
          <InputLabel>Type</InputLabel>
          <Select size="small" value="translate" data-cy="no-label-id">
            {Options}
          </Select>
        </FormControl>
        <Note tone="bad">
          looks labelled — no labelId, so nothing connects the two
        </Note>
      </Box>
      <Box sx={{ width: 210 }}>
        <Select size="small" value="translate" fullWidth>
          {Options}
        </Select>
        <Note tone="bad">no label, no aria-label — anonymous</Note>
      </Box>
    </Box>
  ),
} satisfies Story;

/** The export dialog, rebuilt from its own code. */
export const FindingUnderlinedSelects = {
  tags: ['!dev'],
  args: {},
  render: () => (
    <Box sx={{ display: 'grid', gap: 2, width: 260 }}>
      {['Format', 'Languages', 'Message format'].map((label) => (
        <FormControl key={label} variant="standard" fullWidth>
          <InputLabel id={`export-${label}`}>{label}</InputLabel>
          <Select labelId={`export-${label}`} value="translate">
            {Options}
          </Select>
        </FormControl>
      ))}
      <Note tone="bad">
        the whole export dialog — underline, where the rest of the product has a
        box
      </Note>
    </Box>
  ),
} satisfies Story;
