import clsx from 'clsx';
import { styled } from '@mui/material';

import { useTranslationCell } from '../useTranslationCell';
import { TranslationVisual } from '../translationVisual/TranslationVisual';
import { ControlsTranslation } from '../cell/ControlsTranslation';
import { TranslationFlags } from '../cell/TranslationFlags';

const StyledContainer = styled('div')`
  display: grid;
  grid-template-columns: auto 1fr;
  grid-template-rows: 1fr auto;
  grid-template-areas:
    'translation translation '
    'flags       controls  ';

  .flags {
    padding: 0px 12px 4px 16px;
    grid-area: flags;
    display: flex;
    align-items: center;
  }

  .controls {
    padding: 12px 12px 12px 12px;
    grid-area: controls;
    justify-self: end;
  }
`;

const StyledTranslation = styled('div')`
  grid-area: translation;
  min-height: 23px;
  margin: 8px 12px 0px 16px;
  position: relative;
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
    isEditingRow,
    editingLanguageTag,
    handleOpen,
    handleClose,
    setState: handleStateChange,
    translation,
    language,
    canChangeState,
    editEnabled,
    keyData,
    setAssignedTaskState,
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
      <StyledTranslation>
        <TranslationVisual
          width={width}
          text={translation?.text}
          locale={language.tag}
          targetLocale={editingLanguageTag}
          disabled={disabled}
          showHighlights={isEditingRow && language.base}
          isPlural={keyData.keyIsPlural}
        />
      </StyledTranslation>
      <TranslationFlags
        className="flags"
        keyData={keyData}
        lang={language.tag}
      />
      <ControlsTranslation
        onEdit={() => handleOpen()}
        onComments={() => handleOpen('comments')}
        commentsCount={translation?.commentCount}
        unresolvedCommentCount={translation?.unresolvedCommentCount}
        stateChangeEnabled={canChangeState}
        editEnabled={editable}
        state={state}
        onStateChange={handleStateChange}
        active={active}
        lastFocusable={lastFocusable}
        className="controls"
        tasks={keyData.tasks?.filter((t) => t.languageTag === language.tag)}
        onTaskStateChange={setAssignedTaskState}
      />
    </StyledContainer>
  );
};
