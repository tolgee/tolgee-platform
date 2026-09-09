/// <reference types="cypress" />
import { HOST } from '../../common/constants';
import { organizationNewTestData } from '../../common/apiCalls/testData/testData';
import {
  login,
  setOrganizationCreationAllowed,
} from '../../common/apiCalls/common';
import { gcy } from '../../common/shared';

context('Organization creation disabled', () => {
  beforeEach(() => {
    organizationNewTestData.clean();
    organizationNewTestData.generateStandard();
    setOrganizationCreationAllowed(false);
    login('milan');
  });

  afterEach(() => {
    organizationNewTestData.clean();
    setOrganizationCreationAllowed(true);
  });

  it('sends a user with no organization to the community projects', () => {
    cy.visit(HOST);
    cy.url().should('include', '/community-projects');
    gcy('community-projects-view').should('be.visible');
  });

  it('creates the organization when property changes', () => {
    setOrganizationCreationAllowed(true);
    cy.visit(HOST);
    gcy('organization-switch').should('exist').should('contain', 'Milan');
  });
});
