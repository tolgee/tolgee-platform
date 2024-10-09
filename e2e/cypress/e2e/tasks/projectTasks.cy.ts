import { login } from '../../common/apiCalls/common';
import { tasks } from '../../common/apiCalls/testData/testData';
import { waitForGlobalLoading } from '../../common/loading';
import { assertMessage, dismissMenu } from '../../common/shared';
import {
  checkTaskPreview,
  getTaskPreview,
  visitTasks,
} from '../../common/tasks';

describe('project tasks', () => {
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

  it('shows project tasks correctly', () => {
    cy.gcy('task-item').should('have.length', 2);
    cy.gcy('task-item').contains('Translate task').should('be.visible');
    cy.gcy('task-item').contains('Review task').should('be.visible');

    cy.gcy('task-item')
      .contains('Translate task')
      .closestDcy('task-item')
      .findDcy('task-item-detail')
      .click();

    cy.gcy('task-detail-keys').contains(2).should('be.visible');
    cy.gcy('task-detail-words').contains(4).should('be.visible');
    cy.gcy('task-detail-characters').contains(26).should('be.visible');
  });

  it('filters project tasks by type', () => {
    cy.gcy('task-item').should('have.length', 2);
    cy.gcy('tasks-header-filter-select').click();
    cy.gcy('tasks-filter-menu').contains('Translate').click();
    cy.gcy('task-item').should('have.length', 1);
    cy.gcy('task-item').contains('Translate task').should('exist');
  });

  it('filters project tasks by language', () => {
    cy.gcy('task-item').should('have.length', 2);
    cy.gcy('tasks-header-filter-select').click();
    cy.gcy('tasks-filter-menu').contains('Language').click();
    cy.gcy('language-select-popover').contains('Czech').click();
    cy.gcy('task-item').should('have.length', 1);
    cy.gcy('task-item').contains('Review task').should('exist');
  });

  it('filters project tasks by assignee', () => {
    cy.gcy('task-item').should('have.length', 2);
    cy.gcy('tasks-header-filter-select').click();
    cy.gcy('tasks-filter-menu').contains('Assignees').click();
    cy.gcy('assignee-search-select-popover').contains('Project user').click();
    cy.gcy('task-item').should('have.length', 1);
    cy.gcy('task-item').contains('Translate task').should('exist');
  });

  it('creates task from project tasks view', () => {
    cy.gcy('tasks-header-add-task').click();
    cy.gcy('create-task-field-type').click();
    cy.gcy('create-task-field-type-item').contains('Review').click();
    cy.gcy('create-task-field-name').type('New review task');
    cy.gcy('create-task-field-languages').click();
    cy.gcy('create-task-field-languages-item').contains('Czech').click();
    dismissMenu();
    cy.gcy('create-task-field-description').type(
      'This is task description ...'
    );
    getTaskPreview('Czech').findDcy('assignee-select').click();
    cy.gcy('assignee-search-select-popover')
      .contains('Organization member')
      .click();
    dismissMenu();

    cy.gcy('create-task-submit').click();

    assertMessage('1 task created');

    cy.gcy('task-label-name')
      .contains('New review task')
      .should('be.visible')
      .closestDcy('task-item')
      .findDcy('task-item-detail')
      .click();

    cy.gcy('task-detail-field-name')
      .find('input')
      .should('have.value', 'New review task');
    cy.gcy('assignee-select').should('contain', 'Organization member');
    cy.gcy('task-detail-author').should('contain', 'Tasks test user');
    cy.gcy('task-detail-created-at').should(
      'contain',
      new Date(Date.now()).getFullYear()
    );
    cy.gcy('task-detail-closed-at').should(
      'not.contain',
      new Date(Date.now()).getFullYear()
    );
    cy.gcy('task-detail-project').should('contain', 'Project with tasks');
  });

  it('task create displays correct numbers for translate task', () => {
    cy.gcy('tasks-header-add-task').click();
    cy.gcy('create-task-field-languages').click();
    cy.gcy('create-task-field-languages-item').contains('Czech').click();
    cy.gcy('create-task-field-languages-item').contains('English').click();
    dismissMenu();
    cy.waitForDom();

    checkTaskPreview({
      language: 'Czech',
      keys: 4,
      alert: false,
      words: 8,
      characters: 52,
    });
    checkTaskPreview({
      language: 'English',
      keys: 2,
      alert: true,
      words: 4,
      characters: 26,
    });
  });

  it('task create displays correct numbers for review task', () => {
    cy.gcy('tasks-header-add-task').click();
    cy.gcy('create-task-field-type').click();
    cy.gcy('create-task-field-type-item').contains('Review').click();
    cy.gcy('create-task-field-languages').click();
    cy.gcy('create-task-field-languages-item').contains('Czech').click();
    cy.gcy('create-task-field-languages-item').contains('English').click();
    dismissMenu();
    cy.waitForDom();

    checkTaskPreview({
      language: 'Czech',
      keys: 2,
      alert: true,
      words: 4,
      characters: 26,
    });
    checkTaskPreview({
      language: 'English',
      keys: 4,
      alert: false,
      words: 8,
      characters: 52,
    });
  });

  it('task create displays correct numbers for filter', () => {
    cy.gcy('tasks-header-add-task').click();
    cy.gcy('create-task-field-languages').click();
    cy.gcy('create-task-field-languages-item').contains('Czech').click();
    cy.gcy('create-task-field-languages-item').contains('English').click();
    dismissMenu();

    cy.gcy('translations-state-filter').click();
    cy.gcy('translations-state-filter-option').contains('Untranslated').click();
    dismissMenu();
    cy.waitForDom();

    checkTaskPreview({
      language: 'Czech',
      keys: 2,
      alert: false,
      words: 4,
      characters: 26,
    });
    checkTaskPreview({
      language: 'English',
      keys: 0,
      alert: false,
      words: 0,
      characters: 0,
    });
  });
});
