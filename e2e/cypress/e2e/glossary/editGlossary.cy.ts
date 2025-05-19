import { glossaryTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy } from '../../common/shared';
import { HOST } from '../../common/constants';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';

describe('Glossary editing', () => {
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

  it('Edits glossary name', () => {
    login('Owner');
    const organizationSlug = data.organizations.find(
      (org) => org.name === 'Owner'
    ).slug;
    cy.visit(`${HOST}/organizations/${organizationSlug}/glossaries`);

    gcy('glossaries-list-more-button').first().click();
    gcy('glossary-edit-button').click();

    gcy('create-edit-glossary-dialog').should('be.visible');
    gcy('create-glossary-field-name')
      .click()
      .focused()
      .clear()
      .type('Edited Glossary Name');
    gcy('create-edit-glossary-submit').click();

    gcy('create-edit-glossary-dialog').should('not.exist');
    cy.contains('Edited Glossary Name').should('be.visible');
  });

  it('Edits glossary base language', () => {
    login('Owner');
    const organizationSlug = data.organizations.find(
      (org) => org.name === 'Owner'
    ).slug;
    cy.visit(`${HOST}/organizations/${organizationSlug}/glossaries`);

    gcy('glossaries-list-more-button').first().click();
    gcy('glossary-edit-button').click();

    gcy('create-edit-glossary-dialog').should('be.visible');
    gcy('glossary-base-language-select').click();
    gcy('glossary-base-language-select-item').contains('French').click();
    gcy('create-edit-glossary-submit').click();

    gcy('create-edit-glossary-dialog').should('not.exist');

    gcy('glossaries-list-more-button').first().click();
    gcy('glossary-edit-button').click();

    gcy('create-edit-glossary-dialog').should('be.visible');
    cy.contains('French').should('be.visible');
    gcy('create-edit-glossary-submit').click();
  });

  it('Edits glossary assigned projects', () => {
    login('Owner');
    const organizationSlug = data.organizations.find(
      (org) => org.name === 'Owner'
    ).slug;
    cy.visit(`${HOST}/organizations/${organizationSlug}/glossaries`);

    gcy('glossaries-list-more-button').first().click();
    gcy('glossary-edit-button').click();

    gcy('create-edit-glossary-dialog').should('be.visible');

    gcy('assigned-projects-select').click();
    gcy('assigned-projects-select-item').contains('TheProject').click();
    cy.wait(50);
    cy.get('body').click(0, 0); // Close the dropdown

    gcy('create-edit-glossary-submit').click();

    gcy('create-edit-glossary-dialog').should('not.exist');

    gcy('glossaries-list-more-button').first().click();
    gcy('glossary-edit-button').click();

    gcy('create-edit-glossary-dialog').should('be.visible');
    cy.should('not.contain', 'TheProject');
    gcy('create-edit-glossary-submit').click();
  });
});
