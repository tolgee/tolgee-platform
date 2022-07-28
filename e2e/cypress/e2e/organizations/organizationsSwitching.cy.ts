import { HOST } from '../../common/constants';
import 'cypress-file-upload';
import { login } from '../../common/apiCalls/common';
import { organizationTestData } from '../../common/apiCalls/testData/testData';
import { gcy, switchToOrganization } from '../../common/shared';

describe('Organization Invitations', () => {
  beforeEach(() => {
    login();
    organizationTestData.clean();
    organizationTestData.generate();
    visit();
  });

  afterEach(() => {
    organizationTestData.clean();
  });

  it('stores preffered organization on BE', () => {
    switchToOrganization('Microsoft');
    visit();
    cy.waitForDom();
    gcy('organization-switch').contains('Microsoft').should('be.visible');

    switchToOrganization('Facebook');
    ensureOrganizationIsPreffered('Facebook');
  });

  it('switches correctly when going directly to organization', () => {
    cy.visit(`${HOST}/organizations/tolgee/members`);
    switchToOrganization('admin');
    gcy('organization-switch').contains('admin').should('be.visible');
    cy.go('back');

    ensureOrganizationIsPreffered('Tolgee');
  });

  it('switches correctly when in organization settings', () => {
    cy.visit(`${HOST}/organizations/tolgee/members`);
    switchToOrganization('Microsoft');
    cy.waitForDom();
    gcy('settings-menu-item')
      .contains('Organization profile')
      .should('have.class', 'selected');
  });

  it('switches organization correctly from user menu', () => {
    gcy('global-user-menu-button').click();
    gcy('user-menu-organization-switch').click();
    gcy('organization-switch-item').contains('Microsoft').click();

    ensureOrganizationIsPreffered('Microsoft');
  });

  const visit = () => {
    cy.visit(`${HOST}/projects`);
  };

  const ensureOrganizationIsPreffered = (organization: string) => {
    cy.waitForDom();
    gcy('organization-switch').contains(organization).should('be.visible');

    visit();

    cy.waitForDom();
    gcy('organization-switch').contains(organization).should('be.visible');
  };
});
