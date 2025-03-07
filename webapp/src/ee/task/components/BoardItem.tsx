import { Link } from 'react-router-dom';
import { useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { Box, IconButton, styled, Tooltip } from '@mui/material';
import { AlarmClock, DotsVertical, InfoCircle } from '@untitled-ui/icons-react';

import { stopAndPrevent } from 'tg.fixtures/eventHandler';
import { Scope } from 'tg.fixtures/permissions';
import { components } from 'tg.service/apiSchema.generated';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { BatchProgress } from 'tg.views/projects/translations/BatchOperations/OperationsSummary/BatchProgress';

import { TaskLabel } from './TaskLabel';
import { TaskMenu } from './TaskMenu';
import { TaskTypeChip } from 'tg.component/task/TaskTypeChip';
import { TaskState } from 'tg.component/task/TaskState';
import { TaskAssignees } from './TaskAssignees';
import { getTaskUrl } from 'tg.constants/links';

type TaskModel = components['schemas']['TaskModel'];
type SimpleProjectModel = components['schemas']['SimpleProjectModel'];

const StyledContainer = styled(Box)`
  display: grid;
  gap: 8px;
  border: 1px solid ${({ theme }) => theme.palette.tokens.border.secondary};
  border-radius: 8px;
  padding: 20px;
  text-decoration: none;
  color: ${({ theme }) => theme.palette.text.primary};
  .showOnHover {
    opacity: 0;
    transition: opacity ease-in-out 0.3s;
  }
  &:hover .showOnHover,
  &:focus-within .showOnHover {
    opacity: 1;
  }

  &:hover,
  &:focus-within {
    background: ${({ theme }) => theme.palette.tokens.background.hover};
  }
`;

const StyledProgress = styled(Box)`
  display: grid;
  width: 80px;
  gap: 24px;
  color: ${({ theme }) => theme.palette.tokens.icon.secondary};
  text-align: right;
`;

const StyledRow = styled(Box)`
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
`;

const StyledSecondaryItem = styled(Box)`
  display: flex;
  color: ${({ theme }) => theme.palette.tokens.icon.secondary};
`;

type Props = {
  task: TaskModel;
  project: SimpleProjectModel;
  projectScopes?: Scope[];
  onDetailOpen: (task: TaskModel) => void;
  newTaskActions: boolean;
};

export const BoardItem = ({
  task,
  project,
  projectScopes,
  onDetailOpen,
  newTaskActions,
}: Props) => {
  const { t } = useTranslate();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const formatDate = useDateFormatter();

  return (
    <StyledContainer
      component={Link}
      // @ts-ignore
      to={getTaskUrl(project.id, task.number)}
    >
      <StyledRow>
        <TaskLabel task={task} hideType />
        <Box
          display="flex"
          gap={0.1}
          marginRight={-1.8}
          className="showOnHover"
          style={{ opacity: anchorEl ? 1 : undefined }}
          onClick={stopAndPrevent()}
        >
          <Tooltip title={t('task_detail_tooltip')} disableInteractive>
            <IconButton
              size="small"
              onClick={stopAndPrevent(() => onDetailOpen(task))}
            >
              <InfoCircle />
            </IconButton>
          </Tooltip>
          <IconButton
            size="small"
            onClick={stopAndPrevent((e) => setAnchorEl(e.currentTarget))}
          >
            <DotsVertical />
          </IconButton>
          <TaskMenu
            task={task}
            project={project}
            projectScopes={projectScopes}
            anchorEl={anchorEl}
            onClose={() => setAnchorEl(null)}
            newTaskActions={newTaskActions}
          />
        </Box>
      </StyledRow>
      <StyledRow>
        <TaskTypeChip type={task.type} />
        <StyledProgress>
          {['IN_PROGRESS', 'NEW'].includes(task.state) ? (
            <BatchProgress progress={task.doneItems} max={task.totalItems} />
          ) : (
            <TaskState state={task.state} />
          )}
        </StyledProgress>
      </StyledRow>
      <StyledRow>
        <StyledSecondaryItem>
          {t('task_word_count', { value: task.baseWordCount })}
        </StyledSecondaryItem>
        <StyledSecondaryItem>
          {task.dueDate ? (
            <Box display="flex" alignItems="center" gap={0.5}>
              <AlarmClock style={{ width: 20, height: 20 }} />
              {formatDate(task.dueDate, { timeZone: 'UTC' })}
            </Box>
          ) : null}
        </StyledSecondaryItem>
        <Box>
          <TaskAssignees task={task} />
        </Box>
      </StyledRow>
    </StyledContainer>
  );
};
