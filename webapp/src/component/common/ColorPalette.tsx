import { styled } from '@mui/material';
import React from 'react';
import clsx from 'clsx';

const PaletteGrid = styled('div')`
  display: grid;
  grid-template-columns: repeat(5, 32px);
  gap: 8px;
  padding: 12px;
`;

const PaletteColor = styled('div')`
  width: 32px;
  height: 32px;
  border-radius: 4px;
  border: 2px solid ${({ theme }) => theme.palette.tokens.primary.contrast};
  cursor: pointer;
  box-shadow: 0 0 2px #aaa;
  &.selected {
    border: 2px solid ${({ theme }) => theme.palette.tokens.icon.primary};
  }
`;

type ColorPaletteProps = {
  palette: Map<string, string>;
  selectedColor: string | null;
  onColorClick: (key: string) => void;
};

export const ColorPalette = ({
  palette,
  selectedColor,
  onColorClick,
}: ColorPaletteProps) => {
  return (
    <PaletteGrid data-cy="color-palette-popover">
      {Array.from(palette.entries()).map(([key, color]) => (
        <PaletteColor
          key={key}
          color={color}
          className={clsx(selectedColor === color && 'selected')}
          onClick={() => onColorClick(key)}
          data-cy="palette-color"
          style={{ backgroundColor: color }}
        />
      ))}
    </PaletteGrid>
  );
};
