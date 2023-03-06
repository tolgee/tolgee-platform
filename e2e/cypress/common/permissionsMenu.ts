import { Scopes } from '../../../webapp/src/fixtures/permissions';
import { assertMessage, confirmHardMode } from './shared';

export const permissionsMenuSelectRole = (role: string) => {
  cy.gcy('permissions-menu-basic').click();
  cy.gcy('permissions-menu').contains(role).click();
  cy.gcy('permissions-menu-save').click();
  confirmHardMode();
  assertMessage('Permissions set');
};

export const permissionsMenuSelectAdvanced = (scopes: Scopes) => {
  cy.gcy('permissions-menu-granular').click();
  // uncheck all
  for (const _ in [0, 1]) {
    cy.gcy('permissions-advanced-checkbox')
      .find('input:checked:enabled')
      .each(($el) => $el.trigger('click'));
  }

  // check scopes
  scopes.forEach((scope) => {
    cy.get('[permissions-scope="' + scope + '"]')
      .find('input')
      .check();
  });

  cy.gcy('permissions-menu-save').click();
  confirmHardMode();
  assertMessage('Permissions set');
};
