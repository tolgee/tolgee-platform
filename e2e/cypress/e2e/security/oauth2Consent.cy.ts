import { login } from '../../common/apiCalls/common';
import { oauth2ConsentTestData } from '../../common/apiCalls/testData/testData';
import { API_URL, HOST } from '../../common/constants';
import { waitForGlobalLoading } from '../../common/loading';
import { gcyAdvanced } from '../../common/shared';

const CLIENT_ID = 'tolgee-browser-extension';
// Must match the project OAuth2ConsentE2eData creates.
const PROJECT_NAME = 'OAuth2 Consent Project';
const REDIRECT_URI = `${API_URL}/internal/e2e-data/oauth2-consent/callback`;
// A fixed PKCE pair: challenge = base64url(sha256(verifier)). The code is never exchanged here, so the verifier is
// never needed — the challenge only has to be well-formed for /oauth2/authorize to accept the request.
const CODE_CHALLENGE = '9fa4Kxg-kvmCollzytmpG-4BeAy0obZey5rQMBKBXVc';
// Reaching the consent screen is a redirect plus the screen's own API calls, and leaving it is another navigation. On
// a backend that has just started that comfortably exceeds the default command timeout, so these steps get their own
// budget rather than relying on retries — PR runs have none.
const NAVIGATION_TIMEOUT = 60000;

const authorizeUrl = (scope: string) =>
  `${API_URL}/oauth2/authorize?response_type=code` +
  `&client_id=${CLIENT_ID}` +
  `&redirect_uri=${encodeURIComponent(REDIRECT_URI)}` +
  `&scope=${encodeURIComponent(scope)}` +
  `&state=e2e-state` +
  `&code_challenge=${CODE_CHALLENGE}` +
  `&code_challenge_method=S256`;

describe('OAuth2 consent', () => {
  beforeEach(() => {
    oauth2ConsentTestData.clean({ failOnStatusCode: false });
    oauth2ConsentTestData
      .generateStandard()
      .then((r) => r.body)
      .then(({ users }) => {
        login(users[0].username);
        // The consent screen authenticates with the stored webapp JWT, so the app has to be loaded once for that JWT
        // to be in local storage.
        cy.visit(HOST);
        waitForGlobalLoading();
      });
  });

  afterEach(() => {
    oauth2ConsentTestData.clean({ failOnStatusCode: false });
  });

  it('shows what is being requested and returns a code on approval', () => {
    cy.visit(authorizeUrl('keys.view translations.view translations.edit'));

    cy.gcy('oauth2-consent', { timeout: NAVIGATION_TIMEOUT }).should(
      'be.visible'
    );
    ['keys.view', 'translations.view', 'translations.edit'].forEach((scope) =>
      gcyAdvanced({ value: 'oauth2-consent-scope', scope }).should('exist')
    );

    // Nothing is pre-selected: the widest grant has to be asked for.
    cy.gcy('oauth2-consent-allow').should('be.disabled');
    cy.gcy('oauth2-consent-project-all').click();
    cy.gcy('oauth2-consent-allow').click();

    cy.url({ timeout: NAVIGATION_TIMEOUT }).should('include', 'code=');
    cy.url().should('include', 'state=e2e-state');
  });

  it('redirects with access_denied when the user denies', () => {
    cy.visit(authorizeUrl('keys.view translations.view'));

    cy.gcy('oauth2-consent', { timeout: NAVIGATION_TIMEOUT }).should(
      'be.visible'
    );
    cy.gcy('oauth2-consent-deny').click();

    cy.url({ timeout: NAVIGATION_TIMEOUT }).should(
      'include',
      'error=access_denied'
    );
    cy.url().should('include', 'state=e2e-state');
    cy.url().should('not.include', 'code=');
  });

  // What the grant is bound to is asserted in OAuth2AuthorizationCodeFlowTest; this pins the browser-observable half.
  it('will not approve a single-project grant until a project is picked', () => {
    cy.visit(authorizeUrl('keys.view translations.view'));

    cy.gcy('oauth2-consent', { timeout: NAVIGATION_TIMEOUT }).should(
      'be.visible'
    );
    cy.gcy('oauth2-consent-project-one').click();
    cy.gcy('oauth2-consent-allow').should('be.disabled');

    cy.gcy('project-select').click();
    gcyAdvanced({
      value: 'user-switch-item',
      'project-name': PROJECT_NAME,
    }).click();

    cy.gcy('oauth2-consent-allow').should('not.be.disabled').click();
    cy.url({ timeout: NAVIGATION_TIMEOUT }).should('include', 'code=');
  });

  it('lets the user narrow the requested scopes before approving', () => {
    cy.visit(authorizeUrl('keys.view translations.view translations.edit'));

    cy.gcy('oauth2-consent', { timeout: NAVIGATION_TIMEOUT }).should(
      'be.visible'
    );
    gcyAdvanced({
      value: 'oauth2-consent-scope',
      scope: 'translations.edit',
    }).should('exist');

    cy.gcy('oauth2-consent-modify').click();
    cy.gcy('oauth2-consent-scopes').should('be.visible');

    // translations.edit is optional for this client — only keys.view and translations.view are locked as required — so
    // deselecting it must stick and the flow must still complete.
    gcyAdvanced({
      value: 'permissions-advanced-item',
      scope: 'translations.edit',
    }).click();
    cy.gcy('oauth2-consent-modify').click();

    gcyAdvanced({
      value: 'oauth2-consent-scope',
      scope: 'translations.edit',
    }).should('not.exist');
    gcyAdvanced({
      value: 'oauth2-consent-scope',
      scope: 'translations.view',
    }).should('exist');

    cy.gcy('oauth2-consent-project-all').click();
    cy.gcy('oauth2-consent-allow').click();
    cy.url({ timeout: NAVIGATION_TIMEOUT }).should('include', 'code=');
  });
});
