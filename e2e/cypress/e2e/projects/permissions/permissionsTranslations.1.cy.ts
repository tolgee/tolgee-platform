import {
  checkPermissions,
  RUN,
  visitProjectWithPermissions,
} from '../../../common/permissions/main';

describe('Permissions translations 1', () => {
  it('translations.view', () => {
    visitProjectWithPermissions({ scopes: ['translations.view'] }).then(
      (projectInfo) => {
        checkPermissions(projectInfo, {
          'project-menu-item-dashboard': RUN,
          'project-menu-item-translations': RUN,
          'project-menu-item-export': RUN,
          'project-menu-item-integrate': RUN,
        });
      }
    );
  });

  it('translations.edit', () => {
    visitProjectWithPermissions({ scopes: ['translations.edit'] }).then(
      (projectInfo) => {
        checkPermissions(projectInfo, {
          'project-menu-item-dashboard': RUN,
          'project-menu-item-translations': RUN,
          'project-menu-item-import': RUN,
          'project-menu-item-export': RUN,
          'project-menu-item-integrate': RUN,
        });
      }
    );
  });
});
