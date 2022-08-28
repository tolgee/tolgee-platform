import { createUser, deleteUser, login } from '../../common/apiCalls/common';
import { HOST } from '../../common/constants';
import { assertMessage } from '../../common/shared';

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

    login(INITIAL_EMAIL, superNewPassword);
    cy.reload();
    cy.contains('User profile');
  });
});
