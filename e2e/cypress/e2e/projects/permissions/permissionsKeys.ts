import {
  checkItemsInMenu,
  RUN,
  SKIP,
  visitProjectWithPermissions,
} from '../../../common/permissions';

describe('Permissions translations', () => {
  it('screenshots.view', () => {
    visitProjectWithPermissions({ scopes: ['screenshots.view'] }).then(
      ({ permissions }) => {
        checkItemsInMenu(permissions, {
          'project-menu-item-dashboard': SKIP,
          'project-menu-item-translations': RUN,
          'project-menu-item-integrate': SKIP,
        });
      }
    );
  });

  it('screenshots.upload', () => {
    visitProjectWithPermissions({ scopes: ['screenshots.upload'] }).then(
      ({ permissions }) => {
        checkItemsInMenu(permissions, {
          'project-menu-item-dashboard': RUN,
          'project-menu-item-translations': RUN,
          'project-menu-item-integrate': RUN,
        });
      }
    );
  });

  it('screenshots.delete', () => {
    visitProjectWithPermissions({ scopes: ['screenshots.delete'] }).then(
      ({ permissions }) => {
        checkItemsInMenu(permissions, {
          'project-menu-item-dashboard': SKIP,
          'project-menu-item-translations': RUN,
          'project-menu-item-integrate': SKIP,
        });
      }
    );
  });

  it('keys.view', () => {
    visitProjectWithPermissions({ scopes: ['keys.view'] }).then(
      ({ permissions }) => {
        checkItemsInMenu(permissions, {
          'project-menu-item-dashboard': RUN,
          'project-menu-item-translations': RUN,
          'project-menu-item-integrate': SKIP,
        });
      }
    );
  });

  it('keys.edit', () => {
    visitProjectWithPermissions({ scopes: ['keys.edit'] }).then(
      ({ permissions }) => {
        checkItemsInMenu(permissions, {
          'project-menu-item-dashboard': SKIP,
          'project-menu-item-translations': RUN,
          'project-menu-item-import': RUN,
          'project-menu-item-integrate': SKIP,
        });
      }
    );
  });

  it('keys.delete', () => {
    visitProjectWithPermissions({ scopes: ['keys.delete'] }).then(
      ({ permissions }) => {
        checkItemsInMenu(permissions, {
          'project-menu-item-dashboard': SKIP,
          'project-menu-item-translations': RUN,
          'project-menu-item-integrate': SKIP,
        });
      }
    );
  });

  it('keys.create', () => {
    visitProjectWithPermissions({ scopes: ['keys.create'] }).then(
      ({ permissions }) => {
        checkItemsInMenu(permissions, {
          'project-menu-item-dashboard': SKIP,
          'project-menu-item-translations': RUN,
          'project-menu-item-integrate': SKIP,
        });
      }
    );
  });
});
