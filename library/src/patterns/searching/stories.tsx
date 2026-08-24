import type { ReactNode } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import {
  Box,
  IconButton,
  InputAdornment,
  TextField,
  Typography,
} from '@mui/material';
import { HelpCircle, SearchSm, XClose } from '../../icons';

const meta = {
  title: 'Patterns/Searching',
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

const Search = ({
  value,
  placeholder = 'Search',
  width = 260,
  clear = true,
  icon = true,
}: {
  value?: string;
  placeholder?: string;
  width?: number;
  clear?: boolean;
  icon?: boolean;
}) => (
  <TextField
    size="small"
    value={value ?? ''}
    placeholder={placeholder}
    sx={{ width }}
    InputProps={{
      readOnly: true,
      startAdornment: icon ? (
        <InputAdornment position="start">
          <SearchSm width={20} height={20} />
        </InputAdornment>
      ) : undefined,
      endAdornment:
        clear && value ? (
          <InputAdornment position="end">
            <IconButton size="small" aria-label="Clear search">
              <XClose width={20} height={20} />
            </IconButton>
          </InputAdornment>
        ) : undefined,
    }}
  />
);

/** Filtering a list you can see. The common one. */
export const FilterAList = {
  args: {},
  render: () => (
    <Box>
      <Search value="homepage" />
      <Box sx={{ mt: 1 }}>
        {['homepage.title', 'homepage.subtitle', 'homepage.button'].map((k) => (
          <Typography key={k} variant="body2" sx={{ py: 0.5 }}>
            {k}
          </Typography>
        ))}
      </Box>
      <Note>
        magnifier, clear button, 500ms debounce — the list narrows below
      </Note>
    </Box>
  ),
} satisfies Story;

/** Rebuilt from searchFieldStyles: a bordered 40px row, everything inside it. */
const SyntaxSearch = ({
  value,
  width = 320,
  clipped = true,
}: {
  value: string;
  width?: number;
  clipped?: boolean;
}) => (
  <Box
    sx={{
      display: 'flex',
      alignItems: 'center',
      gap: 0.75,
      height: 40,
      width,
      px: 1,
      pl: 1.5,
      border: 1,
      borderColor: 'divider',
      borderRadius: 1,
      bgcolor: 'background.default',
    }}
  >
    <Box sx={{ display: 'flex', color: 'text.secondary', flexShrink: 0 }}>
      <SearchSm width={20} height={20} />
    </Box>
    <Box
      sx={{
        flexGrow: 1,
        minWidth: 0,
        overflowX: clipped ? 'hidden' : 'auto',
        whiteSpace: 'nowrap',
        typography: 'body2',
        scrollbarWidth: 'none',
        '&::-webkit-scrollbar': { display: 'none' },
      }}
    >
      {value}
    </Box>
    <IconButton size="small" aria-label="Clear search" sx={{ flexShrink: 0 }}>
      <XClose width={20} height={20} />
    </IconButton>
    <IconButton
      size="small"
      aria-label="Search syntax help"
      sx={{ flexShrink: 0 }}
    >
      <HelpCircle width={20} height={20} />
    </IconButton>
  </Box>
);

/** Searching with syntax. Needs an editor and a way to learn it. */
export const WithSyntax = {
  args: {},
  render: () => (
    <Box>
      <SyntaxSearch value="state:untranslated homepage" />
      <Note>
        a CodeMirror editor in a bordered row — magnifier, clear and the help
        icon all sit inside the border
      </Note>
    </Box>
  ),
} satisfies Story;

/** Nothing found. The state every search needs and the product does not define. */
export const NoResults = {
  args: {},
  render: () => (
    <Box sx={{ width: 300 }}>
      <Search value="zzzz" />
      <Box sx={{ mt: 2, textAlign: 'center' }}>
        <Typography variant="body2" color="text.secondary">
          Nothing matches “zzzz”
        </Typography>
        <Typography variant="caption" color="text.secondary">
          Try a shorter search
        </Typography>
      </Box>
      <Note tone="bad">proposed — the product has no shared empty state</Note>
    </Box>
  ),
} satisfies Story;

/** One job, four components, three behaviors. */
export const FindingFourImplementations = {
  tags: ['!dev'],
  args: {},
  render: () => (
    <Box sx={{ display: 'grid', gap: 2 }}>
      <Box>
        <Search value="homepage" width={280} />
        <Note tone="good">SearchField — icon, clear, 500ms debounce</Note>
      </Box>
      <Box>
        <Search value="homepage" width={250} />
        <Note tone="good">
          SecondaryBarSearchField — the same, wrapped, 250px
        </Note>
      </Box>
      <Box>
        <Search value="homepage" width={200} />
        <Note tone="bad">
          HeaderSearchField — looks the same, fires on every keystroke
        </Note>
      </Box>
      <Box>
        <Search value="Czech" width={220} icon={false} clear={false} />
        <Note tone="bad">FlagSearchField — no magnifier, no clear button</Note>
      </Box>
    </Box>
  ),
} satisfies Story;

/** The overflow today, and the one line that fixes it. */
export const FindingOverflow = {
  tags: ['!dev'],
  args: {},
  render: () => {
    const long =
      'state:untranslated key:homepage.title language:cs tag:draft namespace:web';
    return (
      <Box sx={{ display: 'grid', gap: 2 }}>
        <Box>
          <SyntaxSearch value={long} />
          <Note tone="bad">
            today — `overflow-x: hidden`, clipped, and no way to scroll back
          </Note>
        </Box>
        <Box>
          <SyntaxSearch value={long} clipped={false} />
          <Note tone="good">
            proposed — `overflow-x: auto` with the scrollbar hidden: wheel, drag
            and selection all work, exactly as a native input does
          </Note>
        </Box>
      </Box>
    );
  },
} satisfies Story;
