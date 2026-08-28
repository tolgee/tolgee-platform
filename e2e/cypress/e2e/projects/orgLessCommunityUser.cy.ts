import { HOST } from '../../common/constants';
import { login } from '../../common/apiCalls/common';
import {
  assertSwitchedToOrganization,
  gcy,
  gcyAdvanced,
  openOrganizationSwitch,
  visitRootAndSettle,
} from '../../common/shared';
import { waitForGlobalLoading } from '../../common/loading';
import {
  PAST_SWITCH_DEADLINE_MS,
  PUBLIC_PROJECT_NAME,
  SET_PREFERRED_ORG,
  SWITCH_DEADLINE_MS,
  communityProjectsFixture,
  interceptPreferredOrgDelayed,
  interceptPreferredOrgForbidden,
  keepPersonasOrganizationLess,
  openPublicProject,
} from '../../common/communityProjects';
import { submitFormDirectly } from '../../common/forms';
import { organizationSettingsMenuItem } from '../../common/organizationSettingsMenu';

describe('Org-less community user', () => {
  const customerOrganizationProfile = () =>
    `${HOST}/organizations/${fixture.organizations['publicProjectsUser'].slug}/profile`;

  keepPersonasOrganizationLess();

  const fixture = communityProjectsFixture();

  beforeEach(() => {
    login('orgLessCommunityUser');
  });

  const assertRootLandsOnCommunityProjects = () => {
    visitRootAndSettle();
    cy.url().should('include', '/community-projects');
    gcy('community-projects-view').should('be.visible');
    gcy('no-permissions-message').should('not.exist');
  };

  it('adopts the owning organization from the community projects list', () => {
    assertRootLandsOnCommunityProjects();

    gcyAdvanced({
      value: 'dashboard-projects-list-item',
      name: PUBLIC_PROJECT_NAME,
    }).click();
    cy.url().should('match', /\/projects\/[0-9]+/);
    waitForGlobalLoading();
    assertSwitchedToOrganization('publicProjectsUser');
  });

  it('opens a public project and adopts the owning organization', () => {
    assertRootLandsOnCommunityProjects();

    openPublicProject();
    assertSwitchedToOrganization('publicProjectsUser');
    gcy('notistack-snackbar').should('not.exist');
  });

  it("re-adopts when moving in-app from one organization's project to another's", () => {
    assertRootLandsOnCommunityProjects();

    openPublicProject();
    assertSwitchedToOrganization('publicProjectsUser');

    openOrganizationSwitch();
    gcyAdvanced({
      value: 'switch-popover-footer-action',
      action: 'organization-switch-community',
    }).click();
    waitForGlobalLoading();

    gcyAdvanced({
      value: 'dashboard-projects-list-item',
      name: 'Community Outsider',
    }).click();
    cy.url().should('match', /\/projects\/[0-9]+/);
    waitForGlobalLoading();

    assertSwitchedToOrganization('Community User');
  });

  it('offers a way back to the community projects after adopting', () => {
    assertRootLandsOnCommunityProjects();

    openPublicProject();
    assertSwitchedToOrganization('publicProjectsUser');

    openOrganizationSwitch();
    gcyAdvanced({
      value: 'switch-popover-footer-action',
      action: 'organization-switch-community',
    }).click();

    waitForGlobalLoading();
    cy.url().should('include', '/community-projects');
    gcy('community-projects-view').should('be.visible');
  });

  it('keeps the organization after a reload', () => {
    assertRootLandsOnCommunityProjects();

    openPublicProject();
    visitRootAndSettle();
    assertSwitchedToOrganization('publicProjectsUser');
    gcy('dashboard-projects-list-item').should('have.length', 6);
    gcyAdvanced({
      value: 'dashboard-projects-list-item',
      name: 'Private project',
    }).should('not.exist');
    gcyAdvanced({
      value: 'dashboard-projects-list-item',
      name: 'Community Outsider',
    }).should('not.exist');
    gcy('no-permissions-message').should('not.exist');
  });

  it('disables the create submit after adopting a foreign organization', () => {
    assertRootLandsOnCommunityProjects();

    openPublicProject();
    assertSwitchedToOrganization('publicProjectsUser');

    cy.visit(`${HOST}/projects/add`);
    waitForGlobalLoading();
    gcy('no-permissions-message').should('not.exist');
    gcy('global-form-save-button').should('be.disabled');
    gcy('project-create-no-permission-message').should('be.visible');

    cy.intercept('POST', '**/v2/projects').as('createProject');
    gcy('project-name-field').find('input').type('Not allowed');
    submitFormDirectly();

    visitRootAndSettle();
    cy.get('@createProject.all').should('have.length', 0);
  });

  it('refuses the project form without an organization', () => {
    assertRootLandsOnCommunityProjects();

    cy.visit(`${HOST}/projects/add`);
    waitForGlobalLoading();
    cy.url().should('include', '/projects/add');
    gcyAdvanced({
      value: 'no-permissions-message',
      reason: 'no-organization',
    }).should('be.visible');
    gcy('global-form-save-button').should('not.exist');
    gcy('project-name-field').should('not.exist');
  });

  it('redirects the projects route to the community projects', () => {
    cy.visit(`${HOST}/projects`);
    waitForGlobalLoading();
    cy.url().should('include', '/community-projects');
    gcy('community-projects-view').should('be.visible');
  });

  it('adopts an organization it may view from a direct link', () => {
    cy.visit(customerOrganizationProfile());
    waitForGlobalLoading();
    gcy('no-permissions-message').should('not.exist');
    organizationSettingsMenuItem('profile').should('be.visible');
    assertSwitchedToOrganization('publicProjectsUser');
  });

  it('holds the project page until the adoption write settles', () => {
    interceptPreferredOrgDelayed('heldAdoption');

    cy.visit(`${HOST}/projects/${fixture.publicProjectId}`);
    cy.wait('@heldAdoption.request');
    gcy('project-menu-items').should('not.exist');

    waitForGlobalLoading();
    gcy('project-menu-items').should('be.visible');
  });

  it('holds the organization page until the adoption write settles', () => {
    interceptPreferredOrgDelayed('heldAdoption');

    cy.visit(customerOrganizationProfile());
    cy.wait('@heldAdoption.request');
    organizationSettingsMenuItem('profile').should('not.exist');

    waitForGlobalLoading();
    organizationSettingsMenuItem('profile').should('be.visible');
  });

  it('renders the organization page when the adoption write is refused', () => {
    interceptPreferredOrgForbidden();

    cy.visit(customerOrganizationProfile());
    waitForGlobalLoading();
    organizationSettingsMenuItem('profile').should('be.visible');
    gcy('no-permissions-message').should('not.exist');
  });

  it('keeps an EE route usable when the adoption write is refused', () => {
    interceptPreferredOrgForbidden();

    cy.visit(
      `${HOST}/organizations/${fixture.organizations['publicProjectsUser'].slug}/glossaries`
    );
    waitForGlobalLoading();

    cy.url().should('include', '/glossaries');
    organizationSettingsMenuItem('profile').should('be.visible');
  });

  it('adopts the organization behind an EE route it deep-links into', () => {
    cy.visit(
      `${HOST}/organizations/${fixture.organizations['publicProjectsUser'].slug}/glossaries`
    );
    waitForGlobalLoading();

    cy.url().should('include', '/glossaries');
    assertSwitchedToOrganization('publicProjectsUser');
    organizationSettingsMenuItem('glossaries').should('be.visible');
  });

  it('keeps the project usable when an adoption write never answers', () => {
    interceptPreferredOrgDelayed('stalledAdoption', PAST_SWITCH_DEADLINE_MS);

    cy.visit(`${HOST}/community-projects`);
    waitForGlobalLoading();
    gcyAdvanced({
      value: 'dashboard-projects-list-item',
      name: PUBLIC_PROJECT_NAME,
    }).click();
    cy.wait('@stalledAdoption.request');

    // Past the sequencer deadline the abandoned switch must stop blocking the UI, so the viewer
    // reaches a usable page instead of a permanent loader.
    cy.url({ timeout: SWITCH_DEADLINE_MS + 15000 }).should(
      'match',
      /\/projects\/[0-9]+/
    );
    gcy('global-base-view-content', {
      timeout: SWITCH_DEADLINE_MS + 15000,
    }).should('be.visible');
  });

  it('is sent to the community projects by the preferred-organization link', () => {
    cy.visit(`${HOST}/preferred-organization`);
    waitForGlobalLoading();
    cy.url().should('include', '/community-projects');
    gcy('community-projects-view').should('be.visible');
  });

  it('is sent to the community projects by the billing link on a server without billing', () => {
    cy.visit(`${HOST}/billing`);
    waitForGlobalLoading();
    cy.url().should('include', '/community-projects');
  });

  it('is told why an organization page cannot be loaded', () => {
    cy.intercept('GET', '**/v2/organizations/*', { statusCode: 500 }).as(
      'orgFailed'
    );

    cy.visit(customerOrganizationProfile());
    waitForGlobalLoading();
    gcy('organization-load-failed-message').should('be.visible');
  });

  it('is sent back to the community projects by an organization it may not view', () => {
    cy.visit(
      `${HOST}/organizations/${fixture.organizations['Members Only Outfit'].slug}/profile`
    );
    waitForGlobalLoading();
    cy.url().should('include', '/community-projects');
    organizationSettingsMenuItem('profile').should('not.exist');
  });

  it('offers a user menu without any organization entries', () => {
    assertRootLandsOnCommunityProjects();

    gcy('global-user-menu-button').click();
    cy.waitForDom();
    gcy('user-menu-organization-settings').should('not.exist');
    gcy('user-menu-organization-switch').should('not.exist');
    gcy('user-menu-user-settings').should('be.visible');
  });

  it('offers the help menu even without an organization', () => {
    assertRootLandsOnCommunityProjects();
    gcy('help-menu-button').should('be.visible');

    openPublicProject();
    gcy('help-menu-button').should('be.visible');
  });

  it('adopts the organization on a cold deep-link to a public project', () => {
    assertRootLandsOnCommunityProjects();

    cy.visit(`${HOST}/projects/${fixture.publicProjectId}`);
    waitForGlobalLoading();
    cy.url().should('match', /\/projects\/[0-9]+/);
    assertSwitchedToOrganization('publicProjectsUser');
    gcy('notistack-snackbar').should('not.exist');
  });

  it('keeps the project usable when the organization switch fails', () => {
    interceptPreferredOrgForbidden();
    assertRootLandsOnCommunityProjects();

    cy.visit(`${HOST}/projects/${fixture.publicProjectId}`);
    waitForGlobalLoading();
    cy.url().should('match', /\/projects\/[0-9]+/);
    gcy('global-base-view-content').should('exist');
    gcy('organization-switch').should('not.exist');
    gcy('navigation-item').first().should('contain', PUBLIC_PROJECT_NAME);

    cy.visit(`${HOST}/projects/${fixture.publicProjectId}/translations`);
    waitForGlobalLoading();
    gcy('translations-view-list-button').should('be.visible');
    gcy('organization-switch').should('not.exist');

    assertRootLandsOnCommunityProjects();
  });

  it('does not adopt an organization from a project it cannot view', () => {
    cy.intercept('PUT', SET_PREFERRED_ORG).as('setPreferred');
    cy.intercept('GET', `**/v2/projects/${fixture.privateProjectId}`).as(
      'privateProjectDetail'
    );
    assertRootLandsOnCommunityProjects();

    cy.visit(`${HOST}/projects/${fixture.privateProjectId}`);
    cy.wait('@privateProjectDetail')
      .its('response.statusCode')
      .should('be.oneOf', [403, 404]);
    waitForGlobalLoading();
    cy.url().should('include', '/community-projects');
    gcy('community-projects-view').should('be.visible');
    cy.get('@setPreferred.all').should('have.length', 0);

    assertRootLandsOnCommunityProjects();
  });

  it('lists community projects without an organization', () => {
    assertRootLandsOnCommunityProjects();

    cy.visit(`${HOST}/community-projects`);
    waitForGlobalLoading();
    gcy('no-permissions-message').should('not.exist');
    gcy('dashboard-projects-list-item').should('have.length', 7);
    gcyAdvanced({
      value: 'dashboard-projects-list-item',
      name: 'Private project',
    }).should('not.exist');
    gcy('community-translation-item').should('be.visible');
    gcy('organization-switch').should('not.exist');
  });

  it('does not re-issue the switch when the project is reopened', () => {
    cy.intercept('PUT', SET_PREFERRED_ORG).as('setPreferred');
    cy.intercept('GET', `**/v2/projects/${fixture.publicProjectId}`).as(
      'projectDetail'
    );
    assertRootLandsOnCommunityProjects();

    openPublicProject();
    cy.wait('@setPreferred');

    cy.get('@projectDetail.all').then((beforeReload) => {
      cy.reload();
      cy.get('@projectDetail.all').should(
        'have.length.at.least',
        beforeReload.length + 1
      );
    });
    waitForGlobalLoading();
    assertSwitchedToOrganization('publicProjectsUser');
    cy.get('@setPreferred.all').should('have.length', 1);
  });
});
