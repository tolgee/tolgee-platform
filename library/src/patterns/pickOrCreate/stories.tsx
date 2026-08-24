import type { ReactNode } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import {
  Box,
  Chip,
  IconButton,
  InputBase,
  ListItemText,
  MenuItem,
  MenuList,
  Paper,
  Typography,
} from '@mui/material';
import { Plus } from '../../icons';

const meta = {
  title: 'Patterns/Picking or creating',
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

const Panel = ({
  children,
  width = 260,
}: {
  children: ReactNode;
  width?: number;
}) => (
  <Paper elevation={3} sx={{ width, overflow: 'hidden' }}>
    {children}
  </Paper>
);

/** The search row, rebuilt from SearchSelectContent: input, then the + at the end. */
const SearchRow = ({
  placeholder = 'Search namespace...',
  addNew = true,
}: {
  placeholder?: string;
  addNew?: boolean;
}) => (
  <Box
    sx={{
      display: 'flex',
      alignItems: 'center',
      borderBottom: 1,
      borderColor: 'divider',
      pr: 0.5,
    }}
  >
    <InputBase
      sx={{ padding: '5px 4px 3px 16px', flexGrow: 1, fontSize: 14 }}
      placeholder={placeholder}
    />
    {addNew && (
      <IconButton size="small" sx={{ ml: 0.5 }} aria-label="New namespace">
        <Plus width={20} height={20} />
      </IconButton>
    )}
  </Box>
);

const Row = ({
  label,
  selected,
  muted,
}: {
  label: string;
  selected?: boolean;
  muted?: boolean;
}) => (
  <MenuItem selected={selected} sx={{ height: 40 }}>
    <ListItemText
      primary={label}
      sx={muted ? { color: 'text.secondary' } : undefined}
    />
  </MenuItem>
);

/** 1 — a + beside the search. NamespaceSelector and the organization switch. */
export const PlusBesideSearch = {
  args: {},
  render: () => (
    <Box>
      <Panel>
        <SearchRow />
        <MenuList sx={{ py: 0.5 }}>
          <Row label="<none>" selected muted />
          <Row label="demo" />
          <Row label="summer" />
        </MenuList>
      </Panel>
      <Note tone="good">
        the + carries what you typed into the create dialog — `onAddNew`
      </Note>
    </Box>
  ),
} satisfies Story;

/** 2 — a last row in the list. The import language selector. */
export const RowAtTheEnd = {
  args: {},
  render: () => (
    <Box>
      <Panel width={220}>
        <MenuList sx={{ py: 0.5 }}>
          <Row label="Czech" />
          <Row label="German" />
          <Row label="English" />
          <MenuItem sx={{ height: 40, color: 'primary.main', gap: 1 }}>
            <Plus width={18} height={18} />
            <ListItemText primary="Add new" />
          </MenuItem>
        </MenuList>
      </Panel>
      <Note tone="bad">no search, and the row scrolls away with the list</Note>
    </Box>
  ),
} satisfies Story;

/** 3 — a row that appears only when nothing matches. TagInput. */
export const AppearsWhenNothingMatches = {
  args: {},
  render: () => (
    <Box>
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
          width: 200,
          mb: 0.5,
        }}
      >
        <Chip size="small" label="homepage" />
        <Box sx={{ typography: 'body2', color: 'text.secondary' }}>market</Box>
      </Box>
      <Panel width={200}>
        <MenuList sx={{ py: 0.5 }}>
          <MenuItem sx={{ height: 40 }}>
            <ListItemText primary="Create tag: market" />
          </MenuItem>
        </MenuList>
      </Panel>
      <Note tone="bad">
        only reachable by typing something that does not exist
      </Note>
    </Box>
  ),
} satisfies Story;

/** 4 — the first row of the list. The Add languages dialog. */
export const RowAtTheTop = {
  args: {},
  render: () => (
    <Box>
      <Panel width={300}>
        <SearchRow placeholder="Search" addNew={false} />
        <MenuList sx={{ py: 0.5 }}>
          <MenuItem sx={{ height: 40, gap: 1 }}>
            <Plus width={18} height={18} />
            <ListItemText primary="New custom language" />
          </MenuItem>
          <Row label="English - English - en" muted />
          <Row label="Chinese - 中文 - zh" />
          <Row label="Spanish - español - es" />
        </MenuList>
      </Panel>
      <Note tone="bad">
        first row, and only once you type — before that the option is not there
        at all
      </Note>
    </Box>
  ),
} satisfies Story;

