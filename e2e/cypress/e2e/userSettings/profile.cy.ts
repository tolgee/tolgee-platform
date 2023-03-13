import {
  createUser,
  deleteAllEmails,
  deleteUserSql,
  disableEmailVerification,
  enableEmailVerification,
  getParsedEmailVerification,
  login,
} from '../../common/apiCalls/common';
import { HOST } from '../../common/constants';
import { assertMessage } from '../../common/shared';

describe('User profile', () => {
  const INITIAL_EMAIL = 'honza@honza.com';
  const INITIAL_PASSWORD = 'honzaaaaaaaa';
  const EMAIL_VERIFICATION_TEXT =
    'When you change your email, new email will be set after its verification';
  const NEW_EMAIL = 'pavel@honza.com';

  function visit() {
    cy.visit(HOST + '/account/profile');
  }

  beforeEach(() => {
    enableEmailVerification();
    createUser(INITIAL_EMAIL, INITIAL_PASSWORD);
    login(INITIAL_EMAIL, INITIAL_PASSWORD);
    visit();
  });

  afterEach(() => {
    deleteUserSql(INITIAL_EMAIL);
    deleteUserSql(NEW_EMAIL);
    deleteAllEmails();
    enableEmailVerification();
  });

  it('email verification on user update works', () => {
    cy.get('form').findInputByName('email').clear().type(NEW_EMAIL);
    cy.contains(EMAIL_VERIFICATION_TEXT).should('be.visible');
    cy.xpath("//*[@name='currentPassword']").clear().type(INITIAL_PASSWORD);
    cy.gcy('global-form-save-button').click();
    cy.contains('Email waiting for verification: pavel@honza.com').should(
      'be.visible'
    );
    getParsedEmailVerification().then((v) => {
      cy.visit(v.verifyEmailLink);
      assertMessage('Email was verified');
      visit();
      cy.contains('Email waiting for verification: pavel@honza.com').should(
        'not.exist'
      );
      cy.get('form').findInputByName('email').should('have.value', NEW_EMAIL);
    });
  });

  it('works without email verification enabled', () => {
    disableEmailVerification();
    cy.reload();
    cy.get('form').findInputByName('email').clear().type(NEW_EMAIL);
    cy.contains(EMAIL_VERIFICATION_TEXT).should('not.exist');
    cy.xpath("//*[@name='currentPassword']").clear().type(INITIAL_PASSWORD);
    cy.gcy('global-form-save-button').click();
    cy.waitForDom();
    assertMessage('User data updated');
    cy.get('form').findInputByName('email').should('have.value', NEW_EMAIL);
  });

  it('will change user settings', () => {
    cy.visit(`${HOST}/account/profile`);
    cy.contains('User profile').should('be.visible');
    cy.xpath("//*[@name='name']").clear().type('New name');
    cy.contains('Save').click();
    cy.contains('User data updated').should('be.visible');
    cy.reload();
    cy.contains('User profile').should('be.visible');
    cy.xpath("//*[@name='name']").should('have.value', 'New name');
    cy.xpath("//*[@name='email']").should('have.value', 'honza@honza.com');
  });

  it('will fail when user exists', () => {
    createUser(NEW_EMAIL);
    cy.visit(`${HOST}/account/profile`);
    cy.contains('User profile').should('be.visible');
    cy.xpath("//*[@name='name']").clear().type('New name');
    cy.xpath("//*[@name='email']").clear().type(NEW_EMAIL);
    cy.xpath("//*[@name='currentPassword']").clear().type(INITIAL_PASSWORD);
    cy.contains('Save').click();
    cy.contains('User name already exists.').should('be.visible');
    cy.reload();
    cy.xpath("//*[@name='email']").should('have.value', INITIAL_EMAIL);
  });
});
