import React from 'react';
import { IconButton, makeStyles } from '@material-ui/core';
import { Done, Close, Edit } from '@material-ui/icons';

import { Editor } from 'tg.component/editor/Editor';
import { useEditableRow } from '../useEditableRow';
import { CellPlain } from '../CellPlain';
import { CellControls } from '../CellControls';

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
  language: string | undefined;
  editEnabled: boolean;
};

export const CellData: React.FC<Props> = React.memo(function Cell({
  text,
  keyName,
  language,
  keyId,
  editEnabled,
}) {
  const classes = useStyles();

  const {
    isEditing,
    value,
    setValue,
    handleEdit,
    handleEditCancel,
    handleSave,
  } = useEditableRow({ keyId, keyName, defaultVal: text, language: language });

  return (
    <CellPlain
      background={isEditing ? '#efefef' : undefined}
      className={classes.cell}
    >
      {isEditing ? (
        <>
          <Editor
            minHeight={100}
            initialValue={value}
            variables={[]}
            onChange={(v) => setValue(v as string)}
            onSave={handleSave}
            onCancel={handleEditCancel}
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
          </CellControls>
        </>
      ) : (
        <>
          {text}
          <CellControls key="cell-controls" className={classes.controls}>
            {editEnabled && (
              <IconButton onClick={() => handleEdit(language)} size="small">
                <Edit fontSize="small" />
              </IconButton>
            )}
          </CellControls>
        </>
      )}
    </CellPlain>
  );
});
