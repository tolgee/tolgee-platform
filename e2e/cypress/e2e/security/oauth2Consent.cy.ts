import { internalFetch, login } from '../../common/apiCalls/common';
import { oauth2ConsentTestData } from '../../common/apiCalls/testData/testData';
import { API_URL, HOST, PASSWORD } from '../../common/constants';
import { waitForGlobalLoading } from '../../common/loading';
import { scopeCheckbox } from '../../common/permissionsMenu';
import { gcyAdvanced } from '../../common/shared';

const CLIENT_ID = 'tolgee-browser-extension';
const REDIRECT_URI = `${API_URL}/internal/e2e-data/oauth2-consent/callback`;
// A fixed PKCE pair: challenge = base64url(sha256(verifier)). The code is never exchanged here, so the verifier is
// never needed — the challenge only has to be well-formed for /oauth2/authorize to accept the request.
const CODE_CHALLENGE = '9fa4Kxg-kvmCollzytmpG-4BeAy0obZey5rQMBKBXVc';
// Reaching the consent screen is a redirect plus the screen's own API calls, and leaving it is another navigation. On
// a backend that has just started that comfortably exceeds the default command timeout, so these steps get their own
// budget rather than relying on retries — PR runs have none.
const NAVIGATION_TIMEOUT = 60000;

// A redirect that is not on the user's machine. It must be registered for the extension in BOTH
// e2e/docker-compose.yml (docker run) and backend/app/src/main/resources/application-e2e.yaml (openE2eDev run);
// the two have drifted before. Nothing listens there: this is only ever presented on cases that read the consent
// screen and never approve.
const SITE_REDIRECT_URI = 'https://e2e-site.test/callback';
// The CLI is registered on every instance whose issuer resolves, with no redirect URI configured for it. The port is
// ignored on a loopback redirect, so this one is accepted without anything listening behind it.
const CLI_CLIENT_ID = 'tolgee-cli';
const CLI_REDIRECT_URI = 'http://127.0.0.1:53211/callback';

const authorizeUrl = (
  scope: string,
  clientId: string = CLIENT_ID,
  redirectUri: string = REDIRECT_URI
) =>
  `${API_URL}/oauth2/authorize?response_type=code` +
  `&client_id=${encodeURIComponent(clientId)}` +
  `&redirect_uri=${encodeURIComponent(redirectUri)}` +
  `&scope=${encodeURIComponent(scope)}` +
  `&state=e2e-state` +
  `&code_challenge=${CODE_CHALLENGE}` +
  `&code_challenge_method=S256`;

