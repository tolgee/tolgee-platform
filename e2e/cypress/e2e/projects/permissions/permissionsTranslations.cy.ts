import {
  checkItemsInMenu,
  RUN,
  SKIP,
  visitProjectWithPermissions,
} from '../../../common/permissions/main';

describe('Permissions translations', () => {
  it('translations.view', () => {
    visitProjectWithPermissions({ scopes: ['translations.view'] }).then(
      (projectInfo) => {
        checkItemsInMenu(projectInfo, {
          'project-menu-item-dashboard': RUN,
          'project-menu-item-translations': RUN,
          'project-menu-item-export': RUN,
          'project-menu-item-integrate': RUN,
        });
      }
    );
  });

  it('translations.edit', () => {
    visitProjectWithPermissions({ scopes: ['translations.edit'] }).then(
      (projectInfo) => {
        checkItemsInMenu(projectInfo, {
          'project-menu-item-dashboard': RUN,
          'project-menu-item-translations': RUN,
          'project-menu-item-import': RUN,
          'project-menu-item-export': RUN,
          'project-menu-item-integrate': RUN,
        });
      }
    );
  });

  it('translations.state-edit', () => {
    visitProjectWithPermissions({ scopes: ['translations.state-edit'] }).then(
      (projectInfo) => {
        checkItemsInMenu(projectInfo, {
          'project-menu-item-dashboard': RUN,
          'project-menu-item-export': RUN,
        });
      }
    );
  });

  it('translation-comments.add', () => {
    visitProjectWithPermissions({ scopes: ['translation-comments.add'] }).then(
      (projectInfo) => {
        checkItemsInMenu(projectInfo, {
          'project-menu-item-dashboard': SKIP,
          'project-menu-item-translations': RUN,
          'project-menu-item-export': SKIP,
          'project-menu-item-integrate': SKIP,
        });
      }
    );
  });

  it('translation-comments.edit', () => {
    visitProjectWithPermissions({ scopes: ['translation-comments.edit'] }).then(
      (projectInfo) => {
        checkItemsInMenu(projectInfo, {
          'project-menu-item-dashboard': SKIP,
          'project-menu-item-translations': RUN,
          'project-menu-item-export': SKIP,
          'project-menu-item-integrate': SKIP,
        });
      }
    );
  });

  it('translation-comments.set-state', () => {
    visitProjectWithPermissions({
      scopes: ['translation-comments.set-state'],
    }).then((projectInfo) => {
      checkItemsInMenu(projectInfo, {
        'project-menu-item-dashboard': SKIP,
        'project-menu-item-translations': RUN,
        'project-menu-item-export': SKIP,
        'project-menu-item-integrate': SKIP,
      });
    });
  });
});
