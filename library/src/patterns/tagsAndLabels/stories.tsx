import type { ReactNode } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { Badge, Box, Chip, Typography } from '@mui/material';
import { Bell01, Check, DotsHorizontal, Plus, XClose } from '../../icons';
import { QaCheck } from '../../icons/custom';

const meta = {
  title: 'Patterns/Tags and labels',
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

/** The pill both tags and labels draw: 24px, radius 12, 14px. */
const Pill = ({
  children,
  bg,
  fg,
  outlined,
  onDelete,
}: {
  children: ReactNode;
  bg?: string;
  fg?: string;
  outlined?: boolean;
  onDelete?: boolean;
}) => (
  <Box
    sx={(theme) => ({
      height: 24,
      minWidth: 28,
      borderRadius: '12px',
      display: 'inline-flex',
      alignItems: 'center',
      gap: 0.5,
      px: '10px',
      fontSize: 14,
      lineHeight: '18px',
      whiteSpace: 'nowrap',
      backgroundColor: bg ?? theme.palette.action.hover,
      color: fg ?? theme.palette.text.primary,
      border: outlined ? `1px solid ${theme.palette.divider}` : undefined,
    })}
  >
    {children}
    {onDelete && <XClose width={13} height={13} />}
  </Box>
);

/** Free words a person types. Tags on a key. */
export const Tags = {
  args: {},
  render: () => (
    <Box>
      <Box sx={{ display: 'flex', gap: 0.5, alignItems: 'center' }}>
        <Pill onDelete>figma</Pill>
        <Pill onDelete>draft</Pill>
        <Box
          sx={{
            width: 24,
            height: 24,
            borderRadius: '12px',
            display: 'grid',
            placeItems: 'center',
            color: 'text.secondary',
          }}
        >
          <Plus width={16} height={16} />
        </Box>
      </Box>
      <Note>anyone can add one · no color · deletable in place</Note>
    </Box>
  ),
} satisfies Story;

/** A defined set with colors somebody chose. Translation labels. */
export const Labels = {
  args: {},
  render: () => (
    <Box>
      <Box sx={{ display: 'flex', gap: 0.5 }}>
        <Pill bg="#E8F0FE" fg="#174EA6">
          needs review
        </Pill>
        <Pill bg="#FCE8E6" fg="#A50E0E">
          blocked
        </Pill>
        <Pill bg="#E6F4EA" fg="#137333">
          approved
        </Pill>
      </Box>
      <Note>
        defined in project settings · a color per label · not free-form
      </Note>
    </Box>
  ),
} satisfies Story;

/** A fact about the thing, not something anyone typed. */
export const StateMarkers = {
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 3, alignItems: 'flex-start' }}>
      <Box>
        <Chip size="small" label="Base" />
        <Note>the base language, on the dashboard</Note>
      </Box>
      <Box>
        <Chip size="small" label="Shared" color="primary" />
        <Note>a translation memory that is shared</Note>
      </Box>
      <Box>
        <Chip size="small" label="Translate" />
        <Note>a task type</Note>
      </Box>
    </Box>
  ),
} satisfies Story;

/** A badge is attached to a control — and it does not always carry a number. */
export const Counts = {
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 5, alignItems: 'flex-start' }}>
      <Box sx={{ textAlign: 'center' }}>
        <Badge badgeContent={11} color="primary">
          <QaCheck width={24} height={24} />
        </Badge>
        <Note>QA · 11 unresolved</Note>
      </Box>
      <Box sx={{ textAlign: 'center' }}>
        <Badge
          badgeContent={<Check width={12} height={12} />}
          sx={{
            '& .MuiBadge-badge': {
              bgcolor: 'emphasis.600',
              height: 16,
              minWidth: 18,
              p: 0,
            },
          }}
        >
          <QaCheck width={24} height={24} />
        </Badge>
        <Note>QA · nothing to resolve — a tick, not a zero</Note>
      </Box>
      <Box sx={{ textAlign: 'center' }}>
        <Badge
          badgeContent={<DotsHorizontal width={12} height={12} />}
          sx={{
            '& .MuiBadge-badge': {
              bgcolor: 'emphasis.600',
              height: 16,
              minWidth: 18,
              p: 0,
            },
          }}
        >
          <QaCheck width={24} height={24} />
        </Badge>
        <Note>QA · stale — the checks have not run yet</Note>
      </Box>
      <Box sx={{ textAlign: 'center' }}>
        <Badge badgeContent={4} color="secondary">
          <Bell01 width={24} height={24} />
        </Badge>
        <Note>notifications · unseen, in secondary</Note>
      </Box>
    </Box>
  ),
} satisfies Story;

/** The four kinds side by side, which is how you tell them apart. */
export const TheFourKinds = {
  args: {},
  render: () => (
    <Box sx={{ display: 'grid', gap: 1.5 }}>
      {(
        [
          [
            'tag',
            <Pill key="t" onDelete>
              figma
            </Pill>,
            'free, typed by anyone, removable',
          ],
          [
            'label',
            <Pill key="l" bg="#E8F0FE" fg="#174EA6">
              needs review
            </Pill>,
            'a defined set, colored, assigned',
          ],
          [
            'marker',
            <Chip key="m" size="small" label="Base" />,
            'a fact about the thing',
          ],
          [
            'count',
            <Badge key="c" badgeContent={11} color="primary">
              <Box sx={{ width: 24, height: 24 }} />
            </Badge>,
            'a number on a control',
          ],
        ] as [string, ReactNode, string][]
      ).map(([kind, node, desc]) => (
        <Box key={kind} sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Typography
            variant="caption"
            sx={{ width: 60, color: 'text.secondary' }}
          >
            {kind}
          </Typography>
          <Box sx={{ width: 150 }}>{node}</Box>
          <Typography variant="caption" sx={{ color: 'text.secondary' }}>
            {desc}
          </Typography>
        </Box>
      ))}
    </Box>
  ),
} satisfies Story;

/** Same shape, thirteen components. */
export const FindingThirteen = {
  tags: ['!dev'],
  args: {},
  render: () => (
    <Box>
      <Box sx={{ display: 'flex', gap: 0.75, flexWrap: 'wrap', maxWidth: 520 }}>
        {[
          'Tag',
          'TranslationLabel',
          'AgencyLabel',
          'BranchNameLabel',
          'DefaultChip',
          'TaskTypeChip',
          'TrialChip',
          'DefaultBranchChip',
          'ServiceLabel',
          'PrimaryServiceLabel',
          'SuggestionsLabel',
          'LanguageLabels',
          'GlossaryTermTags',
        ].map((n) => (
          <Pill key={n}>{n}</Pill>
        ))}
      </Box>
      <Note tone="bad">
        five on `Chip`, eight on a `Box` — one shape, thirteen components
      </Note>
    </Box>
  ),
} satisfies Story;
