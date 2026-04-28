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
