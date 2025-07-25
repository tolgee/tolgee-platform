import { getTranslationCell } from '../common/translations';
import { dismissMenu, gcy } from '../common/shared';

export class E2TranslationLabel {
  getTranslationLabels(key: string, lang: string) {
    return getTranslationCell(key, lang).find('.translation-labels-list');
  }

  private openLabelSelector(key: string, languageTag: string) {
    this.getTranslationLabels(key, languageTag).within(() => {
      gcy('translation-label-control')
        .should('not.be.visible')
        .click()
        .should('be.visible');
    });
    gcy('search-select-search').should('be.visible').click();
    gcy('label-selector-autocomplete').should('be.visible');
  }

  private selectLabel(
    label: string,
    options: {
      expectLabelCount?: number;
      labelsToHide?: string[];
    } = {}
  ) {
    const { expectLabelCount, labelsToHide } = options;

    if (expectLabelCount !== undefined) {
      gcy('label-autocomplete-option').should('have.length', expectLabelCount);
    }

    if (labelsToHide) {
      labelsToHide.forEach((l) => {
        gcy('label-autocomplete-option').contains(l).should('not.exist');
      });
    }

    gcy('label-autocomplete-option')
      .contains(label)
      .should('be.visible')
      .click();
  }

  assignLabelToTranslation(
    key: string,
    languageTag: string,
    label: string,
    expectLabelCount?: number
  ) {
    this.openLabelSelector(key, languageTag);
    this.selectLabel(label, { expectLabelCount });
    dismissMenu();
  }

  assignMultipleLabelsToTranslation(
    key: string,
    languageTag: string,
    labels: string[]
  ) {
    this.openLabelSelector(key, languageTag);
    labels.forEach((label) => {
      this.selectLabel(label);
    });
    dismissMenu();
  }

  assignLabelToTranslationWithSearch(
    key: string,
    languageTag: string,
    label: string,
    labelsToHide?: string[],
    expectLabelCount?: number
  ) {
    this.openLabelSelector(key, languageTag);
    gcy('search-select-search').type(label);
    this.selectLabel(label, { expectLabelCount, labelsToHide });
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
        .closestDcy('translation-label')
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
        .closestDcy('translation-label')
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
