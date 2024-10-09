import {
  checkPermissions,
  RUN,
  SKIP,
  visitProjectWithPermissions,
} from '../../../common/permissions/main';

describe('Batch jobs permissions 2', () => {
  it('translations.batch-by-mt', () => {
    visitProjectWithPermissions({
      scopes: ['translations.batch-by-tm'],
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
