import 'cypress-file-upload';
import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import {
  editCell,
  translationsBeforeEach,
  visitTranslations,
} from '../../common/translations';
import { createKey, deleteProject } from '../../common/apiCalls/common';
import { waitForGlobalLoading } from '../../common/loading';
import { E2TranslationsView } from '../../compounds/E2TranslationsView';
import { visitImport, getFileIssuesDialog } from '../../common/import';
import { gcy, gcyAdvanced } from '../../common/shared';

describe('Translation character limit', () => {
  let project: ProjectDTO;

  beforeEach(() => {
    translationsBeforeEach().then((p) => (project = p));
  });

  afterEach(() => {
    deleteProject(project.id);
  });

  it('Save button disabled when base translation exceeds char limit', () => {
    waitForGlobalLoading();
    const translationsView = new E2TranslationsView();
    const dialog = translationsView.openKeyCreateDialog();
    dialog.getKeyNameInput().type('test-key');
    dialog.setCharLimit(5);
    dialog.getTranslationInput().type('Hello World');
    cy.gcy('global-form-save-button').should('be.disabled');
  });

  it('Save button enabled and key created when within limit', () => {
    waitForGlobalLoading();
    const translationsView = new E2TranslationsView();
    const dialog = translationsView.openKeyCreateDialog();
    dialog.getKeyNameInput().type('test-key');
    dialog.setCharLimit(20);
    dialog.getTranslationInput().type('Hello');
    cy.gcy('global-form-save-button').should('not.be.disabled');
    dialog.save();
    cy.contains('Key created').should('be.visible');
  });

  it('HTML tags are not counted toward char limit', () => {
    waitForGlobalLoading();
    const translationsView = new E2TranslationsView();
    const dialog = translationsView.openKeyCreateDialog();
    dialog.getKeyNameInput().type('test-key');
    dialog.setCharLimit(5);
    // Switch to syntax mode to type raw HTML
    cy.gcy('translations-cell-switch-mode').click();
    cy.gcy('translation-editor')
      .first()
      .find('[contenteditable]')
      .clear()
      .type('<b>Hello</b>');
    // 5 visible chars (Hello), tags excluded
    cy.gcy('global-form-save-button').should('not.be.disabled');
  });

  it('Variables are not counted toward char limit', () => {
    waitForGlobalLoading();
    const translationsView = new E2TranslationsView();
    const dialog = translationsView.openKeyCreateDialog();
    dialog.getKeyNameInput().type('test-key');
    dialog.setCharLimit(6);
    // Switch to syntax mode to type raw ICU
    cy.gcy('translations-cell-switch-mode').click();
    cy.gcy('translation-editor')
      .first()
      .find('[contenteditable]')
      .clear()
      .type('Hello {name}', { parseSpecialCharSequences: false });
    // 6 visible chars (Hello + space), variable {name} excluded
    cy.gcy('global-form-save-button').should('not.be.disabled');
  });

  it('Plural # is not counted toward char limit', () => {
    waitForGlobalLoading();
    const translationsView = new E2TranslationsView();
    const dialog = translationsView.openKeyCreateDialog();
    dialog.getKeyNameInput().type('test-key');
    dialog.setCharLimit(6);
    cy.gcy('key-plural-checkbox').click();
    gcyAdvanced({ value: 'translation-editor', variant: 'other' })
      .find('[contenteditable]')
      .type('# items');
    // "# items" = 6 visible chars, # is not counted -> " items" = 6 chars
    cy.gcy('global-form-save-button').should('not.be.disabled');
  });

  it('Translation editing - shows confirmation when exceeding char limit', () => {
    createKey(project.id, 'limited-key', { en: 'Hi' }, { maxCharLimit: 5 });
    visitTranslations(project.id);
    waitForGlobalLoading();
    editCell('Hi', undefined, false);
    cy.gcy('global-editor')
      .find('[contenteditable]')
      .clear()
      .type('Hello World');
    cy.gcy('translations-cell-main-action-button').should('not.be.disabled');
    cy.gcy('translations-cell-main-action-button').click();
    cy.gcy('global-confirmation-dialog').should('be.visible');
    cy.gcy('global-confirmation-confirm').click();
    cy.contains('Hello World').should('be.visible');
  });

  it('Import shows warning when translation exceeds char limit', () => {
    createKey(project.id, 'char-limit-key', { en: 'OK' }, { maxCharLimit: 5 });
    visitImport(project.id);
    waitForGlobalLoading();
    cy.gcy('import-file-input').attachFile('import/charLimit.json');
    gcy('import-result-file-cell', { timeout: 30000 }).should('be.visible');
    gcy('import-file-issues-button').click();
    getFileIssuesDialog()
      .contains('Translation exceeds character limit')
      .should('be.visible');
  });
});
