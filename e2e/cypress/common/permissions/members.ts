import { ProjectInfo } from './shared';

export function testMembers(projectInfo: ProjectInfo) {
  const scopes = projectInfo.project.computedPermission.scopes;
  if (scopes.includes('members.edit')) {
    cy.gcy('invite-generate-button').should('be.visible');
    cy.gcy('project-member-revoke-button')
      .filter(':enabled')
      .should('have.length', 2);
    cy.gcy('permissions-menu-button')
      .filter(':enabled')
      .should('have.length', 2);
  }
}
