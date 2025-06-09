import { glossaryTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy } from '../../common/shared';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';
import { E2GlossariesView } from '../../compounds/glossaries/E2GlossariesView';

describe('Glossary deletion', () => {
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

  it('Deletes a glossary', () => {
    login('Owner');
    const view = new E2GlossariesView();
    view.findAndVisit(data, 'Owner');

    view.deleteGlossary('Test Glossary');

    cy.contains('Test Glossary').should('not.exist');
  });

  it('Cannot delete glossary without proper permissions', () => {
    login('Member');
    const view = new E2GlossariesView();
    view.findAndVisit(data, 'Owner');

    view.openGlossaryMenu('Test Glossary');
    gcy('glossary-delete-button').should('not.exist');
  });
});
