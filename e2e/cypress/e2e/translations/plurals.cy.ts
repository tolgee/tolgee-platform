import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import {
  createTranslation,
  getCell,
  getTranslationCell,
  translationsBeforeEach,
  visitTranslations,
} from '../../common/translations';
import { waitForGlobalLoading } from '../../common/loading';
import { createKey, deleteProject } from '../../common/apiCalls/common';

describe('Translations Base', () => {
  let project: ProjectDTO = null;

  beforeEach(() => {
    translationsBeforeEach(['en', 'cs']).then((p) => (project = p));
  });

  afterEach(() => {
    deleteProject(project.id);
  });

  it('will switch translation to plural, without a problem', () => {
    cy.wait(100);
    cy.gcy('global-empty-list').should('be.visible');
    createTranslation({
      key: 'Test key',
      translation: 'Translated key with { stuff to escape',
    });

    getCell('Test key').click();
    cy.gcy('key-plural-checkbox').click();
    cy.gcy('translations-cell-save-button').click();
    waitForGlobalLoading();
    getTranslationCell('Test key', 'en')
      .contains("Translated key with '{' stuff to escape")
      .should('be.visible');
  });

  it('will change plural parameter name for all translations', () => {
    createKey(
      project.id,
      'Test key',
      {
        en: '{value, plural, one {# item} other {# items}}',
        cs: '{value, plural, one {# položka} few {# položky} other {# položek}}',
      },
      { isPlural: true }
    );
    visitTranslations(project.id);
    waitForGlobalLoading();

    getCell('Test key').click();
    cy.gcy('key-plural-checkbox-expand').click();
    cy.gcy('key-plural-variable-name').clear().type('testVariable');
    cy.gcy('translations-cell-save-button').click();
    waitForGlobalLoading();
  });
});
