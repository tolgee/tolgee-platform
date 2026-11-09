import {
  aiReviewTestData,
  getProjectByNameFromTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy, gcyAdvanced } from '../../common/shared';
import {
  openBatchOperationMenu,
  selectAll,
} from '../../common/batchOperations';
import {
  restoreAiReviewFeature,
  withoutAiReviewFeature,
} from '../../common/aiReview';
import { E2TranslationsView } from '../../compounds/E2TranslationsView';

describe('AI review batch operation', () => {
  let projectId: number;
  const view = new E2TranslationsView();

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
    restoreAiReviewFeature();
    aiReviewTestData.clean();
  });

  function visit() {
    view.visit(projectId, { languages: ['en', 'de'] });
  }

  it('preview shows counts and skip reasons', () => {
    visit();
    view.openAiReviewRunDialog().assertVariant('review');
    gcy('ai-review-run-to-review').should('be.visible');
    gcyAdvanced({
      value: 'ai-review-run-skip-reason',
      reason: 'ALREADY_HAS_RESULT',
    }).should('be.visible');
    gcyAdvanced({
      value: 'ai-review-run-skip-reason',
      reason: 'NO_RULE',
    }).should('be.visible');
    gcyAdvanced({
      value: 'ai-review-run-skip-reason',
      reason: 'REJECTED_BY_HUMAN',
    }).should('be.visible');
  });

  it('warns above 1000 translations', () => {
    visit();
    cy.intercept('POST', '**/ai-review/run/preview', {
      statusCode: 200,
      body: { toReview: 1500, dryRunOnly: 0, skipped: {} },
    });
    view.openAiReviewRunDialog().assertVariant('review');
    gcy('ai-review-run-warning').should('be.visible');
  });

  it('start runs a visible job to completion', () => {
    visit();
    view.openAiReviewRunDialog().assertVariant('review').start();
    cy.gcy('batch-operation-dialog-ok', { timeout: 60_000 }).click();
  });

  it('re-review checkbox changes the counts', () => {
    visit();
    const runDialog = view.openAiReviewRunDialog();
    gcy('ai-review-run-to-review')
      .invoke('attr', 'data-cy-count')
      .then((withoutReReview) => {
        runDialog.cancel();
        gcy('ai-review-batch-re-review').click();
        gcy('batch-operations-submit-button').click();
        gcy('ai-review-run-to-review')
          .invoke('attr', 'data-cy-count')
          .should('not.eq', withoutReReview);
        gcy('ai-review-run-includes-resolved').should('be.visible');
      });
  });

  it('is hidden without the feature', () => {
    withoutAiReviewFeature();
    visit();
    selectAll();
    openBatchOperationMenu();
    gcyAdvanced({ value: 'batch-select-item', id: 'machine_translate' }).should(
      'exist'
    );
    gcyAdvanced({ value: 'batch-select-item', id: 'ai_review' }).should(
      'not.exist'
    );
  });
});
