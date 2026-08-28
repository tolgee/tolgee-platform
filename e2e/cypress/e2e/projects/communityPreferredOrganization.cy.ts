import { HOST } from '../../common/constants';
import {
  login,
  setOrganizationCreationAllowed,
} from '../../common/apiCalls/common';
import {
  assertSwitchedToOrganization,
  gcy,
  gcyAdvanced,
  selectOrganizationInSwitch,
  openOrganizationSwitch,
  switchToOrganization,
  visitRootAndSettle,
} from '../../common/shared';
import { waitForGlobalLoading } from '../../common/loading';
import {
  PUBLIC_PROJECT_NAME,
  HELD_REQUEST_MS,
  SET_PREFERRED_ORG,
  interceptPreferredOrgDelayed,
  interceptPreferredOrgForbidden,
  openPublicProject,
  communityProjectsFixture,
  restoreOrganizationCreation,
} from '../../common/communityProjects';
import { submitFormDirectly } from '../../common/forms';
import { organizationSettingsMenuItem } from '../../common/organizationSettingsMenu';

const WITHIN_HOLD_MS = HELD_REQUEST_MS / 3;

describe('Community preferred organization', () => {
  const fixture = communityProjectsFixture();

  describe('user with an organization', () => {
    beforeEach(() => {
      login('communityUser');
    });

    restoreOrganizationCreation();

    afterEach(() => {
      // An unsettled adoption write bleeds a 'resolving' preferred organization into the next test.
      waitForGlobalLoading();
    });

    it('switches a guest to the owning organization and shows its public projects', () => {
      openPublicProject();
      gcy('notistack-snackbar').should('not.exist');
      assertSwitchedToOrganization('publicProjectsUser');

      cy.visit(`${HOST}/`);
      waitForGlobalLoading();
      gcy('dashboard-projects-list-item').should('have.length', 6);
      gcyAdvanced({
        value: 'dashboard-projects-list-item',
        name: 'Community Outsider',
      }).should('not.exist');
      gcyAdvanced({
        value: 'dashboard-projects-list-item',
        name: 'Private project',
      }).should('not.exist');
      gcy('global-plus-button').should('not.exist');
      gcy('project-list-more-button').should('not.exist');
    });

    it('restores the full member experience after switching back to the own organization', () => {
      openPublicProject();
      assertSwitchedToOrganization('publicProjectsUser');

      cy.visit(`${HOST}/`);
      waitForGlobalLoading();
      switchToOrganization('Community User');
      waitForGlobalLoading();
      gcy('global-plus-button').should('exist');
      gcy('dashboard-projects-list-item').should('have.length', 1);
      gcy('project-list-more-button').should('exist');
    });

    it('renders a public project while its adoption write is still in flight', () => {
      interceptPreferredOrgDelayed('heldCrossOrgAdoption', HELD_REQUEST_MS);

      cy.visit(`${HOST}/community-projects`);
      waitForGlobalLoading();
      gcyAdvanced({
        value: 'dashboard-projects-list-item',
        name: PUBLIC_PROJECT_NAME,
      }).click();

      cy.wait('@heldCrossOrgAdoption.request');
      cy.url({ timeout: WITHIN_HOLD_MS }).should('match', /\/projects\/[0-9]+/);
      gcy('project-menu-items', { timeout: WITHIN_HOLD_MS }).should(
        'be.visible'
      );
    });

    it('waits for the adoption write before rendering an organization page it adopts', () => {
      interceptPreferredOrgDelayed('heldOrgPageAdoption', HELD_REQUEST_MS);

      cy.visit(
        `${HOST}/organizations/${fixture.organizations['publicProjectsUser'].slug}/profile`
      );
      cy.wait('@heldOrgPageAdoption.request');

      // The chrome above the page names the active organization, so it must not render this
      // organization's settings under the previous one's name.
      organizationSettingsMenuItem('profile', {
        timeout: WITHIN_HOLD_MS,
      }).should('not.exist');

      waitForGlobalLoading();
      organizationSettingsMenuItem('profile').should('be.visible');
      assertSwitchedToOrganization('publicProjectsUser');
    });

    it('keeps the project page usable when the preferred-organization switch fails', () => {
      interceptPreferredOrgForbidden();
      openPublicProject();
      cy.url().should('match', /\/projects\/[0-9]+/);
      gcy('global-base-view-content').should('exist');
    });

    it('keeps the EE menu of the own organization when the switch back to it fails', () => {
      openPublicProject();
      assertSwitchedToOrganization('publicProjectsUser');

      interceptPreferredOrgForbidden();
      cy.visit(
        `${HOST}/organizations/${fixture.organizations['Community User'].slug}/profile`
      );
      waitForGlobalLoading();

      organizationSettingsMenuItem('glossaries').should('be.visible');
      organizationSettingsMenuItem('translation-memories').should('be.visible');
    });

    it('disables the create submit on a foreign organization and creates in the switched-to one', () => {
      cy.intercept('POST', '**/v2/projects').as('createProject');

      cy.visit(`${HOST}/projects/add`);
      waitForGlobalLoading();
      gcy('global-form-save-button').should('be.enabled');
      gcy('project-create-no-permission-message').should('not.exist');

      openPublicProject();
      assertSwitchedToOrganization('publicProjectsUser');

      cy.visit(`${HOST}/projects/add`);
      waitForGlobalLoading();
      gcy('global-form-save-button').should('be.disabled');
      gcy('project-create-no-permission-message').should('be.visible');

      gcy('project-name-field').find('input').type('Switched org project');

      switchToOrganization('Community User');
      waitForGlobalLoading();
      gcy('global-form-save-button').should('be.enabled');
      gcy('project-create-no-permission-message').should('not.exist');
      gcy('project-name-field')
        .find('input')
        .should('have.value', 'Switched org project');

      gcy('global-form-save-button').click();
      cy.wait('@createProject').its('response.statusCode').should('eq', 200);
      waitForGlobalLoading();
      cy.url().should('match', /\/projects\/[0-9]+/);

      visitRootAndSettle();
      assertSwitchedToOrganization('Community User');
      gcyAdvanced({
        value: 'dashboard-projects-list-item',
        name: 'Switched org project',
      }).should('be.visible');
    });

    it('offers no organization creation in the switcher while the server refuses it', () => {
      visitRootAndSettle();
      openOrganizationSwitch();
      gcy('switch-popover-new').should('be.visible');

      setOrganizationCreationAllowed(false);
      visitRootAndSettle();
      openOrganizationSwitch();
      gcy('switch-popover-new').should('not.exist');
    });

    it('follows the preferred-organization link, with and without a path suffix', () => {
      cy.visit(`${HOST}/preferred-organization`);
      waitForGlobalLoading();
      cy.url().should(
        'match',
        new RegExp(
          `/organizations/${fixture.organizations['Community User'].slug}/profile$`
        )
      );
      organizationSettingsMenuItem('profile').should('be.visible');

      cy.visit(`${HOST}/preferred-organization?path=members`);
      waitForGlobalLoading();
      cy.url().should(
        'include',
        `/organizations/${fixture.organizations['Community User'].slug}/members`
      );
    });

    it('opens the glossaries of an adopted foreign organization', () => {
      openPublicProject();

      cy.visit(
        `${HOST}/organizations/${fixture.organizations['publicProjectsUser'].slug}/glossaries`
      );
      waitForGlobalLoading();

      organizationSettingsMenuItem('glossaries').should('be.visible');
      gcy('global-user-menu-button').should('be.visible');
    });

    it('offers the organization entries in the user menu when there is a preferred organization', () => {
      visitRootAndSettle();

      gcy('global-user-menu-button').click();
      cy.waitForDom();
      gcy('user-menu-organization-switch').should('be.visible');
      gcy('user-menu-organization-settings').should('be.visible');
    });

    it('adopts a foreign organization it opens without granting it any manage entry', () => {
      cy.visit(
        `${HOST}/organizations/${fixture.organizations['publicProjectsUser'].slug}/profile`
      );
      waitForGlobalLoading();

      assertSwitchedToOrganization('publicProjectsUser');
      organizationSettingsMenuItem('profile').should('be.visible');
      organizationSettingsMenuItem('translation-memories').should('not.exist');
    });

    it('sends a deep link to a manage-only page back to the profile it can see', () => {
      cy.visit(
        `${HOST}/organizations/${fixture.organizations['publicProjectsUser'].slug}/apps`
      );
      waitForGlobalLoading();

      cy.url().should('include', '/profile');
      cy.url().should('not.include', '/apps');
      assertSwitchedToOrganization('publicProjectsUser');
    });

    it('sends deep links to the other restricted pages back to the profile too', () => {
      const slug = fixture.organizations['publicProjectsUser'].slug;

      ['members', 'member-privileges', 'sso', 'translation-memories'].forEach(
        (page) => {
          cy.visit(`${HOST}/organizations/${slug}/${page}`);
          waitForGlobalLoading();

          cy.url().should('include', '/profile');
          cy.url().should('not.include', `/${page}`);
          organizationSettingsMenuItem('profile').should('be.visible');
        }
      );
    });

    it('shows glossaries but hides translation memories in the foreign org settings', () => {
      openPublicProject();
      cy.visit(
        `${HOST}/organizations/${fixture.organizations['publicProjectsUser'].slug}/profile`
      );
      waitForGlobalLoading();
      organizationSettingsMenuItem('profile').should('be.visible');
      organizationSettingsMenuItem('glossaries').should('be.visible');
      organizationSettingsMenuItem('translation-memories').should('not.exist');
      gcy('organization-profile-leave-button').should('be.disabled');
      gcy('organization-profile-delete-button').should('not.exist');
    });

    it('creates a project when the form is submitted directly (positive control for the refusal specs)', () => {
      cy.intercept('POST', '**/v2/projects').as('createProject');

      cy.visit(`${HOST}/projects/add`);
      waitForGlobalLoading();
      gcy('project-name-field').find('input').type('Directly submitted');
      submitFormDirectly();

      cy.wait('@createProject').its('response.statusCode').should('eq', 200);
      waitForGlobalLoading();
      cy.url().should('match', /\/projects\/[0-9]+/);
    });

    it('issues one create even when the form is submitted twice while the first is in flight', () => {
      cy.intercept('POST', '**/v2/projects', (req) =>
        req.continue((res) => {
          res.setDelay(HELD_REQUEST_MS);
        })
      ).as('createProject');

      cy.visit(`${HOST}/projects/add`);
      waitForGlobalLoading();
      gcy('project-name-field').find('input').type('Submitted twice');

      submitFormDirectly();
      cy.wait('@createProject.request');
      submitFormDirectly();

      waitForGlobalLoading();
      cy.get('@createProject.all').should('have.length', 1);
    });

    it('creates one organization even when the form is submitted twice after the create lands', () => {
      interceptPreferredOrgDelayed('heldAdoptionAfterCreate', HELD_REQUEST_MS);
      cy.intercept('POST', '**/v2/organizations').as('createOrganization');

      cy.visit(`${HOST}/organizations/add`);
      waitForGlobalLoading();
      gcy('organization-name-field').find('input').type('Submitted twice');

      // The slug is not required client-side, so the save button enables before it is filled.
      gcy('organization-address-part-field')
        .find('input')
        .should('not.have.value', '');
      gcy('global-form-save-button').should('not.be.disabled').click();
      cy.wait('@createOrganization');
      submitFormDirectly();

      cy.wait('@heldAdoptionAfterCreate');
      waitForGlobalLoading();
      cy.get('@createOrganization.all').should('have.length', 1);
    });

    it('surfaces a server refusal of the create request', () => {
      cy.intercept('POST', '**/v2/projects', {
        statusCode: 403,
        body: { code: 'user_is_not_owner_or_maintainer_of_organization' },
      }).as('createProject');

      cy.visit(`${HOST}/projects/add`);
      waitForGlobalLoading();
      gcy('error-message').should('not.exist');
      gcy('project-name-field').find('input').type('Refused by server');
      gcy('global-form-save-button').click();

      cy.wait('@createProject');
      waitForGlobalLoading();
      cy.url().should('include', '/projects/add');
      gcy('error-message').should('be.visible');
      gcy('global-form-save-button').should('be.enabled');
    });
  });

  describe('user who owns one organization and belongs to another', () => {
    beforeEach(() => {
      login('dualOrgCommunityUser');
    });

    it('blocks the submit while switching into an organization it cannot create in', () => {
      interceptPreferredOrgDelayed('heldSwitch', HELD_REQUEST_MS);
      cy.intercept('POST', '**/v2/projects').as('createProject');

      cy.visit(`${HOST}/projects/add`);
      waitForGlobalLoading();
      gcy('global-form-save-button').should('be.enabled');
      gcy('project-name-field').find('input').type('Submitted mid-switch');

      selectOrganizationInSwitch('Community User');

      gcy('global-form-save-button').should('be.disabled');
      gcy('project-create-switching-message').should('be.visible');
      gcy('project-create-no-permission-message').should('not.exist');
      submitFormDirectly();
      waitForGlobalLoading();
      cy.get('@createProject.all').should('have.length', 0);

      cy.wait('@heldSwitch');
      waitForGlobalLoading();
      gcy('global-form-save-button').should('be.disabled');
      gcy('project-create-no-permission-message').should('be.visible');
      cy.get('@createProject.all').should('have.length', 0);
    });

    it('keeps the newest selection when an earlier switch resolves after it', () => {
      interceptPreferredOrgDelayed(
        'overlappingSwitches',
        HELD_REQUEST_MS,
        () => fixture.organizations['Community User'].id
      );

      cy.visit(`${HOST}/projects/add`);
      waitForGlobalLoading();

      selectOrganizationInSwitch('Community User');
      selectOrganizationInSwitch('Dual Org Member');

      cy.wait('@overlappingSwitches');
      cy.wait('@overlappingSwitches');
      waitForGlobalLoading();
      assertSwitchedToOrganization('Dual Org Member');

      visitRootAndSettle();
      assertSwitchedToOrganization('Dual Org Member');
    });

    it('stays put when the switcher selection is refused, and switches on a later try', () => {
      interceptPreferredOrgForbidden();

      cy.visit(
        `${HOST}/organizations/${fixture.organizations['Dual Org Member'].slug}/profile`
      );
      waitForGlobalLoading();

      selectOrganizationInSwitch('Community User');
      waitForGlobalLoading();

      cy.url().should('include', fixture.organizations['Dual Org Member'].slug);
      assertSwitchedToOrganization('Dual Org Member');

      // Cypress has no intercept removal, and a spy-only intercept would not shadow the 403 above.
      cy.intercept('PUT', SET_PREFERRED_ORG, (req) => req.continue()).as(
        'freshSwitch'
      );
      selectOrganizationInSwitch('Community User');
      cy.wait('@freshSwitch');
      waitForGlobalLoading();
      assertSwitchedToOrganization('Community User');
    });
  });

  describe('server admin', () => {
    beforeEach(() => {
      login('admin', 'admin');
    });

    it('offers the manage and EE menu entries on a foreign organization', () => {
      cy.visit(
        `${HOST}/organizations/${fixture.organizations['Members Only Outfit'].slug}/profile`
      );
      waitForGlobalLoading();

      organizationSettingsMenuItem('glossaries').should('be.visible');
      organizationSettingsMenuItem('translation-memories').should('be.visible');
      organizationSettingsMenuItem('profile').should('be.visible');
    });

    it('creates a project in a foreign organization it is not a member of', () => {
      cy.intercept('POST', '**/v2/projects').as('createProject');

      openPublicProject();
      assertSwitchedToOrganization('publicProjectsUser');

      cy.visit(`${HOST}/projects/add`);
      waitForGlobalLoading();
      gcy('project-create-no-permission-message').should('not.exist');
      gcy('global-form-save-button').should('be.enabled');

      gcy('project-name-field').find('input').type('Admin created project');
      gcy('global-form-save-button').click();

      cy.wait('@createProject').then(({ response }) => {
        expect(response?.statusCode).to.eq(200);
        expect(response?.body.organizationOwner.slug).to.eq(
          fixture.organizations['publicProjectsUser'].slug
        );
      });
      waitForGlobalLoading();
      cy.url().should('match', /\/projects\/[0-9]+/);
    });

    it('adopts a customer organization when it opens one of its EE pages', () => {
      visitRootAndSettle();

      const customer = fixture.organizations['publicProjectsUser'];
      cy.intercept('PUT', SET_PREFERRED_ORG).as('adoption');
      cy.visit(`${HOST}/organizations/${customer.slug}/glossaries`);
      waitForGlobalLoading();

      cy.url().should('include', '/glossaries');
      cy.wait('@adoption')
        .its('request.url')
        .should('include', `/set-preferred-organization/${customer.id}`);
    });

    it('adopts a foreign organization it opens and keeps its admin access to it', () => {
      cy.visit(
        `${HOST}/organizations/${fixture.organizations['publicProjectsUser'].slug}/profile`
      );
      waitForGlobalLoading();
      organizationSettingsMenuItem('profile').should('be.visible');
      gcy('organization-profile-delete-button').should('be.visible');

      visitRootAndSettle();
      assertSwitchedToOrganization('publicProjectsUser');
    });
  });

  describe('supporter who owns no organization of its own', () => {
    beforeEach(() => {
      login('supporterCommunityUser');
    });

    it('manages a foreign organization but cannot edit its profile', () => {
      cy.visit(
        `${HOST}/organizations/${fixture.organizations['Members Only Outfit'].slug}/profile`
      );
      waitForGlobalLoading();

      organizationSettingsMenuItem('members').should('be.visible');
      organizationSettingsMenuItem('apps').should('be.visible');

      gcy('organization-profile-delete-button').should('not.exist');
      gcy('global-form-save-button').should('be.disabled');
    });

    it('reads the apps page of a foreign organization, as the read-only bypass allows', () => {
      cy.visit(
        `${HOST}/organizations/${fixture.organizations['Members Only Outfit'].slug}/apps`
      );
      waitForGlobalLoading();

      cy.url().should('include', '/apps');
      cy.url().should('not.include', '/profile');
    });

    it('offers no project creation in organizations it only supports or belongs to', () => {
      openPublicProject();
      assertSwitchedToOrganization('publicProjectsUser');

      visitRootAndSettle();
      gcy('global-plus-button').should('not.exist');

      cy.visit(`${HOST}/projects/add`);
      waitForGlobalLoading();
      gcy('global-form-save-button').should('be.disabled');
      gcy('project-create-no-permission-message').should('be.visible');

      switchToOrganization('Community User');
      waitForGlobalLoading();
      gcy('global-form-save-button').should('be.disabled');
      gcy('project-create-no-permission-message').should('be.visible');
    });
  });
});
