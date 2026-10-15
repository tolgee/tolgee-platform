import { gcy, gcyAdvanced } from '../../common/shared';
import { waitForGlobalLoading } from '../../common/loading';

export type AiReviewSkipReason =
  | 'noRule'
  | 'humanReviewed'
  | 'alreadyHasResult'
  | 'openQaIssues'
  | 'never';

export class E2AiReviewRunDialog {
  openBatchOperationsSelect() {
    gcy('batch-operations-select').click();
    return this;
  }

  getBatchOperationItem() {
    return gcyAdvanced({ value: 'batch-select-item', operation: 'ai_review' });
  }

  openFromBatchOperations() {
    this.openBatchOperationsSelect();
    this.getBatchOperationItem().click();
    gcy('batch-operations-submit-button').click();
    this.getDialog();
    return this;
  }

  finishStartedJob() {
    gcy('batch-operation-dialog-ok', { timeout: 60_000 }).click();
    gcy('batch-operation-dialog-ok').should('not.exist');
    waitForGlobalLoading();
  }

  getDialog() {
    return gcy('ai-review-run-dialog').should('be.visible');
  }

  assertWillReview(count: number) {
    this.getDialog().should('have.attr', 'data-cy-will-review', String(count));
    return this;
  }

  assertSkipped(reason: AiReviewSkipReason, count: number) {
    gcyAdvanced({ value: 'ai-review-run-skipped-reason', reason }).should(
      'have.attr',
      'data-cy-count',
      String(count)
    );
    return this;
  }

  assertNoSkippedReason(reason: AiReviewSkipReason) {
    gcyAdvanced({ value: 'ai-review-run-skipped-reason', reason }).should(
      'not.exist'
    );
    return this;
  }

  assertNothingToReview() {
    this.getDialog().should('have.attr', 'data-cy-nothing-to-review', 'true');
    gcy('ai-review-run-confirm').should('not.exist');
    gcy('ai-review-run-ok').should('be.visible');
    return this;
  }

  ok() {
    gcy('ai-review-run-ok').click();
    gcy('ai-review-run-dialog').should('not.exist');
  }

  toggleRereview() {
    gcy('ai-review-run-rereview-checkbox').find('input').click();
    return this;
  }

  confirm() {
    gcy('ai-review-run-confirm').click();
    gcy('ai-review-run-dialog').should('not.exist');
  }

  later() {
    gcy('ai-review-run-cancel').click();
    gcy('ai-review-run-dialog').should('not.exist');
  }
}
