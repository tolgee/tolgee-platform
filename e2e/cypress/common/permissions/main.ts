import Bluebird from 'cypress/types/bluebird';
import { login } from '../apiCalls/common';
import {
  generatePermissionsData,
  PermissionsOptions,
} from '../apiCalls/testData/testData';
import { HOST } from '../constants';
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

export function visitProjectWithPermissions(
  options: Partial<PermissionsOptions>
): Bluebird<ProjectInfo> {
  let projectInfo: ProjectInfo;
  let projectId: number;

  // combining regular promises with cypress promises results in this shit
  return new Cypress.Promise<ProjectInfo>((resolve) => {
    generatePermissionsData
      .clean()
      .then(() => generatePermissionsData.generate(options))
      .then((res) => {
        projectId = res.body.projects[0].id;
      })
      .then(() => login('me@me.me'))
      .then(() => selectLangsInLocalstorage(projectId, ['en', 'de', 'cs']))
      .then(() => getProjectInfo(projectId))
      .then((data) => {
        projectInfo = data;
      })
      .then(() =>
        cy
          .visit(`${HOST}/projects/${projectId}`)
          .then(() => resolve(projectInfo))
      );
  });
}
