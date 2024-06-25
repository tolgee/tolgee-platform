import 'cypress-file-upload';
import { createKey, deleteProject } from '../../../common/apiCalls/common';
import {
  createExportableProject,
  exportSelectFormat,
  exportToggleLanguage,
  getFileName,
  visitExport,
} from '../../../common/export';

describe('Export Basics', () => {
  const downloadsFolder = Cypress.config('downloadsFolder');

  let projectId: number;

  beforeEach(() => {
    createExportableProject().then((p) => {
      createKey(p.id, `test.test`, {
        en: `Test english`,
        cs: `Test czech`,
      });
      createKey(p.id, `test.array[0]`, {
        en: `Test english`,
        cs: `Test czech`,
      });
      visitExport(p.id);
      projectId = p.id;
      cy.gcy('export-submit-button').should('be.visible');
    });
  });

  it('exports all to zip by default', () => {
    cy.gcy('export-submit-button').click();
    cy.verifyDownload(getFileName('Test project', 'zip'));
  });

  it('exports one language to json', () => {
    exportToggleLanguage('Česky');

    cy.gcy('export-submit-button').click();

    cy.readFile(
      downloadsFolder + '/' + getFileName('Test project', 'json', 'en')
    ).should('deep.equal', {
      'test.array[0]': 'Test english',
      'test.test': 'Test english',
    });
  });

  it('exports with nested structure', () => {
    exportToggleLanguage('English');
    exportSelectFormat('Structured JSON');

    cy.gcy('export-submit-button').click();
    const fileName = getFileName('Test project', 'json', 'cs');
    cy.verifyDownload(fileName);

    const getFile = () => cy.readFile(downloadsFolder + '/' + fileName);
    getFile().its('test').its('test').should('eq', 'Test czech');
    getFile().its('test').its('array[0]').should('eq', 'Test czech');
  });

  it('the support arrays switch works', { retries: { runMode: 5 } }, () => {
    exportToggleLanguage('English');
    exportSelectFormat('Structured JSON');

    cy.gcy('export-support_arrays-selector').click();
    cy.waitForDom();
    cy.gcy('export-submit-button').click();

    const fileName = getFileName('Test project', 'json', 'cs');
    cy.verifyDownload(fileName);

    cy.readFile(downloadsFolder + '/' + fileName)
      .its('test')
      .its('array')
      .its(0)
      .should('eq', 'Test czech');
  });

  it('exports one language to xliff', () => {
    exportToggleLanguage('Česky');

    exportSelectFormat('XLIFF');

    cy.gcy('export-submit-button').click();
    cy.verifyDownload(getFileName('Test project', 'xliff', 'en'));
  });

  afterEach(() => {
    deleteProject(projectId);
  });
});
