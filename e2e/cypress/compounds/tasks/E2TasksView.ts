import { HOST } from '../../common/constants';

export class E2TasksView {
  visit(projectId: number) {
    cy.visit(`${HOST}/projects/${projectId}/tasks`);
  }
}
