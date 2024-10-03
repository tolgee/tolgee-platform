import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslate } from '@tolgee/react';
import { Box, IconButton, styled, Tooltip, useTheme } from '@mui/material';
import { AlarmClock, DotsVertical } from '@untitled-ui/icons-react';

import { TaskDetail } from 'tg.component/CustomIcons';
import { components } from 'tg.service/apiSchema.generated';
import { BatchProgress } from 'tg.views/projects/translations/BatchOperations/OperationsSummary/BatchProgress';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { Scope } from 'tg.fixtures/permissions';
import { TaskMenu } from './TaskMenu';
import { TaskLabel } from './TaskLabel';
import { getTaskRedirect } from './utils';
import { TaskState } from './TaskState';
import { stopAndPrevent } from 'tg.fixtures/eventHandler';
import { TaskAssignees } from './TaskAssignees';

type TaskModel = components['schemas']['TaskModel'];
type SimpleProjectModel = components['schemas']['SimpleProjectModel'];

const StyledContainer = styled('div')`
  display: contents;
  &:hover > * {
    background: ${({ theme }) => theme.palette.tokens.text._states.hover};
    cursor: pointer;
  }
`;

const StyledItem = styled(Box)`
  display: flex;
  align-items: center;
  align-self: stretch;
  justify-self: stretch;
  gap: 8px;
  color: ${({ theme }) => theme.palette.text.primary};
  text-decoration: none;
`;

const StyledProgress = styled(StyledItem)`
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: 24px;
  color: ${({ theme }) => theme.palette.tokens.icon.secondary};
`;

const StyledAssignees = styled(StyledItem)`
  justify-content: start;
  display: flex;
  flex-wrap: wrap;
  padding: 8px 0px;
`;

type Props = {
  task: TaskModel;
  onDetailOpen: (task: TaskModel) => void;
  project: SimpleProjectModel;
  projectScopes?: Scope[];
  showProject?: boolean;
  newTaskActions: boolean;
};

export const TaskItem = ({
  task,
  onDetailOpen,
  project,
  showProject,
  projectScopes,
  newTaskActions,
}: Props) => {
  const { t } = useTranslate();
  const theme = useTheme();
  const formatDate = useDateFormatter();

  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  const handleClose = () => {
    setAnchorEl(null);
  };

  const linkProps = {
    component: Link,
    to: getTaskRedirect(project, task.number),
  };

  return (
    <StyledContainer data-cy="task-item">
      <StyledItem {...linkProps}>
        <TaskLabel sx={{ padding: '12px 0px 12px 16px' }} task={task} />
      </StyledItem>
      <StyledItem
        {...linkProps}
        color={theme.palette.tokens.text.secondary}
        alignItems="center"
        justifyContent="center"
      >
        {t('task_keys_count', { value: task.totalItems })}
      </StyledItem>
      <StyledProgress {...linkProps}>
        {task.state === 'IN_PROGRESS' ? (
          <BatchProgress progress={task.doneItems} max={task.totalItems} />
        ) : (
          <TaskState state={task.state} />
        )}
        {task.dueDate ? (
          <Box display="flex" alignItems="center" gap={0.5}>
            <AlarmClock style={{ width: 20, height: 20 }} />
            {formatDate(task.dueDate, { timeZone: 'UTC' })}
          </Box>
        ) : null}
      </StyledProgress>
      {showProject && (
        <StyledItem {...linkProps}>
          <Tooltip title={<div>{project.name}</div>} disableInteractive>
            <div>
              <AvatarImg
                owner={{
                  name: project.name,
                  avatar: project.avatar,
                  type: 'PROJECT',
                  id: project.id,
                }}
                size={24}
              />
            </div>
          </Tooltip>
        </StyledItem>
      )}
      <StyledAssignees sx={{ paddingRight: '10px' }} {...linkProps}>
        <TaskAssignees task={task} />
      </StyledAssignees>
      <StyledItem sx={{ pr: 1, gap: 0.5 }} style={{ cursor: 'auto' }}>
        <Tooltip title={t('task_detail_tooltip')} disableInteractive>
          <IconButton
            size="small"
            onClick={stopAndPrevent(() => onDetailOpen(task))}
            data-cy="task-item-detail"
          >
            <TaskDetail />
          </IconButton>
        </Tooltip>
        <IconButton
          size="small"
          onClick={stopAndPrevent((e) => setAnchorEl(e.currentTarget))}
          data-cy="task-item-menu"
        >
          <DotsVertical />
        </IconButton>
      </StyledItem>
      <TaskMenu
        anchorEl={anchorEl}
        onClose={handleClose}
        task={task}
        project={project}
        projectScopes={projectScopes}
        newTaskActions={newTaskActions}
      />
    </StyledContainer>
  );
};
