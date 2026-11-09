import {
  aiReviewTestData,
  getProjectByNameFromTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import {
  confirmStandard,
  gcy,
  gcyAdvanced,
  visitProjectSettingsAdvanced,
} from '../../common/shared';
import { editCell } from '../../common/translations';
import { waitForGlobalLoading } from '../../common/loading';
import { E2AiReviewRulesView } from '../../compounds/aiReview/E2AiReviewRulesView';
import { E2AiReviewRunDialog } from '../../compounds/aiReview/E2AiReviewRunDialog';
import { E2ActivityChecker } from '../../compounds/E2ActivityChecker';
import { E2TranslationsView } from '../../compounds/E2TranslationsView';

const AUTOMATIC_REVIEW_TIMEOUT = 30_000;

describe('AI reviewer in activity', () => {
  let projectId: number;
  const rulesView = new E2AiReviewRulesView();
  const runDialog = new E2AiReviewRunDialog();
  const activity = new E2ActivityChecker();

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

  it('project activity shows rule and settings changes', () => {
    rulesView.visit(projectId);

    const createDialog = rulesView.openCreateDialog();
    gcy('ai-review-rule-form-name').type('Activity rule');
    createDialog.save();
    runDialog.cancel();

    const editDialog = rulesView.openEditDialog('Legal dry run');
    gcy('ai-review-rule-form-name').type(' 2');
    editDialog.save();
    runDialog.cancel();

    gcyAdvanced({
      value: 'ai-review-rule-menu',
      name: 'Activity rule',
    }).click();
    gcy('ai-review-rule-menu-delete').click();
    confirmStandard();
    waitForGlobalLoading();

    visitProjectSettingsAdvanced(projectId);
    gcyAdvanced({
      value: 'source-change-select',
      field: 'humanEdited',
    }).click();
    gcyAdvanced({
      value: 'source-change-select-item',
      action: 'RETRANSLATE',
    }).click();
    waitForGlobalLoading();

    activity.visitDashboard(projectId);
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
    new E2TranslationsView().visit(projectId, {
      languages: ['en', 'de'],
      search: 'key_not_reviewed',
    });
    editCell('Noch nicht geprüft', 'Neuer Text');
    gcyAdvanced(
      { value: 'ai-review-badge', status: 'APPROVED' },
      { timeout: AUTOMATIC_REVIEW_TIMEOUT }
    ).should('be.visible');
    activity.visitDashboard(projectId);
    const startedBy: DataCy.Value = 'activity-started-by';
    gcyAdvanced({ value: 'activity-compact', type: 'AI_REVIEW' })
      .not(`:has([data-cy=${startedBy}])`)
      .findDcy('activity-system-actor')
      .should('be.visible');
  });

  // Asserts on seeded data: a live run finalizes its activity asynchronously, so it would race.
  it('a run shows the AI reviewer and who started it', () => {
    activity.visitDashboard(projectId);
    cy.gcy('activity-started-by', { timeout: 30_000 }).should('be.visible');
    cy.gcy('activity-started-by')
      .first()
      .closestDcy('activity-compact')
      .findDcy('activity-system-actor')
      .should('be.visible');
  });

  it('translation history shows the AI reviewer', () => {
    new E2TranslationsView().visit(projectId, {
      languages: ['en', 'de'],
      search: 'key_not_reviewed',
    });
    editCell('Noch nicht geprüft', 'Neuer Text');
    gcyAdvanced(
      { value: 'ai-review-badge', status: 'APPROVED' },
      { timeout: AUTOMATIC_REVIEW_TIMEOUT }
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
