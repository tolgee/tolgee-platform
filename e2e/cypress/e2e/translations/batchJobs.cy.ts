import { getCell, visitTranslations } from '../../common/translations';
import { waitForGlobalLoading } from '../../common/loading';
import { batchJobs } from '../../common/apiCalls/testData/testData';
import { dismissMenu, gcy } from '../../common/shared';
import {
  executeBatchOperation,
  executeBatchOperationWithConfirmation,
  selectAll,
  selectOperation,
} from '../../common/batchOperations';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';
import { login } from '../../common/apiCalls/common';
import { selectNamespace } from '../../common/namespace';
import { assertHasState } from '../../common/state';

describe('Batch jobs', { scrollBehavior: false }, () => {
  let project: TestDataStandardResponse['projects'][number] = null;

  beforeEach(() => {
    batchJobs.clean();
    batchJobs.generateStandard().then((data) => {
      project = data.body.projects[0];
      login('test_username');
      visit();

      // wait for loading to appear and disappear again
      cy.gcy('global-base-view-content').should('be.visible');
      waitForGlobalLoading();
    });
  });

  it('will delete all properly', () => {
    selectAll();
    selectOperation('Delete');
    executeBatchOperationWithConfirmation();
    gcy('global-empty-list').should('be.visible');
  });

  it('will delete all except first one', () => {
    selectAll();
    gcy('translations-row-checkbox').first().click();
    selectOperation('Delete');
    executeBatchOperationWithConfirmation();
    gcy('translations-key-count').contains('1').should('be.visible');
  });

  it('will change namespace', () => {
    selectAll();
    selectOperation('Change namespace');
    selectNamespace('new-namespace');
    executeBatchOperation();
    cy.gcy('namespaces-banner-content')
      .contains('new-namespace')
      .should('be.visible');
  });

  it('will add and remove tags', () => {
    selectAll();
    selectOperation('Add tags');
    cy.gcy('tag-autocomplete-input').type('new-tag');
    cy.gcy('tag-autocomplete-option').contains('Add').click();
    executeBatchOperation();
    cy.gcy('translations-tag').contains('new-tag').should('be.visible');

    selectAll();
    selectOperation('Remove tags');
    cy.gcy('tag-autocomplete-input').type('new-tag');
    cy.gcy('tag-autocomplete-option').contains('new-tag').click();
    executeBatchOperation();
    cy.gcy('translations-tag').should('not.exist');
  });

  it('will clear translations', () => {
    selectAll();
    selectOperation('Clear translations');
    selectLanguage('English');
    cy.gcy('translations-table-cell-translation-text')
      .contains('en')
      .should('exist');
    executeBatchOperation();
    cy.gcy('translations-table-cell-translation-text')
      .contains('en')
      .should('not.exist');
  });

  it('will change state to reviewed and back to translated', () => {
    selectAll();
    selectOperation('Mark as reviewed');
    selectLanguage('English');
    executeBatchOperation();
    assertHasState('en', 'Reviewed');

    selectAll();
    selectOperation('Mark as translated');
    selectLanguage('English');
    executeBatchOperation();
    assertHasState('en', 'Translated');
  });

  it('will pre-translate with TM', () => {
    cy.gcy('translations-row-checkbox').first().click();
    selectOperation('Pre-translate by TM');
    selectLanguage();
    executeBatchOperation();
  });

  it('will Machine translate', () => {
    cy.gcy('translations-row-checkbox').first().click();
    selectOperation('Machine translation');
    selectLanguage();
    executeBatchOperation();
    getCell('en translated with GOOGLE from en to de').should('be.visible');
    cy.gcy('translations-auto-translated-indicator').should('exist');
  });

  it('will copy translations', () => {
    selectAll();
    selectOperation('Copy translations');
    cy.gcy('batch-operation-copy-source-select').click();
    cy.gcy('batch-operation-copy-source-select-item')
      .contains('English')
      .click();
    selectLanguage();
    executeBatchOperation();
    cy.gcy('translations-row')
      .eq(1)
      .findDcy('translations-table-cell-language')
      .contains('de')
      .closestDcy('translations-table-cell')
      .findDcy('translations-table-cell-translation-text')
      .contains('en')
      .should('exist');
  });

  const visit = () => {
    visitTranslations(project.id);
  };
});

function selectLanguage(language = 'German') {
  cy.gcy('batch-operations-section')
    .findDcy('translations-language-select-form-control')
    .click();
  cy.gcy('translations-language-select-item').contains(language).click();
  dismissMenu();
  cy.gcy('translations-language-select-item').should('not.exist');
}
