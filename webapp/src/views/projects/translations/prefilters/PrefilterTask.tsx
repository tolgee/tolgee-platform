import { T } from '@tolgee/react';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';

import { PrefilterContainer } from './ContainerPrefilter';
import { TaskLabel } from 'tg.component/task/TaskLabel';

type Props = {
  taskId: number;
};

export const PrefilterTask = ({ taskId }: Props) => {
  const project = useProject();

  const { data } = useApiQuery({
    url: '/v2/projects/{projectId}/tasks/{taskId}',
    method: 'get',
    path: { projectId: project.id, taskId },
  });

  if (!data) {
    return null;
  }

  return (
    <PrefilterContainer
      title={<T keyName="task_filter_indicator_label" />}
      content={<TaskLabel task={data} />}
    />
  );
};