describe('OAuth2 consent', () => {
  let projectName: string;
  // The fixture decides how it must be addressed as a CIMD client; see OAuth2ConsentE2eDataController.
  let cimdBaseUrl: string;

  before(() => {
    internalFetch('e2e-data/oauth2-consent/cimd-base-url').then(({ body }) => {
      cimdBaseUrl = body.baseUrl;
    });
  });

  beforeEach(() => {
    oauth2ConsentTestData.clean({ failOnStatusCode: false });
    oauth2ConsentTestData
      .generateStandard()
      .then((r) => r.body)
      .then(({ users, projects }) => {
        projectName = projects[0].name;
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
    cy.visit(
      authorizeUrl(
        'keys.view translations.view translations.edit translations.state-edit'
      )
    );

    cy.gcy('oauth2-consent', { timeout: NAVIGATION_TIMEOUT }).should(
      'be.visible'
    );
    cy.gcy('oauth2-consent-unverified').should('not.exist');
    cy.gcy('oauth2-consent-subtitle').should('be.visible');
    // Keys holds more scopes than this app asked for, so its one scope is named rather than collapsed.
    gcyAdvanced({ value: 'oauth2-consent-scope', scope: 'keys.view' }).should(
      'exist'
    );
    // Translations has nothing left to grant, so the group says that in one chip instead of naming each one.
    gcyAdvanced({ value: 'oauth2-consent-scope', scope: '_all' }).should(
      'exist'
    );
    gcyAdvanced({
      value: 'oauth2-consent-scope',
      scope: 'translations.edit',
    }).should('not.exist');

    // Nothing is pre-selected: the widest grant has to be asked for.
    cy.gcy('oauth2-consent-allow').should('be.disabled');
    cy.gcy('oauth2-consent-project-all').click();
    cy.gcy('oauth2-consent-allow').click();

    cy.url({ timeout: NAVIGATION_TIMEOUT }).should('include', 'code=');
    cy.url().should('include', 'state=e2e-state');
  });

  it('names the scopes when the resource has more to grant than the app asked for', () => {
    cy.visit(authorizeUrl('translations.view translations.edit'));

    cy.gcy('oauth2-consent', { timeout: NAVIGATION_TIMEOUT }).should(
      'be.visible'
    );
    gcyAdvanced({ value: 'oauth2-consent-scope', scope: '_all' }).should(
      'not.exist'
    );
    gcyAdvanced({
      value: 'oauth2-consent-scope',
      scope: 'translations.edit',
    }).should('exist');
  });

  it('asks for the password when the session never passed one, and approves once it is given', () => {
    // Logging in answers with a super token, so the only way to reach the dialog is a token that never passed a
    // password check — which is exactly the session an attacker holding a lifted JWT would have.
    internalFetch('e2e-data/oauth2-consent/non-super-jwt').then(({ body }) => {
      cy.window().then((w) => w.localStorage.setItem('jwtToken', body.jwt));
    });

    cy.visit(authorizeUrl('keys.view translations.view'));

    cy.gcy('oauth2-consent', { timeout: NAVIGATION_TIMEOUT }).should(
      'be.visible'
    );
    cy.gcy('oauth2-consent-project-all').click();
    cy.gcy('oauth2-consent-allow').click();

    cy.gcy('sensitive-protection-dialog').should('be.visible');
    cy.url().should('not.include', 'code=');

    cy.gcy('sensitive-dialog-password-input').type(PASSWORD);
    cy.gcy('sensitive-protection-dialog')
      .findDcy('global-form-save-button')
      .click();

    cy.url({ timeout: NAVIGATION_TIMEOUT }).should('include', 'code=');
    cy.url().should('include', 'state=e2e-state');
  });

  it('says so when the code goes to an application on the user machine', () => {
    cy.visit(
      authorizeUrl('translations.view', CLI_CLIENT_ID, CLI_REDIRECT_URI)
    );

    cy.gcy('oauth2-consent', { timeout: NAVIGATION_TIMEOUT }).should(
      'be.visible'
    );
    cy.gcy('oauth2-consent-local-app').should('be.visible');
    // The CLI is one of ours, so it is not presented as an app nobody vetted.
    cy.gcy('oauth2-consent-unverified').should('not.exist');
  });

  it('does not claim a local application when the code goes to a site', () => {
    cy.visit(authorizeUrl('translations.view', CLIENT_ID, SITE_REDIRECT_URI));

    cy.gcy('oauth2-consent', { timeout: NAVIGATION_TIMEOUT }).should(
      'be.visible'
    );
    cy.gcy('oauth2-consent-local-app').should('not.exist');
  });

  it('renders the unverified-app treatment for a CIMD client', () => {
    cy.visit(
      authorizeUrl(
        'translations.view',
        `${cimdBaseUrl}/cimd-client`,
        `${cimdBaseUrl}/callback`
      )
    );

    cy.gcy('oauth2-consent', { timeout: NAVIGATION_TIMEOUT }).should(
      'be.visible'
    );
    cy.gcy('oauth2-consent-unverified-warning').should('be.visible');
    // The app's self-asserted name must not reach the subtitle, which renders above the warning.
    cy.gcy('oauth2-consent-subtitle').should('not.exist');
    cy.gcy('oauth2-consent-app-name').should('contain', 'E2E Unverified App');
    cy.gcy('oauth2-consent-origin').should(
      'contain',
      new URL(cimdBaseUrl).origin
    );
    cy.gcy('oauth2-consent-unverified').find('img').should('not.exist');
  });

  it('says an unverified app is unverified and nothing more, even when it is local', () => {
    cy.visit(
      authorizeUrl(
        'translations.view',
        `${cimdBaseUrl}/cimd-client`,
        CLI_REDIRECT_URI
      )
    );

    cy.gcy('oauth2-consent', { timeout: NAVIGATION_TIMEOUT }).should(
      'be.visible'
    );
    cy.gcy('oauth2-consent-unverified-warning').should('be.visible');
    // Both banners ask the same question, and the stronger one is the one that stays.
    cy.gcy('oauth2-consent-local-app').should('not.exist');
  });

  it('forwards the resource indicator into the authorize call', () => {
    const mcpResource = `${HOST}/mcp/developer`;
    cy.intercept('POST', '/v2/oauth2/authorize').as('authorize');

    cy.visit(
      authorizeUrl('keys.view translations.view') +
        `&resource=${encodeURIComponent(mcpResource)}`
    );

    cy.wait('@authorize', { timeout: NAVIGATION_TIMEOUT })
      .its('request.body.resource')
      .should('eq', mcpResource);
    cy.gcy('oauth2-consent', { timeout: NAVIGATION_TIMEOUT }).should(
      'be.visible'
    );
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
      value: 'project-search-select-item',
      'project-name': projectName,
    }).click();

    cy.gcy('oauth2-consent-allow').should('not.be.disabled').click();
    cy.url({ timeout: NAVIGATION_TIMEOUT }).should('include', 'code=');
  });

  it('lets the user narrow the requested scopes before approving', () => {
    cy.visit(authorizeUrl('keys.view translations.view translations.edit'));

    cy.gcy('oauth2-consent', { timeout: NAVIGATION_TIMEOUT }).should(
      'be.visible'
    );
    cy.gcy('oauth2-consent-modify').click();
    cy.gcy('oauth2-consent-scopes').should('be.visible');

    scopeCheckbox('keys.view').find('input').should('be.disabled');

    // Only keys.view and translations.view are locked as required for this client
    // (OAuth2ClientRegistry.browserExtension).
    scopeCheckbox('translations.edit').find('input').should('be.checked');
    scopeCheckbox('translations.edit').click();
    scopeCheckbox('translations.edit').find('input').should('not.be.checked');

    cy.gcy('oauth2-consent-project-all').click();
    cy.gcy('oauth2-consent-allow').click();
    cy.url({ timeout: NAVIGATION_TIMEOUT }).should('include', 'code=');
  });

  it('deselects everything the client did not require', () => {
    cy.visit(authorizeUrl('keys.view translations.view translations.edit'));

    cy.gcy('oauth2-consent', { timeout: NAVIGATION_TIMEOUT }).should(
      'be.visible'
    );
    cy.gcy('oauth2-consent-modify').click();
    cy.gcy('oauth2-consent-scopes').should('be.visible');

    cy.gcy('oauth2-consent-deselect-all').click();

    scopeCheckbox('translations.edit').find('input').should('not.be.checked');
    // keys.view and translations.view are required by this client, so they are locked and stay.
    scopeCheckbox('keys.view').find('input').should('be.checked');
    scopeCheckbox('translations.view').find('input').should('be.checked');
  });
});
