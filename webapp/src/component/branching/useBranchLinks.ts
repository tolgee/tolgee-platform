import { useLocation } from 'react-router-dom';

import { applyBranchToUrl, extractBranchFromPathname } from './branchingPath';
import { Link, LINKS } from 'tg.constants/links';
import { getCachedBranch } from './branchCache';
import { useProject } from 'tg.hooks/useProject';
import { BRANCH_ROUTES, BranchRouteKey } from '../../branching/branchRoutes';

const BRANCHING_LINKS = new Set<Link>([
  LINKS.PROJECT_DASHBOARD,
  LINKS.PROJECT_TRANSLATIONS,
  LINKS.PROJECT_IMPORT,
  LINKS.PROJECT_EXPORT,
]);

export const useBranchLinks = (selectedBranch?: string) => {
  const location = useLocation();
  const project = useProject();
  const branch =
    selectedBranch ||
    extractBranchFromPathname(location.pathname) ||
    getCachedBranch(project.id);

  const withBranchLink = (
    link: Link,
    params?: { [key: string]: string | number }
  ) => {
    const url = link.build(params);

    if (!branch || !BRANCHING_LINKS.has(link)) {
      return url;
    }

    return applyBranchToUrl(url, branch);
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
