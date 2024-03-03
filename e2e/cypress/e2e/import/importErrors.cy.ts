import 'cypress-file-upload';
import { assertMessage, gcy } from '../../common/shared';
import { visitImport } from '../../common/import';
import { importTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';

describe('Import errors', () => {
  beforeEach(() => {
    importTestData.clean();
  });

  describe('All selected', () => {
    beforeEach(() => {
      importTestData.generateAllSelected().then((importData) => {
        login('franta');
        visitImport(importData.body.project.id);
      });
    });

    it('Does not apply when row not resolved', () => {
      gcy('import_apply_import_button').should('be.visible');
      cy.wait(100);
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

  it('does not add too many languages', { retries: { runMode: 3 } }, () => {
    importTestData.generateWithManyLanguages().then((importData) => {
      login('franta');
      visitImport(importData.body.project.id);
    });

    const files = [];
    for (let i = 1; i <= 20; i++) {
      files.push('import/simple.json');
    }

    gcy('import-file-input').attachFile(files);
    assertMessage('Cannot add more than 100 languages');
  });

  describe('file error message', () => {
    beforeEach(() => {
      importTestData.generateBase().then((res) => {
        login('franta');
        visitImport(res.body.id);
      });
      cy.get('[data-cy=dropzone]').attachFile(
        {
          filePath: 'import/error.json.zip',
          fileName: 'error.zip',
        },
        { subjectType: 'drag-n-drop', force: true }
      );
    });

    it('shows error for bad file', () => {
      gcy('import-file-error')
        .contains('Cannot parse file')
        .should('be.visible');
    });

    it('error shows more and less', { retries: { runMode: 3 } }, () => {
      gcy('import-progress-overlay').should('be.visible');
      gcy('import-file-error')
        .findDcy('import-file-error-more-less-button')
        .click();
      gcy('import-file-error')
        .contains("Unrecognized token 'asdlasj")
        .should('be.visible');
      gcy('import-file-error')
        .findDcy('import-file-error-more-less-button')
        .click();
      gcy('import-file-error')
        .contains("Unrecognized token 'asdlasj")
        .should('not.exist');
    });

    it('collapses error', { retries: { runMode: 3 } }, () => {
      gcy('import-file-error')
        .findDcy('import-file-error-collapse-button')
        .click()
        .should('not.be.visible');
    });
  });

  after(() => {
    importTestData.clean();
  });
});
