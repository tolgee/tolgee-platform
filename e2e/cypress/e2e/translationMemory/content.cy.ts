import 'cypress-file-upload';
import { translationMemoryTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';
import { E2TranslationMemoriesView } from '../../compounds/translationMemories/E2TranslationMemoriesView';
import { E2TranslationMemoryView } from '../../compounds/translationMemories/E2TranslationMemoryView';

describe('Translation Memory content browser', () => {
  let data: TestDataStandardResponse;
  const listView = new E2TranslationMemoriesView();
  const tmView = new E2TranslationMemoryView();

  beforeEach(() => {
    translationMemoryTestData.clean();
    translationMemoryTestData.generateStandard().then((res) => {
      data = res.body;
      login('test_username');
    });
  });

  afterEach(() => {
    translationMemoryTestData.clean();
  });

  it('navigates to TM content by clicking list item', () => {
    listView.findAndVisitTm(data, 'test_username', 'Shared Marketing TM');

    cy.url().should('include', '/translation-memories/');
    cy.contains('Shared Marketing TM').should('be.visible');
    cy.contains('Translation memories').should('be.visible');
  });

  it('shows entries grouped by source text', () => {
    listView.findAndVisitTm(data, 'test_username', 'Shared Marketing TM');

    tmView.getEntryRows().should('have.length.at.least', 1);
    tmView.getEntryRowContaining('Hello world').should('be.visible');
    tmView.getEntryRowContaining('Thank you').should('be.visible');
  });

  it('search filters entries', () => {
    listView.findAndVisitTm(data, 'test_username', 'Shared Marketing TM');

    tmView.getEntryRows().should('have.length', 2);

    tmView.search('Hello');
    tmView.getEntryRows().should('have.length', 1);
    tmView.getEntryRowContaining('Hello world').should('be.visible');
  });

  it('shows empty state for TM with no entries', () => {
    listView.findAndVisitTm(data, 'test_username', 'Unassigned Shared TM');

    tmView.getEntryRows().should('not.exist');
  });

  describe('Inline editing', () => {
    it('saves an edit on confirm', () => {
      listView.findAndVisitTm(data, 'test_username', 'Shared Marketing TM');

      tmView.clickTranslationCell('Hallo Welt');
      tmView.editFieldType('Hallo Welt!');
      tmView.saveEdit();

      tmView.getEntryRowContaining('Hallo Welt!').should('be.visible');
    });

    it('restores original text on cancel', () => {
      listView.findAndVisitTm(data, 'test_username', 'Shared Marketing TM');

      tmView.clickTranslationCell('Hallo Welt');
      tmView.editFieldType('Changed text');
      tmView.cancelEdit();

      tmView.getEntryRowContaining('Hallo Welt').should('be.visible');
      cy.contains('Changed text').should('not.exist');
    });

    // Saving an empty value should delete the stored entry instead of failing the
    // backend's @NotBlank validation. The "Hello world" source group has a sibling
    // entry (French "Bonjour le monde") that keeps the source row alive after the
    // German entry is deleted — and we still see "Thank you" → "Danke" untouched.
    // Assertions are on source text + the surviving German entry: the org doesn't
    // declare French as a language, so the fr column isn't rendered and we can't
    // assert against the French target text directly.
    it('clearing the field deletes the stored entry', () => {
      listView.findAndVisitTm(data, 'test_username', 'Shared Marketing TM');

      tmView.clickTranslationCell('Hallo Welt');
      tmView.editFieldClear();
      tmView.saveEdit();

      cy.contains('Hallo Welt').should('not.exist');
      // "Hello world" source group survives because the French sibling keeps it alive.
      tmView.getEntryRowContaining('Hello world').should('be.visible');
      // The unrelated entry must remain untouched.
      tmView.getEntryRowContaining('Danke').should('be.visible');
    });
  });

  describe('Batch delete', () => {
    it('deletes a single entry', () => {
      listView.findAndVisitTm(data, 'test_username', 'Shared Marketing TM');

      tmView.selectRow('Hallo Welt');
      tmView.batchDelete();

      tmView.getEntryRowContaining('Hallo Welt').should('not.exist');
    });

    it('deletes the whole TU when source is selected', () => {
      listView.findAndVisitTm(data, 'test_username', 'Shared Marketing TM');

      tmView.getEntryRows().should('have.length', 2);
      tmView.selectRow('Hello world');
      tmView.batchDelete();

      tmView.getEntryRowContaining('Hello world').should('not.exist');
      tmView.getEntryRowContaining('Bonjour le monde').should('not.exist');
      tmView.getEntryRows().should('have.length', 1);
    });
  });

  it('creates a new entry via dialog', () => {
    listView.findAndVisitTm(data, 'test_username', 'Shared Marketing TM');

    tmView.openCreateEntryDialog();
    tmView.setSourceText('Good morning');
    tmView.setTargetText('Guten Morgen');
    tmView.submitCreateEntry();

    tmView.getEntryRowContaining('Good morning').should('be.visible');
    tmView.getEntryRowContaining('Guten Morgen').should('be.visible');
  });

  it('shows manual entries alongside virtual rows on a project TM', () => {
    // PROJECT TMs surface the assigned project's translations as virtual rows. A manual entry
    // added on top must show as an extra row in the same browser, not hide or replace the
    // virtual content.
    listView.findAndVisitTm(data, 'test_username', 'Project With TM');

    tmView.getEntryRowContaining('Existing source').should('be.visible');
    tmView.getEntryRowContaining('Reviewed source').should('be.visible');

    tmView.openCreateEntryDialog();
    tmView.setSourceText('Manual e2e phrase');
    tmView.setTargetText('Manuelle E2E-Phrase');
    tmView.submitCreateEntry();

    tmView.getEntryRowContaining('Manual e2e phrase').should('be.visible');
    tmView.getEntryRowContaining('Manuelle E2E-Phrase').should('be.visible');
    tmView.getEntryRowContaining('Existing source').should('be.visible');
    tmView.getEntryRowContaining('Reviewed source').should('be.visible');
  });

  describe('Stored vs virtual rendering', () => {
    // "Multi-project shared TM" has both write-access projects translating "Existing source"
    // (two virtual rows) and a manual stored entry on the same source ("Manual override").
    // The fixture exercises the candidate split: stored rows must stay editable and carry
    // no project-key reference; virtual rows stay read-only and carry their originating key.

    // Guards against the regression where a manual entry sharing a source with virtual
    // rows landed in the virtual row's cell instead of getting its own row.
    it('a manual entry on a shared source renders on its own row without a key reference', () => {
      listView.findAndVisitTm(data, 'test_username', 'Multi-project shared TM');

      tmView.getEntryRowContaining('Existing source').should('have.length', 3);

      // The stored manual row carries no project-key reference (manual entries have
      // no associated key in the project), while each virtual row does.
      tmView
        .getEntryRowContaining('Manual override')
        .find('[data-cy="tm-entry-row-keys"]')
        .should('not.exist');
      tmView
        .getEntryRowContaining('Bestehende Übersetzung aus Konfliktprojekt')
        .find('[data-cy="tm-entry-row-keys"]')
        .should('contain', 'shared-greeting');
    });

    // Inline-editing on a virtual cell must be blocked — the same guard also covers empty
    // cells on a virtual row (both share the `editable = canManage && (stored || !rowFromProject)`
    // gate). Editing the stored manual row in the same group must still work.
    it('clicking a virtual cell does not open the edit form, but the manual row stays editable', () => {
      listView.findAndVisitTm(data, 'test_username', 'Multi-project shared TM');

      // Virtual cell click → no edit field.
      tmView.clickTranslationCell('Bestehende Übersetzung aus Konfliktprojekt');
      cy.get('[data-cy="tm-entry-edit-field"]').should('not.exist');

      // Manual stored cell on the same source group stays editable.
      tmView.clickTranslationCell('Manual override');
      cy.get('[data-cy="tm-entry-edit-field"]').should('be.visible');
      tmView.cancelEdit();
    });
  });

  describe('Import / Export', () => {
    it('shows import and export buttons', () => {
      listView.findAndVisitTm(data, 'test_username', 'Shared Marketing TM');

      tmView.getImportButton().should('be.visible');
      tmView.getExportButton().should('be.visible');
    });

    it('imports a TMX file', () => {
      listView.findAndVisitTm(data, 'test_username', 'Unassigned Shared TM');

      tmView.importTmxFile('translationMemory/sample.tmx');

      tmView.getEntryRowContaining('Good morning').should('be.visible');
      tmView.getEntryRowContaining('Guten Morgen').should('be.visible');
    });

    it('triggers TMX export', () => {
      listView.findAndVisitTm(data, 'test_username', 'Shared Marketing TM');

      tmView.clickExportButton();

      // Cypress can't easily assert file downloads — the click not erroring is enough.
      tmView.getExportButton().should('be.visible');
    });
  });

  describe('Header metadata', () => {
    it('shows TM type badge and full subtitle', () => {
      // "Shared TM with default penalty" is the only fixture TM with a non-zero default
      // penalty — needed to exercise the penalty segment of the subtitle.
      listView.findAndVisitTm(
        data,
        'test_username',
        'Shared TM with default penalty'
      );

      cy.contains('Shared').should('be.visible');
      tmView
        .getSubtitle()
        .should('be.visible')
        .and('contain', 'Base language')
        .and('contain', 'Default penalty');
    });

    it('hides default penalty segment when zero', () => {
      listView.findAndVisitTm(data, 'test_username', 'Shared Marketing TM');

      tmView
        .getSubtitle()
        .should('be.visible')
        .and('contain', 'Base language')
        .and('not.contain', 'Default penalty');
    });
  });

  describe('Layout toggle', () => {
    it('renders a sticky language column header in flat layout', () => {
      listView.findAndVisitTm(data, 'test_username', 'Shared Marketing TM');

      tmView.getListHeader().should('be.visible');
    });

    it('hides the column header in stacked layout and persists the choice in the URL', () => {
      listView.findAndVisitTm(data, 'test_username', 'Shared Marketing TM');

      tmView.switchToStacked();
      tmView.getListHeader().should('not.exist');
      cy.url().should('include', 'layout=stacked');

      tmView.switchToFlat();
      tmView.getListHeader().should('be.visible');
    });
  });
});
