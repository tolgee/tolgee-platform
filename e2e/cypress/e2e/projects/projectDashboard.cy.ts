import { login } from '../../common/apiCalls/common';
import { projectListData } from '../../common/apiCalls/testData/testData';
import { createComment, resolveComment } from '../../common/comments';
import { HOST } from '../../common/constants';
import { createProject, enterProject } from '../../common/projects';
import { gcy, gcyAdvanced, selectInProjectMenu } from '../../common/shared';
import { getCell } from '../../common/state';
import { createTag } from '../../common/tags';
import { createTranslation } from '../../common/translations';

describe('Project stats', () => {
  beforeEach(() => {
    projectListData.clean();
    projectListData.generate();
    login('projectListDashboardUser', 'admin');
    cy.visit(HOST);
    gcy('global-base-view-content').should('be.visible');
  });

  afterEach(() => {
    projectListData.clean();
  });

  it('Activity', () => {
    createProject('Project with activity', 'test_username');
    enterProject('Project with activity');
    createTranslation({
      key: 'new translation',
      translation: 'english translation',
    });
    createTag('new tag');
    setStateToReviewed('english translation');
    createComment('new comment', 'new translation', 'en');
    resolveComment('new comment');

    selectInProjectMenu('Project Dashboard');

    assertGroupVisible('SET_TRANSLATION_COMMENT_STATE');
    assertGroupVisible('ADD_TRANSLATION_COMMENT');
    assertGroupVisible('REVIEW');
    assertGroupVisible('EDIT_KEY_TAGS');
    assertGroupVisible('CREATE_KEY');
    assertGroupVisible('CREATE_PROJECT');

    // expandable group detail
    gcyAdvanced({ value: 'activity-group-item', type: 'CREATE_KEY' })
      .findDcy('activity-group-expand-button')
      .click();
    cy.gcy('activity-group-create-key-item')
      .contains('new translation')
      .should('be.visible');
  });

  it('Global statistics', () => {
    enterProject('Project 2');
    createTag('test_tag');
    selectInProjectMenu('Project Dashboard');
    cy.gcy('project-dashboard-task-count').contains(1).should('be.visible');
    cy.gcy('project-dashboard-language-count')
      .contains('2')
      .should('be.visible');

    cy.gcy('project-dashboard-key-count').contains('5').should('be.visible');
    cy.gcy('project-dashboard-base-word-count')
      .contains('4')
      .should('be.visible');
    cy.gcy('project-dashboard-translated-percentage')
      .contains('75%')
      .should('be.visible');
    cy.gcy('project-dashboard-reviewed-percentage')
      .contains('0%')
      .should('be.visible');
    cy.gcy('project-dashboard-tags').contains('1').should('be.visible');

    cy.gcy('project-dashboard-language-bar').first().trigger('mouseover');

    checkLabelRow('Translated', '100%', 3, 4);
    checkLabelRow('Untranslated', '0%', 2, 0);

    cy.gcy('project-dashboard-language-bar').first().trigger('mouseout');

    cy.wait(1000);

    cy.gcy('project-dashboard-language-bar').eq(1).trigger('mouseover');

    checkLabelRow('Reviewed', '0%', 1, 0);
    checkLabelRow('Translated', '75%', 2, 3);
    checkLabelRow('Untranslated', '25%', 2, 1);
  });
});

const assertGroupVisible = (type: string) => {
  gcyAdvanced({ value: 'activity-group-item', type }).should('be.visible');
};

const checkLabelRow = (
  state: string,
  percentage: string,
  keys: number,
  words: number
) => {
  cy.gcy('project-dashboard-language-label-state')
    .contains(state)
    .should('be.visible')
    .next()
    .contains(percentage)
    .should('be.visible')
    .next()
    .contains(`${keys} key`)
    .should('be.visible')
    .next()
    .contains(`${words} word`)
    .should('be.visible');
};

const setStateToReviewed = (translationText: string) => {
  getCell(translationText)
    .trigger('mouseover')
    .findDcy('translation-state-button')
    .click();
};
