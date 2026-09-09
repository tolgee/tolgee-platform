import { HOST } from '../../common/constants';
import {
  login,
  setOrganizationCreationAllowed,
} from '../../common/apiCalls/common';
import { publicProjectsData } from '../../common/apiCalls/testData/testData';
import {
  assertSwitchedToOrganization,
  gcy,
  gcyAdvanced,
  openPublicProject,
} from '../../common/shared';
import { waitForGlobalLoading } from '../../common/loading';

describe('Org-less community user', () => {
  beforeEach(() => {
    // Without this the server hands every account an organization of its own on
    // first sign-in.
    setOrganizationCreationAllowed(false);
    publicProjectsData.clean();
    publicProjectsData.generateStandard();
    login('orgLessCommunityUser');
  });

  afterEach(() => {
    publicProjectsData.clean();
    setOrganizationCreationAllowed(true);
  });

  it('lands on the community projects instead of a dead end', () => {
    cy.visit(`${HOST}/`);
    waitForGlobalLoading();
    cy.url().should('include', '/community-projects');
    gcy('community-projects-view').should('be.visible');
    gcy('global-help-menu-button').should('be.visible');
    gcy('organization-switch').should('not.exist');
    gcy('dashboard-projects-list-item').should('have.length', 7);

    gcy('community-my-contributions-toggle').click();
    waitForGlobalLoading();
    gcy('dashboard-projects-list-item').should('not.exist');
  });

  it('opens a public project from the community list and adopts its organization', () => {
    cy.visit(`${HOST}/`);
    waitForGlobalLoading();

    gcyAdvanced({
      value: 'dashboard-projects-list-item',
      name: 'Community Alpha',
    }).click();
    cy.url().should('match', /\/projects\/[0-9]+/);
    waitForGlobalLoading();

    assertSwitchedToOrganization('publicProjectsUser');
  });

  it('opens a public project from the public list and adopts its organization', () => {
    openPublicProject('Community Alpha');
    gcy('notistack-snackbar').should('not.exist');
    gcy('global-base-view-content').should('exist');
    assertSwitchedToOrganization('publicProjectsUser');
  });

  it('still refuses project creation without an organization', () => {
    cy.visit(`${HOST}/projects/add`);
    waitForGlobalLoading();
    cy.url().should('include', '/community-projects');
  });

  it('offers a way back to the community projects after adopting', () => {
    openPublicProject('Community Alpha');
    assertSwitchedToOrganization('publicProjectsUser');

    gcy('organization-switch').click();
    gcyAdvanced({
      value: 'switch-popover-footer-action',
      action: 'organization-switch-community',
    }).click();
    waitForGlobalLoading();

    cy.url().should('include', '/community-projects');
    gcy('community-projects-view').should('be.visible');
  });
});
