import React from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/core';

import { components } from 'tg.service/apiSchema.generated';
import { StateType } from 'tg.constants/translationStates';
import { useEditableRow } from '../useEditableRow';
import { TranslationVisual } from '../TranslationVisual';
import { useTranslationsDispatch } from '../context/TranslationsContext';
import { useCellStyles } from '../cell/styles';
import { CellStateBar } from '../cell/CellStateBar';
import { ControlsTranslation } from '../cell/ControlsTranslation';
import { TranslationOpened } from '../TranslationOpened';

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
      padding: '12px 14px 12px 12px',
      marginTop: -16,
    },
  };
});

type Props = {
  data: KeyWithTranslationsModel;
  language: LanguageModel;
  colIndex?: number;
  onResize?: (colIndex: number) => void;
  editEnabled: boolean;
  width?: number | string;
  active: boolean;
  lastFocusable: boolean;
};

export const CellTranslation: React.FC<Props> = ({
  data,
  language,
  colIndex,
  onResize,
  editEnabled,
  width,
  active,
  lastFocusable,
}) => {
  const classes = useStyles();
  const cellClasses = useCellStyles({
    position: lastFocusable ? 'right' : undefined,
  });

  const translation = data.translations[language.tag];
  const state = translation?.state || 'UNTRANSLATED';

  const {
    isEditing,
    editVal,
    value,
    setValue,
    handleOpen,
    handleClose,
    handleSave,
    autofocus,
    handleModeChange,
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
    onResize?.(colIndex || 0);
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
      onClick={
        editEnabled && !isEditing ? () => handleOpen('editor') : undefined
      }
    >
      {editVal ? (
        <TranslationOpened
          className={classes.editor}
          keyId={data.keyId}
          language={language}
          translation={translation}
          value={value}
          onChange={(v) => setValue(v as string)}
          onSave={() => handleSave()}
          onCmdSave={() => handleSave('EDIT_NEXT')}
          onCancel={handleClose}
          autofocus={autofocus}
          state={state}
          onStateChange={handleStateChange}
          mode={editVal.mode}
          onModeChange={handleModeChange}
          editEnabled={editEnabled}
        />
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
                onEdit={() => handleOpen('editor')}
                editEnabled={editEnabled}
                state={state}
                onStateChange={handleStateChange}
                onComments={() => handleOpen('comments')}
                commentsCount={translation?.commentCount}
              />
            ) : (
              // hide as many components as possible in order to be performant
              <ControlsTranslation
                commentsCount={translation?.commentCount}
                lastFocusable={lastFocusable}
              />
            )}
          </div>
        </>
      )}

      <CellStateBar state={state} onResize={handleResize} />
    </div>
  );
};
