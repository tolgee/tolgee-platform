import { gcy } from '../../common/shared';

export class E2TranslationMemoryCreateEditDialog {
  setName(name: string | undefined) {
    const chain = gcy('create-translation-memory-field-name')
      .click()
      .focused()
      .clear();
    if (name !== undefined) {
      chain.type(name);
    }
    this.checkName(name || '');
  }

  checkName(name: string) {
    gcy('create-translation-memory-field-name')
      .find('input')
      .should('have.value', name);
  }

  setBaseLanguage(language: string) {
    gcy('base-language-select').click();
    gcy('base-language-select-item').contains(language).click();
    this.checkBaseLanguage(language);
  }

  checkBaseLanguage(language: string) {
    gcy('base-language-select').should('contain', language);
  }

  toggleAssignedProject(projectName: string) {
    gcy('tm-add-project-autocomplete').find('input').click();
    cy.get('li[role="option"]').contains(projectName).click();
  }

  cancel() {
    gcy('create-edit-translation-memory-cancel').click();
  }

  submit() {
    gcy('create-edit-translation-memory-submit').click();
  }
}
