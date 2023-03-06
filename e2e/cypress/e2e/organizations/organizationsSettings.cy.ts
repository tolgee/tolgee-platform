import { HOST } from '../../common/constants';
import 'cypress-file-upload';
import {
  assertMessage,
  clickGlobalSave,
  confirmHardMode,
  gcy,
  switchToOrganization,
} from '../../common/shared';
import { login } from '../../common/apiCalls/common';
import { organizationTestData } from '../../common/apiCalls/testData/testData';
import {
  permissionsMenuSelectAdvanced,
  permissionsMenuSelectRole,
} from '../../common/permissionsMenu';

describe('Organization Settings', () => {
  beforeEach(() => {
    login();
    organizationTestData.clean();
    organizationTestData.generate();
    visitProfile();
  });

  const newValues = {
    name: 'What a nice organization',
    addressPart: 'what-a-nice-organization',
    description: 'This is an nice updated value!',
  };

  it('modifies organization', () => {
    gcy('organization-name-field').within(() =>
      cy.get('input').clear().type(newValues.name)
    );
    gcy('organization-address-part-field').within(() =>
      cy.get('input').should('have.value', newValues.addressPart)
    );
    gcy('organization-description-field').within(() =>
      cy.get('input').clear().type(newValues.description)
    );
    clickGlobalSave();
    cy.contains('Organization settings updated').should('be.visible');
    cy.visit(`${HOST}/organizations/what-a-nice-organization/profile`);
    gcy('organization-name-field').within(() =>
      cy.get('input').should('have.value', newValues.name)
    );
    gcy('organization-description-field').within(() =>
      cy.get('input').should('have.value', newValues.description)
    );
  });

  it('changes member privileges', () => {
    gcy('organization-side-menu').contains('Member permissions').click();
    gcy('permissions-menu-button').click();
    permissionsMenuSelectRole('Translate', { confirm: true });
    visitMemberPrivileges();
    gcy('permissions-menu-button').contains('Translate');
  });

  it("member privileges change doesn't affect profile", () => {
    gcy('organization-side-menu').contains('Member permissions').click();
    gcy('permissions-menu-button').click();
    permissionsMenuSelectRole('Translate', { confirm: true });
    visitProfile();
    gcy('organization-name-field').within(() =>
      cy.get('input').should('have.value', 'Tolgee')
    );
    gcy('organization-address-part-field').within(() =>
      cy.get('input').should('have.value', 'tolgee')
    );
    gcy('organization-description-field').within(() =>
      cy.get('input').should('have.value', 'This is us')
    );
  });

  it('changes advanced permissions', () => {
    gcy('organization-side-menu').contains('Member permissions').click();
    gcy('permissions-menu-button').click();
    permissionsMenuSelectAdvanced(['keys.create', 'keys.edit'], {
      confirm: true,
    });
  });

  it('Gates cannot change Tolgee settings', () => {
    login('gates@microsoft.com');
    visitProfile();
    switchToOrganization('Tolgee');
    cy.gcy('global-user-menu-button').click();
    cy.gcy('user-menu-organization-settings')
      .contains('Organization settings')
      .click();
    cy.waitForDom();
    cy.gcy('global-form-save-button').should('be.disabled');
    cy.gcy('organization-profile-delete-button').should('be.disabled');
    cy.gcy('settings-menu-item')
      .contains('Organization profile')
      .should('be.visible');
    cy.gcy('settings-menu-item')
      .contains('Organization members')
      .should('not.exist');
    cy.gcy('settings-menu-item')
      .contains('Member permissions')
      .should('not.exist');
  });

  it('deletes organization', () => {
    gcy('organization-profile-delete-button').click();
    confirmHardMode();
    assertMessage('Organization deleted');
  });

  after(() => {
    organizationTestData.clean();
  });

  const visitProfile = () => {
    cy.visit(`${HOST}/organizations/tolgee/profile`);
  };

  const visitMemberPrivileges = () => {
    cy.visit(`${HOST}/organizations/tolgee/member-privileges`);
  };
});
