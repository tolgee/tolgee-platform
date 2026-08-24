import type { ReactNode } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { Box, Divider, MenuItem, MenuList, Paper } from '@mui/material';

const meta = {
  title: 'Components/Menus/Menu',
  component: MenuList,
  parameters: {
    layout: 'centered',
  },
} satisfies Meta<typeof MenuList>;

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

/** A menu is a Paper with a list in it. Shown open, since that is the only state worth drawing. */
const Surface = ({
  children,
  width = 200,
}: {
  children: ReactNode;
  width?: number;
}) => (
  <Paper elevation={3} sx={{ width }}>
    <MenuList sx={{ py: 1 }}>{children}</MenuList>
  </Paper>
);

export const TheStandard = {
  args: {},
  render: () => (
    <Surface>
      <MenuItem>Edit</MenuItem>
      <MenuItem>Duplicate</MenuItem>
      <Divider />
      <MenuItem>Delete</MenuItem>
    </Surface>
  ),
} satisfies Story;

export const WithASelection = {
  args: {},
  render: () => (
    <Surface>
      <MenuItem selected>Newest first</MenuItem>
      <MenuItem>Oldest first</MenuItem>
      <MenuItem>Alphabetical</MenuItem>
    </Surface>
  ),
} satisfies Story;

/** Three avatar menus, one styled block, written out three times. */
export const FindingThreePopovers = {
  tags: ['!dev'],
  args: {},
  render: () => (
    <Box>
      <Box sx={{ display: 'flex', gap: 2 }}>
        <Surface width={170}>
          <MenuItem>Account settings</MenuItem>
          <MenuItem>Log out</MenuItem>
        </Surface>
        <Surface width={170}>
          <MenuItem>Verify e-mail</MenuItem>
          <MenuItem>Log out</MenuItem>
        </Surface>
        <Surface width={170}>
          <MenuItem>Add a picture</MenuItem>
          <MenuItem>Log out</MenuItem>
        </Surface>
      </Box>
      <Note tone="bad">
        UserPresentAvatarMenu, UserUnverifiedEmailMenu, UserMissingAvatarMenu —
        the same styled(Popover), character for character
      </Note>
    </Box>
  ),
} satisfies Story;
