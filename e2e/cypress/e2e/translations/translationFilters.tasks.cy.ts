import { visitTranslations } from '../../common/translations';
import { waitForGlobalLoading } from '../../common/loading';
import { tasks } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
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

  /**
   * The task filter nests three menus deep, one more than assertFilter's fixed two escapes,
   * so the dismissal is spelled out here.
   */
  function assertTaskFilter(and: () => void, toSeeAfter: string[]) {
    cy.gcy('translations-filter-select').click();
    cy.waitForDom();
    cy.gcy('submenu-item').contains('Tasks').click();
    cy.waitForDom();
    and();
    dismissMenu();
    dismissMenu();
    dismissMenu();
    waitForGlobalLoading();
    toSeeAfter.forEach((i) => cy.contains(i).should('be.visible'));
    cy.gcy('translations-key-name').should('have.length', toSeeAfter.length);
  }

  it('filters keys never in a translate task', () => {
    assertTaskFilter(() => {
      translationsView.selectTaskStatus('TRANSLATE', 'NEVER_IN_TASK');
    }, ['key 0', 'key 1', 'key 2', 'key 3']);
  });

  it('filters keys never in a review task', () => {
    // keys 0 and 1 are in an open Czech review task
    assertTaskFilter(() => {
      translationsView.selectTaskStatus('REVIEW', 'NEVER_IN_TASK');
    }, ['key 2', 'key 3']);
  });

  it('filters keys currently in an open review task', () => {
    assertTaskFilter(() => {
      translationsView.selectTaskStatus('REVIEW', 'IN_OPEN_TASK');
    }, ['key 0', 'key 1']);
  });

  it('filters keys not in any open review task', () => {
    assertTaskFilter(() => {
      translationsView.selectTaskStatus('REVIEW', 'NOT_IN_OPEN_TASK');
    }, ['key 2', 'key 3']);
  });

  it('filters keys which have been in a review task', () => {
    assertTaskFilter(() => {
      translationsView.selectTaskStatus('REVIEW', 'HAS_BEEN_IN_TASK');
    }, ['key 0', 'key 1']);
  });

  it('combines the two task types with AND', () => {
    // never translated AND already reviewed matches nothing in this fixture
    assertTaskFilter(() => {
      {
        translationsView.selectTaskStatus('REVIEW', 'HAS_BEEN_IN_TASK');
        translationsView.selectTaskStatus('TRANSLATE', 'HAS_BEEN_IN_TASK');
      }
    }, []);
  });

  it('keeps the conditions of one task type exclusive', () => {
    assertTaskFilter(() => {
      {
        translationsView.selectTaskStatus('REVIEW', 'NEVER_IN_TASK');
        translationsView.getTaskStatusFilter('HAS_BEEN_IN_TASK').click();
        translationsView
          .assertTaskStatusChecked('HAS_BEEN_IN_TASK')
          .assertTaskStatusChecked('NEVER_IN_TASK', false);
      }
    }, ['key 0', 'key 1']);
  });

  it('resets a task type with Any', () => {
    cy.gcy('translations-filter-select').click();
    cy.waitForDom();
    cy.gcy('submenu-item').contains('Tasks').click();
    cy.waitForDom();

    translationsView.selectTaskStatus('REVIEW', 'NEVER_IN_TASK');
    translationsView.getTaskStatusFilter('ANY').click();
    translationsView
      .assertTaskStatusChecked('ANY')
      .assertTaskStatusChecked('NEVER_IN_TASK', false);

    dismissMenu();
    dismissMenu();
    dismissMenu();
    waitForGlobalLoading();

    cy.gcy('translations-filter-select-clear').should('not.exist');
    ['key 0', 'key 1', 'key 2', 'key 3'].forEach((i) =>
      cy.contains(i).should('be.visible')
    );
  });

  it('keeps the condition submenu open across selections', () => {
    cy.gcy('translations-filter-select').click();
    cy.waitForDom();
    cy.gcy('submenu-item').contains('Tasks').click();
    cy.waitForDom();

    translationsView.selectTaskStatus('REVIEW', 'NEVER_IN_TASK');
    // the list is still on screen, so the next condition needs no reopening
    translationsView
      .getTaskStatusFilter('HAS_BEEN_IN_TASK')
      .should('be.visible');
  });

  it('hides the task filter when the view is already scoped to a task', () => {
    // the view shows one task, so a task condition has nothing left to narrow
    cy.url().then((url) => {
      cy.visit(`${url.split('?')[0]}?task=1`);
    });
    waitForGlobalLoading();

    cy.gcy('translations-filter-select').click();
    cy.waitForDom();
    cy.gcy('submenu-item').contains('Tags').should('exist');
    cy.gcy('submenu-item').contains('Tasks').should('not.exist');
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
});
