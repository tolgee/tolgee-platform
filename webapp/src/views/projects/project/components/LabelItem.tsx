import { components } from 'tg.service/apiSchema.generated';
import { Box, styled } from '@mui/material';
import React from 'react';
import { TABLE_LAST_CELL } from 'tg.views/projects/languages/tableStyles';
import { SettingsIconButton } from 'tg.component/common/buttons/SettingsIconButton';

function shadeColor(color: string, percent: number) {
  let c = color.replace('#', '');
  if (c.length === 3)
    c = c
      .split('')
      .map((x) => x + x)
      .join('');
  const num = parseInt(c, 16);
  let r = (num >> 16) + percent;
  let g = ((num >> 8) & 0x00ff) + percent;
  let b = (num & 0x0000ff) + percent;
  r = Math.max(Math.min(255, r), 0);
  g = Math.max(Math.min(255, g), 0);
  b = Math.max(Math.min(255, b), 0);
  return `#${((r << 16) | (g << 8) | b).toString(16).padStart(6, '0')}`;
}

const StyledListItem = styled('div')`
  display: flex;
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
  &:last-child {
    border-bottom: 0;
  }
  position: relative;
  padding: ${({ theme }) => theme.spacing(1)};
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
`;

const StyledItemText = styled('div')<{ color?: string }>`
  flex-grow: 1;
  padding: ${({ theme }) => theme.spacing(1)};
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
  color: ${({ color }) => color || 'inherit'};
`;

const StyledLabel = styled('div')<{ color: string }>`
  background-color: ${({ color }) => color || 'transparent'};
  border-radius: 4px;
  color: ${({ color }) => shadeColor(color, 150)};
  padding: ${({ theme }) => theme.spacing(0.2)} 5px;
  font-size: 14px;
`;

const StyledItemActions = styled('div')`
  display: flex;
  gap: ${({ theme }) => theme.spacing(1)};
  align-items: center;
  flex-wrap: wrap;
  padding: 0;
`;

type LabelModel = components['schemas']['LabelModel'];

type Props = {
  label: LabelModel;
  onLabelEdit: () => void;
};

export const LabelItem: React.FC<Props> = ({ label, onLabelEdit }) => {
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
        <Box
          className={TABLE_LAST_CELL}
          mt={1}
          mb={1}
          data-cy="project-settings-languages-list-edit-button"
          onClick={onLabelEdit}
        >
          <SettingsIconButton size="small" />
        </Box>
      </StyledItemActions>
    </StyledListItem>
  );
};
