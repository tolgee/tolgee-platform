import type { ReactNode } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import {
  Box,
  Button,
  ButtonGroup,
  IconButton,
  ToggleButton,
  ToggleButtonGroup,
} from '@mui/material';
import {
  ChevronDown,
  ClipboardCheck,
  Code01,
  LayoutGrid02,
  LayoutLeft,
  Moon01,
  Sun,
} from '../../icons';

const meta = {
  title: 'Components/Buttons/ToggleButton',
  component: ToggleButton,
  parameters: {
    layout: 'centered',
  },
} satisfies Meta<typeof ToggleButton>;

export default meta;

type Story = StoryObj<typeof meta>;

const Sample = ({
  caption,
  tone = 'muted',
  children,
}: {
  caption: string;
  tone?: 'muted' | 'good' | 'bad';
  children: ReactNode;
}) => (
  <Box>
    {children}
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
      {caption}
    </Box>
  </Box>
);

export const Segmented = {
  args: { value: 'list' },
  render: () => (
    <ToggleButtonGroup value="list" exclusive size="small">
      <ToggleButton value="list" aria-label="List view">
        <LayoutLeft width={20} height={20} />
      </ToggleButton>
      <ToggleButton value="table" aria-label="Table view">
        <LayoutGrid02 width={20} height={20} />
      </ToggleButton>
    </ToggleButtonGroup>
  ),
} satisfies Story;

export const IconToggle = {
  args: { value: 'code' },
  render: () => (
    <Box sx={{ display: 'flex', gap: 4, alignItems: 'flex-start' }}>
      <Sample caption="on — aria-pressed=true">
        <ToggleButton value="code" selected size="small" aria-label="Show code">
          <Code01 width={20} height={20} />
        </ToggleButton>
      </Sample>
      <Sample caption="off — aria-pressed=false">
        <ToggleButton value="code" size="small" aria-label="Show code">
          <Code01 width={20} height={20} />
        </ToggleButton>
      </Sample>
    </Box>
  ),
} satisfies Story;

export const ThreeOptions = {
  args: { value: 'light' },
  render: () => (
    <ToggleButtonGroup value="light" exclusive size="small">
      <ToggleButton value="light" aria-label="Light theme">
        <Sun width={18} height={18} />
      </ToggleButton>
      <ToggleButton value="dark" aria-label="Dark theme">
        <Moon01 width={18} height={18} />
      </ToggleButton>
      <ToggleButton value="system">System</ToggleButton>
    </ToggleButtonGroup>
  ),
} satisfies Story;

/** Three controls that look alike and mean three different things. */
export const NotEveryPairIsAToggle = {
  args: { value: 'x' },
  render: () => (
    <Box sx={{ display: 'flex', gap: 4, alignItems: 'flex-start' }}>
      <Sample caption="toggle — aria-pressed">
        <ToggleButton value="code" selected size="small" aria-label="Show code">
          <Code01 width={20} height={20} />
        </ToggleButton>
      </Sample>
      <Sample caption="one-way action — plain button">
        <IconButton size="small" aria-label="Mark as reviewed">
          <ClipboardCheck width={20} height={20} />
        </IconButton>
      </Sample>
      <Sample caption="disclosure — aria-expanded">
        <IconButton size="small" aria-label="Expand" aria-expanded={false}>
          <ChevronDown width={20} height={20} />
        </IconButton>
      </Sample>
    </Box>
  ),
} satisfies Story;

/** Left: the state carried by color. Right: the component that carries it. */
export const FindingSegmentedByHand = {
  args: { value: 'list' },
  render: () => (
    <Box sx={{ display: 'flex', gap: 4, alignItems: 'flex-start' }}>
      <Sample caption="ButtonGroup + color — no pressed state" tone="bad">
        <ButtonGroup>
          <Button color="primary" sx={{ px: 1 }}>
            <LayoutLeft width={20} height={20} />
          </Button>
          <Button color="default" sx={{ px: 1 }}>
            <LayoutGrid02 width={20} height={20} />
          </Button>
        </ButtonGroup>
      </Sample>
      <Sample caption="ToggleButtonGroup — pressed state included" tone="good">
        <ToggleButtonGroup value="list" exclusive size="small">
          <ToggleButton value="list" aria-label="List view">
            <LayoutLeft width={20} height={20} />
          </ToggleButton>
          <ToggleButton value="table" aria-label="Table view">
            <LayoutGrid02 width={20} height={20} />
          </ToggleButton>
        </ToggleButtonGroup>
      </Sample>
    </Box>
  ),
} satisfies Story;

/** The editor's code switch, as built and as it would be. */
export const FindingIconByColor = {
  args: { value: 'code' },
  render: () => (
    <Box sx={{ display: 'flex', gap: 4, alignItems: 'flex-start' }}>
      <Sample caption="IconButton + color — on" tone="bad">
        <IconButton size="small" color="primary">
          <Code01 width={20} height={20} />
        </IconButton>
      </Sample>
      <Sample caption="IconButton + color — off" tone="bad">
        <IconButton size="small">
          <Code01 width={20} height={20} />
        </IconButton>
      </Sample>
      <Sample caption="ToggleButton — on" tone="good">
        <ToggleButton value="code" selected size="small" aria-label="Show code">
          <Code01 width={20} height={20} />
        </ToggleButton>
      </Sample>
    </Box>
  ),
} satisfies Story;

/** The real component next to ours: nothing in the theme speaks for it. */
export const FindingNoThemeEntry = {
  args: { value: 'list' },
  render: () => (
    <Box sx={{ display: 'flex', gap: 4, alignItems: 'flex-start' }}>
      <Sample caption="our Button — 4px corner, our grey">
        <Button>Table</Button>
      </Sample>
      <Sample caption="ToggleButton — Material UI's defaults" tone="bad">
        <ToggleButton value="table">Table</ToggleButton>
      </Sample>
    </Box>
  ),
} satisfies Story;
