import { useRef } from 'react';
import clsx from 'clsx';
import { styled, Box } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { StateType } from 'tg.constants/translationStates';
import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';
import {
  StyledCell,
  CELL_CLICKABLE,
  CELL_HOVER,
  CELL_PLAIN,
  CELL_RAISED,
  CELL_SELECTED,
} from '../cell/styles';
import { useEditableRow } from '../useEditableRow';
import { useTranslationsActions } from '../context/TranslationsContext';
import { TranslationVisual } from '../TranslationVisual';
import { CellStateBar } from '../cell/CellStateBar';
import { ControlsTranslation } from '../cell/ControlsTranslation';
import { TranslationOpened } from '../TranslationOpened';
import { TranslationFlags } from '../cell/TranslationFlags';

type LanguageModel = components['schemas']['LanguageModel'];
type KeyWithTranslationsModel =
  components['schemas']['KeyWithTranslationsModel'];
type TranslationViewModel = components['schemas']['TranslationViewModel'];

const StyledWrapper = styled(StyledCell)`
  &.splitContainer {
    display: flex;
    flex-grow: 1;
    position: relative;
  }
`;

const StyledContainer = styled('div')`
  flex-basis: 50%;
  flex-grow: 1;
  position: relative;
  display: grid;
  grid-template-columns: 40px 60px 1fr;
  grid-template-rows: 1fr auto;
  grid-template-areas:
    'flag     language translation  '
    'controls controls controls     ';
`;

const StyledAutoTranslationIndicator = styled(TranslationFlags)`
  position: relative;
  height: 0px;
  justify-self: start;
`;

const StyledTranslation = styled('div')`
  grid-area: translation;
  margin: 12px 12px 8px 0px;
`;

const StyledTranslationContent = styled('div')`
  position: relative;
`;

const StyledLanguage = styled(Box)`
  margin: 12px 8px 8px 0px;
`;

const StyledCircledLanguageIcon = styled(CircledLanguageIcon)`
  grid-area: flag;
  margin: 12px 8px 8px 12px;
  width: 20px;
  height: 20px;
  padding: 1px;
`;

const StyledTranslationOpened = styled(TranslationOpened)`
  overflow: hidden;
  flex-basis: 50%;
  flex-grow: 1;
`;

type Props = {
  data: KeyWithTranslationsModel;
  language: LanguageModel;
  colIndex?: number;
  onResize?: (colIndex: number) => void;
  editEnabled: boolean;
  width?: number | string;
  active: boolean;
  lastFocusable: boolean;
  className?: string;
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
  className,
}) => {
  const cellRef = useRef<HTMLDivElement>(null);
  const { setTranslationState } = useTranslationsActions();

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
    isEditingRow,
  } = useEditableRow({
    keyId: data.keyId,
    keyName: data.keyName,
    defaultVal: translation?.text,
    language: language.tag,
    cellRef: cellRef,
  });

  const handleStateChange = (state: StateType) => {
    setTranslationState({
      keyId: data.keyId,
      language: language.tag,
      translationId: data.translations[language.tag]?.id,
      state,
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

  const showAllLines = isEditing || (language.base && isEditingRow);

  const state = translation?.state || 'UNTRANSLATED';

  return (
    <StyledWrapper
      position="right"
      className={clsx({
        [CELL_PLAIN]: true,
        [CELL_RAISED]: isEditing,
        splitContainer: true,
      })}
      tabIndex={0}
      ref={cellRef}
    >
      <StyledContainer
        className={clsx(
          {
            [CELL_HOVER]: !isEditing,
            [CELL_CLICKABLE]: editEnabled,
            [CELL_SELECTED]: isEditing,
          },
          className
        )}
        style={{ width }}
        onClick={editEnabled ? () => toggleEdit() : undefined}
        data-cy="translations-table-cell"
      >
        <StyledCircledLanguageIcon flag={language.flagEmoji} />

        <StyledLanguage
          data-cy="translations-table-cell-language"
          sx={{ fontWeight: language.base ? 'bold' : 'normal' }}
        >
          {language.tag}
        </StyledLanguage>

        <StyledTranslation>
          <StyledTranslationContent>
            <TranslationVisual
              width={width}
              text={isEditing ? value : translation?.text}
              locale={language.tag}
              limitLines={!showAllLines}
            />
          </StyledTranslationContent>
          <StyledAutoTranslationIndicator keyData={data} lang={language.tag} />
        </StyledTranslation>

        <CellStateBar state={state} onResize={handleResize} />

        {!isEditing && (
          <ControlsTranslation
            onEdit={() => handleOpen('editor')}
            onComments={() => handleOpen('comments')}
            commentsCount={translation?.commentCount}
            unresolvedCommentCount={translation?.unresolvedCommentCount}
            editEnabled={editEnabled}
            state={state}
            onStateChange={handleStateChange}
            active={active}
            lastFocusable={lastFocusable}
          />
        )}
      </StyledContainer>

      {editVal && (
        <StyledTranslationOpened
          keyData={data}
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
    </StyledWrapper>
  );
};
