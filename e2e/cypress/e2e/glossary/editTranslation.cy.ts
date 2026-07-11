import { glossaryTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy } from '../../common/shared';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';
import { E2GlossaryView } from '../../compounds/glossaries/E2GlossaryView';

describe('Glossary term translation editing', () => {
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

  it('Changes a translation', () => {
    login('Owner');
    const view = new E2GlossaryView();
    view.findAndVisit(data, 'Owner', 'Test Glossary');

    view.setTranslation('A.B.C, s.r.o.', 'Nový překlad');
    view.checkTranslationExists('Nový překlad');
  });

  it('Cannot change a non-translatable translation', () => {
    login('Owner');
    const view = new E2GlossaryView();
    view.findAndVisit(data, 'Owner', 'Test Glossary');

    gcy('glossary-translation-cell')
      .filter(':contains("Apple")')
      .first()
      .click();
    gcy('glossary-translation-edit-field').should('not.exist');
  });

  it('Clears a translation', () => {
    login('Owner');
    const view = new E2GlossaryView();
    view.findAndVisit(data, 'Owner', 'Test Glossary');

    view.setTranslation('zábava', undefined);
    cy.contains('zábava').should('not.exist');
  });

  it('Cannot change translation if just a member', () => {
    login('Member');
    const view = new E2GlossaryView();
    view.findAndVisit(data, 'Owner', 'Test Glossary');

    gcy('glossary-translation-cell')
      .filter(':contains("A.B.C, s.r.o.")')
      .click();
    gcy('glossary-translation-edit-field').should('not.exist');
  });
});
