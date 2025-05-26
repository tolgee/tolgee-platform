import { styled } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';
import { TranslationLabel } from 'tg.component/TranslationLabel';
import { LabelControl } from 'tg.views/projects/translations/TranslationsList/Label/LabelControl';
import React from 'react';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import clsx from 'clsx';
import { CELL_SHOW_ON_HOVER } from 'tg.views/projects/translations/cell/styles';

type LabelModel = components['schemas']['LabelModel'];

const StyledLabels = styled('div')`
  display: flex;
  grid-area: labels;
  align-items: center;
`;

const StyledList = styled('div')`
  display: flex;
  gap: 6px;
  & > div:last-child {
    margin-right: 6px;
  }
  .translation-label {
    cursor: default;
  }
`;

type Props = {
  labels: LabelModel[] | undefined;
  className: string;
  onSelect?: (labelId: number) => void;
  onDelete?: (labelId: number) => void;
};

export const TranslationLabels = ({
  labels,
  className,
  onSelect,
  onDelete,
}: Props) => {
  const { satisfiesPermission } = useProjectPermissions();
  const canAssignLabels = satisfiesPermission('translation-labels.assign');

  return (
    <StyledLabels className={className}>
      <StyledList className="translation-labels-list">
        {labels &&
          labels.map((label) => (
            <TranslationLabel
              label={label}
              key={label.id}
              tooltip={label.description}
              onClick={(e) => e.stopPropagation()}
              onDelete={
                onDelete && canAssignLabels
                  ? () => onDelete(label.id)
                  : undefined
              }
            />
          ))}
      </StyledList>
      {canAssignLabels && (
        <LabelControl
          className={clsx('clickable', CELL_SHOW_ON_HOVER)}
          onSelect={onSelect}
          existing={labels}
        />
      )}
    </StyledLabels>
  );
};
