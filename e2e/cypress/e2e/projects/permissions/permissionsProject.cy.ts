import {
  checkItemsInMenu,
  RUN,
  visitProjectWithPermissions,
} from '../../../common/permissions';

describe('Lowest permissions', () => {
  it('activity.view', () => {
    visitProjectWithPermissions({ scopes: ['activity.view'] }).then(
      ({ permissions }) => {
        checkItemsInMenu(permissions, {
          'project-menu-item-dashboard': RUN,
        });
      }
    );
  });

  it('languages.edit', () => {
    visitProjectWithPermissions({ scopes: ['languages.edit'] }).then(
      ({ permissions }) => {
        checkItemsInMenu(permissions, {
          'project-menu-item-dashboard': RUN,
          'project-menu-item-languages': RUN,
        });
      }
    );
  });

  it('project.edit', () => {
    visitProjectWithPermissions({ scopes: ['project.edit'] }).then(
      ({ permissions }) => {
        checkItemsInMenu(permissions, {
          'project-menu-item-dashboard': RUN,
          'project-menu-item-settings': RUN,
        });
      }
    );
  });

  it('members.view', () => {
    visitProjectWithPermissions({ scopes: ['members.view'] }).then(
      ({ permissions }) => {
        checkItemsInMenu(permissions, {
          'project-menu-item-dashboard': RUN,
          'project-menu-item-members': RUN,
        });
      }
    );
  });

  it('members.edit', () => {
    visitProjectWithPermissions({ scopes: ['members.edit'] }).then(
      ({ permissions }) => {
        checkItemsInMenu(permissions, {
          'project-menu-item-dashboard': RUN,
          'project-menu-item-members': RUN,
        });
      }
    );
  });
});
