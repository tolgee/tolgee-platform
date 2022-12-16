import 'cypress-file-upload';
import { createKey } from '../../common/apiCalls/common';
import {
  createExportableProject,
  exportSelectFormat,
  exportToggleLanguage,
  visitExport,
} from '../../common/export';

describe('Projects Basics', () => {
  const downloadsFolder = Cypress.config('downloadsFolder');

  beforeEach(() => {
    createExportableProject().then((p) => {
      createKey(p.id, `test.test`, {
        en: `Test english`,
        cs: `Test czech`,
      });
      visitExport(p.id);
      cy.gcy('export-submit-button').should('be.visible');
    });
  });

  it('exports all to zip by default', () => {
    cy.gcy('export-submit-button').click();
    cy.verifyDownload('Test project.zip');
  });

  it('exports one language to json', () => {
    exportToggleLanguage('Česky');

    cy.gcy('export-submit-button').click();

    cy.readFile(downloadsFolder + '/en.json').should('deep.equal', {
      'test.test': 'Test english',
    });
  });

  it('exports with nested structure', () => {
    exportToggleLanguage('English');

    cy.gcy('export-nested-selector').click();
    cy.gcy('export-submit-button').click();
    cy.verifyDownload('cs.json');

    cy.readFile(downloadsFolder + '/cs.json')
      .its('test')
      .its('test')
      .should('eq', 'Test czech');
  });

  it('exports one language to xliff', () => {
    exportToggleLanguage('Česky');

    exportSelectFormat('XLIFF');

    cy.gcy('export-submit-button').click();
    cy.verifyDownload('en.xliff');
  });
});
