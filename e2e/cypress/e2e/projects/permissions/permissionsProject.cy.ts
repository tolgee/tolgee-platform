import {
  checkPermissions,
  RUN,
  visitProjectWithPermissions,
} from '../../../common/permissions/main';

describe('Permissions project', () => {
  it('activity.view', () => {
    visitProjectWithPermissions({ scopes: ['activity.view'] }).then(
      (projectInfo) => {
        checkPermissions(projectInfo, {
          'project-menu-item-dashboard': RUN,
        });
      }
    );
  });

  it('languages.edit', () => {
    visitProjectWithPermissions({ scopes: ['languages.edit'] }).then(
      (projectInfo) => {
        checkPermissions(projectInfo, {
          'project-menu-item-dashboard': RUN,
          'project-menu-item-languages': RUN,
          'project-menu-item-ai': RUN,
        });
      }
    );
  });

  it('project.edit', () => {
    visitProjectWithPermissions({ scopes: ['project.edit'] }).then(
      (projectInfo) => {
        checkPermissions(projectInfo, {
          'project-menu-item-dashboard': RUN,
          'project-menu-item-settings': RUN,
        });
      }
    );
  });

  it('members.view', () => {
    visitProjectWithPermissions({ scopes: ['members.view'] }).then(
      (projectInfo) => {
        checkPermissions(projectInfo, {
          'project-menu-item-dashboard': RUN,
          'project-menu-item-members': RUN,
        });
      }
    );
  });

  it('members.edit', () => {
    visitProjectWithPermissions({ scopes: ['members.edit'] }).then(
      (projectInfo) => {
        checkPermissions(projectInfo, {
          'project-menu-item-dashboard': RUN,
          'project-menu-item-members': RUN,
        });
      }
    );
  });
});
