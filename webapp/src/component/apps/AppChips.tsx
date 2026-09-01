import React from 'react';
import { Chip, styled, Tooltip, Typography } from '@mui/material';

const StyledRow = styled('div')`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(0.75)};
  min-width: 0;
`;

const StyledChips = styled('div')`
  display: flex;
  flex-wrap: wrap;
  gap: ${({ theme }) => theme.spacing(0.75)};
  min-width: 0;
`;

const StyledLabel = styled('span')(({ theme }) => ({
  ...theme.typography.caption,
  color: theme.palette.text.secondary,
  textDecoration: 'underline dotted',
  textUnderlineOffset: 3,
  cursor: 'help',
}));

const StyledChipLabel = styled('span')`
  display: inline-flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(0.5)};
`;

export type AppChipItem = {
  /** Stable identity within the row — used as the React key. */
  id: string;
  label: React.ReactNode;
};

type AppChipsProps = {
  items: (string | AppChipItem)[];
  dataCy: string;
  color?: 'default' | 'info' | 'warning';
  variant?: 'filled' | 'outlined';
  /** Short caption naming what the chips are. */
  label?: React.ReactNode;
  /** Explanation of the row, shown when hovering its caption. */
  tooltip?: React.ReactNode;
  /**
   * Shown in place of the chips when there are none. Without it, an empty row
   * renders nothing at all.
   */
  emptyLabel?: React.ReactNode;
};

const normalize = (item: string | AppChipItem): AppChipItem =>
  typeof item === 'string' ? { id: item, label: item } : item;

export const AppChips = ({
  items,
  dataCy,
  color,
  variant,
  label,
  tooltip,
  emptyLabel,
}: AppChipsProps) => {
  if (items.length === 0 && !emptyLabel) {
    return null;
  }

  const caption = label && (
    <Tooltip title={tooltip ?? ''} disableHoverListener={!tooltip}>
      <StyledLabel>{label}</StyledLabel>
    </Tooltip>
  );

  if (items.length === 0) {
    return (
      <StyledRow>
        {caption}
        <Typography variant="caption" color="text.secondary" data-cy={dataCy}>
          {emptyLabel}
        </Typography>
      </StyledRow>
    );
  }

  return (
    <StyledRow>
      {caption}
      <StyledChips data-cy={dataCy}>
        {items.map(normalize).map((item) => (
          <Chip
            key={item.id}
            size="small"
            color={color}
            variant={variant}
            label={<StyledChipLabel>{item.label}</StyledChipLabel>}
          />
        ))}
      </StyledChips>
    </StyledRow>
  );
};
