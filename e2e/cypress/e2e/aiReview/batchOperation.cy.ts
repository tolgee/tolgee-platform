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
  assertRunDialogVariant,
  restoreAiReviewFeature,
  startRun,
  withoutAiReviewFeature,
} from '../../common/aiReview';
import { HOST } from '../../common/constants';

describe('AI review batch operation', () => {
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
    restoreAiReviewFeature();
    aiReviewTestData.clean();
  });

  function visit() {
    cy.visit(
      `${HOST}/projects/${projectId}/translations?languages=en&languages=de`
    );
  }

  function openRunDialog() {
    selectAll();
    openBatchOperationMenu();
    gcyAdvanced({ value: 'batch-select-item', id: 'ai_review' }).click();
    gcy('batch-operations-submit-button').click();
    assertRunDialogVariant('review');
  }

  it('preview shows counts and skip reasons', () => {
    visit();
    openRunDialog();
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
    openRunDialog();
    gcy('ai-review-run-warning').should('be.visible');
  });

  it('start runs a visible job to completion', () => {
    visit();
    openRunDialog();
    startRun();
    cy.gcy('batch-operation-dialog-ok', { timeout: 60_000 }).click();
  });

  it('re-review checkbox changes the counts', () => {
    visit();
    selectAll();
    openBatchOperationMenu();
    gcyAdvanced({ value: 'batch-select-item', id: 'ai_review' }).click();
    gcy('batch-operations-submit-button').click();
    gcy('ai-review-run-to-review')
      .invoke('attr', 'data-cy-count')
      .then((withoutReReview) => {
        gcy('ai-review-run-cancel').click();
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
