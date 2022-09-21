/// <reference types="cypress" />
import { HOST } from '../../common/constants';
import { sensitiveOperationProtectionTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { assertMessage, confirmHardMode, gcy } from '../../common/shared';
import { loginWithFakeGithub } from '../../common/login';

context('Sensitive operations', () => {
  let data: {
    frantasProjectId: number;
    pepasProjectId: number;
    frantaExpiredSuperJwt: string;
    pepaExpiredSuperJwt: string;
  };

  beforeEach(() => {
    sensitiveOperationProtectionTestData.clean();
    sensitiveOperationProtectionTestData.generate().then((r) => {
      data = r.body;
    });
  });

  afterEach(() => {
    sensitiveOperationProtectionTestData.clean();
  });

  it('Asks for password before operation', () => {
    login('franta');
    cy.visit(`${HOST}/projects/${data.frantasProjectId}/manage/edit`);
    gcy('project-settings-delete-button')
      .click()
      .then(() => {
        window.localStorage.setItem('jwtToken', data.frantaExpiredSuperJwt);
      });
    confirmHardMode();
    gcy('sensitive-dialog-password-input').type('admin');
    gcy('sensitive-protection-dialog')
      .findDcy('global-form-save-button')
      .click();
    assertMessage('deleted');
  });

  it('Asks for OTP before operation', () => {
    getOtp().then((otp) => {
      login('pepa', undefined, otp);
    });
    cy.visit(`${HOST}/projects/${data.pepasProjectId}/manage/edit`);
    gcy('project-settings-delete-button')
      .click()
      .then(() => {
        window.localStorage.setItem('jwtToken', data.pepaExpiredSuperJwt);
      });
    confirmHardMode();
    getOtp().then((otp) => {
      gcy('sensitive-dialog-otp-input').type(otp);
    });
    gcy('sensitive-protection-dialog')
      .findDcy('global-form-save-button')
      .click();
    assertMessage('deleted');
  });

  it("Doesn't ask when third party", () => {
    cy.visit(HOST);
    loginWithFakeGithub();
    cy.visit(HOST + '/account/profile');
    gcy('delete-user-button').click();
    confirmHardMode();
    assertMessage('deleted');
    gcy('login-button').should('be.visible');
  });
});

function getOtp() {
  return sensitiveOperationProtectionTestData.getOtp().then((r) => r.body.otp);
}
