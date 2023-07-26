/// <reference types="cypress" />
import * as totp from 'totp-generator';
import { HOST, PASSWORD, USERNAME } from '../../common/constants';
import { getAnyContainingText } from '../../common/xPath';
import {
  createUser,
  deleteAllEmails,
  getParsedResetPasswordEmail,
  login,
  userDisableMfa,
  userEnableMfa,
} from '../../common/apiCalls/common';
import { assertMessage, getPopover } from '../../common/shared';
import {
  checkAnonymousIdSet,
  checkAnonymousIdUnset,
  checkAnonymousUserIdentified,
  loginViaForm,
  loginWithFakeGithub,
  loginWithFakeOAuth2,
} from '../../common/login';
import { waitForGlobalLoading } from '../../common/loading';

context('Login', () => {
  beforeEach(() => {
    cy.visit(HOST);
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

  it('login with github', () => {
    checkAnonymousIdSet();

    loginWithFakeGithub();

    checkAnonymousIdUnset();
    checkAnonymousUserIdentified();
  });
  it('login with oauth2', () => {
    loginWithFakeOAuth2();
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
    cy.contains('Reset password').click();
    cy.xpath("//*[@name='email']").type(username);
    cy.contains('Send request').click();
    cy.contains(
      'Request successfully sent! If you are signed up using this email,' +
        ' you will receive an email with a link for password reset. Check your mail box.'
    );
    getParsedResetPasswordEmail().then((r) => {
      cy.visit(r.resetLink);
    });
    const newPassword = 'new_password';
    cy.xpath("//*[@name='password']").type(newPassword);
    cy.xpath("//*[@name='passwordRepeat']").type(newPassword);
    cy.contains('Save new password').click();
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
