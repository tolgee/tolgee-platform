import { login } from '../../../common/apiCalls/common';
import { suggestionsTestData } from '../../../common/apiCalls/testData/testData';
import { waitForGlobalLoading } from '../../../common/loading';
import { gcyAdvanced } from '../../../common/shared';
import {
  getCellCancelButton,
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
    cy.gcy('suggestions-list')
      .findDcy('translation-suggestion')
      .should('have.length', 2);
    gcyAdvanced({
      value: 'suggestion-action',
      action: 'accept',
    }).should('not.exist');
  });

  it('translator can delete his own suggestion', () => {
    visitTranslations(projectId);
    getTranslationCell('key 0', 'cs').click();
    waitForGlobalLoading();
    cy.gcy('translation-suggestion')
      .contains('Navržený překlad 0-1')
      .closest('[data-cy="translation-suggestion"]')
      .within(() => {
        gcyAdvanced({ value: 'suggestion-action', action: 'menu' }).click();
      });
    gcyAdvanced({
      value: 'translation-suggestion-action-menu-item',
      action: 'delete',
    }).click();
    waitForGlobalLoading();
    cy.gcy('translation-suggestion')
      .contains('Navržený překlad 0-1')
      .should('not.exist');
  });

  it('read cell shows the newest 3 suggestions + a "Show all" line, and keeps them after the editor opens and closes', () => {
    visitTranslations(projectId);
    assertReadCellSuggestions(['2-4', '2-3', '2-2']);
    getTranslationCell('key 2', 'cs')
      .findDcy('suggestions-show-all')
      .should('be.visible');
    getTranslationCell('key 2', 'cs').click();
    waitForGlobalLoading();
    getCellCancelButton().click();
    waitForGlobalLoading();
    assertReadCellSuggestions(['2-4', '2-3', '2-2']);
  });

  it('"Show all" opens the editor and reveals every active suggestion', () => {
    visitTranslations(projectId);
    getTranslationCell('key 2', 'cs').findDcy('suggestions-show-all').click();
    waitForGlobalLoading();
    cy.gcy('suggestions-list')
      .findDcy('translation-suggestion')
      .should('have.length', 4);
    cy.gcy('suggestions-list').should('contain', 'Many suggestion 2-1');
  });

  it('a view-only user sees the 3 previews but no "Show all" line (cannot open the cell)', () => {
    login('view.only@test.com');
    visitTranslations(projectId);
    assertReadCellSuggestions(['2-4', '2-3', '2-2']);
    getTranslationCell('key 2', 'cs')
      .findDcy('suggestions-show-all')
      .should('not.exist');
  });

  it('keeps up to 3 read-cell suggestions after suggesting on a cell that already has some', () => {
    visitTranslations(projectId);
    getTranslationCell('key 2', 'cs').click();
    cy.gcy('global-editor').clear().type('Brand new suggestion');
    cy.gcy('translations-cell-main-action-button')
      .should('contain', 'Suggest')
      .click();
    waitForGlobalLoading();
    getTranslationCell('key 2', 'cs')
      .findDcy('translation-suggestion')
      .should('have.length', 3);
    getTranslationCell('key 2', 'cs').within(() => {
      cy.gcy('translation-suggestion')
        .eq(0)
        .should('contain', 'Brand new suggestion');
      cy.gcy('translation-suggestion')
        .eq(1)
        .should('contain', 'Many suggestion 2-4');
      cy.gcy('translation-suggestion')
        .eq(2)
        .should('contain', 'Many suggestion 2-3');
    });
  });

  it('shows no "Show all" line when the cell has 3 or fewer suggestions', () => {
    visitTranslations(projectId);
    getTranslationCell('key 3', 'cs')
      .findDcy('translation-suggestion')
      .should('have.length', 1);
    getTranslationCell('key 3', 'cs')
      .findDcy('suggestions-show-all')
      .should('not.exist');
  });

  it('renders the 3-suggestion stack in table view too', () => {
    visitTranslations(projectId);
    cy.gcy('translations-view-table-button').click();
    waitForGlobalLoading();
    assertReadCellSuggestions(['2-4', '2-3', '2-2']);
  });

  it('"Show all" opens the editor from table view too', () => {
    visitTranslations(projectId);
    cy.gcy('translations-view-table-button').click();
    waitForGlobalLoading();
    getTranslationCell('key 2', 'cs').findDcy('suggestions-show-all').click();
    waitForGlobalLoading();
    cy.gcy('suggestions-list')
      .findDcy('translation-suggestion')
      .should('have.length', 4);
  });

  function assertReadCellSuggestions(suffixes: string[]) {
    getTranslationCell('key 2', 'cs')
      .findDcy('translation-suggestion')
      .should('have.length', suffixes.length);
    getTranslationCell('key 2', 'cs').within(() => {
      suffixes.forEach((suffix, index) => {
        cy.gcy('translation-suggestion')
          .eq(index)
          .should('contain', `Many suggestion ${suffix}`);
      });
    });
  }
});
