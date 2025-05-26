import { components } from 'tg.service/apiSchema.generated';
import { IconButton, styled } from '@mui/material';
import React from 'react';
import { Edit01, XClose } from '@untitled-ui/icons-react';
import { TranslationLabel } from 'tg.component/TranslationLabel';

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
      <StyledItemText data-cy="project-settings-label-item-name">
        <TranslationLabel color={label.color}>{label.name}</TranslationLabel>
      </StyledItemText>
      <StyledItemText data-cy="project-settings-label-item-description">
        {label.description && (
          <span style={{ fontSize: '0.8em', color: '#888' }}>
            {label.description}
          </span>
        )}
      </StyledItemText>
      <StyledItemText
        data-cy="project-settings-label-item-color"
        color={label.color}
      >
        {label.color}
      </StyledItemText>
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
