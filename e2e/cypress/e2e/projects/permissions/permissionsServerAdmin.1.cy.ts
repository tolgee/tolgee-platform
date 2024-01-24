import { login } from '../../../common/apiCalls/common';
import { HOST } from '../../../common/constants';
import {
  checkPermissions,
  RUN,
  SKIP,
  visitProjectWithPermissions,
} from '../../../common/permissions/main';
import {
  switchToOrganization,
  visitProjectDashboard,
} from '../../../common/shared';

describe('Server admin 1', () => {
  it('admin', () => {
    visitProjectWithPermissions({ scopes: ['admin'] }).then((projectInfo) => {
      // login as admin
      login('admin', 'admin');

      // check that admin has no warning banner on his home page
      switchToOrganization('admin');
      cy.gcy('administration-access-message').should('not.exist');

      cy.intercept(
        'PUT',
        '**/v2/user-preferences/set-preferred-organization/**'
      ).as('set-preferred');

      // check that he has admin banner on project which is not his
      visitProjectDashboard(projectInfo.project.id);
      cy.gcy('administration-access-message', { timeout: 30_000 }).should(
        'be.visible'
      );

      cy.wait('@set-preferred', { timeout: 30_000 })
        .its('response.statusCode')
        .should('eq', 200);

      cy.visit(HOST);
      cy.gcy('administration-access-message').should('be.visible');
      cy.visit(`${HOST}/organizations/admin-admin-com/profile`);
      cy.gcy('administration-access-message').should('be.visible');

      // check that admin has correct access to everything
      visitProjectDashboard(projectInfo.project.id);
      checkPermissions(projectInfo, {
        'project-menu-item-dashboard': RUN,
        'project-menu-item-translations': SKIP,
        'project-menu-item-settings': SKIP,
        'project-menu-item-languages': SKIP,
        'project-menu-item-members': SKIP,
        'project-menu-item-import': SKIP,
        'project-menu-item-export': SKIP,
        'project-menu-item-developer': SKIP,
        'project-menu-item-integrate': SKIP,
      });
    });
  });
});
