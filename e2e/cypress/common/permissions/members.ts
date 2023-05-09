import { ProjectInfo } from './shared';

export function testMembers(projectInfo: ProjectInfo) {
  const scopes = projectInfo.project.computedPermission.scopes;
  if (scopes.includes('members.edit')) {
    cy.gcy('invite-generate-button').should('be.visible');
  }
}
