import { HOST } from './constants';

export const visitAdministration = () => {
  cy.visit(`${HOST}/administration/organizations`);
};

export function getUserListItem(userName = 'John User') {
  return cy.contains(userName).closestDcy('administration-users-list-item');
}
