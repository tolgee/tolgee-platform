import { useRef } from 'react';
import { useQueryClient } from 'react-query';

import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import {
  matchUrlPrefix,
  useApiMutation,
  useApiQuery,
} from 'tg.service/http/useQueryApi';

type AutoTranslationSettingsDto =
  components['schemas']['AutoTranslationSettingsDto'];

type Props = {
  onReset: () => void;
};

export const useAutoTranslateSettings = ({ onReset }: Props) => {
  const queryClient = useQueryClient();
  const project = useProject();

  const settings = useApiQuery({
    url: '/v2/projects/{projectId}/auto-translation-settings',
    method: 'get',
    path: { projectId: project.id },
  });

  const updateSettings = useApiMutation({
    url: '/v2/projects/{projectId}/auto-translation-settings',
    method: 'put',
  });

  const lastUpdateRef = useRef<Promise<any>>();
  const applyUpdate = (data: AutoTranslationSettingsDto) => {
    const promise = updateSettings
      .mutateAsync({
        path: { projectId: project.id },
        content: { 'application/json': data },
      })
      .then((data) => {
        if (promise === lastUpdateRef.current) {
          queryClient.setQueriesData(
            matchUrlPrefix(
              '/v2/projects/{projectId}/auto-translation-settings'
            ),
            {
              version: Math.random(), // force data change
              ...data,
            }
          );
        }
      })
      .catch(() => {
        onReset();
      });
    lastUpdateRef.current = promise;
  };

  return {
    settings,
    updateSettings,
    applyUpdate,
  };
};
