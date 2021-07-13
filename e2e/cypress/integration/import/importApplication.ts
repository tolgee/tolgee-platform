import {
  cleanImportData,
  generateApplicableImportData,
  login,
} from '../../common/apiCalls';
import 'cypress-file-upload';
import {
  gcy,
  selectInProjectMenu,
  toggleInMultiselect,
} from '../../common/shared';
import { visitImport } from '../../common/import';

describe('Import application', () => {
  beforeEach(() => {
    cleanImportData();

    generateApplicableImportData().then((importData) => {
      login('franta');
      visitImport(importData.body.project.id);
    });
  });

  it('Applies import', () => {
    gcy('import_apply_import_button').click();
    cy.gcy('import-result-row').should('not.exist');
    selectInProjectMenu('Translations');
    toggleInMultiselect(gcy('translations-language-select-form-control'), [
      'French',
      'English',
    ]);
    cy.gcy('translations-cell-data')
      .contains('What a text')
      .should('be.visible');
    cy.gcy('translations-cell-data')
      .contains('What a french text')
      .should('be.visible');
  });
});
