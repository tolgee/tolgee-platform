import { glossaryTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy } from '../../common/shared';
import { HOST } from '../../common/constants';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';

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
    const organization = data.organizations.find((org) => org.name === 'Owner');
    const glossaryId = organization.glossaries.find(
      (glossary) => glossary.name === 'Test Glossary'
    ).id;
    cy.visit(
      `${HOST}/organizations/${organization.slug}/glossaries/${glossaryId}`
    );

    gcy('glossary-translation-cell')
      .filter(':contains("A.B.C, s.r.o.")')
      .click();
    gcy('glossary-translation-edit-field')
      .find('textarea')
      .first()
      .clear()
      .type('Nový překlad');

    gcy('glossary-translation-save-button').click();

    cy.contains('Nový překlad').should('be.visible');
  });

  it('Cannot change a non-translatable translation', () => {
    login('Owner');
    const organization = data.organizations.find((org) => org.name === 'Owner');
    const glossaryId = organization.glossaries.find(
      (glossary) => glossary.name === 'Test Glossary'
    ).id;
    cy.visit(
      `${HOST}/organizations/${organization.slug}/glossaries/${glossaryId}`
    );

    gcy('glossary-translation-cell')
      .filter(':contains("Apple")')
      .first()
      .click();
    gcy('glossary-translation-edit-field').should('not.exist');
  });

  it('Clears a translation', () => {
    login('Owner');
    const organization = data.organizations.find((org) => org.name === 'Owner');
    const glossaryId = organization.glossaries.find(
      (glossary) => glossary.name === 'Test Glossary'
    ).id;
    cy.visit(
      `${HOST}/organizations/${organization.slug}/glossaries/${glossaryId}`
    );

    gcy('glossary-translation-cell').filter(':contains("zábava")').click();

    gcy('glossary-translation-edit-field')
      .should('be.visible')
      .find('textarea')
      .first()
      .clear();

    gcy('glossary-translation-save-button').click();

    cy.contains('zábava').should('not.exist');
  });

  it('Cannot change translation if just a member', () => {
    login('Member');
    const organization = data.organizations.find((org) => org.name === 'Owner');
    const glossaryId = organization.glossaries.find(
      (glossary) => glossary.name === 'Test Glossary'
    ).id;
    cy.visit(
      `${HOST}/organizations/${organization.slug}/glossaries/${glossaryId}`
    );

    gcy('glossary-translation-cell')
      .filter(':contains("A.B.C, s.r.o.")')
      .click();
    gcy('glossary-translation-edit-field').should('not.exist');
  });
});
