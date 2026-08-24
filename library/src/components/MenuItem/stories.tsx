import type { ReactNode } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import {
  Box,
  Checkbox,
  ListItemText,
  MenuItem,
  MenuList,
  Paper,
} from '@mui/material';
import { ChevronRight } from '../../icons';

const meta = {
  title: 'Components/Menus/MenuItem',
  component: MenuItem,
  parameters: {
    layout: 'centered',
  },
} satisfies Meta<typeof MenuItem>;

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

const Surface = ({
  children,
  width = 220,
}: {
  children: ReactNode;
  width?: number;
}) => (
  <Paper elevation={3} sx={{ width }}>
    <MenuList sx={{ py: 1 }}>{children}</MenuList>
  </Paper>
);

export const Plain = {
  args: {},
  render: () => (
    <Surface>
      <MenuItem>Edit</MenuItem>
      <MenuItem selected>Duplicate</MenuItem>
      <MenuItem disabled>Delete</MenuItem>
    </Surface>
  ),
} satisfies Story;

/** The product's row: 40px, whatever is in it. */
export const Compact = {
  args: {},
  render: () => (
    <Surface>
      <MenuItem sx={{ height: 40 }}>
        <ListItemText primary="Czech" />
      </MenuItem>
      <MenuItem sx={{ height: 40 }}>
        <ListItemText primary="English" />
      </MenuItem>
    </Surface>
  ),
} satisfies Story;

export const WithACheckbox = {
  args: {},
  render: () => (
    <Surface>
      <MenuItem sx={{ height: 40 }}>
        <Checkbox size="small" checked sx={{ p: 0.5, mr: 1 }} />
        <ListItemText primary="Untranslated" />
      </MenuItem>
      <MenuItem sx={{ height: 40 }}>
        <Checkbox size="small" sx={{ p: 0.5, mr: 1 }} />
        <ListItemText primary="Reviewed" />
      </MenuItem>
    </Surface>
  ),
} satisfies Story;

export const WithASubmenu = {
  args: {},
  render: () => (
    <Surface>
      <MenuItem sx={{ display: 'flex', justifyContent: 'space-between' }}>
        <ListItemText primary="Languages" />
        <ChevronRight width={18} height={18} />
      </MenuItem>
      <MenuItem sx={{ display: 'flex', justifyContent: 'space-between' }}>
        <ListItemText primary="Tags" />
        <ChevronRight width={18} height={18} />
      </MenuItem>
    </Surface>
  ),
} satisfies Story;

/** Two rows that should be the same height, from two components that both say 40px. */
export const FindingTwoCompactRows = {
  tags: ['!dev'],
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 3, alignItems: 'flex-start' }}>
      <Box>
        <Surface width={190}>
          <MenuItem sx={{ height: 40 }}>
            <ListItemText primary="Through CompactMenuItem" />
          </MenuItem>
        </Surface>
        <Note>ListComponents — used by three row types</Note>
      </Box>
      <Box>
        <Surface width={190}>
          <MenuItem sx={{ height: 40 }}>
            <ListItemText primary="Through SearchStyled" />
          </MenuItem>
        </Surface>
        <Note tone="bad">a second styled(MenuItem) saying the same thing</Note>
      </Box>
    </Box>
  ),
} satisfies Story;

/** SubmenuItem skips the shared row, so it is not 40px like its neighbors. */
export const FindingSubmenuHeight = {
  tags: ['!dev'],
  args: {},
  render: () => (
    <Box>
      <Surface width={220}>
        <MenuItem sx={{ height: 40 }}>
          <ListItemText primary="Untranslated" />
        </MenuItem>
        <MenuItem sx={{ display: 'flex', justifyContent: 'space-between' }}>
          <ListItemText primary="Languages" />
          <ChevronRight width={18} height={18} />
        </MenuItem>
        <MenuItem sx={{ height: 40 }}>
          <ListItemText primary="Reviewed" />
        </MenuItem>
      </Surface>
      <Note tone="bad">
        the middle row is a SubmenuItem — built on MenuItem directly, so it
        keeps Material UI&apos;s height
      </Note>
    </Box>
  ),
} satisfies Story;
