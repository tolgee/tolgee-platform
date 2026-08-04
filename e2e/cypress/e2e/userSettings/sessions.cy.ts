import { HOST, API_URL } from '../../common/constants';
import { sessionsTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import {
  assertMessage,
  confirmStandard,
  gcy,
  gcyAdvanced,
} from '../../common/shared';

describe('Active sessions', () => {
  beforeEach(() => {
    sessionsTestData.clean();
    sessionsTestData.generateStandard();
    login('user@user.com');
    cy.visit(`${HOST}/account`);
  });

  afterEach(() => {
    sessionsTestData.clean();
  });

  const row = (ip: string) =>
    gcyAdvanced({ value: 'session-list-item', 'session-ip': ip });

  const visitSessions = () => cy.visit(`${HOST}/account/security`);

  it('is part of the account security screen', () => {
    visitSessions();
    gcy('account-security-sessions').should('exist');
    gcy('session-list-item').should('exist');
  });

  it('lists the live sessions only', () => {
    visitSessions();
    // ten seeded live sessions plus the one this test just logged in with
    gcy('session-list-item').should('have.length', 11);

    // revoked, expired and impersonation sessions are never shown
    row('10.10.0.12').should('not.exist');
    row('10.10.0.13').should('not.exist');
    row('10.10.0.8').should('not.exist');
  });

  it('describes each device from its user agent', () => {
    visitSessions();
    row('10.10.0.1')
      .findDcy('session-list-item-device')
      .contains('Edge · Windows');
    row('10.10.0.2')
      .findDcy('session-list-item-device')
      .contains('Opera · Linux');
    row('10.10.0.5')
      .findDcy('session-list-item-device')
      .contains('Safari · iOS');
    // an Android user agent also says "Linux", a macOS one also says "Safari"
    row('10.10.0.6')
      .findDcy('session-list-item-device')
      .contains('Chrome · Android');
    row('10.10.0.7')
      .findDcy('session-list-item-device')
      .contains('Chrome · macOS');
    // unparseable and missing user agents still render something
    row('10.10.0.9').findDcy('session-list-item-device').should('not.be.empty');
    row('10.10.0.11')
      .findDcy('session-list-item-device')
      .contains('Unknown device');
  });

  it('shows the location, falling back to the IP when there is none', () => {
    visitSessions();
    row('10.10.0.1').findDcy('session-list-item-location').contains('Prague');
    // a session located to a country but no city shows the country alone
    row('10.10.0.11')
      .findDcy('session-list-item-location')
      .contains('Netherlands');
    // no location resolved - a private address reads as local rather than as a raw IP
    row('10.10.0.9')
      .findDcy('session-list-item-location')
      .contains('Local network');
  });

  it('marks exactly one session as the current one', () => {
    visitSessions();
    gcy('session-list-item-current-badge').should('have.length', 1);
    // none of the seeded sessions is the one this browser is using
    row('10.10.0.1')
      .findDcy('session-list-item-current-badge')
      .should('not.exist');
  });

  it('revokes a single session', () => {
    visitSessions();
    row('10.10.0.1').findDcy('session-list-item-revoke-button').click();
    confirmStandard();
    assertMessage('Session revoked');
    row('10.10.0.1').should('not.exist');
    gcy('session-list-item').should('have.length', 10);
  });

  it('revokes all other sessions and blocks their tokens', () => {
    // a second live token, used once so that it is cached as active
    cy.request({
      url: API_URL + '/api/public/generatetoken',
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'user@user.com', password: 'admin' }),
    }).then((response) => {
      const otherToken = response.body.accessToken;

      cy.request({
        url: API_URL + '/v2/user',
        headers: { Authorization: `Bearer ${otherToken}` },
      })
        .its('status')
        .should('eq', 200);

      visitSessions();
      gcy('sessions-revoke-all-others-button').click();
      confirmStandard();
      assertMessage('All other sessions were revoked');

      gcy('session-list-item').should('have.length', 1);
      gcy('session-list-item-current-badge').should('have.length', 1);

      cy.request({
        url: API_URL + '/v2/user',
        headers: { Authorization: `Bearer ${otherToken}` },
        failOnStatusCode: false,
      })
        .its('status')
        .should('eq', 401);
    });
  });

  it('offers no revoke button for the current session', () => {
    visitSessions();
    gcy('session-list-item-current-badge')
      .closestDcy('session-list-item')
      .findDcy('session-list-item-revoke-button')
      .should('not.exist');
  });

  it('drops the session of a signed-out browser', () => {
    visitSessions();
    gcy('session-list-item').should('have.length', 11);

    gcy('global-user-menu-button').click();
    gcy('user-menu-logout').click();
    cy.url().should('include', '/login');

    login('user@user.com');
    visitSessions();
    // the signed-out session is gone, the fresh login replaces it
    gcy('session-list-item').should('have.length', 11);
    gcy('session-list-item-current-badge').should('have.length', 1);
  });

  it('still lands on the login screen when the sign-out call fails', () => {
    visitSessions();
    cy.intercept('DELETE', '**/v2/user/sessions/current', {
      forceNetworkError: true,
    });

    gcy('global-user-menu-button').click();
    gcy('user-menu-logout').click();

    cy.url().should('include', '/login');
    cy.window().its('localStorage.jwtToken').should('not.exist');
  });
});
