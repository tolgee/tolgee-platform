import { visitTranslations } from '../../common/translations';
import { waitForGlobalLoading } from '../../common/loading';
import { batchJobs } from '../../common/apiCalls/testData/testData';
import { gcy } from '../../common/shared';
import {
  executeBatchOperation,
  executeBatchOperationWithConfirmation,
  selectAll,
  selectOperation,
} from '../../common/batchJobs';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';
import { login } from '../../common/apiCalls/common';
import { selectNamespace } from '../../common/namespace';

describe('Batch jobs', () => {
  let project: TestDataStandardResponse['projects'][number] = null;

  beforeEach(() => {
    batchJobs.clean();
    batchJobs.generateStandard().then((data) => {
      project = data.body.projects[0];
      login('admin');
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
    cy.gcy('batch-operations-section')
      .findDcy('translations-language-select-form-control')
      .click();
    cy.gcy('translations-language-select-item').contains('English').click();
    cy.get('body').click(0, 0);
    cy.gcy('translations-table-cell-translation-text')
      .contains('en')
      .should('exist');
    executeBatchOperation();
    cy.gcy('translations-table-cell-translation-text')
      .contains('en')
      .should('not.exist');
  });

  it('will copy translations', () => {
    selectAll();
    selectOperation('Copy translations');
    cy.gcy('batch-operation-copy-source-select').click();
    cy.gcy('batch-operation-copy-source-select-item')
      .contains('English')
      .click();
    cy.gcy('batch-operations-section')
      .findDcy('translations-language-select-form-control')
      .click();
    cy.gcy('translations-language-select-item').contains('German').click();
    cy.get('body').click(0, 0);
    cy.gcy('translations-table-cell-translation-text')
      .contains('en1')
      .should('have.length', 1);
    executeBatchOperation();
    cy.gcy('translations-table-cell-translation-text')
      .contains('en1')
      .should('have.length', 2);
  });

  const visit = () => {
    visitTranslations(project.id);
  };
});
