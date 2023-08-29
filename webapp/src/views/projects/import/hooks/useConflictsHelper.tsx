import { useProject } from 'tg.hooks/useProject';
import { useApiQuery } from 'tg.service/http/useQueryApi';

export const useConflictsHelper = (props: {
  languageId: number;
  showResolved: boolean;
  page: number;
}) => {
  const project = useProject();

  const conflictsLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/import/result/languages/{languageId}/translations',
    method: 'get',
    path: {
      languageId: props.languageId,
      projectId: project.id,
    },
    query: {
      onlyConflicts: true,
      onlyUnresolved: !props.showResolved,
      page: props.page,
      size: 50,
    },
  });

  const languageLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/import/result/languages/{languageId}',
    method: 'get',
    path: {
      languageId: props.languageId,
      projectId: project.id,
    },
  });

  return {
    conflictsLoadable,
    languageLoadable,
  };
};
