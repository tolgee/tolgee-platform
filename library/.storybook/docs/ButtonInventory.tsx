import type { ComponentType, SVGProps } from 'react';
import { Box, Button, Tooltip, type ButtonProps } from '@mui/material';
import {
  ArrowUpRight,
  Check,
  CheckDone01,
  Key02,
  Plus,
  Share03,
  ShoppingCart01,
} from '../../src/icons';
import { CheckDone, Stars } from '../../src/icons/custom';
import { DocsTheme } from './DocsTheme';
import { BUTTON_USAGES, type ButtonUsage } from './buttonUsages';
import { RESTYLED_BUTTONS, type Decl } from './restyledButtons';
import type { Theme } from '@mui/material/styles';

const camel = (prop: string) =>
  prop.replace(/-([a-z])/g, (_, c: string) => c.toUpperCase());

const dig = (obj: unknown, path: string) =>
  path
    .split('.')
    .reduce<unknown>(
      (a, k) => (a as Record<string, unknown>)?.[k],
      obj,
    ) as string;

/** Rebuilds the styled block so the sample is the real thing, not an impression of it. */
const declsToSx = (decls: Decl[]) => (theme: Theme) =>
  Object.fromEntries(
    decls.map((d) => [
      camel(d.prop),
      d.palette
        ? [d.prefix, dig(theme.palette, d.palette)].filter(Boolean).join(' ')
        : d.spacing
          ? theme.spacing(...(d.spacing as [number]))
          : d.value,
    ]),
  );

/** Files that redefine shape, color or height — not the ones that only nudge padding. */
const OFF_SPEC_FILES = new Set(
  RESTYLED_BUTTONS.filter((r) => r.structural.length > 0).map((r) => r.file),
);

const ICONS: Record<string, ComponentType<SVGProps<SVGSVGElement>>> = {
  ArrowUpRight,
  Check,
  CheckDone,
  CheckDone01,
  Key02,
  Plus,
  Share03,
  ShoppingCart01,
  Stars,
};

const icon = (name: string | null) => {
  const Icon = name ? ICONS[name] : undefined;
  return Icon ? <Icon width={20} height={20} /> : undefined;
};

/** MUI's own default is `medium` and the theme does not change it, so the two are one button. */
const sizeOf = (u: ButtonUsage) => (u.size === 'medium' ? null : u.size);

const colorProp = (color: string | null) =>
  (color as ButtonProps['color']) ?? undefined;

const VARIANTS = ['contained', 'outlined', 'text'] as const;
const COLORS = [
  'primary',
  'secondary',
  'error',
  'success',
  'info',
  'inherit',
  'default',
  null,
];
const SIZE_COLUMNS: (string | null)[] = [null, 'small', 'large'];

const Chip = ({
  children,
  marked,
  title,
}: {
  children: string;
  marked?: boolean;
  title?: string;
}) => {
  const chip = (
    <Box
      component="span"
      sx={{
        px: 0.75,
        py: 0.25,
        borderRadius: 0.5,
        typography: 'caption',
        whiteSpace: 'nowrap',
        cursor: title ? 'help' : undefined,
        color: marked ? 'text.primary' : 'text.secondary',
        border: (theme) =>
          `1px solid ${marked ? theme.palette.error.main : theme.palette.divider}`,
      }}
    >
      {children}
    </Box>
  );
  return title ? (
    <Tooltip title={title} placement="top">
      {chip}
    </Tooltip>
  ) : (
    chip
  );
};

