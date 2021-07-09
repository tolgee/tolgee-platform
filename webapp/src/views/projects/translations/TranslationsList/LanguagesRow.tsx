import React from 'react';
import { makeStyles, Box, IconButton } from '@material-ui/core';
import { Done, Close, Edit } from '@material-ui/icons';

import { components } from 'tg.service/apiSchema.generated';
import { CellPlain } from '../CellPlain';
import { CircledLanguageIcon } from '../CircledLanguageIcon';
import { CellControls } from '../CellControls';
import { useEditableRow } from '../useEditableRow';
import { Editor } from 'tg.component/editor/Editor';

type LanguageModel = components['schemas']['LanguageModel'];
type KeyWithTranslationsModel =
  components['schemas']['KeyWithTranslationsModel'];

const useStyles = makeStyles((theme) => ({
  content: {
    display: 'flex',
    width: '100%',
    alignItems: 'stretch',
  },
  languages: {
    display: 'flex',
    flexDirection: 'column',
    flexBasis: '50%',
    flexGrow: 1,
    overflow: 'hidden',
  },
  editor: {
    display: 'flex',
    flexBasis: '50%',
    flexGrow: 1,
    justifyContent: 'stretch',
  },
  rowWrapper: {
    display: 'flex',
    flexGrow: 1,
    '&:hover': {
      background: theme.palette.grey[50],
    },
  },
  rowContent: {
    display: 'flex',
    overflow: 'hidden',
    '&:hover $controls': {
      display: 'flex',
    },
  },
  data: {
    overflow: 'hidden',
    textOverflow: 'ellipsis',
  },
  languageContent: {
    width: 100,
    flexShrink: 0,
    display: 'flex',
    alignItems: 'center',
    '& > * + *': {
      marginLeft: 4,
    },
  },
  controls: {
    display: 'none',
  },
}));

type Props = {
  languages: LanguageModel[];
  data: KeyWithTranslationsModel;
};

export const LanguagesRow: React.FC<Props> = React.memo(function Cell({
  languages,
  data,
}) {
  const classes = useStyles();

  const { editVal, value, setValue, handleEdit, handleEditCancel, handleSave } =
    useEditableRow({
      keyId: data.keyId,
      keyName: data.keyName,
      translations: data.translations,
      language: null,
    });

  return (
    <div className={classes.content}>
      <div className={classes.languages}>
        {languages.map((l) => (
          <div key={l.id} className={classes.rowWrapper}>
            <CellPlain
              background={l.tag === editVal?.language ? '#efefef' : undefined}
            >
              <div className={classes.rowContent}>
                <div className={classes.languageContent}>
                  <CircledLanguageIcon flag={l.flagEmoji} />
                  <Box>{l.tag}</Box>
                </div>
                <div className={classes.data}>
                  {data.translations[l.tag]?.text}
                </div>
                <CellControls key="cell-controls" className={classes.controls}>
                  <IconButton onClick={() => handleEdit(l.tag)} size="small">
                    <Edit fontSize="small" />
                  </IconButton>
                </CellControls>
              </div>
            </CellPlain>
          </div>
        ))}
      </div>
      {editVal?.language && (
        <div className={classes.editor}>
          <CellPlain background="#efefef">
            <Editor
              key={editVal.language}
              minHeight={100}
              initialValue={value}
              variables={[]}
              onChange={(v) => setValue(v as string)}
              onSave={(direction) => handleSave(direction)}
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
          </CellPlain>
        </div>
      )}
    </div>
  );
});
