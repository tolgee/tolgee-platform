import { assertMessage, gcy } from './shared';
import { HOST } from './constants';
import { waitForGlobalLoading } from './loading';

export const enterProjectSettings = (projectName: string) => {
  visitList();
  selectInProjectMoreMenu(projectName, 'project-settings-button');
  waitForGlobalLoading();
};

export const selectInProjectMoreMenu = (
  projectName: string,
  itemDataCy: Parameters<typeof cy.gcy>[0]
) => {
  visitList();

  gcy('global-paginated-list')
    .contains(projectName)
    .closestDcy('dashboard-projects-list-item')
    .within(() => {
      cy.gcy('project-list-more-button').click();
    });
  cy.gcy(itemDataCy).should('be.visible').click();
};

export const enterProject = (projectName: string) => {
  visitList();
  gcy('global-paginated-list')
    .contains(projectName)
    .closestDcy('dashboard-projects-list-item')
    .findDcy('project-list-translations-button')
    .click();
  gcy('global-base-view-content').should('be.visible');
};

export const createProject = (name: string, owner: string) => {
  gcy('global-plus-button').click();
  gcy('project-owner-select').click();
  gcy('project-owner-select-item').contains(owner).click();
  gcy('project-name-field').find('input').type(name);
  gcy('global-form-save-button').click();
  assertMessage('Project created');
  gcy('global-paginated-list')
    .contains(name)
    .closestDcy('dashboard-projects-list-item')
    .within(() => {
      gcy('project-list-owner').contains(owner).should('be.visible');
    });
};

export const visitList = () => {
  cy.visit(`${HOST}`);
};
