import { gcy } from '../../common/shared';

export class E2GlossaryCreateEditDialog {
  setName(name: string | undefined) {
    const chain = gcy('create-glossary-field-name').click().focused().clear();
    if (name !== undefined) {
      chain.type(name);
    }
    this.checkName(name || '');
  }

  checkName(name: string) {
    gcy('create-glossary-field-name').find('input').should('have.value', name);
  }

  setBaseLanguage(language: string) {
    gcy('glossary-base-language-select').click();
    gcy('glossary-base-language-select-item').contains(language).click();
    this.checkBaseLanguage(language);
  }

  checkBaseLanguage(language: string) {
    gcy('glossary-base-language-select').should('contain', language);
  }

  toggleAssignedProject(projectName: string) {
    gcy('assigned-projects-select').click();
    gcy('assigned-projects-select-item').contains(projectName).click();
    cy.get('body').type('{esc}'); // Close the dropdown
  }

  cancel() {
    gcy('create-edit-glossary-cancel').click();
    gcy('create-edit-glossary-dialog').should('not.exist');
  }

  submit() {
    gcy('create-edit-glossary-submit').click();
    gcy('create-edit-glossary-dialog').should('not.exist');
  }
}
