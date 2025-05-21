import {
  checkPermissions,
  RUN,
  SKIP,
  visitProjectWithPermissions,
} from '../../../common/permissions/main';

describe('Permissions AI playground', () => {
  it('prompts.view', () => {
    visitProjectWithPermissions({ scopes: ['prompts.view'] }).then(
      (projectInfo) => {
        checkPermissions(projectInfo, {
          'project-menu-item-dashboard': SKIP,
          'project-menu-item-ai': RUN,
        });
      }
    );
  });

  it('prompts.edit', () => {
    visitProjectWithPermissions({ scopes: ['prompts.edit'] }).then(
      (projectInfo) => {
        checkPermissions(projectInfo, {
          'project-menu-item-dashboard': SKIP,
          'project-menu-item-translations': RUN,
          'project-menu-item-languages': RUN,
          'project-menu-item-export': RUN,
          'project-menu-item-integrate': RUN,
          'project-menu-item-ai': RUN,
          'project-menu-item-settings': RUN,
        });
      }
    );
  });
});
