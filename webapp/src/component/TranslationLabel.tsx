import { styled, Tooltip } from '@mui/material';
import React from 'react';

const StyledLabel = styled('div')<{ color?: string }>`
  background-color: ${({ color }) => color || 'transparent'};
  border-radius: 8px;
  color: ${({ color, theme }) =>
    color ? getShadeFromLabelColor(color) : theme.palette.tooltip.text};
  padding: 2px 6px;
  font-size: 11px;
  font-weight: 600;
`;

function adjustColorBrightness(hex: string, amount: number): string {
  let color = hex.replace('#', '');
  if (color.length === 3) {
    color = color
      .split('')
      .map((c) => c + c)
      .join('');
  }
  const num = parseInt(color, 16);
  let r = (num >> 16) + amount;
  let g = ((num >> 8) & 0x00ff) + amount;
  let b = (num & 0x0000ff) + amount;

  r = Math.max(Math.min(255, r), 0);
  g = Math.max(Math.min(255, g), 0);
  b = Math.max(Math.min(255, b), 0);

  return `#${((r << 16) | (g << 8) | b).toString(16).padStart(6, '0')}`;
}

function getShadeFromLabelColor(color: string): string {
  const hex = color.replace('#', '');
  const r = parseInt(hex.substring(0, 2), 16);
  const g = parseInt(hex.substring(2, 4), 16);
  const b = parseInt(hex.substring(4, 6), 16);
  const brightness = (r * 299 + g * 587 + b * 114) / 1000;
  return brightness > 128
    ? adjustColorBrightness(color, -120)
    : adjustColorBrightness(color, 120);
}

export const TranslationLabel: React.FC<{
  color?: string;
  tooltip?: string;
  children: React.ReactNode;
  className?: string;
}> = ({ children, tooltip, color, className }) => {
  const content = (
    <StyledLabel
      data-cy="translation-label"
      color={color}
      className={className}
    >
      {children}
    </StyledLabel>
  );
  return tooltip ? (
    <Tooltip disableInteractive title={tooltip}>
      {content}
    </Tooltip>
  ) : (
    content
  );
};
