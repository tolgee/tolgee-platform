import { login, v2apiFetch } from '../../common/apiCalls/common';
import {
  generatePermissionsData,
  PermissionsOptions,
} from '../../common/apiCalls/testData/testData';
import { HOST } from '../../common/constants';
import {
  checkItemsInMenu,
  ComputedPermissionModel,
  RUN,
  SKIP,
} from '../../common/permissions';

describe('Lowest permissions', () => {
  let projectId: number;
  let permissions: ComputedPermissionModel;

  it('translations.view', () => {
    visitProject({ scopes: ['translations.view'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': RUN,
        'project-menu-item-translations': RUN,
        'project-menu-item-export': RUN,
        'project-menu-item-integrate': RUN,
      });
    });
  });

  it('translations.edit', () => {
    visitProject({ scopes: ['translations.edit'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': RUN,
        'project-menu-item-translations': RUN,
        'project-menu-item-import': RUN,
        'project-menu-item-export': RUN,
        'project-menu-item-integrate': RUN,
      });
    });
  });

  it('translations.state-edit', () => {
    visitProject({ scopes: ['translations.state-edit'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': RUN,
        'project-menu-item-export': RUN,
      });
    });
  });

  it('translation-comments.add', () => {
    visitProject({ scopes: ['translation-comments.add'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': SKIP,
        'project-menu-item-translations': RUN,
        'project-menu-item-export': SKIP,
        'project-menu-item-integrate': SKIP,
      });
    });
  });

  it('translation-comments.edit', () => {
    visitProject({ scopes: ['translation-comments.edit'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': SKIP,
        'project-menu-item-translations': RUN,
        'project-menu-item-export': SKIP,
        'project-menu-item-integrate': SKIP,
      });
    });
  });

  it('translation-comments.set-state', () => {
    visitProject({ scopes: ['translation-comments.set-state'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': SKIP,
        'project-menu-item-translations': RUN,
        'project-menu-item-export': SKIP,
        'project-menu-item-integrate': SKIP,
      });
    });
  });

  it('screenshots.view', () => {
    visitProject({ scopes: ['screenshots.view'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': SKIP,
        'project-menu-item-translations': RUN,
        'project-menu-item-integrate': SKIP,
      });
    });
  });

  it('screenshots.upload', () => {
    visitProject({ scopes: ['screenshots.upload'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': RUN,
        'project-menu-item-translations': RUN,
        'project-menu-item-integrate': RUN,
      });
    });
  });

  it('screenshots.delete', () => {
    visitProject({ scopes: ['screenshots.delete'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': SKIP,
        'project-menu-item-translations': RUN,
        'project-menu-item-integrate': SKIP,
      });
    });
  });

  it('activity.view', () => {
    visitProject({ scopes: ['activity.view'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': RUN,
      });
    });
  });

  it('languages.edit', () => {
    visitProject({ scopes: ['languages.edit'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': RUN,
        'project-menu-item-languages': RUN,
      });
    });
  });

  it('admin', () => {
    visitProject({ scopes: ['admin'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-translations': RUN,
        'project-menu-item-dashboard': RUN,
        'project-menu-item-settings': RUN,
        'project-menu-item-languages': RUN,
        'project-menu-item-members': RUN,
        'project-menu-item-import': RUN,
        'project-menu-item-export': RUN,
        'project-menu-item-integrate': SKIP,
      });
    });
  });

  it('project.edit', () => {
    visitProject({ scopes: ['project.edit'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': RUN,
        'project-menu-item-settings': RUN,
      });
    });
  });

  it('members.view', () => {
    visitProject({ scopes: ['members.view'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': RUN,
        'project-menu-item-members': RUN,
      });
    });
  });

  it('members.edit', () => {
    visitProject({ scopes: ['members.edit'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': RUN,
        'project-menu-item-members': RUN,
      });
    });
  });

  it('keys.view', () => {
    visitProject({ scopes: ['keys.view'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': RUN,
        'project-menu-item-translations': RUN,
        'project-menu-item-integrate': SKIP,
      });
    });
  });

  it('keys.edit', () => {
    visitProject({ scopes: ['keys.edit'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': SKIP,
        'project-menu-item-translations': RUN,
        'project-menu-item-import': RUN,
        'project-menu-item-integrate': SKIP,
      });
    });
  });

  it('keys.delete', () => {
    visitProject({ scopes: ['keys.delete'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': SKIP,
        'project-menu-item-translations': RUN,
        'project-menu-item-integrate': SKIP,
      });
    });
  });

  it('keys.create', () => {
    visitProject({ scopes: ['keys.create'] }).then(() => {
      checkItemsInMenu(permissions, {
        'project-menu-item-dashboard': SKIP,
        'project-menu-item-translations': RUN,
        'project-menu-item-integrate': SKIP,
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
