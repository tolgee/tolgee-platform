import { login } from '../../../common/apiCalls/common';
import {
  checkPermissions,
  RUN,
  SKIP,
  visitProjectWithPermissions,
} from '../../../common/permissions/main';

describe('Server admin 2', () => {
  it('Server admin', () => {
    visitProjectWithPermissions({ scopes: ['admin'] }).then((projectInfo) => {
      // login as admin
      login('Server admin', 'admin');
      checkPermissions(projectInfo, {
        'project-menu-item-dashboard': SKIP,
        'project-menu-item-translations': RUN,
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
