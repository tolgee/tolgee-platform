import { login } from '../../../common/apiCalls/common';
import { appsTestData } from '../../../common/apiCalls/testData/testData';
import { API_URL, HOST } from '../../../common/constants';
import { gcy } from '../../../common/shared';

const MANIFEST_URL = `${API_URL}/internal/e2e-data/apps/manifest.json`;

const registerApp = () => {
  gcy('organization-apps-register-button').click();
  gcy('organization-apps-register-manifest-url').type(MANIFEST_URL);
  gcy('organization-apps-register-continue').click();
  gcy('organization-apps-register-submit').click();
};

describe('organization apps', () => {
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
      });
  });

  afterEach(() => {
    appsTestData.clean();
  });

  it('registers an app from a manifest URL and enables it for a project', () => {
    gcy('organization-apps-register-button').click();
    gcy('organization-apps-register-manifest-url').type(MANIFEST_URL);
    gcy('organization-apps-register-continue').click();
    gcy('organization-apps-register-consent-scope').should(
      'contain',
      'translations.view'
    );
    gcy('organization-apps-register-submit').click();
    gcy('organization-apps-item').should('contain', 'E2E Test App');
    gcy('organization-apps-item-scopes').should('contain', 'translations.view');

    cy.visit(`${HOST}/projects/${projectId}/manage/edit/apps`);
    gcy('project-settings-apps-item').should('contain', 'E2E Test App');
    gcy('project-settings-apps-item-toggle')
      .find('input')
      .should('not.be.checked');
    gcy('project-settings-apps-item-toggle').click();
    gcy('project-settings-apps-item-toggle').find('input').should('be.checked');

    gcy('project-settings-apps-item-toggle').click();
    gcy('project-settings-apps-item-toggle')
      .find('input')
      .should('not.be.checked');
  });

  it('refreshes an app manifest from the org settings', () => {
    registerApp();
    gcy('organization-apps-item').should('exist');

    gcy('organization-apps-item-refresh').click();
    gcy('organization-apps-refresh-dialog').should('be.visible');
    gcy('organization-apps-refresh-scope-kept').should(
      'contain',
      'translations.view'
    );
    gcy('organization-apps-refresh-submit').click();
    gcy('organization-apps-refresh-dialog').should('not.exist');
    gcy('organization-apps-item').should('contain', 'E2E Test App');
  });

  it('removes a registered app', () => {
    registerApp();
    gcy('organization-apps-item').should('exist');

    gcy('organization-apps-item-remove').click();
    gcy('global-confirmation-confirm').click();
    gcy('organization-apps-item').should('not.exist');
  });
});
