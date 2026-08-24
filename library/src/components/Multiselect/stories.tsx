import type { ReactNode } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import {
  Box,
  Checkbox,
  Chip,
  InputBase,
  ListItemText,
  MenuItem,
  MenuList,
  Paper,
} from '@mui/material';
import { SearchSm } from '../../icons';

const meta = {
  title: 'Components/Forms/Multiselect',
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

const Rows = ({ items }: { items: [string, boolean][] }) => (
  <MenuList sx={{ py: 0.5 }}>
    {items.map(([label, checked]) => (
      <MenuItem key={label} sx={{ height: 40 }}>
        <Checkbox size="small" checked={checked} sx={{ p: 0.5, mr: 1 }} />
        <ListItemText primary={label} />
      </MenuItem>
    ))}
  </MenuList>
);

/** The search select with checkboxes — the shape the product uses most. */
export const WithSearch = {
  args: {},
  render: () => (
    <Paper elevation={3} sx={{ width: 230, overflow: 'hidden' }}>
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
      <Rows
        items={[
          ['English', true],
          ['Czech', true],
          ['German', false],
        ]}
      />
    </Paper>
  ),
} satisfies Story;

/** Without a search box, when the list is short. */
export const WithoutSearch = {
  args: {},
  render: () => (
    <Paper elevation={3} sx={{ width: 230 }}>
      <Rows
        items={[
          ['Untranslated', true],
          ['Translated', false],
          ['Reviewed', true],
        ]}
      />
    </Paper>
  ),
} satisfies Story;

/** What the trigger shows once something is picked. */
export const TheTrigger = {
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 4, alignItems: 'flex-start' }}>
      <Box>
        <Box
          sx={{
            border: 1,
            borderColor: 'divider',
            borderRadius: 1,
            px: 1,
            py: 0.5,
            display: 'flex',
            gap: 0.5,
            width: 200,
            height: 40,
            alignItems: 'center',
          }}
        >
          <Chip size="small" label="English" />
          <Chip size="small" label="Czech" />
        </Box>
        <Note>chips — languages, assignees</Note>
      </Box>
      <Box>
        <Box
          sx={{
            border: 1,
            borderColor: 'divider',
            borderRadius: 1,
            px: 1.5,
            display: 'flex',
            width: 200,
            height: 40,
            alignItems: 'center',
          }}
        >
          2 states selected
        </Box>
        <Note>a summary — filters</Note>
      </Box>
    </Box>
  ),
} satisfies Story;

/** Four components draw this, and they do not agree. */
export const FindingFourImplementations = {
  tags: ['!dev'],
  args: {},
  render: () => (
    <Box>
      <Box sx={{ display: 'flex', gap: 2, alignItems: 'flex-start' }}>
        <Box>
          <Paper elevation={3} sx={{ width: 180 }}>
            <Rows
              items={[
                ['English', true],
                ['Czech', false],
              ]}
            />
          </Paper>
          <Note tone="bad">SearchSelectMulti</Note>
        </Box>
        <Box>
          <Paper elevation={3} sx={{ width: 180 }}>
            <Rows
              items={[
                ['Docs', true],
                ['Web', false],
              ]}
            />
          </Paper>
          <Note tone="bad">InfiniteMultiSearchSelect</Note>
        </Box>
        <Box>
          <Paper elevation={3} sx={{ width: 180 }}>
            <Rows
              items={[
                ['English', true],
                ['German', false],
              ]}
            />
          </Paper>
          <Note tone="bad">LanguagesSelect</Note>
        </Box>
      </Box>
      <Note tone="bad">
        plus `Select multiple` in nine more places — one picture, four
        implementations
      </Note>
    </Box>
  ),
} satisfies Story;
