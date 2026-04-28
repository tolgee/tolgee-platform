import {
  translationMemoryTestData,
  getOrganizationByNameFromTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';
import { HOST } from '../../common/constants';
import { gcy } from '../../common/shared';
import { E2TranslationMemoriesView } from '../../compounds/translationMemories/E2TranslationMemoriesView';

// The list item now also renders the names of every assigned project (Used-in-projects
// header), so a plain `:contains(name)` matches every TM whose project list mentions the
// name. Filter on the TM-name child specifically to pick a single TM by its own name.
const tmListItem = (name: string) =>
  gcy('translation-memory-list-item').filter(
    `:has([data-cy="translation-memory-list-name"]:contains("${name}"))`
  );

describe('Translation Memory org settings', () => {
  let data: TestDataStandardResponse;

  beforeEach(() => {
    translationMemoryTestData.clean();
    translationMemoryTestData.generateStandard().then((res) => {
      data = res.body;
    });
  });

  afterEach(() => {
    translationMemoryTestData.clean();
  });

  // --- Navigation ---

  it('navigates from org settings sidebar', () => {
    login('test_username');
    const organization = getOrganizationByNameFromTestData(
      data,
      'test_username'
    );
    cy.visit(`${HOST}/organizations/${organization.slug}/profile`);

    gcy('settings-menu-item')
      .filter(':contains("Translation memories")')
      .click();

    cy.url().should('include', '/translation-memories');
    gcy('translation-memory-list-item').should('have.length.at.least', 1);
  });

  // --- List ---

  it('lists both shared and project translation memories', () => {
    login('test_username');
    const view = new E2TranslationMemoriesView();
    view.findAndVisit(data, 'test_username');

    tmListItem('Shared Marketing TM').should('be.visible');
    tmListItem('Project With TM').should('be.visible');
  });

  it('shows type badges and assigned projects', () => {
    login('test_username');
    const view = new E2TranslationMemoriesView();
    view.findAndVisit(data, 'test_username');

    // Shared TM shows "Shared" badge and project name
    tmListItem('Shared Marketing TM')
      .should('contain', 'Shared')
      .should('contain', 'Project With TM');

    // Unassigned TM shows "No project"
    tmListItem('Unassigned Shared TM').should('contain', 'No project');

    // Project TM shows "Project only" badge
    tmListItem('Project With TM').should('contain', 'Project only');
  });

  // Project TMs surface their virtual entries (computed from the project translations) in
  // the content browser. The list-side entry count must include those, otherwise users see
  // "0 entries" on the list while the content view shows N rows. Backend has unit coverage;
  // this test guards the same invariant end-to-end.
  it('list entry count matches the content row count for project TMs', () => {
    login('test_username');
    const view = new E2TranslationMemoriesView();
    view.findAndVisit(data, 'test_username');

    // Drive the assertion off the content view first — its row count is the ground truth
    // (real DOM, not a derived plural string), and Cypress's auto-retry on the row count
    // gives the entries query time to land before we compare. Then go back to the list
    // and assert the displayed count agrees.
    view.openTm('Project With TM');
    gcy('translation-memory-entry-row')
      .should('have.length.at.least', 1)
      .its('length')
      .then((rowCount) => {
        cy.go('back');
        tmListItem('Project With TM')
          .findDcy('translation-memory-list-entries-count')
          .should('contain', String(rowCount));
      });
  });

  // --- Create ---

  it('creates a new translation memory', () => {
    login('test_username');
    const view = new E2TranslationMemoriesView();
    view.findAndVisit(data, 'test_username');

    const dialog = view.openCreateDialog(false);
    dialog.setName('New Test TM');
    dialog.setBaseLanguage('English');
    dialog.submit();

    tmListItem('New Test TM').should('be.visible');
  });

  it('creates a TM with assigned project', () => {
    login('test_username');
    const view = new E2TranslationMemoriesView();
    view.findAndVisit(data, 'test_username');

    const dialog = view.openCreateDialog(false);
    dialog.setName('Assigned TM');
    // Base language must be picked first — once a project is assigned, the form locks the
    // base-language dropdown (the chosen language must match the project's base) and the
    // project picker filters out projects with a different base language.
    dialog.setBaseLanguage('English');
    dialog.toggleAssignedProject('Project With TM');
    dialog.submit();

    tmListItem('Assigned TM')
      .should('be.visible')
      .should('contain', 'Project With TM');
  });

  // --- Settings ---

  it('opens settings dialog and edits TM name', () => {
    login('test_username');
    const view = new E2TranslationMemoriesView();
    view.findAndVisit(data, 'test_username');

    view.openSettingsDialog('Shared Marketing TM');

    gcy('create-translation-memory-field-name')
      .find('input')
      .clear()
      .type('Renamed TM');
    gcy('create-edit-translation-memory-submit').click();
    gcy('tm-settings-dialog').should('not.exist');

    tmListItem('Renamed TM').should('be.visible');
  });

  it('write-only-reviewed is locked in shared TM settings', () => {
    // The flag determines how stored entries are seeded into a shared TM, so flipping it
    // mid-life would leave inconsistent state. Form locks the switch in edit mode for
    // SHARED TMs — guard the lock here so a future regression that re-enables editing
    // is caught.
    login('test_username');
    const view = new E2TranslationMemoriesView();
    view.findAndVisit(data, 'test_username');

    view.openSettingsDialog('Shared Marketing TM');

    gcy('tm-settings-write-only-reviewed').find('input').should('be.disabled');
  });

  // --- Delete ---

  it('deletes a shared translation memory', () => {
    login('test_username');
    const view = new E2TranslationMemoriesView();
    view.findAndVisit(data, 'test_username');

    view.deleteTranslationMemory('Shared Marketing TM');

    cy.contains('Shared Marketing TM').should('not.exist');
  });

  it('cannot delete a project translation memory', () => {
    login('test_username');
    const view = new E2TranslationMemoriesView();
    view.findAndVisit(data, 'test_username');

    // Project TMs expose the kebab menu (for the reviewed-only toggle) but the
    // Delete option is hidden — the project owns the TM and it cannot be dropped.
    tmListItem('Project With TM')
      .findDcy('translation-memories-list-more-button')
      .click();
    gcy('translation-memory-edit-button').should('be.visible');
    gcy('translation-memory-delete-button').should('not.exist');
  });

  // --- Permissions ---

  it('member cannot manage translation memories', () => {
    login('tm_org_member');
    const view = new E2TranslationMemoriesView();
    view.findAndVisit(data, 'test_username');

    tmListItem('Shared Marketing TM')
      .findDcy('translation-memories-list-more-button')
      .should('not.exist');
  });
});
