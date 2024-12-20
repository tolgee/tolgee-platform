import {
  checkPermissions,
  RUN,
  SKIP,
  visitProjectWithPermissions,
} from '../../../common/permissions/main';

describe('Keys permissions 2', () => {
  it('keys.edit', () => {
    visitProjectWithPermissions({ scopes: ['keys.edit'] }, true).then(
      (projectInfo) => {
        checkPermissions(projectInfo, {
          'project-menu-item-dashboard': SKIP,
          'project-menu-item-translations': RUN,
          'project-menu-item-integrate': SKIP,
        });
      }
    );
  });

  it('keys.delete', () => {
    visitProjectWithPermissions({ scopes: ['keys.delete'] }, true).then(
      (projectInfo) => {
        checkPermissions(projectInfo, {
          'project-menu-item-dashboard': SKIP,
          'project-menu-item-translations': RUN,
          'project-menu-item-integrate': SKIP,
        });
      }
    );
  });

  it('keys.create', () => {
    visitProjectWithPermissions({ scopes: ['keys.create'] }, true).then(
      (projectInfo) => {
        checkPermissions(projectInfo, {
          'project-menu-item-dashboard': SKIP,
          'project-menu-item-translations': RUN,
          'project-menu-item-integrate': SKIP,
        });
      }
    );
  });
});
