import {
  aiReviewTestData,
  getProjectByNameFromTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { confirmStandard, gcy, gcyAdvanced } from '../../common/shared';
import { editCell } from '../../common/translations';
import { visitAiReviewRules } from '../../common/aiReview';
import { HOST } from '../../common/constants';
import { waitForGlobalLoading } from '../../common/loading';

describe('AI reviewer in activity', () => {
  let projectId: number;

  beforeEach(() => {
    aiReviewTestData.clean();
    aiReviewTestData.generateStandard().then((res) => {
      projectId = getProjectByNameFromTestData(
        res.body,
        'ai_review_project'
      )!.id;
    });
    login('test_username');
  });

  afterEach(() => {
    aiReviewTestData.clean();
  });

  function visitDashboard() {
    cy.visit(`${HOST}/projects/${projectId}`);
    cy.gcy('project-dashboard-activity-list').should('be.visible');
  }

  it('project activity shows rule and settings changes', () => {
    visitAiReviewRules(projectId);

    gcy('ai-review-add-rule-button').click();
    gcy('ai-review-rule-form-name').type('Activity rule');
    gcy('ai-review-rule-form-save').click();
    cy.gcy('global-confirmation-cancel').click();

    gcyAdvanced({
      value: 'ai-review-rule-edit',
      name: 'Legal dry run',
    }).click();
    gcy('ai-review-rule-form-name').type(' 2');
    gcy('ai-review-rule-form-save').click();
    cy.gcy('global-confirmation-cancel').click();

    gcyAdvanced({
      value: 'ai-review-rule-menu',
      name: 'Activity rule',
    }).click();
    gcy('ai-review-rule-menu-delete').click();
    confirmStandard();
    waitForGlobalLoading();

    gcyAdvanced({
      value: 'source-change-select',
      field: 'humanEdited',
    }).click();
    gcyAdvanced({
      value: 'source-change-select-item',
      action: 'RETRANSLATE',
    }).click();
    waitForGlobalLoading();

    visitDashboard();
    // activity-compact uses display: contents, so it has no own box to be "visible".
    gcyAdvanced({
      value: 'activity-compact',
      type: 'AI_REVIEW_RULE_CREATE',
    }).should('exist');
    gcyAdvanced({
      value: 'activity-compact',
      type: 'AI_REVIEW_RULE_UPDATE',
    }).should('exist');
    gcyAdvanced({
      value: 'activity-compact',
      type: 'AI_REVIEW_RULE_DELETE',
    }).should('exist');
    gcyAdvanced({
      value: 'activity-compact',
      type: 'AI_REVIEW_SETTINGS_UPDATE',
    }).should('exist');
  });

  it('an automatic review shows the AI reviewer without an author', () => {
    cy.visit(
      `${HOST}/projects/${projectId}/translations?languages=en&languages=de&search=key_not_reviewed`
    );
    editCell('Noch nicht geprüft', 'Neuer Text');
    gcyAdvanced(
      { value: 'ai-review-badge', status: 'APPROVED' },
      { timeout: 30_000 }
    ).should('be.visible');
    visitDashboard();
    const startedBy: DataCy.Value = 'activity-started-by';
    gcyAdvanced({ value: 'activity-compact', type: 'AI_REVIEW' })
      .not(`:has([data-cy=${startedBy}])`)
      .findDcy('activity-system-actor')
      .should('be.visible');
  });

  // Asserts on seeded data: a live run finalizes its activity asynchronously, so it would race.
  it('a run shows the AI reviewer and who started it', () => {
    visitDashboard();
    cy.gcy('activity-started-by', { timeout: 30_000 }).should('be.visible');
    cy.gcy('activity-started-by')
      .first()
      .closestDcy('activity-compact')
      .findDcy('activity-system-actor')
      .should('be.visible');
  });

  it('translation history shows the AI reviewer', () => {
    cy.visit(
      `${HOST}/projects/${projectId}/translations?languages=en&languages=de&search=key_not_reviewed`
    );
    editCell('Noch nicht geprüft', 'Neuer Text');
    // The badge arrives over the websocket once the automatic review is done.
    gcyAdvanced(
      { value: 'ai-review-badge', status: 'APPROVED' },
      { timeout: 30_000 }
    ).should('be.visible');
    gcyAdvanced({
      value: 'translations-table-cell',
      key: 'key_not_reviewed',
      language: 'de',
    }).click();
    gcyAdvanced({ value: 'translation-panel-toggle', id: 'history' }).click();
    cy.waitForDom();
    cy.gcy('translation-history-item', { timeout: 30_000 })
      .findDcy('activity-system-actor')
      .should('be.visible');
  });
});
