import { HOST } from './constants';

export const visitAdministration = () => {
  cy.visit(`${HOST}/administration/organizations`);
};

export function getUserListItem(userName = 'John User') {
  return cy.contains(userName).closestDcy('administration-users-list-item');
}

export function debugUserAccount(userName?: string) {
  visitAdministration();
  cy.gcy('settings-menu-item').contains('Users').click();
  getUserListItem(userName)
    .findDcy('administration-user-debug-account')
    .click();
}
