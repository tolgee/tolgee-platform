import { components, operations } from 'tg.service/apiSchema.generated';
import { useApiInfiniteQuery } from 'tg.service/http/useQueryApi';

type QueryParameters = operations['getTasks_1']['parameters']['query'];
type TaskWithProjectModel = components['schemas']['TaskWithProjectModel'];

type Props = {
  query: QueryParameters;
};

export const useMyBoardTask = ({ query }: Props) => {
  const result = useApiInfiniteQuery({
    url: '/v2/user-tasks',
    method: 'get',
    query,
    options: {
      keepPreviousData: true,
      getNextPageParam: (lastPage) => {
        if (
          lastPage.page &&
          lastPage.page.number! < lastPage.page.totalPages! - 1
        ) {
          return {
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

  const items: TaskWithProjectModel[] = [];

  result.data?.pages.forEach((data) =>
    data._embedded?.tasks?.forEach((t) => items.push(t))
  );

  return {
    ...result,
    items,
  };
};
