import type { ReactNode } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { Badge, Box, Chip } from '@mui/material';
import { Bell01, CheckCircle, Flag02, XClose } from '../../icons';
import { QaCheck } from '../../icons/custom';

const meta = {
  title: 'Components/Chip',
  component: Chip,
  parameters: {
    layout: 'centered',
  },
} satisfies Meta<typeof Chip>;

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

/** BaseChip: 12px side padding, no label padding, 15px text. */
const baseChipSx = {
  px: 1.5,
  '& .MuiChip-label': { px: 0, fontSize: 15 },
  '& .MuiChip-icon': { ml: 0, mr: '4px' },
} as const;

export const Plain = {
  args: { label: 'Base' },
  render: () => (
    <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
      <Chip size="small" label="Base" />
      <Chip size="small" label="Shared" color="primary" />
      <Chip size="small" label="Project only" />
    </Box>
  ),
} satisfies Story;

/** The three the product shares, rebuilt from common/chips. */
export const TheSharedThree = {
  args: { label: '' },
  render: () => (
    <Box sx={{ display: 'flex', gap: 3, alignItems: 'flex-start' }}>
      <Box>
        <Chip
          label="Translate"
          icon={<Flag02 width={16} height={16} />}
          sx={baseChipSx}
        />
        <Note>DefaultChip — icon in `text.secondary`</Note>
      </Box>
      <Box>
        <Chip
          label="Done"
          icon={<CheckCircle width={16} height={16} />}
          sx={(theme) => ({
            ...baseChipSx,
            backgroundColor: theme.palette.tokens.success.main,
            color: theme.palette.common.white,
          })}
        />
        <Note>SuccessChip — the success token, not MUI&apos;s green</Note>
      </Box>
      <Box>
        <Chip
          label="main"
          sx={(theme) => ({
            ...baseChipSx,
            backgroundColor: theme.palette.tokens.background.onDefault,
            border: `1px solid ${theme.palette.tokens._components.input.outlined.enabledBorder}`,
          })}
        />
        <Note>TransparentChip — outlined, sits on a colored surface</Note>
      </Box>
    </Box>
  ),
} satisfies Story;

export const Deletable = {
  args: { label: '' },
  render: () => (
    <Box sx={{ display: 'flex', gap: 1 }}>
      <Chip
        size="small"
        label="figma"
        onDelete={() => {}}
        deleteIcon={<XClose width={14} height={14} />}
      />
      <Chip
        size="small"
        label="draft"
        onDelete={() => {}}
        deleteIcon={<XClose width={14} height={14} />}
      />
    </Box>
  ),
} satisfies Story;

export const AsABadge = {
  args: { label: '' },
  render: () => (
    <Box sx={{ display: 'flex', gap: 5, alignItems: 'center' }}>
      <Box sx={{ textAlign: 'center' }}>
        <Badge badgeContent={11} color="primary">
          <QaCheck width={24} height={24} />
        </Badge>
        <Note>QaBadge — `QaCheck`, count in primary</Note>
      </Box>
      <Box sx={{ textAlign: 'center' }}>
        <Badge badgeContent={4} color="secondary">
          <Bell01 width={24} height={24} />
        </Badge>
        <Note>notifications — `Bell01`, count in secondary</Note>
      </Box>
    </Box>
  ),
} satisfies Story;

/** One pill, three definitions. */
export const FindingThreePills = {
  tags: ['!dev'],
  args: { label: '' },
  render: () => {
    const pill = {
      height: 24,
      borderRadius: '12px',
      display: 'inline-flex',
      alignItems: 'center',
      px: '10px',
      fontSize: 14,
    } as const;
    return (
      <Box sx={{ display: 'grid', gap: 1.5 }}>
        <Box>
          <Box sx={{ ...pill, bgcolor: 'action.hover' }}>figma</Box>
          <Note>
            Tags → `Wrapper` · 24px, radius 12, 14px, emphasis background
          </Note>
        </Box>
        <Box>
          <Box sx={{ ...pill, bgcolor: 'action.hover' }}>needs review</Box>
          <Note>
            Labels → `StyledTranslationLabel` · 24px, radius 12, 14px, color
            from the label
          </Note>
        </Box>
        <Box>
          <Chip label="Base" sx={baseChipSx} />
          <Note tone="bad">
            BaseChip · 15px text, and Material UI&apos;s height
          </Note>
        </Box>
      </Box>
    );
  },
} satisfies Story;

/** Eight components draw a pill without using Chip at all. */
export const FindingReinvented = {
  tags: ['!dev'],
  args: { label: '' },
  render: () => (
    <Box>
      <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
        {[
          'TranslationLabel',
          'AgencyLabel',
          'BranchNameLabel',
          'ServiceLabel',
        ].map((n) => (
          <Box
            key={n}
            sx={{
              height: 24,
              borderRadius: '12px',
              display: 'inline-flex',
              alignItems: 'center',
              px: '10px',
              fontSize: 14,
              bgcolor: 'action.hover',
            }}
          >
            {n}
          </Box>
        ))}
      </Box>
      <Note tone="bad">
        each a `styled(Box)` or a bare div — none of them a Chip
      </Note>
    </Box>
  ),
} satisfies Story;
