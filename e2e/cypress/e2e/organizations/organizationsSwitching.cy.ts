import { HOST } from '../../common/constants';
import 'cypress-file-upload';
import { login } from '../../common/apiCalls/common';
import { organizationTestData } from '../../common/apiCalls/testData/testData';
import {
  gcy,
  switchToOrganization,
  switchToOrganizationWithSearch,
} from '../../common/shared';

describe('Organization switching', () => {
  beforeEach(() => {
    login();
    organizationTestData.clean();
    organizationTestData.generate();
    visit();
  });

  afterEach(() => {
    organizationTestData.clean();
  });

  it('stores preferred organization on BE', () => {
    switchToOrganization('Microsoft');
    visit();
    cy.waitForDom();
    gcy('organization-switch').contains('Microsoft').should('be.visible');

    switchToOrganization('Facebook');
    assertOrganizationIsPreferred('Facebook');
  });

  it('switches correctly when going directly to organization', () => {
    cy.visit(`${HOST}/organizations/tolgee/members`);
    switchToOrganizationWithSearch('admin');
    gcy('organization-switch').contains('admin').should('be.visible');
    cy.go('back');
    assertOrganizationIsPreferred('Tolgee');
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
    assertOrganizationIsPreferred('Microsoft');
  });

  const visit = () => {
    cy.visit(`${HOST}/projects`);
  };

  const assertOrganizationIsPreferred = (organization: string) => {
    cy.waitForDom();
    gcy('organization-switch').contains(organization).should('be.visible');

    visit();

    cy.waitForDom();
    gcy('organization-switch').contains(organization).should('be.visible');
  };
});
