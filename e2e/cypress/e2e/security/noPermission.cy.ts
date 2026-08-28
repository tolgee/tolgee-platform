/// <reference types="cypress" />
import { HOST } from '../../common/constants';
import { organizationNewTestData } from '../../common/apiCalls/testData/testData';
import {
  login,
  setOrganizationCreationAllowed,
} from '../../common/apiCalls/common';
import {
  assertSwitchedToOrganization,
  gcy,
  gcyAdvanced,
} from '../../common/shared';
import { waitForGlobalLoading } from '../../common/loading';

context('No permission', () => {
  beforeEach(() => {
    organizationNewTestData.clean();
    organizationNewTestData.generateStandard();
    setOrganizationCreationAllowed(false);
    login('milan');
  });

  afterEach(() => {
    setOrganizationCreationAllowed(true);
    organizationNewTestData.clean();
  });

  it('sends a user without an organization to the community projects', () => {
    cy.visit(HOST);
    cy.url().should('include', '/community-projects');
    gcy('community-projects-view').should('be.visible');
  });

  it('shows the no permission screen where creating an organization is refused', () => {
    cy.visit(`${HOST}/organizations/add`);
    gcyAdvanced({
      value: 'no-permissions-message',
      reason: 'server-disallows',
    }).should('be.visible');
  });

  it('creates the organization when property changes', () => {
    setOrganizationCreationAllowed(true);
    cy.visit(HOST);
    assertSwitchedToOrganization('Milan');
  });

  it('offers the organization form where creating an organization is allowed', () => {
    setOrganizationCreationAllowed(true);
    cy.visit(HOST);
    waitForGlobalLoading();

    cy.visit(`${HOST}/organizations/add`);
    waitForGlobalLoading();
    gcy('no-permissions-message').should('not.exist');
    gcy('global-form-save-button').should('be.visible');
  });

  it('creates an organization through the relocated route', () => {
    setOrganizationCreationAllowed(true);

    cy.visit(`${HOST}/organizations/add`);
    waitForGlobalLoading();
    gcy('organization-name-field').find('input').type('Milan Organization');
    gcy('organization-address-part-field')
      .find('input')
      .should('not.have.value', '');

    gcy('global-form-save-button').click();
    waitForGlobalLoading();
    assertSwitchedToOrganization('Milan Organization');
  });

  it('refuses the organization form to a member once creation is turned off', () => {
    setOrganizationCreationAllowed(true);
    cy.visit(HOST);
    waitForGlobalLoading();
    gcy('organization-switch').should('exist');

    setOrganizationCreationAllowed(false);
    cy.visit(`${HOST}/organizations/add`);
    waitForGlobalLoading();
    gcyAdvanced({
      value: 'no-permissions-message',
      reason: 'server-disallows',
    }).should('be.visible');
    gcy('global-form-save-button').should('not.exist');
  });
});
