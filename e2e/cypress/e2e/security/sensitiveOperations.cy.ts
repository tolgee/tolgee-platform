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
    doPasswordProtectedOperation();

    gcy('sensitive-dialog-password-input').type('admin');
    gcy('sensitive-protection-dialog')
      .findDcy('global-form-save-button')
      .click();
    assertMessage('deleted');
  });

  it('Asks for OTP before operation', () => {
    doOtpProtectedOperation();
    getOtp().then((otp) => {
      gcy('sensitive-dialog-otp-input').type(otp);
    });
    gcy('sensitive-protection-dialog')
      .findDcy('global-form-save-button')
      .click();
    assertMessage('deleted');
  });

  it('Wrong password is not accepted', () => {
    doPasswordProtectedOperation();
    gcy('sensitive-dialog-password-input').type('Your mom is admin!');
    gcy('sensitive-protection-dialog')
      .findDcy('global-form-save-button')
      .click();
    cy.contains('Wrong current password entered').should('be.visible');
  });

  it('Wrong OTP is not accepted', () => {
    doOtpProtectedOperation();
    getOtp().then((otp) => {
      const wrongOtp = (parseInt(otp) - 1).toString().padStart(6, '0');
      gcy('sensitive-dialog-otp-input').type(wrongOtp);
    });
    gcy('sensitive-protection-dialog')
      .findDcy('global-form-save-button')
      .click();
    cy.contains('Invalid 2FA code').should('be.visible');
  });

  it('Asks for OTP before operation', () => {
    doOtpProtectedOperation();
    getOtp().then((otp) => {
      gcy('sensitive-dialog-otp-input').type(otp);
    });
    gcy('sensitive-protection-dialog')
      .findDcy('global-form-save-button')
      .click();
    assertMessage('deleted');
  });

  it('Can be cancelled for password', () => {
    doPasswordProtectedOperation();
    gcy('sensitive-protection-dialog')
      .findDcy('global-form-cancel-button')
      .click();
    gcy('sensitive-protection-dialog').should('not.exist');
    cy.contains('Project settings').should('be.visible');
  });

  it('Can be cancelled for OTP', () => {
    doOtpProtectedOperation();
    gcy('sensitive-protection-dialog')
      .findDcy('global-form-cancel-button')
      .click();
    gcy('sensitive-protection-dialog').should('not.exist');
    cy.contains('Project settings').should('be.visible');
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

  function doOtpProtectedOperation() {
    getOtp().then((otp) => {
      doSensitiveOperation(
        'pepa',
        data.pepasProjectId,
        data.pepaExpiredSuperJwt,
        otp
      );
    });
  }

  function doPasswordProtectedOperation() {
    doSensitiveOperation(
      'franta',
      data.frantasProjectId,
      data.frantaExpiredSuperJwt
    );
  }
});

function getOtp() {
  return sensitiveOperationProtectionTestData.getOtp().then((r) => r.body.otp);
}

function doSensitiveOperation(
  username: string,
  projectId: number,
  expiredJwt: string,
  otp: string = undefined
) {
  login(username, undefined, otp);
  cy.visit(`${HOST}/projects/${projectId}/manage/edit`);
  gcy('project-settings-delete-button')
    .click()
    .then(() => {
      window.localStorage.setItem('jwtToken', expiredJwt);
    });
  confirmHardMode();
}
