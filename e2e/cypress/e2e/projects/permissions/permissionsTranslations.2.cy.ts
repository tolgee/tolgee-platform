import {
  checkPermissions,
  RUN,
  SKIP,
  visitProjectWithPermissions,
} from '../../../common/permissions/main';

describe('Permissions translations 2', () => {
  it('translations.state-edit', () => {
    visitProjectWithPermissions({ scopes: ['translations.state-edit'] }).then(
      (projectInfo) => {
        checkPermissions(projectInfo, {
          'project-menu-item-dashboard': RUN,
          'project-menu-item-translations': RUN,
          'project-menu-item-export': RUN,
          'project-menu-item-integrate': SKIP,
        });
      }
    );
  });

  it('translation-comments.add', () => {
    visitProjectWithPermissions({ scopes: ['translation-comments.add'] }).then(
      (projectInfo) => {
        checkPermissions(projectInfo, {
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
        checkPermissions(projectInfo, {
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
      checkPermissions(projectInfo, {
        'project-menu-item-dashboard': SKIP,
        'project-menu-item-translations': RUN,
        'project-menu-item-export': SKIP,
        'project-menu-item-integrate': SKIP,
      });
    });
  });
});
