import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { useApiInfiniteQuery } from 'tg.service/http/useQueryApi';

type TranslationHistoryModel = components['schemas']['TranslationHistoryModel'];

type TranslationViewModel = components['schemas']['TranslationViewModel'];
type LanguageModel = components['schemas']['LanguageModel'];

type Props = {
  keyId: number;
  translation: TranslationViewModel | undefined;
  language: LanguageModel;
};

export const useHistory = ({ keyId, translation, language }: Props) => {
  const project = useProject();
  const path = {
    projectId: project.id,
    translationId: translation?.id as number,
  };
  const query = {
    size: 20,
  };

  const history = useApiInfiniteQuery({
    url: '/v2/projects/{projectId}/translations/{translationId}/history',
    method: 'get',
    path,
    query,
    options: {
      enabled: Boolean(translation?.id),
      getNextPageParam: (lastPage) => {
        if (
          lastPage.page &&
          lastPage.page.number! < lastPage.page.totalPages! - 1
        ) {
          return {
            path,
            query: {
              ...query,
              page: lastPage.page!.number! + 1,
            },
          };
        } else {
          return null;
        }
      },
    },
  });

  const fetchMore = () => {
    history.fetchNextPage();
  };

  const historyItems: TranslationHistoryModel[] = [];

  history.data?.pages.forEach((page) =>
    page._embedded?.revisions?.forEach((item) => historyItems.push(item))
  );

  historyItems.reverse();

  return {
    fetchMore,
    historyItems,
    hasNextPage: history.hasNextPage,
    loading: history.isLoading,
  };
};
