import { applyBranchToUrl } from './branchingPath';
import { useBranchFromUrlPath } from './useBranchFromUrlPath';
import { Link, LINKS } from 'tg.constants/links';
import { getCachedBranch } from './branchCache';
import { useProject } from 'tg.hooks/useProject';
import { BRANCH_ROUTES, BranchRouteKey } from '../../branching/branchRoutes';

const BRANCHING_LINKS = new Set<Link>([
  LINKS.PROJECT_DASHBOARD,
  LINKS.PROJECT_TRANSLATIONS,
  LINKS.PROJECT_IMPORT,
  LINKS.PROJECT_EXPORT,
  LINKS.PROJECT_TASKS,
]);

export const useBranchLinks = (selectedBranch?: string) => {
  const project = useProject();
  const branch =
    selectedBranch || useBranchFromUrlPath() || getCachedBranch(project.id);

  const withBranchLink = (
    link: Link,
    params?: { [key: string]: string | number },
    branchName?: string
  ) => {
    const url = link.build(params);
    const branchParam = branchName || branch;

    if (!branchParam || !BRANCHING_LINKS.has(link)) {
      return url;
    }

    return applyBranchToUrl(url, branchParam);
  };

  const withBranchUrl = (url?: string) => {
    if (!url || !branch) return url;
    return applyBranchToUrl(url, branch);
  };

  const buildLink = (key: BranchRouteKey, branchName?: string) =>
    BRANCH_ROUTES[key].build(project.id, branchName || branch || undefined);

  return {
    branch,
    withBranchLink,
    withBranchUrl,
    buildLink,
  };
};
