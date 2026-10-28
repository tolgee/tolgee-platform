import { gcy, visitProjectSettings } from '../../common/shared';
import { visitExport } from '../../common/export';
import { loginViaForm } from '../../common/login';
import { communityStatsTestData } from '../../common/apiCalls/testData/testData';
import {
  disableRegistration,
  enableRegistration,
} from '../../common/apiCalls/common';
import { waitForGlobalLoading } from '../../common/loading';
import { visitTranslations } from '../../common/translations';

describe('Community project browsed without an account', () => {
  let publicProjectId: number;
  let privateProjectId: number;

  beforeEach(() => {
    communityStatsTestData.clean();
    communityStatsTestData.generateStandard().then((res) => {
      const projects = res.body.projects as { id: number; name: string }[];
      publicProjectId = projects.find(
        (p) => p.name === 'Community Stats Demo'
      )!.id;
      privateProjectId = projects.find(
        (p) => p.name === 'Community Stats Private'
      )!.id;
    });
  });

  afterEach(() => {
    communityStatsTestData.clean();
    enableRegistration();
  });

  const visitTranslationsAsGuest = (projectId: number) => {
    visitTranslations(projectId);
    waitForGlobalLoading();
  };

  it('shows the translations read-only with the guest bar and guest top bar', () => {
    visitTranslationsAsGuest(publicProjectId);

    gcy('translations-guest-bar').should('be.visible');
    gcy('translations-guest-bar').within(() => {
      gcy('guest-login-button').should('not.exist');
    });
    gcy('guest-login-button').should('be.visible');
    gcy('guest-sign-up-button').should('be.visible');
    gcy('global-user-menu-button').should('not.exist');
    gcy('translations-row').should('have.length.at.least', 1);
  });

  it('asks a guest to log in when a translation cell is clicked', () => {
    visitTranslationsAsGuest(publicProjectId);

    gcy('translations-table-cell-translation').first().click();
    gcy('global-editor').should('not.exist');
    gcy('global-confirmation-dialog').should('be.visible');
    gcy('global-confirmation-cancel').click();
    gcy('global-confirmation-dialog').should('not.exist');

    gcy('translations-view-table-button').click();
    gcy('translations-view-table').should('be.visible');
    gcy('translations-table-cell-translation').first().click();
    gcy('global-editor').should('not.exist');
    gcy('global-confirmation-dialog').should('be.visible');
    gcy('global-confirmation-cancel').click();

    gcy('translations-table-cell-translation').first().click();
    gcy('global-confirmation-confirm').click();
    cy.url().should('include', '/login');
    loginViaForm();
    cy.url().should('include', `/projects/${publicProjectId}/translations`);
    gcy('translations-guest-bar').should('not.exist');
  });

  it('sends a guest to sign up from the dialog and hides the top bar buttons there', () => {
    visitTranslationsAsGuest(publicProjectId);
    gcy('translations-table-cell-translation').first().click();
    gcy('guest-login-dialog-sign-up-link').click();
    cy.url().should('include', '/sign_up');
    gcy('global-confirmation-dialog').should('not.exist');
    gcy('guest-login-button').should('not.exist');
    gcy('guest-sign-up-button').should('not.exist');
  });

  it('offers no sign-up to a guest when registrations are off', () => {
    disableRegistration();
    visitTranslationsAsGuest(publicProjectId);
    gcy('guest-login-button').should('be.visible');
    gcy('guest-sign-up-button').should('not.exist');

    gcy('translations-table-cell-translation').first().click();
    gcy('global-confirmation-dialog').should('be.visible');
    gcy('guest-login-dialog-sign-up-link').should('not.exist');
  });

  it('offers only what the view-only floor allows in the project menu', () => {
    visitTranslationsAsGuest(publicProjectId);
    gcy('project-menu-item-dashboard').should('be.visible');
    gcy('project-menu-item-translations').should('be.visible');
    gcy('project-menu-item-export').should('be.visible');
    gcy('project-menu-item-projects').should('not.exist');
    gcy('project-menu-item-integrate').should('not.exist');
    gcy('project-menu-item').should('have.length', 3);
  });

  it('sends a guest to login for a private project and back after login', () => {
    visitTranslationsAsGuest(privateProjectId);
    cy.url().should('include', '/login');
    loginViaForm();
    cy.url().should('include', `/projects/${privateProjectId}/translations`);
  });

  it('sends a guest to login for member-only routes but opens the export', () => {
    visitProjectSettings(publicProjectId);
    cy.url().should('include', '/login');
    visitExport(publicProjectId);
    waitForGlobalLoading();
    cy.url().should('include', `/projects/${publicProjectId}/export`);
    gcy('guest-login-button').should('be.visible');
  });

  it('returns to the translations after logging in from the top bar', () => {
    visitTranslationsAsGuest(publicProjectId);
    gcy('guest-login-button').click();
    cy.url().should('include', '/login');
    gcy('guest-login-button').should('not.exist');
    loginViaForm();
    cy.url().should('include', `/projects/${publicProjectId}/translations`);
    gcy('translations-guest-bar').should('not.exist');
    gcy('global-user-menu-button').should('be.visible');
  });
});
