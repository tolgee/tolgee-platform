import { components, operations } from 'tg.service/apiSchema.generated';
import { useApiInfiniteQuery } from 'tg.service/http/useQueryApi';

type QueryParameters = operations['getTasks_1']['parameters']['query'];
type TaskModel = components['schemas']['TaskModel'];

type Props = {
  projectId: number;
  query: QueryParameters;
};

export const useProjectBoardTasks = ({ projectId, query }: Props) => {
  const result = useApiInfiniteQuery({
    url: '/v2/projects/{projectId}/tasks',
    method: 'get',
    path: { projectId },
    query,
    options: {
      keepPreviousData: true,
      getNextPageParam: (lastPage) => {
        if (
          lastPage.page &&
          lastPage.page.number! < lastPage.page.totalPages! - 1
        ) {
          return {
            path: { projectId },
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

  const items: TaskModel[] = [];

  result.data?.pages.forEach((data) =>
    data._embedded?.tasks?.forEach((t) => items.push(t))
  );

  return {
    ...result,
    items,
  };
};
