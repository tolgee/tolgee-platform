import { styled } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';
import { TranslationLabel } from 'tg.component/TranslationLabel';
import { LabelControl } from 'tg.views/projects/translations/TranslationsList/Label/LabelControl';
import React from 'react';
import { XClose } from '@untitled-ui/icons-react';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';

type LabelModel = components['schemas']['LabelModel'];

const StyledLabels = styled('div')`
  display: flex;
  grid-area: labels;
  gap: 8px;
  align-items: center;

  .translation-label {
    display: inline-flex;
    align-items: center;
    transition: max-width 0.2s ease-in-out;
    max-width: 100%;
    overflow: hidden;
    white-space: nowrap;

    &:hover {
      max-width: calc(100% + 16px);

      svg.close-button {
        width: 16px;
        margin-left: 4px;
      }
    }

    svg.close-button {
      width: 0;
      transition: width 0.2s;
      overflow: hidden;
    }
  }
`;

const StyledList = styled('div')`
  display: flex;
  gap: 8px;
`;

const StyledClose = styled(XClose)`
  width: 0;
  cursor: pointer;
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
      <StyledList>
        {labels &&
          labels.map((label) => (
            <TranslationLabel
              className="translation-label"
              data-cy="translation-label"
              color={label.color}
              key={label.id}
              tooltip={label.description}
            >
              <div
                className="translation-label-content"
                onClick={(e) => e.stopPropagation()}
              >
                {label.name}
              </div>
              {onDelete && canAssignLabels && (
                <StyledClose
                  width={16}
                  height={16}
                  className={'close-button'}
                  data-cy={`translation-label-delete`}
                  onClick={(e) => {
                    e.stopPropagation();
                    onDelete(label.id);
                  }}
                />
              )}
            </TranslationLabel>
          ))}
      </StyledList>
      {canAssignLabels && (
        <LabelControl
          className="clickable"
          onSelect={onSelect}
          existing={labels}
        />
      )}
    </StyledLabels>
  );
};
