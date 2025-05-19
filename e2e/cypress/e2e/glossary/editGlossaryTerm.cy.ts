import { glossaryTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy } from '../../common/shared';
import { HOST } from '../../common/constants';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';

describe('Glossary term editing', () => {
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

  it('Edits a glossary term name', () => {
    login('Owner');
    const organizationSlug = data.organizations.find(
      (org) => org.name === 'Owner'
    ).slug;
    cy.visit(`${HOST}/organizations/${organizationSlug}/glossaries`);

    gcy('glossary-list-item').filter(':contains("Test Glossary")').click();

    cy.contains('Term').should('be.visible');
    gcy('glossary-term-list-item').filter(':contains("Term")').click();
    gcy('create-glossary-term-dialog').should('be.visible');

    gcy('create-glossary-term-field-text')
      .click()
      .focused()
      .clear()
      .type('Edited Test Term');

    gcy('create-glossary-term-submit').click();
    gcy('create-glossary-term-dialog').should('not.exist');

    cy.contains('Edited Test Term').should('be.visible');
  });

  it('Edits a glossary term description', () => {
    login('Owner');
    const organizationSlug = data.organizations.find(
      (org) => org.name === 'Owner'
    ).slug;
    cy.visit(`${HOST}/organizations/${organizationSlug}/glossaries`);

    gcy('glossary-list-item').filter(':contains("Test Glossary")').click();

    cy.contains('Term').should('be.visible');
    gcy('glossary-term-list-item').filter(':contains("Term")').click();
    gcy('create-glossary-term-dialog').should('be.visible');

    gcy('create-glossary-term-field-description')
      .click()
      .focused()
      .clear()
      .type('This is an edited description for the test term');

    gcy('create-glossary-term-submit').click();
    gcy('create-glossary-term-dialog').should('not.exist');

    cy.contains('This is an edited description for the test term').should(
      'be.visible'
    );
  });

  it('Edits glossary term flags', () => {
    login('Owner');
    const organizationSlug = data.organizations.find(
      (org) => org.name === 'Owner'
    ).slug;
    cy.visit(`${HOST}/organizations/${organizationSlug}/glossaries`);

    gcy('glossary-list-item').filter(':contains("Test Glossary")').click();

    cy.contains('Term').should('be.visible');
    gcy('glossary-term-list-item').filter(':contains("Term")').click();
    gcy('create-glossary-term-dialog').should('be.visible');

    gcy('create-glossary-term-flag-case-sensitive').click();
    gcy('create-glossary-term-flag-abbreviation').click();
    gcy('create-glossary-term-flag-forbidden').click();

    gcy('create-glossary-term-submit').click();
    gcy('create-glossary-term-dialog').should('not.exist');

    gcy('glossary-term-list-item').filter(':contains("Term")').click();
    gcy('create-glossary-term-dialog').should('be.visible');

    gcy('create-glossary-term-flag-case-sensitive')
      .find('input')
      .should('be.checked');
    gcy('create-glossary-term-flag-abbreviation')
      .find('input')
      .should('be.checked');
    gcy('create-glossary-term-flag-forbidden')
      .find('input')
      .should('be.checked');
  });
});
