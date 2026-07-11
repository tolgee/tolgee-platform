import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useEnabledFeatures } from 'tg.globalContext/helpers';

type Props = {
  projectId: number;
  enabled?: boolean;
};

export const useProjectGlossaries = ({ projectId, enabled = true }: Props) => {
  const { isEnabled } = useEnabledFeatures();
  const glossaryFeatureEnabled = isEnabled('GLOSSARY');
  const query = useApiQuery({
    url: '/v2/projects/{projectId}/glossaries',
    method: 'get',
    path: { projectId },
    options: {
      enabled: enabled && glossaryFeatureEnabled,
      keepPreviousData: true,
      noGlobalLoading: true,
    },
  });

  if (!glossaryFeatureEnabled) {
    return {
      data: [],
      isLoading: false,
    };
  }

  return {
    data: query.data?._embedded?.glossaries,
    isLoading: query.isLoading,
  };
};
