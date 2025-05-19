import { glossaryTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy } from '../../common/shared';
import { HOST } from '../../common/constants';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';

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
    const organizationSlug = data.organizations.find(
      (org) => org.name === 'Owner'
    ).slug;
    cy.visit(`${HOST}/organizations/${organizationSlug}/glossaries`);

    // Verify the glossary exists before deletion
    cy.contains('Test Glossary').should('be.visible');

    // Open the menu and click delete
    gcy('glossary-list-item')
      .filter(':contains("Test Glossary")')
      .findDcy('glossaries-list-more-button')
      .click();
    gcy('glossary-delete-button').click();

    // Confirm deletion in the confirmation dialog
    gcy('global-confirmation-dialog').should('be.visible');
    gcy('global-confirmation-hard-mode-text-field').type('TEST GLOSSARY');
    gcy('global-confirmation-confirm').click();

    // Verify the glossary is deleted
    cy.contains('Test Glossary').should('not.exist');
  });

  it('Cancels glossary deletion', () => {
    login('Owner');
    const organizationSlug = data.organizations.find(
      (org) => org.name === 'Owner'
    ).slug;
    cy.visit(`${HOST}/organizations/${organizationSlug}/glossaries`);

    // Verify the glossary exists
    cy.contains('Test Glossary').should('be.visible');

    // Open the menu and click delete
    gcy('glossary-list-item')
      .filter(':contains("Test Glossary")')
      .findDcy('glossaries-list-more-button')
      .click();
    gcy('glossary-delete-button').click();

    // Cancel deletion in the confirmation dialog
    gcy('global-confirmation-dialog').should('be.visible');
    gcy('global-confirmation-cancel').click();

    // Verify the glossary still exists
    cy.contains('Test Glossary').should('be.visible');
  });

  it('Cannot delete glossary without proper permissions', () => {
    login('Member');
    const organizationSlug = data.organizations.find(
      (org) => org.name === 'Owner'
    ).slug;
    cy.visit(`${HOST}/organizations/${organizationSlug}/glossaries`);

    // Verify the glossary exists
    cy.contains('Test Glossary').should('be.visible');

    // Open the menu and verify delete button is not present
    gcy('glossary-list-item')
      .filter(':contains("Test Glossary")')
      .findDcy('glossaries-list-more-button')
      .click();
    cy.get('[data-cy="glossary-delete-button"]').should('not.exist');
  });
});
