import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import {
  createKey,
  createProject,
  deleteProject,
  login,
  v2apiFetch,
} from '../../common/apiCalls/common';
import {
  selectLangsInLocalstorage,
  visitTranslations,
} from '../../common/translations';
import { waitForGlobalLoading } from '../../common/loading';
import { HOST } from '../../common/constants';

describe('Soft delete keys', () => {
  let project: ProjectDTO;
  let key1Id: number;
  let key2Id: number;
  let englishLanguageId: number;

  beforeEach(() => {
    login().then(() => {
      createProject({
        name: 'Soft delete test',
        languages: [{ tag: 'en', name: 'English', originalName: 'English' }],
      }).then((r) => {
        project = r.body as ProjectDTO;
        selectLangsInLocalstorage(project.id, ['en']);
        v2apiFetch(`projects/${project.id}/languages`).then((langRes) => {
          englishLanguageId = langRes.body._embedded.languages[0].id;
        });
        createKey(project.id, 'key1', { en: 'Key 1 translation' }).then(
          (k) => (key1Id = k.id)
        );
        createKey(project.id, 'key2', { en: 'Key 2 translation' }).then(
          (k) => (key2Id = k.id)
        );
      });
    });
  });

  afterEach(() => {
    if (project) {
      deleteProject(project.id);
    }
  });

  it('shows trash icon after soft-deleting a key', () => {
    // Soft-delete key1 via API
    v2apiFetch(`projects/${project.id}/keys/${key1Id}`, {
      method: 'DELETE',
    });

    visitTranslations(project.id);
    waitForGlobalLoading();

    // key1 should not be visible in the translations view
    cy.contains('key1').should('not.exist');
    // key2 should still be visible
    cy.contains('key2').should('be.visible');

    // Trash icon should be visible
    cy.gcy('translations-trash-button').should('be.visible');
  });

  it('trash icon is hidden when no keys are soft-deleted', () => {
    visitTranslations(project.id);
    waitForGlobalLoading();

    cy.contains('key1').should('be.visible');
    cy.contains('key2').should('be.visible');

    // Trash icon should not exist
    cy.gcy('translations-trash-button').should('not.exist');
  });

  it('shows trashed keys in trash page', () => {
    // Soft-delete key1
    v2apiFetch(`projects/${project.id}/keys/${key1Id}`, {
      method: 'DELETE',
    });

    cy.visit(`${HOST}/projects/${project.id}/translations/trash`);
    waitForGlobalLoading();

    // key1 should be in trash
    cy.gcy('trash-row').should('have.length', 1);
    cy.gcy('trash-row').contains('key1').should('be.visible');
  });

  it('restores a key from trash', () => {
    // Soft-delete key1
    v2apiFetch(`projects/${project.id}/keys/${key1Id}`, {
      method: 'DELETE',
    });

    cy.visit(`${HOST}/projects/${project.id}/translations/trash`);
    waitForGlobalLoading();

    // Click restore button
    cy.gcy('trash-row').contains('key1').should('be.visible');
    cy.gcy('trash-restore-button').click();
    waitForGlobalLoading();

    // Trash should be empty
    cy.gcy('trash-row').should('not.exist');

    // key1 should be back in translations
    visitTranslations(project.id);
    waitForGlobalLoading();
    cy.contains('key1').should('be.visible');
    cy.contains('key2').should('be.visible');
  });

  it('allows trashing a key with the same name as an already-trashed key', () => {
    // Soft-delete key1 via API
    v2apiFetch(`projects/${project.id}/keys/${key1Id}`, {
      method: 'DELETE',
    });

    // Create a new key1 with the same name and soft-delete it too
    createKey(project.id, 'key1', { en: 'New key 1 translation' }).then(
      (newKey) => {
        v2apiFetch(`projects/${project.id}/keys/${newKey.id}`, {
          method: 'DELETE',
        });
      }
    );

    // Translations view should only show key2
    visitTranslations(project.id);
    waitForGlobalLoading();
    cy.contains('key1').should('not.exist');
    cy.contains('key2').should('be.visible');

    // Trash should contain both trashed key1 entries
    cy.visit(`${HOST}/projects/${project.id}/translations/trash`);
    waitForGlobalLoading();

    cy.gcy('trash-row').should('have.length', 2);
    cy.gcy('trash-row').filter(':contains("key1")').should('have.length', 2);
  });

  it('permanently deletes a key from trash', () => {
    // Soft-delete key1
    v2apiFetch(`projects/${project.id}/keys/${key1Id}`, {
      method: 'DELETE',
    });

    cy.visit(`${HOST}/projects/${project.id}/translations/trash`);
    waitForGlobalLoading();

    // Click permanent delete button
    cy.gcy('trash-row').contains('key1').should('be.visible');
    cy.gcy('trash-permanent-delete-button').click();
    // Confirm the dialog
    cy.gcy('global-confirmation-confirm').click();
    waitForGlobalLoading();

    // Trash should be empty
    cy.gcy('trash-row').should('not.exist');

    // key1 should not be in translations either
    visitTranslations(project.id);
    waitForGlobalLoading();
    cy.contains('key1').should('not.exist');
    cy.contains('key2').should('be.visible');
  });

  it('soft-deleted key is excluded from task and restored key reappears', () => {
    // 1. Create a translate task with both keys
    v2apiFetch(`projects/${project.id}/tasks`, {
      method: 'POST',
      body: JSON.stringify({
        name: 'Test translate task',
        type: 'TRANSLATE',
        languageId: englishLanguageId,
        assignees: [],
        keys: [key1Id, key2Id],
      }),
    });

    // 2. Verify task shows 2 keys
    cy.visit(`${HOST}/projects/${project.id}/tasks`);
    waitForGlobalLoading();
    cy.gcy('task-item')
      .contains('Test translate task')
      .closestDcy('task-item')
      .findDcy('task-item-detail')
      .click();
    cy.gcy('task-detail-keys').should('contain', 2);

    // 3. Soft-delete key1
    v2apiFetch(`projects/${project.id}/keys/${key1Id}`, {
      method: 'DELETE',
    });

    // 4. Revisit tasks — task should now show 1 key
    cy.visit(`${HOST}/projects/${project.id}/tasks`);
    waitForGlobalLoading();
    cy.gcy('task-item')
      .contains('Test translate task')
      .closestDcy('task-item')
      .findDcy('task-item-detail')
      .click();
    cy.gcy('task-detail-keys').should('contain', 1);

    // 5. Restore key1 from trash
    v2apiFetch(`projects/${project.id}/keys/trash/${key1Id}/restore`, {
      method: 'PUT',
    });

    // 6. Revisit tasks — task should show 2 keys again
    cy.visit(`${HOST}/projects/${project.id}/tasks`);
    waitForGlobalLoading();
    cy.gcy('task-item')
      .contains('Test translate task')
      .closestDcy('task-item')
      .findDcy('task-item-detail')
      .click();
    cy.gcy('task-detail-keys').should('contain', 2);
  });
});
