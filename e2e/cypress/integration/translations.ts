import { clickAdd } from '../common/shared';
import {
  getAnyContainingText,
  getClosestContainingText,
} from '../common/xPath';
import {
  createProject,
  deleteProject,
  login,
  setTranslations,
} from '../common/apiCalls';
import { HOST } from '../common/constants';
import { ProjectDTO } from '../../../webapp/src/service/response.types';
import {
  deleteLanguage,
  visitLanguageSettings,
  visitProjectSettings,
} from '../common/languages';
import {
  getCellCancelButton,
  getCellEditButton,
  getCellSaveButton,
} from '../common/translations';

describe('Translations', () => {
  let project: ProjectDTO = null;

  beforeEach(() => {
    cy.wrap(null).then(() =>
      login().then(() => {
        cy.wrap(null).then(() =>
          createProject({
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
            project = r.body as ProjectDTO;
            window.localStorage.setItem(
              'selectedLanguages',
              `{"${project.id}":["en"]}`
            );
            visit();
          })
        );
      })
    );
  });

  afterEach(() => {
    cy.wrap(null).then(() => deleteProject(project.id));
  });

  it("won't fail when language deleted", () => {
    createTranslation('Test key', 'Translated test key', { isFirst: true });
    cy.contains('Translation created').should('be.visible');
    toggleLang('Česky');
    visitProjectSettings(project.id);
    visitLanguageSettings('Česky');
    deleteLanguage();
    visit();
    // wait for loading to appear and disappear again
    cy.gcy('global-base-view-loading').should('be.visible');
    cy.gcy('global-base-view-loading').should('not.exist');
    cy.contains('Translated test key').should('be.visible');
  });

  it('will create translation', () => {
    createTranslation('Test key', 'Translated test key', { isFirst: true });
    cy.contains('Translation created').should('be.visible');
    cy.xpath(getAnyContainingText('Key', 'a'))
      .xpath(getClosestContainingText('Test key'))
      .scrollIntoView()
      .should('be.visible');
    cy.xpath(getAnyContainingText('Key', 'a'))
      .xpath(getClosestContainingText('Translated test key'))
      .should('be.visible');
    createTranslation('Test key 2', 'Translated test key 2', {
      isFirst: false,
    });
    cy.xpath(getAnyContainingText('Key', 'a'))
      .xpath(getClosestContainingText('Test key 2'))
      .scrollIntoView()
      .should('be.visible');
    cy.xpath(getAnyContainingText('Key', 'a'))
      .xpath(getClosestContainingText('Translated test key 2'))
      .should('be.visible');
  });

  describe('with 5 translations', () => {
    beforeEach(() => {
      const promises = [];
      for (let i = 1; i < 5; i++) {
        promises.push(
          setTranslations(
            project.id,
            `Cool key ${i.toString().padStart(2, '0')}`,
            {
              en: `Cool translated text ${i}`,
              cs: `Studený přeložený text ${i}`,
            }
          )
        );
      }
      cy.wrap(null).then(() =>
        Cypress.Promise.all(promises).then(() => {
          visit();
        })
      );

      // wait for loading to appear and disappear again
      cy.gcy('global-base-view-loading').should('be.visible');
      cy.gcy('global-base-view-loading').should('not.exist');
    });

    it('will edit key', () => {
      editCell('Cool key 01', 'Cool key edited');

      cy.contains('Cool key edited').should('be.visible');
      cy.contains('Cool key 02').should('be.visible');
      cy.contains('Cool key 04').should('be.visible');
    });

    it('will edit translation', () => {
      editCell('Cool translated text 1', 'Super cool changed text...');
      cy.xpath(
        `${getAnyContainingText(
          'Super cool changed text...'
        )}/parent::*//button[@type='submit']`
      ).should('not.exist');
      cy.contains('Super cool changed text...').should('be.visible');
      cy.contains('Cool translated text 2').should('be.visible');
    });

    it('will cancel key edit', () => {
      editCell('Cool key 01', 'Cool key edited', false);
      getCellCancelButton().click();

      cy.contains('Discard changes?').should('be.visible');
      clickDiscardChanges();
      cy.contains('Cool key edited').should('not.exist');
      cy.contains('Cool key 01').should('be.visible');
    });

    it('will ask for confirmation on changed edit', () => {
      editCell('Cool key 01', 'Cool key edited', false);
      cy.contains('Cool key 04')
        .xpath(
          "./ancestor::*[@data-cy='translations-table-cell']//*[@data-cy='translations-cell-edit-button']"
        )
        .invoke('show')
        .click();
      cy.contains(`Discard changes?`).should('be.visible');
      clickDiscardChanges();
      cy.contains('Cool key 04')
        .xpath(
          "./ancestor::*[@data-cy='translations-table-cell']//*[@data-cy='translations-cell-save-button']"
        )
        .should('be.visible');
    });

    describe('Options', () => {
      it('will select language', () => {
        cy.contains('Studený přeložený text 1').should('be.visible');
        toggleLang('Česky');
        cy.contains('Studený přeložený text 1').should('not.exist');
        toggleLang('English');
        cy.contains('Select at least one language').should('be.visible');
      });

      it('will search', () => {
        cy.gcy('global-search-field').type('Cool key 04');
        cy.contains('Cool key 01').should('not.exist');
        cy.contains('Cool key 04').should('be.visible');
      });
    });
  });

  const visit = () => {
    cy.visit(`${HOST}/projects/${project.id}/translations`);
  };
});

const editCell = (oldValue: string, newValue: string, save = true) => {
  getCellEditButton(oldValue).click();

  // wait for editor to appear
  cy.gcy('global-editor').should('be.visible');
  cy.contains(oldValue).should('be.visible');
  // select all, delete and type new text
  cy.focused().type('{selectall}').type('{backspace}').type(newValue);

  if (save) {
    getCellSaveButton().click();
  }
};

function createTranslation(
  testKey: string,
  testTranslated: string,
  options: { isFirst?: boolean }
) {
  if (options?.isFirst) {
    clickAdd();
  } else {
    cy.xpath(getAnyContainingText('Add', 'span')).click();
  }
  cy.xpath("//textarea[@name='key']").type(testKey);
  cy.xpath("//textarea[@name='translations.en']").type(testTranslated);
  cy.xpath(getAnyContainingText('save')).click();
}

function clickDiscardChanges() {
  cy.xpath(getAnyContainingText('Discard changes', 'button')).click();
}

const toggleLang = (lang) => {
  cy.get('#languages-select-translations').click();
  cy.get('#language-select-translations-menu')
    .contains(lang)
    .should('be.visible')
    .click();
  cy.get('body').click();
  // wait for loading to disappear
  cy.gcy('global-base-view-loading').should('not.exist');
};
