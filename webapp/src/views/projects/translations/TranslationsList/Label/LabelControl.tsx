import { styled } from '@mui/material';
import React, { useState, forwardRef, useEffect } from 'react';
import clsx from 'clsx';
import { AddLabel } from 'tg.views/projects/translations/TranslationsList/Label/Control/AddLabel';
import { stopBubble } from 'tg.fixtures/eventHandler';
import { LabelSelector } from 'tg.views/projects/translations/TranslationsList/Label/Control/LabelSelector';
import { components } from 'tg.service/apiSchema.generated';
import { useLabels } from 'tg.hooks/useLabels';

type LabelModel = components['schemas']['LabelModel'];

const StyledControl = styled('div')`
  display: flex;
  align-items: center;
  justify-content: center;
  color: ${({ theme }) => theme.palette.tokens.icon.secondary};
  border-radius: 12px;
  border: 1px solid ${({ theme }) => theme.palette.tokens.icon.secondary};
  height: 24px;
  font-size: 14px;

  &.shrank {
    width: 24px;
  }

  &:hover,
  &:focus-within {
    cursor: pointer;
    border: 1px solid ${({ theme }) => theme.palette.primary.main};
    color: ${({ theme }) => theme.palette.primary.main};
  }
`;

type LabelControlProps = {
  className?: string;
  existing?: LabelModel[];
  onSelect?: (labelId: number) => void;
  onSelectLabel?: (labelModel: LabelModel) => void;
  onSelectModeChange?: (selectMode: boolean) => void;
};

export const LabelControl = forwardRef<HTMLDivElement, LabelControlProps>(
  (props, ref) => {
    const { className, existing, onSelect, onSelectLabel, onSelectModeChange } =
      props;
    const [selectMode, setSelectMode] = useState<boolean>(false);
    const { labels: availableLabels } = useLabels({});

    useEffect(() => {
      onSelectModeChange?.(selectMode);
    }, [selectMode]);

    // Only render the component if available labels exist
    if (!availableLabels || availableLabels.length === 0) {
      return null;
    }

    const enterSelectMode = () => {
      setSelectMode(true);
      stopBubble();
    };
    const exitSelectMode = () => {
      setSelectMode(false);
      stopBubble();
    };

    const extendedAddLabelControl = existing?.length === 0;

    return (
      <StyledControl
        ref={ref}
        className={clsx(
          !selectMode && 'hover',
          !selectMode && !extendedAddLabelControl && 'shrank',
          className
        )}
        data-cy="translation-label-control"
      >
        {selectMode ? (
          <LabelSelector
            onClose={stopBubble(exitSelectMode)}
            onSelect={(labelId: number) => {
              onSelect?.(labelId);
              onSelectLabel?.(
                availableLabels.find(
                  (label) => label.id === labelId
                ) as LabelModel
              );
            }}
            existing={existing}
          />
        ) : (
          <AddLabel
            onClick={stopBubble(enterSelectMode)}
            showText={extendedAddLabelControl}
            className="clickable"
            data-cy="translation-label-add"
          />
        )}
      </StyledControl>
    );
  }
);

LabelControl.displayName = 'LabelControl'; // Optional but useful for debugging
