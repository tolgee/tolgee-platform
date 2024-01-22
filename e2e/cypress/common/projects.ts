import { assertMessage, gcy, switchToOrganization } from './shared';
import { HOST } from './constants';
import { waitForGlobalLoading } from './loading';

export const enterProjectSettings = (
  projectName: string,
  organization?: string
) => {
  visitList();
  selectInProjectMoreMenu(projectName, 'project-settings-button', organization);
  return waitForGlobalLoading();
};

export const selectInProjectMoreMenu = (
  projectName: string,
  itemDataCy: Parameters<typeof cy.gcy>[0],
  organization?: string
) => {
  visitList();

  if (organization) {
    switchToOrganization(organization);
  }

  gcy('global-paginated-list')
    .contains(projectName)
    .closestDcy('dashboard-projects-list-item')
    .within(() => {
      cy.gcy('project-list-more-button').click();
    });
  cy.gcy(itemDataCy).should('be.visible').click();
};

export const enterProject = (projectName: string, organization?: string) => {
  visitList();
  if (organization) {
    switchToOrganization(organization);
  }
  gcy('global-paginated-list')
    .contains(projectName)
    .closestDcy('dashboard-projects-list-item')
    .findDcy('project-list-translations-button')
    .click();
  return gcy('global-base-view-content').should('be.visible');
};

export const createProject = (name: string, owner: string) => {
  switchToOrganization(owner);
  gcy('global-plus-button').click();
  gcy('project-name-field').find('input').type(name);
  gcy('global-form-save-button').click();
  assertMessage('Project created');
  gcy('organization-switch').contains(owner).should('be.visible');
  gcy('navigation-item').contains(name);
};

export const visitList = () => {
  cy.visit(`${HOST}`);
};
