import 'cypress-file-upload';
import {
  gcy,
  selectInProjectMenu,
  toggleInMultiselect,
} from '../../common/shared';
import { visitImport } from '../../common/import';
import { importTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';

describe('Import application', () => {
  beforeEach(() => {
    importTestData.clean();

    importTestData.generateApplicable().then((importData) => {
      login('franta');
      visitImport(importData.body.project.id);
    });
  });

  it(
    'Applies import',
    {
      retries: {
        runMode: 10,
      },
    },
    () => {
      gcy('import_apply_import_button').click();
      cy.gcy('import-result-row').should('not.exist');
      selectInProjectMenu('Translations');
      toggleInMultiselect(gcy('translations-language-select-form-control'), [
        'French',
        'English',
      ]);
      cy.contains('What a text').should('exist');
      cy.contains('What a french text').should('exist');
    }
  );
});
