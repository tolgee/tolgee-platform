import { Box, Button } from '@mui/material';
import { T } from '@tolgee/react';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { QUERY } from 'tg.constants/links';
import { useUser } from 'tg.globalContext/helpers';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useTranslationsActions } from 'tg.views/projects/translations/context/TranslationsContext';
import { TaskState } from './utils';

type Props = {
  taskNumber: number;
  projectId: number;
};

export const TaskAllDonePlaceholder = ({ taskNumber, projectId }: Props) => {
  const [_, setTaskHideDone] = useUrlSearchState(
    QUERY.TRANSLATIONS_PREFILTERS_TASK_HIDE_CLOSED
  );
  const { finishTask } = useTranslationsActions();
  const user = useUser();

  const handleFinishTask = async () => {
    await finishTask(taskNumber);
    setTaskHideDone('false');
  };

  const taskLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/tasks/{taskNumber}',
    method: 'get',
    path: { projectId, taskNumber },
  });

  const isAssigned = taskLoadable.data?.assignees.find(
    (u) => u.id === user?.id
  );

  return (
    <EmptyListMessage
      height="0px"
      hint={
        isAssigned &&
        !['FINISHED', 'CANCELED'].includes(
          taskLoadable.data?.state as TaskState
        ) && (
          <Button onClick={handleFinishTask} color="primary">
            <T keyName="task_all_done_placeholder_finish_task" />
          </Button>
        )
      }
    >
      <Box display="flex" gap={1}>
        {taskLoadable.data?.type === 'TRANSLATE' ? (
          <T keyName="task_all_done_placeholder_translate_label" />
        ) : taskLoadable.data?.type === 'REVIEW' ? (
          <T keyName="task_all_done_placeholder_review_label" />
        ) : null}
      </Box>
    </EmptyListMessage>
  );
};
