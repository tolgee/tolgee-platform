import React from 'react';
import clsx from 'clsx';
import { Badge, Box, styled } from '@mui/material';
import {
  Check,
  MessageTextSquare02,
  Edit02,
  ClipboardCheck,
} from '@untitled-ui/icons-react';
import { useTranslate } from '@tolgee/react';

import { StateInType } from 'tg.constants/translationStates';
import { components } from 'tg.service/apiSchema.generated';
import { ControlsButton } from './ControlsButton';
import { StateTransitionButtons } from './StateTransitionButtons';
import { CELL_HIGHLIGHT_ON_HOVER, CELL_SHOW_ON_HOVER } from './styles';
import { useTranslationsSelector } from '../context/TranslationsContext';
import { useTaskTransitionTranslation } from 'tg.translationTools/useTaskTransitionTranslation';

type State = components['schemas']['TranslationViewModel']['state'];
type TaskModel = components['schemas']['KeyTaskViewModel'];

const StyledControlsWrapper = styled(Box)`
  display: grid;
  box-sizing: border-box;
  justify-content: end;
  padding: 0px 0px 0px 0px;
  gap: 4px;
  margin: 0px 0px;
`;

const StyledStateButtons = styled('div')`
  display: flex;
  justify-content: flex-end;
  padding-right: 8px;
`;

const StyledBadge = styled(Badge)`
  & .unresolved {
    font-size: 10px;
    height: unset;
    padding: 3px 3px;
    display: flex;
  }
  & .resolved {
    background: ${({ theme }) => theme.palette.emphasis[600]};
    padding: 0px;
    height: 16px;
    width: 18px;
    display: flex;
    min-width: unset;
    align-items: center;
    justify-content: center;
  }
`;

const StyledCheckIcon = styled(Check)`
  color: ${({ theme }) => theme.palette.emphasis[100]};
  width: 14px !important;
  height: 14px !important;
  margin: -5px;
`;

type ControlsProps = {
  state?: State;
  editEnabled?: boolean;
  stateChangeEnabled?: boolean;
  onEdit?: () => void;
  onStateChange?: (state: StateInType) => void;
  onComments?: () => void;
  commentsCount: number | undefined;
  tasks: TaskModel[] | undefined;
  onTaskStateChange: (done: boolean) => void;
  unresolvedCommentCount: number | undefined;
  // render last focusable button
  lastFocusable: boolean;
  active?: boolean;
  containerProps?: React.ComponentProps<typeof Box>;
  className?: string;
};

export const ControlsTranslation: React.FC<ControlsProps> = ({
  state,
  editEnabled,
  stateChangeEnabled,
  onEdit,
  onStateChange,
  onComments,
  tasks,
  onTaskStateChange,
  commentsCount,
  unresolvedCommentCount,
  lastFocusable,
  active,
  className,
}) => {
  const spots: string[] = [];

  const translateTransition = useTaskTransitionTranslation();
  const displayTransitionButtons = stateChangeEnabled && state;
  const displayEdit = editEnabled && onEdit;
  const commentsPresent = Boolean(commentsCount);
  const displayComments = onComments || commentsPresent;
  const onlyResolved = commentsPresent && !unresolvedCommentCount;
  const prefilteredTask = useTranslationsSelector((c) => c.prefilter?.task);
  const task = tasks?.[0];
  const displayTaskButton =
    task && task.number === prefilteredTask && task.userAssigned;

  if (displayTransitionButtons) {
    spots.push('state');
  }
  if (displayEdit) {
    spots.push('edit');
  }
  if (displayComments) {
    spots.push('comments');
  }
  if (displayTaskButton) {
    spots.push('task');
  }

  const inDomTransitionButtons = displayTransitionButtons && active;
  const inDomEdit = displayEdit && active;
  const inDomComments = displayComments || active || lastFocusable;
  const inDomTask = displayTaskButton;

  const gridTemplateAreas = `'${spots.join(' ')}'`;
  const gridTemplateColumns = spots
    .map((spot) => (spot === 'state' ? 'auto' : '28px'))
    .join(' ');

  const { t } = useTranslate();

  return (
    <StyledControlsWrapper
      style={{
        gridTemplateAreas,
        gridTemplateColumns,
      }}
      className={className}
    >
      {inDomTransitionButtons && (
        <StyledStateButtons style={{ gridArea: 'state' }}>
          <StateTransitionButtons
            state={state}
            onStateChange={onStateChange}
            className={CELL_SHOW_ON_HOVER}
          />
        </StyledStateButtons>
      )}
      {inDomEdit && (
        <ControlsButton
          style={{ gridArea: 'edit' }}
          onClick={onEdit}
          data-cy="translations-cell-edit-button"
          className={CELL_SHOW_ON_HOVER}
          tooltip={t('translations_cell_edit')}
        >
          <Edit02 />
        </ControlsButton>
      )}
      {inDomComments && (
        <ControlsButton
          style={{ gridArea: 'comments' }}
          onClick={onComments}
          data-cy="translations-cell-comments-button"
          className={clsx({
            [CELL_SHOW_ON_HOVER]: !commentsPresent,
            [CELL_HIGHLIGHT_ON_HOVER]: onlyResolved,
          })}
          tooltip={t('translation_cell_comments')}
        >
          {onlyResolved ? (
            <StyledBadge
              badgeContent={<StyledCheckIcon />}
              classes={{
                badge: 'resolved',
              }}
            >
              <MessageTextSquare02 />
            </StyledBadge>
          ) : (
            <StyledBadge
              badgeContent={unresolvedCommentCount}
              color="primary"
              classes={{ badge: 'unresolved' }}
            >
              <MessageTextSquare02 />
            </StyledBadge>
          )}
        </ControlsButton>
      )}
      {inDomTask && (
        <ControlsButton
          style={{ gridArea: 'task' }}
          onClick={() => onTaskStateChange(!task?.done)}
          data-cy="translations-cell-task-button"
          color={
            task?.userAssigned
              ? task?.done
                ? 'secondary'
                : 'primary'
              : undefined
          }
          tooltip={translateTransition(task.type, task.done)}
        >
          <ClipboardCheck />
        </ControlsButton>
      )}
    </StyledControlsWrapper>
  );
};
