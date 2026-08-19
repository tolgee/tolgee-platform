import type { ReactNode } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import {
  Box,
  IconButton,
  InputAdornment,
  InputLabel,
  MenuItem,
  Select,
  TextField,
} from '@mui/material';
import { SearchSm, XClose } from '../../icons';

const meta = {
  title: 'Components/Inputs/Input',
  component: TextField,
  parameters: {
    layout: 'centered',
  },
} satisfies Meta<typeof TextField>;

export default meta;

type Story = StoryObj<typeof meta>;

/**
 * The product's own shape: the label is an element above the field, and the box below it
 * reserves room so a message can appear without moving the form.
 */
const Field = ({
  label,
  children,
}: {
  label?: ReactNode;
  children: ReactNode;
}) => (
  <Box sx={{ display: 'grid', minHeight: 64 }}>
    {label && (
      <InputLabel sx={{ fontSize: 14, fontWeight: 500, mb: 0.5 }}>
        {label}
      </InputLabel>
    )}
    {children}
  </Box>
);

export const TheStandard = {
  args: {},
  render: () => (
    <Box sx={{ display: 'grid', gap: 1, width: 300 }}>
      <Field label="Task name (optional)">
        <TextField size="small" placeholder="Task" fullWidth />
      </Field>
      <Field label="Type">
        <Select size="small" value="translate" fullWidth>
          <MenuItem value="translate">Translate</MenuItem>
          <MenuItem value="review">Review</MenuItem>
        </Select>
      </Field>
    </Box>
  ),
} satisfies Story;

export const WithAMessage = {
  args: {},
  render: () => (
    <Box sx={{ display: 'grid', gap: 1, width: 300 }}>
      <Field label="Project name">
        <TextField
          size="small"
          fullWidth
          defaultValue="Website copy"
          helperText="Shown to everyone in the project"
        />
      </Field>
      <Field label="Project name">
        <TextField
          size="small"
          fullWidth
          error
          defaultValue=" "
          helperText="Name cannot be blank"
        />
      </Field>
    </Box>
  ),
} satisfies Story;

export const Multiline = {
  args: {},
  render: () => (
    <Field label="Description">
      <TextField size="small" multiline minRows={3} sx={{ width: 300 }} />
    </Field>
  ),
} satisfies Story;

export const Search = {
  args: {},
  render: () => (
    <TextField
      size="small"
      placeholder="Search"
      defaultValue="homepage"
      sx={{ width: 260 }}
      InputProps={{
        startAdornment: (
          <InputAdornment position="start">
            <SearchSm width={20} height={20} />
          </InputAdornment>
        ),
        endAdornment: (
          <InputAdornment position="end">
            <IconButton size="small" aria-label="Clear search">
              <XClose width={20} height={20} />
            </IconButton>
          </InputAdornment>
        ),
      }}
    />
  ),
} satisfies Story;

const Deviation = ({
  caption,
  count,
  children,
}: {
  caption: string;
  count: string;
  children: ReactNode;
}) => (
  <Box sx={{ width: 240 }}>
    {children}
    <Box sx={{ typography: 'caption', color: 'error.main', mt: 0.5 }}>
      {caption}
    </Box>
    <Box sx={{ typography: 'caption', color: 'text.secondary' }}>{count}</Box>
  </Box>
);

/** Three shapes for one field, all of them in the product today. */
export const FindingThreeShapes = {
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 3, alignItems: 'flex-start' }}>
      <Box sx={{ width: 240 }}>
        <Field label="Task name">
          <TextField size="small" fullWidth placeholder="Task" />
        </Field>
        <Box sx={{ typography: 'caption', color: 'success.main', mt: 0.5 }}>
          label above the field
        </Box>
        <Box sx={{ typography: 'caption', color: 'text.secondary' }}>
          74 of 100 · the standard
        </Box>
      </Box>
      <Deviation caption="label floating in the border" count="6 of 100">
        <TextField size="small" fullWidth label="Task name" />
      </Deviation>
      <Deviation caption="underline, no box" count="21 of 100">
        <TextField
          size="small"
          fullWidth
          variant="standard"
          label="Task name"
        />
      </Deviation>
    </Box>
  ),
} satisfies Story;

/** The wrapper states the standard but lets any caller walk over it. */
export const FindingOverridable = {
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 3, alignItems: 'flex-start' }}>
      <Box sx={{ width: 240 }}>
        <Field label="Through the wrapper">
          <TextField size="small" fullWidth />
        </Field>
        <Box sx={{ typography: 'caption', color: 'text.secondary' }}>
          variant=&quot;outlined&quot; set by the wrapper
        </Box>
      </Box>
      <Box sx={{ width: 240 }}>
        <Field label="Same wrapper, one prop">
          <TextField size="small" fullWidth variant="standard" />
        </Field>
        <Box sx={{ typography: 'caption', color: 'error.main' }}>
          caller passed variant=&quot;standard&quot;
        </Box>
        <Box sx={{ typography: 'caption', color: 'text.secondary' }}>
          11 formik fields do this
        </Box>
      </Box>
    </Box>
  ),
} satisfies Story;
