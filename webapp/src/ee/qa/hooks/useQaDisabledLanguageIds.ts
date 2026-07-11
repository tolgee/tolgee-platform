import { useMemo } from 'react';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useQaChecksEnabled } from './useQaChecksEnabled';
import { LanguageModel } from 'tg.service/apiSchemaTypes.generated';

export const useQaDisabledLanguages = (): LanguageModel[] => {
  const project = useProject();
  const qaEnabled = useQaChecksEnabled();

  const languagesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/qa-settings/languages',
    method: 'get',
    path: { projectId: project.id },
    options: { enabled: qaEnabled, staleTime: 60_000 },
  });

  return useMemo(
    () =>
      languagesLoadable.data
        ?.filter((config) => !config.enabled)
        ?.map((config) => config.language) ?? [],
    [languagesLoadable.data]
  );
};

export const useQaDisabledLanguageIds = (): Set<number> => {
  const languages = useQaDisabledLanguages();

  return useMemo(
    () => new Set<number>(languages.map((language) => language.id)),
    [languages]
  );
};

export const useQaDisabledLanguageTags = (): Set<string> => {
  const languages = useQaDisabledLanguages();

  return useMemo(
    () => new Set<string>(languages.map((language) => language.tag)),
    [languages]
  );
};
