type ConsentProjectInfo = {
  project?: { id: number; name: string } | null;
  requestedProjectId?: number | null;
};

export const isRequestedProjectInaccessible = (
  info: ConsentProjectInfo
): boolean => info.requestedProjectId != null && !info.project;
