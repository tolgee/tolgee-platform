import clsx from 'clsx';
import { styled } from '@mui/material';

import { useTranslationCell } from '../useTranslationCell';
import { TranslationVisual } from '../translationVisual/TranslationVisual';
import { ControlsTranslation } from '../cell/ControlsTranslation';
import { TranslationLanguage } from './TranslationLanguage';
import { AiPlaygroundPreview } from '../translationVisual/AiPlaygroundPreview';

const StyledContainer = styled('div')`
  display: grid;
  grid-template-columns: auto 1fr;
  grid-template-rows: auto 1fr auto;
  grid-template-areas:
    'language    controls-t '
    'translation translation '
    'controls-b  controls-b  ';

  .language {
    align-self: start;
    padding: 12px 12px 4px 16px;
  }

  .controls-t {
    padding-right: 6px;
    padding-top: 12px;
    grid-area: controls-t;
    justify-self: end;
  }

  .controls-b {
    padding: 0px 12px 4px 12px;
    grid-area: controls-b;
  }
`;

const StyledTranslation = styled('div')`
  display: grid;
  grid-auto-rows: max-content;
  grid-area: translation;
  min-height: 23px;
  margin: 0px 12px 16px 16px;
  position: relative;
  gap: 8px;
`;

type Props = {
  colIndex?: number;
  width?: number | string;
  active: boolean;
  lastFocusable: boolean;
  className?: string;
  tools: ReturnType<typeof useTranslationCell>;
};

export const TranslationRead: React.FC<Props> = ({
  width,
  active,
  lastFocusable,
  className,
  tools,
}) => {
  const {
    isEditing,
    handleOpen,
    handleClose,
    setState: handleStateChange,
    translation,
    language,
    canChangeState,
    keyData,
    editEnabled,
    setAssignedTaskState,
    aiPlaygroundData,
    aiPlaygroundEnabled,
  } = tools;

  const toggleEdit = () => {
    if (isEditing) {
      handleClose();
    } else {
      handleOpen();
    }
  };

  const state = translation?.state || 'UNTRANSLATED';

  const disabled = state === 'DISABLED';
  const editable = editEnabled && !disabled;

  return (
    <StyledContainer
      className={clsx(className)}
      data-cy="translations-table-cell"
      data-cy-language={language.tag}
      data-cy-key={keyData.keyName}
      onClick={editable && !isEditing ? () => toggleEdit() : undefined}
    >
      <TranslationLanguage
        language={language}
        keyData={keyData}
        className="language"
        inactive
      />

      {!aiPlaygroundEnabled && (
        <ControlsTranslation
          onEdit={() => handleOpen()}
          onComments={() => handleOpen('comments')}
          commentsCount={translation?.commentCount}
          tasks={keyData.tasks?.filter((t) => t.languageTag === language.tag)}
          onTaskStateChange={setAssignedTaskState}
          unresolvedCommentCount={translation?.unresolvedCommentCount}
          stateChangeEnabled={canChangeState}
          editEnabled={editable}
          state={state}
          onStateChange={handleStateChange}
          active={active}
          lastFocusable={lastFocusable}
          className="controls-t"
        />
      )}

      <StyledTranslation>
        <TranslationVisual
          width={width}
          text={translation?.text}
          locale={language.tag}
          disabled={disabled}
          isPlural={keyData.keyIsPlural}
        />
        {aiPlaygroundData && (
          <AiPlaygroundPreview
            translation={aiPlaygroundData.translation}
            contextDescription={aiPlaygroundData.contextDescription}
            isPlural={keyData.keyIsPlural}
            locale={language.tag}
          />
        )}
      </StyledTranslation>
    </StyledContainer>
  );
};
