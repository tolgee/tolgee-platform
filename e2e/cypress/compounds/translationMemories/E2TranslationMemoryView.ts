import { gcy } from '../../common/shared';

export class E2TranslationMemoryView {
  getSubtitle() {
    return gcy('tm-content-subtitle');
  }

  getListHeader() {
    return gcy('tm-entries-header');
  }

  search(text: string) {
    gcy('global-list-search').find('input').clear().type(text);
  }

  clearSearch() {
    gcy('global-list-search').find('input').clear();
  }

  switchToFlat() {
    gcy('tm-entries-layout-flat').click();
  }

  switchToStacked() {
    gcy('tm-entries-layout-stacked').click();
  }

  getEntryRows() {
    return gcy('translation-memory-entry-row');
  }

  // No `.first()` here — callers that need a single element add it themselves. Keeping
  // the unfiltered subject lets `.should('not.exist')` work, since `.first()` on an empty
  // jQuery set errors before the assertion can override the existence requirement.
  getEntryRowContaining(text: string) {
    return gcy('translation-memory-entry-row').filter(`:contains("${text}")`);
  }

  clickTranslationCell(text: string) {
    gcy('tm-entry-translation-cell').filter(`:contains("${text}")`).click();
  }

  editFieldType(text: string) {
    gcy('tm-entry-edit-field').find('textarea').first().clear().type(text);
  }

  editFieldClear() {
    gcy('tm-entry-edit-field').find('textarea').first().clear();
  }

  saveEdit() {
    gcy('tm-entry-save').click();
  }

  cancelEdit() {
    gcy('tm-entry-cancel').click();
  }

  selectRow(rowText: string) {
    this.getEntryRowContaining(rowText)
      .first()
      .find('[data-cy="tm-entry-row-checkbox"] input')
      .check();
  }

  batchDelete() {
    gcy('tm-batch-delete-button').click();
    cy.get('[role="dialog"]').contains('button', 'Confirm').click();
  }

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

  submitCreateEntry() {
    gcy('tm-entry-create-submit').click();
    gcy('tm-create-entry-dialog').should('not.exist');
  }

  getImportButton() {
    return gcy('tm-import-menu-button');
  }

  getExportButton() {
    return gcy('tm-export-button');
  }

  clickExportButton() {
    this.getExportButton().click();
  }

  assertImportDialogVisible() {
    gcy('tm-import-dialog').should('be.visible');
  }

  importTmxFile(fixturePath: string, mode?: 'keep' | 'override') {
    this.getImportButton().click();
    this.assertImportDialogVisible();

    gcy('tm-import-dialog')
      .find('input[type="file"]')
      .selectFile(`cypress/fixtures/${fixturePath}`, { force: true });

    gcy('tm-import-submit').should('not.be.disabled');

    if (mode === 'override') {
      gcy('tm-import-mode-override').click();
    }

    gcy('tm-import-submit').click();
    gcy('tm-import-dialog').should('not.exist');
  }

  getEmptyWizard() {
    return gcy('tm-empty-wizard');
  }

  getEmptyWizardManualCard() {
    return gcy('tm-empty-wizard-manual');
  }

  getEmptyWizardCopyCard() {
    return gcy('tm-empty-wizard-copy');
  }

  getEmptyWizardImportCard() {
    return gcy('tm-empty-wizard-import');
  }
}
