import { useEffect } from 'react';
import { useHistory } from 'react-router-dom';

import { BoxLoading } from 'tg.component/common/BoxLoading';
import { LINKS, PARAMS, QUERY } from 'tg.constants/links';
import { useUser } from 'tg.globalContext/helpers';
import { useProject } from 'tg.hooks/useProject';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { ProjectPage } from './ProjectPage';

type TaskModel = components['schemas']['TaskModel'];

export const TaskRedirect = () => {
  const project = useProject();
  const history = useHistory();
  const user = useUser();
  const [taskNum] = useUrlSearchState('number', {
    defaultVal: undefined,
  });
  const [detail] = useUrlSearchState('detail');

  const getLinkToTask = (task: TaskModel) => {
    const languages = new Set([project.baseLanguage!.tag, task.language.tag]);

    let url = `${LINKS.PROJECT_TRANSLATIONS.build({
      [PARAMS.PROJECT_ID]: project.id,
    })}?${QUERY.TRANSLATIONS_PREFILTERS_TASK}=${task.number}`;

    if (detail === 'true') {
      url += `&${QUERY.TRANSLATIONS_TASK_DETAIL}=${task.number}`;
    }

    if (
      task.assignees.find((u) => u.id === user?.id) &&
      (task.state === 'IN_PROGRESS' || task.state === 'NEW')
    ) {
      url += `&${QUERY.TRANSLATIONS_PREFILTERS_TASK_HIDE_CLOSED}=true`;
    }

    url +=
      '&' +
      Array.from(languages)
        .map((l) => `languages=${l}`)
        .join('&');
    return url;
  };

  const taskLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/tasks/{taskNumber}',
    method: 'get',
  });

  useEffect(() => {
    taskLoadable.mutate(
      {
        path: { projectId: project.id, taskNumber: Number(taskNum) },
      },
      {
        onSuccess(data) {
          history.replace(getLinkToTask(data));
        },
      }
    );
  }, []);

  return (
    <ProjectPage>
      <BoxLoading />
    </ProjectPage>
  );
};
