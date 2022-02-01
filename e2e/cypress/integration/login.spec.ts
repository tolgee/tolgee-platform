/// <reference types="cypress" />
import { HOST, PASSWORD, USERNAME } from '../common/constants';
import { getAnyContainingText } from '../common/xPath';
import {
  createUser,
  deleteAllEmails,
  getParsedResetPasswordEmail,
  login,
} from '../common/apiCalls/common';
import { assertMessage, getPopover } from '../common/shared';
import { loginWithFakeGithub } from '../common/login';

context('Login', () => {
  beforeEach(() => {
    cy.visit(HOST);
  });

  it('Will login', () => {
    cy.xpath('//input[@name="username"]')
      .type(USERNAME)
      .should('have.value', USERNAME);
    cy.xpath('//input[@name="password"]')
      .type(PASSWORD)
      .should('have.value', PASSWORD);
    cy.xpath("//button//*[text() = 'Login']").click().should('not.exist');
    cy.xpath("//*[@aria-controls='user-menu']").should('be.visible');
  });

  it('Will fail on invalid credentials', () => {
    cy.xpath('//input[@name="username"]')
      .type('aaaa')
      .should('have.value', 'aaaa');
    cy.xpath('//input[@name="password"]')
      .type(PASSWORD)
      .should('have.value', PASSWORD);
    cy.xpath("//button//*[text() = 'Login']").should('be.visible').click();
    cy.xpath(getAnyContainingText('Login')).should('be.visible');
    cy.contains('Invalid credentials').should('be.visible');
  });

  it('Will login with github', () => {
    loginWithFakeGithub();
  });

  it('Will logout', () => {
    login();
    cy.reload();
    cy.xpath("//*[@aria-controls='user-menu']").click();
    getPopover().contains('Logout').click();
    cy.contains('Login').should('be.visible');
  });

  it('will reset password', () => {
    deleteAllEmails();
    const username = 'test@testuser.com';
    createUser(username);
    cy.contains('Reset password').click();
    cy.xpath("//*[@name='email']").type(username);
    cy.contains('Send request').click();
    cy.contains(
      'Request successfully sent! If you are signed up using this e-mail,' +
        ' you will receive an e-mail with a link for password reset. Check your mail box.'
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
});
