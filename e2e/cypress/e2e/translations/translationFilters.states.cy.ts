import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import { visitTranslations } from '../../common/translations';
import { waitForGlobalLoading } from '../../common/loading';
import { translationsTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
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

  it('filters untranslated', () => {
    assertFilter({
      submenu: 'Translations',
      filterOption: ['Untranslated'],
      toSeeAfter: [
        'key with screenshot',
        'key with screenshot 2',
        'state test key 3',
        'state test key 4',
        'state test key 5',
        'Z key',
      ],
      checkAfter() {
        cy.gcy('translations-filter-select').contains('Untranslated');
      },
    });
  });

  it('filters translated', () => {
    assertFilter({
      submenu: 'Translations',
      filterOption: ['Translated'],
      toSeeAfter: ['commented_key', 'state test key 2'],
    });
  });

  it('filters reviewed', () => {
    assertFilter({
      submenu: 'Translations',
      filterOption: ['Reviewed'],
      toSeeAfter: ['A key', 'state test key'],
    });
  });

  it('filters translated automatically', () => {
    assertFilter({
      submenu: 'Translations',
      filterOption: ['Translated automatically'],
      toSeeAfter: ['A key'],
    });
  });

  it('filters outdated', () => {
    assertFilter({
      submenu: 'Translations',
      filterOption: ['Outdated'],
      toSeeAfter: ['A key'],
    });
  });

  it('filters disabled', () => {
    assertFilter({
      submenu: 'Translations',
      filterOption: ['Disabled'],
      toSeeAfter: ['state test key 6'],
    });
  });

  it('filters Translated OR Translated automatically', () => {
    assertFilter({
      submenu: 'Translations',
      filterOption: ['Translated', 'Translated automatically'],
      toSeeAfter: ['commented_key', 'state test key 2', 'A key'],
      checkAfter() {
        cy.gcy('translations-filter-select').contains('2 filters');
      },
    });
  });

  it('filters translated, including base', () => {
    assertFilter({
      submenu: 'Translations',
      filterOption: ['Translated'],
      and() {
        cy.gcy('translations-filter-apply-for-expand').click();
        cy.gcy('translations-filter-apply-for-all').click();
      },
      toSeeAfter: [
        'commented_key',
        'state test key 2',
        'state test key 4',
        'state test key 5',
        'Z key',
      ],
    });
  });

  it('filters only for base', () => {
    assertFilter({
      submenu: 'Translations',
      filterOption: ['Translated'],
      and() {
        cy.gcy('translations-filter-apply-for-expand').click();
        cy.gcy('translations-filter-apply-for-language')
          .contains('English')
          .click();
      },
      toSeeAfter: ['state test key 4', 'state test key 5', 'Z key'],
    });
  });

  const visit = () => {
    visitTranslations(project.id);
  };
});
