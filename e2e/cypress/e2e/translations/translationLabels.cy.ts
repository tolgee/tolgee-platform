import { login } from '../../common/apiCalls/common';
import { labelsTestData } from '../../common/apiCalls/testData/testData';

import {
  getTranslationCell,
  visitTranslations,
} from '../../common/translations';
import { gcy } from '../../common/shared';

let projectId = null;

const verifyLabelInTranslationCell = (
  key: string,
  lang: string,
  label: string,
  expectedColor: string
) => {
  getTranslationCell(key, lang).within(() => {
    gcy('translation-label')
      .contains(label)
      .parent('[data-cy="translation-label"]')
      .should('be.visible')
      .should('have.css', 'background-color', expectedColor);
  });
};

const verifyLabelsCountInTranslationCell = (
  key: string,
  lang: string,
  expectedCount: number
) => {
  getTranslationCell(key, lang).within(() => {
    gcy('translation-label').should('have.length', expectedCount);
  });
};

describe('Projects Settings - Labels', () => {
  beforeEach(() => {
    labelsTestData.clean();
    labelsTestData.generate().then((data) => {
      login('test_username');
      projectId = data.body.projects[0].id;
      visitTranslations(projectId);
    });
  });

  it('see translation label', () => {
    getTranslationCell('first key', 'en').within(() => {
      gcy('translation-label')
        .should('have.css', 'background-color', 'rgb(255, 0, 0)')
        .contains('First label');
    });
  });

  it('search and add label to translation', () => {
    getTranslationCell('first key', 'en').within(($cell) => {
      gcy('translation-label-control')
        .should('not.be.visible')
        .click()
        .should('be.visible');
      gcy('autocomplete-label-input').should('be.visible').click();
      gcy('label-selector-autocomplete').should('be.visible');
    });
    gcy('label-autocomplete-option')
      .should('have.length', 4)
      .first()
      .contains('Label to assign 1')
      .should('be.visible')
      .click();

    // Verify label after assigning
    verifyLabelsCountInTranslationCell('first key', 'en', 2);
    verifyLabelInTranslationCell(
      'first key',
      'en',
      'Label to assign 1',
      'rgb(255, 0, 255)'
    );

    // refresh the page to ensure the label is saved
    cy.reload();

    // verify label after page reload
    verifyLabelsCountInTranslationCell('first key', 'en', 2);
    verifyLabelInTranslationCell(
      'first key',
      'en',
      'Label to assign 1',
      'rgb(255, 0, 255)'
    );
  });

  it('remove label from translation', () => {
    getTranslationCell('first key', 'en').within(() => {
      gcy('translation-label').should('have.length', 1).contains('First label');
      gcy('translation-label')
        .should('be.visible')
        .within(() => {
          gcy('translation-label-delete')
            .invoke('css', 'width', '16px') // hover is not supported in Cypress, had to use CSS width
            .click();
        });
    });

    // verify the label is removed
    verifyLabelsCountInTranslationCell('first key', 'en', 0);
    // refresh the page to ensure the removal persists
    cy.reload();
    verifyLabelsCountInTranslationCell('first key', 'en', 0);
  });
});
