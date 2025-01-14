import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { useProject } from 'tg.hooks/useProject';
import { Dialog } from '@mui/material';
import { TaskDetail } from './TaskDetail';
import { FC } from 'react';
import { QUERY } from 'tg.constants/links';

export const TranslationsTaskDetail: FC = () => {
  const [taskDetail, setTaskDetail] = useUrlSearchState(
    QUERY.TRANSLATIONS_TASK_DETAIL
  );
  const project = useProject();
  return (
    <>
      {taskDetail !== undefined && (
        <Dialog
          open={true}
          onClose={() => setTaskDetail(undefined)}
          maxWidth="xl"
        >
          <TaskDetail
            taskNumber={Number(taskDetail)}
            onClose={() => setTaskDetail(undefined)}
            projectId={project.id}
          />
        </Dialog>
      )}
    </>
  );
};
