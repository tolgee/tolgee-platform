import { useProject } from 'tg.hooks/useProject';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useIsBranchingEnabled } from 'tg.component/branching/useIsBranchingEnabled';

export const useRuleScopeOptions = () => {
  const project = useProject();
  const isBranchingEnabled = useIsBranchingEnabled();

  const branchesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/branches',
    method: 'get',
    path: { projectId: project.id },
    query: { page: 0, size: 1000 },
    options: { enabled: isBranchingEnabled },
  });
  const branchOptions =
    branchesLoadable.data?._embedded?.branches?.map((branch) => branch.name) ??
    [];

  return { isBranchingEnabled, branchOptions };
};