/** Reference, not a finding: every combination the product uses, grouped by size. */
export const ButtonSummary = () => {
  const count = (variant: string, color: string | null, size: string | null) =>
    BUTTON_USAGES.filter(
      (u) => u.variant === variant && u.color === color && sizeOf(u) === size,
    ).reduce((n, u) => n + u.count, 0);

  const groups = SIZE_COLUMNS.map((size) => ({
    size,
    rows: VARIANTS.flatMap((variant) =>
      COLORS.map((color) => ({
        variant,
        color,
        n: count(variant, color, size),
      })),
    ).filter((r) => r.n > 0),
  })).filter((g) => g.rows.length > 0);

  const height = (size: string | null) =>
    size === null ? '40px' : size === 'small' ? '30px' : 'not on the scale';

  return (
    <DocsTheme>
      <Box sx={{ my: 2, color: 'text.primary' }}>
        {groups.map((g) => (
          <Box key={g.size ?? 'default'} sx={{ mb: 4 }}>
            <Box
              sx={{
                display: 'flex',
                alignItems: 'baseline',
                gap: 1,
                pb: 0.5,
                mb: 1,
                borderBottom: (theme) => `2px solid ${theme.palette.divider}`,
              }}
            >
              <Box sx={{ typography: 'subtitle2' }}>
                {g.size === null ? 'Default size' : `Size ${g.size}`}
              </Box>
              <Box sx={{ typography: 'caption', color: 'text.secondary' }}>
                {height(g.size)} · {g.rows.reduce((a, b) => a + b.n, 0)} buttons
              </Box>
            </Box>
            {g.rows.map((r) => (
              <Box
                key={`${r.variant}-${r.color}`}
                sx={{
                  display: 'grid',
                  gridTemplateColumns: '140px 1fr 60px',
                  alignItems: 'center',
                  gap: 2,
                  py: 0.75,
                  borderBottom: (theme) => `1px solid ${theme.palette.divider}`,
                }}
              >
                <Button
                  variant={r.variant}
                  color={colorProp(r.color)}
                  size={(g.size as ButtonProps['size']) ?? undefined}
                  disableRipple
                  sx={{ justifySelf: 'start', pointerEvents: 'none' }}
                >
                  Button
                </Button>
                <Box sx={{ fontFamily: 'monospace', typography: 'caption' }}>
                  {`${r.variant} · ${r.color ?? 'no color'}`}
                </Box>
                <Box
                  sx={{
                    typography: 'body2',
                    textAlign: 'right',
                    fontVariantNumeric: 'tabular-nums',
                  }}
                >
                  {r.n}
                </Box>
              </Box>
            ))}
          </Box>
        ))}
      </Box>
    </DocsTheme>
  );
};

type Case = {
  title: string;
  why: string;
  fix: string;
  usages: ButtonUsage[];
};

const specialCases = (): Case[] => {
  const has = (fn: (u: ButtonUsage) => boolean) => BUTTON_USAGES.filter(fn);
  return [
    {
      title: 'Drawn by a file that redefines the button',
      fix: 'Read it against the component listed under Hardcoded before trusting the sample.',
      why: 'The sample here is a guess. These files also declare a button of their own, so what ships may look nothing like it.',
      usages: has((u) => OFF_SPEC_FILES.has(u.file)),
    },
    {
      title: 'A size that is not on the scale',
      fix: 'Drop the prop and let the error page layout carry the emphasis.',
      why: 'The theme defines 40px and 30px. `large` is neither, and it appears once.',
      usages: has((u) => u.size === 'large'),
    },
    {
      title: 'Filled, but no color named',
      fix: 'Name primary where it really commits, make the rest outlined.',
      why: 'A contained button with no color falls back to neutral, so it is filled without claiming to be the action that commits. Either it is that action and should say so, or it should not be filled.',
      usages: has((u) => u.variant === 'contained' && u.color === null),
    },
    {
      title: 'Filled and severe at once',
      fix: 'Make them outlined and let the confirmation dialog carry the weight.',
      why: 'Contained claims primary; error and info claim a state. Together they belong on a confirmation, not in a list of settings.',
      usages: has(
        (u) =>
          u.variant === 'contained' &&
          (u.color === 'error' || u.color === 'info'),
      ),
    },
    {
      title: 'Nothing to announce',
      fix: 'Give each one an aria-label naming the action.',
      why: 'No text, no data-cy, no icon title — a screen reader reads out an empty button.',
      usages: has((u) => u.label === '(icon only)'),
    },
    {
      title: 'Asking for the default',
      fix: 'Delete the size prop.',
      why: '`size="medium"` is what a button already is. Harmless, but it reads as a decision that was never made.',
      usages: has((u) => u.size === 'medium'),
    },
  ].filter((c) => c.usages.length > 0);
};

