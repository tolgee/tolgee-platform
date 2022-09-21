/// <reference types="cypress" />
import { HOST } from '../../common/constants';
import { organizationNewTestData } from '../../common/apiCalls/testData/testData';
import { login, setProperty } from '../../common/apiCalls/common';
import { gcy } from '../../common/shared';

context('No permission', () => {
  beforeEach(() => {
    organizationNewTestData.clean();
    organizationNewTestData.generate();
    setProperty('authentication.userCanCreateOrganizations', false);
    login('milan');
  });

  afterEach(() => {
    organizationNewTestData.clean();
  });

  it('shows the no permission screen', () => {
    cy.visit(HOST);
    cy.contains('No permission').should('be.visible');
  });

  it('creates the organization when property changes', () => {
    setProperty('authentication.userCanCreateOrganizations', true);
    cy.visit(HOST);
    gcy('organization-switch').should('exist').should('contain', 'Milan');
  });
});
