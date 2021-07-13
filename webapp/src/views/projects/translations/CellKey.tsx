import React, { useRef, useState } from 'react';
import { useContextSelector } from 'use-context-selector';
import { IconButton, Checkbox, Box, makeStyles } from '@material-ui/core';
import { Done, Close, Edit, CameraAlt } from '@material-ui/icons';

import { Editor } from 'tg.component/editor/Editor';
import { useEditableRow } from './useEditableRow';
import {
  TranslationsContext,
  useTranslationsDispatch,
} from './TranslationsContext';
import { ScreenshotsPopover } from './Screenshots/ScreenshotsPopover';
import { CellPlain } from './CellPlain';
import { CellControls } from './CellControls';

const useStyles = makeStyles({
  cell: {
    '&:hover $controls': {
      display: 'flex',
    },
  },
  controls: {
    display: 'none',
  },
});

type Props = {
  text: string;
  keyId: number;
  keyName: string;
  screenshotCount: number;
  editEnabled: boolean;
};

export const CellKey: React.FC<Props> = React.memo(function Cell({
  text,
  keyName,
  keyId,
  screenshotCount,
  editEnabled,
}) {
  const classes = useStyles();
  const [screenshotsOpen, setScreenshotsOpen] = useState(false);

  const screenshotEl = useRef<HTMLButtonElement | null>(null);

  const {
    isEditing,
    value,
    setValue,
    handleEdit,
    handleEditCancel,
    handleSave,
  } = useEditableRow({
    keyId,
    keyName,
    defaultVal: keyName,
    language: undefined,
  });

  const isSelected = useContextSelector(TranslationsContext, (c) =>
    c.selection.includes(keyId)
  );

  const dispatch = useTranslationsDispatch();

  const toggleSelect = () => {
    dispatch({ type: 'TOGGLE_SELECT', payload: keyId });
  };

  return (
    <CellPlain
      background={isEditing ? '#efefef' : undefined}
      className={classes.cell}
    >
      <>
        {isEditing ? (
          <>
            <Editor
              minHeight={100}
              initialValue={value}
              variables={[]}
              onChange={(v) => setValue(v as string)}
              onSave={handleSave}
              onCancel={handleEditCancel}
              language="plaintext"
              autoFocus
            />
            <CellControls>
              <IconButton
                onClick={() => handleSave()}
                color="primary"
                size="small"
                data-cy="translations-cell-save-button"
              >
                <Done fontSize="small" />
              </IconButton>
              <IconButton
                onClick={handleEditCancel}
                color="secondary"
                size="small"
                data-cy="translations-cell-cancel-button"
              >
                <Close fontSize="small" />
              </IconButton>
              <IconButton
                size="small"
                ref={screenshotEl}
                onClick={() => setScreenshotsOpen(true)}
                data-cy="translations-cell-screenshots-button"
              >
                <CameraAlt
                  fontSize="small"
                  color={screenshotCount > 0 ? 'secondary' : 'disabled'}
                />
              </IconButton>
            </CellControls>
          </>
        ) : (
          <Box display="flex" alignItems="center" width="100%">
            {editEnabled && (
              <Box margin={-1}>
                <Checkbox
                  checked={isSelected}
                  onChange={toggleSelect}
                  data-cy="translations-row-checkbox"
                />
              </Box>
            )}
            <Box overflow="hidden" textOverflow="ellipsis">
              {text}
            </Box>
            <CellControls key="cell-controls">
              {editEnabled && (
                <IconButton
                  onClick={() => handleEdit(undefined)}
                  size="small"
                  className={classes.controls}
                  data-cy="translations-cell-edit-button"
                >
                  <Edit fontSize="small" />
                </IconButton>
              )}
              <IconButton
                size="small"
                ref={screenshotEl}
                onClick={() => setScreenshotsOpen(true)}
                data-cy="translations-cell-screenshots-button"
              >
                <CameraAlt
                  fontSize="small"
                  color={screenshotCount > 0 ? 'secondary' : 'disabled'}
                />
              </IconButton>
            </CellControls>
          </Box>
        )}
        {screenshotsOpen && (
          <ScreenshotsPopover
            anchorEl={screenshotEl.current!}
            keyId={keyId}
            onClose={() => {
              setScreenshotsOpen(false);
            }}
          />
        )}
      </>
    </CellPlain>
  );
});
