import { login } from '../../common/apiCalls/common';
import { labelsTestData } from '../../common/apiCalls/testData/testData';

import {
  getTranslationCell,
  visitTranslations,
} from '../../common/translations';
import { gcy } from '../../common/shared';
import { isDarkMode } from '../../common/helpers';
import {
  assignLabelToTranslation,
  verifyLabelInTranslationCell,
  verifyLabelsCountInTranslationCell,
} from '../../common/label';

let projectId = null;
let emptyProjectId = null;

describe('Projects Settings - Labels', () => {
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
    assignLabelToTranslation('first key', 'en', 'Label to assign 1', 4);

    // Verify label after assigning
    verifyLabelsCountInTranslationCell('first key', 'en', 2);
    verifyLabelInTranslationCell(
      'first key',
      'en',
      'Label to assign 1',
      isDarkMode ? 'rgba(255, 0, 255, 0.85)' : 'rgb(255, 0, 255)'
    );

    // refresh the page to ensure the label is saved
    cy.reload();

    // verify label after page reload
    verifyLabelsCountInTranslationCell('first key', 'en', 2);
    verifyLabelInTranslationCell(
      'first key',
      'en',
      'Label to assign 1',
      isDarkMode ? 'rgba(255, 0, 255, 0.85)' : 'rgb(255, 0, 255)'
    );
  });

  it('remove label from translation', () => {
    visitTranslations(projectId);
    getTranslationCell('first key', 'en').within(() => {
      gcy('translation-label-delete')
        .invoke('css', 'opacity', 1) // hover is not supported in Cypress, had to use CSS opacity
        .click();
    });

    // verify the label is removed
    verifyLabelsCountInTranslationCell('first key', 'en', 0);
    // refresh the page to ensure the removal persists
    cy.reload();
    verifyLabelsCountInTranslationCell('first key', 'en', 0);
  });

  it('filters by label', () => {
    visitTranslations(projectId);
    gcy('translations-row').should('have.length', 2);
    gcy('translations-filter-select').click();
    cy.waitForDom();
    gcy('submenu-item').contains('Labels').should('exist').click();
    gcy('filter-item').contains('First label').click();
    gcy('translations-filter-select').contains('First label');
    gcy('translations-row').should('have.length', 0);
    gcy('translations-filter-apply-for-expand').click();
    gcy('translations-filter-apply-for-all').click();
    gcy('translations-row').contains('first key').should('be.visible');
    gcy('translations-row').should('have.length', 1);
    gcy('translations-filter-apply-for-language').contains('English').click();
    gcy('translations-row').should('have.length', 1);
    gcy('translations-filter-apply-for-language').contains('Czech').click();
    gcy('translations-row').should('have.length', 0);
  });

  it('filters has not labels when no labels exists', () => {
    visitTranslations(emptyProjectId);
    cy.gcy('translations-filter-select').click();
    cy.waitForDom();
    cy.gcy('submenu-item').contains('Labels').should('not.exist');
  });
});
