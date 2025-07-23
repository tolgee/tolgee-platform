import {
  createKey,
  createProject,
  deleteProject,
  login,
} from './apiCalls/common';
import { HOST } from './constants';
import { ProjectDTO } from '../../../webapp/src/service/response.types';
import { waitForGlobalLoading } from './loading';
import { dismissMenu, gcy, gcyAdvanced } from './shared';
import { buildXpath } from './XpathBuilder';
import { KeyDialogFillProps } from '../compounds/E2KeyCreateDialog';
import { E2TranslationsView } from '../compounds/E2TranslationsView';
import Chainable = Cypress.Chainable;

export function getCellCancelButton() {
  return cy.gcy('translations-cell-cancel-button');
}

export function getCellSaveButton() {
  return cy.gcy('translations-cell-main-action-button').contains('Save');
}

export function getCellInsertBaseButton() {
  return cy.gcy('translations-cell-insert-base-button');
}

export const getCell = (value: string) => {
  return cy.gcy('translations-table-cell').contains(value);
};

export const getPluralEditor = (variant: string) => {
  return gcyAdvanced({ value: 'translation-editor', variant }).find(
    '[contenteditable]'
  );
};

export function createTranslation(props: KeyDialogFillProps) {
  waitForGlobalLoading();
  const translationsView = new E2TranslationsView();
  const keyCreateDialog = translationsView.openKeyCreateDialog();
  keyCreateDialog.fillAndSave(props);
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

export function openKeyEditDialog(keyName: string) {
  getKeyCell(keyName).click();
  gcy('translations-key-edit-key-field').should('be.visible');
}

export function typeNewKeyName(newName: string) {
  // select all, delete and type new text
  gcy('translations-key-edit-key-field')
    .should('be.visible')
    .find('[contenteditable]')
    .clear()
    .type(newName);
}

export function saveKeyEditDialog() {
  getCellSaveButton().click();
  waitForGlobalLoading();
}

export const editKeyName = (keyName: string, newName?: string) => {
  openKeyEditDialog(keyName);

  if (newName !== undefined) {
    typeNewKeyName(newName);
    saveKeyEditDialog();
  }
};

export const editCell = (oldValue: string, newValue?: string, save = true) => {
  getCell(oldValue).click();

  // wait for editor to appear
  cy.gcy('global-editor').should('be.visible');
  cy.contains(oldValue).first().should('be.visible');
  cy.wait(10);

  if (newValue !== undefined) {
    // select all, delete and type new text
    getTranslationEditor().clear().type(newValue);

    if (save) {
      getCellSaveButton().click();
    }
    waitForGlobalLoading();
  }
};

export function getTranslationEditor() {
  return buildXpath()
    .descendant()
    .withDataCy('global-editor')
    .descendant()
    .withAttribute('contenteditable')
    .getElement();
}

function getKeyCellXpath(key: string) {
  return buildXpath()
    .descendant()
    .withDataCy('translations-key-name')
    .descendantOrSelf()
    .hasText(key);
}

function getKeyCell(key: string) {
  return getKeyCellXpath(key).getElement();
}

export function getTranslationCell(key: string, languageTag: string) {
  return getKeyCellXpath(key)
    .closestAncestor()
    .withDataCy('translations-row')
    .descendant()
    .attributeEquals('data-cy-language', languageTag)
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
  const translationCell = getTranslationCell(key, languageTag);

  translationCell.click();
  getTranslationEditor().clear().type(newValue);
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
