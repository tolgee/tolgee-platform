import { HOST } from '../common/constants';
import { KeyCreateDialog, KeyDialogFillProps } from './KeyCreateDialog';

export class TranslationsView {
  visit(projectId: number) {
    return cy.visit(`${HOST}/projects/${projectId}/translations`);
  }

  getAddButton() {
    return cy.gcy('translations-add-button');
  }

  openKeyCreateDialog() {
    this.getAddButton().click();
    return new KeyCreateDialog();
  }

  createKey(props: KeyDialogFillProps) {
    const dialog = this.openKeyCreateDialog();
    dialog.fillAndSave(props);
  }
}
