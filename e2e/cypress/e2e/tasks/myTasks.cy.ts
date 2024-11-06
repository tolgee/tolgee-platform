import { login } from '../../common/apiCalls/common';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';
import { tasks } from '../../common/apiCalls/testData/testData';
import {
  createComment,
  deleteComment,
  resolveComment,
} from '../../common/comments';
import { gcyAdvanced } from '../../common/shared';
import { getCell, setStateToReviewed } from '../../common/state';
import { visitMyTasks } from '../../common/tasks';
import { editCell } from '../../common/translations';
import { Sheet, WorkBook } from 'xlsx';
import { checkSheetProperty } from '../../common/xlsx';

describe('my tasks', () => {
  let testData: TestDataStandardResponse;
  const downloadsFolder = Cypress.config('downloadsFolder');
  beforeEach(() => {
    tasks.clean({ failOnStatusCode: false });
    tasks
      .generateStandard()
      .then((r) => r.body)
      .then((data) => {
        testData = data;
      });
  });

  function goToUserTasks(user: string) {
    login(
      testData.users.find((u) => [u.username, u.name].includes(user))?.username
    );
    visitMyTasks();
  }

  it('shows tasks for tasksTestUser', () => {
    goToUserTasks('tasksTestUser');
    cy.gcy('task-item').should('have.length', 2);
  });

  it('shows no tasks for Unrelated user', () => {
    goToUserTasks('Unrelated user');
    cy.gcy('task-item').should('have.length', 0);
  });

  it('shows tasks for Organization member', () => {
    goToUserTasks('Organization member');
    cy.gcy('task-item').should('have.length', 1);
    cy.gcy('task-item').contains('Review task').should('be.visible');
  });

  it('shows tasks for Project user', () => {
    goToUserTasks('Project user');
    cy.gcy('task-item').should('have.length', 1);
    cy.gcy('task-item').contains('Translate task').should('be.visible');
  });

  it('Project member can finish Translate task', () => {
    goToUserTasks('Project user');
    cy.gcy('task-label-name').contains('Translate task').click();
    editCell('Translation 0', 'New translation 0');
    getCell('Translation 1').findDcy('translations-cell-task-button').click();
    cy.get('#alert-dialog-title')
      .contains('All items in the task are finished')
      .should('be.visible');
    cy.gcy('global-confirmation-confirm').click();
    visitMyTasks();
    cy.gcy('task-item')
      .contains('Translate task')
      .closestDcy('task-item')
      .findDcy('task-state')
      .should('contain', 'Done');
  });

  it("Organization member can finish Review task (permissions elevated because he's assigned)", () => {
    goToUserTasks('Organization member');
    cy.gcy('task-label-name').contains('Review task').click();
    setStateToReviewed('Překlad 0');
    cy.waitForDom();
    getCell('Překlad 1').findDcy('translations-cell-task-button').click();
    cy.get('#alert-dialog-title')
      .contains('All items in the task are finished')
      .should('be.visible');
    cy.gcy('global-confirmation-confirm').click();
    visitMyTasks();
    cy.gcy('task-item')
      .contains('Review task')
      .closestDcy('task-item')
      .findDcy('task-state')
      .should('contain', 'Done');
  });

  it("Organization member can add comments (permissions elevated because he's assigned)", () => {
    goToUserTasks('Organization member');
    cy.gcy('task-label-name').contains('Review task').click();
    createComment('Test comment', 'key 0', 'cs');
    resolveComment('Test comment');
    deleteComment('Test comment');
  });

  it('shows stats correctly on half finished task', () => {
    goToUserTasks('Organization member');
    cy.gcy('task-label-name').contains('Review task').click();
    setStateToReviewed('Překlad 0');
    visitMyTasks();
    gcyAdvanced({ value: 'batch-progress', progress: '50' }).should('exist');
    cy.gcy('task-item-detail').click();
    gcyAdvanced({ value: 'task-detail-user-keys' }).should('contain', 1);
    gcyAdvanced({ value: 'task-detail-user-words' }).should('contain', 2);
    gcyAdvanced({ value: 'task-detail-user-characters' }).should('contain', 13);
  });

  it('generates report correctly', () => {
    goToUserTasks('Organization member');
    cy.gcy('task-label-name').contains('Review task').click();
    setStateToReviewed('Překlad 0');

    goToUserTasks('tasksTestUser');
    cy.gcy('task-label-name').contains('Review task').click();
    setStateToReviewed('Překlad 1');

    visitMyTasks();
    gcyAdvanced({ value: 'batch-progress', progress: '100' }).should('exist');
    cy.gcy('task-label-name')
      .contains('Review task')
      .closestDcy('task-item')
      .findDcy('task-item-detail')
      .click();
    cy.waitForDom();
    cy.gcy('task-detail-download-report').click();

    const fileName = 'review_task_report.xlsx';
    cy.verifyDownload(fileName);

    cy.task('readXlsx', downloadsFolder + '/' + fileName).then(
      (result: WorkBook) => {
        const sheet = Object.values(result.Sheets)[0] as Sheet;
        checkSheetProperty(sheet, 'Task name', 'Review task');
        checkSheetProperty(sheet, 'Type', 'Review');
        checkSheetProperty(sheet, 'Project name', 'Project with tasks');
        checkSheetProperty(sheet, 'Base language', 'English (en)');
        checkSheetProperty(sheet, 'Target language', 'Czech (cs)');
        checkSheetProperty(sheet, 'Created by', 'Project user');

        checkSheetProperty(sheet, 'Total to translate', '2'); // keys
        checkSheetProperty(sheet, 'Total to translate', '4', 2); // words
        checkSheetProperty(sheet, 'Total to translate', '26', 3); // characters

        checkSheetProperty(sheet, 'Tasks test user', '1'); // keys
        checkSheetProperty(sheet, 'Tasks test user', '2', 2); // words
        checkSheetProperty(sheet, 'Tasks test user', '13', 3); // characters

        checkSheetProperty(sheet, 'Organization member', '1'); // keys
        checkSheetProperty(sheet, 'Organization member', '2', 2); // words
        checkSheetProperty(sheet, 'Organization member', '13', 3); // characters
      }
    );
  });
});
