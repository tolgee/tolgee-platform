import React from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/core';

import { components } from 'tg.service/apiSchema.generated';
import { Editor } from 'tg.component/editor/Editor';
import { StateType } from 'tg.constants/translationStates';
import { useEditableRow } from '../useEditableRow';
import { TranslationVisual } from '../TranslationVisual';
import { useTranslationsDispatch } from '../context/TranslationsContext';
import { useCellStyles } from '../cell/styles';
import { CellStateBar } from '../cell/CellStateBar';
import { ControlsTranslation } from '../cell/ControlsTranslation';
import { ControlsEditor } from '../cell/ControlsEditor';

type LanguageModel = components['schemas']['LanguageModel'];
type KeyWithTranslationsModel =
  components['schemas']['KeyWithTranslationsModel'];

const useStyles = makeStyles((theme) => {
  return {
    container: {
      display: 'flex',
      flexDirection: 'column',
      position: 'relative',
    },
    editor: {
      overflow: 'hidden',
      display: 'flex',
      flexDirection: 'column',
      flexGrow: 1,
    },
    editorContainer: {
      padding: '12px 12px 0px 12px',
      flexGrow: 1,
    },
    editorControls: {
      display: 'flex',
    },
    translation: {
      flexGrow: 1,
      margin: '12px 12px 8px 12px',
      overflow: 'hidden',
      position: 'relative',
    },
    controls: {
      boxSizing: 'border-box',
      gridArea: 'controls',
      display: 'flex',
      justifyContent: 'flex-end',
      overflow: 'hidden',
      minHeight: 44,
      padding: '12px 12px 12px 12px',
      marginTop: -16,
    },
  };
});

type Props = {
  data: KeyWithTranslationsModel;
  language: LanguageModel;
  colIndex: number;
  onResize: (colIndex: number) => void;
  editEnabled: boolean;
  width: number;
  active: boolean;
  renderEdit: boolean;
};

export const CellTranslation: React.FC<Props> = ({
  data,
  language,
  colIndex,
  onResize,
  editEnabled,
  width,
  active,
  renderEdit,
}) => {
  const classes = useStyles();
  const cellClasses = useCellStyles();

  const translation = data.translations[language.tag];
  const state = translation?.state || 'UNTRANSLATED';

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
    defaultVal: translation?.text || '',
    language: language.tag,
  });
  const dispatch = useTranslationsDispatch();

  const handleStateChange = (state: StateType) => {
    dispatch({
      type: 'SET_TRANSLATION_STATE',
      payload: {
        keyId: data.keyId,
        translationId: translation?.id as number,
        language: language.tag as string,
        state,
      },
    });
  };

  const handleResize = () => {
    onResize(colIndex);
  };

  return (
    <div
      className={clsx({
        [classes.container]: true,
        [cellClasses.cellPlain]: true,
        [cellClasses.hover]: !isEditing,
        [cellClasses.cellClickable]: editEnabled && !isEditing,
        [cellClasses.cellRaised]: isEditing,
      })}
      style={{ width }}
      onClick={editEnabled && !isEditing ? handleEdit : undefined}
    >
      {isEditing ? (
        <div className={classes.editor}>
          <div className={classes.editorContainer}>
            <Editor
              value={value}
              onChange={(v) => setValue(v as string)}
              onSave={() => handleSave()}
              onCmdSave={() => handleSave('EDIT_NEXT')}
              onCancel={handleEditCancel}
              autofocus={autofocus}
            />
          </div>
          <div className={classes.editorControls}>
            <ControlsEditor
              state={state}
              onSave={handleSave}
              onCancel={handleEditCancel}
              onStateChange={handleStateChange}
            />
          </div>
        </div>
      ) : (
        <>
          <div
            className={classes.translation}
            data-cy="translations-table-cell"
          >
            <TranslationVisual
              width={width}
              text={isEditing ? value : translation?.text}
              locale={language.tag}
              limitLines={!isEditing}
            />
          </div>
          <div className={classes.controls}>
            {active ? (
              <ControlsTranslation
                onEdit={handleEdit}
                editEnabled={editEnabled}
                state={state}
                onStateChange={handleStateChange}
              />
            ) : (
              // hide as many components as possible in order to be performant
              <ControlsTranslation
                editEnabled={editEnabled}
                onEdit={renderEdit ? handleEdit : undefined}
              />
            )}
          </div>
        </>
      )}

      <CellStateBar state={state} onResize={handleResize} />
    </div>
  );
};
