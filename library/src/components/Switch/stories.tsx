import type { ReactNode } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import {
  Box,
  FormControlLabel,
  InputLabel,
  Switch,
  Typography,
} from '@mui/material';

const meta = {
  title: 'Components/Forms/Switch',
  component: Switch,
  parameters: {
    layout: 'centered',
  },
} satisfies Meta<typeof Switch>;

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

export const TheStandard = {
  args: {},
  render: () => (
    <Box sx={{ display: 'grid', gap: 1 }}>
      <FormControlLabel control={<Switch defaultChecked />} label="Enabled" />
      <FormControlLabel control={<Switch />} label="Send me e-mail" />
    </Box>
  ),
} satisfies Story;

export const WithDescription = {
  args: {},
  render: () => (
    <Box sx={{ display: 'grid', width: 320 }}>
      <FormControlLabel
        control={<Switch defaultChecked />}
        label="Quality checks"
      />
      <InputLabel sx={{ fontSize: 12, fontWeight: 400 }}>
        Runs on every translation you save
      </InputLabel>
    </Box>
  ),
} satisfies Story;

export const Sizes = {
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 3, alignItems: 'center' }}>
      <FormControlLabel
        control={<Switch size="small" defaultChecked />}
        label="Small"
      />
      <FormControlLabel control={<Switch defaultChecked />} label="Default" />
    </Box>
  ),
} satisfies Story;

export const Disabled = {
  args: {},
  render: () => (
    <Box sx={{ display: 'grid', gap: 1 }}>
      <FormControlLabel
        control={<Switch disabled defaultChecked />}
        label="On, locked"
      />
      <FormControlLabel control={<Switch disabled />} label="Off, locked" />
    </Box>
  ),
} satisfies Story;

/** The title is on the screen. It is not attached to the switch. */
export const FindingLabelNotAttached = {
  tags: ['!dev'],
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 6, alignItems: 'flex-start' }}>
      <Box sx={{ width: 260 }}>
        <Box
          sx={{
            display: 'grid',
            gridTemplateAreas: '"title switch" "description switch"',
            gridTemplateColumns: '1fr auto',
            alignItems: 'center',
          }}
        >
          <Typography sx={{ gridArea: 'title' }}>Quality checks</Typography>
          <Typography
            variant="caption"
            color="text.secondary"
            sx={{ gridArea: 'description' }}
          >
            Runs on every translation you save
          </Typography>
          <Box sx={{ gridArea: 'switch' }}>
            <Switch defaultChecked />
          </Box>
        </Box>
        <Note tone="bad">
          reads as &ldquo;switch, on&rdquo; — the words are separate elements
        </Note>
      </Box>
      <Box sx={{ width: 260 }}>
        <FormControlLabel
          control={<Switch defaultChecked />}
          label="Quality checks"
          labelPlacement="start"
          sx={{ ml: 0, justifyContent: 'space-between', width: '100%' }}
        />
        <Note tone="good">
          reads as &ldquo;Quality checks, switch, on&rdquo;
        </Note>
      </Box>
    </Box>
  ),
} satisfies Story;

/** A tooltip that only exists while the switch is unusable names nothing. */
export const FindingTooltipOnlyWhenDisabled = {
  tags: ['!dev'],
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 6, alignItems: 'flex-start' }}>
      <Box sx={{ width: 220 }}>
        <Switch defaultChecked />
        <Note tone="bad">enabled — tooltip is false, so no name at all</Note>
      </Box>
      <Box sx={{ width: 220 }}>
        <Switch disabled defaultChecked />
        <Note>disabled — tooltip appears, and now it has a name</Note>
      </Box>
    </Box>
  ),
} satisfies Story;
