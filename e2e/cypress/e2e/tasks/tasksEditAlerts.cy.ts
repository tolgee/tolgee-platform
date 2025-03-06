import { login } from '../../common/apiCalls/common';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';
import { tasks } from '../../common/apiCalls/testData/testData';
import { visitMyTasks, visitTasks } from '../../common/tasks';
import { getCell, visitTranslations } from '../../common/translations';

describe('tasks edit alerts', () => {
  let testData: TestDataStandardResponse;
  beforeEach(() => {
    tasks.clean({ failOnStatusCode: false });
    tasks
      .generate()
      .then((r) => r.body)
      .then((data) => {
        testData = data;
      });
  });

  function loginAsUser(user: string) {
    login(
      testData.users.find((u) => [u.username, u.name].includes(user))?.username
    );
  }
  function goToProject(name: string) {
    visitTranslations(testData.projects.find((p) => p.name === name)?.id);
  }

  it('user not assigned to a task', () => {
    loginAsUser('Organization owner');
    goToProject('Project with tasks');
    getCell('Překlad 1').click();

    cy.gcy('task-info-message').contains('not assigned to you');
  });

  it('user assigned to task, but not in task view', () => {
    loginAsUser('Tasks test user');
    goToProject('Project with tasks');
    getCell('Překlad 1').click();

    cy.gcy('task-info-message').contains('This is part of a review task');
  });

  it('info about blocked task', () => {
    loginAsUser('Tasks test user');
    visitMyTasks();
    cy.gcy('task-item').contains('Blocked task').click();
    getCell('Translation 1').click();
    cy.gcy('task-info-message').contains('is blocked');
  });

  it('info about finished task', () => {
    loginAsUser('admin');
    visitTasks(
      testData.projects.find((p) => p.name === 'Project with tasks')?.id
    );

    cy.gcy('task-item').contains('Finished review task').click();

    getCell('Translation 1').click();
    cy.gcy('task-info-message').contains('is finished');
  });

  it('info about canceled task', () => {
    loginAsUser('admin');
    visitTasks(
      testData.projects.find((p) => p.name === 'Project with tasks')?.id
    );

    cy.gcy('task-item').contains('Canceled review task').click();

    getCell('Translation 1').click();
    cy.gcy('task-info-message').contains('is canceled');
  });
});
