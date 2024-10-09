import { login } from '../../../common/apiCalls/common';
import {
  checkPermissions,
  RUN,
  SKIP,
  visitProjectWithPermissions,
} from '../../../common/permissions/main';

describe('Server admin 3', () => {
  it('Server admin', () => {
    visitProjectWithPermissions({ scopes: ['admin'] }).then((projectInfo) => {
      // login as admin
      login('Server admin', 'admin');
      checkPermissions(projectInfo, {
        'project-menu-item-dashboard': SKIP,
        'project-menu-item-translations': SKIP,
        'project-menu-item-tasks': RUN,
        'project-menu-item-settings': RUN,
        'project-menu-item-languages': RUN,
        'project-menu-item-members': RUN,
        'project-menu-item-import': RUN,
        'project-menu-item-export': RUN,
        'project-menu-item-developer': RUN,
        'project-menu-item-integrate': RUN,
      });
    });
  });
});
