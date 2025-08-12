import { login } from '../../../common/apiCalls/common';
import { suggestionsTestData } from '../../../common/apiCalls/testData/testData';
import { waitForGlobalLoading } from '../../../common/loading';
import {
  assertMessage,
  gcyAdvanced,
  visitProjectDashboard,
} from '../../../common/shared';
import { assertHasState } from '../../../common/state';
import {
  getPluralEditor,
  getTranslationCell,
  visitTranslations,
} from '../../../common/translations';

describe('Suggestions reviewer', () => {
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
        login('reviewer@test.com');
        visitTranslations(projectId);
      });
  });

  afterEach(() => {
    suggestionsTestData.clean();
  });

  it('reviewer can edit and suggest on reviewed translation', () => {
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

  it('reviewer can edit and suggest on unreviewed translation', () => {
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

  it('reviewer can accept suggestion', () => {
    acceptSuggestion();
    assertMessage('Suggestion accepted, other variants declined (1)');
  });

  it('accepted suggestion stays reviewed', () => {
    acceptSuggestion();
    assertHasState('Navržený překlad 0-1', 'Reviewed');
  });

  it('acceptation of suggestion declines other suggestions', () => {
    acceptSuggestion();
    getTranslationCell('key 0', 'cs').click();
    cy.gcy('suggestions-list')
      .findDcy('translation-panel-items-count')
      .should('contain', '0');
  });

  it('reviewer can reverse inactive suggestion', () => {
    acceptSuggestion();
    getTranslationCell('key 0', 'cs').click();
    cy.gcy('suggestions-list')
      .findDcy('translation-tools-suggestions-show-all-checkbox')
      .click();
    waitForGlobalLoading();
    cy.gcy('suggestions-list')
      .findDcy('translation-suggestion')
      .should('have.length', 2);
    gcyAdvanced({ value: 'suggestion-action', action: 'reverse' })
      .first()
      .click();
    cy.gcy('suggestions-list')
      .findDcy('translation-tools-suggestions-show-all-checkbox')
      .click();
    waitForGlobalLoading();
    cy.gcy('suggestions-list')
      .findDcy('translation-suggestion')
      .should('have.length', 1);
    visitProjectDashboard(projectId);
    gcyAdvanced({
      value: 'activity-compact',
      type: 'SUGGESTION_SET_ACTIVE',
    }).should('contain', 'Navržený překlad');
  });

  it('reviewer can decline suggestion', () => {
    getTranslationCell('key 0', 'cs').click();
    waitForGlobalLoading();
    gcyAdvanced({
      value: 'suggestion-action',
      action: 'decline',
    })
      .first()
      .click();
    waitForGlobalLoading();
    cy.gcy('suggestions-list')
      .findDcy('translation-suggestion')
      .should('have.length', 1);

    visitProjectDashboard(projectId);
    gcyAdvanced({
      value: 'activity-compact',
      type: 'DECLINE_SUGGESTION',
    }).should('contain', 'Navržený překlad');
  });

  it('reviewer can delete his own suggestion', () => {
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
      .contains('Navržený překlad 0-2')
      .should('not.exist');

    visitProjectDashboard(projectId);
    gcyAdvanced({
      value: 'activity-compact',
      type: 'DELETE_SUGGESTION',
    }).should('contain', 'Navržený překlad');
  });

  it('reviewer can accept his own suggestion', () => {
    getTranslationCell('key 0', 'cs').click();
    waitForGlobalLoading();
    gcyAdvanced({
      value: 'suggestion-action',
      action: 'menu',
    })
      .should('exist')
      .click();
    cy.gcy('translation-suggestion-action-menu-item')
      .contains('Accept suggestion')
      .click();
    waitForGlobalLoading();
    cy.gcy('translation-suggestion')
      .contains('Navržený překlad 0-2')
      .should('not.exist');
    assertMessage('Suggestion accepted, other variants declined (1)');

    visitProjectDashboard(projectId);
    gcyAdvanced({
      value: 'activity-compact',
      type: 'ACCEPT_SUGGESTION',
    }).should('contain', 'Navržený překlad 0-1');
  });

  function acceptSuggestion() {
    getTranslationCell('key 0', 'cs').click();
    waitForGlobalLoading();
    gcyAdvanced({
      value: 'suggestion-action',
      action: 'accept',
    })
      .first()
      .click();
    waitForGlobalLoading();
  }
});
