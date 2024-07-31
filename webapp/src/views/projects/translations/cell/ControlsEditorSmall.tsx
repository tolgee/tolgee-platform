import React from 'react';
import { useTranslate } from '@tolgee/react';
import { Box, styled } from '@mui/material';
import { Code, ContentCopy, Task } from '@mui/icons-material';

import { components } from 'tg.service/apiSchema.generated';
import { StateInType } from 'tg.constants/translationStates';
import { ControlsButton } from './ControlsButton';
import { StateTransitionButtons } from './StateTransitionButtons';
import { useTranslationsSelector } from '../context/TranslationsContext';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useProject } from 'tg.hooks/useProject';

type State = components['schemas']['TranslationViewModel']['state'];

const StyledContainer = styled(Box)`
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
`;

const StyledIcons = styled('div')`
  display: flex;
  gap: 12px;
  padding-right: 4px;
`;

type ControlsProps = {
  state?: State;
  mode?: 'placeholders' | 'syntax';
  isBaseLanguage?: boolean;
  stateChangeEnabled?: boolean;
  onInsertBase?: () => void;
  onStateChange?: (state: StateInType) => void;
  onModeToggle?: () => void;
  controlsProps?: React.ComponentProps<typeof Box>;
  taskId?: number;
  onTaskStateChange?: (done: boolean) => void;
};

export const ControlsEditorSmall: React.FC<ControlsProps> = ({
  state,
  mode,
  isBaseLanguage,
  stateChangeEnabled,
  onInsertBase,
  onModeToggle,
  onStateChange,
  controlsProps,
  taskId,
  onTaskStateChange,
}) => {
  const project = useProject();
  const { t } = useTranslate();
  const displayTransitionButtons = state && stateChangeEnabled;
  const { satisfiesLanguageAccess } = useProjectPermissions();
  const baseLanguage = useTranslationsSelector((c) =>
    c.languages?.find((l) => l.base)
  );
  const displayInsertBase =
    onInsertBase &&
    !isBaseLanguage &&
    satisfiesLanguageAccess('translations.view', baseLanguage?.id);

  const displayTaskButton = typeof taskId === 'number';

  const displayEditorMode = project.icuPlaceholders;

  return (
    <StyledContainer {...controlsProps}>
      <StyledIcons>
        {displayTransitionButtons && (
          <StateTransitionButtons state={state} onStateChange={onStateChange} />
        )}
        {onModeToggle && displayEditorMode && (
          <ControlsButton
            onClick={onModeToggle}
            onMouseDown={(e) => {
              e.preventDefault();
            }}
            color={mode === 'placeholders' ? 'default' : 'primary'}
            data-cy="translations-cell-switch-mode"
            tooltip={
              mode === 'placeholders'
                ? t('translations_editor_switch_show_code')
                : t('translations_editor_switch_hide_code')
            }
          >
            <Code fontSize="small" />
          </ControlsButton>
        )}

        {displayInsertBase && (
          <ControlsButton
            onClick={onInsertBase}
            onMouseDown={(e) => {
              e.preventDefault();
            }}
            color="default"
            data-cy="translations-cell-insert-base-button"
            tooltip={t('translations_cell_insert_base')}
          >
            <ContentCopy fontSize="small" />
          </ControlsButton>
        )}

        {displayTaskButton && onTaskStateChange && (
          <ControlsButton
            style={{ gridArea: 'task' }}
            data-cy="translations-cell-task-button"
            tooltip={t('translation_cell_task')}
            onClick={() => onTaskStateChange(true)}
          >
            <Task fontSize="small" />
          </ControlsButton>
        )}
      </StyledIcons>
    </StyledContainer>
  );
};
