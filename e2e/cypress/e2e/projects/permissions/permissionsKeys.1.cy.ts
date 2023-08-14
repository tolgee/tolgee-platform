import {
  checkPermissions,
  RUN,
  SKIP,
  visitProjectWithPermissions,
} from '../../../common/permissions/main';

describe('Keys permissions 1', () => {
  it('screenshots.view', { retries: { runMode: 3 } }, () => {
    visitProjectWithPermissions({ scopes: ['screenshots.view'] }).then(
      (projectInfo) => {
        checkPermissions(projectInfo, {
          'project-menu-item-dashboard': SKIP,
          'project-menu-item-translations': RUN,
          'project-menu-item-integrate': SKIP,
        });
      }
    );
  });

  it('screenshots.upload', { retries: { runMode: 3 } }, () => {
    visitProjectWithPermissions({ scopes: ['screenshots.upload'] }).then(
      (projectInfo) => {
        checkPermissions(projectInfo, {
          'project-menu-item-dashboard': RUN,
          'project-menu-item-translations': RUN,
          'project-menu-item-integrate': RUN,
        });
      }
    );
  });

  it('screenshots.delete', () => {
    visitProjectWithPermissions({ scopes: ['screenshots.delete'] }).then(
      (projectInfo) => {
        checkPermissions(projectInfo, {
          'project-menu-item-dashboard': SKIP,
          'project-menu-item-translations': RUN,
          'project-menu-item-integrate': SKIP,
        });
      }
    );
  });

  it('keys.view', () => {
    visitProjectWithPermissions({ scopes: ['keys.view'] }).then(
      (projectInfo) => {
        checkPermissions(projectInfo, {
          'project-menu-item-dashboard': RUN,
          'project-menu-item-translations': RUN,
          'project-menu-item-integrate': SKIP,
        });
      }
    );
  });
});
