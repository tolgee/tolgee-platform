import { useLocation } from 'react-router-dom';

import { applyBranchToUrl, extractBranchFromPathname } from './branchingPath';
import { Link, LINKS } from 'tg.constants/links';
import { getCachedBranch } from './branchCache';
import { useProject } from 'tg.hooks/useProject';

const BRANCHING_LINKS = new Set<Link>([
  LINKS.PROJECT_DASHBOARD,
  LINKS.PROJECT_TRANSLATIONS,
]);

export const useBranchLinks = () => {
  const location = useLocation();
  const project = useProject();
  const branch =
    extractBranchFromPathname(location.pathname) || getCachedBranch(project.id);

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

  return {
    branch,
    withBranchLink,
    withBranchUrl,
  };
};
