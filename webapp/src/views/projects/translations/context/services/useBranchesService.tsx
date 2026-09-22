import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProjectContextOptional } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { useIsBranchingEnabled } from 'tg.component/branching/useIsBranchingEnabled';

type Props = {
  projectId?: number;
  branchName?: string;
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

export const useBranchesService = ({
  projectId,
  branchName,
  enabled = true,
}: Props) => {
  const contextProjectId = useProjectContextOptional()?.project?.id;
  const resolvedProjectId = projectId || contextProjectId;
  const isBranchingEnabled = useIsBranchingEnabled();
  const isActive =
    enabled && isBranchingEnabled && resolvedProjectId !== undefined;
  const loadableBranches = useApiQuery({
    url: '/v2/projects/{projectId}/branches',
    method: 'get',
    path: { projectId: resolvedProjectId! },
    query: {
      size: 10000,
    },
    options: {
      enabled: isActive,
      keepPreviousData: true,
      refetchOnMount: true,
      noGlobalLoading: true,
    },
  });

  if (!isActive) {
    return {
      branches: [],
      selected: null,
      default: null,
      selectedName: branchName || undefined,
      loadable: {
        isLoading: false,
        isFetching: false,
        isFetched: true,
        data: [],
      },
    };
  }

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
