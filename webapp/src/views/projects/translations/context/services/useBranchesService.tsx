import { useApiInfiniteQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useTranslationsService } from 'tg.views/projects/translations/context/services/useTranslationsService';
import { useLocation } from 'react-router-dom';
import { components } from 'tg.service/apiSchema.generated';
import { extractBranchFromPathname } from 'tg.component/branching/branchingPath';

type Props = {
  projectId?: number;
  translations?: ReturnType<typeof useTranslationsService>;
  enabled?: boolean;
};

type BranchModel = components['schemas']['BranchModel'];

const DEFAULT_BRANCH_NAME = 'main';

const defaultBranchObject: BranchModel = {
  id: 0,
  active: true,
  name: DEFAULT_BRANCH_NAME,
  isProtected: true,
  isDefault: true,
};

export const useBranchesService = ({ projectId, enabled = true }: Props) => {
  const location = useLocation();
  const routeBranch = extractBranchFromPathname(location.pathname);

  projectId = projectId || useProject().id;

  const loadableBranches = useApiInfiniteQuery({
    url: '/v2/projects/{projectId}/branches',
    method: 'get',
    path: { projectId: projectId },
    query: {
      size: 10000,
      activeOnly: true,
    },
    options: {
      keepPreviousData: true,
      refetchOnMount: true,
      noGlobalLoading: true,
    },
  });

  const loadedBranches = loadableBranches.data?.pages.flatMap(
    (p) => p._embedded?.branches ?? []
  );

  const data =
    loadableBranches.isFetched && loadedBranches!.length > 0
      ? loadableBranches.data!.pages.flatMap((p) => p._embedded?.branches ?? [])
      : Array.from([defaultBranchObject]);

  const urlBranch = routeBranch
    ? data?.find((b) => b.name === routeBranch)
    : undefined;

  const defaultBranch = loadableBranches.isFetched
    ? loadedBranches?.find((b) => b.isDefault) || defaultBranchObject
    : null;

  const selected = routeBranch ? (urlBranch ? urlBranch : null) : defaultBranch;

  return {
    branches: data,
    selected: selected,
    default: defaultBranch,
    selectedName: routeBranch || undefined,
    loadable: loadableBranches,
  };
};
