import { satisfiesLanguageAccess } from '../../../../webapp/src/fixtures/permissions';
import { deleteSelected } from '../batchOperations';
import { waitForGlobalLoading } from '../loading';
import { confirmStandard, dismissMenu } from '../shared';
import { getCell } from '../state';
import { createTag } from '../tags';
import { createTranslation, editCell } from '../translations';
import { getLanguageId, getLanguages, ProjectInfo } from './shared';

export function testKeys(info: ProjectInfo) {
  const { project } = info;
  const scopes = project.computedPermission.scopes;
  cy.gcy('translations-table-cell').contains('key-1').should('be.visible');

  // test if user can select only from viewable languages
  cy.gcy('translations-language-select-form-control').click();
  const visibleLanguages = getLanguages().filter(([tag, name]) => {
    return satisfiesLanguageAccess(
      project.computedPermission,
      'translations.view',
      getLanguageId(info.languages, tag)
    );
  });

  cy.gcy('translations-language-select-item').should(
    'have.length',
    visibleLanguages.length
  );

  cy.focused().type('{esc}', { force: true });

  if (scopes.includes('keys.edit')) {
    editCell('key-1', 'new-key');
    createTag('Test tag');
  }

  if (scopes.includes('screenshots.view')) {
    cy.gcy('translations-table-cell').first().focus();
    cy.gcy('translations-cell-screenshots-button')
      .first()
      .should('exist')
      .click();
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
    cy.waitForDom();
    dismissMenu();
  }

  if (
    !scopes.includes('translations.edit') &&
    scopes.includes('translations.view')
  ) {
    getCell('German text 1').click();
    cy.gcy('global-editor').should('not.exist');
  }

  if (scopes.includes('keys.create')) {
    createTranslation({ key: 'new_test_key' });
  }

  if (scopes.includes('keys.delete')) {
    cy.gcy('translations-row-checkbox').first().click();
    deleteSelected();
  }
}
