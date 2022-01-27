import { useRef } from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/core';

import { components } from 'tg.service/apiSchema.generated';
import { StateType } from 'tg.constants/translationStates';
import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';
import { useCellStyles } from '../cell/styles';
import { useEditableRow } from '../useEditableRow';
import { useTranslationsDispatch } from '../context/TranslationsContext';
import { TranslationVisual } from '../TranslationVisual';
import { CellStateBar } from '../cell/CellStateBar';
import { ControlsTranslation } from '../cell/ControlsTranslation';
import { TranslationOpened } from '../TranslationOpened';
import { AutoTranslationIndicator } from '../cell/AutoTranslationIndicator';

type LanguageModel = components['schemas']['LanguageModel'];
type KeyWithTranslationsModel =
  components['schemas']['KeyWithTranslationsModel'];
type TranslationViewModel = components['schemas']['TranslationViewModel'];

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
      gridTemplateRows: '1fr auto',
      gridTemplateAreas: `
        "flag     language translation  "
        "controls controls controls     "
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
    },
    translation: {
      gridArea: 'translation',
      margin: '12px 12px 8px 0px',
    },
    translationContent: {
      overflow: 'hidden',
      position: 'relative',
    },
    autoIndicator: {
      position: 'relative',
      height: 0,
      justifySelf: 'start',
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
  const cellRef = useRef<HTMLDivElement>(null);
  const cellClasses = useCellStyles({ position: 'right' });
  const dispatch = useTranslationsDispatch();

  const translation = data.translations[language.tag] as
    | TranslationViewModel
    | undefined;

  const {
    isEditing,
    editVal,
    value,
    setValue,
    handleOpen,
    handleClose,
    handleSave,
    handleModeChange,
    autofocus,
  } = useEditableRow({
    keyId: data.keyId,
    keyName: data.keyName,
    defaultVal: translation?.text,
    language: language.tag,
    cellRef: cellRef,
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
    onResize?.(colIndex || 0);
  };

  const toggleEdit = () => {
    if (isEditing) {
      handleClose();
    } else {
      handleOpen('editor');
    }
  };

  const state = translation?.state || 'UNTRANSLATED';

  return (
    <div
      className={clsx({
        [cellClasses.cellPlain]: true,
        [classes.splitContainer]: true,
        [cellClasses.scrollMargins]: true,
        [cellClasses.cellRaised]: isEditing,
      })}
      tabIndex={0}
      ref={cellRef}
    >
      <div
        className={clsx({
          [classes.container]: true,
          [cellClasses.hover]: !isEditing,
          [cellClasses.cellClickable]: editEnabled,
          [cellClasses.cellSelected]: isEditing,
        })}
        style={{ width }}
        onClick={editEnabled ? () => toggleEdit() : undefined}
        data-cy="translations-table-cell"
      >
        <CircledLanguageIcon
          flag={language.flagEmoji}
          className={classes.flag}
        />

        <div
          className={classes.language}
          data-cy="translations-table-cell-language"
        >
          {language.tag}
        </div>

        <div className={classes.translation}>
          <div className={classes.translationContent}>
            <TranslationVisual
              width={width}
              text={isEditing ? value : translation?.text}
              locale={language.tag}
              limitLines={!isEditing}
            />
          </div>
          <AutoTranslationIndicator
            keyData={data}
            lang={language.tag}
            className={classes.autoIndicator}
          />
        </div>

        <CellStateBar state={state} onResize={handleResize} />

        <div className={classes.controls}>
          {!isEditing &&
            (active ? (
              <ControlsTranslation
                onEdit={() => handleOpen('editor')}
                onComments={() => handleOpen('comments')}
                commentsCount={translation?.commentCount}
                unresolvedCommentCount={translation?.unresolvedCommentCount}
                editEnabled={editEnabled}
                state={state}
                onStateChange={handleStateChange}
              />
            ) : (
              // hide as many components as possible in order to be performant
              <ControlsTranslation
                lastFocusable={lastFocusable}
                commentsCount={translation?.commentCount}
                unresolvedCommentCount={translation?.unresolvedCommentCount}
              />
            ))}
        </div>
      </div>

      {editVal && (
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
          cellRef={cellRef}
        />
      )}
    </div>
  );
};
