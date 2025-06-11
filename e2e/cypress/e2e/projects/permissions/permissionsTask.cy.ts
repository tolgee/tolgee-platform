import {
  checkPermissions,
  RUN,
  visitProjectWithPermissions,
} from '../../../common/permissions/main';

describe('Permissions task', () => {
  it('tasks.view', () => {
    visitProjectWithPermissions({ scopes: ['tasks.view'] }).then(
      (projectInfo) => {
        checkPermissions(projectInfo, {
          'project-menu-item-dashboard': RUN,
          'project-menu-item-translations': RUN,
          'project-menu-item-tasks': RUN,
          'project-menu-item-export': RUN,
          'project-menu-item-integrate': RUN,
        });
      }
    );
  });

  it('tasks.edit', () => {
    visitProjectWithPermissions({ scopes: ['tasks.edit'] }).then(
      (projectInfo) => {
        checkPermissions(projectInfo, {
          'project-menu-item-dashboard': RUN,
          'project-menu-item-translations': RUN,
          'project-menu-item-tasks': RUN,
          'project-menu-item-export': RUN,
          'project-menu-item-integrate': RUN,
        });
      }
    );
  });
});
