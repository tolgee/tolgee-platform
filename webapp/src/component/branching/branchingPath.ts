import { Location } from 'history';

const BRANCH_SEGMENT = '/tree/';

export const extractBranchFromPathname = (pathname: string): string | null => {
  const idx = pathname.indexOf(BRANCH_SEGMENT);
  if (idx === -1) return null;
  const after = pathname.substring(idx + BRANCH_SEGMENT.length);
  if (!after) return null;
  return after;
};

export const applyBranchToUrl = (url: string, branch: string) => {
  const [path, query] = url.split('?');
  const idx = path.indexOf(BRANCH_SEGMENT);
  const base = idx === -1 ? path : path.substring(0, idx);
  const newPath = `${base}${BRANCH_SEGMENT}${branch}`;
  return query ? `${newPath}?${query}` : newPath;
};

export const applyBranchToLocation = (
  location: Location,
  branch: string | null
) => {
  const idx = location.pathname.indexOf(BRANCH_SEGMENT);
  const basePath =
    idx === -1 ? location.pathname : location.pathname.substring(0, idx);

  if (!branch) {
    return { ...location, pathname: basePath };
  }

  return {
    ...location,
    pathname: `${basePath}${BRANCH_SEGMENT}${branch}`,
  };
};
