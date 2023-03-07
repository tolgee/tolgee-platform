/// <reference types="cypress" />

import { avatarTestData } from '../common/apiCalls/testData/testData';
import { login } from '../common/apiCalls/common';
import { gcy, gcyChain, visitProjectSettings } from '../common/shared';
import 'cypress-file-upload';
import { HOST } from '../common/constants';

describe('Avatars', () => {
  let projectId: number;
  let organizationSlug: string;

  beforeEach(() => {
    avatarTestData.clean();
    avatarTestData.generate().then((res) => {
      projectId = res.body['projectId'];
      organizationSlug = res.body['organizationSlug'];
    });
    login('franta');
  });

  afterEach(() => {
    avatarTestData.clean();
  });

  describe('Project avatar', () => {
    beforeEach(() => {
      visitProjectSettings(projectId);
    });

    it('Has uploaded avatar', () => {
      const projectUploadedAvatar = getProjectSettingsUploadedAvatarImage();
      validateUploadedAvatar(projectUploadedAvatar);
    });

    it('Clears avatar', () => {
      clearUploadedAvatar();
      getProjectSettingsUploadedAvatarImage().should('not.exist');
      getProjectSettingsGeneratedAvatar().should('be.visible');
    });

    it('Cancels upload dialog', () => {
      selectFile();
      gcy('global-confirmation-cancel').click();
      gcy('global-confirmation-cancel').should('not.exist');
    });

    it('Upload dialog has cropper', () => {
      selectFile();
      cy.get('.cropper-face.cropper-move').should('be.visible');
    });

    it('Uploads avatar', () => {
      clearUploadedAvatar();
      selectFile();
      gcy('global-confirmation-confirm').click();
      waitForConfirmationNotExists();
      const projectUploadedAvatar = getProjectSettingsUploadedAvatarImage();
      validateUploadedAvatar(projectUploadedAvatar);
    });
  });

  describe('User avatar', () => {
    beforeEach(() => {
      cy.visit(HOST + '/account/profile');
    });

    it('shows the avatar in the user menu', () => {
      const userMenuAvatar = gcyChain(
        'global-user-menu-button',
        'avatar-image'
      );
      validateUploadedAvatar(userMenuAvatar);
    });

    it('Uploads avatar', () => {
      clearUploadedAvatar();
      selectFile();
      gcy('global-confirmation-confirm').click();
      waitForConfirmationNotExists();
      const projectUploadedAvatar = getUserProfileUploadedAvatar();
      validateUploadedAvatar(projectUploadedAvatar);
    });
  });

  describe('Organization avatar', () => {
    beforeEach(() => {
      cy.visit(`${HOST}/organizations/${organizationSlug}/profile`);
    });

    it('Uploads organization avatar', () => {
      clearUploadedAvatar();
      selectFile();
      gcy('global-confirmation-confirm').click();
      waitForConfirmationNotExists();
      const projectUploadedAvatar = getOrganizationProfileUploadedAvatar();
      validateUploadedAvatar(projectUploadedAvatar);
    });
  });
});

function getOrganizationProfileUploadedAvatar() {
  return gcyChain('organization-profile', 'avatar-image');
}

function getUserProfileUploadedAvatar() {
  return gcyChain('user-profile', 'avatar-image');
}

function getProjectSettingsUploadedAvatarImage() {
  return gcyChain('project-settings', 'avatar-image');
}

function getProjectSettingsGeneratedAvatar() {
  return gcyChain('project-settings', 'auto-avatar-img');
}

function validateUploadedAvatar(
  projectUploadedAvatar: Cypress.Chainable<JQuery<HTMLElement>>
) {
  projectUploadedAvatar
    .should('be.visible')
    .and(($img) => expect(($img[0] as any).naturalWidth).to.be.greaterThan(0))
    .invoke('attr', 'src')
    .should('contain', '.png');
}

function selectFile() {
  cy.waitForDom();
  gcy('avatar-menu-open-button').click();
  gcy('avatar-upload-button').click();
  gcy('avatar-upload-file-input').attachFile('avatars/pepi.jpg');
}

function clearUploadedAvatar() {
  gcy('avatar-menu-open-button').click();
  gcy('avatar-remove-button').click();
}

function waitForConfirmationNotExists() {
  gcy('global-confirmation-confirm', {
    timeout: 10000,
  }).should('not.exist');
}
