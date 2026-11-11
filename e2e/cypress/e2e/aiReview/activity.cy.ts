import {
  aiReviewTestData,
  getProjectByNameFromTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import {
  confirmStandard,
  gcyAdvanced,
  visitProjectSettingsAdvanced,
} from '../../common/shared';
import { editCell } from '../../common/translations';
import { waitForGlobalLoading } from '../../common/loading';
import { E2AiReviewRulesView } from '../../compounds/aiReview/E2AiReviewRulesView';
import { E2AiReviewRunDialog } from '../../compounds/aiReview/E2AiReviewRunDialog';
import { E2ActivityChecker } from '../../compounds/E2ActivityChecker';
import { E2AiReviewBadge } from '../../compounds/aiReview/E2AiReviewBadge';
import { E2SourceChangeSettings } from '../../compounds/aiReview/E2SourceChangeSettings';
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
    createDialog.getNameInput().type('Activity rule');
    createDialog.save();
    runDialog.cancel();

    const editDialog = rulesView.openEditDialog('Legal score only');
    editDialog.getNameInput().type(' 2');
    editDialog.save();
    runDialog.cancel();

    rulesView.deleteRule('Activity rule');
    confirmStandard();
    waitForGlobalLoading();

    visitProjectSettingsAdvanced(projectId);
    new E2SourceChangeSettings().choose('humanEdited', 'RETRANSLATE');

    activity.visitDashboard(projectId);
    // activity-compact uses display: contents, so it has no own box to be "visible".
    activity.getActivity('AI_REVIEW_RULE_CREATE').should('exist');
    activity.getActivity('AI_REVIEW_RULE_UPDATE').should('exist');
    activity.getActivity('AI_REVIEW_RULE_DELETE').should('exist');
    activity.getActivity('AI_REVIEW_SETTINGS_UPDATE').should('exist');
  });

  it('an automatic review shows the AI reviewer without an author', () => {
    new E2TranslationsView().visit(projectId, {
      languages: ['en', 'de'],
      search: 'key_not_reviewed',
    });
    editCell('Noch nicht geprüft', 'Neuer Text');
    new E2AiReviewBadge('key_not_reviewed').assertVisible('APPROVED', {
      timeout: AUTOMATIC_REVIEW_TIMEOUT,
    });
    activity.visitDashboard(projectId);
    activity.getActivity('AI_REVIEW').should('exist');
    gcyAdvanced({
      value: 'activity-system-actor',
      'started-by': 'false',
    }).should('be.visible');
  });

  // Asserts on seeded data: a live run finalizes its activity asynchronously, so it would race.
  it('a run shows the AI reviewer with the started-by tooltip', () => {
    activity.visitDashboard(projectId);
    gcyAdvanced({
      value: 'activity-system-actor',
      'started-by': 'true',
    }).should('be.visible');
  });

  it('translation history shows the AI reviewer', () => {
    new E2TranslationsView().visit(projectId, {
      languages: ['en', 'de'],
      search: 'key_not_reviewed',
    });
    editCell('Noch nicht geprüft', 'Neuer Text');
    new E2AiReviewBadge('key_not_reviewed').assertVisible('APPROVED', {
      timeout: AUTOMATIC_REVIEW_TIMEOUT,
    });
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
