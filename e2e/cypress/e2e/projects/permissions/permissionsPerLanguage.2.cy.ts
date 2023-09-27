import {
  checkPermissions,
  RUN,
  visitProjectWithPermissions,
} from '../../../common/permissions/main';

describe('Permissions per language 2', () => {
  it('translations.state-edit', () => {
    visitProjectWithPermissions({
      scopes: ['translations.view', 'translations.state-edit'],
      stateChangeLanguageTags: ['de'],
    }).then((projectInfo) => {
      checkPermissions(projectInfo, {
        'project-menu-item-dashboard': RUN,
        'project-menu-item-translations': RUN,
        'project-menu-item-export': RUN,
        'project-menu-item-integrate': RUN,
      });
    });
  });

  it('combined translations.edit and translations.state-edit', () => {
    visitProjectWithPermissions({
      scopes: ['translations.edit', 'translations.state-edit'],
      translateLanguageTags: ['cs'],
      stateChangeLanguageTags: ['de'],
    }).then((projectInfo) => {
      checkPermissions(projectInfo, {
        'project-menu-item-dashboard': RUN,
        'project-menu-item-translations': RUN,
        'project-menu-item-import': RUN,
        'project-menu-item-export': RUN,
        'project-menu-item-integrate': RUN,
      });
    });
  });
});
