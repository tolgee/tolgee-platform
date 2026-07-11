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

describe('Organization Members', () => {
  let organizationData: Record<string, { slug: string }>;

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

  it('Can remove users managed by the organization', () => {
    gcy('global-paginated-list').within(() => {
      cy.contains('Lonely Developer')
        .closestDcy('organization-member-item')
        .findDcy('organization-members-remove-user-button')
        .click();
    });
    confirmStandard();
    assertMessage('User removed from organization');

    gcy('global-paginated-list').within(() => {
      cy.gcy('organization-member-item').contains('Cukrberg').should('exist');
    });

    gcy('global-paginated-list').within(() => {
      cy.gcy('organization-member-item')
        .contains('Lonely Developer')
        .should('not.exist');
    });
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
