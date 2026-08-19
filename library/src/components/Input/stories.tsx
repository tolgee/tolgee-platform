import type { Meta, StoryObj } from '@storybook/react-vite';
import { Box, IconButton, InputAdornment, TextField } from '@mui/material';
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

export const Labelled = {
  args: {},
  render: () => (
    <TextField label="Project name" size="small" defaultValue="Website copy" />
  ),
} satisfies Story;

export const States = {
  args: {},
  render: () => (
    <Box sx={{ display: 'grid', gap: 2, width: 260 }}>
      <TextField label="Empty" size="small" placeholder="Type a name" />
      <TextField label="Filled" size="small" defaultValue="Website copy" />
      <TextField
        label="With a hint"
        size="small"
        helperText="Shown to everyone in the project"
      />
      <TextField
        label="Rejected"
        size="small"
        error
        defaultValue="  "
        helperText="Name cannot be blank"
      />
      <TextField
        label="Unavailable"
        size="small"
        disabled
        defaultValue="Locked"
      />
    </Box>
  ),
} satisfies Story;

export const Variants = {
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 3, alignItems: 'flex-start' }}>
      <Box sx={{ textAlign: 'center' }}>
        <TextField label="Outlined" size="small" defaultValue="79 uses" />
        <Box sx={{ typography: 'caption', color: 'text.secondary', mt: 0.5 }}>
          the default
        </Box>
      </Box>
      <Box sx={{ textAlign: 'center' }}>
        <TextField
          label="Standard"
          size="small"
          variant="standard"
          defaultValue="21 uses"
        />
        <Box sx={{ typography: 'caption', color: 'text.secondary', mt: 0.5 }}>
          no border
        </Box>
      </Box>
    </Box>
  ),
} satisfies Story;

export const Multiline = {
  args: {},
  render: () => (
    <TextField
      label="Description"
      size="small"
      multiline
      minRows={3}
      sx={{ width: 300 }}
      defaultValue={
        'Shown on the project list.\nTwo lines fit before it grows.'
      }
    />
  ),
} satisfies Story;

/** Search is an Input with two adornments and a debounce — not a component of its own. */
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

/** Same field, three ways in — and the wrapper forces its own variant and height. */
export const FindingThreeDoors = {
  args: {},
  render: () => (
    <Box sx={{ display: 'grid', gap: 2, width: 300 }}>
      <Box>
        <TextField label="From @mui/material" size="small" fullWidth />
        <Box sx={{ typography: 'caption', color: 'text.secondary' }}>
          64 files · nothing added
        </Box>
      </Box>
      <Box>
        <Box sx={{ display: 'grid' }}>
          <Box
            component="label"
            sx={{ typography: 'caption', fontWeight: 500, mb: 0.5 }}
          >
            From tg.component/common/TextField
          </Box>
          <TextField size="small" fullWidth sx={{ minHeight: '64px' }} />
        </Box>
        <Box sx={{ typography: 'caption', color: 'error.main' }}>
          15 files · label above, outlined and small forced, 64px reserved
        </Box>
      </Box>
    </Box>
  ),
} satisfies Story;
