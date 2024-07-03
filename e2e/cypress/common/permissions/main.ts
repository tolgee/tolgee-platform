import { login } from '../apiCalls/common';
import {
  generatePermissionsData,
  PermissionsOptions,
} from '../apiCalls/testData/testData';
import { visitProjectDashboard } from '../shared';
import { selectLangsInLocalstorage } from '../translations';
import { testBatchOperations } from './batchOperations';
import { testDashboard } from './dashboard';
import { testDeveloper } from './developer';
import { testExport } from './export';
import { testIntegration } from './integration';
import { testKeys } from './keys';
import { testMembers } from './members';
import { testMyTasks } from './myTasks';
import {
  getProjectInfo,
  pageAcessibleWithoutErrors,
  ProjectInfo,
} from './shared';
import { testTranslations } from './translations';

export const SKIP = false;
export const RUN = true;

export function checkNumberOfMenuItems(items: MenuItem[]) {
  cy.gcy('project-menu-items')
    .findDcy('project-menu-item')
    // there is an extra item "projects"
    .should('have.length', items.length + 1);
  items.forEach((item) => {
    cy.gcy(item).should('be.visible');
  });
}

type MenuItem = Exclude<
  DataCy.Value & `project-menu-item-${string}`,
  'project-menu-item-projects'
>;

type Settings = Partial<Record<MenuItem, boolean>>;

export function checkPermissions(projectInfo: ProjectInfo, settings: Settings) {
  checkNumberOfMenuItems(Object.keys(settings) as MenuItem[]);
  Object.keys(settings).forEach((item: MenuItem) => {
    const value = settings[item];

    if (value !== SKIP) {
      // go to page
      cy.gcy(item).click();
      // check if there an error
      pageAcessibleWithoutErrors();

      switch (item as MenuItem) {
        case 'project-menu-item-dashboard':
          testDashboard(projectInfo);
          break;
        case 'project-menu-item-translations':
          testMyTasks(projectInfo);
          testTranslations(projectInfo);
          testKeys(projectInfo);
          testBatchOperations(projectInfo);
          break;
        case 'project-menu-item-members':
          testMembers(projectInfo);
          break;
        case 'project-menu-item-export':
          testExport(projectInfo);
          break;
        case 'project-menu-item-developer':
          testDeveloper(projectInfo);
          break;
        case 'project-menu-item-integrate':
          testIntegration(projectInfo);
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
): Promise<ProjectInfo> {
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
