import { Location } from 'history';

const BRANCH_SEGMENT = '/tree/';

const findBranchIndex = (pathname: string) => pathname.indexOf(BRANCH_SEGMENT);

const sliceBranchPart = (pathname: string) => {
  const idx = findBranchIndex(pathname);
  if (idx === -1) return undefined;
  return pathname.slice(idx + BRANCH_SEGMENT.length);
};

export const extractBranchFromPathname = (pathname: string) => {
  const branchPart = sliceBranchPart(pathname);
  if (!branchPart) return undefined;

  const cleaned = branchPart.replace(/\/+$/, '');

  try {
    return decodeURIComponent(cleaned);
  } catch (e) {
    return cleaned;
  }
};

export const stripBranchFromPathname = (pathname: string) => {
  const idx = findBranchIndex(pathname);
  if (idx === -1) return pathname;
  const stripped = pathname.slice(0, idx);
  return stripped === '' ? '/' : stripped.replace(/\/$/, '');
};

export const applyBranchToPathname = (
  pathname: string,
  branch?: string | null
) => {
  const basePath = stripBranchFromPathname(pathname);

  if (!branch) {
    return basePath;
  }

  // Encode branch but keep path separators so we can support names like "feat/demo"
  const encodedBranch = encodeURIComponent(branch).replace(/%2F/gi, '/');
  const trimmedBase = basePath.replace(/\/$/, '');
  return `${trimmedBase}${BRANCH_SEGMENT}${encodedBranch}`;
};

export const applyBranchToLocation = (
  location: Location | { pathname: string; search?: string; hash?: string },
  branch?: string | null
) => {
  return (
    applyBranchToPathname(location.pathname, branch) +
    (location.search || '') +
    (location.hash || '')
  );
};

export const applyBranchToUrl = (url: string, branch?: string | null) => {
  const parsed = new URL(url, window.location.origin);
  return (
    applyBranchToPathname(parsed.pathname, branch) + parsed.search + parsed.hash
  );
};
