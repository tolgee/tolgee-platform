import {
  cleanTranslationFiltersData,
  createTranslationFiltersData,
  login,
} from '../../common/apiCalls';
import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import { visitTranslations } from '../../common/translations';
import { gcy, selectInSelect } from '../../common/shared';

describe('Translations Base', () => {
  let project: ProjectDTO = null;

  before(() => {
    cleanTranslationFiltersData();
    createTranslationFiltersData().then((p) => {
      project = p;
    });
  });

  beforeEach(() => {
    login('franta', 'admin');
    visit();
    cy.contains('Translations').should('be.visible');
    cy.gcy('global-base-view-loading').should('be.visible');
    cy.gcy('global-base-view-loading').should('not.exist');
  });

  after(() => {
    cleanTranslationFiltersData();
  });

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
    {
      filterOption: 'Is translated in English',
      toMissAfter: ['A key', 'key with screenshot 2'],
      toSeeAfter: ['Z key'],
      only: false,
    },

    {
      filterOption: 'Not translated in English',
      toMissAfter: ['Z key'],
      toSeeAfter: ['A key', 'key with screenshot 2'],
      only: false,
    },
  ].forEach((test) => {
    // eslint-disable-next-line no-only-tests/no-only-tests
    const fn = test.only ? it.only : it;
    fn(`filters '${test.filterOption}'`, () =>
      assertFilter(test.filterOption, test.toMissAfter, test.toSeeAfter)
    );
  });

  describe('filter exclusiveness', () => {
    beforeEach(() => {
      gcy('translations-filter-select').click();
    });
    [
      ['Not translated in English', 'Is translated in English'],
      ['No screenshots', 'With screenshots'],
      ['At least one translated', 'Missing translation'],
    ].forEach((pair) => {
      it(`Filters ${pair[0]} and ${pair[1]} are exclusive`, () => {
        cy.contains(pair[0]).click();
        cy.contains(pair[1]).click();

        gcy('translations-filter-option')
          .contains(pair[0])
          .closest('li')
          .find('input')
          .should('not.checked');
      });
    });
  });

  const assertFilter = (
    filterOption: string,
    toMissAfter: string[],
    toSeeAfter: string[]
  ) => {
    toMissAfter.forEach((i) => cy.contains(i).should('be.visible'));
    selectInSelect(gcy('translations-filter-select'), filterOption);
    cy.get('body').click(0, 0);
    toSeeAfter.forEach((i) => cy.contains(i).should('be.visible'));
    toMissAfter.forEach((i) => cy.contains(i).should('not.exist'));
    selectInSelect(gcy('translations-filter-select'), filterOption);
    cy.gcy('global-base-view-loading').should('not.exist');
    cy.get('body').click(0, 0);
    cy.wait(10);
  };

  const visit = () => {
    visitTranslations(project.id);
  };
});
