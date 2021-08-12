import { Box, makeStyles } from '@material-ui/core';
import React from 'react';
import { Editor } from 'tg.component/editor/Editor';
import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';
import { StateType } from 'tg.constants/translationStates';
import { components } from 'tg.service/apiSchema.generated';
import { CellContent, CellControls, CellPlain } from '../cell';
import { useTranslationsDispatch } from '../context/TranslationsContext';
import { TranslationVisual } from '../TranslationVisual';
import { useEditableRow } from '../useEditableRow';

type LanguageModel = components['schemas']['LanguageModel'];
type KeyWithTranslationsModel =
  components['schemas']['KeyWithTranslationsModel'];

const useStyles = makeStyles((theme) => {
  return {
    content: {
      display: 'flex',
      width: '100%',
      alignItems: 'stretch',
      flexGrow: 1,
    },
    languageRow: {
      display: 'flex',
      flexDirection: 'column',
      flexBasis: '50%',
      width: '50%',
      flexGrow: 1,
      overflow: 'hidden',
    },
    editor: {
      display: 'flex',
      flexBasis: '50%',
      width: '50%',
      flexGrow: 1,
      justifyContent: 'stretch',
      border: `1px solid white`,
      borderWidth: '0px 0px 0px 1px',
    },
    rowContent: {
      display: 'flex',
      flexGrow: 1,
      overflow: 'hidden',
      alignItems: 'flex-start',
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
    translation: {
      overflow: 'hidden',
    },
    controls: {
      display: 'none',
    },
  };
});

type Props = {
  data: KeyWithTranslationsModel;
  language: LanguageModel;
  colIndex: number;
  onResize: (colIndex: number) => void;
  editEnabled: boolean;
};

export const LanguageCell: React.FC<Props> = ({
  data,
  language: l,
  colIndex,
  onResize,
  editEnabled,
}) => {
  const dispatch = useTranslationsDispatch();
  const classes = useStyles();

  const {
    isEditing,
    value,
    setValue,
    handleEdit,
    handleEditCancel,
    handleSave,
    autofocus,
  } = useEditableRow({
    keyId: data.keyId,
    keyName: data.keyName,
    translations: data.translations,
    language: l.tag,
  });

  const handleStateChange = (language: string) => (state: StateType) => {
    dispatch({
      type: 'SET_TRANSLATION_STATE',
      payload: {
        keyId: data.keyId,
        language: language,
        translationId: data.translations[language]?.id,
        state,
      },
    });
  };

  const translation = data.translations[l.tag];

  const toggleEdit = () => {
    if (isEditing) {
      handleEditCancel();
    } else {
      handleEdit(l.tag);
    }
  };

  return (
    <div className={classes.content}>
      <div className={classes.languageRow}>
        <CellPlain
          hover={!isEditing}
          background={isEditing ? '#efefef' : undefined}
          onClick={editEnabled ? () => toggleEdit() : undefined}
          flexGrow={1}
          state={translation?.state || 'UNTRANSLATED'}
          onResize={() => onResize(colIndex)}
        >
          <CellContent>
            <div className={classes.rowContent}>
              <div className={classes.languageContent}>
                <CircledLanguageIcon flag={l.flagEmoji} />
                <Box>{l.tag}</Box>
              </div>
              <div className={classes.translation}>
                <TranslationVisual
                  locale={l.tag}
                  maxLines={isEditing ? undefined : 3}
                  text={isEditing ? value : translation?.text}
                  wrapVariants={!isEditing}
                />
              </div>
            </div>
          </CellContent>
          {!isEditing && (
            <CellControls
              mode="view"
              state={translation?.state}
              onStateChange={handleStateChange(l.tag)}
              editEnabled={editEnabled}
              onEdit={() => toggleEdit()}
              absolute
            />
          )}
        </CellPlain>
      </div>
      {isEditing && (
        <div className={classes.editor}>
          <CellPlain background="#efefef">
            <CellContent>
              <Editor
                value={value}
                onChange={(v) => setValue(v as string)}
                onSave={() => handleSave()}
                onCmdSave={() => handleSave('EDIT_NEXT')}
                onCancel={handleEditCancel}
                autofocus={autofocus}
                background="#efefef"
              />
            </CellContent>
            <CellControls
              mode="edit"
              state={translation?.state}
              onSave={handleSave}
              onCancel={handleEditCancel}
              onStateChange={handleStateChange(l.tag)}
            />
          </CellPlain>
        </div>
      )}
    </div>
  );
};
