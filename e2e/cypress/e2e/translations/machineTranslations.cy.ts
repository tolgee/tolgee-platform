import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import {
  create4Translations,
  editCell,
  getPluralEditor,
  getTranslationCell,
  selectLangsInLocalstorage,
  translationsBeforeEach,
  visitTranslations,
} from '../../common/translations';
import { waitForGlobalLoading } from '../../common/loading';

describe('Translation memory', () => {
  let project: ProjectDTO = null;

  beforeEach(() => {
    translationsBeforeEach()
      .then((p) => (project = p))
      .then(() => {
        create4Translations(project.id);
        selectLangsInLocalstorage(project.id, ['en', 'cs']);
        visit();
      });
  });

  // afterEach(() => {
  //   deleteProject(project.id);
  // });

  it('will show correct suggestions', () => {
    waitForGlobalLoading();
    openEditor('Studený přeložený text 1');
    cy.gcy('translation-tools-machine-translation-item')
      .contains('Cool translated text 1 translated with GOOGLE from en to cs')
      .should('be.visible');
    cy.gcy('translation-tools-machine-translation-item')
      .contains('Cool translated text 1 translated with AWS from en to cs')
      .should('be.visible');
  });

  it('will apply suggestion on click', () => {
    waitForGlobalLoading();
    openEditor('Studený přeložený text 1');
    cy.gcy('translation-tools-machine-translation-item')
      .contains('Cool translated text 1 translated with GOOGLE from en to cs')
      .should('be.visible')
      .click();
    waitForGlobalLoading(300);
    cy.gcy('global-editor')
      .contains('Cool translated text 1 translated with GOOGLE from en to cs')
      .should('be.visible');
  });

  it('will update suggestions when base is changed', () => {
    waitForGlobalLoading();
    openEditor('Studený přeložený text 1');
    cy.gcy('translation-tools-machine-translation-item')
      .contains('Cool translated text 1 translated with GOOGLE from en to cs')
      .should('be.visible');

    editCell('Cool translated text 1', 'Cool translated text 1 edited', true);
    waitForGlobalLoading();
    openEditor('Studený přeložený text 1');

    cy.gcy('translation-tools-machine-translation-item')
      .contains(
        'Cool translated text 1 edited translated with GOOGLE from en to cs'
      )
      .should('be.visible');
  });

  it('will suggest correctly when key is plural', () => {
    // edit key to be plural
    waitForGlobalLoading();
    openEditor('Cool key 01');
    cy.gcy('key-plural-checkbox').click();
    cy.gcy('translations-cell-save-button').click();

    waitForGlobalLoading();

    getTranslationCell('Cool key 01', 'en').click();
    getPluralEditor('one').type('# item');
    getPluralEditor('other').clear().type('# items');
    cy.gcy('translations-cell-save-button').click();
    waitForGlobalLoading();

    // check variant "one"
    getTranslationCell('Cool key 01', 'cs').click();
    getPluralEditor('one').click();
    waitForGlobalLoading();
    cy.gcy('translation-tools-machine-translation-item')
      .contains('#1 item translated with GOOGLE from en to cs')
      .should('be.visible')
      .click();

    // check variant "few"
    getPluralEditor('few').click();
    waitForGlobalLoading();
    cy.gcy('translation-tools-machine-translation-item')
      .contains('#2 items translated with GOOGLE from en to cs')
      .should('be.visible')
      .click();

    // check variant "other"
    getPluralEditor('other').click();
    waitForGlobalLoading();
    cy.gcy('translation-tools-machine-translation-item')
      .contains('#10 items translated with GOOGLE from en to cs')
      .should('be.visible')
      .click();

    cy.gcy('translations-cell-save-button').click();
    waitForGlobalLoading();
    cy.gcy('global-editor').should('not.exist');
  });

  const visit = () => {
    visitTranslations(project.id);
  };

  const openEditor = (text: string) => {
    cy.contains(text).click();
    cy.gcy('global-editor').should('be.visible');
  };
});
