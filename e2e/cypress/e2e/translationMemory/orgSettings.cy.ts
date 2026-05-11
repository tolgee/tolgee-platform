import {
  translationMemoryTestData,
  getOrganizationByNameFromTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';
import { HOST } from '../../common/constants';
import { E2TranslationMemoriesView } from '../../compounds/translationMemories/E2TranslationMemoriesView';
import { E2TranslationMemoryView } from '../../compounds/translationMemories/E2TranslationMemoryView';

describe('Translation Memory org settings', () => {
  let data: TestDataStandardResponse;
  const view = new E2TranslationMemoriesView();
  const tmView = new E2TranslationMemoryView();

  beforeEach(() => {
    translationMemoryTestData.clean();
    translationMemoryTestData.generateStandard().then((res) => {
      data = res.body;
    });
  });

  afterEach(() => {
    translationMemoryTestData.clean();
  });

  describe('Navigation & Permissions', () => {
    it('navigates from org settings sidebar', () => {
      login('test_username');
      const organization = getOrganizationByNameFromTestData(
        data,
        'test_username'
      );
      cy.visit(`${HOST}/organizations/${organization.slug}/profile`);

      view.clickSidebarEntry();

      cy.url().should('include', '/translation-memories');
      view.getListItems().should('have.length.at.least', 1);
    });

    it('member cannot manage translation memories', () => {
      login('tm_org_member');
      view.findAndVisit(data, 'test_username');

      view.getMoreButtonFor('Shared Marketing TM').should('not.exist');
    });
  });

  describe('List', () => {
    it('lists both shared and project translation memories', () => {
      login('test_username');
      view.findAndVisit(data, 'test_username');

      view.getListItem('Shared Marketing TM').should('be.visible');
      view.getListItem('Project With TM').should('be.visible');
    });

    it('shows type badges and assigned projects', () => {
      login('test_username');
      view.findAndVisit(data, 'test_username');

      view
        .getListItem('Shared Marketing TM')
        .should('contain', 'Shared')
        .should('contain', 'Project With TM');
      view.getListItem('Unassigned Shared TM').should('contain', 'No project');
      view.getListItem('Project With TM').should('contain', 'Project only');
    });

    // Project TMs surface their virtual entries (computed from the project translations) in
    // the content browser. The list-side count must include those, otherwise users see
    // "0 entries" on the list while the content view shows N rows. Backend has unit
    // coverage; this test guards the same invariant end-to-end.
    it('list entry count matches the content row count for project TMs', () => {
      login('test_username');
      view.findAndVisit(data, 'test_username');

      // Drive the assertion off the content view's row count — that's the ground truth
      // (real DOM, not a derived plural string) and Cypress's auto-retry on the row count
      // gives the entries query time to land before we compare.
      view.openTm('Project With TM');
      tmView
        .getEntryRows()
        .should('have.length.at.least', 1)
        .its('length')
        .then((rowCount) => {
          cy.go('back');
          view
            .getEntriesCountFor('Project With TM')
            .should('contain', String(rowCount));
        });
    });

    // Same invariant for SHARED TMs that draw virtual content from a write-access-assigned
    // project: the list count must include those virtual rows, not just stored entries.
    // "Shared TM with default penalty" is assigned to "Project With TM" with default
    // writeAccess=true and has 1 stored entry — exercises the union path.
    it('list entry count matches the content row count for shared TMs with write-access projects', () => {
      login('test_username');
      view.findAndVisit(data, 'test_username');

      view.openTm('Shared TM with default penalty');
      tmView
        .getEntryRows()
        .should('have.length.at.least', 1)
        .its('length')
        .then((rowCount) => {
          cy.go('back');
          view
            .getEntriesCountFor('Shared TM with default penalty')
            .should('contain', String(rowCount));
        });
    });

    // "Multi-project shared TM" has two write-access projects ("Project With TM" and
    // "Conflict source project") that both translate "Existing source" but with different
    // German targets, plus a manual stored entry ("Manual override") on the same source.
    // The three contributions must surface as three separate rows: one manual + two
    // virtuals, each carrying its own variant text. Virtual rows expose the originating
    // project's key reference.
    it('renders one candidate row per project when two projects translate the same source differently', () => {
      login('test_username');
      view.findAndVisit(data, 'test_username');

      view.openTm('Multi-project shared TM');

      tmView.getEntryRowContaining('Existing source').should('have.length', 3);

      // Each variant's specific German target is visible.
      tmView.getEntryRowContaining('Manual override').should('have.length', 1);
      tmView
        .getEntryRowContaining('Bestehende Übersetzung')
        .should('have.length.at.least', 1);
      tmView
        .getEntryRowContaining('Bestehende Übersetzung aus Konfliktprojekt')
        .should('have.length', 1);

      // Each virtual row exposes its originating project's key name.
      cy.contains('[data-cy="tm-entry-row-keys"]', 'existing-key').should(
        'be.visible'
      );
      cy.contains('[data-cy="tm-entry-row-keys"]', 'shared-greeting').should(
        'be.visible'
      );
    });
  });

  describe('Create', () => {
    it('creates a new translation memory', () => {
      login('test_username');
      view.findAndVisit(data, 'test_username');

      const dialog = view.openCreateDialog(false);
      dialog.setName('New Test TM');
      dialog.setBaseLanguage('English');
      dialog.submit();

      view.getListItem('New Test TM').should('be.visible');
    });

    it('creates a TM with assigned project', () => {
      login('test_username');
      view.findAndVisit(data, 'test_username');

      const dialog = view.openCreateDialog(false);
      dialog.setName('Assigned TM');
      // Base language must be picked first — once a project is assigned, the form locks
      // the base-language dropdown (it must match the project's base) and the project
      // picker filters out projects whose base differs.
      dialog.setBaseLanguage('English');
      dialog.toggleAssignedProject('Project With TM');
      dialog.submit();

      view
        .getListItem('Assigned TM')
        .should('be.visible')
        .should('contain', 'Project With TM');
    });
  });

  describe('Settings', () => {
    it('opens settings dialog and edits TM name', () => {
      login('test_username');
      view.findAndVisit(data, 'test_username');

      const dialog = view.openSettingsDialog('Shared Marketing TM');
      dialog.clearAndSetName('Renamed TM');
      dialog.submit();
      view.getSettingsDialog().should('not.exist');

      view.getListItem('Renamed TM').should('be.visible');
    });

    // Post-unification all TM content surfaces virtually from write-access projects, so
    // the write-only-reviewed flag only filters the virtual half; flipping it mid-life is
    // safe and the switch must be editable on shared TMs too.
    it('write-only-reviewed switch is editable in shared TM settings', () => {
      login('test_username');
      view.findAndVisit(data, 'test_username');

      const dialog = view.openSettingsDialog('Shared Marketing TM');
      dialog
        .getWriteOnlyReviewedSwitch()
        .find('input')
        .should('not.be.disabled');
    });
  });

  describe('Delete', () => {
    it('deletes a shared translation memory', () => {
      login('test_username');
      view.findAndVisit(data, 'test_username');

      view.deleteTranslationMemory('Shared Marketing TM');

      view.getListItem('Shared Marketing TM').should('not.exist');
    });

    // Project TMs expose the kebab menu (for the reviewed-only toggle) but the Delete
    // option is hidden — the project owns the TM and it cannot be dropped.
    it('cannot delete a project translation memory', () => {
      login('test_username');
      view.findAndVisit(data, 'test_username');

      view.openMenu('Project With TM');
      view.getEditMenuItem().should('be.visible');
      view.getDeleteMenuItem().should('not.exist');
    });
  });
});
