import { login } from '../../../common/apiCalls/common';
import { suggestionsTestData } from '../../../common/apiCalls/testData/testData';
import { waitForGlobalLoading } from '../../../common/loading';
import { gcyAdvanced } from '../../../common/shared';
import {
  getPluralEditor,
  getTranslationCell,
  visitTranslations,
} from '../../../common/translations';

describe('Suggestions translator', () => {
  let projectId: number;

  beforeEach(() => {
    suggestionsTestData.clean();
    suggestionsTestData
      .generate({
        suggestionsMode: 'ENABLED',
        translationProtection: 'PROTECT_REVIEWED',
      })
      .then((r) => {
        projectId = r.body.projects[0].id;
        login('translator@test.com');
      });
  });

  afterEach(() => {
    suggestionsTestData.clean();
  });

  it('translator can suggest on reviewed translation', () => {
    visitTranslations(projectId);
    getTranslationCell('key 1', 'cs').click();
    cy.gcy('global-editor').clear().type('Návrh 1');
    cy.gcy('translations-cell-main-action-button')
      .should('contain', 'Suggest')
      .click();
    waitForGlobalLoading();
    cy.gcy('translation-suggestion').contains('Návrh 1').should('be.visible');
  });

  it('translator can edit and suggest on unreviewed translation', () => {
    visitTranslations(projectId);
    getTranslationCell('pluralKey', 'cs').click();
    getPluralEditor('one').first().clear().type('# návrh');
    getPluralEditor('few').first().clear().type('# návrhy');
    getPluralEditor('other').first().clear().type('# návrhů');
    cy.gcy('translations-cell-main-action-button')
      .contains('Save')
      .should('be.visible');
    cy.gcy('translations-cell-menu-open-button').click();
    cy.gcy('translations-cell-menu-item').contains('Suggest').click();
    waitForGlobalLoading();
    cy.gcy('translation-suggestion').contains('návrhů').should('be.visible');
  });

  it("translator can't accept suggestion", () => {
    visitTranslations(projectId);
    getTranslationCell('key 0', 'cs').click();
    waitForGlobalLoading();
    gcyAdvanced({
      value: 'suggestion-action',
      action: 'accept',
    }).should('not.exist');
    gcyAdvanced({
      value: 'suggestion-action',
      action: 'menu',
    }).should('not.exist');
  });

  it('translator can delete his own suggestion', () => {
    visitTranslations(projectId);
    getTranslationCell('key 0', 'cs').click();
    waitForGlobalLoading();
    gcyAdvanced({
      value: 'suggestion-action',
      action: 'delete',
    })
      .should('exist')
      .click();
    waitForGlobalLoading();
    cy.gcy('translation-suggestion')
      .contains('Navržený překlad 0-1')
      .should('not.exist');
  });
});
