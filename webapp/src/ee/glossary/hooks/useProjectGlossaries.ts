import { useApiQuery } from 'tg.service/http/useQueryApi';

type Props = {
  projectId: number;
  enabled?: boolean;
};

export const useProjectGlossaries = ({ projectId, enabled = true }: Props) => {
  const query = useApiQuery({
    url: '/v2/projects/{projectId}/glossaries',
    method: 'get',
    path: { projectId },
    options: {
      enabled,
      keepPreviousData: true,
      noGlobalLoading: true,
    },
  });

  return {
    data: query.data?._embedded?.glossaries,
    isLoading: query.isLoading,
  };
};
