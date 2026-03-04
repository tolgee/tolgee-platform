import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import {
  createProject,
  deleteProject,
  login,
} from '../../common/apiCalls/common';
import {
  deleteSelected,
  selectAll,
  selectOperation,
} from '../../common/batchOperations';
import { selectLangsInLocalstorage } from '../../common/translations';
import { waitForGlobalLoading } from '../../common/loading';
import { assertMessage, dismissMenu } from '../../common/shared';
import { E2TrashSection } from '../../compounds/E2TrashSection';
import { E2TranslationsView } from '../../compounds/E2TranslationsView';
import { E2TasksView } from '../../compounds/tasks/E2TasksView';

describe('Soft delete keys', () => {
  let project: ProjectDTO;

  const trash = new E2TrashSection();
  const translations = new E2TranslationsView();
  const tasks = new E2TasksView();

  beforeEach(() => {
    login().then(() => {
      createProject({
        name: 'Soft delete test',
        languages: [
          { tag: 'en', name: 'English', originalName: 'English' },
          { tag: 'cs', name: 'Czech', originalName: 'Czech' },
        ],
      }).then((r) => {
        project = r.body as ProjectDTO;
        selectLangsInLocalstorage(project.id, ['en']);
      });
    });
  });

  afterEach(() => {
    if (project) {
      deleteProject(project.id);
    }
  });

  function createTwoKeys() {
    translations.visit(project.id);
    waitForGlobalLoading();
    translations.createKey({ key: 'key1', translation: 'Key 1 translation' });
    waitForGlobalLoading();
    translations.createKey({ key: 'key2', translation: 'Key 2 translation' });
    waitForGlobalLoading();
  }

  function softDeleteKey(keyName: string) {
    cy.contains('[data-cy="translations-row"]', keyName)
      .findDcy('translations-row-checkbox')
      .click();
    deleteSelected();
  }

  it('shows trash icon after soft-deleting a key', () => {
    createTwoKeys();

    // Soft-delete key1 via UI
    softDeleteKey('key1');

    // key1 should not be visible in the translations view
    cy.contains('key1').should('not.exist');
    // key2 should still be visible
    cy.contains('key2').should('be.visible');

    // Trash icon should be visible
    cy.gcy('translations-trash-button').should('be.visible');
  });

  it('trash icon is hidden when no keys are soft-deleted', () => {
    createTwoKeys();

    cy.contains('key1').should('be.visible');
    cy.contains('key2').should('be.visible');

    // Trash icon should not exist
    cy.gcy('translations-trash-button').should('not.exist');
  });

  it('shows trashed keys in trash page', () => {
    createTwoKeys();

    // Soft-delete key1
    softDeleteKey('key1');

    trash.visit(project.id);

    // key1 should be in trash
    trash.assertTrashRowCount(1);
    trash.getTrashRows().contains('key1').should('be.visible');
  });

  it('restores a key from trash', () => {
    createTwoKeys();

    // Soft-delete key1
    softDeleteKey('key1');

    trash.visit(project.id);

    // Restore key1
    trash.restoreKey('key1');

    // Trash should be empty
    trash.assertTrashEmpty();

    // key1 should be back in translations
    translations.visit(project.id);
    waitForGlobalLoading();
    cy.contains('key1').should('be.visible');
    cy.contains('key2').should('be.visible');
  });

  it('allows trashing a key with the same name as an already-trashed key', () => {
    createTwoKeys();

    // Soft-delete key1
    softDeleteKey('key1');

    // Create a new key1 with the same name
    translations.createKey({
      key: 'key1',
      translation: 'New key 1 translation',
    });
    waitForGlobalLoading();

    // Soft-delete the new key1
    softDeleteKey('key1');

    // Translations view should only show key2
    cy.contains('key1').should('not.exist');
    cy.contains('key2').should('be.visible');

    // Trash should contain both trashed key1 entries
    trash.visit(project.id);

    trash.assertTrashRowCount(2);
    trash.getTrashRows().filter(':contains("key1")').should('have.length', 2);
  });

  it('permanently deletes a key from trash', () => {
    createTwoKeys();

    // Soft-delete key1
    softDeleteKey('key1');

    trash.visit(project.id);

    // Permanently delete key1
    trash.permanentlyDeleteKey('key1');

    // Trash should be empty
    trash.assertTrashEmpty();

    // key1 should not be in translations either
    translations.visit(project.id);
    waitForGlobalLoading();
    cy.contains('key1').should('not.exist');
    cy.contains('key2').should('be.visible');
  });

  it('soft-deleted key is excluded from task and restored key reappears', () => {
    createTwoKeys();

    // 1. Create a translate task with both keys via UI
    selectAll();
    selectOperation('Create task');
    cy.gcy('create-task-field-name').type('Test translate task');
    cy.gcy('create-task-field-languages').click();
    cy.gcy('create-task-field-languages-item').contains('Czech').click();
    dismissMenu();
    cy.gcy('create-task-submit').click();
    assertMessage('1 task created');
    waitForGlobalLoading();

    // 2. Verify task shows 2 keys
    tasks.visit(project.id);
    waitForGlobalLoading();
    cy.gcy('task-item')
      .contains('Test translate task')
      .closestDcy('task-item')
      .findDcy('task-item-detail')
      .click();
    cy.gcy('task-detail-keys').should('contain', 2);

    // 3. Soft-delete key1 via UI
    translations.visit(project.id);
    waitForGlobalLoading();
    softDeleteKey('key1');

    // 4. Revisit tasks — task should now show 1 key
    tasks.visit(project.id);
    waitForGlobalLoading();
    cy.gcy('task-item')
      .contains('Test translate task')
      .closestDcy('task-item')
      .findDcy('task-item-detail')
      .click();
    cy.gcy('task-detail-keys').should('contain', 1);

    // 5. Restore key1 from trash via UI
    trash.visit(project.id);
    trash.restoreKey('key1');

    // 6. Revisit tasks — task should show 2 keys again
    tasks.visit(project.id);
    waitForGlobalLoading();
    cy.gcy('task-item')
      .contains('Test translate task')
      .closestDcy('task-item')
      .findDcy('task-item-detail')
      .click();
    cy.gcy('task-detail-keys').should('contain', 2);
  });
});
