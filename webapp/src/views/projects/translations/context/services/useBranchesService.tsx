import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';

type Props = {
  projectId?: number;
  branchName?: string;
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

export const useBranchesService = ({ projectId, branchName }: Props) => {
  projectId = projectId || useProject().id;

  const loadableBranches = useApiQuery({
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

  const loadedBranches = loadableBranches.data?._embedded?.branches ?? [];

  const data =
    loadableBranches.isFetched && loadedBranches.length > 0
      ? loadedBranches
      : [defaultBranchObject];

  const urlBranch = branchName && data.find((b) => b.name === branchName);

  const defaultBranch = loadableBranches.isFetched
    ? loadedBranches.find((b) => b.isDefault) || defaultBranchObject
    : null;

  const selected = branchName ? urlBranch || null : defaultBranch;

  return {
    branches: data,
    selected,
    default: defaultBranch,
    selectedName: branchName,
    loadable: loadableBranches,
  };
};
