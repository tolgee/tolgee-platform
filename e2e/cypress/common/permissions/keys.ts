import { createKey } from '../apiCalls/common';
import { waitForGlobalLoading } from '../loading';
import { assertMessage, confirmStandard } from '../shared';
import { createTag } from '../tags';
import { editCell } from '../translations';
import { ProjectInfo } from './shared';

export function testKeys(info: ProjectInfo) {
  const { project } = info;
  const scopes = project.computedPermission.scopes;
  cy.gcy('translations-table-cell').contains('key-1').should('be.visible');

  if (scopes.includes('keys.edit')) {
    editCell('key-1', 'new-key');
    createTag('Test tag');
  }

  if (scopes.includes('screenshots.view')) {
    cy.gcy('translations-table-cell').first().focus();
    cy.gcy('translations-cell-screenshots-button').should('exist').click();
    cy.gcy('screenshot-thumbnail').should('be.visible');
    if (scopes.includes('screenshots.delete')) {
      cy.gcy('screenshot-thumbnail').trigger('mouseover');
      cy.gcy('screenshot-thumbnail-delete').click();
      confirmStandard();
      waitForGlobalLoading();
      cy.gcy('screenshot-thumbnail').should('not.exist');
    }
    if (scopes.includes('screenshots.upload')) {
      cy.gcy('add-box').should('be.visible');
    }
    // close popup
    cy.get('body').click(0, 0);
  }

  if (
    !scopes.includes('translations.edit') &&
    scopes.includes('translations.view')
  ) {
    cy.gcy('translations-table-cell-translation').first().click();
    cy.gcy('global-editor').should('not.exist');
  }

  if (scopes.includes('keys.create')) {
    createKey(info.project.id, 'new_test_key', { en: 'Test translation' });
  }

  if (scopes.includes('keys.delete')) {
    cy.gcy('translations-row-checkbox').first().click();
    cy.gcy('translations-delete-button').click();
    confirmStandard();
    assertMessage('Translations deleted!');
  }
}
