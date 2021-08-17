import React from 'react';
import { Edit } from '@material-ui/icons';

import { StateType } from 'tg.constants/translationStates';
import { useCellStyles } from './styles';
import { ControlsButton } from './ControlsButton';
import { StateTransitionButtons } from './StateTransitionButtons';

type ControlsProps = {
  state?: StateType;
  editEnabled?: boolean;
  onEdit?: () => void;
  onStateChange?: (state: StateType) => void;
};

export const ControlsTranslation: React.FC<ControlsProps> = ({
  state,
  editEnabled,
  onEdit,
  onStateChange,
}) => {
  const cellClasses = useCellStyles();

  // right section
  const displayTransitionButtons = editEnabled && state;
  const displayEdit = editEnabled && onEdit;
  const displayRightPart = displayTransitionButtons || displayEdit || null;

  return (
    displayRightPart && (
      <>
        {displayTransitionButtons && (
          <StateTransitionButtons
            state={state}
            onStateChange={onStateChange}
            className={cellClasses.showOnHover}
          />
        )}
        {displayEdit && (
          <ControlsButton
            onClick={onEdit}
            data-cy="translations-cell-edit-button"
            className={cellClasses.showOnHover}
          >
            <Edit fontSize="small" />
          </ControlsButton>
        )}
      </>
    )
  );
};