/** Three positions, two rules about when. */
export const FindingFourPositions = {
  tags: ['!dev'],
  args: {},
  render: () => (
    <Box
      sx={{
        display: 'flex',
        gap: 2,
        alignItems: 'flex-start',
        flexWrap: 'wrap',
      }}
    >
      <Box>
        <Panel width={190}>
          <SearchRow placeholder="Search..." />
          <MenuList sx={{ py: 0.5 }}>
            <Row label="demo" />
            <Row label="summer" />
          </MenuList>
        </Panel>
        <Note tone="good">in the search row · always</Note>
      </Box>
      <Box>
        <Panel width={190}>
          <SearchRow placeholder="Search" addNew={false} />
          <MenuList sx={{ py: 0.5 }}>
            <MenuItem sx={{ height: 40, gap: 1 }}>
              <Plus width={18} height={18} />
              <ListItemText primary="New custom" />
            </MenuItem>
            <Row label="Chinese" />
          </MenuList>
        </Panel>
        <Note tone="bad">first row · only while typing</Note>
      </Box>
      <Box>
        <Panel width={190}>
          <MenuList sx={{ py: 0.5 }}>
            <Row label="Czech" />
            <Row label="German" />
            <MenuItem sx={{ height: 40, color: 'primary.main', gap: 1 }}>
              <Plus width={18} height={18} />
              <ListItemText primary="Add new" />
            </MenuItem>
          </MenuList>
        </Panel>
        <Note tone="bad">last row · always, but it scrolls</Note>
      </Box>
      <Box>
        <Panel width={190}>
          <MenuList sx={{ py: 0.5 }}>
            <MenuItem sx={{ height: 40 }}>
              <ListItemText primary={'Add "tag"'} />
            </MenuItem>
          </MenuList>
        </Panel>
        <Note tone="bad">a popup row · only when nothing matches</Note>
      </Box>
    </Box>
  ),
} satisfies Story;

/** All three, side by side. */
export const FindingThreeShapes = {
  tags: ['!dev'],
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 2, alignItems: 'flex-start' }}>
      <Box>
        <Panel width={200}>
          <SearchRow placeholder="Search..." />
          <MenuList sx={{ py: 0.5 }}>
            <Row label="demo" />
            <Row label="summer" />
          </MenuList>
        </Panel>
        <Note tone="good">a + beside the search</Note>
      </Box>
      <Box>
        <Panel width={200}>
          <MenuList sx={{ py: 0.5 }}>
            <Row label="Czech" />
            <MenuItem sx={{ height: 40, color: 'primary.main', gap: 1 }}>
              <Plus width={18} height={18} />
              <ListItemText primary="Add new" />
            </MenuItem>
          </MenuList>
        </Panel>
        <Note tone="bad">a last row</Note>
      </Box>
      <Box>
        <Panel width={200}>
          <MenuList sx={{ py: 0.5 }}>
            <MenuItem sx={{ height: 40 }}>
              <ListItemText primary="Create tag: market" />
            </MenuItem>
          </MenuList>
        </Panel>
        <Note tone="bad">only when nothing matches</Note>
      </Box>
    </Box>
  ),
} satisfies Story;

/** The label and the color it is drawn in, as the four do it today. */
export const FindingNotInviting = {
  tags: ['!dev'],
  args: {},
  render: () => (
    <Box sx={{ display: 'grid', gap: 2 }}>
      <Box>
        <Panel width={260}>
          <MenuList sx={{ py: 0.5 }}>
            <MenuItem sx={{ height: 40, gap: 1, color: 'primary.main' }}>
              <Plus width={18} height={18} />
              <ListItemText primary="Add new" />
            </MenuItem>
            <MenuItem sx={{ height: 40, gap: 1 }}>
              <Plus width={18} height={18} />
              <ListItemText primary="New custom language" />
            </MenuItem>
            <MenuItem sx={{ height: 40 }}>
              <ListItemText primary={'Add "tag"'} />
            </MenuItem>
          </MenuList>
        </Panel>
        <Note tone="bad">
          today — one in primary, one in the text color, one with no icon at all
        </Note>
      </Box>
      <Box>
        <Panel width={260}>
          <MenuList sx={{ py: 0.5 }}>
            {['New namespace', 'New language', 'New tag'].map((label) => (
              <MenuItem
                key={label}
                sx={{ height: 40, gap: 1, color: 'primary.main' }}
              >
                <Plus width={18} height={18} />
                <ListItemText primary={label} />
              </MenuItem>
            ))}
          </MenuList>
        </Panel>
        <Note tone="good">
          the rule — primary, a plus, and “New” followed by the thing
        </Note>
      </Box>
    </Box>
  ),
} satisfies Story;

/** What the + hands over. */
export const CarriesTheSearch = {
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 3, alignItems: 'center' }}>
      <Box>
        <Panel width={220}>
          <SearchRow placeholder="Search namespace..." />
          <MenuList sx={{ py: 0.5 }}>
            <Typography
              variant="body2"
              sx={{ px: 2, py: 1, color: 'text.secondary' }}
            >
              Nothing found
            </Typography>
          </MenuList>
        </Panel>
        <Note>typed “marketing”, nothing matches</Note>
      </Box>
      <Box sx={{ color: 'text.secondary' }}>→</Box>
      <Box>
        <Paper elevation={3} sx={{ width: 240, p: 2 }}>
          <Typography variant="subtitle2" sx={{ mb: 1 }}>
            New namespace
          </Typography>
          <Box
            sx={{
              border: 1,
              borderColor: 'divider',
              borderRadius: 1,
              px: 1.5,
              height: 40,
              display: 'flex',
              alignItems: 'center',
              typography: 'body2',
            }}
          >
            marketing
          </Box>
        </Paper>
        <Note tone="good">the dialog opens with it already filled in</Note>
      </Box>
    </Box>
  ),
} satisfies Story;
