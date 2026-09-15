import { visitTranslations } from '../../common/translations';
import { waitForGlobalLoading } from '../../common/loading';
import { tasks } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { assertFilter } from '../../common/filters';

describe('Translation filters tasks', () => {
  beforeEach(() => {
    tasks.clean({ failOnStatusCode: false });
    tasks
      .generateStandard()
      .then((r) => r.body)
      .then(({ users, projects }) => {
        login(users.find((u) => u.name === 'Tasks test user')?.username);
        const testProject = projects.find(
          ({ name }) => name === 'Project with tasks'
        );
        visitTranslations(testProject.id);
      });
    waitForGlobalLoading();
  });

  after(() => {
    tasks.clean({ failOnStatusCode: false });
  });

  it('filters keys never in a task', () => {
    assertFilter({
      submenu: 'Tasks',
      and: () => cy.gcy('translations-filter-never-in-task').click(),
      toSeeAfter: ['key 2', 'key 3'],
      checkAfter() {
        cy.gcy('translations-filter-select').contains('Never in a task');
      },
    });
  });

  it('filters keys which have been in a task', () => {
    assertFilter({
      submenu: 'Tasks',
      and: () => cy.gcy('translations-filter-has-been-in-task').click(),
      toSeeAfter: ['key 0', 'key 1'],
    });
  });
});
