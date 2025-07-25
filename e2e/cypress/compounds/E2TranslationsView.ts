import { HOST } from '../common/constants';
import { E2KeyCreateDialog, KeyDialogFillProps } from './E2KeyCreateDialog';
import { getTranslationCell } from '../common/translations';
import { gcy } from '../common/shared';

export class E2TranslationsView {
  visit(projectId: number) {
    return cy.visit(`${HOST}/projects/${projectId}/translations`);
  }

  getAddButton() {
    return cy.gcy('translations-add-button');
  }

  openKeyCreateDialog() {
    this.getAddButton().click();
    return new E2KeyCreateDialog();
  }

  createKey(props: KeyDialogFillProps) {
    const dialog = this.openKeyCreateDialog();
    dialog.fillAndSave(props);
  }

  getTranslationCell(key: string, languageTag: string) {
    return getTranslationCell(key, languageTag);
  }

  closeTranslationEdit() {
    gcy('translations-cell-cancel-button').click();
  }

  openFilterSelect() {
    gcy('translations-filter-select').click();
    cy.waitForDom();
    return this;
  }

  selectLabelsFilter() {
    this.getLabelsFilter().should('exist').click();
    return this;
  }

  getLabelsFilter() {
    return gcy('submenu-item').contains('Labels');
  }

  selectLabelByName(labelName: string) {
    gcy('filter-item').contains(labelName).click();
    return this;
  }

  applyFilterForExpand() {
    gcy('translations-filter-apply-for-expand').click();
    return this;
  }

  applyFilterForAll() {
    gcy('translations-filter-apply-for-all').click();
    return this;
  }

  applyFilterForLanguage(language: string) {
    gcy('translations-filter-apply-for-language').contains(language).click();
    return this;
  }

  getTranslationsRows() {
    return gcy('translations-row');
  }

  assertTranslationsRowsCount(count: number) {
    this.getTranslationsRows().should('have.length', count);
  }

  filterByLabel(labelName: string) {
    this.openFilterSelect().selectLabelsFilter().selectLabelByName(labelName);
    gcy('translations-filter-select').contains(labelName);
    return this;
  }
}
