import { login } from '../../common/apiCalls/common';
import { labelsTestData } from '../../common/apiCalls/testData/testData';
import {
  getTranslationCell,
  visitTranslations,
} from '../../common/translations';
import { gcy } from '../../common/shared';
import { isDarkMode } from '../../common/helpers';
import { E2TranslationLabel } from '../../compounds/E2TranslationLabel';
import { E2ActivityChecker } from '../../compounds/E2ActivityChecker';
import { E2TranslationsView } from '../../compounds/E2TranslationsView';

let projectId = null;
let emptyProjectId = null;

describe('Projects Settings - Labels', () => {
  const translationLabel = new E2TranslationLabel();
  const activityChecker = new E2ActivityChecker();

  beforeEach(() => {
    labelsTestData.clean();
    labelsTestData.generate().then((data) => {
      login('test_username');
      projectId = data.body.projects[0].id;
      emptyProjectId = data.body.projects[2].id;
    });
  });

  it('see translation label', () => {
    visitTranslations(projectId);
    getTranslationCell('first key', 'en').within(() => {
      gcy('translation-label')
        .should(
          'have.css',
          'background-color',
          isDarkMode ? 'rgba(255, 0, 0, 0.85)' : 'rgb(255, 0, 0)'
        )
        .contains('First label');
    });
  });

  it('search and add label to translation', () => {
    visitTranslations(projectId);
    translationLabel.assignLabelToTranslation(
      'first key',
      'en',
      'Label to assign 1',
      4
    );

    // Verify label after assigning
    translationLabel.verifyLabelsCountInTranslationCell('first key', 'en', 2);
    translationLabel.verifyLabelInTranslationCell(
      'first key',
      'en',
      'Label to assign 1',
      isDarkMode ? 'rgba(255, 0, 255, 0.85)' : 'rgb(255, 0, 255)'
    );

    // refresh the page to ensure the label is saved
    cy.reload();

    // verify label after page reload
    translationLabel.verifyLabelsCountInTranslationCell('first key', 'en', 2);
    translationLabel.verifyLabelInTranslationCell(
      'first key',
      'en',
      'Label to assign 1',
      isDarkMode ? 'rgba(255, 0, 255, 0.85)' : 'rgb(255, 0, 255)'
    );
  });

  it('remove label from translation', () => {
    visitTranslations(projectId);

    translationLabel.unassignLabelFromTranslation(
      'first key',
      'en',
      'First label'
    );

    // verify the label is removed
    translationLabel.verifyLabelsCountInTranslationCell('first key', 'en', 0);
    // refresh the page to ensure the removal persists
    cy.reload();
    translationLabel.verifyLabelsCountInTranslationCell('first key', 'en', 0);
  });

  it('filters by label', () => {
    visitTranslations(projectId);
    const view = new E2TranslationsView();

    view.assertTranslationsRowsCount(2);

    view.filterByLabel('First label');
    view.assertTranslationsRowsCount(0);

    view.applyFilterForExpand().applyFilterForAll();

    view.getTranslationsRows().contains('first key').should('be.visible');
    view.assertTranslationsRowsCount(1);

    view.applyFilterForLanguage('English');
    view.assertTranslationsRowsCount(1);

    view.applyFilterForLanguage('Czech');
    view.assertTranslationsRowsCount(0);
  });

  it('filters has not labels when no labels exists', () => {
    visitTranslations(emptyProjectId);
    cy.gcy('translations-filter-select').click();
    cy.waitForDom();
    cy.gcy('submenu-item').contains('Labels').should('not.exist');
  });

  it('creates activity when translation labels are updated', () => {
    visitTranslations(projectId);

    // Add a label to translation
    translationLabel.assignLabelToTranslation(
      'first key',
      'en',
      'Label to assign 1',
      4
    );

    activityChecker
      .checkActivity('Translation labels updated')
      .assertActivityDetails([
        'Translation labels updated',
        'first key',
        'Label to assign 1',
      ]);
  });

  it('creates activity when translation labels are removed', () => {
    visitTranslations(projectId);

    // Remove the label and verify activity
    translationLabel.unassignLabelFromTranslation(
      'first key',
      'en',
      'First label'
    );

    activityChecker
      .checkActivity('Translation labels updated')
      .assertActivityDetails(['Translation labels updated', 'first key']);
  });
});
