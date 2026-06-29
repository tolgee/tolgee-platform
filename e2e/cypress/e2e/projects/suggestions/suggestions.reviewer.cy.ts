import { login } from '../../../common/apiCalls/common';
import { suggestionsTestData } from '../../../common/apiCalls/testData/testData';
import { waitForGlobalLoading } from '../../../common/loading';
import {
  assertMessage,
  assertNotMessage,
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

  it('reviewer can accept only, leaving the other suggestion active', () => {
    cy.intercept('PUT', /\/suggestion\/\d+\/accept/).as('accept');
    getTranslationCell('key 0', 'cs').click();
    waitForGlobalLoading();
    acceptOnly('Navržený překlad 0-1');
    cy.wait('@accept')
      .its('request.url')
      .should('not.contain', 'declineOther=true');
    assertMessage('Suggestion accepted');
    assertNotMessage('declined');
    waitForGlobalLoading();
    getTranslationCell('key 0', 'cs').click();
    cy.gcy('suggestions-list')
      .findDcy('translation-panel-items-count')
      .should('contain', '1');
    cy.gcy('suggestions-list')
      .findDcy('translation-suggestion')
      .contains('Navržený překlad 0-2')
      .should('exist');
    cy.gcy('suggestions-list')
      .findDcy('translation-suggestion')
      .contains('Navržený překlad 0-1')
      .should('not.exist');
  });

  it('accepted suggestion stays reviewed', () => {
    getTranslationCell('key 0', 'cs').click();
    waitForGlobalLoading();
    acceptOnly('Navržený překlad 0-1');
    waitForGlobalLoading();
    assertHasState('Navržený překlad 0-1', 'Reviewed');
  });

  it('accept and decline others declines the rest', () => {
    cy.intercept('PUT', /\/suggestion\/\d+\/accept/).as('accept');
    getTranslationCell('key 0', 'cs').click();
    waitForGlobalLoading();
    openRowMenu('Navržený překlad 0-1', 'accept-decline-others');
    cy.wait('@accept')
      .its('request.url')
      .should('contain', 'declineOther=true');
    assertMessage('Suggestion accepted, other variants declined (1)');
    waitForGlobalLoading();
    getTranslationCell('key 0', 'cs').click();
    cy.gcy('suggestions-list')
      .findDcy('translation-panel-items-count')
      .should('contain', '0');
  });

  it('reviewer can reverse inactive suggestion', () => {
    getTranslationCell('key 0', 'cs').click();
    waitForGlobalLoading();
    openRowMenu('Navržený překlad 0-1', 'accept-decline-others');
    waitForGlobalLoading();
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
    cy.gcy('suggestions-list')
      .findDcy('translation-suggestion')
      .should('have.length', 2);
    cy.gcy('translation-suggestion')
      .contains('Navržený překlad 0-1')
      .closest('[data-cy="translation-suggestion"]')
      .within(() => {
        gcyAdvanced({ value: 'suggestion-action', action: 'decline' }).click();
      });
    waitForGlobalLoading();
    cy.gcy('suggestions-list')
      .findDcy('translation-suggestion')
      .should('have.length', 1)
      .within(() => {
        gcyAdvanced({ value: 'suggestion-action', action: 'accept' }).should(
          'exist'
        );
        gcyAdvanced({ value: 'suggestion-action', action: 'menu' }).click();
      });
    gcyAdvanced({
      value: 'translation-suggestion-action-menu-item',
      action: 'delete',
    }).should('exist');
    gcyAdvanced({
      value: 'translation-suggestion-action-menu-item',
      action: 'accept-decline-others',
    }).should('not.exist');

    visitProjectDashboard(projectId);
    gcyAdvanced({
      value: 'activity-compact',
      type: 'DECLINE_SUGGESTION',
    }).should('contain', 'Navržený překlad');
  });

  it('reviewer can delete his own suggestion', () => {
    getTranslationCell('key 0', 'cs').click();
    waitForGlobalLoading();
    openRowMenu('Navržený překlad 0-2', 'delete');
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
    openRowMenu('Navržený překlad 0-2', 'accept-decline-others');
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

  it('single active suggestion shows accept inline without the overflow menu', () => {
    getTranslationCell('pluralKey', 'cs').click();
    waitForGlobalLoading();
    cy.gcy('suggestions-list')
      .findDcy('translation-suggestion')
      .should('have.length', 1)
      .within(() => {
        gcyAdvanced({ value: 'suggestion-action', action: 'accept' }).should(
          'exist'
        );
        gcyAdvanced({ value: 'suggestion-action', action: 'menu' }).should(
          'not.exist'
        );
        gcyAdvanced({ value: 'suggestion-action', action: 'accept' }).click();
      });
    assertMessage('Suggestion accepted');
  });

  it('read cell shows the next correct 3 suggestions after one is declined', () => {
    getTranslationCell('key 2', 'cs')
      .findDcy('translation-suggestion')
      .should('have.length', 3);
    getTranslationCell('key 2', 'cs').within(() => {
      cy.contains('+1').should('be.visible');
    });
    getTranslationCell('key 2', 'cs').click();
    waitForGlobalLoading();
    cy.gcy('suggestions-list')
      .findDcy('translation-suggestion')
      .first()
      .should('contain', 'Many suggestion 2-4');
    gcyAdvanced({ value: 'suggestion-action', action: 'decline' })
      .first()
      .click();
    waitForGlobalLoading();
    visitTranslations(projectId);
    waitForGlobalLoading();
    getTranslationCell('key 2', 'cs')
      .findDcy('translation-suggestion')
      .should('have.length', 3);
    getTranslationCell('key 2', 'cs').within(() => {
      cy.gcy('translation-suggestion')
        .eq(0)
        .should('contain', 'Many suggestion 2-3');
      cy.gcy('translation-suggestion')
        .eq(1)
        .should('contain', 'Many suggestion 2-2');
      cy.gcy('translation-suggestion')
        .eq(2)
        .should('contain', 'Many suggestion 2-1');
      cy.contains('+1').should('not.exist');
    });
  });

  function acceptOnly(text: string) {
    cy.gcy('translation-suggestion')
      .contains(text)
      .closest('[data-cy="translation-suggestion"]')
      .within(() => {
        gcyAdvanced({ value: 'suggestion-action', action: 'accept' }).click();
      });
  }

  function openRowMenu(
    text: string,
    action: 'accept-decline-others' | 'delete'
  ) {
    cy.gcy('translation-suggestion')
      .contains(text)
      .closest('[data-cy="translation-suggestion"]')
      .within(() => {
        gcyAdvanced({ value: 'suggestion-action', action: 'menu' }).click();
      });
    gcyAdvanced({
      value: 'translation-suggestion-action-menu-item',
      action,
    }).click();
  }
});
