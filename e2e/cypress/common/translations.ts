import { getAnyContainingText } from './xPath';
import {
  createProject,
  deleteProject,
  login,
  setTranslations,
} from './apiCalls';
import { HOST } from './constants';
import { ProjectDTO } from '../../../webapp/src/service/response.types';
import Chainable = Cypress.Chainable;
import { waitForGlobalLoading } from './loading';

export function getCellCancelButton() {
  return cy.gcy('translations-cell-cancel-button');
}

export function getCellSaveButton() {
  return cy.gcy('translations-cell-save-button');
}

export function createTranslation(testKey: string, testTranslated?: string) {
  waitForGlobalLoading();
  cy.gcy('translations-add-button').click();
  cy.get('.CodeMirror-code')
    .should('be.visible')
    .type(testKey, { force: true });
  cy.xpath(getAnyContainingText('save')).click();
  cy.contains('Key created').should('be.visible');

  if (testTranslated) {
    cy.gcy('translations-view-list').contains('en').first().click();
    cy.get('.CodeMirror-code')
      .should('be.visible')
      .type(testTranslated, { force: true });
    cy.xpath(getAnyContainingText('save')).click();
    waitForGlobalLoading();
  }
}

export function translationsBeforeEach(): Chainable<ProjectDTO> {
  return login().then(() => {
    return createProject({
      name: 'Test',
      languages: [
        {
          tag: 'en',
          name: 'English',
          originalName: 'English',
        },
        {
          tag: 'cs',
          name: 'Česky',
          originalName: 'česky',
        },
      ],
    }).then((r) => {
      const project = r.body as ProjectDTO;
      window.localStorage.setItem(
        'selectedLanguages',
        `{"${project.id}":["en"]}`
      );
      return visitTranslations(project.id).then(() => project);
    });
  });
}

export const visitTranslations = (projectId: number) => {
  return cy.visit(`${HOST}/projects/${projectId}/translations`);
};

export const editCell = (oldValue: string, newValue: string, save = true) => {
  cy.contains(oldValue).click();

  // wait for editor to appear
  cy.gcy('global-editor').should('be.visible');
  cy.contains(oldValue).should('be.visible');
  // select all, delete and type new text
  cy.focused().type('{meta}a').type('{backspace}').type(newValue);

  if (save) {
    getCellSaveButton().click();
  }
};

export function clickDiscardChanges() {
  cy.xpath(getAnyContainingText('Discard changes', 'button')).click();
}

export const toggleLang = (lang) => {
  cy.get('#languages-select-translations').click();
  cy.get('#language-select-translations-menu')
    .contains(lang)
    .should('be.visible')
    .click();
  cy.get('body').click();
  // wait for loading to disappear
  waitForGlobalLoading();
};

export const create4Translations = (projectId: number) => {
  const promises = [];
  for (let i = 1; i < 5; i++) {
    promises.push(
      setTranslations(projectId, `Cool key ${i.toString().padStart(2, '0')}`, {
        en: `Cool translated text ${i}`,
        cs: `Studený přeložený text ${i}`,
      })
    );
  }

  Cypress.Promise.all(promises).then(() => {
    visitTranslations(projectId);
  });

  // wait for loading to appear and disappear again
  cy.gcy('global-base-view-content').should('be.visible');
  waitForGlobalLoading();
};

export const forEachView = (
  projectIdProvider: () => number,
  testFn: () => void
) => {
  ['list', 'table'].forEach((viewType) => {
    describe(`with ${viewType} view`, () => {
      afterEach(() => {
        deleteProject(projectIdProvider());
      });

      beforeEach(() => {
        if (viewType === 'table') {
          cy.gcy('translations-view-table-button').click();
        }
      });
      testFn();
    });
  });
};
