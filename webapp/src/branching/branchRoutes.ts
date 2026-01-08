import { LINKS, PARAMS } from 'tg.constants/links';

type BranchRoute = {
  base: string;
  branched: string;
  build: (projectId: number | string, branch?: string) => string;
};

export const BRANCH_ROUTES: Record<string, BranchRoute> = {
  translations: {
    base: LINKS.PROJECT_TRANSLATIONS.template,
    branched: LINKS.PROJECT_TRANSLATIONS_BRANCHED.template,
    build: (projectId, branch) =>
      branch
        ? LINKS.PROJECT_TRANSLATIONS_WITH_BRANCH.build({
            [PARAMS.PROJECT_ID]: projectId,
            [PARAMS.BRANCH]: branch,
          })
        : LINKS.PROJECT_TRANSLATIONS.build({ [PARAMS.PROJECT_ID]: projectId }),
  },
  dashboard: {
    base: LINKS.PROJECT_DASHBOARD.template,
    branched: LINKS.PROJECT_DASHBOARD_BRANCHED.template,
    build: (projectId, branch) =>
      branch
        ? LINKS.PROJECT_DASHBOARD_BRANCHED.build({
            [PARAMS.PROJECT_ID]: projectId,
            [PARAMS.BRANCH]: branch,
          })
        : LINKS.PROJECT_DASHBOARD.build({ [PARAMS.PROJECT_ID]: projectId }),
  },
  import: {
    base: LINKS.PROJECT_IMPORT.template,
    branched: LINKS.PROJECT_IMPORT_BRANCHED.template,
    build: (projectId, branch) =>
      branch
        ? LINKS.PROJECT_IMPORT_BRANCHED.build({
            [PARAMS.PROJECT_ID]: projectId,
            [PARAMS.BRANCH]: branch,
          })
        : LINKS.PROJECT_IMPORT.build({ [PARAMS.PROJECT_ID]: projectId }),
  },
  export: {
    base: LINKS.PROJECT_EXPORT.template,
    branched: LINKS.PROJECT_EXPORT_BRANCHED.template,
    build: (projectId, branch) =>
      branch
        ? LINKS.PROJECT_EXPORT_BRANCHED.build({
            [PARAMS.PROJECT_ID]: projectId,
            [PARAMS.BRANCH]: branch,
          })
        : LINKS.PROJECT_EXPORT.build({ [PARAMS.PROJECT_ID]: projectId }),
  },
  tasks: {
    base: LINKS.PROJECT_TASKS.template,
    branched: LINKS.PROJECT_TASKS_BRANCHED.template,
    build: (projectId, branch) =>
      branch
        ? LINKS.PROJECT_TASKS_BRANCHED.build({
            [PARAMS.PROJECT_ID]: projectId,
            [PARAMS.BRANCH]: branch,
          })
        : LINKS.PROJECT_TASKS.build({ [PARAMS.PROJECT_ID]: projectId }),
  },
};

export type BranchRouteKey = keyof typeof BRANCH_ROUTES;
