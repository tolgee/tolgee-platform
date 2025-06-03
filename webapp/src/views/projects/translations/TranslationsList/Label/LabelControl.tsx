import { styled } from '@mui/material';
import React, { useState } from 'react';
import clsx from 'clsx';
import { AddLabel } from 'tg.views/projects/translations/TranslationsList/Label/AddLabel';
import { CELL_SHOW_ON_HOVER } from 'tg.views/projects/translations/cell/styles';
import { StyledTranslationLabel } from 'tg.component/TranslationLabel';
import { stopBubble } from 'tg.fixtures/eventHandler';
import { LabelSelector } from 'tg.views/projects/translations/TranslationsList/Label/LabelSelector';
import { components } from 'tg.service/apiSchema.generated';

type LabelModel = components['schemas']['LabelModel'];

const StyledControl = styled(StyledTranslationLabel)`
  background-color: ${({ theme }) => theme.palette.tooltip.background};
  border: 1px solid ${({ theme }) => theme.palette.primary.main};
  color: ${({ theme }) => theme.palette.text.primary};

  &.hover:hover {
    cursor: pointer;
  }
`;

export const LabelControl: React.FC<{
  className?: string;
  existing?: LabelModel[];
  onSelect?: (labelId: number) => void;
}> = ({ className, onSelect, existing }) => {
  const [selectMode, setSelectMode] = useState<boolean>(false);
  const enterSelectMode = () => {
    setSelectMode(true);
    stopBubble();
  };
  const exitSelectMode = () => {
    setSelectMode(false);
    stopBubble();
  };
  return (
    <StyledControl
      className={clsx(!selectMode && 'hover', CELL_SHOW_ON_HOVER, className)}
      data-cy="translation-label-control"
    >
      {selectMode ? (
        <LabelSelector
          onClose={stopBubble(exitSelectMode)}
          onSelect={onSelect}
          existing={existing}
        />
      ) : (
        <AddLabel
          onClick={stopBubble(enterSelectMode)}
          className="clickable"
          data-cy="translation-label-add"
        />
      )}
    </StyledControl>
  );
};
