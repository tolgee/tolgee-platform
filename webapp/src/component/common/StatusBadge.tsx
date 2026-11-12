import React from 'react';
import clsx from 'clsx';
import { styled } from '@mui/material';

export type StatusBadgeTone =
  | 'success'
  | 'warning'
  | 'error'
  | 'teal'
  | 'neutral';
export type StatusBadgeVariant = 'tinted' | 'outlined';

/**
 * A small standalone pill (22px, 13px text) that shows a status or a result: an AI score,
 * an issue severity, later a translation memory match.
 * Not the MUI Badge (the bubble on the corner of an icon, like QaBadge's count).
 *
 * Colours come from the Figma tokens, so light and dark mode follow the design system.
 * Hover and keyboard focus on a parent button darken the fill a little in the badge's own tone.
 */
const StyledBadge = styled('span')`
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  height: 22px;
  padding: 0 8px;
  border: 1px solid;
  border-radius: 9999px;
  font-size: 13px;
  font-weight: 500;
  line-height: 20px;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.2px;
  white-space: nowrap;

  & svg {
    width: 14px;
    height: 14px;
    flex-shrink: 0;
  }

  /* Tinted: a light fill with a border one step stronger. In dark mode the alert
     backgrounds are near black, so the fill is the tone's translucent state layer. */
  &.success {
    color: ${({ theme }) =>
      theme.palette.tokens._components.alert.success.color};
    background: ${({ theme }) =>
      theme.palette.mode === 'dark'
        ? theme.palette.tokens.success._states.selected
        : theme.palette.tokens._components.alert.success.background};
    border-color: ${({ theme }) =>
      theme.palette.tokens.success._states.focusVisible};
  }
  &.warning {
    color: ${({ theme }) =>
      theme.palette.tokens._components.alert.warning.color};
    background: ${({ theme }) =>
      theme.palette.mode === 'dark'
        ? theme.palette.tokens.warning._states.selected
        : theme.palette.tokens._components.alert.warning.background};
    border-color: ${({ theme }) =>
      theme.palette.tokens.warning._states.focusVisible};
  }
  /* Dark mode: the soft error fill and border tokens like the other tones; the text is Tolgee red 100 (enough contrast for 13px text),
     because the error alert text is nearly white there. */
  &.error {
    color: ${({ theme }) =>
      theme.palette.mode === 'dark'
        ? theme.palette.colors.red['100']
        : theme.palette.tokens._components.alert.error.color};
    background: ${({ theme }) =>
      theme.palette.mode === 'dark'
        ? theme.palette.tokens.error._states.selected
        : theme.palette.tokens._components.alert.error.background};
    border-color: ${({ theme }) =>
      theme.palette.tokens.error._states.focusVisible};
  }
  /* Teal is Tolgee's secondary colour, built the same way as the tones above. */
  &.teal {
    color: ${({ theme }) =>
      theme.palette.mode === 'dark'
        ? theme.palette.tokens.secondary.light
        : theme.palette.tokens._components.chip.placeHolderPluralText};
    background: ${({ theme }) =>
      theme.palette.tokens.secondary._states.selected};
    border-color: ${({ theme }) =>
      theme.palette.tokens.secondary._states.focusVisible};
  }
  &.neutral {
    color: ${({ theme }) => theme.palette.tokens.text.secondary};
    background: ${({ theme }) => theme.palette.tokens.text._states.selected};
    border-color: ${({ theme }) => theme.palette.tokens.border.secondary};
  }

  /* Outlined: no fill, a stronger border. Neutral uses the main text colour. */
  &.outlined {
    background: transparent;
  }
  &.outlined.success {
    border-color: ${({ theme }) =>
      theme.palette.tokens.success._states.outlinedBorder};
  }
  &.outlined.warning {
    border-color: ${({ theme }) =>
      theme.palette.tokens.warning._states.outlinedBorder};
  }
  &.outlined.error {
    border-color: ${({ theme }) =>
      theme.palette.tokens.error._states.outlinedBorder};
  }
  &.outlined.teal {
    border-color: ${({ theme }) =>
      theme.palette.tokens.secondary._states.outlinedBorder};
  }
  &.outlined.neutral {
    color: ${({ theme }) => theme.palette.tokens.text.primary};
    border-color: ${({ theme }) => theme.palette.tokens.border.primary};
  }

  /* Hover and keyboard focus on the button that holds the badge. */
  button:hover &,
  button.Mui-focusVisible & {
    background-image: linear-gradient(
      ${({ theme }) => theme.palette.tokens.text._states.focus},
      ${({ theme }) => theme.palette.tokens.text._states.focus}
    );
  }
`;

type Props = Omit<React.HTMLAttributes<HTMLSpanElement>, 'color'> & {
  tone: StatusBadgeTone;
  variant?: StatusBadgeVariant;
  icon?: React.ReactNode;
  children?: React.ReactNode;
};

export const StatusBadge = React.forwardRef<HTMLSpanElement, Props>(
  function StatusBadge(
    { tone, variant = 'tinted', icon, children, className, ...rest },
    ref
  ) {
    return (
      <StyledBadge
        ref={ref}
        className={clsx(tone, variant, className)}
        {...rest}
      >
        {icon}
        {children}
      </StyledBadge>
    );
  }
);
