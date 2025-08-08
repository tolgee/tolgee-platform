import { login } from '../../common/apiCalls/common';
import { suggestionsTestData } from '../../common/apiCalls/testData/testData';
import { HOST } from '../../common/constants';
import { waitForGlobalLoading } from '../../common/loading';
import { assertHasState } from '../../common/state';
import {
  getCell,
  getTranslationCell,
  visitTranslations,
} from '../../common/translations';

describe('Project settings reviewed translation protection', () => {
  let projectId: number;
  beforeEach(() => {
    suggestionsTestData.clean();
    suggestionsTestData.generate().then((r) => {
      projectId = r.body.projects[0].id;
    });
  });

  afterEach(() => {
    suggestionsTestData.clean();
  });

  it("can't edit reviewed translation", () => {
    enableTranslationsProtection();
    login('translator@test.com');
    visitTranslations(projectId);
    waitForGlobalLoading();
    cy.waitForDom();
    waitForGlobalLoading();
    getCell('Translation 0').click();
    cy.waitForDom();
    cy.gcy('global-editor').should('not.exist');
  });

  it('can edit unreviewed translation', () => {
    enableTranslationsProtection();
    login('translator@test.com');
    visitTranslations(projectId);
    waitForGlobalLoading();
    getTranslationCell('pluralKey', 'en').click();
    cy.waitForDom();
    cy.gcy('global-editor').should('be.visible');
  });

  it('translation will stay in reviewed state when edited', () => {
    enableTranslationsProtection();
    login('reviewer@test.com');
    visitTranslations(projectId);
    getCell('Translation 0').click();
    cy.gcy('global-editor')
      .should('be.visible')
      .clear()
      .type('Updated translation');
    cy.gcy('translations-cell-main-action-button')
      .should('contain', 'Save')
      .click();
    waitForGlobalLoading();
    assertHasState('Updated translation', 'Reviewed');
  });

  it('can create suggestion for reviewed translation', () => {
    enableTranslationsProtection(true);
    login('translator@test.com');
    visitTranslations(projectId);
    waitForGlobalLoading();
    getCell('Translation 0').click();
    cy.waitForDom();
    cy.gcy('global-editor')
      .should('be.visible')
      .clear()
      .type('My badass suggestion');
    cy.gcy('translations-cell-main-action-button')
      .should('contain', 'Suggest')
      .click();
    waitForGlobalLoading();
    getTranslationCell('key 0', 'en')
      .findDcy('translation-suggestion')
      .contains('My badass suggestion');
  });

  function enableTranslationsProtection(canSuggest?: boolean) {
    login('organization.owner@test.com');
    cy.visit(`${HOST}/projects/${projectId}/manage/edit/advanced`);
    cy.gcy('project-settings-translation-protection-switch').click();
    waitForGlobalLoading();
    if (canSuggest) {
      cy.gcy('project-settings-suggestions-mode-switch').click();
      waitForGlobalLoading();
    }
  }
});
