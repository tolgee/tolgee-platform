import type { ReactNode } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import {
  Box,
  Checkbox,
  IconButton,
  Divider,
  InputBase,
  ListItemText,
  MenuItem,
  MenuList,
  Paper,
  Typography,
} from '@mui/material';
import { FlagImage } from '../../components/languages/FlagImage';
import { ChevronRight, Edit02, SearchSm, XClose } from '../../icons';

const meta = {
  title: 'Patterns/Picking a language',
  component: Box,
  parameters: {
    layout: 'centered',
  },
} satisfies Meta<typeof Box>;

export default meta;

type Story = StoryObj<typeof meta>;

const LANGS = [
  { name: 'English', original: 'English', tag: 'en', flag: '🇬🇧' },
  { name: 'Czech', original: 'Čeština', tag: 'cs', flag: '🇨🇿' },
  { name: 'German', original: 'Deutsch', tag: 'de', flag: '🇩🇪' },
  { name: 'French', original: 'Français', tag: 'fr', flag: '🇫🇷' },
];

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
  width = 240,
}: {
  children: ReactNode;
  width?: number;
}) => (
  <Paper elevation={3} sx={{ width, overflow: 'hidden' }}>
    {children}
  </Paper>
);

const Trigger = ({
  children,
  width = 240,
}: {
  children: ReactNode;
  width?: number;
}) => (
  <Box
    sx={{
      width,
      height: 40,
      px: 1.5,
      display: 'flex',
      alignItems: 'center',
      gap: 1,
      border: 1,
      borderColor: 'divider',
      borderRadius: 1,
    }}
  >
    {children}
  </Box>
);

const SearchRow = () => (
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
      placeholder="Find language"
    />
  </Box>
);

/** 1 — Create project, Base language. A plain Select with the flag after the name. */
export const SingleSelect = {
  args: {},
  render: () => (
    <Box>
      <Trigger width={200}>
        <Box sx={{ flexGrow: 1 }}>English</Box>
        <FlagImage flagEmoji="🇬🇧" width={20} />
      </Trigger>
      <Panel width={200}>
        <MenuList sx={{ py: 0.5 }}>
          {LANGS.slice(0, 2).map((l) => (
            <MenuItem key={l.tag} sx={{ height: 40, gap: 1 }}>
              <ListItemText primary={l.name} />
              <FlagImage flagEmoji={l.flag} width={20} />
            </MenuItem>
          ))}
        </MenuList>
      </Panel>
      <Note>name first, flag last, no tag</Note>
    </Box>
  ),
} satisfies Story;

/** 2 — Create project, Find language. Searches the whole ISO catalogue. */
export const SearchTheCatalogue = {
  args: {},
  render: () => (
    <Box>
      <Panel width={300}>
        <SearchRow />
        <MenuList sx={{ py: 0.5 }}>
          <MenuItem disabled sx={{ height: 40, gap: 1 }}>
            <FlagImage flagEmoji="🇬🇧" width={20} />
            <ListItemText primary="English - English - en" />
          </MenuItem>
          {LANGS.slice(1).map((l) => (
            <MenuItem key={l.tag} sx={{ height: 40, gap: 1 }}>
              <FlagImage flagEmoji={l.flag} width={20} />
              <ListItemText primary={`${l.name} - ${l.original} - ${l.tag}`} />
            </MenuItem>
          ))}
        </MenuList>
      </Panel>
      <Note>flag first, name — original — tag, already-added greyed out</Note>
    </Box>
  ),
} satisfies Story;

/** 3 — Translations header. Multiselect with two rules on top. */
export const MultiWithRules = {
  args: {},
  render: () => (
    <Box>
      <Trigger width={220}>
        <Box sx={{ flexGrow: 1 }}>en, cs, fr, de</Box>
      </Trigger>
      <Panel width={220}>
        <MenuList sx={{ py: 0.5 }}>
          <MenuItem sx={{ height: 40 }}>
            <Checkbox size="small" checked sx={{ p: 0.5, mr: 1 }} />
            <ListItemText primary="All languages" />
          </MenuItem>
          <MenuItem sx={{ height: 40 }}>
            <Checkbox size="small" sx={{ p: 0.5, mr: 1 }} />
            <ListItemText primary="Base language only" />
          </MenuItem>
          <Divider />
          {LANGS.map((l) => (
            <MenuItem key={l.tag} sx={{ height: 40 }}>
              <Checkbox size="small" checked sx={{ p: 0.5, mr: 1 }} />
              <ListItemText primary={l.name} />
            </MenuItem>
          ))}
        </MenuList>
      </Panel>
      <Note>tags in the trigger, names in the list, no flags anywhere</Note>
    </Box>
  ),
} satisfies Story;

/** 4 — Import. One per row, underlined, with a way to create a new one. */
export const InATableRow = {
  args: {},
  render: () => (
    <Box>
      <Box sx={{ width: 220 }}>
        <Typography variant="caption" color="text.secondary">
          Language
        </Typography>
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            gap: 1,
            borderBottom: 1,
            borderColor: 'text.primary',
            pb: 0.5,
          }}
        >
          <FlagImage flagEmoji="🇨🇿" width={20} />
          <Box sx={{ flexGrow: 1 }}>Czech</Box>
          <XClose width={16} height={16} />
        </Box>
      </Box>
      <Panel width={220}>
        <MenuList sx={{ py: 0.5 }}>
          {LANGS.slice(0, 3).map((l) => (
            <MenuItem key={l.tag} sx={{ height: 40, gap: 1 }}>
              <FlagImage flagEmoji={l.flag} width={20} />
              <ListItemText primary={l.name} />
            </MenuItem>
          ))}
          <MenuItem sx={{ height: 40, color: 'primary.main' }}>
            <ListItemText primary="+ Add new" />
          </MenuItem>
        </MenuList>
      </Panel>
      <Note tone="bad">underlined, not the product&apos;s field</Note>
    </Box>
  ),
} satisfies Story;

