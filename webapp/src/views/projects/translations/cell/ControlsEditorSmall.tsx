import React from 'react';
import { useTranslate } from '@tolgee/react';
import { Box, styled } from '@mui/material';
import { Code01, ClipboardCheck, Copy06 } from '@untitled-ui/icons-react';

import { components } from 'tg.service/apiSchema.generated';
import { StateInType } from 'tg.constants/translationStates';
import { ControlsButton } from './ControlsButton';
import { StateTransitionButtons } from './StateTransitionButtons';
import { useTranslationsSelector } from '../context/TranslationsContext';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useProject } from 'tg.hooks/useProject';
import { useTaskTransitionTranslation } from 'tg.translationTools/useTaskTransitionTranslation';

type State = components['schemas']['TranslationViewModel']['state'];
type TaskModel = components['schemas']['KeyTaskViewModel'];

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
  tasks?: TaskModel[];
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
  tasks,
  onTaskStateChange,
}) => {
  const project = useProject();
  const { t } = useTranslate();
  const displayTransitionButtons = state && stateChangeEnabled;
  const { satisfiesLanguageAccess } = useProjectPermissions();
  const baseLanguage = useTranslationsSelector((c) =>
    c.languages?.find((l) => l.base)
  );
  const translateTransition = useTaskTransitionTranslation();
  const displayInsertBase =
    onInsertBase &&
    !isBaseLanguage &&
    satisfiesLanguageAccess('translations.view', baseLanguage?.id);

  const task = tasks?.[0];

  const prefilteredTask = useTranslationsSelector((c) => c.prefilter?.task);
  const displayTaskButton =
    task && task.number === prefilteredTask && task.userAssigned;

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
            <Code01 />
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
            <Copy06 />
          </ControlsButton>
        )}

        {displayTaskButton && onTaskStateChange && (
          <ControlsButton
            style={{ gridArea: 'task' }}
            data-cy="translations-cell-task-button"
            onClick={() => onTaskStateChange(!task?.done)}
            color={task?.done ? 'secondary' : 'primary'}
            tooltip={translateTransition(task.type, task.done)}
          >
            <ClipboardCheck />
          </ControlsButton>
        )}
      </StyledIcons>
    </StyledContainer>
  );
};
