import type { ReactNode } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { Box, Chip, LinearProgress, Typography } from '@mui/material';
import { FlagImage } from '../../components/languages/FlagImage';

const meta = {
  title: 'Patterns/Showing a language',
  component: Box,
  parameters: {
    layout: 'centered',
  },
} satisfies Meta<typeof Box>;

export default meta;

type Story = StoryObj<typeof meta>;

const LANGS = [
  { name: 'English', original: 'English', tag: 'en', flag: '🇬🇧', base: true },
  { name: 'Czech', original: 'čeština', tag: 'cs', flag: '🇨🇿' },
  { name: 'German', original: 'Deutsch', tag: 'de', flag: '🇩🇪' },
  { name: 'French', original: 'français', tag: 'fr', flag: '🇫🇷' },
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

/** The circular one: a round window over a flag that is drawn wider than it. */
const Circled = ({ flag, size = 18 }: { flag: string; size?: number }) => (
  <Box
    sx={{
      width: size,
      height: size,
      borderRadius: '50%',
      overflow: 'hidden',
      display: 'inline-flex',
      alignItems: 'center',
      justifyContent: 'center',
      flexShrink: 0,
      position: 'relative',
      filter: 'drop-shadow(0px 0px 1px rgba(0,0,0,0.2))',
    }}
  >
    <FlagImage
      flagEmoji={flag}
      height={size}
      style={{ position: 'absolute', height: size * 1.4 }}
    />
  </Box>
);

/** The two shapes, side by side, at the size each is used at. */
export const TheTwoShapes = {
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 6, alignItems: 'flex-start' }}>
      <Box>
        <Box sx={{ display: 'grid', gap: 1 }}>
          {LANGS.slice(0, 3).map((l) => (
            <Box
              key={l.tag}
              sx={{ display: 'flex', alignItems: 'center', gap: 1 }}
            >
              <FlagImage flagEmoji={l.flag} width={20} />
              <Typography variant="body2">{l.name}</Typography>
            </Box>
          ))}
        </Box>
        <Note>rectangular — `FlagImage`, ~25 places</Note>
      </Box>
      <Box>
        <Box sx={{ display: 'grid', gap: 1 }}>
          {LANGS.slice(0, 3).map((l) => (
            <Box
              key={l.tag}
              sx={{ display: 'flex', alignItems: 'center', gap: 1 }}
            >
              <Circled flag={l.flag} size={20} />
              <Typography variant="body2">{l.name}</Typography>
            </Box>
          ))}
        </Box>
        <Note>circular — `CircledLanguageIcon`, ~27 places</Note>
      </Box>
    </Box>
  ),
} satisfies Story;

/** Project languages table — circular, full label, base marked by a tick. */
export const InATable = {
  args: {},
  render: () => (
    <Box
      sx={{ width: 340, border: 1, borderColor: 'divider', borderRadius: 1 }}
    >
      {LANGS.map((l) => (
        <Box
          key={l.tag}
          sx={{
            display: 'flex',
            alignItems: 'center',
            gap: 1,
            px: 1.5,
            height: 44,
            borderBottom: 1,
            borderColor: 'divider',
            '&:last-of-type': { borderBottom: 0 },
          }}
        >
          <Circled flag={l.flag} size={20} />
          <Typography variant="body2" sx={{ flexGrow: 1 }}>
            {l.name !== l.original
              ? `${l.name} | ${l.original} (${l.tag})`
              : `${l.name} (${l.tag})`}
          </Typography>
          {l.base && <Typography variant="body2">✓</Typography>}
        </Box>
      ))}
    </Box>
  ),
} satisfies Story;

/** Language stats panel — circular, name above, tag and Base below. */
export const InAStatsPanel = {
  args: {},
  render: () => (
    <Box sx={{ width: 320 }}>
      {LANGS.map((l) => (
        <Box key={l.tag} sx={{ py: 1 }}>
          <Typography variant="body2">
            {l.name !== l.original ? `${l.name} | ${l.original}` : l.name}
          </Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 0.5 }}>
            <Circled flag={l.flag} size={18} />
            <Typography variant="caption" color="text.secondary">
              {l.tag}
            </Typography>
            {l.base && <Chip size="small" label="Base" />}
            <Box sx={{ flexGrow: 1, ml: 1 }}>
              <LinearProgress
                variant="determinate"
                value={l.base ? 100 : 35}
                sx={{ height: 6, borderRadius: 3 }}
              />
            </Box>
          </Box>
        </Box>
      ))}
    </Box>
  ),
} satisfies Story;

