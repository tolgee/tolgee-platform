import { gcy } from '../../common/shared';

export class E2TranslationMemoryCreateEditDialog {
  getNameField() {
    return gcy('create-translation-memory-field-name');
  }

  setName(name: string | undefined) {
    const chain = this.getNameField().click().focused().clear();
    if (name !== undefined) {
      chain.type(name);
    }
    this.checkName(name || '');
  }

  clearAndSetName(name: string) {
    this.getNameField().find('input').clear().type(name);
  }

  checkName(name: string) {
    this.getNameField().find('input').should('have.value', name);
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
    // Scoped data-cy lookup — the option element is rendered with this attribute and the
    // project name is the option's text. Avoids brittle text-only matching on `li[role=…]`.
    gcy('tm-add-project-option').contains(projectName).click();
  }

  getWriteOnlyReviewedSwitch() {
    return gcy('tm-settings-write-only-reviewed');
  }

  cancel() {
    gcy('create-edit-translation-memory-cancel').click();
  }

  submit() {
    gcy('create-edit-translation-memory-submit').click();
  }
}
