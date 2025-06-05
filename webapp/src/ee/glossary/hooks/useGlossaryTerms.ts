import {
  useApiInfiniteQuery,
  useApiMutation,
} from 'tg.service/http/useQueryApi';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { useMemo } from 'react';
import { useGlossary } from 'tg.ee.module/glossary/hooks/useGlossary';
import { usePreferredOrganization } from 'tg.globalContext/helpers';

type Props = {
  search: string | undefined;
  languageTags: string[] | undefined;
};

export const useGlossaryTerms = ({ search, languageTags }: Props) => {
  const { preferredOrganization } = usePreferredOrganization();
  const glossary = useGlossary();

  const path = {
    organizationId: preferredOrganization!.id,
    glossaryId: glossary.id,
  };
  const query = {
    search,
    languageTags,
    size: 30,
    sort: ['tr.text,desc'],
  };

  const loadable = useApiInfiniteQuery({
    url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}/termsWithTranslations',
    method: 'get',
    path,
    query,
    options: {
      keepPreviousData: true,
      refetchOnMount: true,
      noGlobalLoading: true,
      getNextPageParam: (lastPage) => {
        if (
          lastPage.page &&
          lastPage.page.number! < lastPage.page.totalPages! - 1
        ) {
          return {
            path: path,
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

  const terms = useMemo(() => {
    const pages = loadable.data?.pages ?? [];
    return pages.flatMap((p) => p._embedded?.glossaryTerms ?? []);
  }, [loadable.data]);

  const total = loadable.data?.pages?.[0]?.page?.totalElements;

  const onFetchNextPageHint = async () => {
    if (!loadable.isFetching && loadable.hasNextPage) {
      await loadable.fetchNextPage();
    }
  };

  const getAllTermsIdsMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}/termsIds',
    method: 'get',
  });

  const getAllTermsIds = async () => {
    const data = await getAllTermsIdsMutation.mutateAsync({ path, query });
    return data._embedded?.longList ?? [];
  };

  useGlobalLoading(loadable.isLoading);

  return {
    terms,
    total,
    loading: loadable.isLoading,
    onFetchNextPageHint,
    getAllTermsIds,
  };
};
