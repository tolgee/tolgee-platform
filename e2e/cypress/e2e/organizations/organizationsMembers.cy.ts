import { HOST } from '../../common/constants';
import 'cypress-file-upload';
import {
  assertMessage,
  confirmStandard,
  gcy,
  goToPage,
} from '../../common/shared';
import { organizationTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { E2OrganizationMembersView } from '../../compounds/organizationMembers/E2OrganizationMembersView';

const MANAGED_MEMBER = 'LonelyDev@tolgee.io';
const OTHER_OWNER = 'evan@netsuite.com';
const NON_MANAGED_MEMBER = 'cukrberg@facebook.com';
const NON_OWNER_MEMBER = 'gates@microsoft.com';

type OrganizationData = Record<string, { slug: string }>;

const membersView = new E2OrganizationMembersView();

const generateOrganizations = () => {
  organizationTestData.clean();
  return organizationTestData
    .generate()
    .then((res) => res.body as OrganizationData);
};

const visitMembers = (organizationData: OrganizationData, name: string) => {
  cy.visit(`${HOST}/organizations/${organizationData[name].slug}/members`);
};

describe('Organization Members', () => {
  beforeEach(() => {
    login();
    generateOrganizations().then((data) => visitMembers(data, 'Tolgee'));
  });

  afterEach(() => {
    organizationTestData.clean();
  });

  it('contains organization users', () => {
    membersView.getMfaBadge(OTHER_OWNER).should('be.visible');
    gcy('global-paginated-list').within(() => {
      cy.contains('Cukrberg')
        .closestDcy('organization-member-item')
        .contains('cukrberg@facebook.com')
        .should('be.visible');
      cy.contains('admin');
      cy.contains('Goldberg');
      cy.contains('Bill Gates');
    });
  });

  it(
    'May change role to member to other owner',
    { retries: { runMode: 3 } },
    () => {
      setGoldbergMember();
    }
  );

  it('Can remove other users', () => {
    membersView.getRemoveButton(OTHER_OWNER).click();
    confirmStandard();
    assertMessage('User removed from organization');
    membersView.getRemoveButton(NON_MANAGED_MEMBER).click();
    confirmStandard();
    assertMessage('User removed from organization');
  });

  it('Can disable and re-enable a user managed by the organization', () => {
    membersView.getDisableButton(MANAGED_MEMBER).should('exist');
    membersView.getRemoveButton(MANAGED_MEMBER).should('not.exist');
    membersView.disableMember(MANAGED_MEMBER);
    assertMessage('User disabled');

    membersView.getDisabledLabel(MANAGED_MEMBER).should('exist');
    membersView.enableMember(MANAGED_MEMBER);
    assertMessage('User re-enabled');

    membersView.getDisabledLabel(MANAGED_MEMBER).should('not.exist');
    membersView.getDisableButton(MANAGED_MEMBER).should('exist');
  });

  it('Shows the remove button (not disable/enable) for non-managed members', () => {
    membersView.getRemoveButton(NON_MANAGED_MEMBER).should('exist');
    membersView.getDisableButton(NON_MANAGED_MEMBER).should('not.exist');
    membersView.getEnableButton(NON_MANAGED_MEMBER).should('not.exist');
  });

  it('Can leave', () => {
    leaveOrganization();
    assertMessage('Organization left');
  });

  it('Cannot leave when single owner', () => {
    setGoldbergMember();
    leaveOrganization();
    assertMessage('Organization has no other owner.');
  });

  it('Can search', () => {
    cy.gcy('global-paginated-list').within(() => {
      cy.contains('Cukrberg').should('exist');
    });

    cy.gcy('global-list-search').within(() => {
      cy.get('input').type('Bill');
    });

    cy.gcy('global-paginated-list').within(() => {
      cy.gcy('organization-member-item')
        .contains('Cukrberg')
        .should('not.exist');
      cy.gcy('organization-member-item')
        .contains('Bill Gates')
        .should('be.visible');
    });
  });

  const setGoldbergMember = () => {
    gcy('global-paginated-list').within(() => {
      cy.contains('Goldberg')
        .closestDcy('organization-member-item')
        .findDcy('organization-role-menu-button')
        .click();
    });
    cy.gcy('organization-role-menu')
      .filter(':visible')
      .contains('MEMBER')
      .click();

    confirmStandard();
    assertMessage('Organization role changed');
  };

  function leaveOrganization() {
    cy.gcy('global-paginated-list').within(() => {
      cy.contains('admin')
        .closestDcy('organization-member-item')
        .findDcy('organization-member-leave-button')
        .click();
    });
    confirmStandard();
  }
});

describe('Organization Members pagination', () => {
  beforeEach(() => {
    login();
    generateOrganizations().then((data) => visitMembers(data, 'Facebook'));
  });

  afterEach(() => {
    organizationTestData.clean();
  });

  it('Paginates', () => {
    gcy('global-paginated-list').contains('Cukrberg').should('be.visible');
    gcy('global-paginated-list')
      .contains('owner@zzzcool16.com')
      .should('be.visible');
    goToPage(2);
    gcy('global-paginated-list')
      .contains('owner@zzzcool2.com')
      .should('be.visible');
  });
});

describe('Organization Members as a member', () => {
  beforeEach(() => {
    generateOrganizations().then((data) => {
      login(NON_OWNER_MEMBER);
      membersView.spyOnInvitationsRequest();
      visitMembers(data, 'Tolgee');
    });
  });

  afterEach(() => {
    organizationTestData.clean();
  });

  it('sees the members but cannot manage them', () => {
    membersView.getMember(OTHER_OWNER).should('be.visible');
    membersView.getMember(NON_MANAGED_MEMBER).should('be.visible');
    membersView.getMember(MANAGED_MEMBER).should('be.visible');
    membersView.getInviteButton().should('not.exist');
    membersView.getRoleMenuButton(OTHER_OWNER).should('be.disabled');
    membersView.getRemoveButton(NON_MANAGED_MEMBER).should('not.exist');
    membersView.getDisableButton(MANAGED_MEMBER).should('not.exist');
    membersView.getMfaBadge(OTHER_OWNER).should('not.exist');
    membersView.assertInvitationsNotRequested();
  });
});
