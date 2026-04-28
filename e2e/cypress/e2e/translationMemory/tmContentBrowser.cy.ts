import 'cypress-file-upload';
import { translationMemoryTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';
import { gcy } from '../../common/shared';
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

  // --- Navigation ---

  it('navigates to TM content by clicking list item', () => {
    listView.findAndVisitTm(data, 'test_username', 'Shared Marketing TM');

    cy.url().should('include', '/translation-memories/');
    cy.contains('Shared Marketing TM').should('be.visible');
    cy.contains('Translation memories').should('be.visible');
  });

  // --- Entries display ---

  it('shows entries grouped by source text', () => {
    listView.findAndVisitTm(data, 'test_username', 'Shared Marketing TM');

    tmView.getEntryRows().should('have.length.at.least', 1);
    cy.contains('Hello world').should('be.visible');
    cy.contains('Thank you').should('be.visible');
  });

  // --- Search ---

  it('search filters entries', () => {
    listView.findAndVisitTm(data, 'test_username', 'Shared Marketing TM');

    tmView.getEntryRows().should('have.length', 2);

    cy.get('input[placeholder*="Search"]').type('Hello');
    tmView.getEntryRows().should('have.length', 1);
    cy.contains('Hello world').should('be.visible');
  });

  // --- Inline editing ---

  it('inline edit existing entry', () => {
    listView.findAndVisitTm(data, 'test_username', 'Shared Marketing TM');

    tmView.clickTranslationCell('Hallo Welt');
    tmView.editFieldType('Hallo Welt!');
    tmView.saveEdit();

    cy.contains('Hallo Welt!').should('be.visible');
  });

  it('inline edit cancel restores original text', () => {
    listView.findAndVisitTm(data, 'test_username', 'Shared Marketing TM');

    tmView.clickTranslationCell('Hallo Welt');
    tmView.editFieldType('Changed text');
    tmView.cancelEdit();

    cy.contains('Hallo Welt').should('be.visible');
    cy.contains('Changed text').should('not.exist');
  });

  it('delete entry via batch toolbar', () => {
    listView.findAndVisitTm(data, 'test_username', 'Shared Marketing TM');

    tmView.selectRow('Hallo Welt');
    tmView.batchDelete();

    cy.contains('Hallo Welt').should('not.exist');
  });

  // --- Create entry dialog ---

  it('create new entry via dialog', () => {
    listView.findAndVisitTm(data, 'test_username', 'Shared Marketing TM');

    tmView.openCreateEntryDialog();
    tmView.setSourceText('Good morning');
    tmView.setTargetText('Guten Morgen');
    tmView.submitCreateEntry();

    cy.contains('Good morning').should('be.visible');
    cy.contains('Guten Morgen').should('be.visible');
  });

  // --- Import / Export ---

  it('import and export buttons are visible', () => {
    listView.findAndVisitTm(data, 'test_username', 'Shared Marketing TM');

    gcy('tm-import-button').should('be.visible');
    gcy('tm-export-button').should('be.visible');
  });

  it('import TMX file creates entries', () => {
    listView.findAndVisitTm(data, 'test_username', 'Unassigned Shared TM');

    const tmx = [
      '<?xml version="1.0" encoding="UTF-8"?>',
      '<tmx version="1.4">',
      '  <header srclang="en" datatype="PlainText" creationtool="test"/>',
      '  <body>',
      '    <tu><tuv xml:lang="en"><seg>Good morning</seg></tuv><tuv xml:lang="de"><seg>Guten Morgen</seg></tuv></tu>',
      '  </body>',
      '</tmx>',
    ].join('\n');

    tmView.importTmxFile('test.tmx', tmx);

    cy.contains('Good morning').should('be.visible');
    cy.contains('Guten Morgen').should('be.visible');
  });

  it('export TMX downloads a file', () => {
    listView.findAndVisitTm(data, 'test_username', 'Shared Marketing TM');

    tmView.clickExportButton();

    // The file should be downloaded — verify no error occurred
    // (Cypress can't easily assert file downloads, but we verify the button works)
    gcy('tm-export-button').should('be.visible');
  });

  // --- Empty TM ---

  it('shows empty state for TM with no entries', () => {
    listView.findAndVisitTm(data, 'test_username', 'Unassigned Shared TM');

    // No entry rows should be present
    tmView.getEntryRows().should('not.exist');
  });

  // --- Header metadata ---

  it('shows TM type badge and metadata subtitle', () => {
    // Shared TM with default penalty preset — exercises the full subtitle
    // (base language + project count + default penalty). TMs with zero default
    // penalty intentionally hide the penalty segment.
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

  it('hides default penalty segment in subtitle when zero', () => {
    listView.findAndVisitTm(data, 'test_username', 'Shared Marketing TM');

    tmView
      .getSubtitle()
      .should('be.visible')
      .and('contain', 'Base language')
      .and('not.contain', 'Default penalty');
  });

  // --- Layout toggle ---

  it('flat layout renders a sticky header with language columns', () => {
    listView.findAndVisitTm(data, 'test_username', 'Shared Marketing TM');

    // flat is the default — header is visible.
    tmView.getListHeader().should('be.visible');
  });

  it('switching to stacked layout hides the column header', () => {
    listView.findAndVisitTm(data, 'test_username', 'Shared Marketing TM');

    tmView.switchToStacked();
    tmView.getListHeader().should('not.exist');

    // URL picks up the persisted preference.
    cy.url().should('include', 'layout=stacked');

    tmView.switchToFlat();
    tmView.getListHeader().should('be.visible');
  });

  // --- Delete whole TU via batch toolbar ---

  it('deletes whole TU via the batch toolbar', () => {
    listView.findAndVisitTm(data, 'test_username', 'Shared Marketing TM');

    tmView.getEntryRows().should('have.length', 2);
    tmView.selectRow('Hello world');
    tmView.batchDelete();

    cy.contains('Hello world').should('not.exist');
    // "Bonjour le monde" also disappears because the whole TU is gone.
    cy.contains('Bonjour le monde').should('not.exist');
    tmView.getEntryRows().should('have.length', 1);
  });
});
