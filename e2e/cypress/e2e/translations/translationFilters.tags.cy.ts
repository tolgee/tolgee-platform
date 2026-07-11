import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import { visitTranslations } from '../../common/translations';
import { waitForGlobalLoading } from '../../common/loading';
import { translationsTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { assertFilter } from '../../common/filters';

const STATE_KEYS = [
  'state test key',
  'state test key 2',
  'state test key 3',
  'state test key 4',
  'state test key 5',
  'state test key 6',
];

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

  it('filters tagged by "Cool tag"', () => {
    assertFilter({
      submenu: 'Tags',
      filterOption: ['Cool tag'],
      toSeeAfter: ['A key'],
      checkAfter() {
        cy.gcy('translations-filter-select')
          .findDcy('translations-tag')
          .contains('Cool tag');
      },
    });
  });

  it('filters tagged by "Cool tag" OR "Lame tag"', () => {
    assertFilter({
      submenu: 'Tags',
      filterOption: ['Cool tag', 'Lame tag'],
      toSeeAfter: ['A key', 'Z key'],
      checkAfter() {
        cy.gcy('translations-filter-select').contains('2 filters');
      },
    });
  });

  it('filters keys without tag', () => {
    assertFilter({
      submenu: 'Tags',
      filterOption: ['Without tag'],
      toSeeAfter: [
        ...STATE_KEYS,
        'commented_key',
        'key with screenshot',
        'key with screenshot 2',
      ],
      checkAfter() {
        cy.gcy('translations-filter-select').contains('Without tag');
      },
    });
  });

  it('excludes keys without tag', () => {
    assertFilter({
      submenu: 'Tags',
      excludeOption: ['Without tag'],
      toSeeAfter: ['A key', 'Z key'],
    });
  });

  it('excludes keys with "Cool tag" and "Lame tag"', () => {
    assertFilter({
      submenu: 'Tags',
      excludeOption: ['Cool tag', 'Lame tag'],
      toSeeAfter: [
        ...STATE_KEYS,
        'commented_key',
        'key with screenshot',
        'key with screenshot 2',
      ],
    });
  });

  it('filters after click on tag', () => {
    cy.gcy('translations-table-cell')
      .findDcy('translations-tag')
      .contains('Cool tag')
      .click();
    cy.gcy('translations-key-name').should('have.length', 1);
    cy.contains('A key').should('be.visible');
    cy.gcy('translations-table-cell')
      .findDcy('translations-tag')
      .contains('Cool tag')
      .click();
    cy.contains('key with screenshot').should('be.visible');
  });

  const visit = () => {
    visitTranslations(project.id);
  };
});
