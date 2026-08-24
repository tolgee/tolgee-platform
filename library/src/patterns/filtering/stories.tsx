import type { ReactNode } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import {
  Box,
  Button,
  Checkbox,
  Divider,
  ListItemText,
  MenuItem,
  MenuList,
  Paper,
  Radio,
  Typography,
} from '@mui/material';
import { FlagImage } from '../../components/languages/FlagImage';
import { ChevronDown, ChevronRight, XClose } from '../../icons';

const meta = {
  title: 'Patterns/Filtering',
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

const Trigger = ({
  children,
  width = 220,
  clearable,
}: {
  children: ReactNode;
  width?: number;
  clearable?: boolean;
}) => (
  <Box
    sx={{
      width,
      height: 40,
      px: 1.5,
      display: 'flex',
      alignItems: 'center',
      gap: 0.75,
      border: 1,
      borderColor: 'divider',
      borderRadius: 1,
      cursor: 'pointer',
    }}
  >
    <Box sx={{ flexGrow: 1, minWidth: 0, typography: 'body2' }}>{children}</Box>
    {clearable && <XClose width={16} height={16} />}
    <ChevronDown width={18} height={18} />
  </Box>
);

const Panel = ({
  children,
  width = 220,
}: {
  children: ReactNode;
  width?: number;
}) => (
  <Paper elevation={3} sx={{ width }}>
    <MenuList sx={{ py: 0.5 }}>{children}</MenuList>
  </Paper>
);

const Submenu = ({
  label,
  selected,
}: {
  label: string;
  selected?: boolean;
}) => (
  <MenuItem
    selected={selected}
    sx={{ display: 'flex', justifyContent: 'space-between' }}
  >
    <ListItemText primary={label} />
    <ChevronRight width={18} height={18} />
  </MenuItem>
);

/** Rebuilt from ButtonToggle: contained, its own tokens, and a shadow. */
const ExcludeButton = ({ active }: { active?: boolean }) => (
  <Button
    variant="contained"
    disableElevation
    sx={(theme) => {
      const t = theme.palette.tokens._components.buttonToggle;
      return {
        ml: 1,
        px: 1,
        py: 0.5,
        fontSize: 13,
        fontWeight: 500,
        lineHeight: 'normal',
        textTransform: 'none',
        alignSelf: 'center',
        minHeight: '0px !important',
        borderRadius: '4px',
        boxShadow: '0px 2px 8px 0px rgba(0, 0, 0, 0.2)',
        backgroundColor: active ? t.active : t.enabled,
        color: active ? t.textActive : t.textEnabled,
        '&:hover': {
          boxShadow: '0px 2px 8px 0px rgba(0, 0, 0, 0.2)',
          backgroundColor: active ? t.activeHover : t.hovered,
          color: active ? t.textActiveHover : t.textHovered,
        },
      };
    }}
  >
    {active ? 'Excluded' : 'Exclude'}
  </Button>
);

/** Rebuilt from FilterItem: a 40px grid row, the toggle hidden until hover. */
const Row = ({
  label,
  checked,
  exclusive,
  excluded,
  showExclude,
  revealed,
}: {
  label: string;
  checked?: boolean;
  exclusive?: boolean;
  excluded?: boolean;
  showExclude?: boolean;
  revealed?: boolean;
}) => {
  const Control = exclusive ? Radio : Checkbox;
  return (
    <MenuItem
      sx={{
        height: 40,
        display: 'grid',
        gridTemplateColumns: 'auto 1fr auto auto',
        alignItems: 'center',
        gap: 1,
        pl: '4px !important',
        '& .hidden': { opacity: 0, transition: 'opacity ease-in 0.1s' },
        '&:hover .hidden': { opacity: 1 },
      }}
    >
      <Control
        size="small"
        checked={checked || excluded}
        sx={(theme) => ({
          margin: '-8px -8px -8px 0px',
          ...(excluded ? { color: theme.palette.tokens.icon.primary } : {}),
        })}
      />
      <ListItemText primary={label} sx={{ overflow: 'hidden' }} />
      {showExclude && (
        <Box className={excluded || revealed ? undefined : 'hidden'}>
          <ExcludeButton active={excluded} />
        </Box>
      )}
    </MenuItem>
  );
};

/** The whole shape: a trigger, a menu of groups, a submenu of rows. */
export const Anatomy = {
  args: {},
  render: () => (
    <Box>
      <Trigger clearable>2 filters</Trigger>
      <Box sx={{ display: 'flex', alignItems: 'flex-start', mt: 0.5 }}>
        <Panel>
          <Submenu label="Tags" />
          <Submenu label="Namespaces" />
          <Submenu label="Labels" selected />
          <Divider />
          <Typography variant="caption" sx={{ px: 2, color: 'text.secondary' }}>
            Translations
          </Typography>
          <Row label="Untranslated" checked />
          <Row label="Reviewed" />
        </Panel>
        <Panel width={200}>
          <Row label="draft" checked showExclude />
          <Row label="figma" showExclude />
          <Row label="review" excluded showExclude />
        </Panel>
      </Box>
      <Note>
        trigger · groups · submenu — the shape all three families share
      </Note>
    </Box>
  ),
} satisfies Story;

/** Three kinds of row, and the app has all three. */
export const TheThreeRows = {
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 3, alignItems: 'flex-start' }}>
      <Box>
        <Panel width={200}>
          <Row label="Untranslated" checked />
          <Row label="Reviewed" />
        </Panel>
        <Note>several at once — checkbox</Note>
      </Box>
      <Box>
        <Panel width={200}>
          <Row label="Any state" checked exclusive />
          <Row label="Untranslated" exclusive />
        </Panel>
        <Note>one of — radio</Note>
      </Box>
      <Box>
        <Panel width={210}>
          <Row label="draft" checked showExclude />
          <Row label="review" excluded showExclude />
        </Panel>
        <Note>in, or explicitly out — exclude</Note>
      </Box>
    </Box>
  ),
} satisfies Story;

