import React from 'react';
import clsx from 'clsx';
import { Tooltip, makeStyles } from '@material-ui/core';
import { T } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { Editor } from 'tg.component/editor/Editor';
import { StateType, translationStates } from 'tg.constants/translationStates';
import { useEditableRow } from '../useEditableRow';
import { CellControls } from '../cell';
import { TranslationVisual } from '../TranslationVisual';
import { useTranslationsDispatch } from '../context/TranslationsContext';
import { useCellStyles } from '../cell/styles';
import { stopBubble } from 'tg.fixtures/eventHandler';

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
    stateHover: {
      position: 'absolute',
      width: 12,
      height: '100%',
    },
    stateBorder: {
      position: 'absolute',
      height: '100%',
    },
    editor: {
      overflow: 'hidden',
      display: 'flex',
      flexDirection: 'column',
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
      gridArea: 'controls',
      display: 'flex',
      justifyContent: 'space-between',
      overflow: 'hidden',
      alignItems: 'stretch',
      minHeight: 46,
      marginTop: -20,
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

export const CellData: React.FC<Props> = React.memo(function Cell({
  data,
  language,
  colIndex,
  onResize,
  editEnabled,
  width,
  active,
  renderEdit,
}) {
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
      onClick={
        editEnabled && !isEditing ? () => handleEdit(language.tag) : undefined
      }
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
            <CellControls
              mode="edit"
              state={state}
              onSave={handleSave}
              onCancel={handleEditCancel}
              onStateChange={handleStateChange}
            />
          </div>
        </div>
      ) : (
        <>
          <div className={classes.translation}>
            <TranslationVisual
              width={width}
              text={isEditing ? value : translation?.text}
              locale={language.tag}
              limitLines={!isEditing}
            />
          </div>
          <div className={classes.controls}>
            {isEditing ? (
              <CellControls mode="view" />
            ) : active ? (
              <CellControls
                mode="view"
                onEdit={() => handleEdit(language.tag)}
                onCancel={handleEditCancel}
                onSave={handleSave}
                editEnabled={editEnabled}
                state={state}
                onStateChange={handleStateChange}
              />
            ) : (
              // hide as many components as possible in order to be performant
              <CellControls
                mode="view"
                editEnabled={editEnabled}
                onEdit={renderEdit ? () => handleEdit(language.tag) : undefined}
              />
            )}
          </div>
        </>
      )}

      <Tooltip title={<T noWrap>{translationStates[state]?.translationKey}</T>}>
        <div
          className={classes.stateHover}
          data-cy="translations-state-indicator"
        >
          <div
            className={clsx(classes.stateBorder, cellClasses.state)}
            onMouseDown={stopBubble(handleResize)}
            onClick={stopBubble()}
            onMouseUp={stopBubble()}
            style={{
              borderLeft: `4px solid ${translationStates[state]?.color}`,
            }}
          />
        </div>
      </Tooltip>
    </div>
  );
});
