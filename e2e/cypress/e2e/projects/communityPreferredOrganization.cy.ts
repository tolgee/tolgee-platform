import { HOST } from '../../common/constants';
import { login, logout, setProperty } from '../../common/apiCalls/common';
import { publicProjectsData } from '../../common/apiCalls/testData/testData';
import {
  assertSwitchedToOrganization,
  gcy,
  gcyAdvanced,
  switchToOrganization,
} from '../../common/shared';
import { waitForGlobalLoading } from '../../common/loading';

const SET_PREFERRED_ORG = '**/v2/user-preferences/set-preferred-organization/*';

describe('Community preferred organization', () => {
  let organizations: Record<string, { slug: string }>;
  let privateProjectId: number;
  let publicProjectId: number;

  beforeEach(() => {
    publicProjectsData.clean();
    publicProjectsData.generateStandard().then((res) => {
      organizations = {};
      res.body.organizations.forEach((org) => {
        organizations[org.name] = org;
      });
      privateProjectId = res.body.projects.find(
        (project) => project.name === 'Private project'
      ).id;
      publicProjectId = res.body.projects.find(
        (project) => project.name === 'Community Alpha'
      ).id;
    });
  });

  afterEach(() => {
    publicProjectsData.clean();
  });

  const interceptPreferredOrgForbidden = () =>
    cy.intercept('PUT', SET_PREFERRED_ORG, {
      statusCode: 403,
      body: { code: 'operation_not_permitted' },
    });

  const openPublicProject = () => {
    cy.visit(`${HOST}/public-projects`);
    waitForGlobalLoading();
    gcy('dashboard-projects-list-item').contains('Community Alpha').click();
    cy.url().should('match', /\/projects\/[0-9]+/);
    waitForGlobalLoading();
  };

  it('hides the help menu from a signed-out visitor on an unmatched route', () => {
    logout();

    cy.visit(`${HOST}/definitely-not-a-route`, { failOnStatusCode: false });
    waitForGlobalLoading();
    gcy('help-menu-button').should('not.exist');
  });

  describe('user with an organization', () => {
    beforeEach(() => {
      login('communityUser');
    });

    it('switches a guest to the owning organization and shows its public projects', () => {
      openPublicProject();
      gcy('notistack-snackbar').should('not.exist');
      assertSwitchedToOrganization('publicProjectsUser');

      cy.visit(`${HOST}/`);
      waitForGlobalLoading();
      gcy('dashboard-projects-list-item').should('have.length', 6);
      cy.contains('Community Outsider').should('not.exist');
      cy.contains('Private project').should('not.exist');
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

    it('keeps the project page usable when the preferred-organization switch fails', () => {
      interceptPreferredOrgForbidden();
      openPublicProject();
      cy.url().should('match', /\/projects\/[0-9]+/);
      gcy('global-base-view-content').should('exist');
    });

    it('shows glossaries but hides translation memories in the foreign org settings', () => {
      openPublicProject();
      cy.visit(
        `${HOST}/organizations/${organizations['publicProjectsUser'].slug}/profile`
      );
      waitForGlobalLoading();
      gcyAdvanced({ value: 'settings-menu-item', item: 'profile' }).should(
        'be.visible'
      );
      gcyAdvanced({ value: 'settings-menu-item', item: 'glossaries' }).should(
        'be.visible'
      );
      gcyAdvanced({
        value: 'settings-menu-item',
        item: 'translation-memories',
      }).should('not.exist');
      gcy('organization-profile-leave-button').should('be.disabled');
    });
  });

  describe('user without any organization', () => {
    beforeEach(() => {
      setProperty('authentication.userCanCreateOrganizations', false);
      login('orgLessCommunityUser');
    });

    afterEach(() => {
      setProperty('authentication.userCanCreateOrganizations', true);
    });

    const visitRootAndExpectNoOrganization = () => {
      cy.visit(HOST);
      waitForGlobalLoading();
      gcy('no-permissions-message').should('be.visible');
    };

    it('opens a public project and adopts the owning organization', () => {
      visitRootAndExpectNoOrganization();

      openPublicProject();
      assertSwitchedToOrganization('publicProjectsUser');
      gcy('notistack-snackbar').should('not.exist');
    });

    it('keeps the organization after a reload', () => {
      visitRootAndExpectNoOrganization();

      openPublicProject();
      cy.visit(HOST);
      waitForGlobalLoading();
      assertSwitchedToOrganization('publicProjectsUser');
      gcy('dashboard-projects-list-item').should('have.length', 6);
      gcy('dashboard-projects-list-item')
        .contains('Private project')
        .should('not.exist');
      gcy('dashboard-projects-list-item')
        .contains('Community Outsider')
        .should('not.exist');
      gcy('no-permissions-message').should('not.exist');
    });

    it('still requires an organization to create a project', () => {
      visitRootAndExpectNoOrganization();

      cy.visit(`${HOST}/projects/add`);
      waitForGlobalLoading();
      cy.url().should('include', '/projects/add');
      gcy('no-permissions-message').should('be.visible');
    });

    it('offers the help menu even without an organization', () => {
      visitRootAndExpectNoOrganization();
      gcy('help-menu-button').should('be.visible');

      openPublicProject();
      gcy('help-menu-button').should('be.visible');
    });

    it('adopts the organization on a cold deep-link to a public project', () => {
      visitRootAndExpectNoOrganization();

      cy.visit(`${HOST}/projects/${publicProjectId}`);
      waitForGlobalLoading();
      cy.url().should('match', /\/projects\/[0-9]+/);
      assertSwitchedToOrganization('publicProjectsUser');
      gcy('notistack-snackbar').should('not.exist');
    });

    it('keeps the project usable when the organization switch fails', () => {
      interceptPreferredOrgForbidden();
      visitRootAndExpectNoOrganization();

      cy.visit(`${HOST}/projects/${publicProjectId}`);
      waitForGlobalLoading();
      cy.url().should('match', /\/projects\/[0-9]+/);
      gcy('global-base-view-content').should('exist');
      gcy('organization-switch').should('not.exist');
      gcy('navigation-item').first().should('contain', 'Community Alpha');

      visitRootAndExpectNoOrganization();
    });

    it('does not adopt an organization from a project it cannot view', () => {
      cy.intercept('PUT', SET_PREFERRED_ORG).as('setPreferred');
      cy.intercept('GET', `**/v2/projects/${privateProjectId}`).as(
        'privateProjectDetail'
      );
      visitRootAndExpectNoOrganization();

      cy.visit(`${HOST}/projects/${privateProjectId}`);
      cy.wait('@privateProjectDetail')
        .its('response.statusCode')
        .should('be.oneOf', [403, 404]);
      waitForGlobalLoading();
      gcy('no-permissions-message').should('be.visible');
      gcy('global-base-view-content').should('not.exist');
      cy.get('@setPreferred.all').should('have.length', 0);

      visitRootAndExpectNoOrganization();
    });

    it('still requires an organization to create an organization', () => {
      visitRootAndExpectNoOrganization();

      cy.visit(`${HOST}/organizations/add`);
      waitForGlobalLoading();
      cy.url().should('include', '/organizations/add');
      gcy('no-permissions-message').should('be.visible');
    });

    it('lists community projects without an organization', () => {
      visitRootAndExpectNoOrganization();

      cy.visit(`${HOST}/community-projects`);
      waitForGlobalLoading();
      gcy('no-permissions-message').should('not.exist');
      gcy('dashboard-projects-list-item').should('have.length', 7);
      gcy('dashboard-projects-list-item')
        .contains('Private project')
        .should('not.exist');
      gcy('community-translation-item').should('be.visible');
      gcy('organization-switch').should('not.exist');
    });

    it('does not re-issue the switch when the project is reopened', () => {
      cy.intercept('PUT', SET_PREFERRED_ORG).as('setPreferred');
      cy.intercept('GET', `**/v2/projects/${publicProjectId}`).as(
        'projectDetail'
      );
      visitRootAndExpectNoOrganization();

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
});
