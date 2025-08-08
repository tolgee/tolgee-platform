import { login } from '../../../common/apiCalls/common';
import { suggestionsTestData } from '../../../common/apiCalls/testData/testData';
import { waitForGlobalLoading } from '../../../common/loading';
import { assertMessage, gcyAdvanced } from '../../../common/shared';
import { assertHasState } from '../../../common/state';
import {
  getTranslationCell,
  visitTranslations,
} from '../../../common/translations';

describe('Suggestions in when translations are not protected', () => {
  let projectId: number;

  beforeEach(() => {
    suggestionsTestData.clean();
    suggestionsTestData
      .generate({
        suggestionsMode: 'ENABLED',
      })
      .then((r) => {
        projectId = r.body.projects[0].id;
      });
  });

  afterEach(() => {
    suggestionsTestData.clean();
  });

  it('translator can edit or suggest on reviewed translation', () => {
    login('translator@test.com');
    visitTranslations(projectId);
    getTranslationCell('key 1', 'cs').click();
    cy.gcy('global-editor').clear().type('Návrh 1');
    cy.gcy('translations-cell-main-action-button')
      .contains('Save')
      .should('be.visible');
    cy.gcy('translations-cell-menu-open-button').click();
    cy.gcy('translations-cell-menu-item').contains('Suggest').click();
    waitForGlobalLoading();
    cy.gcy('translation-suggestion').contains('Návrh 1').should('be.visible');
  });

  it('accepted suggestion stays reviewed', () => {
    login('reviewer@test.com');
    visitTranslations(projectId);
    getTranslationCell('key 0', 'cs').click();
    waitForGlobalLoading();
    gcyAdvanced({
      value: 'suggestion-action',
      action: 'accept',
    })
      .first()
      .click();
    waitForGlobalLoading();
    assertHasState('Navržený překlad 0-1', 'Reviewed');
  });

  it("can't add duplicate suggestion", () => {
    login('translator@test.com');
    visitTranslations(projectId);
    getTranslationCell('key 0', 'cs').click();
    cy.gcy('global-editor').clear().type('Navržený překlad 0-1');
    cy.gcy('translations-cell-menu-open-button').click();
    cy.gcy('translations-cell-menu-item').contains('Suggest').click();
    waitForGlobalLoading();
    assertMessage('Duplicate suggestion');
  });
});
