import { login } from '../../common/apiCalls/common';
import {
  generatePermissionsData,
  PermissionsOptions,
} from '../../common/apiCalls/testData/testData';
import { HOST } from '../../common/constants';
import { checkItemsInMenu } from '../../common/permissions';

describe('Lowest permissions', () => {
  let projectId: number;

  it('translations.view', () => {
    visitProject({ scopes: ['translations.view'] });
    checkItemsInMenu([
      'project-menu-item-dashboard',
      'project-menu-item-translations',
      'project-menu-item-export',
      'project-menu-item-integrate',
    ]);
  });

  it('translations.edit', () => {
    visitProject({ scopes: ['translations.edit'] });
    checkItemsInMenu([
      'project-menu-item-dashboard',
      'project-menu-item-translations',
      'project-menu-item-import',
      'project-menu-item-export',
      'project-menu-item-integrate',
    ]);
  });

  it('keys.edit', () => {
    visitProject({ scopes: ['keys.edit'] });
    checkItemsInMenu([
      'project-menu-item-dashboard',
      'project-menu-item-translations',
      'project-menu-item-import',
      'project-menu-item-integrate',
    ]);
  });

  it('screenshots.upload', () => {
    visitProject({ scopes: ['screenshots.upload'] });
    checkItemsInMenu([
      'project-menu-item-dashboard',
      'project-menu-item-translations',
      'project-menu-item-integrate',
    ]);
  });

  it('screenshots.delete', () => {
    visitProject({ scopes: ['screenshots.delete'] });
    checkItemsInMenu([
      'project-menu-item-dashboard',
      'project-menu-item-translations',
      'project-menu-item-integrate',
    ]);
  });

  it('screenshots.view', () => {
    visitProject({ scopes: ['screenshots.view'] });
    checkItemsInMenu([
      'project-menu-item-dashboard',
      'project-menu-item-translations',
      'project-menu-item-integrate',
    ]);
  });

  it('activity.view', () => {
    visitProject({ scopes: ['activity.view'] });
    checkItemsInMenu([
      'project-menu-item-dashboard',
      'project-menu-item-integrate',
    ]);
  });

  it('languages.edit', () => {
    visitProject({ scopes: ['languages.edit'] });
    checkItemsInMenu([
      'project-menu-item-dashboard',
      'project-menu-item-languages',
      'project-menu-item-integrate',
    ]);
  });

  it('admin', () => {
    visitProject({ scopes: ['admin'] });
    checkItemsInMenu([
      'project-menu-item-dashboard',
      'project-menu-item-translations',
      'project-menu-item-settings',
      'project-menu-item-languages',
      'project-menu-item-members',
      'project-menu-item-import',
      'project-menu-item-export',
      'project-menu-item-integrate',
    ]);
  });

  it('project.edit', () => {
    visitProject({ scopes: ['project.edit'] });
    checkItemsInMenu([
      'project-menu-item-dashboard',
      'project-menu-item-settings',
      'project-menu-item-integrate',
    ]);
  });

  it('members.view', () => {
    visitProject({ scopes: ['members.view'] });
    checkItemsInMenu([
      'project-menu-item-dashboard',
      'project-menu-item-members',
      'project-menu-item-integrate',
    ]);
  });

  it('members.edit', () => {
    visitProject({ scopes: ['members.edit'] });
    checkItemsInMenu([
      'project-menu-item-dashboard',
      'project-menu-item-members',
      'project-menu-item-integrate',
    ]);
  });

  function visitProject(options: Partial<PermissionsOptions>) {
    generatePermissionsData
      .clean()
      .then(() => generatePermissionsData.generate(options))
      .then((res) => {
        projectId = res.body.projects[0].id;
      })
      .then(() => login('me@me.me'))
      .then(() => cy.visit(`${HOST}/projects/${projectId}`));
  }
});
