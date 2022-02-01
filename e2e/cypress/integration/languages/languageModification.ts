import {
  cleanLanguagesData,
  generateLanguagesData,
  login,
} from '../../common/apiCalls';
import {
  setLanguageData,
  visitLanguageSettings,
  visitProjectLanguages,
  visitProjectSettings,
} from '../../common/languages';
import { assertMessage, gcy, selectInSelect } from '../../common/shared';

describe('Language modification', () => {
  let projectId: number;

  beforeEach(() => {
    cleanLanguagesData();

    generateLanguagesData().then((languageData) => {
      projectId = languageData.body.id;
      login('franta');
      visitProjectLanguages(projectId);
    });
  });

  it('modifies language', () => {
    visitLanguageSettings('English');
    setLanguageData({
      name: 'Modified',
      originalName: 'Modified Original',
      tag: 'tg',
      flagEmoji: '🇦🇿',
    });
    gcy('global-form-save-button').click();
    gcy('project-settings-languages-list-name')
      .contains('English (en)')
      .should('be.visible')
      .find('img')
      .should('have.attr', 'alt', '🇦🇿');
  });

  it('cannot delete base language', () => {
    visitLanguageSettings('English');
    gcy('language-delete-button').click();
    assertMessage('Cannot delete base language');
  });

  it('Sets project base language', () => {
    visitProjectSettings(projectId);
    selectInSelect(gcy('base-language-select'), 'German');
    gcy('global-form-save-button').click();
    cy.reload();
    gcy('base-language-select').should('contain', 'German');
    gcy('base-language-select').find('img').should('have.attr', 'alt', '🇩🇪');
  });
});
