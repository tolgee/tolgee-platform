import { visitTranslations } from '../../common/translations';
import { waitForGlobalLoading } from '../../common/loading';
import { suggestionsTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { assertFilter } from '../../common/filters';

describe('Translation filters suggestions', () => {
  let projectId: number;

  before(() => {
    suggestionsTestData.clean();
    suggestionsTestData
      .generate({ suggestionsMode: 'ENABLED' })
      .then((data) => {
        projectId = data.body.projects[0].id;
      });
  });

  beforeEach(() => {
    login('organization.owner@test.com');
    visit();
    waitForGlobalLoading();
  });

  after(() => {
    suggestionsTestData.clean();
  });

  it('filters with suggestions', () => {
    assertFilter({
      submenu: 'Suggestions',
      filterOption: ['With suggestions'],
      toSeeAfter: ['key 0', 'pluralKey'],
      checkAfter() {
        cy.gcy('translations-filter-select').contains('With suggestions');
      },
    });
  });

  it('filters without suggestions', () => {
    assertFilter({
      submenu: 'Suggestions',
      filterOption: ['No suggestions'],
      toSeeAfter: ['key 1', 'key 2', 'key 3'],
      checkAfter() {
        cy.gcy('translations-filter-select').contains('No suggestions');
      },
    });
  });

  const visit = () => {
    visitTranslations(projectId);
  };
});
