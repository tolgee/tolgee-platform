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

  // tasks.assigned-access grants no project visibility of its own, so it is paired with translations.view here:
  // testMyTasks only runs under the translations menu item, which this scope alone would not show.
  it('translations.view + tasks.assigned-access', () => {
    visitProjectWithPermissions({
      scopes: ['translations.view', 'tasks.assigned-access'],
    }).then((projectInfo) => {
      checkPermissions(projectInfo, {
        'project-menu-item-dashboard': RUN,
        'project-menu-item-translations': RUN,
        'project-menu-item-export': RUN,
        'project-menu-item-integrate': RUN,
      });
    });
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
