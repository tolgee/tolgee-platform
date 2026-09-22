import { visitTranslations } from '../../common/translations';
import { waitForGlobalLoading } from '../../common/loading';
import { tasks } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { assertFilter } from '../../common/filters';
import { E2TranslationsView } from '../../compounds/E2TranslationsView';
import { dismissMenu } from '../../common/shared';
import { setFeature } from '../../common/features';

describe('Translation filters tasks', () => {
  const translationsView = new E2TranslationsView();

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

  afterEach(() => {
    setFeature('TASKS', true);
    setFeature('ORDER_TRANSLATION', false);
  });

  after(() => {
    tasks.clean({ failOnStatusCode: false });
  });

  it('filters keys never in a task', () => {
    assertFilter({
      submenu: 'Tasks',
      and: () => translationsView.getTaskStatusFilter('NEVER_IN_TASK').click(),
      toSeeAfter: ['key 2', 'key 3'],
      checkAfter() {
        cy.gcy('translations-filter-select-clear').should('exist');
      },
    });
  });

  it('filters keys not in any open task', () => {
    assertFilter({
      submenu: 'Tasks',
      and: () =>
        translationsView.getTaskStatusFilter('NOT_IN_OPEN_TASK').click(),
      toSeeAfter: ['key 2', 'key 3'],
      checkAfter() {
        cy.gcy('translations-filter-select-clear').should('exist');
      },
    });
  });

  it('keeps the task status options exclusive', () => {
    assertFilter({
      submenu: 'Tasks',
      and: () => {
        translationsView.getTaskStatusFilter('NEVER_IN_TASK').click();
        translationsView.getTaskStatusFilter('HAS_BEEN_IN_TASK').click();
        translationsView
          .assertTaskStatusFilterChecked('NEVER_IN_TASK', false)
          .assertTaskStatusFilterChecked('NOT_IN_OPEN_TASK', false)
          .assertTaskStatusFilterChecked('HAS_BEEN_IN_TASK');
      },
      toSeeAfter: ['key 0', 'key 1'],
    });
  });

  it('narrows the open task filter by task type', () => {
    assertFilter({
      submenu: 'Tasks',
      and: () => {
        translationsView.getTaskStatusFilter('NOT_IN_OPEN_TASK').click();
        translationsView.getTaskTypeFilter('REVIEW').click();
      },
      toSeeAfter: ['key 0', 'key 1', 'key 2', 'key 3'],
    });

    assertFilter({
      submenu: 'Tasks',
      and: () => {
        translationsView.getTaskStatusFilter('NOT_IN_OPEN_TASK').click();
        translationsView.getTaskTypeFilter('TRANSLATE').click();
      },
      toSeeAfter: ['key 2', 'key 3'],
    });
  });

  it('offers the task types only as a scope on a chosen status', () => {
    assertFilter({
      submenu: 'Tasks',
      and: () => {
        translationsView
          .getTaskTypeFilter('TRANSLATE')
          .should('have.attr', 'aria-disabled', 'true');
        translationsView.getTaskStatusFilter('NOT_IN_OPEN_TASK').click();
        translationsView
          .getTaskTypeFilter('TRANSLATE')
          .should('not.have.attr', 'aria-disabled', 'true');
      },
      toSeeAfter: ['key 2', 'key 3'],
    });
  });

  it('offers the filter to order-translation projects without the tasks feature', () => {
    setFeature('TASKS', false);
    setFeature('ORDER_TRANSLATION', true);
    cy.reload();
    waitForGlobalLoading();

    cy.gcy('translations-filter-select').click();
    cy.waitForDom();
    cy.gcy('submenu-item').contains('Tasks').should('exist');
  });

  it('clicking a selected status again clears it and its type scope', () => {
    cy.gcy('translations-filter-select').click();
    cy.waitForDom();
    cy.gcy('submenu-item').contains('Tasks').click();
    cy.waitForDom();

    translationsView.getTaskStatusFilter('NOT_IN_OPEN_TASK').click();
    translationsView.getTaskTypeFilter('TRANSLATE').click();
    translationsView.getTaskStatusFilter('NOT_IN_OPEN_TASK').click();

    translationsView
      .assertTaskStatusFilterChecked('NOT_IN_OPEN_TASK', false)
      .assertTaskStatusFilterChecked('HAS_BEEN_IN_TASK', false)
      .assertTaskStatusFilterChecked('NEVER_IN_TASK', false)
      .assertTaskTypeFilterChecked('TRANSLATE')
      .assertTaskTypeFilterChecked('REVIEW');

    dismissMenu();
    dismissMenu();
    waitForGlobalLoading();

    cy.gcy('translations-filter-select-clear').should('not.exist');
    ['key 0', 'key 1', 'key 2', 'key 3'].forEach((i) =>
      cy.contains(i).should('be.visible')
    );
  });

  it('filters keys which have been in a task', () => {
    assertFilter({
      submenu: 'Tasks',
      and: () =>
        translationsView.getTaskStatusFilter('HAS_BEEN_IN_TASK').click(),
      toSeeAfter: ['key 0', 'key 1'],
    });
  });
});
