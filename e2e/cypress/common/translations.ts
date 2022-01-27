import {
  createProject,
  deleteProject,
  login,
  setTranslations,
} from './apiCalls/common';
import { HOST } from './constants';
import { ProjectDTO } from '../../../webapp/src/service/response.types';
import { waitForGlobalLoading } from './loading';
import { assertMessage } from './shared';
import Chainable = Cypress.Chainable;

export function getCellCancelButton() {
  return cy.gcy('translations-cell-cancel-button');
}

export function getCellSaveButton() {
  return cy.gcy('translations-cell-save-button');
}

export function createTranslation(
  testKey: string,
  translation?: string,
  tag?: string
) {
  waitForGlobalLoading();
  cy.gcy('translations-add-button').click();
  cy.gcy('translation-create-key-input').type(testKey);
  if (tag) {
    cy.gcy('translations-tag-input').type(tag);
    cy.gcy('tag-autocomplete-option').contains(`Add "${tag}"`).click();
  }
  if (translation) {
    cy.gcy('translation-create-translation-input').first().type(translation);
  }

  cy.gcy('global-form-save-button').click();
  assertMessage('Key created');
}

export function selectLangsInLocalstorage(projectId: number, langs: string[]) {
  window.localStorage.setItem(
    'selectedLanguages',
    JSON.stringify({ [projectId]: langs })
  );
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
      selectLangsInLocalstorage(project.id, ['en']);
      return visitTranslations(project.id).then(() => project);
    });
  });
}

export const visitTranslations = (projectId: number) => {
  return cy.visit(`${HOST}/projects/${projectId}/translations`);
};

export const editCell = (oldValue: string, newValue?: string, save = true) => {
  cy.gcy('translations-table-cell').contains(oldValue).click();

  // wait for editor to appear
  cy.gcy('global-editor').should('be.visible');
  cy.contains(oldValue).should('be.visible');
  cy.wait(10);

  if (newValue) {
    // select all, delete and type new text
    cy.focused().type('{meta}a').type('{backspace}').type(newValue);

    if (save) {
      getCellSaveButton().click();
    }
    waitForGlobalLoading();
  }
};

export function confirmSaveChanges() {
  cy.gcy('global-confirmation-confirm').contains('Save').click();
}

export const toggleLang = (lang) => {
  cy.gcy('translations-language-select-form-control').click();
  cy.gcy('translations-language-select-item')
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
