import clsx from 'clsx';
import { makeStyles, Tooltip } from '@material-ui/core';
import { T } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { Editor } from 'tg.component/editor/Editor';
import { stopBubble } from 'tg.fixtures/eventHandler';
import { StateType, translationStates } from 'tg.constants/translationStates';
import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';
import { useCellStyles } from '../cell/styles';
import { useEditableRow } from '../useEditableRow';
import { CellControls } from '../cell';
import { useTranslationsDispatch } from '../context/TranslationsContext';
import { TranslationVisual } from '../TranslationVisual';

type LanguageModel = components['schemas']['LanguageModel'];
type KeyWithTranslationsModel =
  components['schemas']['KeyWithTranslationsModel'];

const useStyles = makeStyles((theme) => {
  return {
    splitContainer: {
      display: 'flex',
      flexGrow: 1,
      position: 'relative',
    },
    container: {
      flexBasis: '50%',
      flexGrow: 1,
      position: 'relative',
      display: 'grid',
      gridTemplateColumns: 'auto 80px 1fr',
      gridTemplateAreas: `
        "flag     language translation"
        "controls controls controls   "
      `,
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
    flag: {
      gridArea: 'flag',
      margin: '12px 8px 8px 12px',
      width: 20,
      height: 20,
      padding: 1,
    },
    language: {
      margin: '12px 8px 8px 0px',
    },
    editor: {
      overflow: 'hidden',
      flexBasis: '50%',
      flexGrow: 1,
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
      gridArea: 'translation',
      margin: '12px 12px 8px 0px',
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

export const LanguageCell: React.FC<Props> = ({
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
  const dispatch = useTranslationsDispatch();

  const translation = data.translations[language.tag];
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
    defaultVal: translation?.text,
    language: language.tag,
  });

  const handleStateChange = (state: StateType) => {
    dispatch({
      type: 'SET_TRANSLATION_STATE',
      payload: {
        keyId: data.keyId,
        language: language.tag,
        translationId: data.translations[language.tag]?.id,
        state,
      },
    });
  };

  const handleResize = () => {
    onResize(colIndex);
  };

  const toggleEdit = () => {
    if (isEditing) {
      handleEditCancel();
    } else {
      handleEdit(language.tag);
    }
  };

  const state = translation?.state || 'UNTRANSLATED';

  return (
    <div
      className={clsx({
        [classes.splitContainer]: true,
        [cellClasses.cellRaised]: isEditing,
      })}
    >
      <div
        className={clsx({
          [classes.container]: true,
          [cellClasses.cellPlain]: true,
          [cellClasses.hover]: !isEditing,
          [cellClasses.cellClickable]: editEnabled,
          [cellClasses.cellSelected]: isEditing,
        })}
        style={{ width }}
        onClick={editEnabled ? () => toggleEdit() : undefined}
      >
        <CircledLanguageIcon
          flag={language.flagEmoji}
          className={classes.flag}
        />

        <div className={classes.language}>{language.tag}</div>

        <div className={classes.translation}>
          <TranslationVisual
            width={width}
            text={isEditing ? value : translation?.text}
            locale={language.tag}
            limitLines={!isEditing}
          />
        </div>

        <Tooltip
          title={<T noWrap>{translationStates[state]?.translationKey}</T>}
        >
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
      </div>

      {isEditing && (
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
      )}
    </div>
  );
};
