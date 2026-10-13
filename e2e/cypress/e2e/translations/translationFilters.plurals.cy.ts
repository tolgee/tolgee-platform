import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import { visitTranslations } from '../../common/translations';
import { waitForGlobalLoading } from '../../common/loading';
import { translationsTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { assertFilter } from '../../common/filters';
import { gcyAdvanced } from '../../common/shared';

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
    });
  });

  it('filters non-plural keys', () => {
    assertFilter({
      submenu: 'Plurals',
      filterOption: ['Non-plural keys'],
      toSeeAfter: ['A key', 'Z key'],
    });
  });

  it('plural and non-plural keys are mutually exclusive', () => {
    assertFilter({
      submenu: 'Plurals',
      filterOption: ['Plural keys'],
      and() {
        gcyAdvanced({
          value: 'filter-item',
          filter: 'filterIsNotPlural',
        }).click();
      },
      toSeeAfter: ['A key', 'Z key'],
    });
  });

  const visit = () => {
    visitTranslations(project.id);
  };
});
