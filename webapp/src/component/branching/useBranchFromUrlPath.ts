import { useLocation } from 'react-router-dom';

import { extractBranchFromPathname } from './branchingPath';

export const useBranchFromUrlPath = () => {
  const location = useLocation();
  return extractBranchFromPathname(location.pathname);
};
