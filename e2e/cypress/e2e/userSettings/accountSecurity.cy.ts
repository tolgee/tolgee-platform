import * as totp from 'totp-generator';
import {
  createUser,
  deleteUser,
  login,
  userEnableMfa,
} from '../../common/apiCalls/common';
import { HOST } from '../../common/constants';
import { assertMessage } from '../../common/shared';
import { waitForGlobalLoading } from '../../common/loading';

describe('User profile', () => {
  const INITIAL_EMAIL = 'honza@honza.com';
  const INITIAL_PASSWORD = 'honzaaaaaaaa';

  beforeEach(() => {
    createUser(INITIAL_EMAIL, INITIAL_PASSWORD);
    login(INITIAL_EMAIL, INITIAL_PASSWORD);
    cy.visit(HOST + '/account/security');
  });

  afterEach(() => {
    deleteUser(INITIAL_EMAIL);
  });

  it('changes password', () => {
    const superNewPassword = 'super_new_password';
    cy.xpath("//*[@name='currentPassword']").clear().type(INITIAL_PASSWORD);
    cy.xpath("//*[@name='password']").clear().type(superNewPassword);
    cy.xpath("//*[@name='passwordRepeat']").clear().type(superNewPassword);
    cy.contains('Save').click();
    assertMessage('updated');

    cy.xpath("//*[@name='currentPassword']").should('not.have.value');
    cy.xpath("//*[@name='password']").should('not.have.value');
    cy.xpath("//*[@name='passwordRepeat']").should('not.have.value');

    // Ensure we're still logged in
    cy.reload();
    waitForGlobalLoading();
    cy.location().its('pathname').should('not.eq', '/login');

    // Verify password change applied
    login(INITIAL_EMAIL, superNewPassword);
    cy.reload();
    cy.contains('User profile');
  });

  it('enables mfa', () => {
    cy.gcy('mfa-enable-button').click();
    cy.gcy('mfa-enable-dialog-totp-key')
      .invoke('text')
      .then((key) => {
        cy.gcy('mfa-enable-dialog')
          .find('[name="otp"]')
          .clear()
          .type(totp(key.replace(/ /g, '')));
        cy.gcy('mfa-enable-dialog')
          .find('[name="password"]')
          .clear()
          .type(INITIAL_PASSWORD);

        cy.gcy('mfa-enable-dialog').findDcy('global-form-save-button').click();

        assertMessage('enabled');
        cy.gcy('mfa-recovery-codes-dialog').should('be.visible');
        cy.gcy('mfa-enable-dialog').should('not.exist');

        // Ensure we're still logged in
        cy.reload();
        waitForGlobalLoading();
        cy.location().its('pathname').should('not.eq', '/login');
      });
  });

  it('disables mfa', () => {
    userEnableMfa(INITIAL_EMAIL, [0]);
    cy.reload();

    cy.gcy('mfa-disable-button').click();
    cy.gcy('mfa-disable-dialog')
      .find('[name="password"]')
      .clear()
      .type(INITIAL_PASSWORD);
    cy.gcy('mfa-disable-dialog').findDcy('global-form-save-button').click();

    assertMessage('disabled');
    cy.gcy('mfa-disable-dialog').should('not.exist');

    // Ensure we're still logged in
    cy.reload();
    waitForGlobalLoading();
    cy.location().its('pathname').should('not.eq', '/login');
  });
});
