/// <reference types="cypress" />
import * as totp from 'totp-generator';
import { HOST, PASSWORD, USERNAME } from '../../common/constants';
import { ssoOrganizationsLoginTestData } from '../../common/apiCalls/testData/testData';
import { getAnyContainingText } from '../../common/xPath';
import {
  createUser,
  deleteAllEmails,
  disableEmailVerification,
  getParsedResetPasswordEmail,
  login,
  logout,
  enableOrganizationsSsoProvider,
  disableOrganizationsSsoProvider,
  enableGlobalSsoProvider,
  disableGlobalSsoProvider,
  userDisableMfa,
  userEnableMfa,
  deleteUserSql,
} from '../../common/apiCalls/common';
import { assertMessage, getPopover } from '../../common/shared';
import {
  checkAnonymousIdSet,
  checkAnonymousIdUnset,
  checkAnonymousUserIdentified,
  loginViaForm,
  loginWithFakeGithub,
  loginWithFakeGoogle,
  loginWithFakeOAuth2,
  loginWithFakeSso,
} from '../../common/login';
import { waitForGlobalLoading } from '../../common/loading';

const TEST_USERNAME = 'johndoe@doe.com';
const TEST_USERNAME_SSO = 'johndoe@domain.com';

context('Login', () => {
  beforeEach(() => {
    cy.visit(HOST);
  });

  it('can change language', () => {
    cy.gcy('global-language-menu').should('be.visible');
  });

  it('login', () => {
    checkAnonymousIdSet();

    loginViaForm();

    checkAnonymousIdUnset();
    checkAnonymousUserIdentified();

    cy.gcy('login-button').should('not.exist');
    cy.xpath("//*[@aria-controls='user-menu']").should('be.visible');
  });

  it('fails on invalid credentials', () => {
    cy.xpath('//input[@name="username"]')
      .type('aaaa')
      .should('have.value', 'aaaa');
    cy.xpath('//input[@name="password"]')
      .type(PASSWORD)
      .should('have.value', PASSWORD);
    cy.gcy('login-button').should('be.visible').click();
    cy.xpath(getAnyContainingText('Login')).should('be.visible');
    cy.contains('Invalid credentials').should('be.visible');
  });

  it('logout', () => {
    login();
    cy.reload();
    checkAnonymousIdUnset();
    cy.xpath("//*[@aria-controls='user-menu']").click();
    getPopover().contains('Logout').click();
    cy.gcy('login-button').should('be.visible');
    checkAnonymousIdSet();
  });

  it('resets password', () => {
    deleteAllEmails();
    const username = 'test@testuser.com';
    createUser(username);
    cy.contains('Forgot your password?').click();
    cy.xpath("//*[@name='email']").type(username);
    cy.contains('Send link').click();
    cy.contains(
      'Request successfully sent! If you are signed up using this email,' +
        ' you will receive an email with a link for password reset. Check your mail box.'
    );
    getParsedResetPasswordEmail().then((r) => {
      cy.visit(r.resetLink);
    });
    const newPassword = 'new_very.strong.password';
    cy.xpath("//*[@name='password']").type(newPassword);
    cy.contains('Set new password').click();
    assertMessage('Password successfully reset');
    login(username, newPassword);
  });

  context('MFA', () => {
    const TOTP_KEY_B32 = 'meowmeowmeowmeow';
    const TOTP_KEY = [
      0x61, 0x1d, 0x66, 0x11, 0xd6, 0x61, 0x1d, 0x66, 0x11, 0xd6,
    ];

    before(() => {
      userEnableMfa(USERNAME, TOTP_KEY, ['meow-meow']);
    });

    after(() => {
      userDisableMfa(USERNAME);
    });

    beforeEach(() => {
      cy.visit(HOST);
    });

    it('asks for MFA', () => {
      cy.xpath('//input[@name="username"]').type(USERNAME);
      cy.xpath('//input[@name="password"]').type(PASSWORD);
      cy.gcy('login-button').click();
      cy.xpath('//input[@name="otp"]').should('exist');
    });

    it('accepts valid TOTP code', { retries: 3 }, () => {
      cy.xpath('//input[@name="username"]').type(USERNAME);
      cy.xpath('//input[@name="password"]').type(PASSWORD);
      cy.gcy('login-button').click();

      cy.xpath('//input[@name="otp"]').type(totp(TOTP_KEY_B32));
      cy.gcy('login-button').click();
      waitForGlobalLoading();
      cy.gcy('login-button').should('not.exist');
      cy.xpath("//*[@aria-controls='user-menu']").should('be.visible');
    });

    it('accepts recovery code', () => {
      cy.xpath('//input[@name="username"]').type(USERNAME);
      cy.xpath('//input[@name="password"]').type(PASSWORD);
      cy.gcy('login-button').click();

      cy.xpath('//input[@name="otp"]').type('meow-meow');
      cy.gcy('login-button').click();
      waitForGlobalLoading();
      cy.gcy('login-button').should('not.exist');
      cy.xpath("//*[@aria-controls='user-menu']").should('be.visible');
    });
  });
});

context('Login third party', () => {
  beforeEach(() => {
    deleteUserSql(TEST_USERNAME);
    disableEmailVerification();
    cy.visit(HOST);
  });

  afterEach(() => {
    deleteUserSql(TEST_USERNAME);
  });

  it('login with github', () => {
    checkAnonymousIdSet();

    loginWithFakeGithub(TEST_USERNAME);
    cy.contains('Projects').should('be.visible');

    checkAnonymousIdUnset();
    checkAnonymousUserIdentified();
  });
  it('login with google', () => {
    checkAnonymousIdSet();

    loginWithFakeGoogle(TEST_USERNAME);
    cy.contains('Projects').should('be.visible');

    checkAnonymousIdUnset();
    checkAnonymousUserIdentified();
  });
  it('login with oauth2', { retries: { runMode: 5 } }, () => {
    loginWithFakeOAuth2(TEST_USERNAME);
    cy.contains('Projects').should('be.visible');
  });
});

context('SSO Organizations Login', () => {
  beforeEach(() => {
    deleteUserSql(TEST_USERNAME_SSO);
    disableEmailVerification();
    ssoOrganizationsLoginTestData.clean();
    ssoOrganizationsLoginTestData.generate();
    enableOrganizationsSsoProvider();

    cy.visit(HOST);
  });

  it('login with organizations sso', { retries: { runMode: 5 } }, () => {
    cy.contains('SSO login').click();
    cy.xpath("//*[@name='domain']").type('domain.com');
    loginWithFakeSso(TEST_USERNAME_SSO);
    cy.contains('Projects').should('be.visible');
  });

  afterEach(() => {
    logout();
    disableOrganizationsSsoProvider();
    ssoOrganizationsLoginTestData.clean();
    deleteUserSql(TEST_USERNAME_SSO);
  });
});

context('SSO Global Login', () => {
  beforeEach(() => {
    deleteUserSql(TEST_USERNAME_SSO);
    disableEmailVerification();
    enableGlobalSsoProvider();
    cy.visit(HOST);
  });

  it('login with global sso', { retries: { runMode: 5 } }, () => {
    loginWithFakeSso(TEST_USERNAME_SSO);
    cy.contains('Projects').should('be.visible');
  });

  afterEach(() => {
    logout();
    disableGlobalSsoProvider();
    deleteUserSql(TEST_USERNAME_SSO);
  });
});
