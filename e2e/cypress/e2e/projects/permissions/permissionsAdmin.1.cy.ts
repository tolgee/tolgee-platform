import {
  checkPermissions,
  RUN,
  SKIP,
  visitProjectWithPermissions,
} from '../../../common/permissions/main';

describe('Permissions admin 1', () => {
  it('admin', () => {
    visitProjectWithPermissions({ scopes: ['admin'] }).then((projectInfo) => {
      checkPermissions(projectInfo, {
        'project-menu-item-dashboard': RUN,
        'project-menu-item-translations': SKIP,
        'project-menu-item-tasks': SKIP,
        'project-menu-item-settings': SKIP,
        'project-menu-item-languages': SKIP,
        'project-menu-item-members': SKIP,
        'project-menu-item-import': SKIP,
        'project-menu-item-export': SKIP,
        'project-menu-item-developer': SKIP,
        'project-menu-item-integrate': SKIP,
      });
    });
  });
});
