import React from 'react';
import { IconButton, makeStyles } from '@material-ui/core';
import { Done, Close, Edit } from '@material-ui/icons';

import { Editor } from 'tg.component/editor/Editor';
import { CellPlain } from './CellPlain';
import { CellControls } from './CellControls';
import { useEditableCell } from './useEditableCell';

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
};

export const CellData: React.FC<Props> = React.memo(function Cell({
  text,
  keyName,
  language,
  keyId,
}) {
  const classes = useStyles();

  const {
    isEditing,
    value,
    setValue,
    handleEdit,
    handleEditCancel,
    handleSave,
  } = useEditableCell({ text, keyId, keyName, language });

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
            >
              <Done fontSize="small" />
            </IconButton>
            <IconButton
              onClick={handleEditCancel}
              color="secondary"
              size="small"
            >
              <Close fontSize="small" />
            </IconButton>
          </CellControls>
        </>
      ) : (
        <>
          {text}
          <CellControls key="cell-controls" className={classes.controls}>
            <IconButton onClick={handleEdit} size="small">
              <Edit fontSize="small" />
            </IconButton>
          </CellControls>
        </>
      )}
    </CellPlain>
  );
});