/** Translation cell — rectangular, name only, base in bold. */
export const InATranslationCell = {
  args: {},
  render: () => (
    <Box
      sx={{ width: 280, borderLeft: 3, borderColor: 'warning.light', pl: 1.5 }}
    >
      {LANGS.slice(0, 2).map((l) => (
        <Box key={l.tag} sx={{ py: 1 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <FlagImage flagEmoji={l.flag} height={16} />
            <Typography variant="body2" sx={{ fontWeight: l.base ? 700 : 400 }}>
              {l.name}
            </Typography>
          </Box>
          <Typography variant="body2" sx={{ mt: 0.5 }}>
            {l.base ? 'Share' : 'Sdílet'}
          </Typography>
        </Box>
      ))}
      <Note>rectangular, name only, base in bold — `LanguageHeading`</Note>
    </Box>
  ),
} satisfies Story;

/** A set of flags with no names — a project's languages at a glance. */
export const AsASet = {
  args: {},
  render: () => (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
        {[...LANGS, ...LANGS].map((l, i) => (
          <Circled key={i} flag={l.flag} size={20} />
        ))}
        <Box
          sx={{
            height: 20,
            px: 1,
            borderRadius: 10,
            bgcolor: 'action.hover',
            display: 'inline-flex',
            alignItems: 'center',
            typography: 'caption',
          }}
        >
          +5
        </Box>
      </Box>
      <Note>
        `CircledLanguageIconList` — round, no names, each with the full label in
        a tooltip
      </Note>
    </Box>
  ),
} satisfies Story;

/** Every label format in the product, on one language. */
export const EveryLabelFormat = {
  args: {},
  render: () => {
    const l = LANGS[1];
    const rows: [string, ReactNode, 'good' | 'bad' | 'muted'][] = [
      ['LanguageItem', `${l.name} | ${l.original} (${l.tag})`, 'good'],
      ['Find language', `${l.name} - ${l.original} - ${l.tag}`, 'bad'],
      ['Import', l.name, 'bad'],
      ['Translations filter', l.name, 'bad'],
      ['Stats panel', `${l.name} | ${l.original}`, 'bad'],
      ['Task filter trigger', '(flag only)', 'bad'],
    ];
    return (
      <Box sx={{ display: 'grid', gap: 0.75, width: 380 }}>
        {rows.map(([where, label, tone]) => (
          <Box
            key={where}
            sx={{ display: 'flex', alignItems: 'center', gap: 1 }}
          >
            <Typography
              variant="caption"
              sx={{ width: 150, color: 'text.secondary', flexShrink: 0 }}
            >
              {where}
            </Typography>
            <Circled flag={l.flag} size={18} />
            <Typography
              variant="body2"
              sx={{ color: tone === 'good' ? 'success.main' : 'text.primary' }}
            >
              {label}
            </Typography>
          </Box>
        ))}
      </Box>
    );
  },
} satisfies Story;

/** The same three views, decided: rectangular everywhere but the stack. */
export const Decided = {
  args: {},
  render: () => (
    <Box sx={{ display: 'grid', gap: 3 }}>
      <Box>
        <Box
          sx={{
            width: 320,
            border: 1,
            borderColor: 'divider',
            borderRadius: 1,
          }}
        >
          {LANGS.map((l) => (
            <Box
              key={l.tag}
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1,
                px: 1.5,
                height: 40,
                borderBottom: 1,
                borderColor: 'divider',
                '&:last-of-type': { borderBottom: 0 },
              }}
            >
              <FlagImage flagEmoji={l.flag} width={20} />
              <Typography variant="body2" sx={{ flexGrow: 1 }}>
                {l.name !== l.original
                  ? `${l.name} | ${l.original} (${l.tag})`
                  : `${l.name} (${l.tag})`}
              </Typography>
              {l.base && <Chip size="small" label="Base" />}
            </Box>
          ))}
        </Box>
        <Note tone="good">a table — rectangular</Note>
      </Box>

      <Box>
        <Box sx={{ width: 320 }}>
          {LANGS.slice(0, 2).map((l) => (
            <Box
              key={l.tag}
              sx={{ display: 'flex', alignItems: 'center', gap: 1, py: 0.75 }}
            >
              <FlagImage flagEmoji={l.flag} width={20} />
              <Typography variant="body2" sx={{ width: 110 }}>
                {l.name} ({l.tag})
              </Typography>
              <Box sx={{ flexGrow: 1 }}>
                <LinearProgress
                  variant="determinate"
                  value={l.base ? 100 : 35}
                  sx={{ height: 6, borderRadius: 3 }}
                />
              </Box>
            </Box>
          ))}
        </Box>
        <Note tone="good">
          a stats row — rectangular, and the tag no longer needs its own line
        </Note>
      </Box>

      <Box>
        <Box sx={{ display: 'flex', alignItems: 'center' }}>
          {LANGS.map((l, i) => (
            <Box
              key={l.tag}
              sx={{ ml: i ? '-6px' : 0, zIndex: LANGS.length - i }}
            >
              <Circled flag={l.flag} size={20} />
            </Box>
          ))}
        </Box>
        <Note>a stack — the one case that stays round</Note>
      </Box>
    </Box>
  ),
} satisfies Story;

/** The proposal: one component, three densities, one shape. */
export const Proposed = {
  args: {},
  render: () => {
    const l = LANGS[1];
    return (
      <Box sx={{ display: 'grid', gap: 2, width: 380 }}>
        {[
          ['full', `${l.name} | ${l.original} (${l.tag})`],
          ['short', `${l.name} (${l.tag})`],
          ['icon', ''],
        ].map(([variant, label]) => (
          <Box
            key={variant}
            sx={{ display: 'flex', alignItems: 'center', gap: 1 }}
          >
            <Typography
              variant="caption"
              sx={{ width: 60, color: 'text.secondary', flexShrink: 0 }}
            >
              {variant}
            </Typography>
            <Circled flag={l.flag} size={20} />
            {label && <Typography variant="body2">{label}</Typography>}
          </Box>
        ))}
      </Box>
    );
  },
} satisfies Story;
