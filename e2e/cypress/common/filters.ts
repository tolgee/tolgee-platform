import { waitForGlobalLoading } from './loading';

export const assertFilter = ({
  submenu,
  filterOption,
  toSeeAfter,
  excludeOption,
  and,
  checkAfter,
}: {
  submenu: string;
  filterOption?: string[];
  excludeOption?: string[];
  toSeeAfter: string[];
  and?: () => void;
  checkAfter?: () => void;
}) => {
  cy.gcy('translations-filter-select').click();
  cy.waitForDom();
  cy.gcy('submenu-item').contains(submenu).click();
  cy.waitForDom();
  filterOption?.forEach((option) => {
    cy.gcy('filter-item').contains(option).click();
  });
  excludeOption?.forEach((option) => {
    cy.gcy('filter-item')
      .contains(option)
      .closestDcy('filter-item')
      .findDcy('filter-item-exclude')
      .click();
  });
  and?.();

  cy.focused().type('{Esc}');
  cy.focused().type('{Esc}');
  waitForGlobalLoading();
  toSeeAfter.forEach((i) => cy.contains(i).should('be.visible'));
  cy.gcy('translations-key-name').should('have.length', toSeeAfter.length);

  checkAfter?.();
  cy.gcy('translations-filter-select-clear').click();
};
