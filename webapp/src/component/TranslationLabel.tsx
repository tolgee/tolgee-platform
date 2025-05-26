import { styled, Theme, Tooltip } from '@mui/material';
import React from 'react';
import { components } from 'tg.service/apiSchema.generated';
import clsx from 'clsx';
import { CloseButton } from 'tg.component/common/buttons/CloseButton';

type LabelModel = components['schemas']['LabelModel'];

const DARK_MODE_OPACITY = 0.85;
const CONTRAST = 150;

export const StyledTranslationLabel = styled('div')<{ color?: string }>`
  background-color: ${({ color, theme }) => getBackgroundColor(theme, color)};
  border-radius: 12px;
  color: ${({ color, theme }) => getTextColor(theme, color)};
  padding: 0 8px;
  font-size: 14px;
  line-height: 18px;
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  overflow: hidden;
  white-space: nowrap;
  height: 24px;
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

function fixLightTextColor(textColor: string) {
  const hex = textColor.replace('#', '');
  let r = parseInt(hex.substring(0, 2), 16);
  let g = parseInt(hex.substring(2, 4), 16);
  let b = parseInt(hex.substring(4, 6), 16);

  const MIN_VALUE = 0xe1; // 225

  r = Math.max(r, MIN_VALUE);
  g = Math.max(g, MIN_VALUE);
  b = Math.max(b, MIN_VALUE);

  return `#${r.toString(16).padStart(2, '0')}${g
    .toString(16)
    .padStart(2, '0')}${b.toString(16).padStart(2, '0')}`.toUpperCase();
}

function hexToRgb(hex: string) {
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  return { r, g, b };
}

function getBackgroundColor(theme: Theme, hex?: string) {
  if (!hex) {
    return 'transparent';
  }
  if (theme.palette.mode === 'dark') {
    const rgb = hexToRgb(hex);
    return `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, ${DARK_MODE_OPACITY})`;
  }
  return hex;
}

function getTextColor(theme: Theme, color?: string): string {
  if (!color) {
    return theme.palette.tooltip.text;
  }
  const hex = color.replace('#', '');
  const r = parseInt(hex.substring(0, 2), 16);
  const g = parseInt(hex.substring(2, 4), 16);
  const b = parseInt(hex.substring(4, 6), 16);
  const brightness = (r * 299 + g * 587 + b * 114) / 1000;
  let adjustedColor = adjustColorBrightness(
    color,
    brightness > 128 ? -CONTRAST : CONTRAST
  );
  if (brightness < 128) {
    adjustedColor = fixLightTextColor(adjustedColor);

    const textHex = adjustedColor.replace('#', '');
    const textR = parseInt(textHex.substring(0, 2), 16);
    const textG = parseInt(textHex.substring(2, 4), 16);
    const textB = parseInt(textHex.substring(4, 6), 16);

    if (g > r && g > b && textB > textG + 50 && textB > 200) {
      return '#ffffff';
    }

    if (b > r && b > g && textR > textB + 50 && textR > 200) {
      return '#ffffff';
    }

    if (brightness < 100) {
      return '#ffffff';
    }
  }
  return adjustedColor;
}

export const TranslationLabel: React.FC<{
  label: LabelModel;
  tooltip?: string;
  children?: React.ReactNode;
  className?: string;
  onClick?: (e: React.MouseEvent<HTMLDivElement>) => void;
  onDelete?: (labelId: number) => void;
}> = ({ label, children, tooltip, className, onClick, onDelete, ...rest }) => {
  const content = (
    <CloseButton
      data-cy="translation-label-delete"
      onClose={
        onDelete
          ? (e) => {
              e.stopPropagation();
              onDelete?.(label.id);
            }
          : undefined
      }
      xs
    >
      <StyledTranslationLabel
        color={label.color}
        className={clsx(className, 'translation-label')}
        data-cy="translation-label"
        {...rest}
        translation-label-delete
      >
        <div className="translation-label-content" onClick={onClick}>
          {children || label.name}
        </div>
      </StyledTranslationLabel>
    </CloseButton>
  );
  return tooltip ? (
    <Tooltip disableInteractive title={tooltip}>
      {content}
    </Tooltip>
  ) : (
    content
  );
};
