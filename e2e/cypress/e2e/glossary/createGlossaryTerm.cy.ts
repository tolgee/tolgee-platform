import { glossaryTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy } from '../../common/shared';
import { HOST } from '../../common/constants';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';

describe('Glossary term creation', () => {
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

  it('Creates a new glossary term', () => {
    login('Owner');
    const organizationSlug = data.organizations.find(
      (org) => org.name === 'Owner'
    ).slug;
    cy.visit(`${HOST}/organizations/${organizationSlug}/glossaries`);

    gcy('glossary-list-item').filter(':contains("Test Glossary")').click();

    gcy('global-plus-button').click();
    gcy('create-glossary-term-dialog').should('be.visible');

    gcy('create-glossary-term-field-text').type('New Test Term');
    gcy('create-glossary-term-field-description').type(
      'This is a test term description'
    );
    gcy('create-glossary-term-flag-case-sensitive').click();
    gcy('create-glossary-term-flag-abbreviation').click();

    gcy('create-glossary-term-submit').click();
    gcy('create-glossary-term-dialog').should('not.exist');

    cy.contains('New Test Term').should('be.visible');
  });

  it('Creates a non-translatable glossary term', () => {
    login('Owner');
    const organizationSlug = data.organizations.find(
      (org) => org.name === 'Owner'
    ).slug;
    cy.visit(`${HOST}/organizations/${organizationSlug}/glossaries`);

    gcy('glossary-list-item').filter(':contains("Test Glossary")').click();
    gcy('global-plus-button').click();

    gcy('create-glossary-term-field-text').type('Non-Translatable Term');
    gcy('create-glossary-term-field-description').type(
      'This term should not be translated'
    );
    gcy('create-glossary-term-flag-non-translatable').click();

    gcy('create-glossary-term-submit').click();
    cy.contains('Non-Translatable Term').should('be.visible');
  });
});
