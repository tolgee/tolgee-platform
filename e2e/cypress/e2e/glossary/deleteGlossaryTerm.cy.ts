import { glossaryTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy } from '../../common/shared';
import { HOST } from '../../common/constants';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';

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
    const organizationSlug = data.organizations.find(
      (org) => org.name === 'Owner'
    ).slug;
    cy.visit(`${HOST}/organizations/${organizationSlug}/glossaries`);

    gcy('glossary-list-item').filter(':contains("Test Glossary")').click();

    gcy('glossary-term-list-item')
      .filter(':contains("Apple")')
      .find('input[type="checkbox"]')
      .click();

    gcy('glossary-batch-delete-button').click();

    gcy('global-confirmation-dialog').should('be.visible');
    gcy('global-confirmation-confirm').click();

    cy.contains('Apple').should('not.exist');
  });

  it('Cancels glossary term deletion', () => {
    login('Owner');
    const organizationSlug = data.organizations.find(
      (org) => org.name === 'Owner'
    ).slug;
    cy.visit(`${HOST}/organizations/${organizationSlug}/glossaries`);

    gcy('glossary-list-item').filter(':contains("Test Glossary")').click();

    gcy('glossary-term-list-item')
      .filter(':contains("Apple")')
      .find('input[type="checkbox"]')
      .click();

    gcy('glossary-batch-delete-button').click();

    gcy('global-confirmation-dialog').should('be.visible');
    gcy('global-confirmation-cancel').click();

    gcy('global-confirmation-dialog').should('not.exist');
    cy.contains('Apple').should('be.visible');
  });

  it('Cannot delete glossary term without proper permissions', () => {
    login('Member');
    const organizationSlug = data.organizations.find(
      (org) => org.name === 'Owner'
    ).slug;
    cy.visit(`${HOST}/organizations/${organizationSlug}/glossaries`);

    gcy('glossary-list-item').filter(':contains("Test Glossary")').click();

    gcy('glossary-term-list-item')
      .filter(':contains("Apple")')
      .find('input[type="checkbox"]')
      .click();

    gcy('glossary-batch-delete-button').should('be.visible');
    gcy('glossary-batch-delete-button').should('be.disabled');
  });
});
