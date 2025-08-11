import { InfiniteData } from 'react-query';
import { components, paths } from 'tg.service/apiSchema.generated';
import { useApiInfiniteQuery } from 'tg.service/http/useQueryApi';

type SuggestionParams =
  paths['/v2/projects/{projectId}/languages/{languageId}/key/{keyId}/suggestion']['get']['parameters'];

type PagedModelTranslationSuggestionModel =
  components['schemas']['PagedModelTranslationSuggestionModel'];

type FilterStateParam = SuggestionParams['query']['filterState'];

type Props = {
  languageId: number;
  keyId: number;
  projectId: number;
  filterState?: FilterStateParam;
  onSuccess?: (
    data: InfiniteData<PagedModelTranslationSuggestionModel>
  ) => void;
  enabled?: boolean;
  expectedCount: number;
};

export const useInfiniteSuggestions = ({
  languageId,
  keyId,
  projectId,
  filterState,
  onSuccess,
  enabled,
  expectedCount,
}: Props) => {
  const params: SuggestionParams = {
    query: {
      sort: ['createdAt,desc'],
      filterState,
      // @ts-ignore force react-query cache only requests with the same count expected
      expectedCount,
    },
    path: {
      languageId,
      keyId,
      projectId,
    },
  };

  const suggestionsLoadable = useApiInfiniteQuery({
    url: '/v2/projects/{projectId}/languages/{languageId}/key/{keyId}/suggestion',
    method: 'get',
    ...params,
    options: {
      enabled,
      getNextPageParam: (lastPage) => {
        if (
          lastPage.page &&
          lastPage.page.number! < lastPage.page.totalPages! - 1
        ) {
          return {
            path: params.path,
            query: {
              ...params.query,
              page: lastPage.page!.number! + 1,
            },
          };
        } else {
          return null;
        }
      },
      onSuccess,
    },
  });

  return suggestionsLoadable;
};
