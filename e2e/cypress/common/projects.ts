import { gcy } from './shared';
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
  gcy('global-paginated-list').contains(projectName).click();
  gcy('global-base-view-content').should('be.visible');
};

export const visitList = () => {
  cy.visit(`${HOST}`);
};
