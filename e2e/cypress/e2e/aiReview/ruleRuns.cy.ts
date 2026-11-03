import {
  aiReviewTestData,
  getProjectByNameFromTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy, gcyAdvanced } from '../../common/shared';
import {
  assertRunDialogVariant,
  startRun,
  visitAiReviewRules,
} from '../../common/aiReview';

describe('AI review rule runs', () => {
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

  it('Review now shows the preview and starts a visible job', () => {
    visitAiReviewRules(projectId);
    gcyAdvanced({ value: 'ai-review-rule-item', name: 'UI rule' }).within(
      () => {
        gcy('ai-review-rule-review-now').click();
      }
    );
    assertRunDialogVariant('review');
    gcy('ai-review-run-to-review').should('be.visible');
    startRun();
    gcy('batch-operations-summary').should('exist');
    gcyAdvanced({ value: 'ai-review-rule-item', name: 'UI rule' }).within(
      () => {
        gcy('ai-review-rule-backlog', { timeout: 60_000 }).should('not.exist');
      }
    );
  });

  it('Score now on a Dry run rule', () => {
    visitAiReviewRules(projectId);
    gcyAdvanced({
      value: 'ai-review-rule-item',
      name: 'Legal dry run',
    }).within(() => {
      gcy('ai-review-rule-review-now').click();
    });
    assertRunDialogVariant('score');
    startRun();
    gcyAdvanced({ value: 'ai-review-rule-item', name: 'Legal dry run' }).within(
      () => {
        gcy('ai-review-rule-backlog', { timeout: 60_000 }).should('not.exist');
      }
    );
  });

  it('Retry failed reviews brings the failed count to 0', () => {
    visitAiReviewRules(projectId);
    gcyAdvanced({ value: 'ai-review-rule-item', name: 'UI rule' }).within(
      () => {
        gcy('ai-review-rule-retry-failed').click();
      }
    );
    assertRunDialogVariant('retry');
    startRun();
    gcyAdvanced(
      { value: 'ai-review-rule-counts-active', failed: '0' },
      { timeout: 60_000 }
    ).should('exist');
  });

  it('after save with backlog opens the run dialog and Cancel closes it', () => {
    visitAiReviewRules(projectId);
    gcyAdvanced({ value: 'ai-review-rule-edit', name: 'UI rule' }).click();
    gcy('ai-review-rule-form-save').click();
    assertRunDialogVariant('review');
    gcy('ai-review-run-cancel').click();
    gcy('ai-review-run-dialog').should('not.exist');
  });

  it('after save on a dry run rule opens the run dialog with score wording', () => {
    visitAiReviewRules(projectId);
    gcyAdvanced({
      value: 'ai-review-rule-edit',
      name: 'Legal dry run',
    }).click();
    gcy('ai-review-rule-form-save').click();
    assertRunDialogVariant('score');
  });

  it('run links are hidden without the batch machine translate scope', () => {
    login('prompts_viewer_username');
    visitAiReviewRules(projectId);
    gcy('ai-review-rule-item').should('have.length', 2);
    gcy('ai-review-rule-review-now').should('not.exist');
    gcy('ai-review-rule-retry-failed').should('not.exist');
  });
});
