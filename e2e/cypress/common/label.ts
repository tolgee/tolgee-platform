import { getTranslationCell } from './translations';
import { dismissMenu, gcy } from './shared';
import { E2ProjectLabelsSection } from '../compounds/projectSettings/labels/E2ProjectLabelsSection';

export function assignLabelToTranslation(
  key: string,
  languageTag: string,
  label: string,
  expectLabelCount?: number | undefined
) {
  getTranslationLabels(key, languageTag).within(() => {
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
  gcy('label-autocomplete-option').contains(label).should('be.visible').click();
  dismissMenu();
}

export const verifyLabelInTranslationCell = (
  key: string,
  lang: string,
  label: string,
  expectedColor: string
) => {
  getTranslationLabels(key, lang).within(() => {
    gcy('translation-label')
      .contains(label)
      .should('be.visible')
      .should('have.css', 'background-color', expectedColor);
  });
};

export const verifyLabelsCountInTranslationCell = (
  key: string,
  lang: string,
  expectedCount: number
) => {
  getTranslationLabels(key, lang).within(() => {
    gcy('translation-label').should('have.length', expectedCount);
  });
};

export const createLabel = (labelName: string, color?: string) => {
  const projectLabels = new E2ProjectLabelsSection();
  projectLabels.visitFromProjectSettings();
  const labelModal = projectLabels.openCreateLabelModal();
  labelModal.fillAndSave(labelName, color);
};

const getTranslationLabels = (key: string, lang: string) => {
  return getTranslationCell(key, lang).find('.translation-labels-list');
};
