import { waitForGlobalLoading } from './loading';

export function checkNumberOfMenuItems(count: number) {
  cy.gcy('project-menu-items')
    .findDcy('project-menu-item')
    .should('have.length', count);
}

type MenuItem = Exclude<
  DataCy.Value & `project-menu-item-${string}`,
  'project-menu-item-projects'
>;

export function checkItemsInMenu(items: MenuItem[]) {
  checkNumberOfMenuItems(items.length + 1);
  items.forEach((item) => {
    cy.gcy(item).click();
    waitForGlobalLoading();
  });
}
