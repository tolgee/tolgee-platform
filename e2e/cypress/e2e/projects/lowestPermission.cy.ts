import { login, v2apiFetch } from '../../common/apiCalls/common';
import {
  generatePermissionsData,
  PermissionsOptions,
} from '../../common/apiCalls/testData/testData';
import { HOST } from '../../common/constants';
import {
  checkItemsInMenu,
  ComputedPermissionModel,
} from '../../common/permissions';

describe('Lowest permissions', () => {
  let projectId: number;
  let permissions: ComputedPermissionModel;

  it('translations.view', () => {
    visitProject({ scopes: ['translations.view'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': true,
        'project-menu-item-translations': true,
        'project-menu-item-export': true,
        'project-menu-item-integrate': true,
      });
    });
  });

  it('translations.edit', () => {
    visitProject({ scopes: ['translations.edit'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': true,
        'project-menu-item-translations': true,
        'project-menu-item-import': true,
        'project-menu-item-export': true,
        'project-menu-item-integrate': true,
      });
    });
  });

  it('keys.edit', () => {
    visitProject({ scopes: ['keys.edit'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': true,
        'project-menu-item-translations': true,
        'project-menu-item-import': true,
        'project-menu-item-integrate': true,
      });
    });
  });

  it('screenshots.upload', () => {
    visitProject({ scopes: ['screenshots.upload'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': true,
        'project-menu-item-translations': true,
        'project-menu-item-integrate': true,
      });
    });
  });

  it('screenshots.delete', () => {
    visitProject({ scopes: ['screenshots.delete'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': true,
        'project-menu-item-translations': true,
        'project-menu-item-integrate': true,
      });
    });
  });

  it('screenshots.view', () => {
    visitProject({ scopes: ['screenshots.view'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': true,
        'project-menu-item-translations': true,
        'project-menu-item-integrate': true,
      });
    });
  });

  it('activity.view', () => {
    visitProject({ scopes: ['activity.view'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': true,
        'project-menu-item-integrate': true,
      });
    });
  });

  it('languages.edit', () => {
    visitProject({ scopes: ['languages.edit'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': true,
        'project-menu-item-languages': true,
        'project-menu-item-integrate': true,
      });
    });
  });

  it('admin', () => {
    visitProject({ scopes: ['admin'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-translations': true,
        'project-menu-item-dashboard': true,
        'project-menu-item-settings': true,
        'project-menu-item-languages': true,
        'project-menu-item-members': true,
        'project-menu-item-import': true,
        'project-menu-item-export': true,
        'project-menu-item-integrate': true,
      });
    });
  });

  it('project.edit', () => {
    visitProject({ scopes: ['project.edit'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': true,
        'project-menu-item-settings': true,
        'project-menu-item-integrate': true,
      });
    });
  });

  it('members.view', () => {
    visitProject({ scopes: ['members.view'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': true,
        'project-menu-item-members': true,
        'project-menu-item-integrate': true,
      });
    });
  });

  it('members.edit', () => {
    visitProject({ scopes: ['members.edit'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': true,
        'project-menu-item-members': true,
        'project-menu-item-integrate': true,
      });
    });
  });

  it('translation-comments.add', () => {
    visitProject({ scopes: ['translation-comments.add'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': true,
        'project-menu-item-translations': true,
        'project-menu-item-export': true,
        'project-menu-item-integrate': true,
      });
    });
  });

  it('translation-comments.edit', () => {
    visitProject({ scopes: ['translation-comments.edit'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': true,
        'project-menu-item-translations': true,
        'project-menu-item-export': true,
        'project-menu-item-integrate': true,
      });
    });
  });

  it('translation-comments.set-state', () => {
    visitProject({ scopes: ['translation-comments.set-state'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': true,
        'project-menu-item-translations': true,
        'project-menu-item-export': true,
        'project-menu-item-integrate': true,
      });
    });
  });

  it('translations.state-edit', () => {
    visitProject({ scopes: ['translations.state-edit'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': true,
        'project-menu-item-export': true,
        'project-menu-item-integrate': true,
      });
    });
  });

  it('keys.view', () => {
    visitProject({ scopes: ['keys.view'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': true,
        'project-menu-item-translations': true,
        'project-menu-item-integrate': true,
      });
    });
  });

  it('keys.delete', () => {
    visitProject({ scopes: ['keys.delete'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': true,
        'project-menu-item-translations': true,
        'project-menu-item-integrate': true,
      });
    });
  });

  it('keys.create', () => {
    visitProject({ scopes: ['keys.create'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': true,
        'project-menu-item-translations': true,
        'project-menu-item-integrate': true,
      });
    });
  });

  function visitProject(options: Partial<PermissionsOptions>) {
    return generatePermissionsData
      .clean()
      .then(() => generatePermissionsData.generate(options))
      .then((res) => {
        projectId = res.body.projects[0].id;
      })
      .then(() => login('me@me.me'))
      .then(() => v2apiFetch(`projects/${projectId}`))
      .then((res) => {
        permissions = res.body.computedPermission;
      })
      .then(() => cy.visit(`${HOST}/projects/${projectId}`));
  }
});
