const STORAGE_KEY = 'projectBranch';

type BranchCache = Record<string, string | undefined>;

const read = (): BranchCache => {
  try {
    const value = localStorage.getItem(STORAGE_KEY);
    if (!value) return {};
    return JSON.parse(value) as BranchCache;
  } catch (e) {
    return {};
  }
};

const write = (data: BranchCache) => {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
  } catch (e) {
    // ignore
  }
};

export const setCachedBranch = (projectId: number, branch?: string | null) => {
  const data = read();
  if (branch) {
    data[String(projectId)] = branch;
  } else {
    delete data[String(projectId)];
  }
  write(data);
};

export const getCachedBranch = (projectId: number) => {
  const data = read();
  return data[String(projectId)];
};
