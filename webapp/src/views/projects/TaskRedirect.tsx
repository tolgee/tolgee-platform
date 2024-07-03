import { useEffect } from 'react';
import { useHistory } from 'react-router-dom';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { components } from 'tg.service/apiSchema.generated';
import { useApiQuery } from 'tg.service/http/useQueryApi';

type TaskModel = components['schemas']['TaskModel'];

export const TaskRedirect = () => {
  const project = useProject();
  const history = useHistory();
  const [taskNum] = useUrlSearchState('number', {
    defaultVal: undefined,
  });
  const [detail] = useUrlSearchState('detail');

  const getLinkToTask = (task: TaskModel) => {
    const languages = new Set([project.baseLanguage!.tag, task.language.tag]);

    let url = `${LINKS.PROJECT_TRANSLATIONS.build({
      [PARAMS.PROJECT_ID]: project.id,
    })}?task=${task.number}`;

    if (detail === 'true') {
      url += `&taskDetail=${task.number}`;
    }

    url +=
      '&' +
      Array.from(languages)
        .map((l) => `languages=${l}`)
        .join('&');
    return url;
  };

  const taskLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/tasks/{taskNumber}',
    method: 'get',
    path: { projectId: project.id, taskNumber: Number(taskNum) },
  });

  useEffect(() => {
    if (taskLoadable.data) {
      history.replace(getLinkToTask(taskLoadable.data));
    }
  }, [taskLoadable.data]);

  return <BoxLoading />;
};
