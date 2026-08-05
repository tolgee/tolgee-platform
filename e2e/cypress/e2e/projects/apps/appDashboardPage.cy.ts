import { login } from '../../../common/apiCalls/common';
import { appsTestData } from '../../../common/apiCalls/testData/testData';
import { API_URL, HOST } from '../../../common/constants';
import { gcy } from '../../../common/shared';

const MANIFEST_URL = `${API_URL}/internal/e2e-data/apps/manifest.json`;

describe('project app dashboard page', () => {
  let organizationSlug: string;
  let projectId: number;

  beforeEach(() => {
    appsTestData.clean();
    appsTestData
      .generateStandard()
      .then((r) => r.body)
      .then((data) => {
        organizationSlug = data.organizations[0].slug;
        projectId = data.projects[0].id;
        login('apps-test-owner@test.com');

        cy.visit(`${HOST}/organizations/${organizationSlug}/apps`);
        gcy('organization-apps-register-button').click();
        gcy('organization-apps-register-manifest-url').type(MANIFEST_URL);
        gcy('organization-apps-register-continue').click();
        gcy('organization-apps-register-submit').click();
        gcy('organization-apps-item').should('exist');
      });
  });

  afterEach(() => {
    appsTestData.clean();
  });

  it('shows no app menu entry until the app is enabled for the project', () => {
    cy.visit(`${HOST}/projects/${projectId}`);
    gcy('project-menu-item-app').should('not.exist');
  });

  it('adds a dashboard page to the project menu and renders it in a sandboxed iframe', () => {
    cy.visit(`${HOST}/projects/${projectId}/manage/edit/apps`);
    gcy('project-settings-apps-item-toggle').click();
    gcy('project-settings-apps-item-toggle').find('input').should('be.checked');

    gcy('project-menu-item-app')
      .should('have.attr', 'aria-label', 'Home')
      .click();

    gcy('project-app-page-iframe')
      .should('have.attr', 'src', 'https://e2e-app.example.com/')
      .and('have.attr', 'sandbox');
  });

  it('shows the missing-page state when the app is disabled again', () => {
    cy.visit(`${HOST}/projects/${projectId}/manage/edit/apps`);
    gcy('project-settings-apps-item-toggle').click();
    gcy('project-settings-apps-item-toggle').find('input').should('be.checked');

    gcy('project-menu-item-app').click();
    gcy('project-app-page-iframe').should('exist');

    cy.url().then((appPageUrl) => {
      cy.visit(`${HOST}/projects/${projectId}/manage/edit/apps`);
      gcy('project-settings-apps-item-toggle').click();
      gcy('project-settings-apps-item-toggle')
        .find('input')
        .should('not.be.checked');

      cy.visit(appPageUrl);
      gcy('project-app-page-missing').should('exist');
    });
  });
});
