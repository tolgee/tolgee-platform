import {
  checkItemsInMenu,
  RUN,
  visitProjectWithPermissions,
} from '../../../common/permissions/main';

describe('Permissions admin', () => {
  it('admin', () => {
    visitProjectWithPermissions({ scopes: ['admin'] }).then((projectInfo) => {
      checkItemsInMenu(projectInfo, {
        'project-menu-item-translations': RUN,
        'project-menu-item-dashboard': RUN,
        'project-menu-item-settings': RUN,
        'project-menu-item-languages': RUN,
        'project-menu-item-members': RUN,
        'project-menu-item-import': RUN,
        'project-menu-item-export': RUN,
        'project-menu-item-integrate': RUN,
      });
    });
  });
});
