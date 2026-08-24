import type { ReactNode } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import {
  Box,
  FormControlLabel,
  Radio,
  RadioGroup,
  Typography,
} from '@mui/material';

const meta = {
  title: 'Components/Forms/Radio',
  component: Radio,
  parameters: {
    layout: 'centered',
  },
} satisfies Meta<typeof Radio>;

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
    <RadioGroup defaultValue="all" name="scope">
      <FormControlLabel value="all" control={<Radio />} label="All languages" />
      <FormControlLabel
        value="selected"
        control={<Radio />}
        label="Selected languages"
      />
    </RadioGroup>
  ),
} satisfies Story;

export const AsCards = {
  args: {},
  render: () => (
    <RadioGroup defaultValue="fast" name="mode" sx={{ gap: 1, width: 320 }}>
      {[
        { value: 'fast', title: 'Fast', text: 'Machine translation only' },
        {
          value: 'full',
          title: 'Full',
          text: 'Machine translation plus review',
        },
      ].map((o) => (
        <Box
          key={o.value}
          sx={{ border: 1, borderColor: 'divider', borderRadius: 1, p: 1.5 }}
        >
          <FormControlLabel
            value={o.value}
            control={<Radio size="small" />}
            labelPlacement="start"
            sx={{ ml: 0, width: '100%', justifyContent: 'space-between' }}
            label={<Typography fontWeight={500}>{o.title}</Typography>}
          />
          <Typography variant="body2" color="text.secondary">
            {o.text}
          </Typography>
        </Box>
      ))}
    </RadioGroup>
  ),
} satisfies Story;

/** Loose radios versus a group. The picture is the same; the keyboard is not. */
export const FindingNoRadioGroup = {
  tags: ['!dev'],
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 6, alignItems: 'flex-start' }}>
      <Box>
        <Box sx={{ display: 'grid' }}>
          <FormControlLabel control={<Radio checked />} label="Google" />
          <FormControlLabel control={<Radio />} label="Amazon" />
        </Box>
        <Note tone="bad">
          loose radios — no group, no shared name, Tab through each
        </Note>
      </Box>
      <Box>
        <RadioGroup defaultValue="first" name="service">
          <FormControlLabel value="first" control={<Radio />} label="Google" />
          <FormControlLabel value="second" control={<Radio />} label="Amazon" />
        </RadioGroup>
        <Note tone="good">
          RadioGroup — one stop, arrow keys move between options
        </Note>
      </Box>
    </Box>
  ),
} satisfies Story;
