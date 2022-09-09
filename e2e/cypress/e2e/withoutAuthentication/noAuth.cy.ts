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

  it(
    'Has API keys item in project menu',
    {
      retries: {
        runMode: 3,
      },
    },
    () => {
      disableAuthentication();
      createTestProject();
      cy.reload();
      gcy('project-list-more-button').click();
      gcy('project-settings-button').click();
      gcy('project-menu-items')
        .get(`[aria-label="API keys"]`)
        .should('be.visible');
    }
  );

  it('Has no user menu', () => {
    cy.reload();
    const globalUserMenuDataCy = 'global-user-menu-button';
    gcy(globalUserMenuDataCy).should('be.visible');
    disableAuthentication();
    cy.reload();
    cy.contains('Projects').should('be.visible');
    gcy(globalUserMenuDataCy).should('not.exist');
  });

  it('Has no link to User profile', () => {
    cy.visit(HOST + '/account/apiKeys');
    cy.contains('My API keys');

    cy.gcy('settings-menu-item').contains('User profile').should('be.visible');
    disableAuthentication();
    cy.reload();
    cy.contains('My API keys').should('be.visible');
    cy.gcy('settings-menu-item').contains('User profile').should('not.exist');
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
