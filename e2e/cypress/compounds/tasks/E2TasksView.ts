import { E2OrderTranslationDialog } from './E2OrderTranslationDialog';
import { HOST } from '../../common/constants';

export class E2TasksView {
  /**
   * Navigates to the Tasks page for the given project ID.
   */
  visit(projectId: number) {
    cy.visit(`${HOST}/projects/${projectId}/tasks`);
  }

  openOrderTranslationDialog() {
    cy.gcy('tasks-header-order-translation').click();
    return new E2OrderTranslationDialog();
  }
}
