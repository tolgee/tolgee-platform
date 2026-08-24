import type { ReactNode } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { Box, Button, InputLabel, TextField } from '@mui/material';

const meta = {
  title: 'Components/Forms/Form',
  component: Box,
  parameters: {
    layout: 'centered',
  },
} satisfies Meta<typeof Box>;

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

const Field = ({ label, children }: { label: string; children: ReactNode }) => (
  <Box sx={{ display: 'grid', minHeight: 64 }}>
    <InputLabel sx={{ fontSize: 14, fontWeight: 400, mb: 0.5 }}>
      {label}
    </InputLabel>
    {children}
  </Box>
);

/** What StandardForm draws around whatever you put in it. */
export const TheStandard = {
  args: {},
  render: () => (
    <Box sx={{ width: 360 }}>
      <Field label="Name">
        <TextField size="small" fullWidth defaultValue="Website copy" />
      </Field>
      <Field label="Description">
        <TextField size="small" fullWidth multiline minRows={2} />
      </Field>
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1, mt: 1 }}>
        <Button>Cancel</Button>
        <Button variant="contained" color="primary">
          Save
        </Button>
      </Box>
    </Box>
  ),
} satisfies Story;

/** The submit row is the part every form gets for free. */
export const SubmitRow = {
  args: {},
  render: () => (
    <Box sx={{ width: 360 }}>
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
        <Button>Cancel</Button>
        <Button variant="contained" color="primary">
          Save
        </Button>
      </Box>
      <Note>Cancel as text, Save as contained primary, right-aligned</Note>
    </Box>
  ),
} satisfies Story;

/** A rule with an extra action on the left. */
export const WithACustomAction = {
  args: {},
  render: () => (
    <Box sx={{ width: 360, display: 'flex', justifyContent: 'space-between' }}>
      <Button color="error">Delete</Button>
      <Box sx={{ display: 'flex', gap: 1 }}>
        <Button>Cancel</Button>
        <Button variant="contained" color="primary">
          Save
        </Button>
      </Box>
    </Box>
  ),
} satisfies Story;
