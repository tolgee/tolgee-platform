import { setLanguageData, visitLanguageSettings } from '../../common/languages';
import {
  assertMessage,
  gcy,
  selectInSelect,
  visitProjectLanguages,
  visitProjectSettings,
} from '../../common/shared';
import { languagesTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';

describe('Language modification', () => {
  let projectId: number;

  beforeEach(() => {
    languagesTestData.clean();

    languagesTestData.generate().then((languageData) => {
      projectId = languageData.body.id;
      login('franta');
      visitProjectLanguages(projectId);
    });
  });

  it('modifies language', () => {
    visitLanguageSettings('en');
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
    visitLanguageSettings('en');
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
