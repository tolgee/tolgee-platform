import { HOST } from '../../common/constants';
import 'cypress-file-upload';
import {
  assertMessage,
  assertSwitchedToOrganization,
  gcy,
} from '../../common/shared';
import {
  deleteAllEmails,
  getParsedEmailInvitationLink,
  login,
  logout,
  setBypassSeatCountCheck,
} from '../../common/apiCalls/common';
import { organizationTestData } from '../../common/apiCalls/testData/testData';
import { waitForGlobalLoading } from '../../common/loading';

describe('Organization Invitations', () => {
  let organizationData: Record<string, { slug: string }>;

  beforeEach(() => {
    setBypassSeatCountCheck(true);
    login();
    organizationTestData.clean();
    organizationTestData.generate().then((res) => {
      organizationData = res.body as any;
      visit();
    });
  });

  beforeEach(() => {
    setBypassSeatCountCheck(true);
    deleteAllEmails();
  });

  afterEach(() => {
    setBypassSeatCountCheck(false);
  });

  it('generates invitations', () => {
    generateInvitation('MEMBER').should('contain', 'http://');

    generateInvitation('OWNER');
    generateInvitation('OWNER');
    generateInvitation('MEMBER');
    gcy('organization-invitation-item').should('have.length', 4);
    gcy('organization-invitation-item')
      .filter(':contains("MEMBER")')
      .should('have.length', 2);
    gcy('organization-invitation-item')
      .filter(':contains("OWNER")')
      .should('have.length', 2);
  });

  it('cancels invitation', () => {
    generateInvitation('MEMBER');
    generateInvitation('OWNER');

    gcy('organization-invitation-item').should('have.length', 2);
    gcy('organization-invitation-cancel-button').eq(0).click();
    gcy('organization-invitation-item').should('have.length', 1);
    gcy('organization-invitation-cancel-button').click();
    gcy('organization-invitation-item').should('not.exist');
  });

  it('owner invitation can be accepted', () => {
    testAcceptInvitation('OWNER', false);
  });

  it('member invitation can be accepted', () => {
    testAcceptInvitation('MEMBER', false);
  });

  it('owner invitation by email can be accepted', () => {
    testAcceptInvitation('OWNER', true);
  });

  it('member invitation by email can be accepted', () => {
    testAcceptInvitation('MEMBER', true);
  });

  it('invitation can be declined right away', () => {
    generateInvitation('MEMBER').then((code) => {
      logout();
      cy.visit(code as string);
      cy.gcy('accept-invitation-decline').click();
      cy.gcy('login-button').should('be.visible');
      cy.gcy('pending-invitation-banner').should('not.exist');
    });
  });

  it('invitation can be declined later', () => {
    generateInvitation('MEMBER').then((code) => {
      logout();
      cy.visit(code as string);
      cy.gcy('accept-invitation-accept').click();
      cy.gcy('login-button').should('be.visible');
      cy.gcy('pending-invitation-banner').should('contain', 'Tolgee');
      cy.gcy('pending-invitation-dismiss').click();
      cy.gcy('pending-invitation-banner').should('not.exist');
    });
  });

  after(() => {
    organizationTestData.clean();
  });

  function getTolgeeSlug() {
    return organizationData['Tolgee'].slug;
  }

  const visit = () => {
    const slug = getTolgeeSlug();
    cy.visit(`${HOST}/organizations/${slug}/members`);
  };

  const generateInvitation = (roleType: 'MEMBER' | 'OWNER', email = false) => {
    let clipboard: string;
    const slug = getTolgeeSlug();

    cy.visit(`${HOST}/organizations/${slug}/members`, {
      onBeforeLoad(win) {
        if (!email) {
          cy.stub(win, 'prompt').callsFake((_, input) => {
            clipboard = input;
          });
        }
      },
    });

    cy.gcy('invite-generate-button').click();

    if (!email) {
      cy.gcy('invitation-dialog-type-link-button').click();
    }

    gcy('invitation-dialog-role-button').click();
    gcy('organization-role-select-item')
      .filter(':visible')
      .contains(roleType)
      .click();

    cy.gcy('invitation-dialog-input-field').type('test@invitation.com');
    cy.gcy('invitation-dialog-invite-button').click();

    if (!email) {
      return assertMessage('Invitation link copied to clipboard').then(() => {
        return clipboard;
      });
    } else {
      waitForGlobalLoading();
      return assertMessage('Invitation was sent').then(() => {
        return getParsedEmailInvitationLink();
      });
    }
  };

  const testAcceptInvitation = (
    roleType: 'MEMBER' | 'OWNER',
    email: boolean
  ) => {
    generateInvitation(roleType, email).then((code) => {
      login('owner@zzzcool12.com', 'admin');
      cy.visit(code as string);

      cy.gcy('accept-invitation-info-text').should(
        'contain',
        'admin invited you to the organization Tolgee'
      );
      cy.gcy('accept-invitation-accept').should('be.visible').click();
      cy.visit(`${HOST}/projects`);
      assertSwitchedToOrganization('Tolgee');
    });
  };
});
