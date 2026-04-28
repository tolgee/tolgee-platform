import { gcy } from '../../common/shared';

export class E2TranslationMemoryView {
  // --- Header ---

  getSubtitle() {
    return gcy('tm-content-subtitle');
  }

  getListHeader() {
    return gcy('tm-entries-header');
  }

  // --- Layout toggle ---

  switchToFlat() {
    gcy('tm-entries-layout-flat').click();
  }

  switchToStacked() {
    gcy('tm-entries-layout-stacked').click();
  }

  // --- Entries ---

  getEntryRows() {
    return gcy('translation-memory-entry-row');
  }

  clickTranslationCell(text: string) {
    gcy('tm-entry-translation-cell').filter(`:contains("${text}")`).click();
  }

  editFieldType(text: string) {
    gcy('tm-entry-edit-field').find('textarea').first().clear().type(text);
  }

  saveEdit() {
    gcy('tm-entry-save').click();
  }

  cancelEdit() {
    gcy('tm-entry-cancel').click();
  }

  /**
   * Selects a row that contains [rowText] via its checkbox. [rowText] may be
   * any cell content unique to the target row (source or target text).
   */
  selectRow(rowText: string) {
    gcy('translation-memory-entry-row')
      .filter(`:contains("${rowText}")`)
      .first()
      .find('[data-cy="tm-entry-row-checkbox"] input')
      .check();
  }

  /**
   * Clicks the batch-delete button in the bottom toolbar and confirms.
   * Rows must be selected via [selectRow] beforehand.
   */
  batchDelete() {
    gcy('tm-batch-delete-button').click();
    cy.get('[role="dialog"]').contains('button', 'Confirm').click();
  }

  // --- Create entry dialog ---

  openCreateEntryDialog() {
    gcy('global-plus-button').click();
    gcy('tm-create-entry-dialog').should('be.visible');
  }

  setSourceText(text: string) {
    gcy('tm-entry-source-text').find('textarea').first().type(text);
  }

  setTargetText(text: string, index = 0) {
    gcy('tm-entry-target-text').eq(index).find('textarea').first().type(text);
  }

  addLanguage() {
    gcy('tm-entry-add-language').click();
  }

  submitCreateEntry() {
    gcy('tm-entry-create-submit').click();
    gcy('tm-create-entry-dialog').should('not.exist');
  }

  // --- Import / Export ---

  clickImportButton() {
    gcy('tm-import-button').click();
  }

  clickExportButton() {
    gcy('tm-export-button').click();
  }

  assertImportDialogVisible() {
    gcy('tm-import-dialog').should('be.visible');
  }

  importTmxFile(filename: string, content: string, mode?: 'keep' | 'override') {
    this.clickImportButton();
    this.assertImportDialogVisible();

    cy.writeFile(`cypress/fixtures/generated/${filename}`, content);
    gcy('tm-import-dialog')
      .find('input[type="file"]')
      .selectFile(`cypress/fixtures/generated/${filename}`, { force: true });

    gcy('tm-import-submit').should('not.be.disabled');

    if (mode === 'override') {
      gcy('tm-import-mode-override').click();
    }

    gcy('tm-import-submit').click();
    gcy('tm-import-dialog').should('not.exist');
  }
}
