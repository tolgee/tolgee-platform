import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslate } from '@tolgee/react';
import { Box, IconButton, styled, Tooltip, useTheme } from '@mui/material';
import { AccessAlarm, MoreVert } from '@mui/icons-material';

import { TranslationIcon } from 'tg.component/CustomIcons';
import { components } from 'tg.service/apiSchema.generated';
import { BatchProgress } from 'tg.views/projects/translations/BatchOperations/OperationsSummary/BatchProgress';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { TaskMenu } from './TaskMenu';
import { TaskLabel } from './TaskLabel';
import { getLinkToTask } from './utils';

type TaskModel = components['schemas']['TaskModel'];
type SimpleProjectModel = components['schemas']['SimpleProjectModel'];

const StyledContainer = styled('div')`
  display: contents;
  &:hover > * {
    background: ${({ theme }) => theme.palette.tokens.text._states.hover};
  }
`;

const StyledItem = styled(Box)`
  display: flex;
  align-items: center;
  align-self: stretch;
  justify-self: stretch;
  gap: 8px;
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
`;

type Props = {
  task: TaskModel;
  onDetailOpen: (task: TaskModel) => void;
  project: SimpleProjectModel;
  showProject?: boolean;
};

export const TaskItem = ({
  task,
  onDetailOpen,
  project,
  showProject,
}: Props) => {
  const { t } = useTranslate();
  const theme = useTheme();
  const formatDate = useDateFormatter();

  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  const handleClose = () => {
    setAnchorEl(null);
  };

  return (
    <StyledContainer>
      <StyledItem>
        <TaskLabel sx={{ padding: '8px 0px 8px 16px' }} task={task} />
      </StyledItem>
      <StyledItem
        color={theme.palette.tokens.text.secondary}
        alignItems="center"
        justifyContent="center"
      >
        {t('task_keys_count', { value: task.totalItems })}
      </StyledItem>
      <StyledProgress>
        <BatchProgress progress={task.doneItems} max={task.totalItems} />
        {task.dueDate ? (
          <Box display="flex" alignItems="center" gap={0.5}>
            <AccessAlarm sx={{ fontSize: 16 }} />
            {formatDate(task.dueDate, { timeZone: 'UTC' })}
          </Box>
        ) : null}
      </StyledProgress>
      {showProject && (
        <StyledItem>
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
      <StyledAssignees>
        {task.assignees.map((user) => (
          <Tooltip
            key={user.id}
            title={<div>{user.username}</div>}
            disableInteractive
          >
            <div>
              <AvatarImg
                owner={{
                  name: user.name,
                  avatar: user.avatar,
                  type: 'USER',
                  id: user.id,
                }}
                size={24}
              />
            </div>
          </Tooltip>
        ))}
      </StyledAssignees>
      <StyledItem sx={{ pr: 1, gap: 0.5 }}>
        <IconButton
          component={Link}
          to={getLinkToTask(project, task)}
          size="small"
        >
          <TranslationIcon fontSize="small" />
        </IconButton>
        <IconButton size="small" onClick={(e) => setAnchorEl(e.currentTarget)}>
          <MoreVert fontSize="small" />
        </IconButton>
      </StyledItem>
      <TaskMenu
        anchorEl={anchorEl}
        onClose={handleClose}
        task={task}
        onDetailOpen={onDetailOpen}
        project={project}
      />
    </StyledContainer>
  );
};
