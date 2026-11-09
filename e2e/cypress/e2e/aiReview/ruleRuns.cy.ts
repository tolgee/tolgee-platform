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
    gcyAdvanced({ value: 'ai-review-rule-item', name: 'UI rule' }).within(
      () => {
        gcy('ai-review-rule-review-now').click();
      }
    );
    runDialog.assertVariant('review');
    gcy('ai-review-run-to-review').should('be.visible');
    runDialog.start();
    gcy('batch-operations-summary').should('exist');
    gcyAdvanced({ value: 'ai-review-rule-item', name: 'UI rule' }).within(
      () => {
        gcy('ai-review-rule-backlog', { timeout: 60_000 }).should('not.exist');
      }
    );
  });

  it('Score now on a Dry run rule', () => {
    rulesView.visit(projectId);
    gcyAdvanced({
      value: 'ai-review-rule-item',
      name: 'Legal dry run',
    }).within(() => {
      gcy('ai-review-rule-review-now').click();
    });
    runDialog.assertVariant('score').start();
    gcyAdvanced({ value: 'ai-review-rule-item', name: 'Legal dry run' }).within(
      () => {
        gcy('ai-review-rule-backlog', { timeout: 60_000 }).should('not.exist');
      }
    );
  });

  it('Retry failed reviews brings the failed count to 0', () => {
    rulesView.visit(projectId);
    gcyAdvanced({ value: 'ai-review-rule-item', name: 'UI rule' }).within(
      () => {
        gcy('ai-review-rule-retry-failed').click();
      }
    );
    runDialog.assertVariant('retry').start();
    gcyAdvanced(
      { value: 'ai-review-rule-counts-active', failed: '0' },
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

  it('after save on a dry run rule opens the run dialog with score wording', () => {
    rulesView.visit(projectId);
    rulesView.openEditDialog('Legal dry run').save();
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
