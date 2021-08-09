import React from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Box, IconButton, Button, Tooltip } from '@material-ui/core';
import { Edit, CameraAlt } from '@material-ui/icons';

import { StateType, translationStates } from 'tg.constants/translationStates';
import { StateIcon } from './StateIcon';
import { stopBubble } from 'tg.fixtures/eventHandler';
import { useCellStyles } from './styles';

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

type ControlsProps = {
  mode: 'edit' | 'view';
  state?: StateType;
  editEnabled?: boolean;
  onClick?: () => void;
  onEdit?: () => void;
  onSave?: () => void;
  onSaveAndNew?: () => void;
  onCancel?: () => void;
  onScreenshots?: () => void;
  onStateChange?: (state: StateType) => void;
  screenshotRef?: React.Ref<HTMLButtonElement>;
  screenshotsPresent?: boolean;
  absolute?: boolean;
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
  absolute,
}) => {
  const classes = useCellStyles();
  const t = useTranslate();

  return mode === 'view' ? (
    <Box
      display="flex"
      justifyContent="flex-end"
      width="100%"
      minHeight={26}
      className={absolute ? classes.controlsAbsolute : undefined}
    >
      {editEnabled && (
        <>
          {getStateTransitionButtons(
            state,
            onStateChange,
            classes.showOnHover,
            t
          )}
          <IconButton
            onClick={onEdit}
            size="small"
            data-cy="translations-cell-edit-button"
            className={classes.showOnHover}
          >
            <Edit fontSize="small" />
          </IconButton>
        </>
      )}
      {onScreenshots && (
        <IconButton
          size="small"
          ref={screenshotRef}
          onClick={stopBubble(onScreenshots)}
          data-cy="translations-cell-screenshots-button"
        >
          <CameraAlt
            fontSize="small"
            color={screenshotsPresent ? 'secondary' : 'disabled'}
          />
        </IconButton>
      )}
    </Box>
  ) : (
    <Box
      display="flex"
      justifyContent="space-between"
      alignItems="flex-end"
      width="100%"
      minHeight={26}
    >
      <Box
        className={classes.controlsSpaced}
        display="flex"
        alignItems="center"
      >
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
      </Box>
      <Box display="flex">
        {getStateTransitionButtons(
          state,
          onStateChange,
          classes.showOnHover,
          t
        )}
        {onScreenshots && (
          <IconButton
            size="small"
            ref={screenshotRef}
            onClick={onScreenshots}
            data-cy="translations-cell-screenshots-button"
          >
            <CameraAlt
              fontSize="small"
              color={screenshotsPresent ? 'secondary' : 'disabled'}
            />
          </IconButton>
        )}
      </Box>
    </Box>
  );
};
