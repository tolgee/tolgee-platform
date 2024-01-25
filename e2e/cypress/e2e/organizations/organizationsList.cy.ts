import { HOST } from '../../common/constants';
import 'cypress-file-upload';
import {
  assertMessage,
  clickGlobalSave,
  gcy,
  switchToOrganization,
} from '../../common/shared';
import { organizationTestData } from '../../common/apiCalls/testData/testData';
import { login, setBypassSeatCountCheck } from '../../common/apiCalls/common';

describe('Organization List', () => {
  beforeEach(() => {
    setBypassSeatCountCheck(true);
    organizationTestData.clean();
    organizationTestData
      .generate()
      .then(() => login())
      .then(() => visit());
  });

  afterEach(() => {
    organizationTestData.clean();
  });

  afterEach(() => {
    setBypassSeatCountCheck(false);
  });

  it('creates organization', () => {
    goToNewOrganizationForm();
    gcy('organization-name-field').within(() =>
      cy.get('input').type('What a nice organization')
    );
    gcy('organization-address-part-field').within(() =>
      cy.get('input').should('contain.value', 'what-a-nice-organization')
    );
    gcy('organization-description-field').within(() =>
      cy.get('input').type('Very nice organization! Which is nice to create!')
    );
    clickGlobalSave();
    gcy('organization-switch').contains('What a nice organization');
    assertMessage('Organization created').should('be.visible');
    gcy('organization-switch')
      .contains('What a nice organization')
      .should('be.visible');
  });

  it('creates organization without description', () => {
    goToNewOrganizationForm();
    gcy('organization-name-field').within(() =>
      cy.get('input').type('What a nice organization')
    );
    gcy('organization-address-part-field').within(() =>
      cy.get('input').should('contain.value', 'what-a-nice-organization')
    );
    clickGlobalSave();
    assertMessage('Organization created');
  });

  it('validates creation fields', { retries: { runMode: 3 } }, () => {
    goToNewOrganizationForm();
    gcy('organization-name-field').within(() => {
      cy.get('input').type('aaa').clear();
    });

    gcy('organization-address-part-field').click();

    gcy('organization-name-field').contains('This field is required');

    gcy('organization-name-field').within(() => {
      cy.get('input').type(
        'This is too too too too too too too too too too too too too too too too too too long'
      );
      cy.contains('This field can contain at maximum 50 characters');
    });

    clickGlobalSave();
    cy.contains('Organization created').should('not.exist');

    gcy('organization-description-field').click();

    gcy('organization-address-part-field').contains('This field is required');
  });

  describe('list', () => {
    it('contains created data', () => {
      cy.waitForDom();
      cy.gcy('organization-switch').click();
      cy.gcy('organization-switch-item')
        .contains('Facebook')
        .should('be.visible');

      cy.gcy('organization-switch-item')
        .contains('ZZZ Cool company 10')
        .should('be.visible');
      cy.contains('ZZZ Cool company 14').scrollIntoView().should('be.visible');
    });

    it('admin leaves Microsoft', { scrollBehavior: 'center' }, () => {
      switchToOrganization('Microsoft');
      cy.gcy('global-user-menu-button').click();
      cy.gcy('user-menu-organization-settings')
        .contains('Organization settings')
        .click();

      gcy('organization-profile-leave-button').click();

      gcy('global-confirmation-confirm').click();
      assertMessage('Organization left');
    });

    it('admin cannot leave Techfides', { scrollBehavior: 'center' }, () => {
      switchToOrganization('Techfides');
      cy.gcy('global-user-menu-button').click();
      cy.gcy('user-menu-organization-settings')
        .contains('Organization settings')
        .click();

      gcy('organization-profile-leave-button').click();

      gcy('global-confirmation-confirm').click();
      assertMessage('Organization has no other owner.');
    });

    it('admin can change Tolgee settings', { scrollBehavior: 'center' }, () => {
      switchToOrganization('Tolgee');
      cy.gcy('global-user-menu-button').click();
      cy.gcy('user-menu-organization-settings')
        .contains('Organization settings')
        .click();

      cy.gcy('global-form-save-button').should('not.be.disabled');
      cy.gcy('organization-profile-delete-button').should('not.be.disabled');
    });
  });

  after(() => {
    organizationTestData.clean();
  });

  const goToNewOrganizationForm = () => {
    gcy('organization-switch').click();
    gcy('organization-switch-new').click();
  };

  const visit = () => {
    cy.visit(`${HOST}/projects`);
  };
});
