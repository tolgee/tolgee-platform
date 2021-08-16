import React, { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Button, Tooltip, makeStyles } from '@material-ui/core';
import { Edit, CameraAlt } from '@material-ui/icons';

import { StateType, translationStates } from 'tg.constants/translationStates';
import { StateIcon } from './StateIcon';
import { useCellStyles } from './styles';
import { ControlsButton } from './ControlsButton';
import { TagInput } from '../Tags/TagInput';
import { TagAdd } from '../Tags/TagAdd';

const getStateTransitionButtons = (
  state: StateType | undefined,
  onStateChange: ((s: StateType) => void) | undefined,
  className: string,
  t: ReturnType<typeof useTranslate>
) => {
  return (
    state &&
    translationStates[state]?.next.map((s, i) => (
      <Tooltip
        key={i}
        title={t(
          'translation_state_change',
          {
            newState: t(translationStates[s]?.translationKey, {}, true),
          },
          true
        )}
      >
        <ControlsButton
          data-cy="translation-state-button"
          onClick={() => onStateChange?.(s)}
          className={className}
        >
          <StateIcon state={s} fontSize="small" />
        </ControlsButton>
      </Tooltip>
    ))
  );
};

const useStyles = makeStyles({
  leftPart: {
    display: 'flex',
    alignItems: 'flex-start',
    overflow: 'hidden',
    padding: 12,
    '& > * + *': {
      marginLeft: 10,
    },
  },
  rightPart: {
    display: 'flex',
    justifyContent: 'flex-end',
    alignItems: 'flex-end',
    padding: 12,
    '& > * + *': {
      marginLeft: 4,
    },
    flexGrow: 1,
  },
});

type ControlsProps = {
  mode: 'edit' | 'view';
  state?: StateType;
  editEnabled?: boolean;
  onClick?: () => void;
  onEdit?: () => void;
  onSave?: () => void;
  onCancel?: () => void;
  onScreenshots?: () => void;
  onStateChange?: (state: StateType) => void;
  screenshotRef?: React.Ref<any>;
  screenshotsPresent?: boolean;
  screenshotsOpen?: boolean;
  absolute?: boolean;
  firstTag?: boolean;
  onAddTag?: (name: string, onSuccess: () => void) => void;
  addTag?: boolean;
};

export const CellControls: React.FC<ControlsProps> = ({
  mode,
  state,
  editEnabled,
  onEdit,
  onSave,
  onCancel,
  onScreenshots,
  onStateChange,
  screenshotRef,
  screenshotsPresent,
  screenshotsOpen,
  firstTag,
  onAddTag,
  addTag,
}) => {
  const cellClasses = useCellStyles();
  const classes = useStyles();
  const t = useTranslate();
  const [tagEdit, setTagEdit] = useState(false);

  const handleAddTag = (name: string) => {
    onAddTag?.(name, () => setTagEdit(false));
  };

  const modeEdit = mode === 'edit';

  // left section
  const displayTagInput = onAddTag && tagEdit;
  const displayTagAdd = onAddTag && !tagEdit && addTag;
  const displayEditorButtons = modeEdit && !tagEdit;
  const displayLeftPart =
    displayTagInput || displayTagAdd || displayEditorButtons;

  // right section
  const displayTransitionButtons = !tagEdit && state;
  const displayEdit = !tagEdit && editEnabled && !modeEdit && onEdit;
  const displayScreenshots = !tagEdit && onScreenshots;
  const displayRightPart =
    displayTransitionButtons || displayEdit || displayScreenshots;

  return (
    <>
      {displayLeftPart && (
        <div className={classes.leftPart}>
          {displayTagInput && (
            <TagInput onClose={() => setTagEdit(false)} onAdd={handleAddTag} />
          )}
          {displayTagAdd && (
            <TagAdd
              withFullLabel={Boolean(firstTag)}
              onClick={() => setTagEdit(true)}
            />
          )}
          {displayEditorButtons && (
            <>
              <Button
                onClick={onCancel}
                color="primary"
                variant="outlined"
                size="small"
                data-cy="translations-cell-cancel-button"
              >
                <T>translations_cell_cancel</T>
              </Button>
              <Button
                onClick={onSave}
                color="primary"
                variant="contained"
                size="small"
                data-cy="translations-cell-save-button"
              >
                <T>translations_cell_save</T>
              </Button>
            </>
          )}
        </div>
      )}

      {displayRightPart && (
        <div className={classes.rightPart}>
          {displayTransitionButtons &&
            getStateTransitionButtons(
              state,
              onStateChange,
              modeEdit ? '' : cellClasses.showOnHover,
              t
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
          {displayScreenshots && (
            <ControlsButton
              onClick={onScreenshots}
              ref={screenshotRef}
              data-cy="translations-cell-screenshots-button"
              className={
                screenshotsPresent || screenshotsOpen || modeEdit
                  ? undefined
                  : cellClasses.showOnHover
              }
            >
              <CameraAlt
                fontSize="small"
                color={screenshotsPresent ? 'secondary' : 'disabled'}
              />
            </ControlsButton>
          )}
        </div>
      )}
    </>
  );
};
