import { useRef, useState } from 'react';
import { EditorView } from 'codemirror';
import { styled } from '@mui/material';
import { Placeholder } from '@tginternal/editor';

import { ControlsEditorMain } from '../cell/ControlsEditorMain';
import { ControlsEditorSmall } from '../cell/ControlsEditorSmall';
import { useTranslationsSelector } from '../context/TranslationsContext';
import { useTranslationCell } from '../useTranslationCell';
import { TranslationEditor } from '../TranslationEditor';
import { MissingPlaceholders } from '../cell/MissingPlaceholders';
import { useMissingPlaceholders } from '../cell/useMissingPlaceholders';
import { TranslationVisual } from '../translationVisual/TranslationVisual';
import { ControlsEditorReadOnly } from '../cell/ControlsEditorReadOnly';
import { useBaseTranslation } from '../useBaseTranslation';

const StyledContainer = styled('div')`
  display: grid;
`;

const StyledEditor = styled('div')`
  padding: 12px 12px 12px 16px;
`;

const StyledBottom = styled('div')`
  display: flex;
  padding: 4px 12px 12px 16px;
  flex-wrap: wrap;
  gap: 14px;
  align-items: center;
`;

const StyledControls = styled('div')`
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  flex-grow: 1;
`;

type Props = {
  tools: ReturnType<typeof useTranslationCell>;
};

export const TranslationWrite: React.FC<Props> = ({ tools }) => {
  const {
    value,
    keyData,
    translation,
    language,
    canChangeState,
    setState,
    handleSave,
    handleClose,
    handleInsertBase,
    editEnabled,
    disabled,
    baseText,
    setAssignedTaskState,
  } = tools;
  const editVal = tools.editVal!;
  const state = translation?.state || 'UNTRANSLATED';
  const activeVariant = editVal.activeVariant;

  const [mode, setMode] = useState<'placeholders' | 'syntax'>('placeholders');
  const editorRef = useRef<EditorView>(null);
  const baseLanguage = useTranslationsSelector((c) => c.baseLanguage);
  const nested = Boolean(editVal.value.parameter);

  const baseTranslation = useBaseTranslation(
    activeVariant,
    baseText,
    keyData.keyIsPlural
  );

  const missingPlaceholders = useMissingPlaceholders({
    baseTranslation,
    currentTranslation: value,
    nested,
    enabled: baseLanguage !== language.tag,
  });

  const handleModeToggle = () => {
    setMode((mode) => (mode === 'syntax' ? 'placeholders' : 'syntax'));
  };

  const handlePlaceholderClick = (placeholder: Placeholder) => {
    if (editorRef.current) {
      const state = editorRef.current.state;
      const selection = state.selection;
      const placeholderText = placeholder.normalizedValue || '';
      const transactions = selection.ranges.map((value) =>
        state.update({
          changes: {
            from: value.from,
            to: value.to,
            insert: placeholderText,
          },
          selection: {
            anchor: value.from + placeholderText.length,
          },
        })
      );
      editorRef.current.update(transactions);
    }
  };

  return (
    <StyledContainer>
      <StyledEditor onMouseDown={(e) => e.preventDefault()}>
        {editEnabled ? (
          <TranslationEditor tools={tools} editorRef={editorRef} mode={mode} />
        ) : (
          <TranslationVisual
            text={translation?.text || ''}
            locale={language.tag}
            isPlural={keyData.keyIsPlural}
            disabled={disabled}
          />
        )}
      </StyledEditor>

      <StyledBottom>
        {Boolean(missingPlaceholders.length) && (
          <MissingPlaceholders
            placeholders={missingPlaceholders}
            onPlaceholderClick={handlePlaceholderClick}
            variant={editVal.value.parameter ? activeVariant : undefined}
            locale={language.tag}
            className="placeholders"
          />
        )}

        <StyledControls>
          <ControlsEditorSmall
            controlsProps={{
              onMouseDown: (e) => e.preventDefault(),
            }}
            state={state}
            mode={mode}
            isBaseLanguage={language.base}
            stateChangeEnabled={canChangeState}
            onInsertBase={editEnabled ? handleInsertBase : undefined}
            onStateChange={setState}
            onModeToggle={editEnabled ? handleModeToggle : undefined}
            tasks={keyData.tasks?.filter((t) => t.languageTag === language.tag)}
            onTaskStateChange={setAssignedTaskState}
          />
          {editEnabled ? (
            <ControlsEditorMain
              onSave={handleSave}
              onCancel={() => handleClose(true)}
              tasks={keyData.tasks?.filter(
                (t) => t.languageTag === language.tag
              )}
            />
          ) : (
            <ControlsEditorReadOnly onClose={() => handleClose(true)} />
          )}
        </StyledControls>
      </StyledBottom>
    </StyledContainer>
  );
};
