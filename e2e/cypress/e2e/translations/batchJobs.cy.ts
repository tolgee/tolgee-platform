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
import {
  assertExportLanguagesSelected,
  checkZipContent,
  getFileName,
} from '../../common/export';

describe('Batch jobs', { scrollBehavior: false }, () => {
  const downloadsFolder = Cypress.config('downloadsFolder');
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
    cy.gcy('translation-text').contains('en').should('exist');
    executeBatchOperation();
    cy.gcy('translation-text').contains('en').should('not.exist');
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
    assertLanguagesSelected(['German']);
    executeBatchOperation();
  });

  it('will Machine translate', () => {
    cy.gcy('translations-row-checkbox').first().click();
    selectOperation('Machine translation');
    assertLanguagesSelected(['German']);
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
      .contains('German')
      .closestDcy('translations-table-cell')
      .findDcy('translation-text')
      .contains('en')
      .should('exist');
  });

  it('will export selected keys', () => {
    cy.gcy('translations-row-checkbox').first().click();
    selectOperation('Export translations');
    assertExportLanguagesSelected(['English', 'German']);
    cy.gcy('export-submit-button').click();

    cy.verifyDownload(getFileName('test_project', 'zip'));
    checkZipContent({
      path: downloadsFolder + '/',
      file: getFileName('test_project', 'zip'),
      filesContent: {
        'en.json': (content) => {
          expect(JSON.parse(content)).to.deep.equal({ 'a-key': 'en' });
        },
      },
    });
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

function assertLanguagesSelected(languages: string[]) {
  cy.gcy('batch-operations-section')
    .findDcy('translations-language-select-form-control')
    .click();

  cy.gcy('translations-language-select-item').should('be.visible');

  languages.forEach((language) => {
    cy.gcy('translations-language-select-item')
      .contains(language)
      .closestDcy('translations-language-select-item')
      .should('be.visible')
      .find('input')
      .should('be.checked');
  });
  dismissMenu();
}
