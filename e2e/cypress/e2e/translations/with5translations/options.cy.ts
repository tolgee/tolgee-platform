import { ProjectDTO } from '../../../../../webapp/src/service/response.types';
import {
  create4Translations,
  toggleLang,
  translationsBeforeEach,
  visitTranslations,
} from '../../../common/translations';

describe('Options with 5 Translations', () => {
  let project: ProjectDTO = null;

  beforeEach(() => {
    translationsBeforeEach()
      .then((p) => (project = p))
      .then(() => create4Translations(project.id));
  });

  describe('Options', () => {
    it('will remember selected language', () => {
      toggleLang('Česky');
      toggleLang('English');
      cy.contains('Studený přeložený text 1').should('be.visible');
      cy.contains('Cool translated text 1').should('not.exist');
      visit();
      cy.contains('Studený přeložený text 1').should('be.visible');
      cy.contains('Cool translated text 1').should('not.exist');
    });

    it('will select language', () => {
      toggleLang('Česky');
      cy.contains('Studený přeložený text 1').should('be.visible');
      toggleLang('Česky');
      cy.contains('Studený přeložený text 1').should('not.exist');
      toggleLang('English');
      cy.contains('Select at least one language').should('be.visible');
    });

    it('will search', () => {
      cy.gcy('global-search-field').type('Cool key 04');
      cy.contains('Cool key 01').should('not.exist');
      cy.contains('Cool key 04').should('be.visible');
    });
  });

  const visit = () => {
    visitTranslations(project.id);
  };
});
