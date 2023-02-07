import React, { useRef } from 'react';
import clsx from 'clsx';

import { components } from 'tg.service/apiSchema.generated';
import { useEditableRow } from '../useEditableRow';
import { TranslationVisual } from '../TranslationVisual';
import { useTranslationsActions } from '../context/TranslationsContext';
import {
  StyledCell,
  CELL_PLAIN,
  CELL_HOVER,
  CELL_CLICKABLE,
  CELL_RAISED,
} from '../cell/styles';
import { CellStateBar } from '../cell/CellStateBar';
import { ControlsTranslation } from '../cell/ControlsTranslation';
import { TranslationOpened } from '../TranslationOpened';
import { AutoTranslationIndicator } from '../cell/AutoTranslationIndicator';
import { StateType } from 'tg.constants/translationStates';
import { styled } from '@mui/material';

type LanguageModel = components['schemas']['LanguageModel'];
type KeyWithTranslationsModel =
  components['schemas']['KeyWithTranslationsModel'];

const StyledContainer = styled(StyledCell)`
  display: flex;
  flex-direction: column;
  position: relative;
`;

const StyledTranslationOpened = styled(TranslationOpened)`
  overflow: hidden;
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  padding-left: 4px;
`;

const StyledAutoIndicator = styled(AutoTranslationIndicator)`
  height: 0;
  position: relative;
`;

const StyledTranslation = styled('div')`
  flex-grow: 1;
  margin: ${({ theme }) => theme.spacing(1.5, 1.5, 1, 1.5)};
`;

const StyledControls = styled('div')`
  box-sizing: border-box;
  grid-area: controls;
  display: flex;
  justify-content: flex-end;
  overflow: hidden;
  min-height: 44px;
  padding: 12px 14px 12px 12px;
  margin-top: -16px;
`;

type Props = {
  data: KeyWithTranslationsModel;
  language: LanguageModel;
  colIndex?: number;
  onResize?: (colIndex: number) => void;
  editEnabled: boolean;
  stateChangeEnabled: boolean;
  width?: number | string;
  cellPosition: string;
  active: boolean;
  lastFocusable: boolean;
  containerRef: React.RefObject<HTMLDivElement>;
  className?: string;
};

export const CellTranslation: React.FC<Props> = ({
  data,
  language,
  colIndex,
  onResize,
  editEnabled,
  stateChangeEnabled,
  width,
  cellPosition,
  active,
  lastFocusable,
  containerRef,
  className,
}) => {
  const cellRef = useRef<HTMLDivElement>(null);

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
    isEditingRow,
  } = useEditableRow({
    keyId: data.keyId,
    keyName: data.keyName,
    defaultVal: translation?.text || '',
    language: language.tag,
    cellRef,
  });
  const { setTranslationState } = useTranslationsActions();

  const handleStateChange = (state: StateType) => {
    setTranslationState({
      keyId: data.keyId,
      translationId: translation?.id as number,
      language: language.tag as string,
      state,
    });
  };

  const handleResize = () => {
    onResize?.(colIndex || 0);
  };

  const showAllLines = isEditing || (language.base && isEditingRow);

  return (
    <StyledContainer
      position={lastFocusable ? 'right' : undefined}
      className={clsx({
        [CELL_PLAIN]: true,
        [CELL_HOVER]: !isEditing,
        [CELL_CLICKABLE]: editEnabled && !isEditing,
        [CELL_RAISED]: isEditing,
      })}
      style={{ width }}
      onClick={
        editEnabled && !isEditing ? () => handleOpen('editor') : undefined
      }
      tabIndex={0}
      ref={cellRef}
    >
      {editVal ? (
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
          stateChangeEnabled={stateChangeEnabled}
          cellRef={containerRef}
          cellPosition={cellPosition}
        />
      ) : (
        <>
          <StyledTranslation className={className}>
            <div data-cy="translations-table-cell">
              <TranslationVisual
                width={width}
                text={isEditing ? value : translation?.text}
                locale={language.tag}
                limitLines={!showAllLines}
              />
            </div>

            <StyledAutoIndicator keyData={data} lang={language.tag} />
          </StyledTranslation>

          <StyledControls>
            {active ? (
              <ControlsTranslation
                onEdit={() => handleOpen('editor')}
                editEnabled={editEnabled}
                state={state}
                stateChangeEnabled={stateChangeEnabled}
                onStateChange={handleStateChange}
                onComments={() => handleOpen('comments')}
                commentsCount={translation?.commentCount}
                unresolvedCommentCount={translation?.unresolvedCommentCount}
              />
            ) : (
              // hide as many components as possible in order to be performant
              <ControlsTranslation
                commentsCount={translation?.commentCount}
                unresolvedCommentCount={translation?.unresolvedCommentCount}
                lastFocusable={lastFocusable}
              />
            )}
          </StyledControls>
        </>
      )}

      <CellStateBar state={state} onResize={handleResize} />
    </StyledContainer>
  );
};
