import { components } from 'tg.service/apiSchema.generated';
import { IconButton, styled } from '@mui/material';
import React from 'react';
import { Edit01, XClose } from '@untitled-ui/icons-react';

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

const StyledListItem = styled('div')`
  display: contents;
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
  &:last-child {
    border-bottom: 0;
  }
  position: relative;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
`;

const StyledListItemColumn = styled('div')`
  margin: 5px 0;
`;

const StyledItemText = styled(StyledListItemColumn)<{ color?: string }>`
  flex-grow: 1;
  padding: ${({ theme }) => theme.spacing(1)};
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
  justify-content: flex-start;
  color: ${({ color }) => color || 'inherit'};
`;

const StyledItemActions = styled(StyledListItemColumn)`
  display: flex;
  gap: ${({ theme }) => theme.spacing(1)};
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  padding: 0;
`;

const StyledLabel = styled('div')<{ color: string }>`
  background-color: ${({ color }) => color || 'transparent'};
  border-radius: 8px;
  color: ${({ color }) => getShadeFromLabelColor(color)};
  padding: 2px 7px;
  font-size: 12px;
`;

type LabelModel = components['schemas']['LabelModel'];

type Props = {
  label: LabelModel;
  onLabelEdit: () => void;
  onLabelRemove: () => void;
};

export const LabelItem: React.FC<Props> = ({
  label,
  onLabelEdit,
  onLabelRemove,
}) => {
  return (
    <StyledListItem data-cy="project-settings-label-item">
      <StyledItemText>
        <StyledLabel color={label.color}>{label.name}</StyledLabel>
      </StyledItemText>
      <StyledItemText>
        {label.description && (
          <span style={{ fontSize: '0.8em', color: '#888' }}>
            {label.description}
          </span>
        )}
      </StyledItemText>
      <StyledItemText color={label.color}>{label.color}</StyledItemText>
      <StyledItemActions>
        <IconButton
          data-cy="project-settings-labels-edit-button"
          size="small"
          onClick={onLabelEdit}
        >
          <Edit01 width={20} height={20} />
        </IconButton>
        <IconButton
          data-cy="project-settings-labels-remove-button"
          size="small"
          onClick={onLabelRemove}
        >
          <XClose />
        </IconButton>
      </StyledItemActions>
    </StyledListItem>
  );
};
