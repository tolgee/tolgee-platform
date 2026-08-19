import type { Meta, StoryObj } from '@storybook/react-vite';
import {
  Box,
  Button,
  ButtonGroup,
  ToggleButton,
  ToggleButtonGroup,
} from '@mui/material';
import { ArrowDown, LayoutGrid02, LayoutLeft } from '../../icons';

const meta = {
  title: 'Components/Buttons/ButtonGroup',
  component: ButtonGroup,
  parameters: {
    layout: 'centered',
  },
} satisfies Meta<typeof ButtonGroup>;

export default meta;

type Story = StoryObj<typeof meta>;

export const RelatedActions = {
  args: {},
  render: () => (
    <ButtonGroup size="small">
      <Button>Members</Button>
      <Button>Invitations</Button>
    </ButtonGroup>
  ),
} satisfies Story;

export const SplitButton = {
  args: {},
  render: () => (
    <ButtonGroup variant="contained" color="primary" size="small">
      <Button>Save</Button>
      <Button
        sx={{ px: 0.75, minWidth: 'unset' }}
        aria-label="More save options"
      >
        <ArrowDown width={20} height={20} />
      </Button>
    </ButtonGroup>
  ),
} satisfies Story;

export const ViewSwitch = {
  args: {},
  render: () => (
    <ButtonGroup>
      <Button color="primary" aria-label="List view">
        <LayoutLeft width={20} height={20} />
      </Button>
      <Button aria-label="Table view">
        <LayoutGrid02 width={20} height={20} />
      </Button>
    </ButtonGroup>
  ),
} satisfies Story;

export const Sizes = {
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <ButtonGroup size="small">
        <Button>Small</Button>
        <Button>Small</Button>
      </ButtonGroup>
      <ButtonGroup>
        <Button>Default</Button>
        <Button>Default</Button>
      </ButtonGroup>
    </Box>
  ),
} satisfies Story;

/** Left: the state carried by hand. Right: the component that carries it. */
export const FindingToggleByHand = {
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 4, alignItems: 'flex-start' }}>
      <Box>
        <ButtonGroup>
          <Button color="primary" sx={{ px: 1 }}>
            <LayoutLeft width={20} height={20} />
          </Button>
          <Button sx={{ px: 1 }}>
            <LayoutGrid02 width={20} height={20} />
          </Button>
        </ButtonGroup>
        <Box sx={{ typography: 'caption', color: 'error.main', mt: 0.5 }}>
          ButtonGroup + color · no aria-pressed · 4 places
        </Box>
      </Box>
      <Box>
        <ToggleButtonGroup value="list" exclusive size="small">
          <ToggleButton value="list" aria-label="List view">
            <LayoutLeft width={20} height={20} />
          </ToggleButton>
          <ToggleButton value="table" aria-label="Table view">
            <LayoutGrid02 width={20} height={20} />
          </ToggleButton>
        </ToggleButtonGroup>
        <Box sx={{ typography: 'caption', color: 'text.secondary', mt: 0.5 }}>
          ToggleButtonGroup · aria-pressed included · 1 place
        </Box>
      </Box>
    </Box>
  ),
} satisfies Story;

/** A child that sets its own size breaks the shared block. */
export const FindingChildOverride = {
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 4, alignItems: 'flex-start' }}>
      <Box>
        <ButtonGroup>
          <Button size="small">Invite</Button>
          <Button>Copy link</Button>
        </ButtonGroup>
        <Box sx={{ typography: 'caption', color: 'error.main', mt: 0.5 }}>
          size on the child · 5 places
        </Box>
      </Box>
      <Box>
        <ButtonGroup size="small">
          <Button>Invite</Button>
          <Button>Copy link</Button>
        </ButtonGroup>
        <Box sx={{ typography: 'caption', color: 'text.secondary', mt: 0.5 }}>
          size on the group
        </Box>
      </Box>
    </Box>
  ),
} satisfies Story;
