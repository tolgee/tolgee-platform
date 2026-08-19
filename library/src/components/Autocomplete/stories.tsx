import type { ReactNode } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import {
  Autocomplete,
  Box,
  Chip,
  InputBase,
  InputLabel,
  MenuItem,
  Paper,
  TextField,
} from '@mui/material';
import { SearchSm } from '../../icons';

const meta = {
  title: 'Components/Forms/Autocomplete',
  component: Autocomplete,
  parameters: {
    layout: 'centered',
  },
} satisfies Meta<typeof Autocomplete>;

export default meta;

type Story = StoryObj<typeof meta>;

const LANGUAGES = ['Czech', 'English', 'German', 'Slovak'];

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

/** The shape all seven search selects draw, rebuilt from their own CSS. */
const SearchPopover = ({ items = LANGUAGES }: { items?: string[] }) => (
  <Paper sx={{ width: 220, overflow: 'hidden' }} elevation={3}>
    <Box
      sx={{
        display: 'flex',
        alignItems: 'center',
        borderBottom: 1,
        borderColor: 'divider',
      }}
    >
      <Box sx={{ pl: 1.5, display: 'flex', color: 'text.secondary' }}>
        <SearchSm width={18} height={18} />
      </Box>
      <InputBase
        sx={{ padding: '5px 4px 3px 8px', flexGrow: 1, fontSize: 14 }}
        placeholder="Search"
      />
    </Box>
    <Box sx={{ py: 0.5 }}>
      {items.map((item) => (
        <MenuItem key={item} dense>
          {item}
        </MenuItem>
      ))}
    </Box>
  </Paper>
);

export const SearchSelect = {
  args: { options: [], renderInput: () => null },
  render: () => <SearchPopover />,
} satisfies Story;

export const AsAField = {
  args: { options: [], renderInput: () => null },
  render: () => (
    <Box sx={{ display: 'grid', width: 300, minHeight: 64 }}>
      <InputLabel sx={{ fontSize: 14, fontWeight: 400, mb: 0.5 }}>
        Transfer to
      </InputLabel>
      <Autocomplete
        size="small"
        options={LANGUAGES}
        renderInput={(params) => (
          <TextField {...params} placeholder="Search a project" />
        )}
      />
    </Box>
  ),
} satisfies Story;

export const Multiple = {
  args: { options: [], renderInput: () => null },
  render: () => (
    <Box sx={{ width: 320 }}>
      <Autocomplete
        multiple
        size="small"
        options={LANGUAGES}
        defaultValue={['Czech', 'English']}
        renderInput={(params) => <TextField {...params} />}
      />
    </Box>
  ),
} satisfies Story;

/** One picture. Seven implementations of it. */
export const FindingSevenCopies = {
  args: { options: [], renderInput: () => null },
  render: () => (
    <Box>
      <Box sx={{ display: 'flex', gap: 2 }}>
        <SearchPopover items={['Czech', 'English']} />
        <SearchPopover items={['Marketing', 'Docs']} />
        <SearchPopover items={['Anna', 'Ben']} />
      </Box>
      <Note tone="bad">
        languages, projects, assignees — and four more, each with its own copy
        of the same Popper, Paper and input
      </Note>
    </Box>
  ),
} satisfies Story;

/** The tag input builds its own field out of a bare input element. */
export const FindingTagInput = {
  args: { options: [], renderInput: () => null },
  render: () => (
    <Box sx={{ display: 'flex', gap: 4, alignItems: 'flex-start' }}>
      <Box sx={{ width: 240 }}>
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            gap: 0.5,
            border: 1,
            borderColor: 'divider',
            borderRadius: 1,
            px: 1,
            py: 0.5,
            width: 150,
          }}
        >
          <Chip size="small" label="homepage" />
          <Box
            component="input"
            placeholder="Add tag"
            sx={{
              border: 0,
              background: 'transparent',
              padding: '0px 4px',
              outline: 0,
              minWidth: 0,
              width: '100%',
            }}
          />
        </Box>
        <Note tone="bad">
          a bare &lt;input&gt; in a div, with role=&quot;input&quot; on the
          wrapper
        </Note>
      </Box>
      <Box sx={{ width: 260 }}>
        <Autocomplete
          size="small"
          multiple
          options={LANGUAGES}
          defaultValue={['homepage']}
          freeSolo
          renderInput={(params) => (
            <TextField {...params} placeholder="Add tag" />
          )}
        />
        <Note tone="good">the same thing through the Material UI shell</Note>
      </Box>
    </Box>
  ),
} satisfies Story;

/** The one Autocomplete that draws a label draws the wrong one. */
export const FindingUnderlined = {
  args: { options: [], renderInput: () => null },
  render: () => (
    <Box sx={{ display: 'flex', gap: 4, alignItems: 'flex-start' }}>
      <Box sx={{ width: 240 }}>
        <Autocomplete
          size="small"
          options={LANGUAGES}
          renderInput={(params) => (
            <TextField {...params} variant="standard" label="Transfer to" />
          )}
        />
        <Note tone="bad">variant=&quot;standard&quot; — an underline</Note>
      </Box>
      <Box sx={{ width: 240 }}>
        <Box sx={{ display: 'grid' }}>
          <InputLabel sx={{ fontSize: 14, fontWeight: 400, mb: 0.5 }}>
            Transfer to
          </InputLabel>
          <Autocomplete
            size="small"
            options={LANGUAGES}
            renderInput={(params) => <TextField {...params} />}
          />
        </Box>
        <Note tone="good">label above an outlined box</Note>
      </Box>
    </Box>
  ),
} satisfies Story;
