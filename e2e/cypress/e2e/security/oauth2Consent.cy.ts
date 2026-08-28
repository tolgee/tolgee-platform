import { login } from '../../common/apiCalls/common';
import { oauth2ConsentTestData } from '../../common/apiCalls/testData/testData';
import { API_URL, HOST } from '../../common/constants';
import { waitForGlobalLoading } from '../../common/loading';
import { gcyAdvanced } from '../../common/shared';

// A third case in this file made whichever test ran last fail intermittently, on state shared between cases rather
// than on the flow — re-check for that before adding one. Denial is covered by OAuth2AuthorizationCodeFlowTest.
const CLIENT_ID = 'tolgee-browser-extension';
const REDIRECT_URI = `${API_URL}/internal/e2e-data/oauth2-consent/callback`;
// A fixed PKCE pair: challenge = base64url(sha256(verifier)). The code is never exchanged here, so the verifier is
// never needed — the challenge only has to be well-formed for /oauth2/authorize to accept the request.
const CODE_CHALLENGE = '9fa4Kxg-kvmCollzytmpG-4BeAy0obZey5rQMBKBXVc';
// Reaching the consent screen is three full page loads (authorize -> bootstrap -> authorize -> consent) and leaving it
// is another redirect chain. On a backend that has just started, that comfortably exceeds the default command timeout,
// so these steps get their own budget rather than relying on retries — PR runs have none.
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
        // The bootstrap page turns the stored webapp JWT into the session /oauth2/authorize needs, so the app has to
        // be loaded once for that JWT to be in local storage.
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
    cy.gcy('oauth2-consent-scope').should('have.length.at.least', 1);

    cy.gcy('oauth2-consent-allow').click();

    cy.url({ timeout: NAVIGATION_TIMEOUT }).should('include', 'code=');
    cy.url().should('include', 'state=e2e-state');
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
    cy.gcy('permissions-advanced-item')
      .filter('[permissions-scope="translations.edit"]')
      .click();
    cy.gcy('oauth2-consent-modify').click();

    gcyAdvanced({
      value: 'oauth2-consent-scope',
      scope: 'translations.edit',
    }).should('not.exist');
    gcyAdvanced({
      value: 'oauth2-consent-scope',
      scope: 'translations.view',
    }).should('exist');

    cy.gcy('oauth2-consent-allow').click();
    cy.url({ timeout: NAVIGATION_TIMEOUT }).should('include', 'code=');
  });
});
