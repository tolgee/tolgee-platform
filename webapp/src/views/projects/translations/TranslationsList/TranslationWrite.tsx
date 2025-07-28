import { useRef, useState } from 'react';
import { Box, IconButton, Tooltip, styled } from '@mui/material';
import { Placeholder } from '@tginternal/editor';
import { useTranslate } from '@tolgee/react';
import { HelpCircle } from '@untitled-ui/icons-react';

import { TaskInfoMessage } from 'tg.ee';
import { ControlsEditorMain } from '../cell/editorMainActions/ControlsEditorMain';
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
import { useBaseTranslation } from '../useBaseTranslation';
import { TranslationLabels } from 'tg.views/projects/translations/TranslationsList/TranslationLabels';
import { SuggestionsList } from '../Suggestions/SuggestionsList';

const StyledContainer = styled('div')`
  display: grid;
  grid-template-columns: auto 1fr;
  grid-template-rows: auto 1fr auto;
  grid-template-areas:
    'language    labels      controls-t '
    'editor      editor      editor     '
    'controls-b  controls-b  controls-b '
    'suggestions suggestions suggestions';
  gap: 0 6px;

  .language {
    align-self: start;
    padding: 12px 2px 4px 16px;
  }

  .labels {
    padding: 6px 0 0 0;
    min-width: 0;
  }

  .editor {
    padding: 0px 12px 12px 16px;
    grid-area: editor;
    display: grid;
  }

  .controls-t {
    grid-area: controls-t;
    justify-self: end;
    padding-right: 10px;
    padding-top: 12px;
  }
  padding-bottom: 4px;
  margin-bottom: 8px;
`;

const StyledBottom = styled(Box)`
  grid-area: controls-b;
  padding: 0px 12px 0px 16px;
  display: grid;
  gap: 8px;
`;

const StyledSuggestions = styled(Box)`
  grid-area: suggestions;
  padding: 12px 12px 0px 16px;
  display: grid;
`;

const StyledControls = styled(Box)`
  display: flex;
  justify-content: space-between;
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
    setAssignedTaskState,
    addLabel,
    removeLabel,
    prefilteredTask,
    tasks,
  } = tools;
  const { t } = useTranslate();
  const editVal = tools.editVal!;
  const state = translation?.state || 'UNTRANSLATED';
  const activeVariant = editVal.activeVariant;

  const [mode, setMode] = useState<'placeholders' | 'syntax'>('placeholders');
  const editorRef = useRef<EditorView>(null);
  const baseLanguage = useTranslationsSelector((c) => c.baseLanguage);
  const nested = Boolean(editVal.value.parameter);

  const baseTranslation = useBaseTranslation(
    activeVariant,
    keyData.translations[baseLanguage]?.text,
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
      <TranslationLanguage
        keyData={keyData}
        language={language}
        className="language"
      />
      <TranslationLabels
        labels={translation?.labels}
        onSelect={(label) => addLabel(label.id)}
        onDelete={(labelId) => removeLabel(labelId)}
        className="labels"
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
        tasks={keyData.tasks?.filter((t) => t.languageTag === language.tag)}
        onTaskStateChange={setAssignedTaskState}
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
            showHighlights={language.base}
          />
        )}
      </Box>

      <StyledBottom onMouseDown={(e) => e.preventDefault()}>
        {editEnabled ? (
          <>
            <TaskInfoMessage tasks={tasks} currentTask={prefilteredTask} />
            <StyledControls>
              <Box display="flex" alignItems="center" gap="8px">
                <Tooltip title={t('translation_format_help')}>
                  <IconButton
                    style={{ margin: '-4px -4px -4px -6px' }}
                    target="_blank"
                    rel="noreferrer noopener"
                    href="https://tolgee.io/platform/projects_and_organizations/editing_translations"
                  >
                    <HelpCircle width={20} height={20} />
                  </IconButton>
                </Tooltip>
                <MissingPlaceholders
                  placeholders={missingPlaceholders}
                  onPlaceholderClick={handlePlaceholderClick}
                  variant={editVal.value.parameter ? activeVariant : undefined}
                  locale={language.tag}
                />
              </Box>
              <ControlsEditorMain
                className="controls-main"
                onSave={handleSave}
                onCancel={() => handleClose(true)}
                tasks={tasks}
                currentTask={prefilteredTask?.number}
                translation={translation}
                languageId={language.id}
                value={editVal.value}
              />
            </StyledControls>
          </>
        ) : (
          <ControlsEditorReadOnly
            className="controls-main"
            onClose={() => handleClose(true)}
          />
        )}
      </StyledBottom>
      {Boolean(translation?.totalSuggestionCount) && (
        <StyledSuggestions>
          <SuggestionsList
            translation={translation!}
            keyId={keyData.keyId}
            isPlural={keyData.keyIsPlural}
            locale={language.tag}
            languageTag={language.tag}
            languageId={language.id}
          />
        </StyledSuggestions>
      )}
    </StyledContainer>
  );
};
