import { gcy } from '../../common/shared';
import 'cypress-file-upload';

export class E2GlossaryImportDialog {
  selectFile(fixturePath: string) {
    gcy('file-dropzone-select-button').click();
    gcy('glossary-import-dialog')
      .find('input[type="file"]')
      .attachFile(fixturePath, { subjectType: 'input' });
  }

  chooseReplace() {
    gcy('glossary-import-mode-replace').click();
    gcy('glossary-import-mode-replace')
      .find('input[type="radio"]')
      .should('be.checked');
  }

  chooseAdd() {
    gcy('glossary-import-mode-add').click();
    gcy('glossary-import-mode-add')
      .find('input[type="radio"]')
      .should('be.checked');
  }

  submit() {
    gcy('glossary-import-submit-button').click();
    gcy('glossary-import-dialog').should('not.exist');
  }

  cancel() {
    gcy('glossary-import-cancel-button').click();
    gcy('glossary-import-dialog').should('not.exist');
  }
}
