import {
  getAnyContainingText,
  getClosestContainingText,
} from '../../common/xPath';
import { deleteProject } from '../../common/apiCalls';
import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import {
  deleteLanguage,
  visitLanguageSettings,
  visitProjectSettings,
} from '../../common/languages';
import {
  createTranslation,
  toggleLang,
  translationsBeforeEach,
  visitTranslations,
} from '../../common/translations';

describe('Translations Base', () => {
  let project: ProjectDTO = null;

  beforeEach(() => {
    translationsBeforeEach().then((p) => (project = p));
  });

  afterEach(() => {
    deleteProject(project.id);
  });

  it("won't fail when language deleted", () => {
    cy.gcy('global-base-view-loading').should('be.visible');
    cy.gcy('global-base-view-loading').should('not.exist');

    createTranslation('Test key', 'Translated test key');
    toggleLang('Česky');
    visitProjectSettings(project.id);
    visitLanguageSettings('Česky');
    deleteLanguage();
    visit();
    // wait for loading to appear and disappear again
    cy.gcy('global-base-view-content').should('be.visible');
    cy.gcy('global-base-view-loading').should('not.exist');
    cy.contains('Translated test key').should('be.visible');
  });

  it('will create translation', () => {
    cy.gcy('global-empty-list').should('be.visible');
    createTranslation('Test key', 'Translated test key');
    cy.contains('Key created').should('be.visible');
    cy.wait(100);
    cy.xpath(getAnyContainingText('Key', 'a'))
      .xpath(getClosestContainingText('Test key'))
      .scrollIntoView()
      .should('be.visible');
    cy.xpath(getAnyContainingText('Key', 'a'))
      .xpath(getClosestContainingText('Translated test key'))
      .should('be.visible');
    createTranslation('Test key 2', 'Translated test key 2');
    cy.xpath(getAnyContainingText('Key', 'a'))
      .xpath(getClosestContainingText('Test key 2'))
      .scrollIntoView()
      .should('be.visible');
    cy.xpath(getAnyContainingText('Key', 'a'))
      .xpath(getClosestContainingText('Translated test key 2'))
      .should('be.visible');
  });

  const visit = () => {
    visitTranslations(project.id);
  };
});
