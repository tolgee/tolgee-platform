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

export function getCellEditButton(content: string) {
  return cy
    .contains(content)
    .xpath(
      "./ancestor::*[@data-cy='translations-table-cell']//*[@data-cy='translations-cell-edit-button']"
    )
    .invoke('show');
}

export function getCellCancelButton() {
  return cy
    .gcy('global-editor')
    .xpath(
      "./ancestor::*[@data-cy='translations-table-cell']//*[@data-cy='translations-cell-cancel-button']"
    );
}

export function getCellSaveButton() {
  return cy
    .gcy('global-editor')
    .xpath(
      "./ancestor::*[@data-cy='translations-table-cell']//*[@data-cy='translations-cell-save-button']"
    );
}

export function createTranslation(testKey: string, testTranslated?: string) {
  cy.gcy('translations-add-button').click();
  cy.gcy('global-editor').should('be.visible');
  cy.gcy('global-editor').find('textarea').type(testKey, { force: true });
  cy.xpath(getAnyContainingText('save')).click();
  cy.contains('Key created').should('be.visible');

  if (testTranslated) {
    cy.gcy('translations-view-list').contains('en').first().click();
    cy.gcy('global-editor')
      .find('textarea')
      .type(testTranslated, { force: true });
    cy.xpath(getAnyContainingText('save')).click();
    cy.gcy('global-base-view-loading').should('not.exist');
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
        },
        {
          tag: 'cs',
          name: 'Česky',
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
  getCellEditButton(oldValue).click();

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
  cy.gcy('global-base-view-loading').should('not.exist');
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
  cy.gcy('global-base-view-loading').should('not.exist');
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
