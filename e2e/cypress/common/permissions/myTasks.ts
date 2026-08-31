import { waitForGlobalLoading } from '../loading';
import { dismissMenu } from '../shared';
import { visitTranslations } from '../translations';
import { pageAcessibleWithoutErrors, ProjectInfo } from './shared';

export function testMyTasks(projectInfo: ProjectInfo) {
  const scopes = projectInfo.project.computedPermission.scopes;
  const canReachAnAssignedTask =
    scopes.includes('tasks.view') || scopes.includes('tasks.assigned-access');

  cy.gcy('global-user-menu-button').click();
  cy.gcy('user-menu-my-tasks').click();
  cy.gcy('task-item-detail').click();

  if (!canReachAnAssignedTask) {
    // A refused task navigates the user away
    waitForGlobalLoading();
    cy.gcy('task-detail-field-name').should('not.exist');
    visitTranslations(projectInfo.project.id);
    return;
  }

  pageAcessibleWithoutErrors();

  if (scopes.includes('tasks.edit')) {
    cy.gcy('task-detail-field-name').get('input').should('be.enabled');
  } else {
    cy.gcy('task-detail-field-name').get('input').should('be.disabled');
  }
  dismissMenu();
  visitTranslations(projectInfo.project.id);
}
