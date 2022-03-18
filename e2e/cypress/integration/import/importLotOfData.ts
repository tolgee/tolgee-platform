import 'cypress-file-upload';
import { contextGoToPage, gcy } from '../../common/shared';
import {
  assertInResolutionDialog,
  assertInResultDialog,
  getFileIssuesDialog,
  getLanguageRow,
  getResolutionDialog,
  getShowDataDialog,
  visitImport,
} from '../../common/import';
import { importTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';

describe('Import with lot of data', () => {
  beforeEach(() => {
    importTestData.clean();
    importTestData.generateLotOfData().then((importData) => {
      login('franta');
      visitImport(importData.body.project.id);
    });
  });

  it('Shows correct file issues', () => {
    getLanguageRow('multilang.json (en)')
      .findDcy('import-file-issues-button')
      .click();
    getFileIssuesDialog().contains('File issues').should('be.visible');
    getFileIssuesDialog()
      .contains('Key is empty (key index: 1)')
      .should('be.visible');
    getFileIssuesDialog()
      .contains('Key is not string (key name: 4, key index: 2)')
      .should('be.visible');
    getFileIssuesDialog()
      .contains('Value is empty (key name: value_is_emtpy_key)')
      .should('be.visible');
    getFileIssuesDialog()
      .contains(
        'Value is not string (key name: value_is_not_string_key, key index: 5, value: 1)'
      )
      .should('be.visible');
    getFileIssuesDialog()
      .contains('Key is empty (key index: 12)')
      .scrollIntoView()
      .should('be.visible');
    contextGoToPage(getFileIssuesDialog(), 2);
    getFileIssuesDialog()
      .contains('Key is empty (key index: 32)')
      .scrollIntoView()
      .should('be.visible');
  });

  it(
    'Shows correct result',
    {
      retries: {
        runMode: 2,
      },
    },
    () => {
      getLanguageRow('another.json (fr)')
        .findDcy('import-result-show-all-translations-button')
        .click();
      getShowDataDialog().should('be.visible');
      assertInResultDialog('this_is_key_1');
      assertInResultDialog('I am import translation 1');
      assertInResultDialog('this_is_key_143');
      contextGoToPage(getShowDataDialog(), 6);
      assertInResultDialog('this_is_key_99');
    }
  );

  it('Searches', () => {
    getLanguageRow('another.json (fr)')
      .findDcy('import-result-show-all-translations-button')
      .click();
    gcy('global-search-field')
      .filter(':visible')
      .find('input')
      .type('this_is_key_145');
    assertInResultDialog('this_is_key_145');
    assertInResultDialog('I am import translation 145');
  });

  it('Paginates in resolution dialog', () => {
    getLanguageRow('another.json (fr)')
      .findDcy('import-result-resolve-button')
      .click();
    getResolutionDialog().contains('Resolve conflicts').should('be.visible');
    assertInResolutionDialog('this_is_key_1');
    assertInResolutionDialog('I am translation 1');
    assertInResolutionDialog('I am import translation 1');
    contextGoToPage(getResolutionDialog(), 6);
    assertInResolutionDialog('I am import translation 99');
  });

  after(() => {
    importTestData.clean();
  });
});
