import { styled } from '@mui/material';
import React from 'react';

const PaletteGrid = styled('div')`
  display: grid;
  grid-template-columns: repeat(5, 32px);
  gap: 8px;
  padding: 12px;
`;

const PaletteColor = styled('div')<{ color: string; selected: boolean }>`
  width: 32px;
  height: 32px;
  border-radius: 4px;
  background: ${({ color }) => color};
  border: 2px solid
    ${({ selected, theme }) =>
      selected
        ? theme.palette.tokens.icon.primary
        : theme.palette.tokens.primary.contrast};
  cursor: pointer;
  box-shadow: 0 0 2px #aaa;
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
          selected={color === selectedColor}
          onClick={() => onColorClick(key)}
          data-cy="palette-color"
        />
      ))}
    </PaletteGrid>
  );
};
