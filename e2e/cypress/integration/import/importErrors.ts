import {
  cleanImportData,
  generateAllSelectedImportData,
  generateBaseImportData,
  generateManyLanguagesImportData,
  login,
} from '../../common/apiCalls';
import 'cypress-file-upload';
import { assertMessage, gcy } from '../../common/shared';
import { visitImport } from '../../common/import';

describe('Import errors', () => {
  beforeEach(() => {
    cleanImportData();
  });

  describe('All selected', () => {
    beforeEach(() => {
      generateAllSelectedImportData().then((importData) => {
        login('franta');
        visitImport(importData.body.project.id);
      });
    });

    it('Does not apply when row not resolved', () => {
      gcy('import_apply_import_button').click();
      gcy('import-conflicts-not-resolved-dialog').should(
        'contain',
        'Conflicts not resolved'
      );
      gcy('import-conflicts-not-resolved-dialog-resolve-button').click();
      gcy('import-conflict-resolution-dialog')
        .should('be.visible')
        .should('contain.text', 'Resolve conflicts');
    });
  });

  it('does not add too many languages', () => {
    generateManyLanguagesImportData().then((importData) => {
      login('franta');
      visitImport(importData.body.project.id);
    });

    const files = [];
    for (let i = 1; i <= 20; i++) {
      files.push('import/simple.json');
    }

    cy.get('[data-cy=dropzone]').attachFile(files, {
      subjectType: 'drag-n-drop',
    });
    assertMessage('Cannot add more then 100 languages');
  });

  describe('file error message', () => {
    beforeEach(() => {
      generateBaseImportData().then((res) => {
        login('franta');
        visitImport(res.body.id);
      });
      cy.get('[data-cy=dropzone]').attachFile(
        { filePath: 'import/error.jsn', fileName: 'error.json' },
        { subjectType: 'drag-n-drop' }
      );
    });

    it('shows error for bad file', () => {
      gcy('import-file-error')
        .contains('Cannot parse file')
        .should('be.visible');
    });

    it.only('error shows more and less', () => {
      cy.gcy('global-base-view-loading').should('be.visible');
      cy.gcy('global-base-view-loading').should('not.exist');
      gcy('import-file-error')
        .findDcy('import-file-error-more-less-button')
        .click();
      gcy('import-file-error')
        .contains('Cannot construct instance of')
        .should('be.visible');
      gcy('import-file-error')
        .findDcy('import-file-error-more-less-button')
        .click();
      gcy('import-file-error')
        .contains('Cannot construct instance of')
        .should('not.exist');
    });

    it('collapses error', () => {
      gcy('import-file-error')
        .findDcy('import-file-error-collapse-button')
        .click()
        .should('not.be.visible');
    });
  });

  after(() => {
    cleanImportData();
  });
});
