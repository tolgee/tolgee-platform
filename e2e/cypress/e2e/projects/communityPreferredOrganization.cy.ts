import { HOST } from '../../common/constants';
import { login } from '../../common/apiCalls/common';
import { publicProjectsData } from '../../common/apiCalls/testData/testData';
import {
  assertSwitchedToOrganization,
  gcy,
  gcyAdvanced,
  switchToOrganization,
} from '../../common/shared';
import { waitForGlobalLoading } from '../../common/loading';

describe('Community preferred organization', () => {
  let organizations: Record<string, { slug: string }>;

  beforeEach(() => {
    publicProjectsData.clean();
    publicProjectsData.generateStandard().then((res) => {
      organizations = {};
      res.body.organizations.forEach((org) => {
        organizations[org.name] = org;
      });
    });
    login('communityUser');
  });

  afterEach(() => {
    publicProjectsData.clean();
  });

  const openPublicProject = () => {
    cy.visit(`${HOST}/public-projects`);
    waitForGlobalLoading();
    gcy('dashboard-projects-list-item').contains('Community Alpha').click();
    cy.url().should('match', /\/projects\/[0-9]+/);
    waitForGlobalLoading();
  };

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
    cy.intercept('PUT', '**/v2/user-preferences/set-preferred-organization/*', {
      statusCode: 403,
      body: { code: 'operation_not_permitted' },
    });
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
