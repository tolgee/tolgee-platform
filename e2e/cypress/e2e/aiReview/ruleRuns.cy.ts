import {
  aiReviewTestData,
  getProjectByNameFromTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy, gcyAdvanced } from '../../common/shared';
import { E2AiReviewRulesView } from '../../compounds/aiReview/E2AiReviewRulesView';
import { E2AiReviewRunDialog } from '../../compounds/aiReview/E2AiReviewRunDialog';

describe('AI review rule runs', () => {
  let projectId: number;
  const rulesView = new E2AiReviewRulesView();
  const runDialog = new E2AiReviewRunDialog();

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

  it('Review now shows the preview and starts a visible job', () => {
    rulesView.visit(projectId);
    rulesView.clickReviewNow('UI rule');
    runDialog.assertVariant('review');
    runDialog.getToReview().should('be.visible');
    runDialog.start();
    gcy('batch-operations-summary').should('exist');
    rulesView.getRule('UI rule').within(() => {
      gcy('ai-review-rule-review-now', { timeout: 60_000 }).should('not.exist');
    });
  });

  it('Run review on the rules page previews all languages', () => {
    rulesView.visit(projectId);
    gcy('ai-review-run-button').click();
    runDialog.assertVariant('review');
    runDialog.getToReview().should('be.visible');
    gcy('ai-review-run-re-review').click();
    runDialog.getToReview().should('be.visible');
    runDialog.start();
    gcy('batch-operations-summary').should('exist');
  });

  it('Score now on a Score only rule', () => {
    rulesView.visit(projectId);
    rulesView.clickReviewNow('Legal score only');
    runDialog.assertVariant('score').start();
    rulesView.getRule('Legal score only').within(() => {
      gcy('ai-review-rule-review-now', { timeout: 60_000 }).should('not.exist');
    });
  });

  it('Retry failed reviews brings the failed count to 0', () => {
    rulesView.visit(projectId);
    rulesView.clickRetryFailed('UI rule');
    runDialog.assertVariant('retry').start();
    gcyAdvanced(
      { value: 'ai-review-rule-counts', mode: 'ACTIVE', failed: '0' },
      { timeout: 60_000 }
    ).should('exist');
  });

  it('after save with backlog opens the run dialog and Cancel closes it', () => {
    rulesView.visit(projectId);
    rulesView.openEditDialog('UI rule').save();
    runDialog.assertVariant('review');
    runDialog.cancel();
    gcy('ai-review-run-dialog').should('not.exist');
  });

  it('after save on a score-only rule opens the run dialog with score wording', () => {
    rulesView.visit(projectId);
    rulesView.openEditDialog('Legal score only').save();
    runDialog.assertVariant('score');
  });

  it('run links are hidden without the batch machine translate scope', () => {
    login('prompts_viewer_username');
    rulesView.visit(projectId);
    gcy('ai-review-rule-item').should('have.length', 2);
    gcy('ai-review-rule-review-now').should('not.exist');
    gcy('ai-review-rule-retry-failed').should('not.exist');
  });
});
