import {
  checkItemsInMenu,
  RUN,
  visitProjectWithPermissions,
} from '../../../common/permissions/main';

describe('Permissions per language', () => {
  it('translations.view', () => {
    visitProjectWithPermissions({
      scopes: ['translations.view'],
      viewLanguageTags: ['en', 'de'],
    }).then((projectInfo) => {
      checkItemsInMenu(projectInfo, {
        'project-menu-item-dashboard': RUN,
        'project-menu-item-translations': RUN,
        'project-menu-item-export': RUN,
        'project-menu-item-integrate': RUN,
      });
    });
  });

  it('translations.edit', () => {
    visitProjectWithPermissions({
      scopes: ['translations.edit'],
      translateLanguageTags: ['de'],
    }).then((projectInfo) => {
      checkItemsInMenu(projectInfo, {
        'project-menu-item-dashboard': RUN,
        'project-menu-item-translations': RUN,
        'project-menu-item-import': RUN,
        'project-menu-item-export': RUN,
        'project-menu-item-integrate': RUN,
      });
    });
  });

  it('translations.state-edit', () => {
    visitProjectWithPermissions({
      scopes: ['translations.view', 'translations.state-edit'],
      stateChangeLanguageTags: ['de'],
    }).then((projectInfo) => {
      checkItemsInMenu(projectInfo, {
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
      checkItemsInMenu(projectInfo, {
        'project-menu-item-dashboard': RUN,
        'project-menu-item-translations': RUN,
        'project-menu-item-import': RUN,
        'project-menu-item-export': RUN,
        'project-menu-item-integrate': RUN,
      });
    });
  });
});
