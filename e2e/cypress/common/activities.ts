import { gcyAdvanced, selectInProjectMenu } from './shared';

/**
 * Expands the activity group of the given type on the project dashboard.
 */
export function expandActivityGroup(groupType: string) {
  gcyAdvanced({ value: 'activity-group-item', type: groupType })
    .should('be.visible')
    .findDcy('activity-group-expand-button')
    .click();

  return cy;
}

/**
 * Opens the project dashboard and expands the activity group of the given type.
 */
export function checkActivityGroup(groupType: string) {
  selectInProjectMenu('Project Dashboard');
  cy.waitForDom();

  return expandActivityGroup(groupType);
}

export function assertActivityGroupDetails(
  groupType: string,
  expectedTexts: string[]
) {
  gcyAdvanced({ value: 'activity-group-item', type: groupType }).within(() => {
    expectedTexts.forEach((text) => {
      cy.contains(text).should('be.visible');
    });
  });
  return cy;
}
