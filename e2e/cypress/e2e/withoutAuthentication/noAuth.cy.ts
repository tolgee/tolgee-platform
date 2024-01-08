import { HOST } from '../../common/constants';
import 'cypress-file-upload';
import {
  createTestProject,
  disableAuthentication,
  enableAuthentication,
  login,
} from '../../common/apiCalls/common';
import { gcy } from '../../common/shared';
import { waitForGlobalLoading } from '../../common/loading';

describe('Test no authentication mode', () => {
  beforeEach(() => {
    enableAuthentication();
    login().then(() => {
      createTestProject();
    });
    cy.visit(`${HOST}`);
  });

  it('Has limited user menu', () => {
    disableAuthentication();
    createTestProject();
    cy.reload();
    gcy('global-user-menu-button').click();

    // needs have access to tokens
    gcy('user-menu-user-settings')
      .contains('Project API keys')
      .should('be.visible');
    gcy('user-menu-user-settings')
      .contains('Personal Access Tokens')
      .should('be.visible');

    // needs access to language and theme switching
    gcy('user-menu-language-switch').should('be.visible');
    gcy('user-menu-theme-switch').should('be.visible');

    // no access to profile and organization
    gcy('user-menu-user-settings')
      .contains('Account settings')
      .should('not.exist');
    gcy('user-menu-organization-settings').should('not.exist');
    gcy('user-menu-logout').should('not.exist');
  });

  it('should not allow accessing user profile settings', () => {
    disableAuthentication();
    cy.visit(HOST + '/account/profile');
    waitForGlobalLoading();

    cy.location().its('pathname').should('not.eq', '/account/profile');
  });

  it('should not allow accessing user account security settings', () => {
    disableAuthentication();
    cy.visit(HOST + '/account/security');
    waitForGlobalLoading();

    cy.location().its('pathname').should('not.eq', '/account/security');
  });

  after(() => {
    enableAuthentication();
  });
});
