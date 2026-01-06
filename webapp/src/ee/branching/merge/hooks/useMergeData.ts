import {
  useApiInfiniteQuery,
  useApiMutation,
  useApiQuery,
} from 'tg.service/http/useQueryApi';
import { BranchMergeChangeModel, BranchMergeChangeType } from '../types';

export const useMergeData = (
  projectId: number,
  mergeId: number,
  selectedTab: BranchMergeChangeType
) => {
  const previewLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/branches/merge/{mergeId}/preview',
    method: 'get',
    path: { projectId, mergeId },
  });

  const changesLoadable = useApiInfiniteQuery({
    url: '/v2/projects/{projectId}/branches/merge/{mergeId}/changes',
    method: 'get',
    path: { projectId, mergeId },
    query: { size: 25, type: selectedTab },
    options: {
      enabled: Boolean(previewLoadable.data),
      keepPreviousData: true,
      getNextPageParam: (lastPage) => {
        if (
          lastPage.page &&
          lastPage.page.number! < lastPage.page.totalPages! - 1
        ) {
          return {
            path: { projectId, mergeId },
            query: {
              size: 25,
              page: lastPage.page.number! + 1,
              type: selectedTab,
            },
          } as const;
        }
        return undefined;
      },
    },
  });

  const resolveMutation = useApiMutation({
    url: '/v2/projects/{projectId}/branches/merge/{mergeId}/resolve',
    method: 'put',
  });

  const resolveAllMutation = useApiMutation({
    url: '/v2/projects/{projectId}/branches/merge/{mergeId}/resolve-all',
    method: 'put',
  });

  const applyMutation = useApiMutation({
    url: '/v2/projects/{projectId}/branches/merge/{mergeId}/apply',
    method: 'post',
  });

  const deleteMutation = useApiMutation({
    url: '/v2/projects/{projectId}/branches/merge/{mergeId}',
    method: 'delete',
  });

  const refreshPreviewMutation = useApiMutation({
    url: '/v2/projects/{projectId}/branches/merge/{mergeId}/refresh',
    method: 'post',
  });

  const changes: BranchMergeChangeModel[] =
    changesLoadable.data?.pages.flatMap(
      (p: any) => p._embedded?.branchMergeChanges ?? []
    ) ?? [];

  return {
    previewLoadable,
    merge: previewLoadable.data,
    changesLoadable,
    changes,
    resolveMutation,
    resolveAllMutation,
    applyMutation,
    deleteMutation,
    refreshPreviewMutation,
  };
};
