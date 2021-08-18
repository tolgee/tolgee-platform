import clsx from 'clsx';
import { makeStyles } from '@material-ui/core';

import { components } from 'tg.service/apiSchema.generated';
import { Editor } from 'tg.component/editor/Editor';
import { StateType } from 'tg.constants/translationStates';
import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';
import { useCellStyles } from '../cell/styles';
import { useEditableRow } from '../useEditableRow';
import { useTranslationsDispatch } from '../context/TranslationsContext';
import { TranslationVisual } from '../TranslationVisual';
import { CellStateBar } from '../cell/CellStateBar';
import { ControlsTranslation } from '../cell/ControlsTranslation';
import { ControlsEditor } from '../cell/ControlsEditor';

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
      gridTemplateColumns: '40px 60px 1fr',
      gridTemplateAreas: `
        "flag     language translation"
        "controls controls controls   "
      `,
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
      handleEdit();
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

        <CellStateBar state={state} onResize={handleResize} />

        <div className={classes.controls}>
          {!isEditing &&
            (active ? (
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
            ))}
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
            <ControlsEditor
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
