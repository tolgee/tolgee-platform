import {
  checkPermissions,
  RUN,
  SKIP,
  visitProjectWithPermissions,
} from '../../../common/permissions/main';

describe('Developer section', () => {
  it('webhooks.manage', () => {
    visitProjectWithPermissions({
      scopes: ['webhooks.manage'],
    }).then((projectInfo) => {
      checkPermissions(projectInfo, {
        'project-menu-item-dashboard': SKIP,
        'project-menu-item-developer': RUN,
      });
    });
  });

  it('content-delivery.publish', () => {
    visitProjectWithPermissions({
      scopes: ['content-delivery.publish'],
    }).then((projectInfo) => {
      checkPermissions(projectInfo, {
        'project-menu-item-dashboard': SKIP,
        'project-menu-item-developer': RUN,
      });
    });
  });

  it('content-delivery.manage', () => {
    visitProjectWithPermissions({
      scopes: ['content-delivery.manage'],
    }).then((projectInfo) => {
      checkPermissions(projectInfo, {
        'project-menu-item-dashboard': SKIP,
        'project-menu-item-developer': RUN,
      });
    });
  });
});
