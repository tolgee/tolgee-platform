import { dismissMenu } from '../shared';
import { visitTranslations } from '../translations';
import { pageAcessibleWithoutErrors, ProjectInfo } from './shared';

export function testMyTasks(projectInfo: ProjectInfo) {
  const scopes = projectInfo.project.computedPermission.scopes;
  // Opening an assigned task needs tasks.view, or tasks.assigned-access for the assignee elevation. Role presets
  // grant the latter, a granular permission only when it was picked explicitly — without either there is no
  // reachable task to open.
  if (
    !scopes.includes('tasks.view') &&
    !scopes.includes('tasks.assigned-access')
  ) {
    return;
  }

  cy.gcy('global-user-menu-button').click();
  cy.gcy('user-menu-my-tasks').click();
  cy.gcy('task-item-detail').click();
  pageAcessibleWithoutErrors();

  if (scopes.includes('tasks.edit')) {
    cy.gcy('task-detail-field-name').get('input').should('be.enabled');
  } else {
    cy.gcy('task-detail-field-name').get('input').should('be.disabled');
  }
  dismissMenu();
  visitTranslations(projectInfo.project.id);
}
