import {
  createKey,
  createProject,
  deleteProject,
  login,
} from './apiCalls/common';
import { HOST } from './constants';
import { ProjectDTO } from '../../../webapp/src/service/response.types';
import { waitForGlobalLoading } from './loading';
import { assertMessage, dismissMenu, gcyAdvanced } from './shared';
import { selectNamespace } from './namespace';
import { buildXpath } from './XpathBuilder';
import Chainable = Cypress.Chainable;

export function getCellCancelButton() {
  return cy.gcy('translations-cell-cancel-button');
}

export function getCellSaveButton() {
  return cy.gcy('translations-cell-save-button');
}

export function getCellInsertBaseButton() {
  return cy.gcy('translations-cell-insert-base-button');
}

export const getCell = (value: string) => {
  return cy.gcy('translations-table-cell').contains(value);
};

export const getTranslationCell = (key: string, language: string) => {
  return gcyAdvanced({ value: 'translations-table-cell', key, language });
};

export const getPluralEditor = (variant: string) => {
  return gcyAdvanced({ value: 'translation-editor', variant }).find(
    '[contenteditable]'
  );
};

type Props = {
  key: string;
  translation?: string | Record<string, string>;
  tag?: string;
  namespace?: string;
  description?: string;
  variableName?: string;
  assertPresenceOfNamespaceSelectBox?: boolean;
};

export function createTranslation({
  key,
  translation,
  tag,
  namespace,
  description,
  variableName,
  assertPresenceOfNamespaceSelectBox,
}: Props) {
  waitForGlobalLoading();
  cy.gcy('translations-add-button').click();
  if (assertPresenceOfNamespaceSelectBox != undefined) {
    cy.gcy('namespaces-selector').should(
      assertPresenceOfNamespaceSelectBox ? 'exist' : 'not.exist'
    );
  }
  cy.gcy('translation-create-key-input').type(key);
  if (namespace) {
    selectNamespace(namespace);
  }
  if (description) {
    cy.gcy('translation-create-description-input').type(description);
  }
  if (tag) {
    cy.gcy('translations-tag-input').type(tag);
    cy.gcy('tag-autocomplete-option').contains(`Add "${tag}"`).click();
  }
  if (typeof translation === 'string') {
    cy.gcy('translation-editor').first().type(translation);
  } else if (typeof translation === 'object') {
    cy.gcy('key-plural-checkbox').click();
    if (variableName) {
      cy.gcy('key-plural-checkbox-expand').click();
      cy.gcy('key-plural-variable-name').type(variableName);
    }
    Object.entries(translation).forEach(([key, value]) => {
      gcyAdvanced({ value: 'translation-editor', variant: key })
        .find('[contenteditable]')
        .type(value);
    });
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

export function translationsBeforeEach(
  languages?: string[]
): Chainable<ProjectDTO> {
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
      selectLangsInLocalstorage(project.id, languages ?? ['en']);
      return visitTranslations(project.id).then(() => project);
    });
  });
}

export const visitTranslations = (projectId: number) => {
  return cy.visit(`${HOST}/projects/${projectId}/translations`);
};

export const editCell = (oldValue: string, newValue?: string, save = true) => {
  getCell(oldValue).click();

  // wait for editor to appear
  cy.gcy('global-editor').should('be.visible');
  cy.contains(oldValue).first().should('be.visible');
  cy.wait(10);

  if (newValue !== undefined) {
    // select all, delete and type new text
    getEditor().clear().type(newValue);

    if (save) {
      getCellSaveButton().click();
    }
    waitForGlobalLoading();
  }
};

function getEditor() {
  return buildXpath()
    .descendant()
    .withDataCy('global-editor')
    .descendant()
    .withAttribute('contenteditable')
    .getElement();
}

export function editTranslation({
  key,
  languageTag,
  newValue,
}: {
  key: string;
  languageTag: string;
  newValue: string;
}) {
  const translationCell = buildXpath()
    .descendant()
    .withDataCy('translations-key-name')
    .descendantOrSelf()
    .hasText(key)
    .closestAncestor()
    .withDataCy('translations-row')
    .descendant()
    .attributeEquals('data-cy-language', languageTag)
    .getElement();

  translationCell.click();
  getEditor().clear().type(newValue);
  getCellSaveButton().click();
}

export function confirmDiscard() {
  cy.gcy('global-confirmation-confirm').contains('Discard').click();
}

export const toggleLang = (lang) => {
  cy.gcy('translations-language-select-form-control').click();
  cy.gcy('translations-language-select-item')
    .contains(lang)
    .should('be.visible')
    .click();
  dismissMenu();
  // wait for loading to disappear
  waitForGlobalLoading();
};

export const create4Translations = (projectId: number) => {
  const promises = [];
  for (let i = 1; i < 5; i++) {
    promises.push(
      createKey(projectId, `Cool key ${i.toString().padStart(2, '0')}`, {
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

export function createProjectWithThreeLanguages() {
  let project: ProjectDTO;

  return login()
    .then(() =>
      createProject({
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
          {
            tag: 'es',
            name: 'Spanish',
            originalName: 'espanol',
          },
        ],
      })
    )
    .then((r) => {
      project = r.body as ProjectDTO;
      selectLangsInLocalstorage(project.id, ['en']);
      const promises = [];
      for (let i = 1; i < 5; i++) {
        promises.push(
          createKey(project.id, `Cool key ${i.toString().padStart(2, '0')}`, {
            en: `Cool translated text ${i}`,
            cs: `Studený přeložený text ${i}`,
            es: `Texto traducido en frío ${i}`,
          })
        );
      }

      selectLangsInLocalstorage(project.id, ['en', 'cs', 'es']);
      return Cypress.Promise.all(promises);
    })
    .then(() => {
      return project;
    });
}
