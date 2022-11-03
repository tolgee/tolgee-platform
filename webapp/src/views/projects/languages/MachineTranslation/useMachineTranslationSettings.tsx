import { useRef } from 'react';

type SetMachineTranslationSettingsDto =
  components['schemas']['SetMachineTranslationSettingsDto'];

import {
  useApiQuery,
  useApiMutation,
  matchUrlPrefix,
} from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useQueryClient } from 'react-query';
import { useConfig } from 'tg.globalContext/helpers';
import { components } from 'tg.service/apiSchema.generated';

type Props = {
  onReset: () => void;
};

export const useMachineTranslationSettings = ({ onReset }: Props) => {
  const queryClient = useQueryClient();
  const config = useConfig();
  const project = useProject();

  const languages = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: project.id },
    query: { size: 1000 },
    options: {
      onSuccess: onReset,
    },
  });

  const settings = useApiQuery({
    url: '/v2/projects/{projectId}/machine-translation-service-settings',
    method: 'get',
    path: { projectId: project.id },
  });

  const creditBalance = useApiQuery({
    url: '/v2/projects/{projectId}/machine-translation-credit-balance',
    method: 'get',
    path: { projectId: project.id },
  });

  const updateSettings = useApiMutation({
    url: '/v2/projects/{projectId}/machine-translation-service-settings',
    method: 'put',
  });

  const lastUpdateRef = useRef<Promise<any>>();
  const applyUpdate = (data: SetMachineTranslationSettingsDto) => {
    const promise = updateSettings
      .mutateAsync({
        path: { projectId: project.id },
        content: { 'application/json': data },
      })
      .then((data) => {
        if (lastUpdateRef.current === promise) {
          queryClient.setQueriesData(
            matchUrlPrefix(
              '/v2/projects/{projectId}/machine-translation-service-settings'
            ),
            {
              _version: Math.random(), // force new instance
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

  const baseSetting = settings.data?._embedded?.languageConfigs?.find(
    (item) => item.targetLanguageId === null
  );

  const providers = Object.entries(config.machineTranslationServices.services)
    .map(([service, value]) => (value.enabled ? service : undefined))
    .filter(Boolean) as string[];

  return {
    languages,
    settings,
    creditBalance,
    applyUpdate,
    baseSetting,
    updateSettings,
    providers,
  };
};
