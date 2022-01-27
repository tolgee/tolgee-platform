import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import {
  create4Translations,
  createTranslation,
  selectLangsInLocalstorage,
  translationsBeforeEach,
  visitTranslations,
} from '../../common/translations';
import { waitForGlobalLoading } from '../../common/loading';
import { HOST } from '../../common/constants';
import { getCell } from '../../common/state';
import { deleteProject } from '../../common/apiCalls/common';

describe('Translation memory', () => {
  let project: ProjectDTO = null;

  beforeEach(() => {
    translationsBeforeEach()
      .then((p) => (project = p))
      .then(() => {
        create4Translations(project.id);
        selectLangsInLocalstorage(project.id, ['en', 'cs']);
      });
  });

  afterEach(() => {
    deleteProject(project.id);
  });

  it("doesn't trigger auto translation when not enabled", () => {
    visit();
    waitForGlobalLoading();
    createTranslation('mykey', 'Cool translated text 1');
    waitForGlobalLoading();
    cy.gcy('translations-table-cell').contains('mykey').should('be.visible');
    cy.gcy('translations-table-cell')
      .filter(':contains("Cool translated text 1")')
      .should('have.length', 2);
    cy.gcy('translations-table-cell')
      .filter(':contains("Studený přeložený text 1")')
      .should('have.length', 1);
  });

  it('translate with machine translations', () => {
    enableSettings({ translationMemory: true, machineTranslation: true });
    visit();
    waitForGlobalLoading();
    createTranslation('mykey', 'mytranslation');
    waitForGlobalLoading();
    cy.gcy('translations-table-cell').contains('mykey').should('be.visible');
    cy.gcy('translations-table-cell')
      .contains('mytranslation')
      .should('be.visible');
    cy.gcy('translations-table-cell')
      .contains('mytranslation translated with GOOGLE from en to cs')
      .should('be.visible');
    getAutoTranslatedIndicator(
      'mytranslation translated with GOOGLE from en to cs'
    ).should('be.visible');
  });

  it('translate with translation memory', () => {
    enableSettings({ translationMemory: true, machineTranslation: true });
    visit();
    waitForGlobalLoading();
    createTranslation('mykey', 'Cool translated text 1');
    waitForGlobalLoading();
    cy.gcy('translations-table-cell').contains('mykey').should('be.visible');
    cy.gcy('translations-table-cell')
      .filter(':contains("Cool translated text 1")')
      .should('have.length', 2);
    cy.gcy('translations-table-cell')
      .filter(':contains("Studený přeložený text 1")')
      .should('have.length', 2);
    getAutoTranslatedIndicator('Studený přeložený text 1')
      .should('be.visible')
      .findDcy('translations-auto-translated-clear-button')
      .invoke('show')
      .click();
    // auto translated indicator is clearable
    waitForGlobalLoading();
    getAutoTranslatedIndicator('Studený přeložený text 1').should('not.exist');
  });

  const enableSettings = ({ translationMemory, machineTranslation }) => {
    cy.visit(`${HOST}/projects/${project.id}/languages`);
    waitForGlobalLoading();
    if (translationMemory) {
      cy.gcy('languages-auto-translation-memory').click();
    }
    if (machineTranslation) {
      cy.gcy('languages-auto-machine-translation').click();
    }
    waitForGlobalLoading();
  };

  const getAutoTranslatedIndicator = (translationText: string) => {
    return getCell(translationText).findDcy(
      'translations-auto-translated-indicator'
    );
  };

  const visit = () => {
    visitTranslations(project.id);
  };
});
