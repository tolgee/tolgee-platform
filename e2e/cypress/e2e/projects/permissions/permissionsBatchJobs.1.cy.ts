import {
  checkPermissions,
  RUN,
  SKIP,
  visitProjectWithPermissions,
} from '../../../common/permissions/main';

describe('Batch jobs permissions 1', () => {
  it('translations.batch-machine', () => {
    visitProjectWithPermissions({
      scopes: ['translations.batch-machine'],
    }).then((projectInfo) => {
      checkPermissions(projectInfo, {
        'project-menu-item-dashboard': RUN,
        'project-menu-item-translations': RUN,
        'project-menu-item-import': SKIP,
        'project-menu-item-export': SKIP,
        'project-menu-item-integrate': SKIP,
      });
    });
  });
});
