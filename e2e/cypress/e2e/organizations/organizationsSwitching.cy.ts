import { HOST } from '../../common/constants';
import 'cypress-file-upload';
import { login, setBypassSeatCountCheck } from '../../common/apiCalls/common';
import { organizationTestData } from '../../common/apiCalls/testData/testData';
import {
  gcy,
  switchToOrganization,
  switchToOrganizationWithSearch,
} from '../../common/shared';
import { organizationSettingsMenuItem } from '../../common/organizationSettingsMenu';

describe('Organization switching', () => {
  let organizationData: Record<string, { slug: string }>;

  beforeEach(() => {
    setBypassSeatCountCheck(true);
    login();
    organizationTestData.clean({
      timeout: 120000,
    });
    organizationTestData.generate().then((res) => {
      organizationData = res.body as any;
      visit();
    });
  });

  afterEach(() => {
    organizationTestData.clean();
    setBypassSeatCountCheck(false);
  });

  it('stores preferred organization on BE', () => {
    switchToOrganization('Microsoft');
    visit();
    cy.waitForDom();
    gcy('organization-switch').contains('Microsoft').should('be.visible');

    switchToOrganization('Facebook');
    assertOrganizationIsPreferred('Facebook');
  });

  function visitMembers() {
    const slug = organizationData['Tolgee'].slug;
    cy.visit(`${HOST}/organizations/${slug}/members`);
  }

  it('switches correctly when going directly to organization', () => {
    visitMembers();
    switchToOrganizationWithSearch('admin');
    gcy('organization-switch').contains('admin').should('be.visible');
    cy.go('back');
    assertOrganizationIsPreferred('Tolgee');
  });

  it('adopts the newly routed organization while staying inside organization settings', () => {
    visitMembers();
    switchToOrganization('Microsoft');
    cy.waitForDom();
    organizationSettingsMenuItem('profile').should('have.class', 'selected');
    assertOrganizationIsPreferred('Microsoft');
  });

  it('switches organization correctly from user menu', () => {
    gcy('global-user-menu-button').click();
    gcy('user-menu-organization-switch').click();
    gcy('switch-popover-item').contains('Microsoft').click();
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
