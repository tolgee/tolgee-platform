import { getTranslationCell } from '../common/translations';
import { dismissMenu, gcy } from '../common/shared';
import { E2ProjectLabelsSection } from './projectSettings/labels/E2ProjectLabelsSection';

export class E2TranslationLabel {
  private getTranslationLabels(key: string, lang: string) {
    return getTranslationCell(key, lang).find('.translation-labels-list');
  }

  assignLabelToTranslation(
    key: string,
    languageTag: string,
    label: string,
    expectLabelCount?: number
  ) {
    this.getTranslationLabels(key, languageTag).within(() => {
      gcy('translation-label-control')
        .should('not.be.visible')
        .click()
        .should('be.visible');
    });
    gcy('search-select-search').should('be.visible').click();
    gcy('label-selector-autocomplete').should('be.visible');
    if (expectLabelCount !== undefined) {
      gcy('label-autocomplete-option').should('have.length', expectLabelCount);
    }
    gcy('label-autocomplete-option')
      .contains(label)
      .should('be.visible')
      .click();
    dismissMenu();
  }

  unassignLabelFromTranslation(
    key: string,
    languageTag: string,
    label: string
  ) {
    this.getTranslationLabels(key, languageTag).within(() => {
      gcy('translation-label')
        .contains(label)
        .should('be.visible')
        .siblingDcy('translation-label-delete')
        .invoke('css', 'opacity', 1) // hover is not supported in Cypress, had to use CSS opacity
        .click();
    });
  }

  verifyLabelInTranslationCell(
    key: string,
    lang: string,
    label: string,
    expectedColor: string
  ) {
    this.getTranslationLabels(key, lang).within(() => {
      gcy('translation-label')
        .contains(label)
        .should('be.visible')
        .should('have.css', 'background-color', expectedColor);
    });
  }

  verifyLabelsCountInTranslationCell(
    key: string,
    lang: string,
    expectedCount: number
  ) {
    this.getTranslationLabels(key, lang).within(() => {
      gcy('translation-label').should('have.length', expectedCount);
    });
  }
}
