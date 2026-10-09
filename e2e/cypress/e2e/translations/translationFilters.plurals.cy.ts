import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import { visitTranslations } from '../../common/translations';
import { waitForGlobalLoading } from '../../common/loading';
import { translationsTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { assertFilter } from '../../common/filters';

describe('Translation filters - plurals', () => {
  let project: ProjectDTO = null;

  before(() => {
    translationsTestData.cleanupForFilters();
    translationsTestData
      .generateForPluralFilters()
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

  it('filters plural keys', () => {
    assertFilter({
      submenu: 'Plurals',
      filterOption: ['Plural keys'],
      toSeeAfter: ['plural-items', 'plural-days'],
      checkAfter() {
        cy.gcy('translations-filter-select').contains('Plural keys');
      },
    });
  });

  it('filters non-plural keys', () => {
    assertFilter({
      submenu: 'Plurals',
      filterOption: ['Non-plural keys'],
      toSeeAfter: ['A key', 'Z key'],
      checkAfter() {
        cy.gcy('translations-filter-select').contains('Non-plural keys');
      },
    });
  });

  it('plural and non-plural keys are mutually exclusive', () => {
    assertFilter({
      submenu: 'Plurals',
      filterOption: ['Plural keys'],
      and() {
        cy.gcy('filter-item').contains('Non-plural keys').click();
      },
      toSeeAfter: ['A key', 'Z key'],
      checkAfter() {
        cy.gcy('translations-filter-select').contains('Non-plural keys');
      },
    });
  });

  const visit = () => {
    visitTranslations(project.id);
  };
});
