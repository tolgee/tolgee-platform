import { useMemo, useRef, useState } from 'react';
import { Box, styled } from '@mui/material';
import { Placeholder, getTolgeeFormat } from '@tginternal/editor';

import { ControlsEditorMain } from '../cell/ControlsEditorMain';
import { ControlsEditorSmall } from '../cell/ControlsEditorSmall';
import { useTranslationsSelector } from '../context/TranslationsContext';
import { EditorView } from 'codemirror';
import { useTranslationCell } from '../useTranslationCell';
import { TranslationLanguage } from './TranslationLanguage';
import { TranslationEditor } from '../TranslationEditor';
import { MissingPlaceholders } from '../cell/MissingPlaceholders';
import { useMissingPlaceholders } from '../cell/useMissingPlaceholders';
import { TranslationVisual } from '../translationVisual/TranslationVisual';
import { ControlsEditorReadOnly } from '../cell/ControlsEditorReadOnly';

const StyledContainer = styled('div')`
  display: grid;
  grid-template-columns: auto 1fr;
  grid-template-areas:
    'language    controls-t '
    'editor      editor     '
    'controls-b  controls-b ';

  .language {
    align-self: start;
    padding: 12px 12px 4px 16px;
  }

  .editor {
    padding: 0px 12px 12px 16px;
    grid-area: editor;
  }

  .controls-t {
    grid-area: controls-t;
    justify-self: end;
    padding-right: 10px;
    padding-top: 12px;
  }
`;

const StyledBottom = styled(Box)`
  grid-area: controls-b;
  padding: 0px 12px 4px 16px;
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  .controls-main {
    flex-grow: 1;
    justify-content: end;
  }
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
  } = tools;
  const editVal = tools.editVal!;
  const state = translation?.state || 'UNTRANSLATED';
  const activeVariant = editVal.activeVariant;

  const [mode, setMode] = useState<'placeholders' | 'syntax'>('placeholders');
  const editorRef = useRef<EditorView>(null);
  const baseLanguage = useTranslationsSelector((c) => c.baseLanguage);
  const nested = Boolean(editVal.value.parameter);

  const baseTranslation = keyData.translations[baseLanguage]?.text;

  const baseVariant = useMemo(() => {
    if (activeVariant) {
      const variants = getTolgeeFormat(
        baseTranslation || '',
        keyData.keyIsPlural
      )?.variants;
      return variants?.[activeVariant] ?? variants?.['other'];
    } else {
      return baseTranslation;
    }
  }, [baseTranslation, activeVariant]);

  const missingPlaceholders = useMissingPlaceholders({
    baseTranslation: baseVariant,
    currentTranslation: value,
    nested,
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
      <TranslationLanguage
        keyData={keyData}
        language={language}
        className="language"
      />
      <ControlsEditorSmall
        controlsProps={{
          onMouseDown: (e) => e.preventDefault(),
          className: 'controls-t',
        }}
        state={state}
        mode={mode}
        isBaseLanguage={language.base}
        stateChangeEnabled={canChangeState}
        onInsertBase={editEnabled ? handleInsertBase : undefined}
        onStateChange={setState}
        onModeToggle={editEnabled ? handleModeToggle : undefined}
      />
      <Box onMouseDown={(e) => e.preventDefault()} className="editor">
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
      </Box>

      <StyledBottom onMouseDown={(e) => e.preventDefault()}>
        {editEnabled ? (
          <>
            <MissingPlaceholders
              placeholders={missingPlaceholders}
              onPlaceholderClick={handlePlaceholderClick}
              variant={editVal.value.parameter ? activeVariant : undefined}
              locale={language.tag}
            />
            <ControlsEditorMain
              className="controls-main"
              onSave={handleSave}
              onCancel={() => handleClose(true)}
            />
          </>
        ) : (
          <ControlsEditorReadOnly
            className="controls-main"
            onClose={() => handleClose(true)}
          />
        )}
      </StyledBottom>
    </StyledContainer>
  );
};