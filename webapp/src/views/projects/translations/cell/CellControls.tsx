import React, { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { IconButton, Button, Tooltip, makeStyles } from '@material-ui/core';
import { Edit, CameraAlt } from '@material-ui/icons';

import { StateType, translationStates } from 'tg.constants/translationStates';
import { StateIcon } from './StateIcon';
import { stopBubble } from 'tg.fixtures/eventHandler';
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
    translationStates[state]?.next.map((s) => (
      <Tooltip
        key={s}
        title={t(
          'translation_state_change',
          {
            newState: t(translationStates[s]?.translationKey, {}, true),
          },
          true
        )}
      >
        <IconButton
          data-cy="translation-state-button"
          onClick={stopBubble(() => onStateChange?.(s))}
          size="small"
          className={className}
        >
          <StateIcon state={s} fontSize="small" />
        </IconButton>
      </Tooltip>
    ))
  );
};

const useStyles = makeStyles({
  container: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-end',
    minHeight: 26,
    margin: -2,
    overflow: 'hidden',
    '& > *': {
      margin: 2,
    },
  },
  leftPart: {
    display: 'flex',
    overflow: 'hidden',
    '& > * + *': {
      marginLeft: 10,
    },
  },
  rightPart: {
    display: 'flex',
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
  screenshotRef?: React.Ref<HTMLButtonElement>;
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

  return (
    <div className={classes.container}>
      <div className={classes.leftPart} onClick={stopBubble()}>
        {onAddTag &&
          (tagEdit ? (
            <TagInput onClose={() => setTagEdit(false)} onAdd={handleAddTag} />
          ) : (
            addTag && (
              <TagAdd
                withFullLabel={Boolean(firstTag)}
                onClick={() => setTagEdit(true)}
              />
            )
          ))}
        {modeEdit && !tagEdit && (
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
      {!tagEdit && (
        <div className={classes.rightPart} onClick={stopBubble()}>
          {getStateTransitionButtons(
            state,
            onStateChange,
            modeEdit ? '' : cellClasses.showOnHover,
            t
          )}
          {editEnabled && !modeEdit && onEdit && (
            <ControlsButton
              onClick={onEdit}
              data-cy="translations-cell-edit-button"
              className={cellClasses.showOnHover}
            >
              <Edit fontSize="small" />
            </ControlsButton>
          )}
          {onScreenshots && (
            <ControlsButton
              passRef={screenshotRef}
              onClick={onScreenshots}
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
    </div>
  );
};