/** ButtonToggle on its own: the four states it has tokens for. */
export const ExcludeToggle = {
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 3, alignItems: 'center' }}>
      <Box sx={{ textAlign: 'center' }}>
        <ExcludeButton />
        <Note>enabled</Note>
      </Box>
      <Box sx={{ textAlign: 'center' }}>
        <ExcludeButton active />
        <Note>active — the tag is excluded</Note>
      </Box>
    </Box>
  ),
} satisfies Story;

/** In place: invisible until the row is hovered, permanent once used. */
export const ExcludeInARow = {
  args: {},
  render: () => (
    <Box>
      <Panel width={230}>
        <Row label="draft" checked showExclude />
        <Row label="figma" showExclude revealed />
        <Row label="review" excluded showExclude />
      </Panel>
      <Note>
        first row: nothing shown · second: hovered · third: excluded, so it
        stays
      </Note>
    </Box>
  ),
} satisfies Story;

/** Two ways the trigger says what is on. */
export const TwoSummaries = {
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 4, alignItems: 'flex-start' }}>
      <Box>
        <Trigger clearable>Untranslated</Trigger>
        <Note>one filter — its name</Note>
      </Box>
      <Box>
        <Trigger clearable>2 filters</Trigger>
        <Note>more than one — a count</Note>
      </Box>
      <Box>
        <Trigger clearable width={170}>
          <Box sx={{ display: 'flex', gap: 0.5, alignItems: 'center' }}>
            <FlagImage flagEmoji="🇨🇿" width={20} />
            <FlagImage flagEmoji="🇩🇪" width={20} />
          </Box>
        </Trigger>
        <Note tone="bad">the task filter — icons, no words</Note>
      </Box>
    </Box>
  ),
} satisfies Story;

/** The trigger is a text field pretending to be a select. */
export const FindingFakeField = {
  tags: ['!dev'],
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 4, alignItems: 'flex-start' }}>
      <Box>
        <Trigger clearable>2 filters</Trigger>
        <Note tone="bad">
          a TextField with readOnly and inputComponent — announced as a text box
          you cannot type in
        </Note>
      </Box>
      <Box>
        <Trigger clearable>2 filters</Trigger>
        <Note tone="good">
          the same picture with role=&quot;combobox&quot; and aria-expanded
        </Note>
      </Box>
    </Box>
  ),
} satisfies Story;

/** Three families draw the same menu from three sets of components. */
export const FindingThreeFamilies = {
  tags: ['!dev'],
  args: {},
  render: () => (
    <Box>
      <Box sx={{ display: 'flex', gap: 2, alignItems: 'flex-start' }}>
        <Box>
          <Panel width={180}>
            <Submenu label="Tags" />
            <Submenu label="Labels" />
            <Row label="Untranslated" checked />
          </Panel>
          <Note tone="bad">Translations — 9 subfilters</Note>
        </Box>
        <Box>
          <Panel width={180}>
            <Submenu label="Assignees" />
            <Submenu label="Languages" />
            <Row label="Translate" />
          </Panel>
          <Note tone="bad">Tasks — 4 subfilters</Note>
        </Box>
        <Box>
          <Panel width={180}>
            <Submenu label="QA checks" />
            <Row label="Placeholders" checked />
          </Panel>
          <Note tone="bad">QA — its own row component</Note>
        </Box>
      </Box>
      <Note tone="bad">
        one picture, three implementations of the row and two of the trigger
      </Note>
    </Box>
  ),
} satisfies Story;
