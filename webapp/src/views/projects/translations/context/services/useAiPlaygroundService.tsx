import { useEffect, useMemo, useRef } from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { useApiQueries } from 'tg.service/http/useQueryApi';
import { useTranslationsService } from './useTranslationsService';
import { usePromptUrlState } from 'tg.views/projects/translations/useUrlPromptState';
import { useAiReviewEnabled } from 'tg.ee';
import { useProjectContext } from 'tg.hooks/ProjectContext';

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
  const { playgroundType } = usePromptUrlState();
  const { visible: aiReviewVisible } = useAiReviewEnabled();
  const type: AiPlaygroundResultModel['type'] =
    aiReviewVisible && playgroundType === 'review' ? 'REVIEW' : 'TRANSLATE';

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
  }, [translationService.data?.pages?.[0], type]);

  const aiPlaygroundResult = useApiQueries(
    pagedKeys.map((keys) => ({
      url: '/v2/projects/{projectId}/ai-playground-result' as const,
      method: 'post' as const,
      path: {
        projectId,
      },
      content: {
        'application/json': {
          keys: keys,
          languages: languageIds,
          type,
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

  const playgroundJobsProgress = useProjectContext(
    (c) =>
      c.batchOperations
        ?.filter((o) => o.type === 'AI_PLAYGROUND_TRANSLATE')
        .map((o) => `${o.id}:${o.progress}:${o.status}`)
        .join(',') ?? ''
  );
  const lastJobsProgress = useRef(playgroundJobsProgress);

  useEffect(() => {
    if (lastJobsProgress.current === playgroundJobsProgress) {
      return;
    }
    lastJobsProgress.current = playgroundJobsProgress;
    if (enabled) {
      aiPlaygroundResult.forEach((i) => i.refetch());
    }
  }, [playgroundJobsProgress]);

  const data = useMemo(() => {
    const result: Record<number, Record<number, AiPlaygroundResultModel>> = {};
    aiPlaygroundResult.forEach((items) =>
      items.data?._embedded?.results?.forEach((item) => {
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
