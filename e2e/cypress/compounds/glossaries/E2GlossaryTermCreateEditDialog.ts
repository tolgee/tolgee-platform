import { gcy } from '../../common/shared';

export class E2GlossaryTermCreateEditDialog {
  setDefaultTranslation(translation: string | undefined) {
    const chain = gcy('create-glossary-term-field-text')
      .click()
      .focused()
      .clear();
    if (translation !== undefined) {
      chain.type(translation);
    }
    this.checkDefaultTranslation(translation || '');
  }

  checkDefaultTranslation(translation: string) {
    gcy('create-glossary-term-field-text')
      .find('input')
      .should('have.value', translation);
  }

  setDescription(description: string | undefined) {
    const chain = gcy('create-glossary-term-field-description')
      .click()
      .focused()
      .clear();
    if (description !== undefined) {
      chain.type(description);
    }
    this.checkDescription(description || '');
  }

  checkDescription(description: string) {
    gcy('create-glossary-term-field-description')
      .find('textarea')
      .should('have.value', description);
  }

  toggleFlagCaseSensitive() {
    gcy('create-glossary-term-flag-case-sensitive').click();
  }

  toggleFlagAbbreviation() {
    gcy('create-glossary-term-flag-abbreviation').click();
  }

  toggleFlagForbidden() {
    gcy('create-glossary-term-flag-forbidden').click();
  }

  toggleFlagNonTranslatable() {
    gcy('create-glossary-term-flag-non-translatable').click();
  }

  cancel() {
    gcy('create-glossary-term-cancel').click();
    gcy('create-glossary-term-dialog').should('not.exist');
  }

  submit() {
    gcy('create-glossary-term-submit').click();
    gcy('create-glossary-term-dialog').should('not.exist');
  }
}
