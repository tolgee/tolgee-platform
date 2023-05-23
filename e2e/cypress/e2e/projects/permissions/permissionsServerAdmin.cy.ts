import { login } from '../../../common/apiCalls/common';
import { HOST } from '../../../common/constants';
import {
  checkPermissions,
  RUN,
  visitProjectWithPermissions,
} from '../../../common/permissions/main';
import { visitProjectDashboard } from '../../../common/shared';

describe('Permissions admin', () => {
  it('admin', () => {
    visitProjectWithPermissions({ scopes: ['admin'] }).then((projectInfo) => {
      // login as admin
      login('admin', 'admin');
      visitProjectDashboard(projectInfo.project.id);

      // check if warning banner is present
      cy.gcy('administration-access-message').should('be.visible');
      cy.visit(HOST);
      cy.gcy('administration-access-message').should('be.visible');
      cy.visit(`${HOST}/organizations/admin-admin-com/profile`);
      cy.gcy('administration-access-message').should('be.visible');

      // check if user have correct access to everything
      visitProjectDashboard(projectInfo.project.id);
      checkPermissions(projectInfo, {
        'project-menu-item-dashboard': RUN,
        'project-menu-item-translations': RUN,
        'project-menu-item-settings': RUN,
        'project-menu-item-languages': RUN,
        'project-menu-item-members': RUN,
        'project-menu-item-import': RUN,
        'project-menu-item-export': RUN,
        'project-menu-item-integrate': RUN,
      });
    });
  });
});
