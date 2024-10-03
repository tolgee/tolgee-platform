import {
  getAssignedEmailNotification,
  login,
} from '../../common/apiCalls/common';
import { tasks } from '../../common/apiCalls/testData/testData';
import { waitForGlobalLoading } from '../../common/loading';
import { assertMessage, dismissMenu } from '../../common/shared';
import { getTaskPreview, visitTasks } from '../../common/tasks';

describe('tasks notifications', () => {
  beforeEach(() => {
    tasks.clean({ failOnStatusCode: false });
    tasks
      .generateStandard()
      .then((r) => r.body)
      .then(({ users, projects }) => {
        login(users[0].username);
        const testProject = projects.find(
          ({ name }) => name === 'Project with tasks'
        );
        visitTasks(testProject.id);
      });
    waitForGlobalLoading();
  });

  it('sends email to assignee of newly created task', () => {
    cy.gcy('tasks-header-add-task').click();
    cy.gcy('create-task-field-name').type('New review task');
    cy.gcy('create-task-field-languages').click();
    cy.gcy('create-task-field-languages-item').contains('Czech').click();
    dismissMenu();
    getTaskPreview('Czech').findDcy('assignee-select').click();
    cy.gcy('assignee-search-select-popover')
      .contains('Organization member')
      .click();
    dismissMenu();

    cy.gcy('create-task-submit').click();

    assertMessage('1 task created');

    getAssignedEmailNotification().then(({ taskLink, toAddress }) => {
      assert(toAddress === 'organization.member@test.com', 'correct recipient');
      cy.visit(taskLink);
    });
    cy.gcy('task-detail')
      .should('be.visible')
      .findDcy('task-label-name')
      .should('contain', 'New review task');
  });

  it('sends email to new assignee', () => {
    cy.gcy('task-item')
      .contains('Translate task')
      .closestDcy('task-item')
      .findDcy('task-item-detail')
      .click();

    waitForGlobalLoading();
    cy.gcy('task-detail').findDcy('assignee-select').click();
    cy.gcy('user-switch-item').contains('Organization member').click();
    dismissMenu();
    cy.gcy('task-detail-submit').click();

    assertMessage('Task updated sucessfully');

    getAssignedEmailNotification().then(({ toAddress, myTasksLink }) => {
      assert(toAddress === 'organization.member@test.com', 'correct recipient');
      cy.visit(myTasksLink);
    });
    cy.gcy('global-base-view-title').should('contain', 'My tasks');
    cy.gcy('task-item').contains('Translate task').should('be.visible');
  });
});
