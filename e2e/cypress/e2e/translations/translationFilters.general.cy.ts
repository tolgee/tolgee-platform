import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import { toggleLang, visitTranslations } from '../../common/translations';
import { assertMessage, assertMultiselect } from '../../common/shared';
import { waitForGlobalLoading } from '../../common/loading';
import { translationsTestData } from '../../common/apiCalls/testData/testData';
import {
  login,
  setTranslationsViewLanguagesLimit,
} from '../../common/apiCalls/common';
import { assertFilter } from '../../common/filters';

describe('Translations Base', () => {
  let project: ProjectDTO = null;

  before(() => {
    translationsTestData.cleanupForFilters();
    translationsTestData
      .generateForFilters()
      .then((p) => {
        project = p;
      })
      .then(() => {
        login('franta', 'admin');
        visit();
      });
  });

  beforeEach(() => {
    login('franta', 'admin');
    visit();
    waitForGlobalLoading();
  });

  after(() => {
    translationsTestData.cleanupForFilters();
  });

  it("namespaces section doesn't exist (when not enabled in project)", () => {
    cy.gcy('translations-filter-select').click();
    cy.waitForDom();
    cy.gcy('submenu-item').contains('Namespaces').should('not.exist');
    cy.focused().type('{Esc}');
  });

  it('allows to select max languages', () => {
    toggleLang('German');
    setTranslationsViewLanguagesLimit(1);
    visit();
    toggleLang('German');
    assertMessage('Cannot select more than 1 languages');
    setTranslationsViewLanguagesLimit(3);
    visit();
    toggleLang('German');
    assertMultiselect(cy.gcy('translations-language-select-form-control'), [
      'German',
      'English',
    ]);
  });

  it('filters unresolved comments', () => {
    assertFilter({
      submenu: 'Comments',
      filterOption: ['Unresolved comments'],
      toSeeAfter: ['commented_key'],
      checkAfter() {
        cy.gcy('translations-filter-select').contains('Unresolved comments');
      },
    });
  });

  it('filters all comments', () => {
    assertFilter({
      submenu: 'Comments',
      filterOption: ['Any comments'],
      toSeeAfter: ['A key', 'commented_key'],
      checkAfter() {
        cy.gcy('translations-filter-select').contains('Any comments');
      },
    });
  });

  it('combines multiple filters', () => {
    assertFilter({
      submenu: 'Tags',
      filterOption: ['Without tags'],
      and() {
        cy.focused().type('{Esc}');
        cy.gcy('submenu-item').contains('Translations').click();
        cy.gcy('filter-item').contains('Reviewed').click();
      },
      toSeeAfter: ['state test key'],
      checkAfter() {
        cy.gcy('translations-filter-select').contains('2 filters');
      },
    });
  });

  const visit = () => {
    visitTranslations(project.id);
  };
});
