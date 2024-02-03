import { HOST } from '../../common/constants';
import 'cypress-file-upload';
import {
  assertMessage,
  clickGlobalSave,
  confirmHardMode,
  gcy,
  switchToOrganization,
} from '../../common/shared';
import { login, setBypassSeatCountCheck } from '../../common/apiCalls/common';
import { organizationTestData } from '../../common/apiCalls/testData/testData';

describe('Organization Settings', () => {
  let organizationData: Record<string, { slug: string }>;

  beforeEach(() => {
    setBypassSeatCountCheck(true);
    login();
    organizationTestData.clean();
    organizationTestData.generate().then((res) => {
      organizationData = res.body as any;
      visit('Tolgee');
    });
  });

  afterEach(() => {
    setBypassSeatCountCheck(false);
  });

  const newValues = {
    name: 'What a nice organization',
    description: 'This is an nice updated value!',
  };

  it('modifies organization', () => {
    gcy('organization-name-field').within(() =>
      cy.get('input').clear().type(newValues.name)
    );
    gcy('organization-address-part-field').within(() => {
      cy.get('input').should('contain.value', 'what-a-nice-organization');
    });
    gcy('organization-description-field').within(() =>
      cy.get('input').clear().type(newValues.description)
    );
    clickGlobalSave();
    cy.contains('Organization settings updated').should('be.visible');

    cy.reload();

    cy.gcy('global-user-menu-button').click();
    cy.gcy('user-menu-organization-settings')
      .contains('Organization settings')
      .click();

    gcy('organization-name-field').within(() =>
      cy.get('input').should('have.value', newValues.name)
    );
    gcy('organization-description-field').within(() =>
      cy.get('input').should('have.value', newValues.description)
    );
  });

  it('Gates cannot change Tolgee settings', () => {
    login('gates@microsoft.com');
    visit('Tolgee');
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

  function visitSlug(slug: string) {
    cy.visit(`${HOST}/organizations/${slug}/profile`);
  }

  const visit = (name: string) => {
    cy.log(`Visiting ${name}`);
    const slug = organizationData[name].slug;
    visitSlug(slug);
  };
});
