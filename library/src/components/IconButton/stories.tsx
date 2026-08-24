import type { ReactNode } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { Box, IconButton, Tooltip } from '@mui/material';
import { DotsVertical, Edit02, X, XClose } from '../../icons';

const meta = {
  title: 'Components/Buttons/IconButton',
  component: IconButton,
  parameters: {
    layout: 'centered',
  },
} satisfies Meta<typeof IconButton>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Named = {
  args: {},
  render: () => (
    <Tooltip title="Close">
      <IconButton size="small">
        <XClose width={20} height={20} />
      </IconButton>
    </Tooltip>
  ),
} satisfies Story;

export const Sizes = {
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
      <IconButton size="small" aria-label="Edit, small">
        <Edit02 width={20} height={20} />
      </IconButton>
      <IconButton aria-label="Edit, default">
        <Edit02 width={24} height={24} />
      </IconButton>
    </Box>
  ),
} satisfies Story;

export const Colors = {
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
      <IconButton size="small" aria-label="Neutral">
        <DotsVertical width={20} height={20} />
      </IconButton>
      <IconButton size="small" color="primary" aria-label="Primary">
        <DotsVertical width={20} height={20} />
      </IconButton>
      <IconButton size="small" color="error" aria-label="Error">
        <DotsVertical width={20} height={20} />
      </IconButton>
    </Box>
  ),
} satisfies Story;

export const Disabled = {
  args: {},
  render: () => (
    <IconButton size="small" disabled aria-label="Edit, unavailable">
      <Edit02 width={20} height={20} />
    </IconButton>
  ),
} satisfies Story;

const Sample = ({
  label,
  children,
}: {
  label: string;
  children: ReactNode;
}) => (
  <Box sx={{ textAlign: 'center' }}>
    {children}
    <Box sx={{ typography: 'caption', color: 'text.secondary', mt: 0.5 }}>
      {label}
    </Box>
  </Box>
);

/** The finding is that these two are indistinguishable — only one of them can be read out. */
export const FindingNoName = {
  tags: ['!dev'],
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 4, alignItems: 'flex-start' }}>
      <Sample label="no name">
        <IconButton size="small">
          <Edit02 width={20} height={20} />
        </IconButton>
      </Sample>
      <Sample label='aria-label="Edit key"'>
        <IconButton size="small" aria-label="Edit key">
          <Edit02 width={20} height={20} />
        </IconButton>
      </Sample>
    </Box>
  ),
} satisfies Story;

/** Four hand-written squares, ten call sites. */
export const FindingHandSizes = {
  tags: ['!dev'],
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 3, alignItems: 'flex-end' }}>
      {[40, 38, 36, 20].map((px) => (
        <Sample key={px} label={`${px} × ${px}`}>
          <IconButton
            size="small"
            aria-label={`${px} square`}
            sx={{
              height: px,
              width: px,
              border: (theme) => `1px dashed ${theme.palette.divider}`,
            }}
          >
            <DotsVertical width={Math.min(20, px)} height={Math.min(20, px)} />
          </IconButton>
        </Sample>
      ))}
    </Box>
  ),
} satisfies Story;

/** Two glyphs, one meaning. */
export const FindingTwoCloseIcons = {
  tags: ['!dev'],
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 4 }}>
      <Sample label="XClose · 36 uses">
        <IconButton size="small" aria-label="Close">
          <XClose width={20} height={20} />
        </IconButton>
      </Sample>
      <Sample label="X · 8 uses">
        <IconButton size="small" aria-label="Close">
          <X width={20} height={20} />
        </IconButton>
      </Sample>
    </Box>
  ),
} satisfies Story;

/** The scale is 16, 20, 24. These are what the product actually renders. */
export const FindingGlyphSizes = {
  tags: ['!dev'],
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 2, alignItems: 'flex-end' }}>
      {[14, 16, 18, 19, 20, 26].map((px) => (
        <Sample key={px} label={`${px}px`}>
          <IconButton size="small" aria-label={`${px} pixels`}>
            <Edit02 width={px} height={px} />
          </IconButton>
        </Sample>
      ))}
    </Box>
  ),
} satisfies Story;

/** Two are documented, two are not. */
export const FindingSizes = {
  tags: ['!dev'],
  args: {},
  render: () => (
    <Box sx={{ display: 'flex', gap: 3, alignItems: 'flex-end' }}>
      {(['small', 'medium', 'large'] as const).map((size) => (
        <Sample key={size} label={size}>
          <IconButton size={size} aria-label={size}>
            <DotsVertical width={20} height={20} />
          </IconButton>
        </Sample>
      ))}
    </Box>
  ),
} satisfies Story;
