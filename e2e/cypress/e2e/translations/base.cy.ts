import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import { deleteLanguage, visitLanguageSettings } from '../../common/languages';
import {
  createTranslation,
  getTranslationCell,
  toggleLang,
  translationsBeforeEach,
  visitTranslations,
} from '../../common/translations';
import { waitForGlobalLoading } from '../../common/loading';
import { deleteProject, enableNamespaces } from '../../common/apiCalls/common';
import {
  getAnyContainingText,
  getClosestContainingText,
} from '../../common/xPath';
import { visitProjectLanguages } from '../../common/shared';
import { E2TranslationsView } from '../../compounds/E2TranslationsView';

describe('Translations Base', () => {
  let project: ProjectDTO = null;

  beforeEach(() => {
    translationsBeforeEach().then((p) => (project = p));
  });

  afterEach(() => {
    deleteProject(project.id);
  });

  it("won't fail when language deleted", () => {
    waitForGlobalLoading();

    createTranslation({ key: 'Test key', translation: 'Translated test key' });
    toggleLang('Česky');
    visitProjectLanguages(project.id);
    visitLanguageSettings('cs');
    deleteLanguage();
    visit();
    // wait for loading to appear and disappear again
    cy.gcy('global-base-view-content').should('be.visible');
    waitForGlobalLoading();
    cy.contains('Translated test key').should('be.visible');
  });

  it(
    'will create translation without namespace',
    {
      retries: { openMode: 0, runMode: 10 },
    },
    () => {
      cy.wait(100);
      cy.gcy('global-empty-state').should('be.visible');

      const translationsView = new E2TranslationsView();
      const keyCreateDialog = translationsView.openKeyCreateDialog();
      keyCreateDialog.getNamespaceSelectElement().should('not.exist');
      keyCreateDialog.fillAndSave({
        key: 'Test key',
        translation: 'Translated test key',
      });

      cy.contains('Key created').should('be.visible');
      cy.wait(100);
      cy.xpath(getAnyContainingText('Key', 'a'))
        .xpath(getClosestContainingText('Test key'))
        .scrollIntoView()
        .should('be.visible');
      cy.xpath(getAnyContainingText('Key', 'a'))
        .xpath(getClosestContainingText('Translated test key'))
        .should('be.visible');
      createTranslation({
        key: 'Test key 2',
        translation: 'Translated test key 2',
      });
      cy.contains('Key created').should('be.visible');
      cy.wait(100);
      cy.xpath(getAnyContainingText('Key', 'a'))
        .xpath(getClosestContainingText('Test key 2'))
        .scrollIntoView()
        .should('be.visible');
      cy.xpath(getAnyContainingText('Key', 'a'))
        .xpath(getClosestContainingText('Translated test key 2'))
        .should('be.visible');
    }
  );

  it('will create translation with plural', () => {
    cy.gcy('global-empty-state').should('be.visible');
    createTranslation({
      key: 'test-key',
      plural: {
        formValues: { one: '# key', other: '# keys' },
      },
    });
    getTranslationCell('test-key', 'en')
      .findDcy('translation-plural-parameter')
      .contains('value')
      .should('be.visible');
    getTranslationCell('test-key', 'en')
      .findDcy('translation-plural-variant')
      .contains('#1 key')
      .should('be.visible');
  });

  it('will create translation with plural and custom variable name', () => {
    cy.gcy('global-empty-state').should('be.visible');
    createTranslation({
      key: 'test-key',
      plural: {
        variableName: 'testVariable',
        formValues: { one: '# key', other: '# keys' },
      },
    });
    getTranslationCell('test-key', 'en')
      .findDcy('translation-plural-parameter')
      .contains('testVariable')
      .should('be.visible');
    getTranslationCell('test-key', 'en')
      .findDcy('translation-plural-variant')
      .contains('#1 key')
      .should('be.visible');
  });

  it('will create translation with namespace', () => {
    enableNamespaces(project.id);
    cy.wait(100);
    cy.gcy('global-empty-state').should('be.visible');

    const translationsView = new E2TranslationsView();
    const keyCreateDialog = translationsView.openKeyCreateDialog();
    keyCreateDialog.getNamespaceSelectElement().should('exist');
    keyCreateDialog.fillAndSave({
      key: 'Test key',
      translation: 'Translated test key',
      namespace: 'test-ns',
    });

    cy.gcy('translations-namespace-banner')
      .contains('test-ns')
      .should('be.visible');
  });

  it('will create key with description', () => {
    cy.wait(100);
    cy.gcy('global-empty-state').should('be.visible');
    createTranslation({
      key: 'Test key',
      translation: 'Translated test key',
      description: 'Description of test key',
    });

    cy.gcy('translations-key-cell-description')
      .contains('Description of test key')
      .should('be.visible');
  });

  const visit = () => {
    visitTranslations(project.id);
  };
});
