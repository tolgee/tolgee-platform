import { gcyAdvanced } from '../common/shared';
import { E2NamespaceSelector } from './E2NamespaceSelector';

export class E2KeyCreateDialog {
  fill({
    key,
    translation,
    tag,
    namespace,
    description,
    plural,
  }: KeyDialogFillProps) {
    this.getKeyNameInput().type(key);
    if (namespace) {
      this.selectNamespace(namespace);
    }
    if (description) {
      this.getDescriptionInput().type(description);
    }
    if (tag) {
      this.addNewTag(tag);
    }

    this.setSingularTranslation(translation);
    this.setPluralTranslation(plural);
  }

  save() {
    cy.gcy('global-form-save-button').click();
  }

  fillAndSave(props: KeyDialogFillProps) {
    this.fill(props);
    this.save();
  }

  getKeyNameInput() {
    return cy.gcy('translation-create-key-input');
  }

  getDescriptionInput() {
    return cy.gcy('translation-create-description-input');
  }

  getTagInput() {
    return cy.gcy('translations-tag-input');
  }

  getTagAutocompleteOption() {
    return cy.gcy('tag-autocomplete-option');
  }

  getTranslationInput() {
    return cy.gcy('translation-editor').first();
  }

  setSingularTranslation(translation?: string) {
    if (!translation) {
      return;
    }
    this.getTranslationInput().type(translation);
  }

  addNewTag(tag: string) {
    this.getTagInput().type(tag);
    this.getTagAutocompleteOption().contains(`Add "${tag}"`).click();
  }

  setPluralTranslation(plural?: KeyDialogFillProps['plural']) {
    if (!plural) {
      return;
    }

    cy.gcy('key-plural-checkbox').click();
    if (plural.variableName) {
      cy.gcy('key-plural-checkbox-expand').click();
      cy.gcy('key-plural-variable-name').type(plural.variableName);
    }
    Object.entries(plural.formValues).forEach(([key, value]) => {
      gcyAdvanced({ value: 'translation-editor', variant: key })
        .find('[contenteditable]')
        .type(value);
    });
  }

  selectNamespace(namespace: string) {
    new E2NamespaceSelector().selectNamespace(namespace);
  }

  getNamespaceSelectElement() {
    return new E2NamespaceSelector().getNamespaceSelect();
  }
}

export type KeyDialogFillProps = {
  key: string;
  translation?: string;
  plural?: {
    variableName?: string;
    formValues: Record<string, string>;
  };
  tag?: string;
  namespace?: string;
  description?: string;
};