/** 5 — Project tasks filter. Flags in the trigger, names in the list. */
export const InsideAFilterMenu = {
  args: {},
  render: () => (
    <Box>
      <Trigger width={170}>
        <FlagImage flagEmoji="🇨🇿" width={20} />
        <FlagImage flagEmoji="🇩🇪" width={20} />
        <Box sx={{ flexGrow: 1 }} />
        <XClose width={16} height={16} />
      </Trigger>
      <Box sx={{ display: 'flex', alignItems: 'flex-start' }}>
        <Panel width={170}>
          <MenuList sx={{ py: 0.5 }}>
            <MenuItem sx={{ display: 'flex', justifyContent: 'space-between' }}>
              <ListItemText primary="Assignees" />
              <ChevronRight width={18} height={18} />
            </MenuItem>
            <MenuItem
              selected
              sx={{ display: 'flex', justifyContent: 'space-between' }}
            >
              <ListItemText primary="Languages" />
              <ChevronRight width={18} height={18} />
            </MenuItem>
          </MenuList>
        </Panel>
        <Panel width={150}>
          <MenuList sx={{ py: 0.5 }}>
            {LANGS.map((l, i) => (
              <MenuItem key={l.tag} sx={{ height: 40 }}>
                <Checkbox size="small" checked={i < 2} sx={{ p: 0.5, mr: 1 }} />
                <ListItemText primary={l.name} />
              </MenuItem>
            ))}
          </MenuList>
        </Panel>
      </Box>
      <Note tone="bad">
        flags only in the trigger, names only in the list — the exact opposite
        of the others
      </Note>
    </Box>
  ),
} satisfies Story;

/** 6 — Create project, Languages. Rebuilt from PreparedLanguage. */
export const AsChips = {
  args: {},
  render: () => (
    <Box>
      <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mb: 1 }}>
        {LANGS.slice(0, 2).map((l) => (
          <Box
            key={l.tag}
            sx={{
              display: 'inline-flex',
              alignItems: 'center',
              bgcolor: 'action.hover',
              borderRadius: 1,
              pl: 1.5,
              pr: 0.5,
              py: 0.5,
            }}
          >
            <FlagImage
              flagEmoji={l.flag}
              style={{ width: 20, height: 20, marginRight: 8 }}
            />
            <Box sx={{ typography: 'body2' }}>
              {l.name} | {l.original} ({l.tag})
            </Box>
            <IconButton size="small" sx={{ ml: 1 }} aria-label="Customize">
              <Edit02 width={20} height={20} />
            </IconButton>
            <IconButton size="small" aria-label="Remove">
              <XClose width={20} height={20} />
            </IconButton>
          </Box>
        ))}
      </Box>
      <Note tone="good">
        the complete label — and the only picker that lets you edit a language
        before it exists
      </Note>
    </Box>
  ),
} satisfies Story;

/** 7 — Permissions and glossary. Search plus checkbox rows. */
export const SearchAndCheck = {
  args: {},
  render: () => (
    <Box>
      <Panel width={230}>
        <SearchRow />
        <MenuList sx={{ py: 0.5 }}>
          {LANGS.map((l, i) => (
            <MenuItem key={l.tag} sx={{ height: 40 }}>
              <Checkbox size="small" checked={i < 2} sx={{ p: 0.5, mr: 1 }} />
              <FlagImage flagEmoji={l.flag} width={20} />
              <ListItemText primary={l.name} sx={{ ml: 1 }} />
            </MenuItem>
          ))}
        </MenuList>
      </Panel>
      <Note>search plus checkboxes — the closest to a reusable shape</Note>
    </Box>
  ),
} satisfies Story;

/** The proposal: three variants, one way of drawing a language. */
export const ProposedThree = {
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 4, alignItems: 'flex-start' }}>
      <Box>
        <Trigger width={210}>
          <FlagImage flagEmoji="🇬🇧" width={20} />
          <Box sx={{ flexGrow: 1 }}>English</Box>
        </Trigger>
        <Note tone="good">one — a single existing language</Note>
      </Box>
      <Box>
        <Trigger width={210}>
          <FlagImage flagEmoji="🇬🇧" width={20} />
          <FlagImage flagEmoji="🇨🇿" width={20} />
          <Box sx={{ flexGrow: 1, color: 'text.secondary' }}>+2</Box>
        </Trigger>
        <Note tone="good">several — existing languages</Note>
      </Box>
      <Box>
        <Trigger width={210}>
          <SearchSm width={18} height={18} />
          <Box sx={{ flexGrow: 1, color: 'text.secondary' }}>Find language</Box>
        </Trigger>
        <Note tone="good">add — search the whole catalogue</Note>
      </Box>
    </Box>
  ),
} satisfies Story;
