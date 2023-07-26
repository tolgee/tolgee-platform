import { HOST, PASSWORD, USERNAME } from '../common/constants';
import { waitForGlobalLoading } from './loading';
import { getInput } from './xPath';
import { gcy } from './shared';
import {
  deleteUserSql,
  enableEmailVerification,
  enableRegistration,
} from './apiCalls/common';

export const loginWithFakeGithub = () => {
  cy.intercept('https://github.com/login/oauth/**', {
    statusCode: 200,
    body: 'Fake GitHub',
  });
  cy.contains('GitHub login').click();
  cy.contains('Fake GitHub').should('be.visible');
  cy.visit(
    HOST +
      '/login/auth_callback/github?scope=user%3Aemail&code=this_is_dummy_code'
  );
  cy.contains('Projects').should('be.visible');
};

export const loginWithFakeOAuth2 = () => {
  cy.intercept('https://dummy-url.com/**', {
    statusCode: 200,
    body: 'Fake OAuht2',
  }).as('oauth2');
  cy.contains('OAuth2 login').click();

  cy.wait('@oauth2').then((interception) => {
    const params = new URL(interception.request.url).searchParams;
    expect(params.get('client_id')).to.eq('dummy_client_id');
    expect(params.get('response_type')).to.eq('code');
    expect(params.get('scope')).to.eq('openid email profile');
  });

  cy.contains('Fake OAuht2').should('be.visible');
  cy.visit(
    HOST +
      '/login/auth_callback/oauth2?code=this_is_dummy_code&redirect_uri=https%3A%2F%2Fdummy-url.com%2Fcallback'
  );
  cy.contains('Projects').should('be.visible');
};

export const loginViaForm = (username = USERNAME, password = PASSWORD) => {
  cy.xpath('//input[@name="username"]')
    .type(username)
    .should('have.value', username);
  cy.xpath('//input[@name="password"]')
    .type(password)
    .should('have.value', password);
  cy.gcy('login-button').click();
  return waitForGlobalLoading();
};

export const visitSignUp = () => cy.visit(HOST + '/sign_up');

export const fillAndSubmitSignUpForm = (
  username: string,
  withOrganization = true
) => {
  cy.waitForDom();
  cy.xpath(getInput('name')).should('be.visible').type('Test user');
  cy.xpath(getInput('email')).type(username);
  if (withOrganization) {
    cy.xpath(getInput('organizationName')).type('organization');
  }
  cy.xpath(getInput('password')).type('password');
  cy.xpath(getInput('passwordRepeat')).type('password');
  gcy('sign-up-submit-button').click();
};

export const signUpAfter = (username: string) => {
  enableEmailVerification();
  enableRegistration();
  deleteUserSql(username);
};

export function checkAnonymousIdUnset() {
  cy.wrap(localStorage).invoke('getItem', 'anonymousUserId').should('be.null');
}

export function checkAnonymousIdSet() {
  cy.intercept('POST', '/v2/public/business-events/identify').as('identify');
  cy.wrap(localStorage)
    .invoke('getItem', 'anonymousUserId')
    .should('have.length', 36);
}

export function checkAnonymousUserIdentified() {
  cy.wait('@identify').its('response.statusCode').should('eq', 200);
}
