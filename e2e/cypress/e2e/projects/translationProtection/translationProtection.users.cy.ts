import { login } from '../../../common/apiCalls/common';
import { suggestionsTestData } from '../../../common/apiCalls/testData/testData';
import { waitForGlobalLoading } from '../../../common/loading';
import { assertHasState } from '../../../common/state';
import {
  getPluralEditor,
  getTranslationCell,
  visitTranslations,
} from '../../../common/translations';

describe('Reviewed translations protection', () => {
  let projectId: number;

  beforeEach(() => {
    suggestionsTestData.clean();
    suggestionsTestData
      .generate({ translationProtection: 'PROTECT_REVIEWED' })
      .then((r) => {
        projectId = r.body.projects[0].id;
      });
  });

  afterEach(() => {
    suggestionsTestData.clean();
  });

  it('translator cannot edit reviewed translation', () => {
    login('translator@test.com');
    visitTranslations(projectId);
    getTranslationCell('key 1', 'cs').click();
    cy.gcy('global-editor').should('not.exist');
  });

  it('translator can edit unreviewed translation', () => {
    login('translator@test.com');
    visitTranslations(projectId);
    getTranslationCell('pluralKey', 'cs').click();
    cy.gcy('global-editor').should('be.visible');
    getPluralEditor('one').first().clear().type('# nový překlad');
    getPluralEditor('few').first().clear().type('# nové překlady');
    getPluralEditor('other').first().clear().type('# nových překladů');
    cy.gcy('translations-cell-main-action-button')
      .should('contain', 'Save')
      .click();
    waitForGlobalLoading();
    getTranslationCell('pluralKey', 'cs').contains('nových překladů');
  });

  it('reviewer can edit reviewed translation and it stays reviewed', () => {
    login('reviewer@test.com');
    visitTranslations(projectId);
    getTranslationCell('key 1', 'cs').click();
    cy.gcy('global-editor')
      .should('be.visible')
      .clear()
      .type('Edited translation 1');
    cy.gcy('translations-cell-main-action-button')
      .should('contain', 'Save')
      .click();
    waitForGlobalLoading();
    assertHasState('Edited translation 1', 'Reviewed');
  });
});