const CaseBlock = ({ item }: { item: Case }) => (
  <Box
    sx={{
      py: 2,
      borderBottom: (theme) => `1px solid ${theme.palette.divider}`,
    }}
  >
    <Box sx={{ display: 'flex', alignItems: 'baseline', gap: 1, mb: 0.5 }}>
      <Box sx={{ typography: 'subtitle2' }}>{item.title}</Box>
      <Box sx={{ typography: 'caption', color: 'text.secondary' }}>
        {item.usages.reduce((n, u) => n + u.count, 0)}
      </Box>
    </Box>
    <Box sx={{ typography: 'body2', color: 'text.secondary', mb: 1 }}>
      {item.why}
    </Box>
    <Box
      sx={{
        mb: 1.5,
        px: 1.5,
        py: 1,
        borderRadius: 1,
        typography: 'body2',
        background: (theme) => theme.palette.background.paper,
        border: (theme) => `1px solid ${theme.palette.info.main}`,
        borderLeftWidth: 4,
      }}
    >
      <strong>Suggested fix.</strong> {item.fix}
    </Box>
    {item.usages.map((u, i) => (
      <Box
        key={`${u.file}-${u.label}-${i}`}
        sx={{
          display: 'grid',
          gridTemplateColumns: 'max-content minmax(130px, max-content) 1fr',
          gap: 2,
          alignItems: 'center',
          py: 0.5,
        }}
      >
        <Button
          variant={u.variant}
          color={colorProp(u.color)}
          size={(sizeOf(u) as ButtonProps['size']) ?? undefined}
          startIcon={icon(u.startIcon)}
          endIcon={icon(u.endIcon)}
          disableRipple
          sx={{ pointerEvents: 'none' }}
        >
          {u.label === '(icon only)' ? ' ' : u.label}
        </Button>
        <Box sx={{ typography: 'caption', color: 'text.secondary' }}>
          {u.area}
        </Box>
        <Box
          sx={{
            typography: 'caption',
            fontFamily: 'monospace',
            color: 'text.secondary',
          }}
        >
          {u.file}
        </Box>
      </Box>
    ))}
  </Box>
);

export const ButtonInventory = () => {
  const cases = specialCases();
  const offSpec = RESTYLED_BUTTONS.filter((r) => r.structural.length > 0);
  const nudges = RESTYLED_BUTTONS.filter((r) => r.structural.length === 0);
  const open = cases.reduce((n, c) => n + c.usages.length, 0) + offSpec.length;

  return (
    <DocsTheme>
      <Box sx={{ my: 2, color: 'text.primary' }}>
        <Box sx={{ typography: 'h6', mb: 2 }}>{open} open</Box>
        {cases.map((c) => (
          <CaseBlock key={c.title} item={c} />
        ))}

        <Box sx={{ py: 2 }}>
          <Box
            sx={{ display: 'flex', alignItems: 'baseline', gap: 1, mb: 0.5 }}
          >
            <Box sx={{ typography: 'subtitle2' }}>
              Hardcoded — its own button, not the theme&apos;s
            </Box>
            <Box sx={{ typography: 'caption', color: 'text.secondary' }}>
              {offSpec.length}
            </Box>
          </Box>
          <Box sx={{ typography: 'body2', color: 'text.secondary', mb: 1 }}>
            Each paints over shape, color or height under its own name, so it
            never shows up as a <code>Button</code> anywhere.
          </Box>
          <Box
            sx={{
              mb: 1.5,
              px: 1.5,
              py: 1,
              borderRadius: 1,
              typography: 'body2',
              background: (theme) => theme.palette.background.paper,
              border: (theme) => `1px solid ${theme.palette.info.main}`,
              borderLeftWidth: 4,
            }}
          >
            <strong>Suggested fix.</strong> Two of these are not buttons at all
            — give the toggles and the chip their own named components, and take
            the rest back to the theme&apos;s own shape.
          </Box>
          {offSpec.map((r) => (
            <Box
              key={r.file + r.component}
              sx={{
                display: 'grid',
                gridTemplateColumns: '150px minmax(180px, max-content) 1fr',
                gap: 2,
                alignItems: 'center',
                py: 1,
                borderBottom: (theme) => `1px solid ${theme.palette.divider}`,
              }}
            >
              <Box>
                <Button
                  variant="outlined"
                  disableRipple
                  sx={declsToSx(r.decls)}
                >
                  {r.kind}
                </Button>
              </Box>
              <Box>
                <Box sx={{ typography: 'body2' }}>
                  {r.name} <em>— reads as a {r.kind.toLowerCase()}</em>
                </Box>
                <Box sx={{ typography: 'caption', color: 'text.secondary' }}>
                  {r.area} · used in {r.users}{' '}
                  {r.users === 1 ? 'place' : 'places'}
                </Box>
              </Box>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.75 }}>
                {r.structural.map((prop) => (
                  <Chip key={prop} marked title={r.file}>
                    {prop}
                  </Chip>
                ))}
              </Box>
            </Box>
          ))}
        </Box>

        <Box sx={{ mt: 3, typography: 'caption', color: 'text.secondary' }}>
          Not counted: {nudges.length} components adjust only margin, padding or{' '}
          <code>min-width</code> — {nudges.map((r) => r.name).join(', ')}.
        </Box>
      </Box>
    </DocsTheme>
  );
};
