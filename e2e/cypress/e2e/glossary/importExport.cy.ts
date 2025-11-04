import { glossaryTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy } from '../../common/shared';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';
import { E2GlossaryView } from '../../compounds/glossaries/E2GlossaryView';

describe('Glossary import and export', () => {
  let data: TestDataStandardResponse;

  beforeEach(() => {
    glossaryTestData.clean();
    glossaryTestData.generateStandard().then((res) => {
      data = res.body;
    });
  });

  afterEach(() => {
    glossaryTestData.clean();
  });

  it('imports into an empty glossary', () => {
    login('Owner');
    const view = new E2GlossaryView();
    view.findAndVisit(data, 'Owner', 'Empty Glossary');

    const importDialog = view.openImportDialogWhenGlossaryIsEmpty();
    importDialog.selectFile('glossary/import_basic.csv');
    importDialog.submit();

    gcy('glossary-term-list-item')
      .filter(':contains("Imported Term One")')
      .should('be.visible');
    gcy('glossary-term-list-item')
      .filter(':contains("Imported Term Two")')
      .should('be.visible');
  });

  it('imports into existing glossary replacing existing terms', () => {
    login('Owner');
    const view = new E2GlossaryView();
    view.findAndVisit(data, 'Owner', 'Test Glossary');

    const importDialog = view.openImportDialog();
    importDialog.selectFile('glossary/import_basic.csv');
    importDialog.chooseReplace();
    importDialog.submit();

    gcy('glossary-term-list-item')
      .filter(':contains("Imported Term One")')
      .should('be.visible');

    cy.contains('A.B.C Inc').should('not.exist');
  });

  it('imports into existing glossary while keeping previous terms', () => {
    login('Owner');
    const view = new E2GlossaryView();
    view.findAndVisit(data, 'Owner', 'Test Glossary');

    const importDialog = view.openImportDialog();
    importDialog.selectFile('glossary/import_basic.csv');
    importDialog.chooseAdd();
    importDialog.submit();

    gcy('glossary-term-list-item')
      .filter(':contains("Imported Term One")')
      .should('be.visible');

    cy.contains('A.B.C Inc').should('be.visible');
  });

  it('exports glossary to CSV', () => {
    login('Owner');
    const view = new E2GlossaryView();
    view.findAndVisit(data, 'Owner', 'Test Glossary');

    cy.intercept('GET', '/v2/organizations/**/glossaries/**/export').as(
      'exportGlossary'
    );

    view.clickExport();

    cy.wait('@exportGlossary').then(({ response }) => {
      expect(response?.statusCode).to.eq(200);
      const disp = response?.headers?.['content-disposition'] as string;
      expect(disp).to.exist;
      expect(disp).to.include('.csv');
    });
  });
});
