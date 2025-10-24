import { HOST } from './constants';
import { gcy } from './shared';

export const visitAdministrationOrganizations = () => {
  cy.visit(`${HOST}/administration/organizations`);
};

export const visitAdministrationUsers = () => {
  cy.visit(`${HOST}/administration/users`);
};

export function getUserListItem(userName = 'John User') {
  return gcy('administration-users-list-item')
    .contains(userName)
    .closestDcy('administration-users-list-item');
}
