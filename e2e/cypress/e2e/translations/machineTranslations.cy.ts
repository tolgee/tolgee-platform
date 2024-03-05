import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import {
  create4Translations,
  editCell,
  selectLangsInLocalstorage,
  translationsBeforeEach,
  visitTranslations,
} from '../../common/translations';
import { waitForGlobalLoading } from '../../common/loading';
import { deleteProject } from '../../common/apiCalls/common';

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

  afterEach(() => {
    deleteProject(project.id);
  });

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

  const visit = () => {
    visitTranslations(project.id);
  };

  const openEditor = (text: string) => {
    cy.contains(text).click();
    cy.gcy('global-editor').should('be.visible');
  };
});
