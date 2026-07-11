import 'cypress-file-upload';
import {
  assertMessage,
  confirmStandard,
  gcy,
  selectInProjectMenu,
  selectInSelect,
} from '../../common/shared';
import {
  getLanguageRow,
  getLanguageSelect,
  visitImport,
} from '../../common/import';
import {
  importNamespacesTestData,
  importTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { visitTranslations } from '../../common/translations';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';

describe('Import result & manipulation', () => {
  describe('Without namespaces', () => {
    beforeEach(() => {
      importTestData.clean();

      importTestData.generateBasic().then((importData) => {
        login('franta');
        visitImport(importData.body.project.id);
      });
    });

    it('Shows correct import result', () => {
      cy.gcy('import-result-row').should('have.length', 3);

      getLanguageRow('multilang.json (en)').within(() => {
        cy.gcy('import-result-resolve-button')
          .should('contain', '0 / 3')
          .should('not.be.disabled');
        cy.gcy('import-result-language-menu-cell').should(
          'not.contain',
          'German'
        );
        cy.gcy('import-result-language-menu-cell').should('contain', 'English');
        cy.gcy('import-result-total-count-cell').should('contain', '5');
      });

      getLanguageRow('multilang.json (de)').within(() => {
        cy.gcy('import-result-resolve-button')
          .should('contain', '0 / 0')
          .should('be.disabled');
        cy.gcy('import-result-language-menu-cell').should('contain', 'German');
        cy.gcy('import-result-language-menu-cell').should(
          'not.contain',
          'English'
        );
      });
    });

    it('Shows correct file issues number', () => {
      getLanguageRow('multilang.json (en)')
        .findDcy('import-result-file-warnings')
        .should('contain', '4');
    });

    it('Selects language', () => {
      const select = getLanguageSelect('multilang.json (fr)');
      selectInSelect(select, 'French');
      select.should('contain', 'French');
      select.should('not.contain', 'Czech');
      // wait for the changing request is finished, this could be done properly,
      // but for now waiting should be enough
      cy.wait(200);
    });

    it('Changes language', () => {
      const filename = 'multilang.json (en)';
      const select = getLanguageSelect(filename);
      selectInSelect(select, 'French');
      select.should('contain', 'French');
      select.should('not.contain', 'Czech');
      cy.reload();
      getLanguageSelect(filename).should('contain', 'French');
    });

    it('Clears existing language', () => {
      const filename = 'multilang.json (en)';
      getLanguageRow(filename).should('contain.text', 'English');
      getLanguageRow(filename)
        .findDcy('import-row-language-select-clear-button')
        .click()
        .should('not.exist');
      getLanguageSelect(filename).should('not.contain', 'English');
      cy.reload();
      getLanguageSelect(filename);
      getLanguageSelect(filename).should('not.contain.text', 'English');
      getLanguageRow(filename)
        .gcy('import-result-resolved-conflicts-cell')
        .should('contain.text', '0 / 0');
    });

    it('Adds new language', () => {
      const filename = 'multilang.json (en)';
      const select = getLanguageSelect(filename);
      selectInSelect(select, 'Add new');
      cy.gcy('languages-create-autocomplete-field').find('input').type('aze');
      cy.gcy('languages-create-autocomplete-suggested-option')
        .contains('Azerbaijani')
        .click();
      gcy('languages-add-dialog-submit').click();
      getLanguageSelect(filename).should('contain.text', 'Azerbaijani');
      selectInProjectMenu('Languages');
      cy.contains('Azerbaijani').should('be.visible');
    });

    it('Deletes language', () => {
      getLanguageRow('multilang.json (en)')
        .findDcy('import-result-delete-language-button')
        .click();
      confirmStandard();
      cy.reload();
      cy.gcy('import-result-row').should('have.length', 2);
      cy.reload();
      cy.gcy('import-result-row').should('have.length', 2);
    });

    it('Cancels import', () => {
      gcy('import_cancel_import_button').click();
      confirmStandard();
      cy.gcy('import-result-row').should('not.exist');
    });

    it("Doesn't apply when language not selected", () => {
      const filename = 'multilang.json (fr)';
      getLanguageSelect(filename).should(
        'not.contain.text',
        'Select existing language'
      );
      gcy('import_apply_import_button').click();
      getLanguageSelect(filename).should(
        'contain.text',
        'Select existing language'
      );
    });

    after(() => {
      importTestData.clean();
    });
  });

  describe('With namespaces', () => {
    let importData: TestDataStandardResponse;

    beforeEach(() => {
      importNamespacesTestData.clean();

      importNamespacesTestData.generateStandard().then((importDataResponse) => {
        login('franta');
        importData = importDataResponse.body;
        visitImport(importDataResponse.body.projects[0].id);
      });
    });

    it(
      'selects namespace',
      {
        retries: {
          runMode: 3,
        },
      },
      () => {
        selectNamespaceOnRow('multilang.json (de)', 'hello');

        cy.waitForDom();

        getLanguageRow('multilang.json (de)')
          .findDcy('import-result-resolved-conflicts-cell')
          .should('contain', '0 / 0');

        const langRow = getLanguageRow('multilang.json (de)');
        langRow.findDcy('namespaces-selector').click();
        cy.gcy('search-select-item').contains('<none>').click();

        cy.waitForDom();
        getLanguageRow('multilang.json (de)')
          .findDcy('import-result-resolve-button')
          .should('contain', '0 / 1');
      }
    );

    it('imports with selects namespaces', { retries: { runMode: 5 } }, () => {
      gcy('import_apply_import_button').click();
      assertMessage('Import successful');
      gcy('import-result-row').should('have.length', 0);
      visitTranslations(importData.projects[0].id);
      cy.contains('what a key').should('be.visible');
      gcy('translations-namespace-banner')
        .contains('homepage')
        .should('be.visible');
    });
  });

  function selectNamespaceOnRow(row: string, namespace: string) {
    const langRow = getLanguageRow(row);
    langRow.findDcy('namespaces-selector').click();
    cy.gcy('search-select-new').click();
    cy.gcy('namespaces-select-text-field').clear().type(namespace);
    cy.gcy('namespaces-select-confirm').click();
  }
});
