import { gcy } from '../../common/shared';

export class E2TranslationMemoryView {
  // --- Header ---

  getSubtitle() {
    return gcy('tm-content-subtitle');
  }

  getListHeader() {
    return gcy('tm-entries-header');
  }

  // --- Search ---

  search(text: string) {
    gcy('global-list-search').find('input').clear().type(text);
  }

  clearSearch() {
    gcy('global-list-search').find('input').clear();
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

  // --- Import menu / Export ---
  // The import button is a dropdown that fans out to "Import TMX" and "Copy from a project".
  // Compound exposes both the trigger and each menu item separately so tests can either pick
  // an action (`importTmxFile`, `openCopyFromProjectDialog`) or assert visibility independently.

  getImportMenuButton() {
    return gcy('tm-import-menu-button');
  }

  openImportMenu() {
    this.getImportMenuButton().click();
  }

  getImportTmxMenuItem() {
    return gcy('tm-import-menu-tmx');
  }

  getCopyFromProjectMenuItem() {
    return gcy('tm-import-menu-copy-from-project');
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
    this.openImportMenu();
    this.getImportTmxMenuItem().click();
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

  // --- Empty-state wizard ---

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

  // --- Copy-from-project dialog (reused from wizard and toolbar menu) ---

  openCopyFromProjectDialogFromMenu() {
    this.openImportMenu();
    this.getCopyFromProjectMenuItem().click();
    gcy('tm-empty-wizard-copy-dialog').should('be.visible');
  }

  copyFromProject(projectName: string) {
    gcy('tm-empty-wizard-copy-project').find('input').type(projectName);
    cy.get('[role="presentation"]')
      .filter(`:contains("${projectName}")`)
      .first()
      .find('li')
      .first()
      .click();
    gcy('tm-empty-wizard-copy-submit').click();
    gcy('tm-empty-wizard-copy-dialog').should('not.exist');
  }
}
