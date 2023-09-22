import { Scopes } from '../../../webapp/src/fixtures/permissions';
import { assertMessage, confirmHardMode, dismissMenu } from './shared';

type Options = {
  confirm?: boolean;
  languages?: string[];
};

export const permissionsMenuSelectRole = (role: string, options?: Options) => {
  cy.gcy('permissions-menu-basic').click();
  cy.gcy('permissions-menu').contains(role).click();

  if (options?.languages) {
    cy.gcy('permissions-language-menu-button').click();

    options.languages?.forEach((lang) => {
      cy.gcy('search-select-item').contains(lang).click();
    });

    dismissMenu();
  }

  cy.gcy('permissions-menu-save').click();
  if (options?.confirm) {
    confirmHardMode();
  }
  assertMessage('Permissions set');
};

export const permissionsMenuSelectAdvanced = (
  scopes: Scopes,
  options?: Options
) => {
  cy.gcy('permissions-menu-granular').click();
  // uncheck all
  for (const _ in [0, 1]) {
    Cypress.$('permissions-advanced-checkbox')
      .find('input:checked:enabled')
      .each(($el) => ($el as any).trigger('click'));
  }

  // check scopes
  scopes.forEach((scope) => {
    cy.get('[permissions-scope="' + scope + '"]')
      .find('input')
      .focus()
      .check();
  });

  cy.gcy('permissions-menu-save').click();
  if (options?.confirm) {
    confirmHardMode();
  }
  assertMessage('Permissions set');
};
