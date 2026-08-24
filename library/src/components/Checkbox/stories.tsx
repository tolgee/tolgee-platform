import type { ReactNode } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { Box, Checkbox, FormControlLabel, Typography } from '@mui/material';

const meta = {
  title: 'Components/Forms/Checkbox',
  component: Checkbox,
  parameters: {
    layout: 'centered',
  },
} satisfies Meta<typeof Checkbox>;

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

/** The translations key list, rebuilt from CellKey and SelectAllCheckbox. */
export const InAList = {
  args: {},
  render: () => (
    <Box sx={{ width: 300 }}>
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          gap: 1,
          borderBottom: 1,
          borderColor: 'divider',
          py: 0.5,
        }}
      >
        <Checkbox size="small" indeterminate />
        <Typography color="text.secondary">10 keys</Typography>
      </Box>
      {['Main.Add', 'Main.Cancel'].map((key) => (
        <Box
          key={key}
          sx={{ display: 'flex', alignItems: 'center', gap: 1, py: 0.5 }}
        >
          <Checkbox size="small" />
          <Typography>{key}</Typography>
        </Box>
      ))}
      <Note>
        size=&quot;small&quot;, no label of its own — the row is the label
      </Note>
    </Box>
  ),
} satisfies Story;

/** The export dialog and the language settings, rebuilt from their own code. */
export const InAForm = {
  args: {},
  render: () => (
    <Box sx={{ display: 'grid', width: 300 }}>
      <FormControlLabel
        control={<Checkbox defaultChecked />}
        label="Enable machine translation"
      />
      <FormControlLabel
        control={<Checkbox />}
        label="Enable translation memory"
      />
      <FormControlLabel control={<Checkbox />} label="Enable for import" />
      <Note>
        default size, inside FormControlLabel — the words come with it
      </Note>
    </Box>
  ),
} satisfies Story;

export const States = {
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
      <Checkbox size="small" aria-label="Unchecked" />
      <Checkbox size="small" defaultChecked aria-label="Checked" />
      <Checkbox size="small" indeterminate aria-label="Some selected" />
      <Checkbox size="small" disabled aria-label="Unavailable" />
      <Checkbox size="small" disabled defaultChecked aria-label="Locked on" />
    </Box>
  ),
} satisfies Story;

/** One size is written; the other is what you get when nobody says. */
export const TheTwoSizes = {
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 6, alignItems: 'flex-start' }}>
      <Box sx={{ width: 240 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Checkbox size="small" defaultChecked />
          <Typography variant="body2">Suggested</Typography>
        </Box>
        <Note tone="good">size=&quot;small&quot; — written 39 times</Note>
      </Box>
      <Box sx={{ width: 240 }}>
        <FormControlLabel
          control={<Checkbox defaultChecked />}
          label="Enable for import"
        />
        <Note tone="bad">no size prop — 20 times, and nobody chose it</Note>
      </Box>
    </Box>
  ),
} satisfies Story;

/** Both sizes in one dialog, which is where it is visible. */
export const FindingTwoSizesOneDialog = {
  args: {},
  tags: ['!dev'],
  render: () => (
    <Box sx={{ width: 320 }}>
      <Box sx={{ border: 1, borderColor: 'divider', borderRadius: 1, p: 1 }}>
        {['Google', 'Amazon Translate', 'DeepL'].map((s) => (
          <Box
            key={s}
            sx={{ display: 'flex', alignItems: 'center', gap: 2, py: 0.25 }}
          >
            <Typography variant="body2" sx={{ width: 150 }}>
              {s}
            </Typography>
            <Checkbox size="small" defaultChecked />
          </Box>
        ))}
      </Box>
      <Box sx={{ mt: 2 }}>
        <FormControlLabel
          control={<Checkbox />}
          label="Enable translation memory"
        />
        <FormControlLabel
          control={<Checkbox />}
          label="Auto translate imported items"
        />
      </Box>
      <Note tone="bad">
        Machine translation settings — 20px in the table, 24px underneath it
      </Note>
    </Box>
  ),
} satisfies Story;

/** The row is visibly the label. Nothing says so. */
export const FindingNoName = {
  args: {},
  tags: ['!dev'],
  render: () => (
    <Box sx={{ display: 'flex', gap: 5, alignItems: 'flex-start' }}>
      <Box sx={{ width: 250 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Checkbox size="small" defaultChecked />
          <Typography variant="body2">Main.Add</Typography>
        </Box>
        <Note tone="bad">announced as &ldquo;checkbox, checked&rdquo;</Note>
      </Box>
      <Box sx={{ width: 250 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Checkbox
            size="small"
            defaultChecked
            inputProps={{ 'aria-label': 'Select key Main.Add' }}
          />
          <Typography variant="body2">Main.Add</Typography>
        </Box>
        <Note tone="good">
          announced as &ldquo;Select key Main.Add, checked&rdquo;
        </Note>
      </Box>
    </Box>
  ),
} satisfies Story;

/** Ten call sites take the size from the wrong context. */
export const FindingWrongSize = {
  args: {},
  tags: ['!dev'],
  render: () => (
    <Box sx={{ display: 'flex', gap: 6, alignItems: 'flex-start' }}>
      <Box sx={{ width: 250 }}>
        <FormControlLabel
          control={<Checkbox size="small" defaultChecked />}
          label="Hide done"
        />
        <Note tone="bad">small beside a label — seven of these</Note>
      </Box>
      <Box sx={{ width: 250 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Checkbox defaultChecked />
          <Typography variant="body2">English</Typography>
        </Box>
        <Note tone="bad">
          default in a row — three, all in the export dialog
        </Note>
      </Box>
    </Box>
  ),
} satisfies Story;

/** The key cell pins the box back to the size Material UI was asked for. */
export const FindingHandSized = {
  tags: ['!dev'],
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 5, alignItems: 'flex-start' }}>
      <Box sx={{ width: 230 }}>
        <Checkbox size="small" defaultChecked />
        <Note tone="bad">as the theme leaves it — 39.33px</Note>
      </Box>
      <Box sx={{ width: 230 }}>
        <Checkbox size="small" defaultChecked sx={{ width: 38, height: 38 }} />
        <Note tone="good">key cell and trash cell — pinned to 38px</Note>
      </Box>
    </Box>
  ),
} satisfies Story;
