import { dismissMenu } from '../shared';
import { visitTranslations } from '../translations';
import { pageAcessibleWithoutErrors, ProjectInfo } from './shared';

export function testMyTasks(projectInfo: ProjectInfo) {
  cy.gcy('global-user-menu-button').click();
  cy.gcy('user-menu-my-tasks').click();
  cy.gcy('task-item-detail').click();
  pageAcessibleWithoutErrors();

  const scopes = projectInfo.project.computedPermission.scopes;
  if (scopes.includes('tasks.edit')) {
    cy.gcy('task-detail-field-name').get('input').should('be.enabled');
  } else {
    cy.gcy('task-detail-field-name').get('input').should('be.disabled');
  }
  dismissMenu();
  visitTranslations(projectInfo.project.id);
}
