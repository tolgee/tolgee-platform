import { glossaryTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy, gcyChain } from '../../common/shared';
import { HOST } from '../../common/constants';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';

describe('Glossary', () => {
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

  it('Creates a new glossary', () => {
    login('Owner');
    const organizationSlug = data.organizations.find(
      (org) => org.name === 'Owner'
    ).slug;
    cy.visit(`${HOST}/organizations/${organizationSlug}/glossaries`);

    gcy('global-plus-button').click();

    gcy('create-edit-glossary-dialog').should('be.visible');

    gcy('create-glossary-field-name').type('Create Test Glossary');
    gcy('glossary-base-language-select').click();
    gcy('glossary-base-language-select-item').contains('English').click();
    gcy('assigned-projects-select').click();
    gcy('assigned-projects-select-item').contains('TheProject').click();
    cy.get('body').click(0, 0); // Close the dropdown

    gcy('create-glossary-submit').click();
    gcy('create-edit-glossary-dialog').should('not.exist');

    cy.contains('Create Test Glossary').should('be.visible');
  });

  it('Creates a new glossary from empty state', () => {
    login('Bystander');
    const organizationSlug = data.organizations.find(
      (org) => org.name === 'Bystander'
    ).slug;
    cy.visit(`${HOST}/organizations/${organizationSlug}/glossaries`);

    gcy('glossaries-empty-add-button').click();

    gcy('create-edit-glossary-dialog').should('be.visible');

    gcy('create-glossary-field-name').type('Create Test Glossary');
    gcy('glossary-base-language-select').click();
    gcy('glossary-base-language-select-item').contains('English').click();

    gcy('create-glossary-submit').click();
    gcy('create-edit-glossary-dialog').should('not.exist');

    cy.contains('Create Test Glossary').should('be.visible');
  });
});
