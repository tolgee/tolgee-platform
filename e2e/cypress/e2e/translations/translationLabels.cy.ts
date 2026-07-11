import { login } from '../../common/apiCalls/common';
import { labelsTestData } from '../../common/apiCalls/testData/testData';
import {
  getTranslationCell,
  visitTranslations,
} from '../../common/translations';
import { dismissMenu, gcy } from '../../common/shared';
import { isDarkMode } from '../../common/helpers';
import { E2TranslationLabel } from '../../compounds/E2TranslationLabel';
import { E2TranslationsView } from '../../compounds/E2TranslationsView';
import { assertActivityDetails, checkActivity } from '../../common/activities';
import { setFeature } from '../../common/features';
import {
  findBatchOperation,
  openBatchOperationMenu,
  selectAll,
} from '../../common/batchOperations';

let projectId = null;
let emptyProjectId = null;

describe('Projects Settings - Labels', () => {
  const translationLabel = new E2TranslationLabel();

  beforeEach(() => {
    labelsTestData.clean();
    labelsTestData.generate().then((data) => {
      login('test_username');
      projectId = data.body.projects[0].id;
      emptyProjectId = data.body.projects[2].id;
    });
  });

  afterEach(() => {
    setFeature('TRANSLATION_LABELS', true);
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

  it('does not list translation labels when feature is disabled', () => {
    setFeature('TRANSLATION_LABELS', false);
    visitTranslations(projectId);
    translationLabel
      .getTranslationLabels('first key', 'en')
      .should('not.exist');

    // Ensure the filter select does not show labels option
    const view = new E2TranslationsView();
    view.openFilterSelect().getLabelsFilter().should('not.exist');
    dismissMenu();

    // ensure that batch operations assign / unassign labels are not available
    selectAll();
    openBatchOperationMenu();
    findBatchOperation('Assign labels').should('not.exist');
    findBatchOperation('Unassign labels').should('not.exist');
    setFeature('TRANSLATION_LABELS', true);
  });

  it('search and add label to translation', () => {
    visitTranslations(projectId);
    translationLabel.assignLabelToTranslationWithSearch(
      'first key',
      'en',
      'label to assign 1',
      ['label to assign 2', 'label to assign 3'],
      1
    );

    // Verify label after assigning
    translationLabel.verifyLabelsCountInTranslationCell('first key', 'en', 2);
    translationLabel.verifyLabelInTranslationCell(
      'first key',
      'en',
      'label to assign 1',
      isDarkMode ? 'rgba(255, 0, 255, 0.85)' : 'rgb(255, 0, 255)'
    );

    // refresh the page to ensure the label is saved
    cy.reload();

    // verify label after page reload
    translationLabel.verifyLabelsCountInTranslationCell('first key', 'en', 2);
    translationLabel.verifyLabelInTranslationCell(
      'first key',
      'en',
      'label to assign 1',
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

    view.assertTranslationsRowsCount(3);

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
      'label to assign 1',
      4
    );

    checkActivity('Translation labels updated');
    assertActivityDetails([
      'Translation labels updated',
      'first key',
      'label to assign 1',
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

    checkActivity('Translation labels updated');
    assertActivityDetails(['Translation labels updated', 'first key']);
  });

  it('adds labels to translations and sorts them alphabetically', () => {
    visitTranslations(projectId);

    translationLabel.assignMultipleLabelsToTranslation('first key', 'en', [
      'Unassigned label',
      'label to assign 3',
      'label to assign 2',
    ]);

    getTranslationCell('first key', 'en').within(() => {
      translationLabel.getTranslationLabels('first key', 'en').within(() => {
        gcy('translation-label')
          .should('have.length', 4)
          .then((labels) => {
            const labelTexts = Array.from(labels).map(
              (label) => label.textContent
            );
            expect(labelTexts).to.deep.equal([
              'First label',
              'label to assign 2',
              'label to assign 3',
              'Unassigned label',
            ]);
          });
      });
    });
  });
});
