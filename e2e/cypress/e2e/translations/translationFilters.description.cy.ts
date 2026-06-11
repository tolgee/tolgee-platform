import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import { visitTranslations } from '../../common/translations';
import { waitForGlobalLoading } from '../../common/loading';
import { translationsTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { assertFilter } from '../../common/filters';

describe('Translation filters - description', () => {
  let project: ProjectDTO = null;

  before(() => {
    translationsTestData.cleanupForFilters();
    translationsTestData
      .generateForDescriptionFilters()
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

  it('filters keys with a description', () => {
    assertFilter({
      submenu: 'Description',
      filterOption: ['With description'],
      toSeeAfter: ['A key', 'desc-real', 'desc-ws'],
      checkAfter() {
        cy.gcy('translations-filter-select').contains('With description');
      },
    });
  });

  it('filters keys without a description', () => {
    assertFilter({
      submenu: 'Description',
      filterOption: ['No description'],
      toSeeAfter: ['Z key', 'desc-null', 'desc-empty'],
      checkAfter() {
        cy.gcy('translations-filter-select').contains('No description');
      },
    });
  });

  it('with/without description are mutually exclusive', () => {
    assertFilter({
      submenu: 'Description',
      filterOption: ['With description'],
      and() {
        cy.gcy('filter-item').contains('No description').click();
      },
      toSeeAfter: ['Z key', 'desc-null', 'desc-empty'],
      checkAfter() {
        cy.gcy('translations-filter-select').contains('No description');
      },
    });
  });

  const visit = () => {
    visitTranslations(project.id);
  };
});
