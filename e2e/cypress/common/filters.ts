import { waitForGlobalLoading } from './loading';
import { dismissMenu } from './shared';

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

/**
 * The task filter nests three menus deep, one more than assertFilter's fixed two escapes,
 * so the dismissal is spelled out here.
 */
export const assertTaskFilter = (and: () => void, toSeeAfter: string[]) => {
  cy.gcy('translations-filter-select').click();
  cy.waitForDom();
  cy.gcy('submenu-item').contains('Tasks').click();
  cy.waitForDom();
  and();
  dismissMenu();
  dismissMenu();
  dismissMenu();
  waitForGlobalLoading();
  toSeeAfter.forEach((i) => cy.contains(i).should('be.visible'));
  cy.gcy('translations-key-name').should('have.length', toSeeAfter.length);
};
