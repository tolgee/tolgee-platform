import { useMemo } from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { useApiQueries } from 'tg.service/http/useQueryApi';
import { useTranslationsService } from './useTranslationsService';

type LanguageModel = components['schemas']['LanguageModel'];
type AiPlaygroundResultModel = components['schemas']['AiPlaygroundResultModel'];

type Props = {
  translationService: ReturnType<typeof useTranslationsService>;
  allLanguages: LanguageModel[];
  translationsLanguages: string[];
  projectId: number;
  enabled: boolean;
};

export const useAiPlaygroundService = ({
  translationService,
  allLanguages,
  translationsLanguages,
  projectId,
  enabled,
}: Props) => {
  const languageIds = useMemo(() => {
    return allLanguages
      ?.filter((l) => translationsLanguages?.includes(l.tag))
      .map((l) => l.id);
  }, [allLanguages, translationsLanguages]);

  const pagedKeys =
    translationService.data?.pages.map(
      (page) => page._embedded?.keys?.map((item) => item.keyId) ?? []
    ) ?? [];

  // refetch data when first page data changes
  const hash = useMemo(() => {
    return Math.random();
  }, [translationService.data?.pages?.[0]]);

  const aiPlaygroundResult = useApiQueries(
    pagedKeys.map((keys) => ({
      url: '/v2/projects/{projectId}/ai-playground-result',
      method: 'post',
      path: {
        projectId,
      },
      content: {
        'application/json': {
          keys: keys,
          languages: languageIds,
        },
      },
      query: {
        hash,
      },
      options: {
        enabled: Boolean(keys?.length && languageIds?.length) && enabled,
      },
    }))
  );

  const data = useMemo(() => {
    const result: Record<number, Record<number, AiPlaygroundResultModel>> = {};
    aiPlaygroundResult.forEach((items) =>
      items.data?.items.forEach((item) => {
        let key = result[item.keyId];
        if (!key) {
          key = result[item.keyId] = {};
        }
        key[item.languageId] = item;
      })
    );
    return result;
  }, [aiPlaygroundResult]);

  return {
    data: enabled ? data : undefined,
    refetchData: () => aiPlaygroundResult.forEach((i) => i.refetch()),
  };
};
