import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import { visitTranslations } from '../../common/translations';
import { gcy, getPopover, selectInSelect } from '../../common/shared';
import { waitForGlobalLoading } from '../../common/loading';
import { translationsTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';

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
        cy.contains('Translations').should('be.visible');
        waitForGlobalLoading();
      });
  });

  beforeEach(() => {
    login('franta', 'admin');
    visit();
    cy.contains('Translations').should('be.visible');
    waitForGlobalLoading();
  });

  after(() => {
    translationsTestData.cleanupForFilters();
  });

  it(`filters work correctly`, () => {
    [
      {
        filterOption: 'At least one translated',
        toMissAfter: ['key with screenshot'],
        toSeeAfter: ['A key'],
        only: false,
      },
      {
        filterOption: 'Missing translation',
        toMissAfter: [],
        toSeeAfter: ['A key', 'key with screenshot'],
        only: false,
      },
      {
        filterOption: 'With screenshots',
        toMissAfter: ['A key'],
        toSeeAfter: ['key with screenshot', 'key with screenshot 2'],
        only: false,
      },
      {
        filterOption: 'No screenshots',
        toMissAfter: ['key with screenshot', 'key with screenshot 2'],
        toSeeAfter: ['A key'],
        only: false,
      },
    ].forEach((test) => {
      assertFilter(test.filterOption, test.toMissAfter, test.toSeeAfter);
    });
  });

  it('filter exclusiveness', () => {
    gcy('translations-filter-select').click();
    [
      ['No screenshots', 'With screenshots'],
      ['At least one translated', 'Missing translation'],
    ].forEach((pair) => {
      cy.contains(pair[0]).click();
      cy.contains(pair[1]).click();

      gcy('translations-filter-option')
        .contains(pair[0])
        .closest('li')
        .find('input')
        .should('not.checked');
    });
  });

  it('filters by state', () => {
    [
      {
        state: ['Untranslated'],
        toMissAfter: ['state test key 2'],
        toSeeAfter: ['key with screenshot'],
      },
      {
        state: ['Translated'],
        toMissAfter: ['key with screenshot'],
        toSeeAfter: ['Z key'],
      },
      {
        state: ['Reviewed'],
        toMissAfter: ['state test key 4'],
        toSeeAfter: ['state test key 2'],
      },
      {
        state: ['Translated', 'Reviewed'],
        toMissAfter: ['key with screenshot'],
        toSeeAfter: ['state test key 2', 'Z key'],
      },
    ].forEach((test) => {
      assertStateFilter(test.state, test.toMissAfter, test.toSeeAfter);
    });
  });

  it('filters by tags', () => {
    selectInSelect(gcy('translations-filter-select'), 'Tags');
    getPopover().contains('Cool tag').click();
    getPopover().contains('Lame tag').click();
    cy.focused().type('{Esc}');
    cy.focused().type('{Esc}');
    cy.contains('Z translation').should('be.visible');
    cy.contains('A translation').should('be.visible');
    cy.contains('key with screenshot').should('not.exist');
  });

  it('filters after click on tag', () => {
    cy.gcy('translations-tag').contains('Cool tag').click();
    cy.contains('Z translation').should('be.visible');
    cy.contains('key with screenshot').should('not.exist');
    cy.contains('A translation').should('not.exist');
    cy.gcy('translations-tag').contains('Cool tag').click();
    cy.contains('key with screenshot').should('be.visible');
  });

  const assertStateFilter = (
    states: string[],
    toMissAfter: string[],
    toSeeAfter: string[]
  ) => {
    toMissAfter.forEach((i) => cy.contains(i).should('exist'));
    selectInSelect(gcy('translations-filter-select'), 'English');
    states.forEach((state) => {
      getPopover().contains(state).click();
    });
    cy.focused().type('{Esc}');
    cy.focused().type('{Esc}');
    toSeeAfter.forEach((i) => cy.contains(i).should('exist'));
    toMissAfter.forEach((i) => cy.contains(i).should('not.exist'));
    gcy('translations-filter-clear-all').click();
  };

  const assertFilter = (
    filterOption: string,
    toMissAfter: string[],
    toSeeAfter: string[]
  ) => {
    toMissAfter.forEach((i) => cy.contains(i).should('be.visible'));
    selectInSelect(gcy('translations-filter-select'), filterOption);
    cy.focused().type('{Esc}');
    toSeeAfter.forEach((i) => cy.contains(i).should('be.visible'));
    toMissAfter.forEach((i) => cy.contains(i).should('not.exist'));
    selectInSelect(gcy('translations-filter-select'), filterOption);
    cy.focused().type('{Esc}');
  };

  const visit = () => {
    visitTranslations(project.id);
  };
});
