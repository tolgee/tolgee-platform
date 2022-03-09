import 'cypress-file-upload';
import {
  create4Translations,
  createExportableProject,
  exportSelectFormat,
  exportToggleLanguage,
  visitExport,
} from '../../common/export';

describe('Projects Basics', () => {
  beforeEach(() => {
    createExportableProject().then((p) => {
      create4Translations(p.id);
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
    cy.verifyDownload('en.json');
  });

  it('exports one language to xliff', () => {
    exportToggleLanguage('Česky');

    exportSelectFormat('XLIFF');

    cy.gcy('export-submit-button').click();
    cy.verifyDownload('en.xliff');
  });
});
