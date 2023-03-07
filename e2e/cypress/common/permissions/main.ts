import Bluebird from 'cypress/types/bluebird';
import { login } from '../apiCalls/common';
import {
  generatePermissionsData,
  PermissionsOptions,
} from '../apiCalls/testData/testData';
import { HOST } from '../constants';
import { visitProjectDashboard } from '../shared';
import { selectLangsInLocalstorage } from '../translations';
import { testDashboard } from './dashboard';
import { testKeys } from './keys';
import { getProjectInfo, pageIsPermitted, ProjectInfo } from './shared';
import { testTranslations } from './translations';

export const SKIP = false;
export const RUN = true;

export function checkNumberOfMenuItems(count: number) {
  cy.gcy('project-menu-items')
    .findDcy('project-menu-item')
    .should('have.length', count);
}

type MenuItem = Exclude<
  DataCy.Value & `project-menu-item-${string}`,
  'project-menu-item-projects'
>;

type Settings = Partial<Record<MenuItem, boolean>>;

export function checkItemsInMenu(projectInfo: ProjectInfo, settings: Settings) {
  checkNumberOfMenuItems(Object.keys(settings).length + 1);
  Object.keys(settings).forEach((item: MenuItem) => {
    const value = settings[item];

    if (value !== SKIP) {
      // go to page
      cy.gcy(item).click();
      // check if there an error
      pageIsPermitted();

      switch (item as keyof MenuItem) {
        case 'project-menu-item-dashboard':
          testDashboard(projectInfo);
          break;
        case 'project-menu-item-translations':
          testKeys(projectInfo);
          testTranslations(projectInfo);
          break;
      }
    }
  });
}

type UserMail = 'admin@admin.com' | 'member@member.com' | 'me@me.me';

export function loginAndGetInfo(user: UserMail, projectId: number) {
  return login(user)
    .then(() => selectLangsInLocalstorage(projectId, ['en', 'de', 'cs']))
    .then(() => getProjectInfo(projectId));
}

export function visitProjectWithPermissions(
  options: Partial<PermissionsOptions>,
  user: UserMail = 'me@me.me'
): Bluebird<ProjectInfo> {
  return new Cypress.Promise<ProjectInfo>((resolve) => {
    generatePermissionsData
      .clean()
      .then(() => generatePermissionsData.generate(options))
      .then((res) => {
        return res.body.projects[0].id;
      })
      .then((projectId) => loginAndGetInfo(user, projectId))
      .then((projectInfo) =>
        visitProjectDashboard(projectInfo.project.id).then(() =>
          resolve(projectInfo)
        )
      );
  });
}
