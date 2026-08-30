import { HOST } from '../../common/constants';
import 'cypress-file-upload';
import {
  assertMessage,
  confirmStandard,
  gcy,
  goToPage,
} from '../../common/shared';
import { organizationTestData } from '../../common/apiCalls/testData/testData';
import { login, setBypassSeatCountCheck } from '../../common/apiCalls/common';
import { E2OrganizationMembersView } from '../../compounds/organizationMembers/E2OrganizationMembersView';

const MANAGED_MEMBER = 'LonelyDev@tolgee.io';
const NON_MANAGED_MEMBER = 'evan@netsuite.com';

describe('Organization Members', () => {
  let organizationData: Record<string, { slug: string }>;
  const membersView = new E2OrganizationMembersView();

  beforeEach(() => {
    setBypassSeatCountCheck(true);
    login();
    organizationTestData.clean();
    organizationTestData
      .generate()
      .then((res) => {
        return (organizationData = res.body as any);
      })
      .then(() => {
        visit('Tolgee');
      });
  });

  afterEach(() => {
    organizationTestData.clean();
  });

  afterEach(() => {
    setBypassSeatCountCheck(false);
  });

  it('contains organization users', () => {
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
    gcy('global-paginated-list').within(() => {
      cy.contains('Goldberg')
        .closestDcy('organization-member-item')
        .findDcy('organization-members-remove-user-button')
        .click();
    });
    confirmStandard();
    assertMessage('User removed from organization');
    gcy('global-paginated-list').within(() => {
      cy.contains('Cukrberg')
        .closestDcy('organization-member-item')
        .findDcy('organization-members-remove-user-button')
        .click();
    });
    confirmStandard();
    assertMessage('User removed from organization');
  });

  it('Can disable and re-enable a user managed by the organization', () => {
    membersView
      .getMember(MANAGED_MEMBER)
      .findDcy('organization-members-remove-user-button')
      .should('not.exist');
    membersView.disableMember(MANAGED_MEMBER);
    assertMessage('User disabled');

    membersView
      .getMember(MANAGED_MEMBER)
      .findDcy('organization-member-disabled-label')
      .should('exist');
    membersView.enableMember(MANAGED_MEMBER);
    assertMessage('User re-enabled');

    membersView
      .getMember(MANAGED_MEMBER)
      .findDcy('organization-member-disabled-label')
      .should('not.exist');
    membersView
      .getMember(MANAGED_MEMBER)
      .findDcy('organization-members-disable-user-button')
      .should('exist');
  });

  it('Shows the remove button (not disable/enable) for non-managed members', () => {
    membersView
      .getMember(NON_MANAGED_MEMBER)
      .findDcy('organization-members-remove-user-button')
      .should('exist');
    membersView
      .getMember(NON_MANAGED_MEMBER)
      .findDcy('organization-members-disable-user-button')
      .should('not.exist');
    membersView
      .getMember(NON_MANAGED_MEMBER)
      .findDcy('organization-members-enable-user-button')
      .should('not.exist');
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

  it('Paginates', () => {
    visit('Facebook');
    gcy('global-paginated-list').contains('Cukrberg').should('be.visible');
    gcy('global-paginated-list')
      .contains('owner@zzzcool16.com')
      .should('be.visible');
    goToPage(2);
    gcy('global-paginated-list')
      .contains('owner@zzzcool2.com')
      .should('be.visible');
  });

  const visit = (name: string) => {
    const slug = organizationData[name].slug;
    cy.visit(`${HOST}/organizations/${slug}/members`);
  };

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
