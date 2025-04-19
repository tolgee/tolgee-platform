import { HOST } from '../common/constants';
import { E2KeyCreateDialog, KeyDialogFillProps } from './E2KeyCreateDialog';
import { getTranslationCell } from '../common/translations';

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
}
