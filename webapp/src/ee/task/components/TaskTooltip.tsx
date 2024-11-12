import { useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { Dialog, IconButton, Tooltip } from '@mui/material';
import { Translate01 } from '@untitled-ui/icons-react';
import { TaskDetail as TaskDetailIcon } from 'tg.component/CustomIcons';
import { useTranslate } from '@tolgee/react';

import { TaskTooltipContent } from './TaskTooltipContent';
import { components } from 'tg.service/apiSchema.generated';
import { TaskDetail } from './TaskDetail';
import { stopAndPrevent } from 'tg.fixtures/eventHandler';
import { getTaskRedirect } from './utils';

type TaskModel = components['schemas']['TaskModel'];
type SimpleProjectModel = components['schemas']['SimpleProjectModel'];

type Action = 'open' | 'detail';

type Props = {
  taskNumber: number;
  project: SimpleProjectModel;
  children: React.ReactElement<any, any>;
  actions?: Action[] | React.ReactNode | ((task: TaskModel) => React.ReactNode);
  newTaskActions: boolean;
} & Omit<React.ComponentProps<typeof Tooltip>, 'title'>;

export const TaskTooltip = ({
  taskNumber,
  project,
  children,
  actions = ['open', 'detail'],
  newTaskActions,
  ...tooltipProps
}: Props) => {
  const [taskDetailData, setTaskDetailData] = useState<TaskModel>();
  const { t } = useTranslate();
  const popperRef = useRef<any>(null);

  const actionsContent = Array.isArray(actions)
    ? (task: TaskModel) => (
        <>
          {actions.includes('open') && (
            <Tooltip
              enterTouchDelay={1000}
              disableInteractive
              title={t('task_link_translations_tooltip')}
            >
              <IconButton
                data-cy="task-tooltip-action-translations"
                component={Link}
                to={getTaskRedirect(project, task.number)}
                size="small"
              >
                <Translate01 width={20} height={20} />
              </IconButton>
            </Tooltip>
          )}
          {actions.includes('detail') && (
            <Tooltip
              enterTouchDelay={1000}
              disableInteractive
              title={t('task_detail_tooltip')}
            >
              <IconButton
                data-cy="task-tooltip-action-detail"
                size="small"
                onClick={() => setTaskDetailData(task)}
              >
                <TaskDetailIcon width={20} height={20} />
              </IconButton>
            </Tooltip>
          )}
        </>
      )
    : null;

  return (
    <>
      <Tooltip
        {...tooltipProps}
        componentsProps={{
          tooltip: {
            style: { maxWidth: 500, fontSize: 14 },
          },
        }}
        title={
          <TaskTooltipContent
            taskNumber={taskNumber}
            projectId={project.id}
            actions={actionsContent ?? actions}
            popperRef={popperRef}
          />
        }
        PopperProps={{
          popperRef,
        }}
      >
        {children}
      </Tooltip>
      {taskDetailData && (
        <Dialog
          open={true}
          onClose={() => setTaskDetailData(undefined)}
          maxWidth="xl"
          onClick={stopAndPrevent()}
        >
          <TaskDetail
            taskNumber={taskDetailData.number}
            onClose={() => setTaskDetailData(undefined)}
            projectId={project.id}
            task={taskDetailData}
          />
        </Dialog>
      )}
    </>
  );
};
