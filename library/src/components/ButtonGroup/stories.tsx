import type { Meta, StoryObj } from '@storybook/react-vite';
import { Box, Button, ButtonGroup } from '@mui/material';
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

/** What nine places in the product build out of this component. It is not what it looks like. */
export const NotAToggle = {
  args: {},
  render: () => (
    <Box>
      <ButtonGroup>
        <Button color="primary" aria-label="List view">
          <LayoutLeft width={20} height={20} />
        </Button>
        <Button color="default" aria-label="Table view">
          <LayoutGrid02 width={20} height={20} />
        </Button>
      </ButtonGroup>
      <Box sx={{ typography: 'caption', color: 'error.main', mt: 0.75 }}>
        the left one is on — color says so, the markup does not
      </Box>
    </Box>
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

/** A child that sets its own size breaks the shared block. */
export const FindingChildOverride = {
  tags: ['!dev'],
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 4, alignItems: 'flex-start' }}>
      <Box>
        <ButtonGroup>
          <Button size="small">Invite</Button>
          <Button>Copy link</Button>
        </ButtonGroup>
        <Box sx={{ typography: 'caption', color: 'error.main', mt: 0.5 }}>
          size on the child
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
