export type OAuth2ConsentProject = { id: number; name: string };

export type ConsentProjectInfo = {
  project?: OAuth2ConsentProject | null;
  requestedProjectId?: number | null;
};

export const deriveConsentProjects = (info: ConsentProjectInfo) => {
  const requestedInaccessible =
    info.requestedProjectId != null && !info.project;
  const projectOptions = info.project ? [info.project] : [];
  return { requestedInaccessible, projectOptions };
};
