import { HOST } from './constants';
import { gcy } from './shared';

export const visitAdministration = () => {
  cy.visit(`${HOST}/administration/organizations`);
};

export function getUserListItem(userName = 'John User') {
  return gcy('administration-users-list-item')
    .contains(userName)
    .closestDcy('administration-users-list-item');
}
