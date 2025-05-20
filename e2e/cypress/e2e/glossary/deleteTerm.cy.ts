import { glossaryTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy } from '../../common/shared';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';
import { E2GlossaryView } from '../../compounds/glossaries/E2GlossaryView';

describe('Glossary term deletion', () => {
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

  it('Deletes a glossary term', () => {
    login('Owner');
    const view = new E2GlossaryView();
    view.findAndVisit(data, 'Owner', 'Test Glossary');

    view.toggleTermChecked('Apple');
    view.deleteCheckedTerms();

    cy.contains('Apple').should('not.exist');
  });

  it('Cannot delete glossary term without proper permissions', () => {
    login('Member');
    const view = new E2GlossaryView();
    view.findAndVisit(data, 'Owner', 'Test Glossary');

    view.toggleTermChecked('Apple');

    gcy('glossary-batch-delete-button').should('be.visible');
    gcy('glossary-batch-delete-button').should('be.disabled');
  });
});
